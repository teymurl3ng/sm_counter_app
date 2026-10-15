package com.saaf.milkshop.application.sale

import androidx.room3.withWriteTransaction
import androidx.room3.withReadTransaction
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.money as moneyToPaisa
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.UserSecurityService
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock
import com.saaf.milkshop.domain.SaleAmounts
import com.saaf.milkshop.domain.CustomerCreditAgeService
import com.saaf.milkshop.domain.CreditAge
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest

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
    val phone: String? = null,
    val areaRoute: String? = null,
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
    /** Transient acknowledgement of one reviewed local snapshot; never stored or synced. */
    val creditAcknowledgment: String? = null,
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

interface NativeSaleOperations {
    suspend fun previewCredit(sessionHandle: String, request: NativeSaleRequest, date: LocalDate = BusinessClock.today()): NativeSaleCreditSummary? = null
    suspend fun load(sessionHandle: String, date: LocalDate = BusinessClock.today()): NativeSaleContext
    suspend fun record(sessionHandle: String, request: NativeSaleRequest, date: LocalDate = BusinessClock.today()): NativeSaleSubmission
}

/** Typed native Sale boundary. Room and the existing transaction services remain authoritative. */
class NativeSaleFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) : NativeSaleOperations {
    private val reads = HybridReadModelService(database, security)
    private val commands = OperationsBridgeCommandService(database, security)

