package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogDao
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductDao
import com.saaf.milkshop.data.SpoilageDao
import com.saaf.milkshop.data.SpoilageDetailEntity
import java.time.Instant

/**
 * Phase 3 correction inputs. The correction event stores the corrected truth;
 * the ledger rows written by this service are the delta from that truth to the
 * event being corrected.
 */
sealed interface CorrectionInput {
    val originalEventId: Long
    val occurredAt: String
    val notes: String?

    data class Purchase(
        override val originalEventId: Long,
        override val occurredAt: String,
        val rawLiters: Double,
        val fatPercentage: Double,
        val lrReading: Double,
        override val notes: String? = null,
    ) : CorrectionInput

    data class Production(
        override val originalEventId: Long,
        override val occurredAt: String,
        val milkLitersUsed: Double,
        val yogurtKgProduced: Double,
        override val notes: String? = null,
    ) : CorrectionInput

    data class Sale(
        override val originalEventId: Long,
        override val occurredAt: String,
        val unitRate: Double,
        val quantity: Double? = null,
        val value: Double? = null,
        val surchargeAmount: Double = 0.0,
        val channel: String? = null,
        override val notes: String? = null,
    ) : CorrectionInput

    data class Monetary(
        override val originalEventId: Long,
        override val occurredAt: String,
        val amount: Double,
        override val notes: String? = null,
    ) : CorrectionInput

    data class Spoilage(
        override val originalEventId: Long,
        override val occurredAt: String,
        val quantity: Double,
        val reason: String,
        override val notes: String? = null,
    ) : CorrectionInput
}

data class CorrectionResult(
    val eventId: Long,
    val eventType: String,
)

