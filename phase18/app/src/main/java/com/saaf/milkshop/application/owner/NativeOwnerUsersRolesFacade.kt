package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import java.util.Locale

data class NativeOwnerUserRow(
    val userId: Long,
    val loginName: String,
    val displayName: String,
    val roleLabel: String,
    val statusLabel: String,
    val setupRequired: Boolean,
)

data class NativeOwnerUserDetail(
    val row: NativeOwnerUserRow,
    val createdAt: String,
    val updatedAt: String,
    val disabledAt: String?,
    val employeeAccountIdLabel: String,
)

data class NativeOwnerUsersRolesSnapshot(
    val users: List<NativeOwnerUserRow>,
)

/**
 * Owner Users & roles boundary: staff login accounts from
 * [UserSecurityService.listUsers]. Distinct from Employees salary accounts.
 * Does not invent enrollment/disable write flows or call bridge JSON.
 */
class NativeOwnerUsersRolesFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun loadUsers(sessionHandle: String): NativeOwnerUsersRolesSnapshot {
        requireOwnerStaffUsers(sessionHandle)
        val rows = security.listUsers(sessionHandle)
            .sortedWith(compareBy({ it.displayName.lowercase(Locale.US) }, { it.userId }))
            .map { it.toRow() }
        return NativeOwnerUsersRolesSnapshot(users = rows)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        userId: Long,
    ): NativeOwnerUserDetail {
        requireOwnerStaffUsers(sessionHandle)
        val user = security.listUsers(sessionHandle).firstOrNull { it.userId == userId }
            ?: error("This user is not in the Users & roles list.")
        return NativeOwnerUserDetail(
            row = user.toRow(),
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            disabledAt = user.disabledAt,
            employeeAccountIdLabel = user.employeeAccountId?.toString() ?: "Not linked",
        )
    }

    private suspend fun requireOwnerStaffUsers(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_STAFF_USERS)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Users & roles is only available to Owner."
        }
    }

    private fun AppUserEntity.toRow(): NativeOwnerUserRow = NativeOwnerUserRow(
        userId = userId,
        loginName = loginName,
        displayName = displayName,
        roleLabel = humanizeCode(role),
        statusLabel = humanizeCode(status),
        setupRequired = credentialSetupRequired,
    )

    private fun humanizeCode(code: String): String =
        code.replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { it.titlecase(Locale.US) }
            }
            .ifBlank { code }
}
