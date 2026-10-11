package com.saaf.milkshop.application.operations

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.SpoilageInput
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale
import kotlin.math.round
import com.saaf.milkshop.sync.BusinessClock

/**
 * Typed native boundary for Operations Record spoilage.
 *
 * Projects physical loss only. Financial loss assessments never leave this facade.
 */
class NativeRecordSpoilageFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeRecordSpoilageContext {
        security.requirePermission(sessionHandle, AppPermission.PRODUCTION_AND_SPOILAGE)
        val businessDate = date.toString()
        return NativeRecordSpoilageContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            products = trackedProducts(),
            reasons = NativeSpoilageReason.entries.toList(),
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeRecordSpoilageRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeRecordSpoilageSubmission {
        require(request.quantity.isFinite() && request.quantity > 0.0) {
            "Enter a quantity greater than zero."
        }
        val reason = NativeSpoilageReason.entries.singleOrNull { it.code == request.reasonCode }
            ?: throw IllegalArgumentException("Choose a spoilage reason.")
        val note = request.notes?.trim().orEmpty()
        if (reason.requiresNote && note.isEmpty()) {
            throw IllegalArgumentException("Add a note when the reason is Other.")
        }
        val context = load(sessionHandle, date)
        if (!context.postingOpen) {
            throw IllegalStateException("This business day is closed. Ask Owner to reopen it before recording.")
        }
        val product = context.products.singleOrNull { it.productId == request.productId }
            ?: throw IllegalArgumentException("Choose an active product.")
        if (request.quantity > product.onHand + 1e-9) {
            throw IllegalStateException(
                "Not enough ${product.name} on hand. Available ${product.onHandLabel}. Change the quantity.",
            )
        }
        // The mutation, required evidence and receipt reads commit together.
        // A read/authorization failure cannot report failure after saving.
        return database.withWriteTransaction {
            val result = commands.recordSpoilage(
                sessionHandle,
                SpoilageInput(
                    productId = product.productId,
                    quantity = request.quantity,
                    occurredAt = context.businessDate,
                    reason = reason.code,
                    notes = when {
                        note.isNotEmpty() -> note
                        else -> reason.label
                    },
                ),
            )
            val refreshed = load(sessionHandle, date)
            val refreshedProduct = refreshed.products.singleOrNull { it.productId == product.productId }
            NativeRecordSpoilageSubmission(
                receipt = NativeRecordSpoilageReceipt(
                    eventId = result.eventId,
                    product = product.copy(
                        onHand = refreshedProduct?.onHand ?: product.onHand,
                        onHandLabel = refreshedProduct?.onHandLabel ?: product.onHandLabel,
                    ),
                    quantity = round4(result.quantity),
                    reasonCode = reason.code,
                    reasonLabel = reason.label,
                    remainingLabel = refreshedProduct?.onHandLabel ?: product.onHandLabel,
                ),
                refreshedContext = refreshed,
            )
        }
    }

    private suspend fun trackedProducts(): List<NativeSpoilageProduct> {
        val profiles = database.productProfileDao().all().associateBy { it.productId }
        val stock = database.readModelDao().currentStock().associateBy { it.productId }
        return database.productDao().all()
            .asSequence()
            .filter { product ->
                val profile = profiles[product.productId]
                profile?.archived != true && profile?.inventoryTracking != false
            }
            .map { product ->
                val onHand = stock[product.productId]?.quantityOnHand ?: 0.0
                val unit = unitFor(product.name)
                NativeSpoilageProduct(
                    productId = product.productId,
                    name = product.name,
                    unitLabel = unit,
                    onHand = onHand,
                    onHandLabel = "${formatQty(onHand)} $unit",
                )
            }
            .sortedBy { it.name.lowercase(Locale.US) }
            .toList()
    }

    private fun unitFor(name: String): String = when (name.trim().lowercase(Locale.US)) {
        "milk" -> "L"
        "yogurt" -> "kg"
        else -> "units"
    }

    private fun formatQty(value: Double): String =
        BigDecimal.valueOf(value)
            .setScale(1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    private fun round4(value: Double): Double = round(value * 10_000.0) / 10_000.0
}

enum class NativeSpoilageReason(
    val code: String,
    val label: String,
    val requiresNote: Boolean,
) {
    SPOILED("spoiled", "Spoiled / sour / curdled", false),
    SPILL("spill", "Spill", false),
    CONTAMINATION("contamination", "Contamination", false),
    POWER_FAILURE("power_failure", "Power failure", false),
    EQUIPMENT_FAILURE("equipment_failure", "Equipment failure", false),
    OTHER("other", "Other", true),
}

data class NativeSpoilageProduct(
    val productId: Long,
    val name: String,
    val unitLabel: String,
    val onHand: Double,
    val onHandLabel: String,
)

data class NativeRecordSpoilageContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val products: List<NativeSpoilageProduct>,
    val reasons: List<NativeSpoilageReason>,
)

data class NativeRecordSpoilageRequest(
    val productId: Long,
    val quantity: Double,
    val reasonCode: String,
    val notes: String?,
)

data class NativeRecordSpoilageReceipt(
    val eventId: Long,
    val product: NativeSpoilageProduct,
    val quantity: Double,
    val reasonCode: String,
    val reasonLabel: String,
    val remainingLabel: String,
)

data class NativeRecordSpoilageSubmission(
    val receipt: NativeRecordSpoilageReceipt,
    val refreshedContext: NativeRecordSpoilageContext,
)
