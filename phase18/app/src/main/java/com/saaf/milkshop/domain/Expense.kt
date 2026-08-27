package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant

data class ExpenseInput(
    val category: String,
    val paymentAccountId: Long,
    val amount: Double,
    val occurredAt: String,
    val notes: String? = null,
)

data class ExpenseResult(
    val eventId: Long,
    val amount: Double,
)

class ExpenseService(
    private val database: MilkShopDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
) {
    suspend fun record(input: ExpenseInput): ExpenseResult =
        database.withWriteTransaction {
            require(input.amount > 0.0) { "Amount must be greater than zero" }
            val categoryProfile = database.expenseCategoryProfileDao()
                .findByDisplayName(input.category.trim())
                ?: error("Expense category ${input.category} does not exist")
            require(categoryProfile.active) { "Expense category ${input.category} is archived" }

            val paymentAccount = accountDao.findById(input.paymentAccountId)
                ?: error("Payment account ${input.paymentAccountId} does not exist")
            require(paymentAccount.category == "Cash/Bank") {
                "Expense payment must use a Cash/Bank account"
            }

            val expenseAccount = accountDao.findById(categoryProfile.accountId)
                ?: error("Expense account for ${input.category} does not exist")
            require(expenseAccount.category == "Operating Expense") {
                "Expense category is not backed by an Operating Expense account"
            }

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_EXPENSE,
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
                    accountId = expenseAccount.accountId,
                    amount = input.amount,
                )
            )

            ExpenseResult(eventId = eventId, amount = input.amount)
        }

    companion object {
        const val EVENT_TYPE_EXPENSE = "EXPENSE"
        val EXPENSE_CATEGORIES = listOf(
            "Rent",
            "Electricity",
            "Internet",
            "Water",
            "Fuel",
            "Poly bags",
            "LPG",
            "Other",
        )
    }
}
