package com.saaf.milkshop.application.salarypayment

import com.saaf.milkshop.application.bridge.OperationsBridgeCommandService
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.SalaryPaymentInput
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

data class NativeSalaryPaymentEmployee(
    val accountId: Long,
    val name: String,
)

data class NativeSalaryPaymentAccount(
    val accountId: Long,
    val name: String,
)

data class NativeSalaryPaymentContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val employees: List<NativeSalaryPaymentEmployee>,
    val paymentAccounts: List<NativeSalaryPaymentAccount>,
    val defaultPaymentAccountId: Long?,
)

data class NativeSalaryPaymentRequest(
    val employeeAccountId: Long,
    val paymentAccountId: Long,
    val amountPaisa: Long,
    val notes: String?,
)

data class NativeSalaryPaymentReceipt(
    val eventId: Long,
    val employee: NativeSalaryPaymentEmployee,
    val paymentAccount: NativeSalaryPaymentAccount,
    val amountPaisa: Long,
)

data class NativeSalaryPaymentSubmission(
    val receipt: NativeSalaryPaymentReceipt,
    val refreshedContext: NativeSalaryPaymentContext,
)

/**
 * Typed native boundary for Pay salary.
 *
 * Employees and payment accounts come from transaction references.
 * Posting goes through [OperationsBridgeCommandService.recordSalaryPayment].
 */
class NativeSalaryPaymentFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val reads = HybridReadModelService(database, security)
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeSalaryPaymentContext {
        security.requirePermission(sessionHandle, AppPermission.SALARY_PAYMENT)
        val references = reads.transactionReferences(sessionHandle)
        val employees = references.employees.map { reference ->
            NativeSalaryPaymentEmployee(reference.accountId, reference.name)
        }
        val paymentAccounts = references.paymentAccounts.map { reference ->
            NativeSalaryPaymentAccount(reference.accountId, reference.name)
        }
        val defaultAccount = paymentAccounts.firstOrNull { it.name.equals("Cash", ignoreCase = true) }
            ?: paymentAccounts.firstOrNull()
        val businessDate = date.toString()
        return NativeSalaryPaymentContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            employees = employees,
            paymentAccounts = paymentAccounts,
            defaultPaymentAccountId = defaultAccount?.accountId,
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeSalaryPaymentRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeSalaryPaymentSubmission {
        require(request.amountPaisa > 0L) { "Enter an amount greater than zero." }
        val context = load(sessionHandle, date)
        val employee = context.employees.singleOrNull { it.accountId == request.employeeAccountId }
            ?: throw IllegalArgumentException("Choose an active employee.")
        val paymentAccount = context.paymentAccounts.singleOrNull { it.accountId == request.paymentAccountId }
            ?: throw IllegalArgumentException("Choose a valid Cash/Bank account.")
        val result = commands.recordSalaryPayment(
            sessionHandle = sessionHandle,
            input = SalaryPaymentInput(
                employeeAccountId = employee.accountId,
                paymentAccountId = paymentAccount.accountId,
                amount = BigDecimal(request.amountPaisa).movePointLeft(2).toDouble(),
                occurredAt = context.businessDate,
                notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        val refreshedContext = load(sessionHandle, date)
        val refreshedEmployee = refreshedContext.employees.singleOrNull {
            it.accountId == employee.accountId
        } ?: employee
        val refreshedPayment = refreshedContext.paymentAccounts.singleOrNull {
            it.accountId == paymentAccount.accountId
        } ?: paymentAccount
        return NativeSalaryPaymentSubmission(
            receipt = NativeSalaryPaymentReceipt(
                eventId = result.eventId,
                employee = refreshedEmployee,
                paymentAccount = refreshedPayment,
                amountPaisa = request.amountPaisa,
            ),
            refreshedContext = refreshedContext,
        )
    }
}