class CorrectionService(
    private val database: MilkShopDatabase,
    private val productDao: ProductDao = database.productDao(),
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val inventoryLogDao: InventoryLogDao = database.inventoryLogDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
    private val spoilageDao: SpoilageDao = database.spoilageDao(),
) {
    suspend fun correct(input: CorrectionInput): CorrectionResult =
        database.withWriteTransaction {
            val original = eventDao.findById(input.originalEventId)
                ?: error("Event ${input.originalEventId} does not exist")

            when (input) {
                is CorrectionInput.Purchase -> correctPurchase(input, original)
                is CorrectionInput.Production -> correctProduction(input, original)
                is CorrectionInput.Sale -> correctSale(input, original)
                is CorrectionInput.Monetary -> correctMonetary(input, original)
                is CorrectionInput.Spoilage -> correctSpoilage(input, original)
            }
        }

    private suspend fun correctPurchase(
        input: CorrectionInput.Purchase,
        original: EventEntity,
    ): CorrectionResult {
        require(original.eventType == EVENT_TYPE_PURCHASE) {
            "Purchase correction must reference a PURCHASE_RECEIPT event"
        }
        require(original.amountOwed != null) { "Original purchase has no amount owed" }
        require(original.rawLiters != null) { "Original purchase has no raw liters" }
        require(original.milkRateUsed != null) { "Original purchase has no frozen milk rate" }
        require(original.deliveryFeeUsed != null) { "Original purchase has no frozen delivery fee" }
        require(original.tsStandardUsed != null) { "Original purchase has no frozen TS standard" }

        val calculation = VendorPurchaseCalculator.calculate(
            rawLiters = input.rawLiters,
            fatPercentage = input.fatPercentage,
            lrReading = input.lrReading,
            milkRate = original.milkRateUsed,
            deliveryFee = original.deliveryFeeUsed,
            tsStandard = original.tsStandardUsed,
        )

        val originalFinancial = findAuthoritativeFinancialEntries(original)
        require(originalFinancial.size == 1) { "Purchase must resolve to exactly one financial movement" }
        val milk = productDao.findByName("Milk") ?: error("Seeded Milk product is missing")

        val correctionEventId = eventDao.insert(
            EventEntity(
                createdAt = Instant.now().toString(),
                occurredAt = input.occurredAt,
                eventType = EVENT_TYPE_PURCHASE,
                correctsEventId = original.eventId,
                rawLiters = input.rawLiters,
                fatPercentage = input.fatPercentage,
                lrReading = input.lrReading,
                snfPercentage = calculation.snfPercentage,
                tsPercentage = calculation.tsPercentage,
                adjustedLiters = calculation.adjustedLiters,
                amountOwed = calculation.amountOwed,
                milkRateUsed = original.milkRateUsed,
                deliveryFeeUsed = original.deliveryFeeUsed,
                tsStandardUsed = original.tsStandardUsed,
                notes = input.notes,
            ),
        )

        val originalFinancialEntry = originalFinancial.single()
        val correctedInventory = input.rawLiters
        val correctedFinancial = calculation.amountOwed

        val inventoryDelta = correctedInventory - original.rawLiters
        if (inventoryDelta != 0.0) {
            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = correctionEventId,
                    productId = milk.productId,
                    quantity = inventoryDelta,
                ),
            )
        }

        val financialDelta = correctedFinancial - original.amountOwed
        if (financialDelta != 0.0) {
            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = correctionEventId,
                    accountId = originalFinancialEntry.accountId,
                    amount = financialDelta,
                ),
            )
        }

        return CorrectionResult(correctionEventId, EVENT_TYPE_PURCHASE)
    }

    private suspend fun correctProduction(
        input: CorrectionInput.Production,
        original: EventEntity,
    ): CorrectionResult {
        require(original.eventType == EVENT_TYPE_PRODUCTION) {
            "Production correction must reference a PRODUCTION event"
        }
        require(original.milkLitersUsed != null) { "Original production has no milk measurement" }
        require(original.yogurtKgProduced != null) { "Original production has no yogurt measurement" }

        val milk = productDao.findByName("Milk") ?: error("Seeded Milk product is missing")
        val yogurt = productDao.findByName("Yogurt") ?: error("Seeded Yogurt product is missing")

        val correctionEventId = eventDao.insert(
            EventEntity(
                createdAt = Instant.now().toString(),
                occurredAt = input.occurredAt,
                eventType = EVENT_TYPE_PRODUCTION,
                correctsEventId = original.eventId,
                milkLitersUsed = input.milkLitersUsed,
                yogurtKgProduced = input.yogurtKgProduced,
                notes = input.notes,
            ),
        )

        val correctedMilkMovement = -input.milkLitersUsed
        val correctedYogurtMovement = input.yogurtKgProduced
        val milkDelta = correctedMilkMovement - (-original.milkLitersUsed)
        val yogurtDelta = correctedYogurtMovement - original.yogurtKgProduced

        if (milkDelta != 0.0) {
            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = correctionEventId,
                    productId = milk.productId,
                    quantity = milkDelta,
                ),
            )
        }
        if (yogurtDelta != 0.0) {
            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = correctionEventId,
                    productId = yogurt.productId,
                    quantity = yogurtDelta,
                ),
            )
        }

        return CorrectionResult(correctionEventId, EVENT_TYPE_PRODUCTION)
    }

    private suspend fun correctSale(
        input: CorrectionInput.Sale,
        original: EventEntity,
    ): CorrectionResult {
        require(original.eventType == EVENT_TYPE_SALE) {
            "Sale correction must reference a SALE event"
        }
        require(original.saleQuantity != null) { "Original sale has no quantity" }
        require(original.saleValue != null) { "Original sale has no value" }
        require(original.unitRate != null) { "Original sale has no rate" }

        val hasQuantity = input.quantity != null
        val hasValue = input.value != null
        require(hasQuantity.xor(hasValue)) {
            "Enter exactly one of quantity or value for the corrected sale"
        }
        require(input.unitRate > 0.0) { "Rate must be greater than zero" }
        require(input.surchargeAmount >= 0.0) { "Surcharge cannot be negative" }

        val correctedQuantity: Double
        val correctedValue: Double
        if (input.quantity != null) {
            require(input.quantity > 0.0) { "Quantity must be greater than zero" }
            correctedQuantity = input.quantity
            correctedValue = correctedQuantity * input.unitRate
        } else {
            require(input.value!! > 0.0) { "Value must be greater than zero" }
            correctedValue = input.value
            correctedQuantity = correctedValue / input.unitRate
        }
        val correctedTotal = correctedValue + input.surchargeAmount

        val originalInventory = findAuthoritativeInventoryEntries(original)
        val originalFinancial = findAuthoritativeFinancialEntries(original)
        require(originalInventory.size == 1) { "Sale must resolve to exactly one inventory movement" }
        require(originalFinancial.size == 1) { "Sale must resolve to exactly one financial movement" }

        val originalInventoryEntry = originalInventory.single()
        val originalFinancialEntry = originalFinancial.single()
        val originalTotal = original.saleValue + original.surchargeApplied

        val correctionEventId = eventDao.insert(
            EventEntity(
                createdAt = Instant.now().toString(),
                occurredAt = input.occurredAt,
                eventType = EVENT_TYPE_SALE,
                channel = input.channel ?: original.channel,
                correctsEventId = original.eventId,
                saleQuantity = correctedQuantity,
                saleValue = correctedValue,
                unitRate = input.unitRate,
                surchargeApplied = input.surchargeAmount,
                notes = input.notes,
            ),
        )

        val correctedInventoryMovement = -correctedQuantity
        val inventoryDelta = correctedInventoryMovement - (-original.saleQuantity)
        if (inventoryDelta != 0.0) {
            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = correctionEventId,
                    productId = originalInventoryEntry.productId,
                    quantity = inventoryDelta,
                ),
            )
        }

        val financialDelta = correctedTotal - originalTotal
        if (financialDelta != 0.0) {
            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = correctionEventId,
                    accountId = originalFinancialEntry.accountId,
                    amount = financialDelta,
                ),
            )
        }

        return CorrectionResult(correctionEventId, EVENT_TYPE_SALE)
    }

    private suspend fun correctMonetary(
        input: CorrectionInput.Monetary,
        original: EventEntity,
    ): CorrectionResult {
        require(original.eventType in MONETARY_EVENT_TYPES) {
            "Monetary correction must reference a money-only event"
        }
        require(original.amount != null) { "Original monetary event has no amount" }
        require(input.amount > 0.0) { "Corrected amount must be greater than zero" }

        val originalFinancial = findAuthoritativeFinancialEntries(original)
        require(originalFinancial.isNotEmpty()) { "Original monetary event has no financial movements" }
        val correctedAmount = input.amount
        val amountDelta = correctedAmount - original.amount

        val correctionEventId = eventDao.insert(
            EventEntity(
                createdAt = Instant.now().toString(),
                occurredAt = input.occurredAt,
                eventType = original.eventType,
                correctsEventId = original.eventId,
                amount = correctedAmount,
                notes = input.notes,
            ),
        )

        for (entry in originalFinancial) {
            val correctionMovement = when (original.eventType) {
                EVENT_TYPE_VENDOR_PAYMENT, EVENT_TYPE_EXPENSE, EVENT_TYPE_SALARY_PAYMENT -> {
                    when (accountDao.findById(entry.accountId)?.category) {
                        "Vendor", "Cash/Bank", "Operating Expense", "Salary" -> {
                            // Every money-only event in these families has a fixed
                            // sign per account role; preserve that role and apply
                            // only the amount difference.
                            when (original.eventType) {
                                EVENT_TYPE_VENDOR_PAYMENT -> -amountDelta
                                EVENT_TYPE_EXPENSE -> if (accountDao.findById(entry.accountId)?.category == "Operating Expense") amountDelta else -amountDelta
                                EVENT_TYPE_SALARY_PAYMENT -> if (accountDao.findById(entry.accountId)?.category == "Salary") amountDelta else -amountDelta
                                else -> error("Unexpected monetary event type")
                            }
                        }
                        else -> error("Unexpected account category for correction")
                    }
                }
                EVENT_TYPE_CREDIT_COLLECTION -> {
                    when (accountDao.findById(entry.accountId)?.category) {
                        "Customer" -> -amountDelta
                        "Cash/Bank" -> amountDelta
                        else -> error("Unexpected account category for credit collection correction")
                    }
                }
                else -> error("Unsupported monetary event type")
            }

            if (correctionMovement != 0.0) {
                financialLogDao.insert(
                    FinancialLogEntity(
                        eventId = correctionEventId,
                        accountId = entry.accountId,
                        amount = correctionMovement,
                    ),
                )
            }
        }

        return CorrectionResult(correctionEventId, original.eventType)
    }

    private suspend fun correctSpoilage(
        input: CorrectionInput.Spoilage,
        original: EventEntity,
    ): CorrectionResult {
        require(original.eventType == EVENT_TYPE_SPOILAGE) {
            "Spoilage correction must reference a SPOILAGE event"
        }
        require(input.quantity > 0.0) { "Corrected spoilage quantity must be greater than zero" }
        val reason = input.reason.trim()
        require(reason.isNotEmpty()) { "Enter a corrected spoilage reason" }
        val originalDetail = spoilageDao.findByEventId(original.eventId)
            ?: error("Original spoilage event has no detail")

        val correctionEventId = eventDao.insert(
            EventEntity(
                createdAt = Instant.now().toString(),
                occurredAt = input.occurredAt,
                eventType = EVENT_TYPE_SPOILAGE,
                correctsEventId = original.eventId,
                notes = input.notes.cleanOptional(),
            )
        )
        spoilageDao.insert(
            SpoilageDetailEntity(
                eventId = correctionEventId,
                productId = originalDetail.productId,
                quantity = input.quantity,
                reason = reason,
            )
        )

        val inventoryDelta = (-input.quantity) - (-originalDetail.quantity)
        if (inventoryDelta != 0.0) {
            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = correctionEventId,
                    productId = originalDetail.productId,
                    quantity = inventoryDelta,
                )
            )
        }
        return CorrectionResult(correctionEventId, EVENT_TYPE_SPOILAGE)
    }

    /**
     * Corrections may legitimately have zero ledger rows when the corrected truth
     * equals the event they correct. For a later correction, resolve the stable
     * underlying account/product references by walking the correction chain until
     * reaching the nearest event that actually carries the movement. The current
     * event's fields remain the source of corrected truth.
     */
    private suspend fun findAuthoritativeFinancialEntries(event: EventEntity): List<FinancialLogEntity> {
        var current: EventEntity? = event
        while (current != null) {
            val entries = financialLogDao.findByEventId(current.eventId)
            if (entries.isNotEmpty()) return entries
            current = current.correctsEventId?.let { eventDao.findById(it) }
        }
        return emptyList()
    }

    private suspend fun findAuthoritativeInventoryEntries(event: EventEntity): List<InventoryLogEntity> {
        var current: EventEntity? = event
        while (current != null) {
            val entries = inventoryLogDao.findByEventId(current.eventId)
            if (entries.isNotEmpty()) return entries
            current = current.correctsEventId?.let { eventDao.findById(it) }
        }
        return emptyList()
    }

    private companion object {
        const val EVENT_TYPE_PURCHASE = "PURCHASE_RECEIPT"
        const val EVENT_TYPE_PRODUCTION = "PRODUCTION"
        const val EVENT_TYPE_SALE = "SALE"
        const val EVENT_TYPE_VENDOR_PAYMENT = "VENDOR_PAYMENT"
        const val EVENT_TYPE_CREDIT_COLLECTION = "CREDIT_COLLECTION"
        const val EVENT_TYPE_EXPENSE = "EXPENSE"
        const val EVENT_TYPE_SALARY_PAYMENT = "SALARY_PAYMENT"
        const val EVENT_TYPE_SPOILAGE = "SPOILAGE"
        val MONETARY_EVENT_TYPES = setOf(
            EVENT_TYPE_VENDOR_PAYMENT,
            EVENT_TYPE_CREDIT_COLLECTION,
            EVENT_TYPE_EXPENSE,
            EVENT_TYPE_SALARY_PAYMENT,
        )
    }
}
