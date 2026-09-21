package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.EmployeeManagementService
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
    val referenceSalaryLabel: String?,
    val salaryPeriod: String?,
    val joinDate: String?,
)

data class NativeOwnerEmployeesSnapshot(
    val employees: List<NativeOwnerEmployeeRow>,
)

/**
 * Owner Employees boundary: Salary-account list + read detail from
 * [EmployeeManagementService] / [EmployeeProfileService]. Does not invent
 * balance formulas or call bridge JSON. Distinct from Users & roles.
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
        val row = info.toRow(
            active = profile?.active ?: true,
            role = profile?.role,
            phone = profile?.phone,
        )
        return NativeOwnerEmployeeDetail(
            row = row,
            referenceSalaryLabel = profile?.referenceSalary?.let { formatMoney(it) },
            salaryPeriod = profile?.salaryPeriod,
            joinDate = profile?.joinDate,
        )
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
