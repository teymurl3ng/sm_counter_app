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
 * [UserSecurityService.listUsers] plus enroll / enable / role-change writes.
 * Distinct from Employees salary accounts. Never calls bridge JSON.
 */
class NativeOwnerUsersRolesFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    fun enrollableRoles(): List<AppRole> = AppRole.entries.toList()

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

    suspend fun beginEnroll(
        sessionHandle: String,
        input: CreateUserInput,
    ): NativeOwnerPendingEnrollment {
        requireOwnerStaffUsers(sessionHandle)
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
        requireOwnerStaffUsers(sessionHandle)
        security.setUserEnabled(sessionHandle, userId, enabled)
    }

    suspend fun changeRole(
        sessionHandle: String,
        userId: Long,
        newRoleValue: String,
    ) {
        requireOwnerStaffUsers(sessionHandle)
        security.changeRole(sessionHandle, userId, newRoleValue)
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
            requireOwnerStaffUsers(sessionHandle)
            security.reauthenticate(sessionHandle, passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun requireOwnerStaffUsers(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_STAFF_USERS)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Users & roles is only available to Owner."
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
