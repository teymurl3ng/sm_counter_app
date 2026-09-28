package com.saaf.milkshop.application.expense

import com.saaf.milkshop.application.bridge.OperationsBridgeCommandService
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

data class NativeExpenseCategory(
    val accountId: Long,
    val name: String,
)

data class NativeExpensePaymentAccount(
    val accountId: Long,
    val name: String,
)

data class NativeExpenseContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val categories: List<NativeExpenseCategory>,
    val paymentAccounts: List<NativeExpensePaymentAccount>,
    val defaultPaymentAccountId: Long?,
)

data class NativeExpenseRequest(
    val expenseCategoryAccountId: Long,
    val paymentAccountId: Long,
    val amountPaisa: Long,
    val notes: String?,
)

data class NativeExpenseReceipt(
    val eventId: Long,
    val category: NativeExpenseCategory,
    val paymentAccount: NativeExpensePaymentAccount,
    val amountPaisa: Long,
)

data class NativeExpenseSubmission(
    val receipt: NativeExpenseReceipt,
    val refreshedContext: NativeExpenseContext,
)

/**
 * Typed native boundary for Record expense.
 *
 * Categories and payment accounts come from transaction references.
 * Posting goes through [OperationsBridgeCommandService.recordExpense].
 */
class NativeExpenseFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val reads = HybridReadModelService(database, security)
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeExpenseContext {
        security.requirePermission(sessionHandle, AppPermission.EXPENSE)
        val references = reads.transactionReferences(sessionHandle)
        val categories = references.expenseCategories.map { reference ->
            NativeExpenseCategory(reference.accountId, reference.displayName)
        }
        val paymentAccounts = references.paymentAccounts.map { reference ->
            NativeExpensePaymentAccount(reference.accountId, reference.name)
        }
        val defaultAccount = paymentAccounts.firstOrNull { it.name.equals("Cash", ignoreCase = true) }
            ?: paymentAccounts.firstOrNull()
        val businessDate = date.toString()
        return NativeExpenseContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            categories = categories,
            paymentAccounts = paymentAccounts,
            defaultPaymentAccountId = defaultAccount?.accountId,
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeExpenseRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeExpenseSubmission {
        require(request.amountPaisa > 0L) { "Enter an amount greater than zero." }
        val context = load(sessionHandle, date)
        val category = context.categories.singleOrNull { it.accountId == request.expenseCategoryAccountId }
            ?: throw IllegalArgumentException("Choose an active expense category.")
        val paymentAccount = context.paymentAccounts.singleOrNull { it.accountId == request.paymentAccountId }
            ?: throw IllegalArgumentException("Choose a valid Cash/Bank account.")
        val result = commands.recordExpense(
            sessionHandle = sessionHandle,
            expenseCategoryAccountId = category.accountId,
            paymentAccountId = paymentAccount.accountId,
            amount = BigDecimal(request.amountPaisa).movePointLeft(2).toDouble(),
            occurredAt = context.businessDate,
            notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
        )
        val refreshedContext = load(sessionHandle, date)
        val refreshedCategory = refreshedContext.categories.singleOrNull {
            it.accountId == category.accountId
        } ?: category
        val refreshedPayment = refreshedContext.paymentAccounts.singleOrNull {
            it.accountId == paymentAccount.accountId
        } ?: paymentAccount
        return NativeExpenseSubmission(
            receipt = NativeExpenseReceipt(
                eventId = result.eventId,
                category = refreshedCategory,
                paymentAccount = refreshedPayment,
                amountPaisa = request.amountPaisa,
            ),
            refreshedContext = refreshedContext,
        )
    }
}
