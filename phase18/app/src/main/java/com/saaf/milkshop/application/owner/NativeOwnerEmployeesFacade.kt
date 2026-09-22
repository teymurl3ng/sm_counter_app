package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.EmployeeProfileService
import com.saaf.milkshop.domain.UserSecurityService
import java.util.Locale

data class NativeOwnerEmployeeRow(
    val accountId: Long,
    val name: String,
    val balanceLabel: String,
    val statusLabel: String,
    val role: String?,
    val phone: String?,
)

data class NativeOwnerEmployeeDetail(
    val row: NativeOwnerEmployeeRow,
    val referenceSalary: Double?,
    val referenceSalaryLabel: String?,
    val salaryPeriod: String?,
    val joinDate: String?,
    val active: Boolean,
)

data class NativeOwnerEmployeesSnapshot(
    val employees: List<NativeOwnerEmployeeRow>,
)

/**
 * Owner Employees boundary: Salary-account list + read detail + create/rename/profile
 * from [EmployeeManagementService] / [EmployeeProfileService]. Does not invent
 * balance formulas, salary payments, attendance writes, or call bridge JSON.
 * Distinct from Users & roles.
 */
class NativeOwnerEmployeesFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val employees: EmployeeManagementService = EmployeeManagementService(database),
    private val profiles: EmployeeProfileService = EmployeeProfileService(database),
) {
    suspend fun loadEmployees(sessionHandle: String): NativeOwnerEmployeesSnapshot {
        requireOwnerMasterData(sessionHandle)
        val rows = employees.list().map { info ->
            val profile = profiles.get(info.accountId)
            info.toRow(
                active = profile?.active ?: true,
                role = profile?.role,
                phone = profile?.phone,
            )
        }
        return NativeOwnerEmployeesSnapshot(employees = rows)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        accountId: Long,
    ): NativeOwnerEmployeeDetail {
        requireOwnerMasterData(sessionHandle)
        val info = employees.list().firstOrNull { it.accountId == accountId }
            ?: error("This employee is not in the employee list.")
        val profile = profiles.get(accountId)
        val active = profile?.active ?: true
        val row = info.toRow(
            active = active,
            role = profile?.role,
            phone = profile?.phone,
        )
        return NativeOwnerEmployeeDetail(
            row = row,
            referenceSalary = profile?.referenceSalary,
            referenceSalaryLabel = profile?.referenceSalary?.let { formatMoney(it) },
            salaryPeriod = profile?.salaryPeriod,
            joinDate = profile?.joinDate,
            active = active,
        )
    }

    suspend fun createEmployee(
        sessionHandle: String,
        name: String,
    ): Long {
        requireOwnerMasterData(sessionHandle)
        return employees.create(name)
    }

    /**
     * Rename then save profile (auth once). Deactivate is [EmployeeProfileInput.active] = false.
     */
    suspend fun updateEmployee(
        sessionHandle: String,
        accountId: Long,
        name: String,
        profile: EmployeeProfileInput,
    ) {
        requireOwnerMasterData(sessionHandle)
        employees.rename(accountId, name)
        profiles.save(accountId, profile)
    }

    private suspend fun requireOwnerMasterData(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Employees is only available to Owner."
        }
    }

    private fun EmployeeManagementService.EmployeeInfo.toRow(
        active: Boolean,
        role: String?,
        phone: String?,
    ): NativeOwnerEmployeeRow = NativeOwnerEmployeeRow(
        accountId = accountId,
        name = name,
        balanceLabel = formatMoney(balance),
        statusLabel = if (active) "Active" else "Inactive",
        role = role,
        phone = phone,
    )

    private fun formatMoney(amount: Double): String =
        String.format(Locale.US, "PKR %,.2f", amount)
}
