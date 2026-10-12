package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.OwnerFundsDetailEntity
import com.saaf.milkshop.sync.BusinessClock
import com.saaf.milkshop.sync.BusinessDateGuard
import com.saaf.milkshop.sync.LegacyUid
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** One actual transfer back into the shop, linked to a withdrawal for context. */
data class OwnerFundsReceiptInput(
    val withdrawalRootEventId: Long,
    val amount: Double,
    val paymentAccountId: Long? = null,
    val occurredAt: String,
    val notes: String? = null,
    val submissionUid: String = LegacyUid.randomUid(),
    val reviewedWithdrawalAmount: Double? = null,
    val reviewedReceivedTotal: Double? = null,
    val acknowledgeExcess: Boolean = false,
)

data class OwnerFundsReceiptResult(val eventId: Long, val amount: Double, val alreadySaved: Boolean)

/** Review totals are observed inside the same write transaction as the event. */
internal object OwnerFundsReview {
    fun paisa(value: Double): Long {
        require(value.isFinite() && value >= 0) { "Use a finite nonnegative amount" }
        return try { BigDecimal.valueOf(value).movePointRight(2).longValueExact() }
        catch (_: ArithmeticException) { throw IllegalArgumentException("Use exact paisa within the supported range") }
    }

    fun requireCurrent(
        withdrawal: Double, received: Double, proposedReceived: Double,
        expectedWithdrawal: Double?, expectedReceived: Double?, acknowledged: Boolean,
        proposedWithdrawal: Double = withdrawal,
    ) {
        val currentWithdrawal = paisa(withdrawal)
        val currentReceived = paisa(received)
        val proposed = paisa(proposedReceived)
        if (expectedWithdrawal != null || expectedReceived != null) {
            require(expectedWithdrawal != null && expectedReceived != null &&
                paisa(expectedWithdrawal) == currentWithdrawal && paisa(expectedReceived) == currentReceived) {
                "Owner funds changed; review the current withdrawal and received total again"
            }
        }
        if (proposed > paisa(proposedWithdrawal)) require(acknowledged && expectedWithdrawal != null && expectedReceived != null) {
            "Review and acknowledge money received above the corrected withdrawal"
        }
    }
}

class OwnerFundsReceiptService(private val database: MilkShopDatabase) {
    suspend fun record(input: OwnerFundsReceiptInput): OwnerFundsReceiptResult = database.withWriteTransaction {
        OwnerFundsReview.paisa(input.amount).also { require(it > 0) { "Received amount must be positive" } }
        require(input.submissionUid.isNotBlank()) { "Receipt submission ID is missing" }
        val root = database.eventDao().findById(input.withdrawalRootEventId)
            ?: error("Referenced withdrawal does not exist")
        require(root.eventType == OwnerDrawingService.EVENT_TYPE_OWNER_DRAWING && root.correctsEventId == null) {
            "Reference an original withdrawal"
        }
        val account = if (input.paymentAccountId != null) database.accountDao().findById(input.paymentAccountId)
            else database.accountDao().findByCategory("Cash/Bank").singleOrNull { it.name == "Cash" }
        require(account?.category == "Cash/Bank") { "Choose an existing Cash/Bank receiving account" }
        val note = input.notes?.trim()?.takeIf { it.isNotEmpty() }

        val existing = database.syncEnvelopeDao().eventsByUid(listOf(input.submissionUid)).firstOrNull()
        if (existing != null) {
            val detail = database.ownerFundsDao().find(existing.eventId)
            require(existing.eventType == EVENT_TYPE && existing.correctsEventId == null &&
                existing.occurredAt == input.occurredAt && existing.amount == input.amount &&
                existing.notes == note && detail?.withdrawalRootEventId == root.eventId &&
                detail.paymentAccountId == account.accountId &&
                detail.reviewedWithdrawalAmount == input.reviewedWithdrawalAmount &&
                detail.reviewedReceivedTotal == input.reviewedReceivedTotal &&
                detail.excessAcknowledged == input.acknowledgeExcess) {
                "Receipt submission ID belongs to different facts"
            }
            OwnerFundsTruth(database).resolve(existing)
            return@withWriteTransaction OwnerFundsReceiptResult(existing.eventId, input.amount, true)
        }

        val date = LocalDate.parse(input.occurredAt)
        require(!date.isAfter(BusinessClock.today()) && !date.isBefore(LocalDate.parse(root.occurredAt))) {
            "Receipt date must be on or after the withdrawal and not in the future"
        }
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt)
        require(!database.dailyCloseDao().isPostingClosed(input.occurredAt)) { "The receiving date is closed" }

        val truth = OwnerFundsTruth(database)
        truth.ensureDetails(root)
        val (withdrawal, received) = truth.currentTotals(root.eventId)
        require(withdrawal > 0) { "A cancelled withdrawal cannot receive a new linked receipt" }
        val proposed = BigDecimal.valueOf(received).add(BigDecimal.valueOf(input.amount)).toDouble()
        OwnerFundsReview.requireCurrent(withdrawal, received, proposed,
            input.reviewedWithdrawalAmount, input.reviewedReceivedTotal, input.acknowledgeExcess)
        val id = database.eventDao().insert(EventEntity(
            createdAt = Instant.now().toString(), occurredAt = input.occurredAt,
            eventType = EVENT_TYPE, amount = input.amount, notes = note, eventUid = input.submissionUid,
        ))
        database.ownerFundsDao().insert(OwnerFundsDetailEntity(id, account.accountId, root.eventId,
            input.reviewedWithdrawalAmount, input.reviewedReceivedTotal, input.acknowledgeExcess))
        database.financialLogDao().insert(FinancialLogEntity(eventId = id, accountId = account.accountId, amount = input.amount))
        truth.resolve(database.eventDao().findById(id)!!)
        OwnerFundsReceiptResult(id, input.amount, false)
    }

    companion object { const val EVENT_TYPE = "OWNER_FUNDS_IN" }
}
