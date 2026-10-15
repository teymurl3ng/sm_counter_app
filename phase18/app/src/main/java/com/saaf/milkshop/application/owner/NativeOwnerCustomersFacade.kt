package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.MilkShopDatabase
import androidx.room3.withReadTransaction
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
    val credit: CustomerCreditView? = null,
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
    val creditAge: com.saaf.milkshop.domain.CreditAge? = null,
    val ledger: NativeOwnerAccountLedgerDetail? = null,
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
    suspend fun loadCustomers(sessionHandle: String): NativeOwnerCustomersSnapshot = database.withReadTransaction {
        requireOwnerMasterData(sessionHandle)
        val infos = customers.list()
        val credit = CustomerCreditViews(database).load(infos.associate { it.accountId to it.balance })
        val profileRows = database.backupDataDao().customerProfiles().associateBy { it.accountId }
        val rows = infos.map { info ->
            val profile = profileRows[info.accountId]
            info.toRow(
                active = profile?.active ?: true,
                phone = profile?.phone,
                areaRoute = profile?.areaRoute,
            ).copy(credit = credit[info.accountId])
        }
        NativeOwnerCustomersSnapshot(customers = rows)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        accountId: Long,
    ): NativeOwnerCustomerDetail = database.withReadTransaction {
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
        val credit = CustomerCreditViews(database).load(mapOf(accountId to info.balance))[accountId]
        NativeOwnerCustomerDetail(
            ledger = NativeOwnerRecordsFacade(database,security).loadLedger(sessionHandle,accountId),
            creditAge = com.saaf.milkshop.domain.CustomerCreditAgeService(database).load(accountId,
                com.saaf.milkshop.application.readmodels.money(info.balance), com.saaf.milkshop.sync.BusinessClock.today()),
            row = row.copy(credit = credit),
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

    suspend fun creditAction(sessionHandle: String, accountId: Long, action: CustomerCreditCommand) {
        requireOwnerMasterData(sessionHandle)
        val actor=security.requirePermission(sessionHandle,AppPermission.MANAGE_MASTER_DATA)
        require(actor.role == AppRole.OWNER.wireValue) { "Only Owner can change credit terms or review flags." }
        val uid=requireNotNull(database.accountDao().findById(accountId)).uid
        val service=com.saaf.milkshop.domain.CreditReviewService(database,security)
        when(action) {
            is CustomerCreditCommand.Terms -> service.proposeTerms(sessionHandle,uid,action.expectedRevision,action.term)
            is CustomerCreditCommand.Flag -> service.setFlag(sessionHandle,uid,action.reason)
            is CustomerCreditCommand.Clear -> service.clearFlag(sessionHandle,uid,action.reviewedSets,action.reason)
            is CustomerCreditCommand.Retry -> service.reviewPending(sessionHandle,uid,action.uid,false)
            is CustomerCreditCommand.DiscardTerm -> service.reviewPending(sessionHandle,uid,action.uid,true)
            CustomerCreditCommand.Enable -> Unit // The controller calls the authenticated runtime only after this local Owner check.
        }
        com.saaf.milkshop.sync.SyncScheduler.requestPush()
    }

    private suspend fun requireOwnerMasterData(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        val role = AppRole.parse(actor.role)
        require(role == AppRole.OWNER || role == AppRole.MANAGER || role == AppRole.ADMIN) {
            "Customers is only available to Owner, Manager, or Administrator."
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
