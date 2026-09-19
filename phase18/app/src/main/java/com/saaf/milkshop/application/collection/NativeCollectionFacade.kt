package com.saaf.milkshop.application.collection

import com.saaf.milkshop.application.bridge.OperationsBridgeCommandService
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class NativeCollectionCustomer(
    val accountId: Long,
    val name: String,
    val phone: String?,
)

data class NativeCollectionPaymentAccount(
    val accountId: Long,
    val name: String,
)

data class NativeCollectionContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val customers: List<NativeCollectionCustomer>,
    val paymentAccounts: List<NativeCollectionPaymentAccount>,
    val defaultPaymentAccountId: Long?,
)

data class NativeCollectionCustomerBalance(
    val customer: NativeCollectionCustomer,
    val balancePaisa: Long,
)

data class NativeCollectionRequest(
    val customerAccountId: Long,
    val paymentAccountId: Long,
    val amountPaisa: Long,
    val notes: String?,
)

data class NativeCollectionReceipt(
    val eventId: Long,
    val customer: NativeCollectionCustomer,
    val paymentAccount: NativeCollectionPaymentAccount,
    val amountPaisa: Long,
    val customerBalanceAfterPaisa: Long,
)

data class NativeCollectionSubmission(
    val receipt: NativeCollectionReceipt,
    val refreshedContext: NativeCollectionContext,
)

/**
 * Typed native boundary for Cashier Collect.
 *
 * Customer references are loaded without balances. A balance is resolved only
 * after one active customer is selected so Cashier never receives the full
 * financial overview.
 */
class NativeCollectionFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val reads = HybridReadModelService(database, security)
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeCollectionContext {
        security.requirePermission(sessionHandle, AppPermission.SALE_AND_COLLECTION)
        val references = reads.transactionReferences(sessionHandle)
        val customerProfiles = database.customerProfileDao().all().associateBy { it.accountId }
        val customers = references.customers.map { reference ->
            NativeCollectionCustomer(
                accountId = reference.accountId,
                name = reference.name,
                phone = customerProfiles[reference.accountId]?.phone,
            )
        }
        val paymentAccounts = references.paymentAccounts.map { reference ->
            NativeCollectionPaymentAccount(reference.accountId, reference.name)
        }
        val defaultAccount = paymentAccounts.firstOrNull { it.name.equals("Cash", ignoreCase = true) }
            ?: paymentAccounts.firstOrNull()
        val businessDate = date.toString()
        return NativeCollectionContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            customers = customers,
            paymentAccounts = paymentAccounts,
            defaultPaymentAccountId = defaultAccount?.accountId,
        )
    }

    suspend fun loadCustomer(
        sessionHandle: String,
        customerAccountId: Long,
        date: LocalDate = LocalDate.now(),
    ): NativeCollectionCustomerBalance {
        val context = load(sessionHandle, date)
        val customer = context.customers.singleOrNull { it.accountId == customerAccountId }
            ?: throw IllegalArgumentException("Choose an active customer.")
        val balance = database.readModelDao().accountBalance(customerAccountId)?.balance
            ?: throw IllegalArgumentException("The selected customer balance is unavailable.")
        return NativeCollectionCustomerBalance(customer, moneyToPaisa(balance))
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeCollectionRequest,
        date: LocalDate = LocalDate.now(),
    ): NativeCollectionSubmission {
        require(request.amountPaisa > 0L) { "Enter an amount greater than zero." }
        val context = load(sessionHandle, date)
        val customer = context.customers.singleOrNull { it.accountId == request.customerAccountId }
            ?: throw IllegalArgumentException("Choose an active customer.")
        val paymentAccount = context.paymentAccounts.singleOrNull { it.accountId == request.paymentAccountId }
            ?: throw IllegalArgumentException("Choose a valid Cash/Bank account.")
        val result = commands.recordCreditCollection(
            sessionHandle,
            CreditCollectionInput(
                customerAccountId = customer.accountId,
                paymentAccountId = paymentAccount.accountId,
                amount = BigDecimal(request.amountPaisa).movePointLeft(2).toDouble(),
                occurredAt = context.businessDate,
                notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        val refreshedContext = load(sessionHandle, date)
        val refreshedCustomer = refreshedContext.customers.singleOrNull {
            it.accountId == customer.accountId
        } ?: customer
        val balanceAfter = database.readModelDao().accountBalance(customer.accountId)?.balance
            ?: throw IllegalStateException("The saved customer balance could not be verified.")
        return NativeCollectionSubmission(
            receipt = NativeCollectionReceipt(
                eventId = result.eventId,
                customer = refreshedCustomer,
                paymentAccount = paymentAccount,
                amountPaisa = request.amountPaisa,
                customerBalanceAfterPaisa = moneyToPaisa(balanceAfter),
            ),
            refreshedContext = refreshedContext,
        )
    }

    private fun moneyToPaisa(value: Double): Long = BigDecimal.valueOf(value)
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}
