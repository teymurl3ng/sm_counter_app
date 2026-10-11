package com.saaf.milkshop.application.operations

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.SettingKeys
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseInput
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale
import kotlin.math.round
import com.saaf.milkshop.sync.BusinessClock

/**
 * Typed native boundary for Operations Receive milk.
 *
 * Projects physical intake only. Milk rate, delivery fee, amount owed, and
 * supplier balances never leave this facade.
 */
class NativeReceiveMilkFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeReceiveMilkContext {
        security.requirePermission(sessionHandle, AppPermission.PURCHASE)
        val businessDate = date.toString()
        val vendors = activeVendors()
        val tsStandard = database.settingsDao().getValue(SettingKeys.TS_STANDARD)
            ?: throw IllegalStateException("Ask Owner to set the TS standard before receiving milk.")
        require(tsStandard.isFinite() && tsStandard > 0.0) {
            "Ask Owner to set a valid TS standard before receiving milk."
        }
        val milk = database.productDao().findByName("Milk")
        return NativeReceiveMilkContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            vendors = vendors,
            tsStandardPercent = tsStandard,
            milkProductPresent = milk != null,
            milkOnHandLabel = milkOnHandLabel(milk?.productId),
        )
    }


    fun preview(
        context: NativeReceiveMilkContext,
        rawLiters: Double,
        fatPercentage: Double,
        lrReading: Double,
    ): NativeReceiveMilkPreview? {
        if (!rawLiters.isFinite() || rawLiters <= 0.0) return null
        if (!fatPercentage.isFinite() || fatPercentage < 0.0) return null
        if (!lrReading.isFinite() || lrReading <= 0.0) return null
        val snf = (0.25 * lrReading) + (0.22 * fatPercentage) + 0.72
        val ts = fatPercentage + snf
        val adjusted = rawLiters * (ts / context.tsStandardPercent)
        return NativeReceiveMilkPreview(
            snfPercentage = round4(snf),
            tsPercentage = round4(ts),
            adjustedLiters = round4(adjusted),
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeReceiveMilkRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeReceiveMilkSubmission {
        require(request.rawLiters.isFinite() && request.rawLiters > 0.0) {
            "Enter liters greater than zero."
        }
        require(request.fatPercentage.isFinite() && request.fatPercentage >= 0.0) {
            "Fat percentage cannot be negative."
        }
        require(request.lrReading.isFinite() && request.lrReading > 0.0) {
            "Enter an LR reading greater than zero."
        }
        val context = load(sessionHandle, date)
        if (!context.postingOpen) {
            throw IllegalStateException("This business day is closed. Ask Owner to reopen it before recording.")
        }
        val vendor = context.vendors.singleOrNull { it.accountId == request.vendorAccountId }
            ?: throw IllegalArgumentException("Choose an active supplier.")
        if (!context.milkProductPresent) {
            throw IllegalStateException("Ask Owner to set up the Milk product before receiving milk.")
        }
        // The mutation, required evidence and receipt reads commit together.
        // A read/authorization failure cannot report failure after saving.
        return database.withWriteTransaction {
            val result = commands.recordPurchase(
                sessionHandle,
                VendorPurchaseInput(
                    vendorAccountId = vendor.accountId,
                    occurredAt = context.businessDate,
                    rawLiters = request.rawLiters,
                    fatPercentage = request.fatPercentage,
                    lrReading = request.lrReading,
                    notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            val refreshed = load(sessionHandle, date)
            NativeReceiveMilkSubmission(
                receipt = NativeReceiveMilkReceipt(
                    eventId = result.eventId,
                    vendor = vendor,
                    rawLiters = round4(request.rawLiters),
                    fatPercentage = round4(request.fatPercentage),
                    lrReading = round4(request.lrReading),
                    snfPercentage = round4(result.snfPercentage),
                    tsPercentage = round4(result.tsPercentage),
                    adjustedLiters = round4(result.adjustedLiters),
                    milkOnHandLabel = refreshed.milkOnHandLabel,
                ),
                refreshedContext = refreshed,
            )
        }
    }

    private suspend fun activeVendors(): List<NativeReceiveMilkVendor> {
        val profiles = database.vendorProfileDao().all().associateBy { it.accountId }
        return database.accountDao().findByCategory("Vendor")
            .asSequence()
            .filter { profiles[it.accountId]?.active != false }
            .map {
                NativeReceiveMilkVendor(
                    accountId = it.accountId,
                    name = it.name.removePrefix("Vendor: ").trim(),
                )
            }
            .sortedBy { it.name.lowercase(Locale.US) }
            .toList()
    }

    private suspend fun milkOnHandLabel(productId: Long?): String {
        if (productId == null) return "Milk stock unavailable"
        val quantity = database.readModelDao().currentStock()
            .firstOrNull { it.productId == productId }
            ?.quantityOnHand
            ?: 0.0
        val rounded = BigDecimal.valueOf(quantity)
            .setScale(1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        return "$rounded L milk"
    }

    private fun round4(value: Double): Double = round(value * 10_000.0) / 10_000.0
}

data class NativeReceiveMilkVendor(
    val accountId: Long,
    val name: String,
)

data class NativeReceiveMilkContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val vendors: List<NativeReceiveMilkVendor>,
    val tsStandardPercent: Double,
    val milkProductPresent: Boolean,
    val milkOnHandLabel: String,
)

data class NativeReceiveMilkPreview(
    val snfPercentage: Double,
    val tsPercentage: Double,
    val adjustedLiters: Double,
)

data class NativeReceiveMilkRequest(
    val vendorAccountId: Long,
    val rawLiters: Double,
    val fatPercentage: Double,
    val lrReading: Double,
    val notes: String?,
)

data class NativeReceiveMilkReceipt(
    val eventId: Long,
    val vendor: NativeReceiveMilkVendor,
    val rawLiters: Double,
    val fatPercentage: Double,
    val lrReading: Double,
    val snfPercentage: Double,
    val tsPercentage: Double,
    val adjustedLiters: Double,
    val milkOnHandLabel: String,
)

data class NativeReceiveMilkSubmission(
    val receipt: NativeReceiveMilkReceipt,
    val refreshedContext: NativeReceiveMilkContext,
)
