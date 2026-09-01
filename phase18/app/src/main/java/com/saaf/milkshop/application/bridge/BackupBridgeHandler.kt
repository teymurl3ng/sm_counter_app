package com.saaf.milkshop.application.bridge

import android.net.Uri
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.UserSecurityService
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface BackupDocumentPicker {
    suspend fun createDocument(suggestedFileName: String?): Uri?
    suspend fun openDocument(): Uri?
}

internal class BackupBridgeHandler(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val manager: BackupManager,
    private val documents: BackupDocumentPicker,
    private val onSessionChanged: (String?) -> Unit,
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

    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.EXPORT_ENCRYPTED_BACKUP -> export(request)
        HybridBridgeActions.INSPECT_ENCRYPTED_BACKUP -> inspect(request)
        HybridBridgeActions.RESTORE_INSPECTED_BACKUP -> restore(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun export(request: BridgeRequest): JSONObject {
        strictObject(request.payload, required = setOf("passphrase"), allowed = setOf("passphrase", "suggestedFileName"))
        val session = request.requireSession()
        val actor = authorize(session, AppPermission.CREATE_EXPORT_BACKUP)
        val passphrase = request.payload.requiredString("passphrase").toCharArray()
        return try {
            val suggested = request.payload.optionalString("suggestedFileName")?.trim()?.takeIf(String::isNotEmpty)?.also(::validateFileName)
            val uri = documents.createDocument(suggested) ?: throw BackupBridgeFailure("user_cancelled", "Backup creation was cancelled")
            authorize(session, AppPermission.CREATE_EXPORT_BACKUP)
            val meta = manager.exportEncryptedSnapshot(database, uri, passphrase).getOrElse(::throwBackupFailure)
            audit(ACTION_EXPORT_BACKUP, actor.userId, actor.displayName, actor.role, "format_version=${meta.formatVersion}")
            backupExportJson(meta)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun inspect(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("passphrase"))
        val session = request.requireSession()
        authorize(session, AppPermission.RESTORE_BACKUP)
        val passphrase = request.payload.requiredString("passphrase").toCharArray()
        var retained = false
        return try {
            val uri = documents.openDocument() ?: throw BackupBridgeFailure("user_cancelled", "Backup selection was cancelled")
            val actor = authorize(session, AppPermission.RESTORE_BACKUP)
            val inspection = manager.inspectEncryptedSnapshot(uri, passphrase).getOrElse(::throwBackupFailure)
            if (!inspection.containsSecurityState) throw BackupBridgeFailure("backup_incompatible", "This encrypted backup contains no application users")
            clearExpiredTickets()
            tickets.entries.removeIf { entry ->
                val remove = entry.value.sessionId == session
                if (remove) entry.value.passphrase.fill('\u0000')
                remove
            }
            val token = UUID.randomUUID().toString()
            tickets[token] = RestoreTicket(token, session, actor.userId, uri, passphrase, inspection, nowEpochMs() + TICKET_LIFETIME_MS)
            expiryScope.launch {
                delay(TICKET_LIFETIME_MS)
                tickets.remove(token)?.passphrase?.fill('\u0000')
            }
            retained = true
            backupInspectionJson(token, inspection)
        } finally {
            if (!retained) passphrase.fill('\u0000')
        }
    }

    private suspend fun restore(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("restoreTicket", "confirmRestore"))
        if (!request.payload.requiredBoolean("confirmRestore")) {
            throw BridgeProtocolFailure("validation_failed", "Confirm restore before replacing this installation", "confirmRestore")
        }
        val session = request.requireSession()
        val token = request.payload.requiredString("restoreTicket")
        clearExpiredTickets()
        val ticket = tickets.remove(token) ?: throw BackupBridgeFailure("restore_ticket_expired", "Inspect the backup again before restoring")
        return try {
            if (ticket.sessionId != session || ticket.expiresAtEpochMs <= nowEpochMs()) {
                throw BackupBridgeFailure("restore_ticket_expired", "Inspect the backup again before restoring")
            }
            val actor = authorize(session, AppPermission.RESTORE_BACKUP)
            if (actor.userId != ticket.actorUserId) throw BackupBridgeFailure("restore_ticket_expired", "Inspect the backup again before restoring")
            manager.restoreEncryptedReplacing(
                database,
                ticket.uri,
                BackupManager.RestoreSelection(business = true, planning = true, preferences = true, security = true),
                ticket.passphrase,
                security.sessionManager(),
            ).getOrElse(::throwRestoreFailure)
            auditRestore(actor.displayName, actor.role, ticket.inspection.meta.formatVersion)
            tickets.values.forEach { it.passphrase.fill('\u0000') }
            tickets.clear()
            onSessionChanged(null)
            JSONObject().put("restored", true).put("sessionsInvalidated", true)
        } finally {
            ticket.passphrase.fill('\u0000')
        }
    }

    private suspend fun authorize(session: String, permission: AppPermission) =
        security.requirePermission(session, permission).also { security.sessionManager().requireRecentAuthentication(session) }

    private suspend fun audit(action: String, actorId: Long, actorName: String, actorRole: String, details: String) {
        database.securityAuditDao().insert(SecurityAuditEntryEntity(
            occurredAt = Instant.ofEpochMilli(nowEpochMs()).toString(), action = action, result = "success",
            actorUserId = actorId, actorNameSnapshot = actorName, actorRoleSnapshot = actorRole, details = details,
        ))
    }

    private suspend fun auditRestore(actorName: String, actorRole: String, formatVersion: Int) {
        database.securityAuditDao().insert(SecurityAuditEntryEntity(
            occurredAt = Instant.ofEpochMilli(nowEpochMs()).toString(), action = ACTION_RESTORE_BACKUP, result = "success",
            actorUserId = null, actorNameSnapshot = actorName, actorRoleSnapshot = actorRole,
            details = "format_version=$formatVersion;sessions_invalidated=true",
        ))
    }

    private fun clearExpiredTickets() {
        val now = nowEpochMs()
        tickets.entries.removeIf { entry ->
            val expired = entry.value.expiresAtEpochMs <= now
            if (expired) entry.value.passphrase.fill('\u0000')
            expired
        }
    }

    private fun validateFileName(value: String) {
        if (value.length > 120 || value.any { it == '/' || it == '\\' || it.code < 32 }) {
            throw BridgeProtocolFailure("validation_failed", "Choose a simple backup file name", "suggestedFileName")
        }
    }

    private fun throwBackupFailure(error: Throwable): Nothing = when {
        error is com.saaf.milkshop.domain.SecurityFailure.Validation -> throw error
        error is IOException -> throw BackupBridgeFailure("io_failed", "The selected backup file could not be read or written", retryable = true)
        error.message?.contains("passphrase is incorrect or the file was altered", ignoreCase = true) == true ->
            throw BackupBridgeFailure("backup_wrong_passphrase", "The backup passphrase is incorrect, or the file is not authentic")
        error.message?.contains("newer backup format", ignoreCase = true) == true || error.message?.contains("newer database schema", ignoreCase = true) == true ||
            error.message?.contains("contains no application users", ignoreCase = true) == true ->
            throw BackupBridgeFailure("backup_incompatible", "This backup cannot be safely restored by this app version")
        error is BackupManager.BackupFailure -> throw BackupBridgeFailure("conflict", error.message ?: "The restore could not replace the current state")
        error is IllegalArgumentException || error is org.json.JSONException -> throw BackupBridgeFailure("backup_tampered", "The backup is damaged or has been altered")
        else -> throw error
    }

    private fun throwRestoreFailure(error: Throwable): Nothing = when {
        error is IOException -> throw BackupBridgeFailure("io_failed", "The selected backup file could not be read or written", retryable = true)
        error.message?.contains("newer", ignoreCase = true) == true || error.message?.contains("format", ignoreCase = true) == true ||
            error.message?.contains("schema", ignoreCase = true) == true ->
            throw BackupBridgeFailure("backup_incompatible", "This backup cannot be safely restored by this app version")
        else -> throw BackupBridgeFailure("conflict", "The inspected backup changed or the current installation could not be safely replaced")
    }

    private fun backupExportJson(meta: BackupManager.BackupMeta) = JSONObject()
        .put("displayName", meta.fileName).put("createdAt", meta.createdAt).put("formatVersion", meta.formatVersion)

    private fun backupInspectionJson(token: String, inspection: BackupManager.EncryptedBackupInspection) = JSONObject()
        .put("restoreTicket", token).put("createdAt", inspection.meta.createdAt)
        .put("formatVersion", inspection.meta.formatVersion).put("containsSecurityState", inspection.containsSecurityState)
        .put("requiresEmptyInstallation", false)

    override fun close() {
        tickets.values.forEach { it.passphrase.fill('\u0000') }
        tickets.clear()
        expiryScope.cancel()
    }

    companion object {
        const val ACTION_EXPORT_BACKUP = "export_backup"
        const val ACTION_RESTORE_BACKUP = "restore_backup"
        const val TICKET_LIFETIME_MS = 5 * 60 * 1000L
    }
}

internal class BackupBridgeFailure(
    val code: String,
    message: String,
    val retryable: Boolean = false,
) : IllegalArgumentException(message)
