package com.saaf.milkshop.application.sale

import com.saaf.milkshop.application.bridge.OperationsBridgeCommandService
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.UserSecurityService
import java.time.LocalDate

data class NativeSaleProduct(
    val productId: Long,
    val name: String,
    val unit: String,
    val ratePaisaPerUnit: Long,
    val quantityOnHand: String,
)

data class NativeSaleAccount(val accountId: Long, val name: String)

data class NativeSaleContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val products: List<NativeSaleProduct>,
    val customers: List<NativeSaleAccount>,
    val paymentAccounts: List<NativeSaleAccount>,
    val defaultPaymentAccountId: Long?,
    val defaultPaymentAccountBalancePaisa: Long?,
    val todaySalesPaisa: Long,
)

enum class NativeSaleEntryMode { QUANTITY, AMOUNT }
enum class NativeSalePaymentKind { PAID_NOW, CUSTOMER_ACCOUNT }

data class NativeSaleRequest(
    val productId: Long,
    val entryMode: NativeSaleEntryMode,
    val enteredValue: Double,
    val paymentKind: NativeSalePaymentKind,
    val paymentAccountId: Long?,
    val customerAccountId: Long?,
)

data class NativeSaleReceipt(
    val eventId: Long,
    val quantity: Double,
    val totalPaisa: Long,
)

data class NativeSaleSubmission(
    val receipt: NativeSaleReceipt,
    val refreshedContext: NativeSaleContext,
)

/** Typed native Sale boundary. Room and the existing transaction services remain authoritative. */
class NativeSaleFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val reads = HybridReadModelService(database, security)
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(sessionHandle: String, date: LocalDate = LocalDate.now()): NativeSaleContext {
        val businessDate = date.toString()
        val endDate = date.plusDays(1).toString()
        val references = reads.transactionReferences(sessionHandle)
        val dashboard = reads.dashboard(sessionHandle, businessDate, endDate)
        val cashier = reads.cashierSummary(sessionHandle, businessDate, endDate)
        val stockByProduct = dashboard.stock.associateBy { it.productId }
        val ratesByName = mapOf(
            "milk" to references.retailMilkRatePaisaPerUnit,
            "yogurt" to references.retailYogurtRatePaisaPerUnit,
        )
        val products = references.products.mapNotNull { product ->
            val rate = ratesByName[product.name.trim().lowercase()] ?: return@mapNotNull null
            NativeSaleProduct(
                productId = product.productId,
                name = product.name,
                unit = product.unit,
                ratePaisaPerUnit = rate,
                quantityOnHand = stockByProduct[product.productId]?.quantityOnHand ?: "0",
            )
        }
        val accounts = references.paymentAccounts.map { NativeSaleAccount(it.accountId, it.name) }
        val defaultAccount = accounts.firstOrNull { it.name.equals("Cash", ignoreCase = true) }
            ?: accounts.firstOrNull()
        val defaultBalance = cashier.paymentAccounts.firstOrNull {
            it.accountId == defaultAccount?.accountId
        }?.canonicalBalancePaisa
        return NativeSaleContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            products = products,
            customers = references.customers.map { NativeSaleAccount(it.accountId, it.name) },
            paymentAccounts = accounts,
            defaultPaymentAccountId = defaultAccount?.accountId,
            defaultPaymentAccountBalancePaisa = defaultBalance,
            todaySalesPaisa = cashier.salesValuePaisa,
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeSaleRequest,
        date: LocalDate = LocalDate.now(),
    ): NativeSaleSubmission {
        require(request.enteredValue.isFinite() && request.enteredValue > 0.0) {
            "Enter a sale quantity or amount greater than zero."
        }
        val context = load(sessionHandle, date)
        val product = context.products.singleOrNull { it.productId == request.productId }
            ?: throw IllegalArgumentException("Choose an active product with a configured retail rate.")
        val payment = when (request.paymentKind) {
            NativeSalePaymentKind.PAID_NOW -> {
                val accountId = request.paymentAccountId
                    ?: throw IllegalArgumentException("Choose a payment account.")
                if (context.paymentAccounts.none { it.accountId == accountId }) {
                    throw IllegalArgumentException("Choose a valid payment account.")
                }
                SalePayment.Account(accountId)
            }
            NativeSalePaymentKind.CUSTOMER_ACCOUNT -> {
                val customerId = request.customerAccountId
                    ?: throw IllegalArgumentException("Choose a customer account.")
                if (context.customers.none { it.accountId == customerId }) {
                    throw IllegalArgumentException("Choose an active customer.")
                }
                SalePayment.OnCredit(customerId)
            }
        }
        val result = commands.recordSale(
            sessionHandle,
            SaleInput(
                productId = product.productId,
                occurredAt = context.businessDate,
                channel = "retail",
                unitRate = product.ratePaisaPerUnit / 100.0,
                quantity = request.enteredValue.takeIf { request.entryMode == NativeSaleEntryMode.QUANTITY },
                value = request.enteredValue.takeIf { request.entryMode == NativeSaleEntryMode.AMOUNT },
                payment = payment,
                notes = "Recorded from native Sale",
            ),
        )
        return NativeSaleSubmission(
            receipt = NativeSaleReceipt(
                eventId = result.eventId,
                quantity = result.quantity,
                totalPaisa = moneyToPaisa(result.total),
            ),
            refreshedContext = load(sessionHandle, date),
        )
    }

    private fun moneyToPaisa(value: Double): Long = kotlin.math.round(value * 100.0).toLong()
}
