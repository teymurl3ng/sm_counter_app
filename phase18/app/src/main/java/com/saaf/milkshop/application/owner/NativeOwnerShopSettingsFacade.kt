package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BusinessSettingsService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class NativeOwnerExpenseCategoryRow(
    val accountId: Long,
    val name: String,
    val statusLabel: String,
    val active: Boolean,
)

data class NativeOwnerOpeningProductRow(
    val productId: Long,
    val name: String,
    val unitLabel: String,
    val quantity: Double?,
)

data class NativeOwnerOpeningAccountRow(
    val accountId: Long,
    val name: String,
    val kindLabel: String,
    val balance: Double?,
)

data class NativeOwnerShopSettingsSnapshot(
    val shopName: String,
    val phone: String?,
    val address: String?,
    val currencyCode: String,
    val hasStoreIdentity: Boolean,
    val vendorMilkRateLabel: String,
    val vendorDeliveryFeeLabel: String,
    val tsStandardLabel: String,
    val retailMilkRateLabel: String,
    val retailYogurtRateLabel: String,
    val defaultSurchargeLabel: String,
    val milkRateVendor: Double,
    val deliveryFeeVendor: Double,
    val tsStandard: Double,
    val retailRateMilk: Double,
    val retailRateYogurt: Double,
    val defaultDeliverySurcharge: Double,
    val openingRecorded: Boolean,
    val canRecordOpening: Boolean,
    val openingDate: String?,
    val openingDateLabel: String,
    val openingNotes: String?,
    val openingInventoryCount: Int,
    val openingAccountCount: Int,
    val openingProducts: List<NativeOwnerOpeningProductRow>,
    val openingAccounts: List<NativeOwnerOpeningAccountRow>,
    val expenseCategories: List<NativeOwnerExpenseCategoryRow>,
)

/**
 * Owner Shop settings boundary: read store identity, rates, opening state, and
 * expense categories; typed writes for identity, rates, expense categories, and
 * opening record/correct after recent full passphrase authentication.
 */
class NativeOwnerShopSettingsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
    private val storeIdentity: StoreIdentityService = StoreIdentityService(database),
    private val businessSettings: BusinessSettingsService = BusinessSettingsService(database),
    private val expenseCategories: ExpenseCategoryManagementService =
        ExpenseCategoryManagementService(database),
    private val openingState: OpeningStateService = OpeningStateService(database),
) {
    suspend fun load(sessionHandle: String): NativeOwnerShopSettingsSnapshot {
        requireOwnerBusinessSettings(sessionHandle)
        val settings = reads.businessSettings(sessionHandle)
        val rates = businessSettings.get()
        val store = settings.store
        val opening = settings.openingState
        val hasStoreIdentity = store != null && store.storeName.isNotBlank()
        val openingRecorded = opening != null
        val eventCount = database.eventDao().count()
        val canRecordOpening = hasStoreIdentity && !openingRecorded && eventCount == 0

        val productIds = opening?.inventoryBalances?.map { it.productId }?.toSet().orEmpty()
        val accountIds = opening?.accountBalances?.map { it.accountId }?.toSet().orEmpty()
        val inventoryByProduct = opening?.inventoryBalances
            ?.associate { it.productId to (it.quantity.toDoubleOrNull() ?: 0.0) }
            .orEmpty()
        val balanceByAccount = opening?.accountBalances
            ?.associate {
                it.accountId to BigDecimal.valueOf(it.balancePaisa).movePointLeft(2).toDouble()
            }
            .orEmpty()

        val master = reads.masterData(sessionHandle)
        val references = reads.transactionReferences(sessionHandle)
        val openingProducts = master.products
            .filter { product ->
                product.inventoryTracking && (!product.archived || product.productId in productIds)
            }
            .map { product ->
                NativeOwnerOpeningProductRow(
                    productId = product.productId,
                    name = product.name,
                    unitLabel = product.unit,
                    quantity = inventoryByProduct[product.productId],
                )
            }
        val openingAccounts = buildList {
            references.paymentAccounts.forEach { account ->
                add(
                    NativeOwnerOpeningAccountRow(
                        accountId = account.accountId,
                        name = account.name,
                        kindLabel = "Cash or bank",
                        balance = balanceByAccount[account.accountId],
                    ),
                )
            }
            master.customers
                .filter { it.active || it.accountId in accountIds }
                .forEach { account ->
                    add(
                        NativeOwnerOpeningAccountRow(
                            accountId = account.accountId,
                            name = account.name,
                            kindLabel = "Customer owes shop",
                            balance = balanceByAccount[account.accountId],
                        ),
                    )
                }
            master.vendors
                .filter { it.active || it.accountId in accountIds }
                .forEach { account ->
                    add(
                        NativeOwnerOpeningAccountRow(
                            accountId = account.accountId,
                            name = account.name,
                            kindLabel = "Shop owes supplier",
                            balance = balanceByAccount[account.accountId],
                        ),
                    )
                }
        }

        return NativeOwnerShopSettingsSnapshot(
            shopName = store?.storeName?.takeIf { it.isNotBlank() } ?: "Your shop",
            phone = store?.phone,
            address = store?.address,
            currencyCode = store?.currencyCode?.takeIf { it.isNotBlank() } ?: "PKR",
            hasStoreIdentity = hasStoreIdentity,
            vendorMilkRateLabel = formatPaisa(settings.vendorMilkRatePaisaPerUnit),
            vendorDeliveryFeeLabel = formatPaisa(settings.vendorDeliveryFeePaisaPerUnit),
            tsStandardLabel = "${settings.tsStandardPercent}%",
            retailMilkRateLabel = formatPaisa(settings.retailMilkRatePaisaPerUnit),
            retailYogurtRateLabel = formatPaisa(settings.retailYogurtRatePaisaPerUnit),
            defaultSurchargeLabel = formatPaisa(settings.defaultDeliverySurchargePaisa),
            milkRateVendor = rates.milkRateVendor,
            deliveryFeeVendor = rates.deliveryFeeVendor,
            tsStandard = rates.tsStandard,
            retailRateMilk = rates.retailRateMilk,
            retailRateYogurt = rates.retailRateYogurt,
            defaultDeliverySurcharge = rates.defaultDeliverySurcharge,
            openingRecorded = openingRecorded,
            canRecordOpening = canRecordOpening,
            openingDate = opening?.openingDate,
            openingDateLabel = opening?.openingDate ?: "No opening state recorded",
            openingNotes = opening?.notes,
            openingInventoryCount = opening?.inventoryBalances?.size ?: 0,
            openingAccountCount = opening?.accountBalances?.size ?: 0,
            openingProducts = openingProducts,
            openingAccounts = openingAccounts,
            expenseCategories = settings.expenseCategories.map { category ->
                NativeOwnerExpenseCategoryRow(
                    accountId = category.accountId,
                    name = category.displayName,
                    statusLabel = if (category.active) "Active" else "Inactive",
                    active = category.active,
                )
            },
        )
    }

    suspend fun loadCategoryDetail(
        sessionHandle: String,
        accountId: Long,
    ): NativeOwnerExpenseCategoryRow {
        requireOwnerBusinessSettings(sessionHandle)
        val settings = reads.businessSettings(sessionHandle)
        val category = settings.expenseCategories.firstOrNull { it.accountId == accountId }
            ?: error("This expense category is not in Shop settings.")
        return NativeOwnerExpenseCategoryRow(
            accountId = category.accountId,
            name = category.displayName,
            statusLabel = if (category.active) "Active" else "Inactive",
            active = category.active,
        )
    }

    suspend fun saveIdentity(
        sessionHandle: String,
        input: StoreIdentityInput,
    ) {
        requireOwnerBusinessSettings(sessionHandle)
        storeIdentity.save(input)
    }

    suspend fun updateRates(
        sessionHandle: String,
        settings: BusinessSettings,
    ) {
        requireOwnerBusinessSettings(sessionHandle)
        businessSettings.update(settings)
    }

    suspend fun createExpenseCategory(
        sessionHandle: String,
        name: String,
    ): Long {
        requireOwnerMasterData(sessionHandle)
        return expenseCategories.create(name)
    }

    suspend fun setExpenseCategoryArchived(
        sessionHandle: String,
        accountId: Long,
        archived: Boolean,
    ) {
        requireOwnerMasterData(sessionHandle)
        expenseCategories.setArchived(accountId, archived)
    }

    suspend fun recordOpening(
        sessionHandle: String,
        input: OpeningStateInput,
    ) {
        requireOwnerBusinessSettings(sessionHandle, recent = true)
        if (database.openingStateDao().initialCount() != 0) {
            error("Opening balances have already been recorded")
        }
        if (database.eventDao().count() != 0) {
            error("Opening balances can only be recorded before the first business activity")
        }
        openingState.record(input)
    }

    suspend fun correctOpening(
        sessionHandle: String,
        input: OpeningStateInput,
    ) {
        requireOwnerBusinessSettings(sessionHandle, recent = true)
        if (openingState.current() == null) {
            error("Opening balances have not been recorded")
        }
        openingState.correct(input)
    }

    suspend fun reauthenticate(
        sessionHandle: String,
        passphrase: CharArray,
    ) {
        try {
            requireOwnerBusinessSettings(sessionHandle, recent = false)
            security.reauthenticate(sessionHandle, passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun requireOwnerBusinessSettings(
        sessionHandle: String,
        recent: Boolean = false,
    ) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val role = AppRole.parse(actor.role)
        require(role == AppRole.OWNER || role == AppRole.ADMIN) {
            "Shop settings is only available to Owner or Admin."
        }
        if (recent) {
            security.sessionManager().requireRecentAuthentication(sessionHandle)
        }
    }

    private suspend fun requireOwnerMasterData(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        val role = AppRole.parse(actor.role)
        require(role == AppRole.OWNER || role == AppRole.ADMIN) {
            "Shop settings is only available to Owner or Admin."
        }
    }

    private fun formatPaisa(paisa: Long): String {
        val pkr = BigDecimal.valueOf(paisa).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "PKR %,.2f", pkr)
    }
}
