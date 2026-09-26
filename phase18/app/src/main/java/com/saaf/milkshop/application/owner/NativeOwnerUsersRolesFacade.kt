package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.PendingUserEnrollment
import com.saaf.milkshop.domain.UserSecurityService
import java.util.Locale

data class NativeOwnerUserRow(
    val userId: Long,
    val loginName: String,
    val displayName: String,
    val roleWire: String,
    val roleLabel: String,
    val statusLabel: String,
    val enabled: Boolean,
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

data class NativeOwnerPendingEnrollment(
    val userId: Long,
    val displayName: String,
    val loginName: String,
    val enrollmentToken: String,
)

/**
 * Owner Users & roles boundary: staff login accounts from
 * [UserSecurityService.listUsers] plus enroll / enable / role-change /
 * credential-reset writes. Distinct from Employees salary accounts.
 * Never calls bridge JSON.
 */
class NativeOwnerUsersRolesFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    /**
     * Roles the signed-in actor may enroll / assign.
     * Owner: all roles. Admin: Manager / Cashier / Operations only (Owner+Admin remain Owner-only).
     */
    suspend fun enrollableRoles(sessionHandle: String): List<AppRole> {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_STAFF_USERS)
        return when (AppRole.parse(actor.role)) {
            AppRole.OWNER -> AppRole.entries.toList()
            AppRole.ADMIN -> listOf(AppRole.MANAGER, AppRole.CASHIER, AppRole.OPERATOR)
            else -> emptyList()
        }
    }

    suspend fun loadUsers(sessionHandle: String): NativeOwnerUsersRolesSnapshot {
        requireStaffUsersAccess(sessionHandle)
        val rows = security.listUsers(sessionHandle)
            .sortedWith(compareBy({ it.displayName.lowercase(Locale.US) }, { it.userId }))
            .map { it.toRow() }
        return NativeOwnerUsersRolesSnapshot(users = rows)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        userId: Long,
    ): NativeOwnerUserDetail {
        requireStaffUsersAccess(sessionHandle)
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

    suspend fun beginEnroll(
        sessionHandle: String,
        input: CreateUserInput,
    ): NativeOwnerPendingEnrollment {
        requireStaffUsersAccess(sessionHandle)
        val allowed = enrollableRoles(sessionHandle).map { it.wireValue }.toSet()
        require(input.role.trim().lowercase() in allowed) {
            "Administrator cannot enroll Owner or Administrator accounts."
        }
        val pending = security.beginUserEnrollment(sessionHandle, input)
        return pending.toNative()
    }

    suspend fun completeEnroll(
        enrollmentToken: String,
        passphrase: CharArray,
    ): NativeOwnerUserRow {
        try {
            val completed = security.completeUserEnrollment(enrollmentToken, passphrase)
            return completed.toRow()
        } finally {
            passphrase.fill('\u0000')
        }
    }

    suspend fun setEnabled(
        sessionHandle: String,
        userId: Long,
        enabled: Boolean,
    ) {
        requireStaffUsersAccess(sessionHandle)
        security.setUserEnabled(sessionHandle, userId, enabled)
    }

    suspend fun changeRole(
        sessionHandle: String,
        userId: Long,
        newRoleValue: String,
    ) {
        requireStaffUsersAccess(sessionHandle)
        val allowed = enrollableRoles(sessionHandle).map { it.wireValue }.toSet()
        require(newRoleValue.trim().lowercase() in allowed) {
            "Administrator cannot assign Owner or Administrator roles."
        }
        security.changeRole(sessionHandle, userId, newRoleValue)
    }

    /**
     * Begin credential reset for [targetUserId]. Requires recent auth (domain).
     * Complete via [completeEnroll] with the returned enrollment token.
     */
    suspend fun beginCredentialReset(
        sessionHandle: String,
        targetUserId: Long,
    ): NativeOwnerPendingEnrollment {
        requireStaffUsersAccess(sessionHandle)
        val detail = loadDetail(sessionHandle, targetUserId)
        val allowed = enrollableRoles(sessionHandle).map { it.wireValue }.toSet()
        require(detail.row.roleWire.trim().lowercase() in allowed) {
            "Administrator cannot reset Owner or Administrator credentials."
        }
        return security.beginCredentialReset(sessionHandle, targetUserId).toNative()
    }

    /**
     * Re-open credential setup for a user still waiting for it. Requires
     * recent auth (domain). Complete via [completeEnroll] with the returned
     * enrollment token.
     */
    suspend fun resumeCredentialSetup(
        sessionHandle: String,
        targetUserId: Long,
    ): NativeOwnerPendingEnrollment {
        requireStaffUsersAccess(sessionHandle)
        val detail = loadDetail(sessionHandle, targetUserId)
        val allowed = enrollableRoles(sessionHandle).map { it.wireValue }.toSet()
        require(detail.row.roleWire.trim().lowercase() in allowed) {
            "Administrator cannot manage Owner or Administrator credentials."
        }
        return security.resumeUserEnrollment(sessionHandle, targetUserId).toNative()
    }

    /**
     * Full passphrase re-authentication for sensitive Users & roles writes.
     * Wipes [passphrase] before returning.
     */
    suspend fun reauthenticate(
        sessionHandle: String,
        passphrase: CharArray,
    ) {
        try {
            requireStaffUsersAccess(sessionHandle)
            security.reauthenticate(sessionHandle, passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun requireStaffUsersAccess(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_STAFF_USERS)
        val role = AppRole.parse(actor.role)
        // Admin reuses staff-access list/detail under Admin People (no Owner/Admin enroll).
        require(role == AppRole.OWNER || role == AppRole.ADMIN) {
            "Users & roles is only available to Owner or Administrator."
        }
    }

    private fun PendingUserEnrollment.toNative(): NativeOwnerPendingEnrollment =
        NativeOwnerPendingEnrollment(
            userId = user.userId,
            displayName = user.displayName,
            loginName = user.loginName,
            enrollmentToken = enrollmentToken,
        )

    private fun AppUserEntity.toRow(): NativeOwnerUserRow = NativeOwnerUserRow(
        userId = userId,
        loginName = loginName,
        displayName = displayName,
        roleWire = role,
        roleLabel = humanizeCode(role),
        statusLabel = humanizeCode(status),
        enabled = status == UserSecurityService.STATUS_ACTIVE,
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
