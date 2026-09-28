package com.saaf.milkshop.application.operations

import com.saaf.milkshop.application.bridge.EmployeeBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AttendanceInput
import com.saaf.milkshop.domain.UserSecurityService
import java.time.LocalDate
import java.util.Locale
import com.saaf.milkshop.sync.BusinessClock

/**
 * Typed native boundary for Operations My attendance.
 *
 * Self-attendance only for the signed-in account's linked employee.
 */
class NativeAttendanceFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val commands = EmployeeBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeAttendanceContext {
        val actor = security.requirePermission(sessionHandle, AppPermission.ATTENDANCE)
        val businessDate = date.toString()
        val employeeId = actor.employeeAccountId
        if (employeeId == null) {
            return NativeAttendanceContext(
                businessDate = businessDate,
                postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
                link = NativeAttendanceLink.Unlinked,
            )
        }
        val account = database.accountDao().findById(employeeId)
        val employeeName = account?.name
            ?.removePrefix("Employee: ")
            ?.trim()
            .orEmpty()
            .ifBlank { "Employee #$employeeId" }
        val record = database.attendanceDao().current(employeeId, businessDate)
        return NativeAttendanceContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            link = NativeAttendanceLink.Linked(
                employeeAccountId = employeeId,
                employeeName = employeeName,
                todayStatus = record?.status?.let(::formatStatus),
                attendanceId = record?.attendanceId,
            ),
        )
    }

    suspend fun markPresent(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeAttendanceContext {
        val before = load(sessionHandle, date)
        if (!before.postingOpen) {
            throw IllegalStateException(
                "This business day is closed. Ask Owner to reopen it before marking attendance.",
            )
        }
        val linked = before.link as? NativeAttendanceLink.Linked
            ?: throw IllegalStateException(
                "Ask the Owner or Administrator to link this account to an employee.",
            )
        if (linked.todayStatus != null) {
            return before
        }
        commands.recordAttendance(
            sessionHandle,
            AttendanceInput(
                employeeAccountId = linked.employeeAccountId,
                businessDate = before.businessDate,
                status = "present",
                notes = null,
            ),
        )
        return load(sessionHandle, date)
    }

    private fun formatStatus(raw: String): String =
        raw.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.US) }
}

sealed interface NativeAttendanceLink {
    data object Unlinked : NativeAttendanceLink
    data class Linked(
        val employeeAccountId: Long,
        val employeeName: String,
        val todayStatus: String?,
        val attendanceId: Long?,
    ) : NativeAttendanceLink
}

data class NativeAttendanceContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val link: NativeAttendanceLink,
)