    override suspend fun load(sessionHandle: String, date: LocalDate): NativeSaleContext {
        val businessDate = date.toString()
        val endDate = date.plusDays(1).toString()
        val references = reads.transactionReferences(sessionHandle)
        val dashboard = reads.dashboard(sessionHandle, businessDate, endDate)
        val cashier = reads.cashierSummary(sessionHandle, businessDate, endDate)
        val stockByProduct = dashboard.stock.associateBy { it.productId }
        val profiles = database.productProfileDao().all().associateBy { it.productId }
        val customerProfiles = database.customerProfileDao().all().associateBy { it.accountId }
        val products = references.products.map { product ->
            // Catalog prices belong to the stable product ID, never a name prefix.
            // Keep the two original settings as fallback for legacy seeded items.
            val catalogRate = profiles[product.productId]?.defaultSaleRate
                ?.let(::moneyToPaisa)
            val legacyRate = when (product.name.trim().lowercase(java.util.Locale.ROOT)) {
                "milk" -> references.retailMilkRatePaisaPerUnit
                "yogurt" -> references.retailYogurtRatePaisaPerUnit
                else -> null
            }
            NativeSaleProduct(
                productId = product.productId,
                name = product.name,
                unit = product.unit,
                ratePaisaPerUnit = catalogRate ?: legacyRate ?: 0L,
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
            customers = references.customers.map { NativeSaleAccount(it.accountId, it.name,
                phone = customerProfiles[it.accountId]?.phone, areaRoute = customerProfiles[it.accountId]?.areaRoute) },
            paymentAccounts = accounts,
            defaultPaymentAccountId = defaultAccount?.accountId,
            defaultPaymentAccountBalancePaisa = defaultBalance,
            todaySalesPaisa = cashier.salesValuePaisa,
            defaultSurchargeRs = database.settingsDao()
                .getValue(com.saaf.milkshop.domain.SettingKeys.DEFAULT_DELIVERY_SURCHARGE)
                ?: 50.0,
        )
    }

    override suspend fun record(
        sessionHandle: String,
        request: NativeSaleRequest,
        date: LocalDate,
    ): NativeSaleSubmission {
        security.requirePermission(sessionHandle, com.saaf.milkshop.domain.AppPermission.SALE_AND_COLLECTION)
        require(request.enteredValue.isFinite() && request.enteredValue > 0.0) {
            "Enter a sale quantity or amount greater than zero."
        }
        return database.withWriteTransaction {
            val context = load(sessionHandle, date)
            require(context.postingOpen) { "The business day is closed. Reopen it before recording a sale." }
            val product = context.products.singleOrNull { it.productId == request.productId }
                ?: throw IllegalArgumentException("Choose an active product.")
            require(product.ratePaisaPerUnit > 0L || request.wholesale) {
                "Set a sale price for ${product.name} in Products before selling it."
            }
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
            // The mutation, required evidence and receipt reads commit together.
            // A read/authorization failure cannot report failure after saving.
            if (request.paymentKind == NativeSalePaymentKind.CUSTOMER_ACCOUNT) {
                val credit = creditSummary(context, request, date)
                if (credit.warningKey != null && credit.warningKey != request.creditAcknowledgment) {
                    throw CreditWarningRequired(NativeSaleCreditWarning(request.copy(creditAcknowledgment = null), credit))
                }
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
            NativeSaleSubmission(
                receipt = NativeSaleReceipt(
                    eventId = result.eventId,
                    quantity = result.quantity,
                    totalPaisa = moneyToPaisa(result.total),
                ),
                refreshedContext = load(sessionHandle, date),
            )
        }
    }

    override suspend fun previewCredit(sessionHandle: String, request: NativeSaleRequest, date: LocalDate): NativeSaleCreditSummary =
        database.withReadTransaction {
            security.requirePermission(sessionHandle, com.saaf.milkshop.domain.AppPermission.SALE_AND_COLLECTION)
            creditSummary(load(sessionHandle, date), request, date)
        }

    private suspend fun creditSummary(context: NativeSaleContext, request: NativeSaleRequest, date: LocalDate): NativeSaleCreditSummary {
        val customer = context.customers.singleOrNull { it.accountId == request.customerAccountId }
            ?: throw IllegalArgumentException("Choose an active customer.")
        val product = context.products.singleOrNull { it.productId == request.productId }
            ?: throw IllegalArgumentException("Choose an active product.")
        val rate = if (request.wholesale) requireNotNull(request.wholesaleRatePaisa) / 100.0 else product.ratePaisaPerUnit / 100.0
        val surcharge = if (request.surchargeEnabled) request.surchargeAmountRs ?: context.defaultSurchargeRs else 0.0
        val amounts = if (request.enteredValue == 0.0) SaleAmounts(0.0, 0.0, 0.0) else SaleAmounts.calculate(rate,
            request.enteredValue.takeIf { request.entryMode == NativeSaleEntryMode.QUANTITY },
            request.enteredValue.takeIf { request.entryMode == NativeSaleEntryMode.AMOUNT }, surcharge)
        var balance: Long? = null
        var limit: Long? = null
        var projected: Long? = null
        var raw: Double? = null
        var rawLimit: Double? = null
        var unavailable = false
        try {
            rawLimit = database.customerProfileDao().findByAccountId(customer.accountId)?.creditLimit
            require(rawLimit == null || (rawLimit.isFinite() && rawLimit >= 0))
            limit = rawLimit?.let(::moneyToPaisa)
            balance = moneyToPaisa(requireNotNull(database.readModelDao().accountBalance(customer.accountId)).balance)
            raw = database.financialLogDao().rawAccountBalance(customer.accountId)
            require(raw.isFinite())
            projected = moneyToPaisa(database.financialLogDao().projectedAccountBalance(customer.accountId, amounts.total))
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { unavailable = true }
        val age = balance?.let { CustomerCreditAgeService(database).load(customer.accountId, it, date) } ?: CreditAge(false)
        val needsWarning = unavailable || (request.enteredValue > 0 && limit != null && projected != null && projected > limit)
        val creditState = database.creditMetadataDao().state()
        val customerUid = requireNotNull(database.accountDao().findById(customer.accountId)).uid
        val termRevision = database.creditMetadataDao().head(customerUid)?.let { database.creditMetadataDao().revision(it.revisionUid) }
        val key = if (!needsWarning) null else {
            val snapshot = listOf(customer.accountId, customer.name, date, raw?.toBits(), rawLimit?.toBits(),
                projected, unavailable, rate.toBits(), amounts.total.toBits(), creditState?.shopUid, creditState?.enabled,
                creditState?.contractVersion, termRevision?.revisionUid, termRevision?.termKind, termRevision?.termValue,
                request.copy(creditAcknowledgment = null)).joinToString("|")
            MessageDigest.getInstance("SHA-256").digest(snapshot.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
        val review = balance?.let { com.saaf.milkshop.application.owner.CustomerCreditViews(database).load(mapOf(customer.accountId to (it/100.0)),date)[customer.accountId] }
        return NativeSaleCreditSummary(customer.accountId, customer.name, balance, limit, projected,
            moneyToPaisa(amounts.total), age, unavailable, key,review?.term?.label,if(balance==null) "Needs review" else review?.label,
            if(balance==null) "Balance unavailable. Review records before saving." else review?.explanation)
    }

}
