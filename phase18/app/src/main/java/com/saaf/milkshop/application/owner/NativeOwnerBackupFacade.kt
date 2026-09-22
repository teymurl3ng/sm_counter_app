package com.saaf.milkshop.application.owner

import android.content.Context
import android.net.Uri
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.cloud.CloudBackupFailure
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudBackupState
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NativeOwnerBackupSnapshot(
    val configured: Boolean,
    val enabled: Boolean,
    val accountDisplay: String?,
    val scheduleDescription: String,
    val lastSuccessAt: String?,
    val lastAttemptAt: String?,
    val lastErrorMessage: String?,
    val stale: Boolean,
    val pending: Boolean,
    val statusHeadline: String,
    val allowMetered: Boolean = false,
    val cloudActionsAvailable: Boolean = false,
)

data class NativeOwnerBackupExportResult(
    val displayName: String,
    val createdAt: String,
    val formatVersion: Int,
)

data class NativeOwnerBackupInspection(
    val restoreTicket: String,
    val createdAt: String,
    val formatVersion: Int,
    val containsSecurityState: Boolean,
    val fileName: String,
)

data class NativeOwnerBackupRestoreResult(
    val restored: Boolean,
    val sessionsInvalidated: Boolean,
)

/**
 * Cloud Drive actions for Owner Backup. Live impl uses Google authorization
 * Activity results + [com.saaf.milkshop.cloud.CloudBackupCoordinator]. Tests
 * inject a fake that mutates [CloudBackupSettingsStore] without OAuth.
 */
interface NativeOwnerCloudBackupGateway {
    suspend fun connect(
        recoveryPassphrase: CharArray,
        allowMetered: Boolean,
        ownerUserId: Long,
        ownerName: String,
        ownerRole: String,
    )

    suspend fun updatePolicy(enabled: Boolean, allowMetered: Boolean)

    suspend fun runNow()

    suspend fun disconnect(confirmRevoke: Boolean)
}

/**
 * Owner Backup boundary: Drive vault status plus typed local encrypted
 * export / inspect / restore and Drive connect / policy / run-now /
 * disconnect. Never calls bridge JSON; never invents crypto.
 */
class NativeOwnerBackupFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val cloudBackupSettings: CloudBackupSettingsStore? = null,
    private val backupManager: BackupManager? = null,
    private val cloudGateway: NativeOwnerCloudBackupGateway? = null,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    private data class RestoreTicket(
        val token: String,
        val sessionId: String,
        val actorUserId: Long,
        val uri: Uri,
        val passphrase: CharArray,
        val inspection: BackupManager.EncryptedBackupInspection,
        val expiresAtEpochMs: Long,
    )

    private val tickets = ConcurrentHashMap<String, RestoreTicket>()
    private val expiryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun load(
        sessionHandle: String,
        now: Instant = Instant.now(),
    ): NativeOwnerBackupSnapshot {
        requireOwnerExport(sessionHandle, recent = false)
        return snapshotFromSettings(now)
    }

    fun suggestedExportFileName(context: Context): String =
        BackupManager.displayName(context)

    suspend fun exportEncrypted(
        sessionHandle: String,
        uri: Uri,
        passphrase: CharArray,
    ): NativeOwnerBackupExportResult {
        val manager = requireBackupManager()
        return try {
            val actor = requireOwnerExport(sessionHandle, recent = true, allowQuickPin = true)
            val meta = manager.exportEncryptedSnapshot(database, uri, passphrase)
                .getOrElse(::throwBackupFailure)
            audit(
                ACTION_EXPORT_BACKUP,
                actor.userId,
                actor.displayName,
                actor.role,
                "format_version=${meta.formatVersion}",
            )
            NativeOwnerBackupExportResult(
                displayName = meta.fileName,
                createdAt = meta.createdAt,
                formatVersion = meta.formatVersion,
            )
        } finally {
            passphrase.fill('\u0000')
        }
    }

    suspend fun inspectEncrypted(
        sessionHandle: String,
        uri: Uri,
        passphrase: CharArray,
    ): NativeOwnerBackupInspection {
        val manager = requireBackupManager()
        var retained = false
        return try {
            val actor = requireOwnerRestore(sessionHandle, recent = true, allowQuickPin = true)
            val inspection = manager.inspectEncryptedSnapshot(uri, passphrase)
                .getOrElse(::throwBackupFailure)
            if (!inspection.containsSecurityState) {
                throw NativeOwnerBackupFailure(
                    "backup_incompatible",
                    "This encrypted backup contains no application users",
                )
            }
            clearExpiredTickets()
            tickets.entries.removeIf { entry ->
                val remove = entry.value.sessionId == sessionHandle
                if (remove) entry.value.passphrase.fill('\u0000')
                remove
            }
            val token = UUID.randomUUID().toString()
            tickets[token] = RestoreTicket(
                token = token,
                sessionId = sessionHandle,
                actorUserId = actor.userId,
                uri = uri,
                passphrase = passphrase,
                inspection = inspection,
                expiresAtEpochMs = nowEpochMs() + TICKET_LIFETIME_MS,
            )
            expiryScope.launch {
                delay(TICKET_LIFETIME_MS)
                tickets.remove(token)?.passphrase?.fill('\u0000')
            }
            retained = true
            NativeOwnerBackupInspection(
                restoreTicket = token,
                createdAt = inspection.meta.createdAt,
                formatVersion = inspection.meta.formatVersion,
                containsSecurityState = inspection.containsSecurityState,
                fileName = inspection.meta.fileName,
            )
        } finally {
            if (!retained) passphrase.fill('\u0000')
        }
    }

    suspend fun confirmRestore(
        sessionHandle: String,
        restoreTicket: String,
        confirmRestore: Boolean,
    ): NativeOwnerBackupRestoreResult {
        if (!confirmRestore) {
            throw NativeOwnerBackupFailure(
                "validation_failed",
                "Confirm restore before replacing this installation",
            )
        }
        val manager = requireBackupManager()
        clearExpiredTickets()
        val ticket = tickets.remove(restoreTicket)
            ?: throw NativeOwnerBackupFailure(
                "restore_ticket_expired",
                "Inspect the backup again before restoring",
            )
        return try {
            if (ticket.sessionId != sessionHandle || ticket.expiresAtEpochMs <= nowEpochMs()) {
                throw NativeOwnerBackupFailure(
                    "restore_ticket_expired",
                    "Inspect the backup again before restoring",
                )
            }
            val actor = requireOwnerRestore(sessionHandle, recent = true, allowQuickPin = false)
            if (actor.userId != ticket.actorUserId) {
                throw NativeOwnerBackupFailure(
                    "restore_ticket_expired",
                    "Inspect the backup again before restoring",
                )
            }
            manager.restoreEncryptedReplacing(
                database,
                ticket.uri,
                BackupManager.RestoreSelection(
                    business = true,
                    planning = true,
                    preferences = true,
                    security = true,
                ),
                ticket.passphrase,
                security.sessionManager(),
            ).getOrElse(::throwRestoreFailure)
            auditRestore(actor.displayName, actor.role, ticket.inspection.meta.formatVersion)
            tickets.values.forEach { it.passphrase.fill('\u0000') }
            tickets.clear()
            NativeOwnerBackupRestoreResult(restored = true, sessionsInvalidated = true)
        } finally {
            ticket.passphrase.fill('\u0000')
        }
    }

    fun cancelRestoreTicket(restoreTicket: String?) {
        if (restoreTicket.isNullOrBlank()) return
        tickets.remove(restoreTicket)?.passphrase?.fill('\u0000')
    }

    suspend fun reauthenticate(
        sessionHandle: String,
        passphrase: CharArray,
    ) {
        try {
            requireOwnerExport(sessionHandle, recent = false)
            security.reauthenticate(sessionHandle, passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    suspend fun connectDrive(
        sessionHandle: String,
        recoveryPassphrase: CharArray,
        allowMetered: Boolean,
    ): NativeOwnerBackupSnapshot {
        val gateway = requireCloudGateway()
        return try {
            val actor = requireOwnerExport(sessionHandle, recent = true, allowQuickPin = false)
            gateway.connect(
                recoveryPassphrase = recoveryPassphrase,
                allowMetered = allowMetered,
                ownerUserId = actor.userId,
                ownerName = actor.displayName,
                ownerRole = actor.role,
            )
            audit(
                "connect_cloud_backup",
                actor.userId,
                actor.displayName,
                actor.role,
                "provider=google_drive",
            )
            snapshotFromSettings()
        } catch (error: Throwable) {
            throwCloudFailure(error)
        } finally {
            recoveryPassphrase.fill('\u0000')
        }
    }

    suspend fun setDrivePolicy(
        sessionHandle: String,
        enabled: Boolean,
        allowMetered: Boolean,
    ): NativeOwnerBackupSnapshot {
        val gateway = requireCloudGateway()
        return try {
            val actor = requireOwnerExport(sessionHandle, recent = true, allowQuickPin = true)
            gateway.updatePolicy(enabled = enabled, allowMetered = allowMetered)
            audit(
                "configure_cloud_backup",
                actor.userId,
                actor.displayName,
                actor.role,
                "enabled=$enabled;allow_metered=$allowMetered",
            )
            snapshotFromSettings()
        } catch (error: Throwable) {
            throwCloudFailure(error)
        }
    }

    suspend fun runDriveBackupNow(sessionHandle: String): NativeOwnerBackupSnapshot {
        val gateway = requireCloudGateway()
        return try {
            val actor = requireOwnerExport(sessionHandle, recent = false)
            gateway.runNow()
            audit(
                "request_cloud_backup",
                actor.userId,
                actor.displayName,
                actor.role,
                "provider=google_drive",
            )
            snapshotFromSettings()
        } catch (error: Throwable) {
            throwCloudFailure(error)
        }
    }

    suspend fun disconnectDrive(
        sessionHandle: String,
        confirmRevoke: Boolean,
    ): NativeOwnerBackupSnapshot {
        if (!confirmRevoke) {
            throw NativeOwnerBackupFailure(
                "validation_failed",
                "Confirm Google authorization revocation before disconnecting",
            )
        }
        val gateway = requireCloudGateway()
        return try {
            val actor = requireOwnerExport(sessionHandle, recent = true, allowQuickPin = false)
            gateway.disconnect(confirmRevoke = true)
            audit(
                "disconnect_cloud_backup",
                actor.userId,
                actor.displayName,
                actor.role,
                "remote_backups_retained=true",
            )
            snapshotFromSettings()
        } catch (error: Throwable) {
            throwCloudFailure(error)
        }
    }

    override fun close() {
        tickets.values.forEach { it.passphrase.fill('\u0000') }
        tickets.clear()
        expiryScope.cancel()
    }

    private fun snapshotFromSettings(now: Instant = Instant.now()): NativeOwnerBackupSnapshot {
        val state = cloudBackupSettings?.read()
        if (state == null) {
            return NativeOwnerBackupSnapshot(
                configured = false,
                enabled = false,
                accountDisplay = null,
                scheduleDescription = "Google Drive vault is not configured on this device.",
                lastSuccessAt = null,
                lastAttemptAt = null,
                lastErrorMessage = null,
                stale = false,
                pending = false,
                statusHeadline = "Backup not configured",
                allowMetered = false,
                cloudActionsAvailable = cloudGateway != null,
            )
        }
        val stale = isStale(state, now)
        val pending = state.pending != null
        return NativeOwnerBackupSnapshot(
            configured = true,
            enabled = state.enabled,
            accountDisplay = state.accountDisplay.takeIf { it.isNotBlank() },
            scheduleDescription = "Encrypted Google Drive vault",
            lastSuccessAt = state.lastSuccessAt,
            lastAttemptAt = state.lastAttemptAt,
            lastErrorMessage = state.lastErrorMessage,
            stale = stale,
            pending = pending,
            statusHeadline = statusHeadline(state, stale, pending),
            allowMetered = state.allowMetered,
            cloudActionsAvailable = cloudGateway != null,
        )
    }

    private suspend fun requireOwnerExport(
        sessionHandle: String,
        recent: Boolean,
        allowQuickPin: Boolean = false,
    ) = authorize(sessionHandle, AppPermission.CREATE_EXPORT_BACKUP, recent, allowQuickPin)

    private suspend fun requireOwnerRestore(
        sessionHandle: String,
        recent: Boolean,
        allowQuickPin: Boolean = false,
    ) = authorize(sessionHandle, AppPermission.RESTORE_BACKUP, recent, allowQuickPin)

    private suspend fun authorize(
        sessionHandle: String,
        permission: AppPermission,
        recent: Boolean,
        allowQuickPin: Boolean,
    ) = security.requirePermission(sessionHandle, permission).also { actor ->
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Backup is only available to Owner."
        }
        if (recent) {
            security.sessionManager().requireRecentAuthentication(
                sessionHandle,
                allowQuickPin = allowQuickPin,
            )
        }
    }

    private fun requireBackupManager(): BackupManager =
        backupManager
            ?: throw NativeOwnerBackupFailure(
                "not_configured",
                "Local backup is not available in this preview.",
            )

    private fun requireCloudGateway(): NativeOwnerCloudBackupGateway =
        cloudGateway
            ?: throw NativeOwnerBackupFailure(
                "not_configured",
                "Google Drive backup is not available in this preview.",
            )

    private fun isStale(state: CloudBackupState, now: Instant): Boolean {
        val protectedAt = state.lastSuccessAt ?: state.configuredAt
        return runCatching {
            Duration.between(Instant.parse(protectedAt), now) > STALE_AFTER
        }.getOrDefault(false)
    }

    private fun statusHeadline(
        state: CloudBackupState,
        stale: Boolean,
        pending: Boolean,
    ): String = when {
        !state.lastErrorMessage.isNullOrBlank() -> "Backup needs attention"
        stale -> "Recovery point is stale"
        pending -> "Backup pending upload"
        !state.enabled -> "Drive vault paused"
        state.lastSuccessAt != null -> "Drive vault protected"
        else -> "Drive vault configured"
    }

    private suspend fun audit(
        action: String,
        actorId: Long?,
        actorName: String,
        actorRole: String,
        details: String,
    ) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.ofEpochMilli(nowEpochMs()).toString(),
                action = action,
                result = "success",
                actorUserId = actorId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                details = details,
            ),
        )
    }

    private suspend fun auditRestore(actorName: String, actorRole: String, formatVersion: Int) {
        audit(
            ACTION_RESTORE_BACKUP,
            null,
            actorName,
            actorRole,
            "format_version=$formatVersion;sessions_invalidated=true",
        )
    }

    private fun clearExpiredTickets() {
        val now = nowEpochMs()
        tickets.entries.removeIf { entry ->
            val expired = entry.value.expiresAtEpochMs <= now
            if (expired) entry.value.passphrase.fill('\u0000')
            expired
        }
    }

    private fun throwBackupFailure(error: Throwable): Nothing = when {
        error is SecurityFailure.Validation -> throw error
        error is IOException -> throw NativeOwnerBackupFailure(
            "io_failed",
            "The selected backup file could not be read or written",
            retryable = true,
        )
        error.message?.contains("passphrase is incorrect or the file was altered", ignoreCase = true) == true ->
            throw NativeOwnerBackupFailure(
                "backup_wrong_passphrase",
                "The backup passphrase is incorrect, or the file is not authentic",
            )
        error.message?.contains("newer backup format", ignoreCase = true) == true ||
            error.message?.contains("newer database schema", ignoreCase = true) == true ||
            error.message?.contains("contains no application users", ignoreCase = true) == true ->
            throw NativeOwnerBackupFailure(
                "backup_incompatible",
                "This backup cannot be safely restored by this app version",
            )
        error is BackupManager.BackupFailure -> throw NativeOwnerBackupFailure(
            "conflict",
            error.message ?: "The restore could not replace the current state",
        )
        error is IllegalArgumentException || error is org.json.JSONException ->
            throw NativeOwnerBackupFailure(
                "backup_tampered",
                "The backup is damaged or has been altered",
            )
        else -> throw error
    }

    private fun throwRestoreFailure(error: Throwable): Nothing = when {
        error is IOException -> throw NativeOwnerBackupFailure(
            "io_failed",
            "The selected backup file could not be read or written",
            retryable = true,
        )
        error.message?.contains("newer", ignoreCase = true) == true ||
            error.message?.contains("format", ignoreCase = true) == true ||
            error.message?.contains("schema", ignoreCase = true) == true ->
            throw NativeOwnerBackupFailure(
                "backup_incompatible",
                "This backup cannot be safely restored by this app version",
            )
        else -> throw NativeOwnerBackupFailure(
            "conflict",
            "The inspected backup changed or the current installation could not be safely replaced",
        )
    }

    private fun throwCloudFailure(error: Throwable): Nothing = when (error) {
        is NativeOwnerBackupFailure -> throw error
        is SecurityFailure -> throw error
        is CloudBackupFailure.NotConfigured -> throw NativeOwnerBackupFailure(
            "not_configured",
            error.message ?: "Google Drive vault is not configured on this device.",
        )
        is CloudBackupFailure.AuthorizationRequired -> throw NativeOwnerBackupFailure(
            "authorization_required",
            error.message ?: "Choose a Google account to continue.",
        )
        is CloudBackupFailure.AccountMismatch -> throw NativeOwnerBackupFailure(
            "account_mismatch",
            error.message ?: "Google sign-in and Drive authorization selected different accounts.",
        )
        is CloudBackupFailure -> throw NativeOwnerBackupFailure(
            "cloud_failed",
            error.message ?: "Google Drive backup could not complete.",
            retryable = true,
        )
        is IllegalStateException, is IllegalArgumentException -> throw NativeOwnerBackupFailure(
            "conflict",
            error.message ?: "Google Drive backup could not complete.",
        )
        else -> throw error
    }

    companion object {
        const val ACTION_EXPORT_BACKUP = "export_backup"
        const val ACTION_RESTORE_BACKUP = "restore_backup"
        const val TICKET_LIFETIME_MS = 5 * 60 * 1000L
        private val STALE_AFTER = Duration.ofHours(48)
    }
}

class NativeOwnerBackupFailure(
    val code: String,
    message: String,
    val retryable: Boolean = false,
) : IllegalArgumentException(message)
