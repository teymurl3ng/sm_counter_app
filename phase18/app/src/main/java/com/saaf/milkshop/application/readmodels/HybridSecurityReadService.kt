package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.UserSecurityService

internal class HybridSecurityReadService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun users(sessionHandle: String): List<UserAdministrationDto> =
        security.listUsers(sessionHandle).map { it.toAdministrationDto() }

    suspend fun securityAudit(sessionHandle: String): List<SecurityAuditDto> {
        security.requirePermission(sessionHandle, AppPermission.VIEW_SECURITY_AUDIT)
        return database.securityAuditDao().all().map {
            SecurityAuditDto(
                auditId = it.auditId,
                occurredAt = it.occurredAt,
                actionCode = it.action,
                resultCode = it.result,
                reasonCode = it.reasonCode,
                actor = AuditActorDto(it.actorUserId, it.actorNameSnapshot, it.actorRoleSnapshot),
                targetUserId = it.targetUserId,
                details = it.details,
            )
        }
    }
}
