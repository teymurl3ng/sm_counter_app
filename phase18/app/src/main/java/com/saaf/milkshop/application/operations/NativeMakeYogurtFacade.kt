package com.saaf.milkshop.application.operations

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.YogurtProductionInput
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.round
import com.saaf.milkshop.sync.BusinessClock

/**
 * Typed native boundary for Operations Make yogurt.
 *
 * Projects physical conversion only. Rates, margins, and financial loss never
 * leave this facade.
 */
class NativeMakeYogurtFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeMakeYogurtContext {
        security.requirePermission(sessionHandle, AppPermission.PRODUCTION_AND_SPOILAGE)
        val businessDate = date.toString()
        val milk = database.productDao().findByName("Milk")
        val yogurt = database.productDao().findByName("Yogurt")
        val milkOnHand = milk?.productId?.let { stockOf(it) } ?: 0.0
        val yogurtOnHand = yogurt?.productId?.let { stockOf(it) } ?: 0.0
        return NativeMakeYogurtContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            milkProductPresent = milk != null,
            yogurtProductPresent = yogurt != null,
            milkOnHandLiters = milkOnHand,
            milkOnHandLabel = formatLiters(milkOnHand),
            yogurtOnHandLabel = formatKg(yogurtOnHand),
        )
    }

    fun canRecord(
        context: NativeMakeYogurtContext,
        milkLitersUsed: Double,
        yogurtKgProduced: Double,
    ): Boolean {
        if (!context.postingOpen) return false
        if (!context.milkProductPresent || !context.yogurtProductPresent) return false
        if (!milkLitersUsed.isFinite() || milkLitersUsed <= 0.0) return false
        if (!yogurtKgProduced.isFinite() || yogurtKgProduced <= 0.0) return false
        return milkLitersUsed <= context.milkOnHandLiters + 1e-9
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeMakeYogurtRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeMakeYogurtSubmission {
        require(request.milkLitersUsed.isFinite() && request.milkLitersUsed > 0.0) {
            "Enter milk liters greater than zero."
        }
        require(request.yogurtKgProduced.isFinite() && request.yogurtKgProduced > 0.0) {
            "Enter yogurt kg greater than zero."
        }
        val context = load(sessionHandle, date)
        if (!context.postingOpen) {
            throw IllegalStateException("This business day is closed. Ask Owner to reopen it before recording.")
        }
        if (!context.milkProductPresent || !context.yogurtProductPresent) {
            throw IllegalStateException("Ask Owner to set up Milk and Yogurt products before making yogurt.")
        }
        if (request.milkLitersUsed > context.milkOnHandLiters + 1e-9) {
            throw IllegalStateException(
                "Not enough milk on hand. Available ${context.milkOnHandLabel}. Change the milk amount.",
            )
        }
        // The mutation, required evidence and receipt reads commit together.
        // A read/authorization failure cannot report failure after saving.
        return database.withWriteTransaction {
            val result = commands.recordProduction(
                sessionHandle,
                YogurtProductionInput(
                    occurredAt = context.businessDate,
                    milkLitersUsed = request.milkLitersUsed,
                    yogurtKgProduced = request.yogurtKgProduced,
                    notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            val refreshed = load(sessionHandle, date)
            NativeMakeYogurtSubmission(
                receipt = NativeMakeYogurtReceipt(
                    eventId = result.eventId,
                    milkLitersUsed = round4(result.milkLitersUsed),
                    yogurtKgProduced = round4(result.yogurtKgProduced),
                    milkOnHandLabel = refreshed.milkOnHandLabel,
                    yogurtOnHandLabel = refreshed.yogurtOnHandLabel,
                ),
                refreshedContext = refreshed,
            )
        }
    }

    private suspend fun stockOf(productId: Long): Double =
        database.readModelDao().currentStock()
            .firstOrNull { it.productId == productId }
            ?.quantityOnHand
            ?: 0.0

    private fun formatLiters(value: Double): String = "${formatQty(value)} L milk"

    private fun formatKg(value: Double): String = "${formatQty(value)} kg yogurt"

    private fun formatQty(value: Double): String =
        BigDecimal.valueOf(value)
            .setScale(1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    private fun round4(value: Double): Double = round(value * 10_000.0) / 10_000.0
}

data class NativeMakeYogurtContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val milkProductPresent: Boolean,
    val yogurtProductPresent: Boolean,
    val milkOnHandLiters: Double,
    val milkOnHandLabel: String,
    val yogurtOnHandLabel: String,
)

data class NativeMakeYogurtRequest(
    val milkLitersUsed: Double,
    val yogurtKgProduced: Double,
    val notes: String?,
)

data class NativeMakeYogurtReceipt(
    val eventId: Long,
    val milkLitersUsed: Double,
    val yogurtKgProduced: Double,
    val milkOnHandLabel: String,
    val yogurtOnHandLabel: String,
)

data class NativeMakeYogurtSubmission(
    val receipt: NativeMakeYogurtReceipt,
    val refreshedContext: NativeMakeYogurtContext,
)
