package com.saaf.milkshop.domain

import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogDao
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductDao
import com.saaf.milkshop.data.SettingsDao
import androidx.room3.withWriteTransaction
import java.time.Instant

/** Input fields defined by core spec §4. */
data class VendorPurchaseInput(
    val vendorAccountId: Long,
    val occurredAt: String,
    val rawLiters: Double,
    val fatPercentage: Double,
    val lrReading: Double,
    val notes: String? = null,
)

data class VendorPurchaseResult(
    val eventId: Long,
    val snfPercentage: Double,
    val tsPercentage: Double,
    val adjustedLiters: Double,
    val amountOwed: Double,
)

/**
 * Pure calculation for spec §4. It does not read or write the database.
 * Keeping this separate makes the formula independently unit-testable.
 */
object VendorPurchaseCalculator {
    fun calculate(
        rawLiters: Double,
        fatPercentage: Double,
        lrReading: Double,
        milkRate: Double,
        deliveryFee: Double,
        tsStandard: Double,
    ): VendorPurchaseResultValues {
        require(rawLiters > 0.0) { "Raw liters must be greater than zero" }
        require(fatPercentage >= 0.0) { "Fat percentage cannot be negative" }
        require(lrReading > 0.0) { "LR reading must be greater than zero" }
        require(milkRate > 0.0) { "Milk rate must be greater than zero" }
        require(deliveryFee > 0.0) { "Delivery fee must be greater than zero" }
        require(tsStandard > 0.0) { "TS standard must be greater than zero" }

        val snf = (0.25 * lrReading) + (0.22 * fatPercentage) + 0.72
        val ts = fatPercentage + snf
        val adjustedLiters = rawLiters * (ts / tsStandard)
        val amountOwed = (adjustedLiters * milkRate) + (rawLiters * deliveryFee)

        return VendorPurchaseResultValues(
            snfPercentage = snf,
            tsPercentage = ts,
            adjustedLiters = adjustedLiters,
            amountOwed = amountOwed,
        )
    }
}

data class VendorPurchaseResultValues(
    val snfPercentage: Double,
    val tsPercentage: Double,
    val adjustedLiters: Double,
    val amountOwed: Double,
)

/**
 * Phase 1 transaction service. This is the first business operation implemented
 * against the Room model. No UI concerns belong here.
 */
class VendorPurchaseService(
    private val database: MilkShopDatabase,
    private val productDao: ProductDao = database.productDao(),
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val inventoryLogDao: InventoryLogDao = database.inventoryLogDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
    private val settingsDao: SettingsDao = database.settingsDao(),
) {
    suspend fun record(input: VendorPurchaseInput): VendorPurchaseResult =
        database.withWriteTransaction {
            val vendor = requireActiveProfile(database, input.vendorAccountId, "Vendor")

            val milk = productDao.findByName("Milk")
                ?: error("Seeded Milk product is missing")
            requireActiveProduct(database, milk.productId)

            val milkRate = settingsDao.getValue(SettingKeys.MILK_RATE_VENDOR)
                ?: error("Missing setting: ${SettingKeys.MILK_RATE_VENDOR}")
            val deliveryFee = settingsDao.getValue(SettingKeys.DELIVERY_FEE_VENDOR)
                ?: error("Missing setting: ${SettingKeys.DELIVERY_FEE_VENDOR}")
            val tsStandard = settingsDao.getValue(SettingKeys.TS_STANDARD)
                ?: error("Missing setting: ${SettingKeys.TS_STANDARD}")

            val calculation = VendorPurchaseCalculator.calculate(
                rawLiters = input.rawLiters,
                fatPercentage = input.fatPercentage,
                lrReading = input.lrReading,
                milkRate = milkRate,
                deliveryFee = deliveryFee,
                tsStandard = tsStandard,
            )

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_PURCHASE_RECEIPT,
                    rawLiters = input.rawLiters,
                    fatPercentage = input.fatPercentage,
                    lrReading = input.lrReading,
                    snfPercentage = calculation.snfPercentage,
                    tsPercentage = calculation.tsPercentage,
                    adjustedLiters = calculation.adjustedLiters,
                    amountOwed = calculation.amountOwed,
                    milkRateUsed = milkRate,
                    deliveryFeeUsed = deliveryFee,
                    tsStandardUsed = tsStandard,
                    notes = input.notes,
                )
            )

            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = eventId,
                    productId = milk.productId,
                    quantity = input.rawLiters,
                )
            )

            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = vendor.accountId,
                    amount = calculation.amountOwed,
                )
            )

            VendorPurchaseResult(
                eventId = eventId,
                snfPercentage = calculation.snfPercentage,
                tsPercentage = calculation.tsPercentage,
                adjustedLiters = calculation.adjustedLiters,
                amountOwed = calculation.amountOwed,
            )
        }

    companion object {
        const val EVENT_TYPE_PURCHASE_RECEIPT = "PURCHASE_RECEIPT"
    }
}
