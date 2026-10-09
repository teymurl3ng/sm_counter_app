package com.saaf.milkshop.application.sale

import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.UserSecurityService
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

data class NativeSaleProduct(
    val productId: Long,
    val name: String,
    val unit: String,
    val ratePaisaPerUnit: Long,
    val quantityOnHand: String,
)

data class NativeSaleAccount(
    val accountId: Long,
    val name: String,
    /** Current balance for Cash/Bank payment accounts; null for customers. */
    val balancePaisa: Long? = null,
)

data class NativeSaleContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val products: List<NativeSaleProduct>,
    val customers: List<NativeSaleAccount>,
    val paymentAccounts: List<NativeSaleAccount>,
    val defaultPaymentAccountId: Long?,
    val defaultPaymentAccountBalancePaisa: Long?,
    val todaySalesPaisa: Long,
    /** H7: the shop's default Delivery Surcharge (spec §7), rupees. */
    val defaultSurchargeRs: Double,
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
    /** Optional cashier note, persisted on the sale event. */
    val notes: String? = null,
    /** Wholesale pricing: uses the four-value channel convention and an
     *  operator-entered rate instead of the stored retail rate. */
    val wholesale: Boolean = false,
    val wholesaleRatePaisa: Long? = null,
    /** H7: Delivery Surcharge (spec §7) — optional toggle, editable amount. */
    val surchargeEnabled: Boolean = false,
    val surchargeAmountRs: Double? = null,
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

    suspend fun load(sessionHandle: String, date: LocalDate = BusinessClock.today()): NativeSaleContext {
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
        // Name-prefix match: probe renames (e.g. "Milk (Z-leg probe)") and
        // suffix variants still resolve to their retail-rate default instead
        // of silently dropping off the sale form.
        fun defaultRateFor(name: String): Long? {
            val normalized = name.trim().lowercase()
            return ratesByName[normalized]
                ?: ratesByName.entries.firstOrNull { normalized.startsWith(it.key) }?.value
        }
        val products = references.products.mapNotNull { product ->
            val rate = defaultRateFor(product.name) ?: return@mapNotNull null
            NativeSaleProduct(
                productId = product.productId,
                name = product.name,
                unit = product.unit,
                ratePaisaPerUnit = rate,
                quantityOnHand = stockByProduct[product.productId]?.quantityOnHand ?: "0",
            )
        }
        val accounts = references.paymentAccounts.map { reference ->
            val balance = cashier.paymentAccounts.firstOrNull { it.accountId == reference.accountId }
                ?.canonicalBalancePaisa
            NativeSaleAccount(reference.accountId, reference.name, balance)
        }
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
            defaultSurchargeRs = database.settingsDao()
                .getValue(com.saaf.milkshop.domain.SettingKeys.DEFAULT_DELIVERY_SURCHARGE)
                ?: 50.0,
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeSaleRequest,
        date: LocalDate = BusinessClock.today(),
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
        if (request.wholesale) {
            val rate = request.wholesaleRatePaisa
            require(rate != null && rate > 0L) {
                "Enter a wholesale rate greater than zero."
            }
        }
        // Four-value channel convention shared with the hybrid client:
        // retail / credit-retail / wholesale / credit-wholesale.
        val channel = when {
            request.wholesale && request.paymentKind == NativeSalePaymentKind.CUSTOMER_ACCOUNT -> "credit-wholesale"
            request.wholesale -> "wholesale"
            request.paymentKind == NativeSalePaymentKind.CUSTOMER_ACCOUNT -> "credit-retail"
            else -> "retail"
        }
        val unitRatePaisa = if (request.wholesale) {
            request.wholesaleRatePaisa ?: product.ratePaisaPerUnit
        } else {
            product.ratePaisaPerUnit
        }
        val result = commands.recordSale(
            sessionHandle,
            SaleInput(
                productId = product.productId,
                occurredAt = context.businessDate,
                channel = channel,
                unitRate = unitRatePaisa / 100.0,
                quantity = request.enteredValue.takeIf { request.entryMode == NativeSaleEntryMode.QUANTITY },
                value = request.enteredValue.takeIf { request.entryMode == NativeSaleEntryMode.AMOUNT },
                payment = payment,
                notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
                surchargeEnabled = request.surchargeEnabled,
                surchargeAmount = request.surchargeAmountRs,
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
