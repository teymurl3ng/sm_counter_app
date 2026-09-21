package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class NativeOwnerExpenseCategoryRow(
    val accountId: Long,
    val name: String,
    val statusLabel: String,
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
    val openingDateLabel: String,
    val openingNotes: String?,
    val expenseCategories: List<NativeOwnerExpenseCategoryRow>,
)

/**
 * Owner Shop settings boundary: read store identity, rates, opening state, and
 * expense categories from [HybridReadModelService.businessSettings]. Does not
 * invent rates or call bridge JSON. Edit stays out of this facade.
 */
class NativeOwnerShopSettingsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
) {
    suspend fun load(sessionHandle: String): NativeOwnerShopSettingsSnapshot {
        requireOwnerBusinessSettings(sessionHandle)
        val settings = reads.businessSettings(sessionHandle)
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
            openingDateLabel = settings.openingState?.openingDate
                ?: "No opening state recorded",
            openingNotes = settings.openingState?.notes,
            expenseCategories = settings.expenseCategories.map { category ->
                NativeOwnerExpenseCategoryRow(
                    accountId = category.accountId,
                    name = category.displayName,
                    statusLabel = if (category.active) "Active" else "Inactive",
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
        )
    }

    private suspend fun requireOwnerBusinessSettings(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Shop settings is only available to Owner."
        }
    }

    private fun formatPaisa(paisa: Long): String {
        val pkr = BigDecimal.valueOf(paisa).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "PKR %,.2f", pkr)
    }
}
