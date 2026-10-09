package com.saaf.milkshop.application.commands

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.application.bridge.BridgeCommandConflict
import com.saaf.milkshop.application.bridge.BridgeCommandNotFound
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AttendanceInput
import com.saaf.milkshop.domain.AttendanceService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.EmployeeProfileService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.BusinessDateGuard
import com.saaf.milkshop.sync.SyncScheduler
import java.time.Instant

internal class EmployeeBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun createEmployee(sessionHandle: String, name: String): Long = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        requireUniqueEmployeeName(name)
        val accountId = EmployeeManagementService(database).create(name)
        audit(ACTION_CREATE_EMPLOYEE, accountId, actor.userId, actor.displayName, actor.role)
        accountId
    }

    suspend fun renameEmployee(sessionHandle: String, accountId: Long, name: String) = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        requireEmployee(accountId)
        requireUniqueEmployeeName(name, accountId)
        EmployeeManagementService(database).rename(accountId, name)
        audit(ACTION_RENAME_EMPLOYEE, accountId, actor.userId, actor.displayName, actor.role)
    }

    suspend fun saveEmployeeProfile(sessionHandle: String, accountId: Long, input: EmployeeProfileInput) =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            requireEmployee(accountId)
            EmployeeProfileService(database).save(accountId, input)
            audit(ACTION_SAVE_EMPLOYEE_PROFILE, accountId, actor.userId, actor.displayName, actor.role)
        }

    suspend fun recordAttendance(sessionHandle: String, input: AttendanceInput): Long = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.ATTENDANCE)
        BusinessDateGuard.requireHappenedWithinHorizon(input.businessDate)
        requireEmployee(input.employeeAccountId)
        val attendanceId = AttendanceService(database).record(input)
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.now().toString(),
                action = ACTION_RECORD_ATTENDANCE,
                result = RESULT_SUCCESS,
                actorUserId = actor.userId,
                actorNameSnapshot = actor.displayName,
                actorRoleSnapshot = actor.role,
                details = "attendance_id=$attendanceId;employee_account_id=${input.employeeAccountId};business_date=${input.businessDate}",
            )
        )
        attendanceId
    }.also { SyncScheduler.requestPush() }

    private suspend fun requireEmployee(accountId: Long) {
        val account = database.accountDao().findById(accountId)
            ?: throw BridgeCommandNotFound("This employee no longer exists")
        if (account.category != "Salary") throw BridgeCommandNotFound("This employee no longer exists")
    }

    private suspend fun requireUniqueEmployeeName(name: String, exceptAccountId: Long? = null) {
        val candidate = name.trim().removePrefix("Employee: ").trim()
        val duplicate = database.accountDao().findByCategory("Salary").any { account ->
            account.accountId != exceptAccountId &&
                account.name.removePrefix("Employee: ").trim().equals(candidate, ignoreCase = true)
        }
        if (duplicate) throw BridgeCommandConflict("An employee with this name already exists")
    }

    private suspend fun audit(action: String, accountId: Long, actorUserId: Long, actorName: String, actorRole: String) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.now().toString(),
                action = action,
                result = RESULT_SUCCESS,
                actorUserId = actorUserId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                details = "account_id=$accountId",
            )
        )
    }

    companion object {
        const val ACTION_CREATE_EMPLOYEE = "bridge_create_employee"
        const val ACTION_RENAME_EMPLOYEE = "bridge_rename_employee"
        const val ACTION_SAVE_EMPLOYEE_PROFILE = "bridge_save_employee_profile"
        const val ACTION_RECORD_ATTENDANCE = "bridge_record_attendance"
        const val RESULT_SUCCESS = "success"
    }
}
