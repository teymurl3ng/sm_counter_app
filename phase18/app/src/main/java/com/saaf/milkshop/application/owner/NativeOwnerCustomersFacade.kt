package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.CustomerProfileService
import com.saaf.milkshop.domain.UserSecurityService
import java.util.Locale

data class NativeOwnerCustomerRow(
    val accountId: Long,
    val name: String,
    val balanceLabel: String,
    val statusLabel: String,
    val phone: String?,
    val areaRoute: String?,
)

data class NativeOwnerCustomerDetail(
    val row: NativeOwnerCustomerRow,
    val address: String?,
    val notes: String?,
    val creditLimit: Double?,
    val dailyQuota: Double?,
    val active: Boolean,
    val creditLimitLabel: String?,
    val dailyQuotaLabel: String?,
)

data class NativeOwnerCustomersSnapshot(
    val customers: List<NativeOwnerCustomerRow>,
)

/**
 * Owner Customers boundary: customer list + read detail + create/rename/profile
 * from [CustomerManagementService] / [CustomerProfileService]. Does not invent
 * balance formulas or call bridge JSON.
 */
class NativeOwnerCustomersFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val customers: CustomerManagementService = CustomerManagementService(database),
    private val profiles: CustomerProfileService = CustomerProfileService(database),
) {
    suspend fun loadCustomers(sessionHandle: String): NativeOwnerCustomersSnapshot {
        requireOwnerMasterData(sessionHandle)
        val rows = customers.list().map { info ->
            val profile = profiles.get(info.accountId)
            info.toRow(
                active = profile?.active ?: true,
                phone = profile?.phone,
                areaRoute = profile?.areaRoute,
            )
        }
        return NativeOwnerCustomersSnapshot(customers = rows)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        accountId: Long,
    ): NativeOwnerCustomerDetail {
        requireOwnerMasterData(sessionHandle)
        val info = customers.list().firstOrNull { it.accountId == accountId }
            ?: error("This customer is not in the customer list.")
        val profile = profiles.get(accountId)
        val active = profile?.active ?: true
        val row = info.toRow(
            active = active,
            phone = profile?.phone,
            areaRoute = profile?.areaRoute,
        )
        return NativeOwnerCustomerDetail(
            row = row,
            address = profile?.address,
            notes = profile?.notes,
            creditLimit = profile?.creditLimit,
            dailyQuota = profile?.dailyQuota,
            active = active,
            creditLimitLabel = profile?.creditLimit?.let { formatMoney(it) },
            dailyQuotaLabel = profile?.dailyQuota?.let {
                String.format(Locale.US, "%,.2f L / day", it)
            },
        )
    }

    suspend fun createCustomer(
        sessionHandle: String,
        name: String,
    ): Long {
        requireOwnerMasterData(sessionHandle)
        return customers.create(name)
    }

    /**
     * Rename then save profile (auth once). Deactivate is [CustomerProfileInput.active] = false.
     */
    suspend fun updateCustomer(
        sessionHandle: String,
        accountId: Long,
        name: String,
        profile: CustomerProfileInput,
    ) {
        requireOwnerMasterData(sessionHandle)
        customers.rename(accountId, name)
        profiles.save(accountId, profile)
    }

    private suspend fun requireOwnerMasterData(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Customers is only available to Owner."
        }
    }

    private fun CustomerManagementService.CustomerInfo.toRow(
        active: Boolean,
        phone: String?,
        areaRoute: String?,
    ): NativeOwnerCustomerRow = NativeOwnerCustomerRow(
        accountId = accountId,
        name = name,
        balanceLabel = formatMoney(balance),
        statusLabel = if (active) "Active" else "Inactive",
        phone = phone,
        areaRoute = areaRoute,
    )

    private fun formatMoney(amount: Double): String =
        String.format(Locale.US, "PKR %,.2f", amount)
}
