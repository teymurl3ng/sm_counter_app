package com.saaf.milkshop.cloud

import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException

data class CloudBackupStatus(
    val configured: Boolean,
    val enabled: Boolean,
    val provider: String,
    val accountDisplay: String?,
    val allowMetered: Boolean,
    val scheduleDescription: String,
    val lastAttemptAt: String?,
    val lastSuccessAt: String?,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
    val stale: Boolean,
    val pending: Boolean,
    val snapshots: List<CloudRemoteSnapshot>,
)

data class CloudBackupRunResult(
    val snapshotId: String,
    val createdAt: String,
    val uploaded: Boolean,
    val retentionWarnings: Int,
)

class CloudBackupCoordinator(
    private val database: MilkShopDatabase,
    private val backupManager: BackupManager,
    private val settings: CloudBackupSettingsStore,
    private val outbox: CloudBackupOutbox,
    private val cryptography: CloudBackupCryptography,
    private val provider: CloudBackupProvider,
    private val now: () -> Instant = Instant::now,
) {
    suspend fun configure(
        account: CloudAccountIdentity,
        recoveryPassphrase: CharArray,
        allowMetered: Boolean,
        ownerUserId: Long,
        ownerName: String,
        ownerRole: String,
    ): CloudBackupState {
        require(settings.read() == null) { "Disconnect the current cloud-backup account before connecting another one" }
        val vault = cryptography.createVault(recoveryPassphrase)
        val instant = now().toString()
        val existing = provider.listSnapshots(null)
        return CloudBackupState(
            installationId = UUID.randomUUID().toString(),
            configuredAt = instant,
            enabled = true,
            allowMetered = allowMetered,
            accountPermissionId = account.permissionId,
            accountDisplay = maskEmail(account.displayEmail),
            enabledByUserId = ownerUserId,
            enabledByName = ownerName,
            enabledByRole = ownerRole,
            vault = vault,
            snapshots = existing,
        ).also(settings::write)
    }

    fun status(): CloudBackupStatus {
        val state = settings.read()
        if (state == null) {
            return CloudBackupStatus(
                configured = false,
                enabled = false,
                provider = PROVIDER_NAME,
                accountDisplay = null,
                allowMetered = false,
                scheduleDescription = SCHEDULE_DESCRIPTION,
                lastAttemptAt = null,
                lastSuccessAt = null,
                lastErrorCode = null,
                lastErrorMessage = null,
                stale = false,
                pending = false,
                snapshots = emptyList(),
            )
        }
        val protectedAt = state.lastSuccessAt ?: state.configuredAt
        val stale = Duration.between(Instant.parse(protectedAt), now()) > STALE_AFTER
        return CloudBackupStatus(
            configured = true,
            enabled = state.enabled,
            provider = PROVIDER_NAME,
            accountDisplay = state.accountDisplay,
            allowMetered = state.allowMetered,
            scheduleDescription = SCHEDULE_DESCRIPTION,
            lastAttemptAt = state.lastAttemptAt,
            lastSuccessAt = state.lastSuccessAt,
            lastErrorCode = state.lastErrorCode,
            lastErrorMessage = state.lastErrorMessage,
            stale = stale,
            pending = state.pending != null,
            snapshots = state.snapshots.sortedByDescending { Instant.parse(it.createdAt) },
        )
    }

    fun updatePolicy(enabled: Boolean, allowMetered: Boolean): CloudBackupState =
        settings.update { it.copy(enabled = enabled, allowMetered = allowMetered) }

    fun disableAfterRestore(): CloudBackupState = settings.update {
        it.copy(
            enabled = false,
            lastErrorCode = null,
            lastErrorMessage = null,
        )
    }

    fun disconnectLocal() {
        outbox.clearAll()
        settings.clear()
    }

    suspend fun refreshSnapshots(): List<CloudRemoteSnapshot> {
        val state = requireState()
        requireMatchingAccount(state)
        val remote = provider.listSnapshots(null)
        settings.write(state.copy(snapshots = remote))
        return remote
    }

    suspend fun runBackup(): CloudBackupRunResult {
        var state = requireState()
        require(state.enabled) { "Automatic cloud backup is disabled" }
        val attemptedAt = now().toString()
        settings.write(state.copy(lastAttemptAt = attemptedAt, lastErrorCode = null, lastErrorMessage = null))
        return try {
            requireMatchingAccount(state)
            state = settings.read() ?: throw CloudBackupFailure.NotConfigured()
            val pending = state.pending ?: createPending(state).also { created ->
                state = settings.update { it.copy(pending = created) }
            }
            val bytes = outbox.read(pending)
            val remote = provider.findSnapshot(state.vault.vaultId, pending.snapshotId)
                ?: provider.uploadSnapshot(pending, state.vault.vaultId, state.installationId, bytes)
            verifyRemote(remote, pending, bytes)

            val allSnapshots = provider.listSnapshots(null)
            val currentVaultSnapshots = allSnapshots.filter { it.vaultId == state.vault.vaultId }
            var cleanupWarnings = 0
            val deletedIds = mutableSetOf<String>()
            CloudBackupRetention.removals(currentVaultSnapshots).forEach { candidate ->
                try {
                    provider.deleteSnapshot(candidate.remoteFileId)
                    deletedIds += candidate.remoteFileId
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    cleanupWarnings += 1
                }
            }
            val retained = allSnapshots.filterNot { it.remoteFileId in deletedIds }
            outbox.remove(pending)
            settings.write(
                state.copy(
                    pending = null,
                    lastAttemptAt = attemptedAt,
                    lastSuccessAt = attemptedAt,
                    lastErrorCode = null,
                    lastErrorMessage = if (cleanupWarnings == 0) null else "The backup succeeded, but $cleanupWarnings old recovery point(s) could not be removed.",
                    snapshots = retained,
                ),
            )
            audit(state, "success", "snapshot_id=${pending.snapshotId};retention_warnings=$cleanupWarnings")
            CloudBackupRunResult(pending.snapshotId, pending.createdAt, uploaded = true, retentionWarnings = cleanupWarnings)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val code = failureCode(error)
            runCatching {
                settings.update {
                    it.copy(lastAttemptAt = attemptedAt, lastErrorCode = code, lastErrorMessage = recoveryMessage(error))
                }
            }
            runCatching { audit(state, "failure", "category=$code") }
            throw error
        }
    }

    suspend fun downloadAndDecrypt(remoteFileId: String, recoveryPassphrase: CharArray): DecryptedCloudSnapshot {
        val state = requireState()
        requireMatchingAccount(state)
        val expected = state.snapshots.singleOrNull { it.remoteFileId == remoteFileId }
            ?: throw CloudBackupFailure.Remote("Refresh cloud recovery points and choose the backup again")
        val bytes = provider.downloadSnapshot(remoteFileId)
        require(bytes.size.toLong() == expected.sizeBytes) { "Downloaded cloud backup size does not match Google Drive" }
        expected.sha256?.let { require(sha256(bytes).equals(it, ignoreCase = true)) { "Downloaded cloud backup checksum does not match Google Drive" } }
        val decrypted = cryptography.decryptSnapshot(bytes, recoveryPassphrase)
        require(decrypted.snapshotId == expected.snapshotId && decrypted.vaultId == expected.vaultId) {
            "The cloud backup identity does not match its Drive record"
        }
        return decrypted
    }

    private suspend fun createPending(state: CloudBackupState): CloudPendingSnapshot {
        val snapshotId = UUID.randomUUID().toString()
        val createdAt = now().toString()
        val payload = backupManager.buildCompleteSnapshotBytes(database)
        val encrypted = try {
            cryptography.encryptSnapshot(payload, state.vault, snapshotId, createdAt)
        } finally {
            payload.fill(0)
        }
        return try {
            outbox.create(snapshotId, createdAt, encrypted)
        } finally {
            encrypted.fill(0)
        }
    }

    private suspend fun requireMatchingAccount(state: CloudBackupState) {
        val active = provider.accountIdentity()
        if (active.permissionId != state.accountPermissionId) throw CloudBackupFailure.AccountMismatch()
    }

    private suspend fun verifyRemote(
        remote: CloudRemoteSnapshot,
        pending: CloudPendingSnapshot,
        bytes: ByteArray,
    ) {
        require(remote.snapshotId == pending.snapshotId) { "Google Drive returned a different cloud snapshot" }
        require(remote.sizeBytes == pending.sizeBytes) { "Google Drive backup size verification failed" }
        if (remote.sha256 != null) {
            require(remote.sha256.equals(pending.sha256, ignoreCase = true)) { "Google Drive backup checksum verification failed" }
        } else {
            val downloaded = provider.downloadSnapshot(remote.remoteFileId)
            require(downloaded.size == bytes.size && sha256(downloaded) == pending.sha256) {
                "Google Drive backup download verification failed"
            }
        }
    }

    private fun requireState(): CloudBackupState = settings.read() ?: throw CloudBackupFailure.NotConfigured()

    private suspend fun audit(state: CloudBackupState, result: String, details: String) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = now().toString(),
                action = "scheduled_cloud_backup",
                result = result,
                actorUserId = null,
                actorNameSnapshot = "Scheduled backup (enabled by ${state.enabledByName})",
                actorRoleSnapshot = "system",
                details = details,
            ),
        )
    }

    private fun failureCode(error: Throwable): String = when (error) {
        is CloudBackupFailure.AuthorizationRequired -> "authorization_required"
        is CloudBackupFailure.AccountMismatch -> "account_mismatch"
        is CloudBackupFailure.Quota -> "quota"
        is CloudBackupFailure.Network -> "network"
        is CloudBackupFailure.KeyUnavailable -> "key_unavailable"
        is CloudBackupFailure.NotConfigured -> "not_configured"
        else -> "failed"
    }

    private fun recoveryMessage(error: Throwable): String = when (error) {
        is CloudBackupFailure.AuthorizationRequired -> "Open Backup & restore as the owner and reconnect Google Drive."
        is CloudBackupFailure.AccountMismatch -> "Reconnect the Google account originally selected for this backup vault."
        is CloudBackupFailure.Quota -> "Free Google Drive storage, then use Back up now. Existing recovery points were kept."
        is CloudBackupFailure.Network -> "The pending encrypted snapshot was kept and will be retried when the network is available."
        is CloudBackupFailure.KeyUnavailable -> "Reconnect cloud backup and create a new vault. Existing cloud recovery points still require their recovery passphrase."
        else -> error.message ?: "Open Backup & restore and try again."
    }

    companion object {
        private const val PROVIDER_NAME = "Google Drive"
        private const val SCHEDULE_DESCRIPTION = "Approximately every 24 hours"
        private val STALE_AFTER = Duration.ofHours(48)

        fun maskEmail(value: String): String {
            val trimmed = value.trim().lowercase(Locale.ROOT)
            val at = trimmed.indexOf('@')
            if (at <= 0 || at == trimmed.lastIndex) return "Google account"
            val local = trimmed.substring(0, at)
            val domain = trimmed.substring(at + 1)
            val visible = local.take(1)
            return "$visible${"•".repeat((local.length - 1).coerceIn(2, 8))}@$domain"
        }
    }
}
