package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPasswordPolicy
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService

internal class SecurityAdministrationBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun beginEnrollment(session: String, input: CreateUserInput) = security.beginUserEnrollment(session, input.also {
        val normalized = AppPasswordPolicy.normalizeLoginName(it.loginName)
        if (database.appUserDao().findByLoginName(normalized) != null) throw BridgeCommandConflict("This login name is already in use")
        it.employeeAccountId?.let { employeeId ->
            if (database.employeeProfileDao().findByAccountId(employeeId) == null) throw BridgeCommandNotFound("The linked employee does not exist")
            if (database.appUserDao().all().any { user -> user.employeeAccountId == employeeId }) throw BridgeCommandConflict("This employee is already linked to an app account")
        }
    })

    suspend fun resumeEnrollment(session: String, targetUserId: Long) = security.resumeUserEnrollment(session, existing(targetUserId))

    suspend fun completeEnrollment(token: String, passphrase: CharArray) = security.completeUserEnrollment(token, passphrase)

    suspend fun changeRole(session: String, targetUserId: Long, role: String) = security.changeRole(session, existing(targetUserId), role)

    suspend fun setEnabled(session: String, targetUserId: Long, enabled: Boolean) = security.setUserEnabled(session, existing(targetUserId), enabled)

    suspend fun beginCredentialReset(session: String, targetUserId: Long) = security.beginCredentialReset(session, existing(targetUserId))

    private suspend fun existing(targetUserId: Long): Long {
        if (database.appUserDao().findById(targetUserId) == null) throw BridgeCommandNotFound("The user account does not exist")
        return targetUserId
    }
}
