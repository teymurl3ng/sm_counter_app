package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.readmodels.EventHistoryDto
import com.saaf.milkshop.application.readmodels.FinancialMovementDto
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.InventoryMovementDto
import com.saaf.milkshop.application.readmodels.OriginalEventValuesDto
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SpoilageService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NativeOwnerRecordsContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val shopName: String,
)

data class NativeOwnerRecordsRow(
    val eventId: Long,
    val occurredAt: String,
    val createdAt: String,
    val eventTypeCode: String,
    val taskLabel: String,
    val identityLabel: String,
    val actorLabel: String?,
    val timeLabel: String,
    val trailingLabel: String?,
    val stateLabel: String,
    val notes: String?,
    val isCorrection: Boolean,
    val isSuperseded: Boolean,
    val correctsEventId: Long?,
)

data class NativeOwnerRecordsSnapshot(
    val context: NativeOwnerRecordsContext,
    val records: List<NativeOwnerRecordsRow>,
)

data class NativeOwnerRecordsInventoryLine(
    val productName: String,
    val quantityLabel: String,
)

data class NativeOwnerRecordsFinancialLine(
    val accountName: String,
    val amountLabel: String,
)

data class NativeOwnerRecordsDetail(
    val row: NativeOwnerRecordsRow,
    val inventoryMovements: List<NativeOwnerRecordsInventoryLine>,
    val financialMovements: List<NativeOwnerRecordsFinancialLine>?,
    val originalValueLines: List<Pair<String, String>>,
    val notes: String?,
)

/**
 * Owner Records boundary: shop-wide event history + read-only detail.
 * Does not use [com.saaf.milkshop.application.activity.NativeActivityFacade]
 * and does not invent accounting formulas or correction write UI.
 */
class NativeOwnerRecordsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
) {
    suspend fun loadHistory(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeOwnerRecordsSnapshot {
        requireOwner(sessionHandle)
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val businessDate = date.toString()
        val settings = reads.businessSettings(sessionHandle)
        val history = reads.eventHistory(sessionHandle)
        val eventsById = database.eventDao().findAllDescending().associateBy { it.eventId }
        val records = history.map { dto ->
            toRow(dto, eventsById[dto.eventId])
        }
        return NativeOwnerRecordsSnapshot(
            context = NativeOwnerRecordsContext(
                businessDate = businessDate,
                postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
                shopName = settings.store?.storeName?.takeIf { it.isNotBlank() } ?: "Saaf Milk",
            ),
            records = records,
        )
    }

    suspend fun loadDetail(
        sessionHandle: String,
        eventId: Long,
    ): NativeOwnerRecordsDetail {
        requireOwner(sessionHandle)
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val detail = reads.eventDetail(sessionHandle, eventId)
        val entity = database.eventDao().findById(eventId)
        val row = toRow(detail.event, entity)
        return NativeOwnerRecordsDetail(
            row = row,
            inventoryMovements = detail.inventoryMovements.map { it.toLine() },
            financialMovements = detail.financialMovements?.map { it.toLine() },
            originalValueLines = detail.originalValues?.toLines().orEmpty(),
            notes = detail.event.notes?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    fun filterRecords(
        records: List<NativeOwnerRecordsRow>,
        query: String,
    ): List<NativeOwnerRecordsRow> {
        val needle = query.trim().lowercase(Locale.US)
        if (needle.isEmpty()) return records
        return records.filter { row ->
            listOfNotNull(
                row.eventId.toString(),
                row.identityLabel,
                row.taskLabel,
                row.eventTypeCode,
                row.actorLabel,
                row.notes,
                row.occurredAt,
                row.createdAt,
                row.trailingLabel,
                row.stateLabel,
            ).any { it.lowercase(Locale.US).contains(needle) }
        }
    }

    private suspend fun requireOwner(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Records is only available to Owner."
        }
    }

    private fun toRow(dto: EventHistoryDto, entity: EventEntity?): NativeOwnerRecordsRow {
        val taskLabel = taskLabel(dto.eventTypeCode)
        val identityLabel = identityLabel(dto.eventTypeCode, dto.eventId)
        val stateLabel = when {
            dto.isSuperseded -> "Superseded"
            dto.isCorrection -> "Correction"
            else -> "Recorded"
        }
        return NativeOwnerRecordsRow(
            eventId = dto.eventId,
            occurredAt = dto.occurredAt,
            createdAt = dto.createdAt,
            eventTypeCode = dto.eventTypeCode,
            taskLabel = taskLabel,
            identityLabel = identityLabel,
            actorLabel = dto.actor?.displayName?.takeIf { it.isNotBlank() },
            timeLabel = formatClock(dto.createdAt, dto.occurredAt),
            trailingLabel = trailingLabel(dto.eventTypeCode, entity),
            stateLabel = stateLabel,
            notes = dto.notes?.trim()?.takeIf { it.isNotEmpty() },
            isCorrection = dto.isCorrection,
            isSuperseded = dto.isSuperseded,
            correctsEventId = dto.correctsEventId,
        )
    }

    private fun taskLabel(eventType: String): String = when (eventType) {
        SaleService.EVENT_TYPE_SALE -> "Sale"
        CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION -> "Collection"
        VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> "Milk receipt"
        YogurtProductionService.EVENT_TYPE_PRODUCTION -> "Yogurt batch"
        SpoilageService.EVENT_TYPE_SPOILAGE -> "Spoilage"
        ExpenseService.EVENT_TYPE_EXPENSE -> "Expense"
        VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT -> "Supplier payment"
        SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT -> "Salary payment"
        else -> eventType.replace('_', ' ').lowercase(Locale.US)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }

    private fun identityLabel(eventType: String, eventId: Long): String {
        val prefix = when (eventType) {
            SaleService.EVENT_TYPE_SALE -> "S"
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION -> "C"
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> "R"
            YogurtProductionService.EVENT_TYPE_PRODUCTION -> "Y"
            SpoilageService.EVENT_TYPE_SPOILAGE -> "P"
            else -> null
        }
        return if (prefix != null) "#$prefix-$eventId" else "#$eventId"
    }

    private fun trailingLabel(eventType: String, entity: EventEntity?): String? {
        entity ?: return null
        return when (eventType) {
            SaleService.EVENT_TYPE_SALE -> {
                val total = (entity.saleValue ?: 0.0) + entity.surchargeApplied
                "PKR ${formatMoney(money(total))}"
            }
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION,
            ExpenseService.EVENT_TYPE_EXPENSE,
            VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT,
            SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT,
            -> entity.amount?.let { "PKR ${formatMoney(money(it))}" }
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT ->
                entity.rawLiters?.let { "${formatQuantity(it)} L" }
            YogurtProductionService.EVENT_TYPE_PRODUCTION ->
                entity.yogurtKgProduced?.let { "${formatQuantity(it)} kg" }
            SpoilageService.EVENT_TYPE_SPOILAGE ->
                entity.saleQuantity?.let { formatQuantity(it) }
            else -> null
        }
    }

    private fun InventoryMovementDto.toLine() = NativeOwnerRecordsInventoryLine(
        productName = productName,
        quantityLabel = listOf(quantity, unit).filter { it.isNotBlank() }.joinToString(" "),
    )

    private fun FinancialMovementDto.toLine() = NativeOwnerRecordsFinancialLine(
        accountName = accountName,
        amountLabel = "PKR ${formatMoney(amountPaisa)}",
    )

    private fun OriginalEventValuesDto.toLines(): List<Pair<String, String>> = buildList {
        rawLiters?.let { add("Raw liters" to it) }
        fatPercent?.let { add("Fat %" to it) }
        lrReading?.let { add("LR" to it) }
        milkLitersUsed?.let { add("Milk used" to it) }
        yogurtKgProduced?.let { add("Yogurt produced" to it) }
        quantity?.let { add("Quantity" to it) }
        unitRatePaisa?.let { add("Unit rate" to "PKR ${formatMoney(it)}") }
        valuePaisa?.let { add("Value" to "PKR ${formatMoney(it)}") }
        surchargePaisa?.let { add("Surcharge" to "PKR ${formatMoney(it)}") }
        amountPaisa?.let { add("Amount" to "PKR ${formatMoney(it)}") }
        channelCode?.let { add("Channel" to it) }
        reasonCode?.let { add("Reason" to it) }
    }

    private fun money(value: Double): Long = BigDecimal.valueOf(value)
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

    private fun formatMoney(paisa: Long): String =
        String.format(Locale.US, "%,.2f", paisa / 100.0)

    private fun formatQuantity(value: Double): String =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun formatClock(createdAt: String, occurredAt: String): String {
        val clock = runCatching {
            OffsetDateTime.parse(createdAt).toLocalTime()
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        }.getOrNull()
        return when {
            clock != null && occurredAt.isNotBlank() -> "$occurredAt · $clock"
            clock != null -> clock
            else -> occurredAt.ifBlank { createdAt }
        }
    }
}
