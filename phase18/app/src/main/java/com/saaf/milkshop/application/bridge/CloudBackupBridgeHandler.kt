package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.cloud.CloudBackupCoordinator
import com.saaf.milkshop.cloud.CloudBackupFailure
import com.saaf.milkshop.cloud.CloudBackupStatus
import com.saaf.milkshop.cloud.FixedCloudAccessTokenSource
import com.saaf.milkshop.cloud.GoogleAuthorizationTokenSource
import com.saaf.milkshop.cloud.InteractiveCloudAuthorization
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.SecurityFailure
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class CloudBackupBridgeHandler(
    private val app: MilkShopApplication,
    private val authorization: InteractiveCloudAuthorization,
    private val onSessionChanged: (String?) -> Unit,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    private data class RestoreTicket(
        val token: String,
        val sessionId: String,
        val actorUserId: Long,
        val remoteFileId: String,
        val passphrase: CharArray,
        val inspection: BackupManager.EncryptedBackupInspection,
        val expiresAtEpochMs: Long,
    )

    private val tickets = ConcurrentHashMap<String, RestoreTicket>()
    private val expiryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.CLOUD_BACKUP_STATUS -> status(request)
        HybridBridgeActions.CONNECT_CLOUD_BACKUP -> connect(request)
        HybridBridgeActions.CONFIGURE_CLOUD_BACKUP -> configure(request)
        HybridBridgeActions.RUN_CLOUD_BACKUP_NOW -> runNow(request)
        HybridBridgeActions.REFRESH_CLOUD_BACKUPS -> refresh(request)
        HybridBridgeActions.INSPECT_CLOUD_BACKUP -> inspect(request)
        HybridBridgeActions.RESTORE_INSPECTED_CLOUD_BACKUP -> restore(request)
        HybridBridgeActions.DISCONNECT_CLOUD_BACKUP -> disconnect(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun status(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        authorizeOwner(request.requireSession(), AppPermission.CREATE_EXPORT_BACKUP, recent = false)
        return statusJson(localCoordinator().status())
    }

    private suspend fun connect(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("allowMetered"))
        val actor = authorizeOwner(request.requireSession(), AppPermission.CREATE_EXPORT_BACKUP, recent = true)
        var passphrase: CharArray? = null
        return try {
            val recoveryPassphrase = authorization.requestNewRecoveryPassphrase().also { passphrase = it }
            val authorizationResult = authorization.connect()
            authorizeOwner(request.requireSession(), AppPermission.CREATE_EXPORT_BACKUP, recent = true)
            val coordinator = coordinator(FixedCloudAccessTokenSource(authorizationResult.accessToken))
            val driveIdentity = coordinatorAccount(authorizationResult.accessToken)
            if (driveIdentity.displayEmail.isNotBlank() && !driveIdentity.displayEmail.equals(authorizationResult.selectedEmail, ignoreCase = true)) {
                throw CloudBackupFailure.AccountMismatch("Google sign-in and Drive authorization selected different accounts")
            }
            val identity = driveIdentity.copy(displayEmail = authorizationResult.selectedEmail)
            val state = coordinator.configure(
                account = identity,
                recoveryPassphrase = recoveryPassphrase,
                allowMetered = request.payload.requiredBoolean("allowMetered"),
                ownerUserId = actor.userId,
                ownerName = actor.displayName,
                ownerRole = actor.role,
            )
            app.cloudBackupScheduler.apply(state.enabled, state.allowMetered)
            app.cloudBackupScheduler.runNow()
            authorization.requestNotificationPermission()
            audit("connect_cloud_backup", actor.userId, actor.displayName, actor.role, "provider=google_drive")
            statusJson(coordinator.status())
        } catch (error: Throwable) {
            throwCloudFailure(error)
        } finally {
            passphrase?.fill('\u0000')
        }
    }

    private suspend fun configure(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("enabled", "allowMetered"))
        val actor = authorizeOwner(request.requireSession(), AppPermission.CREATE_EXPORT_BACKUP, recent = true)
        return try {
            val state = localCoordinator().updatePolicy(
                enabled = request.payload.requiredBoolean("enabled"),
                allowMetered = request.payload.requiredBoolean("allowMetered"),
            )
            app.cloudBackupScheduler.apply(state.enabled, state.allowMetered)
            audit("configure_cloud_backup", actor.userId, actor.displayName, actor.role, "enabled=${state.enabled};allow_metered=${state.allowMetered}")
            statusJson(localCoordinator().status())
        } catch (error: Throwable) {
            throwCloudFailure(error)
        }
    }

    private suspend fun runNow(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        val actor = authorizeOwner(request.requireSession(), AppPermission.CREATE_EXPORT_BACKUP, recent = false)
        val state = app.cloudBackupSettings.read() ?: throwCloudFailure(CloudBackupFailure.NotConfigured())
        if (!state.enabled) throw BridgeProtocolFailure("conflict", "Enable automatic cloud backup before running it now")
        app.cloudBackupScheduler.runNow()
        audit("request_cloud_backup", actor.userId, actor.displayName, actor.role, "provider=google_drive")
        return JSONObject().put("scheduled", true)
    }

    private suspend fun refresh(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        authorizeOwner(request.requireSession(), AppPermission.CREATE_EXPORT_BACKUP, recent = false)
        return try {
            val coordinator = coordinator(GoogleAuthorizationTokenSource(app))
            coordinator.refreshSnapshots()
            statusJson(coordinator.status())
        } catch (error: Throwable) {
            throwCloudFailure(error)
        }
    }

    private suspend fun inspect(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("remoteFileId"))
        val session = request.requireSession()
        val actor = authorizeOwner(session, AppPermission.RESTORE_BACKUP, recent = true)
        val remoteFileId = request.payload.requiredString("remoteFileId")
        var passphrase: CharArray? = null
        var retained = false
        return try {
            val recoveryPassphrase = authorization.requestRecoveryPassphrase().also { passphrase = it }
            val decrypted = coordinator(GoogleAuthorizationTokenSource(app)).downloadAndDecrypt(remoteFileId, recoveryPassphrase)
            val inspection = try {
                app.backupManager.inspectCompleteSnapshotBytes(decrypted.payload, "Google Drive cloud backup")
            } finally {
                decrypted.payload.fill(0)
            }
            if (!inspection.containsSecurityState) throw CloudBackupFailure.Remote("This cloud backup contains no application users")
            clearExpiredTickets()
            tickets.entries.removeIf { entry ->
                val remove = entry.value.sessionId == session
                if (remove) entry.value.passphrase.fill('\u0000')
                remove
            }
            val token = UUID.randomUUID().toString()
            tickets[token] = RestoreTicket(
                token = token,
                sessionId = session,
                actorUserId = actor.userId,
                remoteFileId = remoteFileId,
                passphrase = recoveryPassphrase,
                inspection = inspection,
                expiresAtEpochMs = nowEpochMs() + TICKET_LIFETIME_MS,
            )
            expiryScope.launch {
                delay(TICKET_LIFETIME_MS)
                tickets.remove(token)?.passphrase?.fill('\u0000')
            }
            retained = true
            backupInspectionJson(token, inspection)
        } catch (error: Throwable) {
            throwCloudFailure(error)
        } finally {
            if (!retained) passphrase?.fill('\u0000')
        }
    }

    private suspend fun restore(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("restoreTicket", "confirmRestore"))
        if (!request.payload.requiredBoolean("confirmRestore")) {
            throw BridgeProtocolFailure("validation_failed", "Confirm restore before replacing this installation", "confirmRestore")
        }
        val session = request.requireSession()
        clearExpiredTickets()
        val ticket = tickets.remove(request.payload.requiredString("restoreTicket"))
            ?: throw BackupBridgeFailure("restore_ticket_expired", "Inspect the cloud backup again before restoring")
        return try {
            if (ticket.sessionId != session || ticket.expiresAtEpochMs <= nowEpochMs()) {
                throw BackupBridgeFailure("restore_ticket_expired", "Inspect the cloud backup again before restoring")
            }
            val actor = authorizeOwner(session, AppPermission.RESTORE_BACKUP, recent = true)
            if (actor.userId != ticket.actorUserId) {
                throw BackupBridgeFailure("restore_ticket_expired", "Inspect the cloud backup again before restoring")
            }
            val decrypted = coordinator(GoogleAuthorizationTokenSource(app)).downloadAndDecrypt(ticket.remoteFileId, ticket.passphrase)
            try {
                app.backupManager.restoreCompleteSnapshotReplacing(
                    database = app.database,
                    bytes = decrypted.payload,
                    sessions = app.nativeSessions,
                    fileName = ticket.inspection.meta.fileName,
                ).getOrElse { throw it }
            } finally {
                decrypted.payload.fill(0)
            }
            localCoordinator().disableAfterRestore()
            app.cloudBackupScheduler.cancelAll()
            audit("restore_cloud_backup", null, actor.displayName, actor.role, "sessions_invalidated=true")
            tickets.values.forEach { it.passphrase.fill('\u0000') }
            tickets.clear()
            onSessionChanged(null)
            JSONObject().put("restored", true).put("sessionsInvalidated", true)
        } catch (error: Throwable) {
            throwCloudFailure(error)
        } finally {
            ticket.passphrase.fill('\u0000')
        }
    }

    private suspend fun disconnect(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("confirmRevoke"))
        if (!request.payload.requiredBoolean("confirmRevoke")) {
            throw BridgeProtocolFailure("validation_failed", "Confirm Google authorization revocation before disconnecting", "confirmRevoke")
        }
        val actor = authorizeOwner(request.requireSession(), AppPermission.CREATE_EXPORT_BACKUP, recent = true)
        val state = app.cloudBackupSettings.read() ?: throwCloudFailure(CloudBackupFailure.NotConfigured())
        return try {
            authorization.revokeSelectedAccount(state.accountPermissionId)
            app.cloudBackupScheduler.cancelAll()
            localCoordinator().disconnectLocal()
            audit("disconnect_cloud_backup", actor.userId, actor.displayName, actor.role, "remote_backups_retained=true")
            JSONObject()
        } catch (error: Throwable) {
            throwCloudFailure(error)
        }
    }

    private fun coordinator(tokens: com.saaf.milkshop.cloud.CloudAccessTokenSource) = app.cloudBackupCoordinator(tokens)

    private fun localCoordinator(): CloudBackupCoordinator = coordinator(object : com.saaf.milkshop.cloud.CloudAccessTokenSource {
        override suspend fun accessToken(): String = throw CloudBackupFailure.AuthorizationRequired()
    })

    private suspend fun coordinatorAccount(token: String): com.saaf.milkshop.cloud.CloudAccountIdentity =
        com.saaf.milkshop.cloud.GoogleDriveCloudBackupProvider(FixedCloudAccessTokenSource(token)).accountIdentity()

    private suspend fun authorizeOwner(session: String, permission: AppPermission, recent: Boolean) =
        app.userSecurity.requirePermission(session, permission).also { actor ->
            if (actor.role != "owner") throw SecurityFailure.PermissionDenied()
            if (recent) app.nativeSessions.requireRecentAuthentication(session)
        }

    private suspend fun audit(
        action: String,
        actorId: Long?,
        actorName: String,
        actorRole: String,
        details: String,
    ) {
        app.database.securityAuditDao().insert(
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

    private fun statusJson(status: CloudBackupStatus) = JSONObject()
        .put("configured", status.configured)
        .put("enabled", status.enabled)
        .put("provider", status.provider)
        .putNullable("accountDisplay", status.accountDisplay)
        .put("allowMetered", status.allowMetered)
        .put("scheduleDescription", status.scheduleDescription)
        .putNullable("lastAttemptAt", status.lastAttemptAt)
        .putNullable("lastSuccessAt", status.lastSuccessAt)
        .putNullable("lastErrorCode", status.lastErrorCode)
        .putNullable("lastErrorMessage", status.lastErrorMessage)
        .put("stale", status.stale)
        .put("pending", status.pending)
        .put("snapshots", JSONArray().apply {
            status.snapshots.forEach { snapshot ->
                put(
                    JSONObject()
                        .put("remoteFileId", snapshot.remoteFileId)
                        .put("snapshotId", snapshot.snapshotId)
                        .put("createdAt", snapshot.createdAt)
                        .put("sizeBytes", snapshot.sizeBytes.toString()),
                )
            }
        })

    private fun backupInspectionJson(token: String, inspection: BackupManager.EncryptedBackupInspection) = JSONObject()
        .put("restoreTicket", token)
        .put("createdAt", inspection.meta.createdAt)
        .put("formatVersion", inspection.meta.formatVersion)
        .put("containsSecurityState", inspection.containsSecurityState)
        .put("requiresEmptyInstallation", false)

    private fun clearExpiredTickets() {
        val now = nowEpochMs()
        tickets.entries.removeIf { entry ->
            val expired = entry.value.expiresAtEpochMs <= now
            if (expired) entry.value.passphrase.fill('\u0000')
            expired
        }
    }

    private fun throwCloudFailure(error: Throwable): Nothing = when (error) {
        is BackupBridgeFailure -> throw error
        is SecurityFailure -> throw error
        is CloudBackupFailure.WrongPassphraseOrTampered ->
            throw BackupBridgeFailure("backup_wrong_passphrase", error.message ?: "The cloud recovery passphrase is incorrect")
        is CloudBackupFailure.UserCancelled ->
            throw BackupBridgeFailure("user_cancelled", error.message ?: "Cloud recovery entry was cancelled")
        is CloudBackupFailure.AuthorizationRequired ->
            throw BackupBridgeFailure("cloud_authorization_required", error.message ?: "Google Drive authorization is required")
        is CloudBackupFailure.AccountMismatch ->
            throw BackupBridgeFailure("cloud_account_mismatch", error.message ?: "The wrong Google account is active")
        is CloudBackupFailure.Quota ->
            throw BackupBridgeFailure("cloud_quota", error.message ?: "Google Drive storage is full")
        is CloudBackupFailure.NotConfigured ->
            throw BackupBridgeFailure("cloud_not_configured", error.message ?: "Cloud backup is not configured")
        is CloudBackupFailure.Network ->
            throw BackupBridgeFailure("io_failed", error.message ?: "Google Drive could not be reached", retryable = true)
        is CloudBackupFailure.KeyUnavailable ->
            throw BackupBridgeFailure("conflict", error.message ?: "The cloud-backup device key is unavailable")
        is CloudBackupFailure.Remote ->
            throw BackupBridgeFailure("io_failed", error.message ?: "Google Drive could not complete the request")
        is BackupManager.BackupFailure ->
            throw BackupBridgeFailure("conflict", error.message ?: "The cloud restore could not replace the current state")
        is IllegalArgumentException, is org.json.JSONException ->
            throw BackupBridgeFailure("backup_tampered", error.message ?: "The cloud backup is damaged or has been altered")
        else -> throw error
    }

    override fun close() {
        tickets.values.forEach { it.passphrase.fill('\u0000') }
        tickets.clear()
        expiryScope.cancel()
        (authorization as? AutoCloseable)?.close()
    }

    companion object {
        const val TICKET_LIFETIME_MS = 5 * 60 * 1000L
    }
}
