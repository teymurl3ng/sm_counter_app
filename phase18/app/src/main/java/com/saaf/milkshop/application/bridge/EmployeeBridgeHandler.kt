package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.commands.EmployeeBridgeCommandService
import com.saaf.milkshop.domain.AttendanceInput
import com.saaf.milkshop.domain.AttendanceService
import com.saaf.milkshop.domain.EmployeeProfileInput
import org.json.JSONObject
import java.math.BigDecimal

internal class EmployeeBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: EmployeeBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.ATTENDANCE -> {
            strictPayload(request.payload, setOf("businessDate"))
            attendanceJson(reads.attendance(request.requireSession(), request.payload.requiredDate("businessDate")))
        }
        HybridBridgeActions.CREATE_EMPLOYEE -> {
            strictPayload(request.payload, setOf("name"))
            JSONObject().put(
                "entityId",
                commands.createEmployee(request.requireSession(), request.payload.requiredString("name")).toString(),
            )
        }
        HybridBridgeActions.RENAME_EMPLOYEE -> {
            strictPayload(request.payload, setOf("accountId", "name"))
            commands.renameEmployee(
                request.requireSession(),
                request.payload.requiredId("accountId"),
                request.payload.requiredString("name"),
            )
            JSONObject()
        }
        HybridBridgeActions.SAVE_EMPLOYEE_PROFILE -> saveEmployeeProfile(request)
        HybridBridgeActions.RECORD_ATTENDANCE -> recordAttendance(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun saveEmployeeProfile(request: BridgeRequest): JSONObject {
        strictPayload(
            request.payload,
            setOf("accountId", "roleLabel", "phone", "referenceSalaryPaisa", "salaryPeriodCode", "joinDate", "active"),
        )
        val salaryPaisa = request.payload.nullablePaisa("referenceSalaryPaisa")
        if (salaryPaisa != null && salaryPaisa < 0L) {
            throw BridgeProtocolFailure("validation_failed", "Reference salary cannot be negative", "referenceSalaryPaisa")
        }
        commands.saveEmployeeProfile(
            request.requireSession(),
            request.payload.requiredId("accountId"),
            EmployeeProfileInput(
                role = request.payload.nullableString("roleLabel"),
                phone = request.payload.nullableString("phone"),
                referenceSalary = salaryPaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
                salaryPeriod = request.payload.nullableString("salaryPeriodCode"),
                joinDate = request.payload.nullableDate("joinDate"),
                active = request.payload.requiredBoolean("active"),
            ),
        )
        return JSONObject()
    }

    private suspend fun recordAttendance(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("employeeAccountId", "businessDate", "statusCode"),
            allowed = setOf("employeeAccountId", "businessDate", "statusCode", "notes"),
        )
        val status = request.payload.requiredString("statusCode")
        if (status !in AttendanceService.STATUSES) {
            throw BridgeProtocolFailure("validation_failed", "Choose present, absent, or half day", "statusCode")
        }
        val attendanceId = commands.recordAttendance(
            request.requireSession(),
            AttendanceInput(
                employeeAccountId = request.payload.requiredId("employeeAccountId"),
                businessDate = request.payload.requiredDate("businessDate"),
                status = status,
                notes = request.payload.optionalString("notes"),
            ),
        )
        return JSONObject().put("entityId", attendanceId.toString())
    }
}
