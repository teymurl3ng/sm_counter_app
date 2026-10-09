package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.commands.HistoryBridgeCommandService
import com.saaf.milkshop.application.readmodels.EventHistoryDto
import com.saaf.milkshop.application.readmodels.FinancialMovementDto
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.InventoryMovementDto
import com.saaf.milkshop.application.readmodels.OriginalEventValuesDto
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionResult
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.OwnerDrawingService
import com.saaf.milkshop.domain.RolePermissions
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
import com.saaf.milkshop.sync.BusinessClock

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
    val balances: List<NativeOwnerAccountBalanceRow> = emptyList(),
    val closeActions: List<NativeOwnerCloseActionRow> = emptyList(),
)

data class NativeOwnerAccountBalanceRow(
    val accountId: Long,
    val displayName: String,
    val categoryLabel: String,
    val balanceLabel: String,
)

data class NativeOwnerAccountLedgerEntryRow(
    val logId: Long,
    val eventId: Long,
    val occurredAt: String,
    val taskLabel: String,
    val movementLabel: String,
    val balanceAfterLabel: String,
    val stateLabel: String,
    val notes: String?,
)

data class NativeOwnerAccountLedgerDetail(
    val accountId: Long,
    val displayName: String,
    val categoryLabel: String,
    val currentBalanceLabel: String,
    val entries: List<NativeOwnerAccountLedgerEntryRow>,
)

data class NativeOwnerCloseActionRow(
    val actionId: Long,
    val businessDate: String,
    val actionLabel: String,
    val actorLabel: String,
    val createdAtLabel: String,
    val reason: String?,
)

data class NativeOwnerRecordsInventoryLine(
    val productName: String,
    val quantityLabel: String,
)

data class NativeOwnerRecordsFinancialLine(
    val accountName: String,
    val amountLabel: String,
)

enum class NativeRecordsGroup {
    Recent,
    Balances,
    Closing,
    Corrections,
}

enum class NativeCorrectionKind {
    Purchase,
    Production,
    Sale,
    Monetary,
    Spoilage,
}

data class NativeCorrectionPrefill(
    val kind: NativeCorrectionKind,
    val originalEventId: Long,
    val occurredAt: String,
    val taskLabel: String,
    val identityLabel: String,
    val notes: String,
    val purchaseRawLiters: String = "",
    val purchaseFat: String = "",
    val purchaseLr: String = "",
    val productionMilk: String = "",
    val productionYogurt: String = "",
    val saleRate: String = "",
    val saleSurcharge: String = "",
    val saleQuantity: String = "",
    val saleValue: String = "",
    val saleChannel: String = "retail",
    val monetaryAmount: String = "",
    val spoilageQuantity: String = "",
    val spoilageReason: String = "",
)

data class NativeOwnerRecordsDetail(
    val row: NativeOwnerRecordsRow,
    val inventoryMovements: List<NativeOwnerRecordsInventoryLine>,
    val financialMovements: List<NativeOwnerRecordsFinancialLine>?,
    val originalValueLines: List<Pair<String, String>>,
    val notes: String?,
    val canCorrect: Boolean,
    val correctionPrefill: NativeCorrectionPrefill?,
)

/**
 * Owner / Manager / Admin Records boundary: shop-wide event history, detail,
 * and typed corrections write (same rules as HistoryBridgeCommandService.correct).
 * Does not use [com.saaf.milkshop.application.activity.NativeActivityFacade]
 * and never calls bridge JSON.
 */
class NativeOwnerRecordsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
) {
    private val commands = HistoryBridgeCommandService(database, security)

    suspend fun loadHistory(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeOwnerRecordsSnapshot {
        requireRecordsHost(sessionHandle)
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val businessDate = date.toString()
        val history = reads.eventHistory(sessionHandle)
        val eventsById = database.eventDao().findAllDescending().associateBy { it.eventId }
        val spoilageByEvent = database.spoilageDao().all().associateBy { it.eventId }
        val productUnits = database.productDao().all().associate { it.productId to it.unit }
        fun spoilageLabel(eventId: Long): String? = spoilageByEvent[eventId]?.let { detail ->
            listOf(formatQuantity(detail.quantity), productUnits[detail.productId].orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
        // Recent-ordering follows the recording time (created_at), not the
        // business date: a just-recorded entry must be the first thing the
        // owner sees, even when seeded/history rows share the business date.
        val records = history.map { dto ->
            toRow(dto, eventsById[dto.eventId], spoilageLabel(dto.eventId))
        }.sortedWith(
            compareByDescending<NativeOwnerRecordsRow> { it.createdAt }
                .thenByDescending { it.eventId },
        )
        val balances = database.readModelDao().accountBalances()
            .filter { it.category == "Customer" || it.category == "Vendor" }
            .map { balance ->
                NativeOwnerAccountBalanceRow(
                    accountId = balance.accountId,
                    displayName = balance.name
                        .removePrefix("Customer: ")
                        .removePrefix("Vendor: ")
                        .trim(),
                    categoryLabel = balance.category,
                    balanceLabel = formatPaisa(money(balance.balance)),
                )
            }
            .sortedWith(compareBy<NativeOwnerAccountBalanceRow> { it.categoryLabel }.thenBy { it.displayName })
        val closeActions = reads.reconciliationArchive(sessionHandle).closeActions.map { action ->
            NativeOwnerCloseActionRow(
                actionId = action.actionId,
                businessDate = action.businessDate,
                actionLabel = when (action.actionCode) {
                    "close" -> "Closed"
                    "reopen" -> "Reopened"
                    else -> action.actionCode.replace('_', ' ').replaceFirstChar { it.uppercase() }
                },
                actorLabel = listOfNotNull(action.actor.displayName, action.actor.roleCode)
                    .joinToString(" · ")
                    .ifBlank { "Unknown user" },
                createdAtLabel = formatDateTime(action.createdAt),
                reason = action.reason?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
        return NativeOwnerRecordsSnapshot(
            context = NativeOwnerRecordsContext(
                businessDate = businessDate,
                postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
                shopName = database.storeProfileDao().get()?.storeName?.takeIf { it.isNotBlank() }
                    ?: "Saaf Milk",
            ),
            records = records,
            balances = balances,
            closeActions = closeActions,
        )
    }

    suspend fun loadLedger(
        sessionHandle: String,
        accountId: Long,
    ): NativeOwnerAccountLedgerDetail {
        requireRecordsHost(sessionHandle)
        val ledger = reads.accountLedger(sessionHandle, accountId)
        return NativeOwnerAccountLedgerDetail(
            accountId = ledger.accountId,
            displayName = ledger.displayName,
            categoryLabel = ledger.categoryCode,
            currentBalanceLabel = formatPaisa(ledger.currentBalancePaisa),
            entries = ledger.entries.asReversed().map { entry ->
                NativeOwnerAccountLedgerEntryRow(
                    logId = entry.logId,
                    eventId = entry.eventId,
                    occurredAt = entry.occurredAt,
                    taskLabel = taskLabel(entry.eventTypeCode),
                    movementLabel = formatPaisa(entry.movementPaisa),
                    balanceAfterLabel = formatPaisa(entry.balanceAfterPaisa),
                    stateLabel = when {
                        entry.isCorrection -> "Correction"
                        entry.isSuperseded -> "Corrected"
                        else -> "Recorded"
                    },
                    notes = entry.notes?.trim()?.takeIf { it.isNotEmpty() },
                )
            },
        )
    }

    suspend fun loadDetail(
        sessionHandle: String,
        eventId: Long,
    ): NativeOwnerRecordsDetail {
        requireRecordsHost(sessionHandle)
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val detail = reads.eventDetail(sessionHandle, eventId)
        val entity = database.eventDao().findById(eventId)
        val spoilageLabel = database.spoilageDao().findByEventId(eventId)?.let { detail ->
            val unit = database.productDao().findById(detail.productId)?.unit.orEmpty()
            listOf(formatQuantity(detail.quantity), unit)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
        val row = toRow(detail.event, entity, spoilageLabel)
        val canCorrectPermission = RolePermissions.allows(
            AppRole.parse(actor.role),
            AppPermission.CORRECTION,
        )
        val alreadyReplaced = database.eventDao().findAllDescending()
            .any { it.correctsEventId == eventId }
        val prefill = entity?.let { buildCorrectionPrefill(it, row) }
        val canCorrect = canCorrectPermission && !alreadyReplaced && prefill != null && !row.isSuperseded
        return NativeOwnerRecordsDetail(
            row = row,
            inventoryMovements = detail.inventoryMovements.map { it.toLine() },
            financialMovements = detail.financialMovements?.map { it.toLine() },
            originalValueLines = detail.originalValues?.toLines().orEmpty(),
            notes = detail.event.notes?.trim()?.takeIf { it.isNotEmpty() },
            canCorrect = canCorrect,
            correctionPrefill = prefill.takeIf { canCorrect },
        )
    }

    /**
     * Same auth / day-closed / already-replaced rules as
     * [HistoryBridgeCommandService.correct]. Typed Kotlin only — no bridge JSON.
     */
    suspend fun correct(
        sessionHandle: String,
        input: CorrectionInput,
    ): CorrectionResult {
        requireRecordsHost(sessionHandle)
        return commands.correct(sessionHandle, input)
    }

    fun filterRecords(
        records: List<NativeOwnerRecordsRow>,
        query: String,
        group: NativeRecordsGroup = NativeRecordsGroup.Recent,
    ): List<NativeOwnerRecordsRow> {
        val grouped = when (group) {
            NativeRecordsGroup.Recent -> records
            NativeRecordsGroup.Balances,
            NativeRecordsGroup.Closing,
            -> emptyList()
            NativeRecordsGroup.Corrections -> records.filter { it.isCorrection || it.isSuperseded }
        }
        val needle = query.trim().lowercase(Locale.US)
        if (needle.isEmpty()) return grouped
        return grouped.filter { row ->
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

    fun buildCorrectionInput(
        prefill: NativeCorrectionPrefill,
        notes: String?,
        purchaseRawLiters: String,
        purchaseFat: String,
        purchaseLr: String,
        productionMilk: String,
        productionYogurt: String,
        saleRate: String,
        saleSurcharge: String,
        saleQuantityMode: Boolean,
        saleQuantity: String,
        saleValue: String,
        saleChannel: String?,
        monetaryAmount: String,
        spoilageQuantity: String,
        spoilageReason: String,
    ): CorrectionInput {
        val cleanedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
        return when (prefill.kind) {
            NativeCorrectionKind.Purchase -> {
                val raw = purchaseRawLiters.toDoubleOrNull()
                    ?: error("Enter corrected raw liters.")
                val fat = purchaseFat.toDoubleOrNull()
                    ?: error("Enter corrected fat percent.")
                val lr = purchaseLr.toDoubleOrNull()
                    ?: error("Enter corrected LR reading.")
                require(raw > 0.0) { "Raw liters must be greater than zero." }
                require(fat >= 0.0) { "Fat percent cannot be negative." }
                require(lr > 0.0) { "LR reading must be greater than zero." }
                CorrectionInput.Purchase(
                    originalEventId = prefill.originalEventId,
                    occurredAt = prefill.occurredAt,
                    rawLiters = raw,
                    fatPercentage = fat,
                    lrReading = lr,
                    notes = cleanedNotes,
                )
            }
            NativeCorrectionKind.Production -> {
                val milk = productionMilk.toDoubleOrNull()
                    ?: error("Enter corrected milk liters used.")
                val yogurt = productionYogurt.toDoubleOrNull()
                    ?: error("Enter corrected yogurt produced.")
                require(milk > 0.0) { "Milk liters must be greater than zero." }
                require(yogurt > 0.0) { "Yogurt produced must be greater than zero." }
                CorrectionInput.Production(
                    originalEventId = prefill.originalEventId,
                    occurredAt = prefill.occurredAt,
                    milkLitersUsed = milk,
                    yogurtKgProduced = yogurt,
                    notes = cleanedNotes,
                )
            }
            NativeCorrectionKind.Sale -> {
                val rate = saleRate.toDoubleOrNull()
                    ?: error("Enter corrected unit rate.")
                val surcharge = saleSurcharge.toDoubleOrNull()
                    ?: error("Enter corrected surcharge.")
                require(rate > 0.0) { "Unit rate must be greater than zero." }
                require(surcharge >= 0.0) { "Surcharge cannot be negative." }
                val channel = saleChannel?.trim()?.takeIf { it.isNotEmpty() }
                if (saleQuantityMode) {
                    val quantity = saleQuantity.toDoubleOrNull()
                        ?: error("Enter corrected quantity.")
                    require(quantity > 0.0) { "Quantity must be greater than zero." }
                    CorrectionInput.Sale(
                        originalEventId = prefill.originalEventId,
                        occurredAt = prefill.occurredAt,
                        unitRate = rate,
                        quantity = quantity,
                        value = null,
                        surchargeAmount = surcharge,
                        channel = channel,
                        notes = cleanedNotes,
                    )
                } else {
                    val value = saleValue.toDoubleOrNull()
                        ?: error("Enter corrected sale value.")
                    require(value > 0.0) { "Sale value must be greater than zero." }
                    CorrectionInput.Sale(
                        originalEventId = prefill.originalEventId,
                        occurredAt = prefill.occurredAt,
                        unitRate = rate,
                        quantity = null,
                        value = value,
                        surchargeAmount = surcharge,
                        channel = channel,
                        notes = cleanedNotes,
                    )
                }
            }
            NativeCorrectionKind.Monetary -> {
                val amount = monetaryAmount.toDoubleOrNull()
                    ?: error("Enter corrected amount.")
                require(amount > 0.0) { "Amount must be greater than zero." }
                CorrectionInput.Monetary(
                    originalEventId = prefill.originalEventId,
                    occurredAt = prefill.occurredAt,
                    amount = amount,
                    notes = cleanedNotes,
                )
            }
            NativeCorrectionKind.Spoilage -> {
                val quantity = spoilageQuantity.toDoubleOrNull()
                    ?: error("Enter corrected spoilage quantity.")
                val reason = spoilageReason.trim()
                require(quantity > 0.0) { "Spoilage quantity must be greater than zero." }
                require(reason.isNotEmpty()) { "Enter a corrected spoilage reason." }
                CorrectionInput.Spoilage(
                    originalEventId = prefill.originalEventId,
                    occurredAt = prefill.occurredAt,
                    quantity = quantity,
                    reason = reason,
                    notes = cleanedNotes,
                )
            }
        }
    }

    private suspend fun requireRecordsHost(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(actor.role)
        require(role == AppRole.OWNER || role == AppRole.MANAGER || role == AppRole.ADMIN) {
            "Records is only available to Owner or Manager."
        }
    }

    private suspend fun buildCorrectionPrefill(
        entity: EventEntity,
        row: NativeOwnerRecordsRow,
    ): NativeCorrectionPrefill? {
        val kind = correctionKind(entity.eventType) ?: return null
        val notes = entity.notes.orEmpty()
        return when (kind) {
            NativeCorrectionKind.Purchase -> NativeCorrectionPrefill(
                kind = kind,
                originalEventId = entity.eventId,
                occurredAt = entity.occurredAt,
                taskLabel = row.taskLabel,
                identityLabel = row.identityLabel,
                notes = notes,
                purchaseRawLiters = formatField(entity.rawLiters),
                purchaseFat = formatField(entity.fatPercentage),
                purchaseLr = formatField(entity.lrReading),
            )
            NativeCorrectionKind.Production -> NativeCorrectionPrefill(
                kind = kind,
                originalEventId = entity.eventId,
                occurredAt = entity.occurredAt,
                taskLabel = row.taskLabel,
                identityLabel = row.identityLabel,
                notes = notes,
                productionMilk = formatField(entity.milkLitersUsed),
                productionYogurt = formatField(entity.yogurtKgProduced),
            )
            NativeCorrectionKind.Sale -> NativeCorrectionPrefill(
                kind = kind,
                originalEventId = entity.eventId,
                occurredAt = entity.occurredAt,
                taskLabel = row.taskLabel,
                identityLabel = row.identityLabel,
                notes = notes,
                saleRate = formatField(entity.unitRate),
                saleSurcharge = formatField(entity.surchargeApplied),
                saleQuantity = formatField(entity.saleQuantity),
                saleValue = formatField(entity.saleValue),
                saleChannel = entity.channel?.takeIf { it.isNotBlank() } ?: "retail",
            )
            NativeCorrectionKind.Monetary -> NativeCorrectionPrefill(
                kind = kind,
                originalEventId = entity.eventId,
                occurredAt = entity.occurredAt,
                taskLabel = row.taskLabel,
                identityLabel = row.identityLabel,
                notes = notes,
                monetaryAmount = formatField(entity.amount),
            )
            NativeCorrectionKind.Spoilage -> {
                val spoilage = database.spoilageDao().findByEventId(entity.eventId)
                NativeCorrectionPrefill(
                    kind = kind,
                    originalEventId = entity.eventId,
                    occurredAt = entity.occurredAt,
                    taskLabel = row.taskLabel,
                    identityLabel = row.identityLabel,
                    notes = notes,
                    spoilageQuantity = formatField(spoilage?.quantity),
                    spoilageReason = spoilage?.reason.orEmpty(),
                )
            }
        }
    }

    private fun correctionKind(eventType: String): NativeCorrectionKind? = when (eventType) {
        VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> NativeCorrectionKind.Purchase
        YogurtProductionService.EVENT_TYPE_PRODUCTION -> NativeCorrectionKind.Production
        SaleService.EVENT_TYPE_SALE -> NativeCorrectionKind.Sale
        CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION,
        ExpenseService.EVENT_TYPE_EXPENSE,
        SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT,
        VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT,
        -> NativeCorrectionKind.Monetary
        SpoilageService.EVENT_TYPE_SPOILAGE -> NativeCorrectionKind.Spoilage
        else -> null
    }

    private fun formatField(value: Double?): String {
        value ?: return ""
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private fun toRow(
        dto: EventHistoryDto,
        entity: EventEntity?,
        spoilageQuantityLabel: String? = null,
    ): NativeOwnerRecordsRow {
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
            trailingLabel = trailingLabel(dto.eventTypeCode, entity, spoilageQuantityLabel),
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
        OwnerDrawingService.EVENT_TYPE_OWNER_DRAWING -> "Owner drawing"
        OpeningStateService.EVENT_TYPE_OPENING_STATE -> "Opening balance"
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

    private fun trailingLabel(
        eventType: String,
        entity: EventEntity?,
        spoilageQuantityLabel: String?,
    ): String? {
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
            OwnerDrawingService.EVENT_TYPE_OWNER_DRAWING,
            -> entity.amount?.let { "PKR ${formatMoney(money(it))}" }
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT ->
                entity.rawLiters?.let { "${formatQuantity(it)} L" }
            YogurtProductionService.EVENT_TYPE_PRODUCTION ->
                entity.yogurtKgProduced?.let { "${formatQuantity(it)} kg" }
            // The spoiled quantity lives in the spoilage detail row, not on
            // the event itself — pass the prebuilt label through.
            SpoilageService.EVENT_TYPE_SPOILAGE -> spoilageQuantityLabel
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

    private fun formatPaisa(paisa: Long): String = "PKR ${formatMoney(paisa)}"

    private fun formatDateTime(value: String): String =
        com.saaf.milkshop.application.AppTimeFormats.dateTime(value)

    private fun formatQuantity(value: Double): String =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /**
     * Row timestamp: business date ("Today" / "26 Sep") plus the recording
     * clock time — never the raw ISO storage string.
     */
    private fun formatClock(createdAt: String, occurredAt: String): String {
        val clock = com.saaf.milkshop.application.AppTimeFormats.clock(createdAt)
        if (clock == createdAt) {
            // createdAt isn't a parseable instant; fall back to the raw date.
            return occurredAt.ifBlank { createdAt }
        }
        val day = formatBusinessDate(occurredAt) ?: return clock
        return "$day · $clock"
    }

    /** "Today" / "Yesterday" / "26 Sep" from a date-only string; null if unparseable. */
    private fun formatBusinessDate(value: String): String? {
        val local = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
        val today = BusinessClock.today()
        return when (local) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> local.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
        }
    }
}
