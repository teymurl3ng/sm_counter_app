package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BusinessSettingsService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
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

data class NativeOwnerShopSettingsSnapshot(
    val shopName: String,
    val phone: String?,
    val address: String?,
    val currencyCode: String,
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
    val openingDateLabel: String,
    val openingNotes: String?,
    val expenseCategories: List<NativeOwnerExpenseCategoryRow>,
)

/**
 * Owner Shop settings boundary: read store identity, rates, opening state, and
 * expense categories; typed writes for identity, rates, and expense categories.
 * Does not invent rates, call bridge JSON, or implement opening record/correct.
 */
class NativeOwnerShopSettingsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
    private val storeIdentity: StoreIdentityService = StoreIdentityService(database),
    private val businessSettings: BusinessSettingsService = BusinessSettingsService(database),
    private val expenseCategories: ExpenseCategoryManagementService =
        ExpenseCategoryManagementService(database),
) {
    suspend fun load(sessionHandle: String): NativeOwnerShopSettingsSnapshot {
        requireOwnerBusinessSettings(sessionHandle)
        val settings = reads.businessSettings(sessionHandle)
        val rates = businessSettings.get()
        val store = settings.store
        return NativeOwnerShopSettingsSnapshot(
            shopName = store?.storeName?.takeIf { it.isNotBlank() } ?: "Saaf Milk",
            phone = store?.phone,
            address = store?.address,
            currencyCode = store?.currencyCode?.takeIf { it.isNotBlank() } ?: "PKR",
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
            openingDateLabel = settings.openingState?.openingDate
                ?: "No opening state recorded",
            openingNotes = settings.openingState?.notes,
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

    private suspend fun requireOwnerBusinessSettings(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Shop settings is only available to Owner."
        }
    }

    private suspend fun requireOwnerMasterData(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Shop settings is only available to Owner."
        }
    }

    private fun formatPaisa(paisa: Long): String {
        val pkr = BigDecimal.valueOf(paisa).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "PKR %,.2f", pkr)
    }
}
