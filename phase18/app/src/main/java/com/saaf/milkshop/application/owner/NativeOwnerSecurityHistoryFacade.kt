package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import java.util.Locale

data class NativeOwnerSecurityAuditRow(
    val auditId: Long,
    val occurredAt: String,
    val actionCode: String,
    val resultCode: String,
    val actorLabel: String,
    val detailsPreview: String?,
)

data class NativeOwnerSecurityAuditDetail(
    val row: NativeOwnerSecurityAuditRow,
    val reasonCode: String?,
    val targetUserId: Long?,
    val details: String?,
)

data class NativeOwnerSecurityHistorySnapshot(
    val audits: List<NativeOwnerSecurityAuditRow>,
)

/**
 * Owner Security history boundary: read [SecurityAuditDao.all] entries for
 * Owner audit browsing. Does not invent filters or call bridge JSON.
 */
class NativeOwnerSecurityHistoryFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun loadAudits(sessionHandle: String): NativeOwnerSecurityHistorySnapshot {
        requireOwnerSecurityAudit(sessionHandle)
        val rows = database.securityAuditDao().all()
            .sortedByDescending { it.auditId }
            .map { it.toRow() }
        return NativeOwnerSecurityHistorySnapshot(audits = rows)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        auditId: Long,
    ): NativeOwnerSecurityAuditDetail {
        requireOwnerSecurityAudit(sessionHandle)
        val entry = database.securityAuditDao().all().firstOrNull { it.auditId == auditId }
            ?: error("This security audit entry is not in the history list.")
        val row = entry.toRow()
        return NativeOwnerSecurityAuditDetail(
            row = row,
            reasonCode = entry.reasonCode,
            targetUserId = entry.targetUserId,
            details = entry.details,
        )
    }

    private suspend fun requireOwnerSecurityAudit(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_SECURITY_AUDIT)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Security history is only available to Owner."
        }
    }

    private fun SecurityAuditEntryEntity.toRow(): NativeOwnerSecurityAuditRow {
        val actorName = actorNameSnapshot?.takeIf { it.isNotBlank() }
        val actorRole = actorRoleSnapshot?.takeIf { it.isNotBlank() }?.let(::humanizeCode)
        val actorLabel = when {
            actorName != null && actorRole != null -> "$actorName · $actorRole"
            actorName != null -> actorName
            actorRole != null -> actorRole
            else -> "Unknown actor"
        }
        val preview = details?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
            if (text.length <= 80) text else text.take(77).trimEnd() + "…"
        }
        return NativeOwnerSecurityAuditRow(
            auditId = auditId,
            occurredAt = occurredAt,
            actionCode = action,
            resultCode = result,
            actorLabel = actorLabel,
            detailsPreview = preview,
        )
    }

    private fun humanizeCode(code: String): String =
        code.replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { it.titlecase(Locale.US) }
            }
            .ifBlank { code }
}
