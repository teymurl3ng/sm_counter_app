package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountDao
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogDao
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.OwnerFundsDetailEntity
import java.time.Instant

/**
 * M18: the owner takes cash out of the drawer for non-shop purposes. There is
 * deliberately NO counter-leg account — a drawing is an equity withdrawal, not
 * an expense — so the single negative movement on the drawer plus the event's
 * amount/notes ARE the record. The schema trigger (`validate_financial_movement`)
 * enforces the Cash/Bank + negative-movement shape; reports exclude the type
 * from operating expenses by not listing it in their CASE sums.
 *
 * Recording mistakes are corrected through CorrectionInput.OwnerDrawing on the
 * declared account. Cancellation is a linked zero-amount corrected truth; an
 * actual cash return is a distinct operation and is not supported by cancellation.
 */
data class OwnerDrawingInput(
    val amount: Double,
    val paymentAccountId: Long? = null,
    val occurredAt: String,
    val notes: String? = null,
)

data class OwnerDrawingResult(
    val eventId: Long,
    val amount: Double,
)

class OwnerDrawingService(
    private val database: MilkShopDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val financialLogDao: FinancialLogDao = database.financialLogDao(),
) {
    suspend fun record(input: OwnerDrawingInput): OwnerDrawingResult =
        database.withWriteTransaction {
            require(input.amount > 0.0) { "Amount must be greater than zero" }
            val notes = input.notes?.trim()?.takeIf { it.isNotEmpty() }
            val paymentAccount = if (input.paymentAccountId != null) {
                accountDao.findById(input.paymentAccountId)
                    ?: error("Payment account ${input.paymentAccountId} does not exist")
            } else {
                accountDao.findByCategory("Cash/Bank").singleOrNull { it.name == "Cash" }
                    ?: error("The Cash drawer account is unavailable")
            }
            require(paymentAccount.category == "Cash/Bank") {
                "Owner drawings must leave a Cash/Bank account"
            }

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_OWNER_DRAWING,
                    amount = input.amount,
                    notes = notes,
                ),
            )
            database.ownerFundsDao().insert(OwnerFundsDetailEntity(eventId, paymentAccount.accountId, eventId))
            financialLogDao.insert(
                FinancialLogEntity(
                    eventId = eventId,
                    accountId = paymentAccount.accountId,
                    amount = -input.amount,
                ),
            )
            OwnerFundsTruth(database).resolve(eventDao.findById(eventId)!!)
            OwnerDrawingResult(eventId = eventId, amount = input.amount)
        }

    companion object {
        const val EVENT_TYPE_OWNER_DRAWING = "OWNER_DRAWING"
    }
}
