package com.saaf.milkshop.presentation.records

import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.HistoryDataRepository
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Application-facing orchestration for History & Corrections read-side work.
 * Existing data queries and formatting semantics are preserved.
 */
class HistoryCorrectionsActions(
    private val repository: HistoryDataRepository,
    private val uiScope: CoroutineScope,
    private val historyCorrectionsUi: HistoryCorrectionsUi,
    private val viewModel: HistoryCorrectionsViewModel,
    private val onCorrectionTargetChanged: (EventEntity) -> Unit,
) {
    suspend fun refreshTransactionHistory() {
        val events = repository.allEvents()
        historyCorrectionsUi.renderHistory(events, ::formatHistoryEvent)
    }

    suspend fun showTransactionDetailById(eventId: Long) {
        val event = repository.eventById(eventId)
        if (event != null) {
            showTransactionDetail(event)
        }
    }

    fun showTransactionDetail(event: EventEntity) {
        viewModel.selectEvent(event)
        onCorrectionTargetChanged(event)
        historyCorrectionsUi.showTransactionDetailLoading(event)

        uiScope.launch {
            val detailData = repository.detailData(event.eventId)
            val inventoryLines = detailData.inventoryLines.map { line ->
                "${line.productName}: ${formatSignedQuantity(line.quantity, line.unit)}"
            }

            val financialLines = detailData.financialLines.map { line ->
                "${line.accountName}: ${line.amount.formatHistoryMoney()}"
            }

            val detail = buildTransactionDetail(event, inventoryLines, financialLines)
            viewModel.setDetail(detail)

            withContext(Dispatchers.Main) {
                historyCorrectionsUi.renderState(viewModel.state)
            }
        }
    }

    private fun buildTransactionDetail(
        event: EventEntity,
        inventoryLines: List<String>,
        financialLines: List<String>,
    ): String = buildString {
        append("Event #${event.eventId}")
        append("\nType: ${event.eventType}")
        append("\nOccurred: ${event.occurredAt}")
        append("\nCreated: ${event.createdAt}")

        event.channel?.takeIf { it.isNotBlank() }?.let {
            append("\nChannel: $it")
        }

        event.correctsEventId?.let {
            append("\nCorrection of event #$it")
        }

        event.notes?.takeIf { it.isNotBlank() }?.let {
            append("\nNote: $it")
        }

        append("\n\nRecorded values")
        when (event.eventType) {
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> {
                append("\nRaw liters: ${event.rawLiters?.formatNumber() ?: "—"} L")
                append("\nFat: ${event.fatPercentage?.formatNumber() ?: "—"} %")
                append("\nLR: ${event.lrReading?.formatNumber() ?: "—"}")
                append("\nSNF: ${event.snfPercentage?.formatNumber() ?: "—"} %")
                append("\nTS: ${event.tsPercentage?.formatNumber() ?: "—"} %")
                append("\nAdjusted liters: ${event.adjustedLiters?.formatNumber() ?: "—"} L")
                append("\nAmount owed: ${event.amountOwed?.formatHistoryMoney() ?: "—"}")
                append("\nMilk rate used: ${event.milkRateUsed?.formatHistoryMoney() ?: "—"}")
                append("\nDelivery fee used: ${event.deliveryFeeUsed?.formatHistoryMoney() ?: "—"}")
                append("\nTS standard used: ${event.tsStandardUsed?.formatNumber() ?: "—"}")
            }
            YogurtProductionService.EVENT_TYPE_PRODUCTION -> {
                append("\nMilk liters used: ${event.milkLitersUsed?.formatNumber() ?: "—"} L")
                append("\nYogurt produced: ${event.yogurtKgProduced?.formatNumber() ?: "—"} kg")
            }
            VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT,
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION,
            ExpenseService.EVENT_TYPE_EXPENSE,
            SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT -> {
                append("\nAmount: ${event.amount?.formatHistoryMoney() ?: "—"}")
            }
            SaleService.EVENT_TYPE_SALE -> {
                append("\nQuantity: ${event.saleQuantity?.formatNumber() ?: "—"}")
                append("\nSale value: ${event.saleValue?.formatHistoryMoney() ?: "—"}")
                append("\nUnit rate: ${event.unitRate?.formatHistoryMoney() ?: "—"}")
                append("\nSurcharge: ${event.surchargeApplied.formatHistoryMoney()}")
            }
            else -> {
                append("\nNo event-specific fields available.")
            }
        }

        append("\n\nInventory movements")
        if (inventoryLines.isEmpty()) {
            append("\nNone")
        } else {
            inventoryLines.forEach { line -> append("\n• $line") }
        }

        append("\n\nFinancial movements")
        if (financialLines.isEmpty()) {
            append("\nNone")
        } else {
            financialLines.forEach { line -> append("\n• $line") }
        }
    }

    private fun formatHistoryEvent(event: EventEntity): String {
        val title = when (event.eventType) {
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> "Milk Purchase"
            YogurtProductionService.EVENT_TYPE_PRODUCTION -> "Yogurt Production"
            VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT -> "Vendor Payment"
            SaleService.EVENT_TYPE_SALE -> "Sale"
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION -> "Credit Collection"
            ExpenseService.EVENT_TYPE_EXPENSE -> "Expense"
            SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT -> "Salary Payment"
            else -> event.eventType
        }

        val details = when (event.eventType) {
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT ->
                "${event.rawLiters?.formatNumber() ?: "—"} L received · Amount owed ${event.amountOwed?.formatHistoryMoney() ?: "—"}"
            YogurtProductionService.EVENT_TYPE_PRODUCTION ->
                "${event.milkLitersUsed?.formatNumber() ?: "—"} L milk used · ${event.yogurtKgProduced?.formatNumber() ?: "—"} kg yogurt"
            SaleService.EVENT_TYPE_SALE ->
                "${event.saleQuantity?.formatNumber() ?: "—"} sold · Sale value ${event.saleValue?.formatHistoryMoney() ?: "—"} · Surcharge ${event.surchargeApplied.formatHistoryMoney()}"
            VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT,
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION,
            ExpenseService.EVENT_TYPE_EXPENSE,
            SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT ->
                "Amount ${event.amount?.formatHistoryMoney() ?: "—"}"
            else -> ""
        }

        val correction = event.correctsEventId?.let { "\nCorrection of event #$it" }.orEmpty()
        val notes = event.notes?.takeIf { it.isNotBlank() }?.let { "\nNote: $it" }.orEmpty()

        return buildString {
            append("Event #${event.eventId} · $title")
            append("\n${event.occurredAt}")
            if (details.isNotBlank()) append("\n$details")
            append(correction)
            append(notes)
        }
    }

    private fun formatSignedQuantity(value: Double, unit: String): String =
        String.format(Locale.US, "%+.2f %s", value, unit).trim()

    private fun Double.formatNumber(): String = String.format(Locale.US, "%.2f", this)
    private fun Double.formatHistoryMoney(): String = "₨" + String.format(Locale.US, "%.2f", this)
}
