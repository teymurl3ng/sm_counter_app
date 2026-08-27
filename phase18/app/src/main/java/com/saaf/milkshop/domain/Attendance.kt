package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AttendanceRecordEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant
import java.time.LocalDate

data class AttendanceInput(
    val employeeAccountId: Long,
    val businessDate: String,
    val status: String,
    val notes: String? = null,
)

class AttendanceService(private val database: MilkShopDatabase) {
    suspend fun record(input: AttendanceInput): Long = database.withWriteTransaction {
        requireActiveProfile(database, input.employeeAccountId, "Salary")
        LocalDate.parse(input.businessDate)
        require(input.status in STATUSES) { "Attendance status must be present, absent, or half_day" }

        val previous = database.attendanceDao().current(input.employeeAccountId, input.businessDate)
        if (previous != null) {
            check(database.attendanceDao().archive(previous.attendanceId) == 1) {
                "Attendance revision could not archive the previous record"
            }
        }
        database.attendanceDao().insert(
            AttendanceRecordEntity(
                employeeAccountId = input.employeeAccountId,
                businessDate = input.businessDate,
                status = input.status,
                notes = input.notes.cleanOptional(),
                createdAt = Instant.now().toString(),
                supersedesId = previous?.attendanceId,
            )
        )
    }

    suspend fun current(employeeAccountId: Long, businessDate: String): AttendanceRecordEntity? =
        database.attendanceDao().current(employeeAccountId, businessDate)

    suspend fun history(employeeAccountId: Long): List<AttendanceRecordEntity> =
        database.attendanceDao().history(employeeAccountId)

    companion object {
        val STATUSES = setOf("present", "absent", "half_day")
    }
}
