package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant

data class SalaryPaymentInput(
    val employeeAccountId: Long,
    val paymentAccountId: Long,
    val amount: Double,
    val occurredAt: String,
    val notes: String? = null,
)

data class SalaryPaymentResult(
    val eventId: Long,
    val amount: Double,
)

class SalaryPaymentService(
    private val database: MilkShopDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
) {
    suspend fun record(input: SalaryPaymentInput): SalaryPaymentResult =
        database.withWriteTransaction {
            require(input.amount > 0.0) { "Amount must be greater than zero" }

            val employee = accountDao.findById(input.employeeAccountId)
                ?: error("Salary account ${input.employeeAccountId} does not exist")
            require(employee.category == "Salary") {
                "Salary payment must use a Salary account"
            }

            val paymentAccount = accountDao.findById(input.paymentAccountId)
                ?: error("Payment account ${input.paymentAccountId} does not exist")
            require(paymentAccount.category == "Cash/Bank") {
                "Salary payment must use a Cash/Bank account"
            }

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_SALARY_PAYMENT,
                    notes = input.notes,
                    amount = input.amount,
                )
            )

            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = paymentAccount.accountId,
                    amount = -input.amount,
                )
            )

            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = employee.accountId,
                    amount = input.amount,
                )
            )

            SalaryPaymentResult(eventId = eventId, amount = input.amount)
        }

    companion object {
        const val EVENT_TYPE_SALARY_PAYMENT = "SALARY_PAYMENT"
    }
}
