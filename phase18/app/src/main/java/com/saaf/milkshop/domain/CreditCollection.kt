package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant

data class CreditCollectionInput(
    val customerAccountId: Long,
    val paymentAccountId: Long,
    val amount: Double,
    val occurredAt: String,
    val notes: String? = null,
)

data class CreditCollectionResult(
    val eventId: Long,
    val amount: Double,
)

class CreditCollectionService(
    private val database: MilkShopDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
) {
    suspend fun record(input: CreditCollectionInput): CreditCollectionResult =
        database.withWriteTransaction {
            require(input.amount > 0.0) { "Amount must be greater than zero" }

            val customer = accountDao.findById(input.customerAccountId)
                ?: error("Customer account ${input.customerAccountId} does not exist")
            require(customer.category == "Customer") {
                "Credit collection must use a Customer account"
            }

            val paymentAccount = accountDao.findById(input.paymentAccountId)
                ?: error("Payment account ${input.paymentAccountId} does not exist")
            require(paymentAccount.category == "Cash/Bank") {
                "Credit collection must use a Cash/Bank account"
            }

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_CREDIT_COLLECTION,
                    notes = input.notes,
                    amount = input.amount,
                )
            )

            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = customer.accountId,
                    amount = -input.amount,
                )
            )

            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = paymentAccount.accountId,
                    amount = input.amount,
                )
            )

            CreditCollectionResult(eventId = eventId, amount = input.amount)
        }

    companion object {
        const val EVENT_TYPE_CREDIT_COLLECTION = "CREDIT_COLLECTION"
    }
}
