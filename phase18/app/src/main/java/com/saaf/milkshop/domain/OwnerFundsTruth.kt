package com.saaf.milkshop.domain

import com.saaf.milkshop.data.*

/** Shared complete-event validation for local writes, restore and UID replay. */
internal class OwnerFundsTruth(private val database: MilkShopDatabase) {
    suspend fun resolve(event: EventEntity): OwnerFundsDetailEntity = resolve(event, mutableSetOf())

    private suspend fun resolve(event: EventEntity, visiting: MutableSet<Long>): OwnerFundsDetailEntity {
        require(visiting.add(event.eventId)) { "Withdrawal history contains a cycle" }
        val parent = event.correctsEventId?.let { database.eventDao().findById(it) ?: error("Withdrawal history is incomplete") }
        val parentDetail = parent?.let { resolve(it, visiting) }
        val rows = database.financialLogDao().findByEventId(event.eventId).map { it.accountId to it.amount }
        val detail = database.ownerFundsDao().find(event.eventId) ?: if (event.eventType == "OWNER_FUNDS_IN") {
            error("Receipt details are missing")
        } else if (parentDetail != null) {
            parentDetail.copy(eventId = event.eventId)
        } else {
            val row = rows.singleOrNull() ?: error("Withdrawal must have one unambiguous original cash movement")
            OwnerFundsDetailEntity(event.eventId, row.first, event.eventId)
        }
        validate(event, detail, parent, parentDetail, rows,
            database.inventoryLogDao().findByEventId(event.eventId).isNotEmpty())
        return detail
    }

    suspend fun ensureDetails(event: EventEntity): OwnerFundsDetailEntity {
        val detail = resolve(event)
        event.correctsEventId?.let { ensureDetails(database.eventDao().findById(it)!!) }
        if (database.ownerFundsDao().find(event.eventId) == null) database.ownerFundsDao().insert(detail)
        return detail
    }

    suspend fun validate(
        event: EventEntity, detail: OwnerFundsDetailEntity, parent: EventEntity?, parentDetail: OwnerFundsDetailEntity?,
        financial: List<Pair<Long, Double>>, hasInventory: Boolean,
    ) {
        require(event.eventType in setOf("OWNER_DRAWING", "OWNER_FUNDS_IN") && !hasInventory) { "Invalid owner funds event shape" }
        val amount = event.amount ?: error("Owner funds amount is missing")
        require(amount.isFinite() && amount >= 0 && (parent != null || amount > 0)) { "Invalid owner funds amount" }
        require(database.accountDao().findById(detail.paymentAccountId)?.category == "Cash/Bank") { "Owner funds account is invalid" }
        require((detail.reviewedWithdrawalAmount == null) == (detail.reviewedReceivedTotal == null)) { "Review snapshots are incomplete" }
        require(listOfNotNull(detail.reviewedWithdrawalAmount, detail.reviewedReceivedTotal).all { it.isFinite() && it >= 0 }) {
            "Review snapshots are invalid"
        }
        val pendingOriginalWithdrawal = event.eventType == "OWNER_DRAWING" && parent == null &&
            event.eventId == 0L && detail.eventId == 0L && detail.withdrawalRootEventId == 0L
        val root = if (pendingOriginalWithdrawal) event else database.eventDao().findById(detail.withdrawalRootEventId)
        require(root?.eventType == "OWNER_DRAWING" && root.correctsEventId == null) { "Withdrawal reference is invalid" }
        if (parent == null && event.eventType == "OWNER_DRAWING") require(detail.withdrawalRootEventId == detail.eventId) { "Withdrawal root is inconsistent" }
        if (parent == null && event.eventType == "OWNER_FUNDS_IN") require(event.occurredAt >= root.occurredAt) {
            "Receipt date precedes the withdrawal"
        }
        else {
            if (parent != null) {
                require(parent.eventType == event.eventType && event.occurredAt == parent.occurredAt && !event.notes.isNullOrBlank()) {
                    "Owner funds correction must retain its date and include a reason"
                }
                require(parentDetail != null && detail.withdrawalRootEventId == parentDetail.withdrawalRootEventId) { "Withdrawal root is inconsistent" }
                require(amount != parent.amount || detail.paymentAccountId != parentDetail.paymentAccountId) { "Owner funds truth must change" }
                require(amount != 0.0 || detail.paymentAccountId == parentDetail.paymentAccountId) { "Cancellation retains its account" }
            }
        }
        val expected = if (event.eventType == "OWNER_DRAWING") movements(amount, detail.paymentAccountId, parent, parentDetail)
            else receiptMovements(amount, detail.paymentAccountId, parent, parentDetail)
        require(financial.sortedBy { it.first } == expected.sortedBy { it.first }) { "Owner funds movements are incomplete or inconsistent" }
    }

    /** Current reference and actual receipts, ignoring superseded correction headers. */
    suspend fun currentTotals(rootId: Long): Pair<Double, Double> {
        val events = database.eventDao().findAllDescending()
        val details = database.ownerFundsDao().all().associateBy { it.eventId }
        val correctedIds = events.mapNotNull { it.correctsEventId }.toSet()
        val current = events.filter { it.eventId !in correctedIds && details[it.eventId]?.withdrawalRootEventId == rootId }
        val withdrawal = current.singleOrNull { it.eventType == "OWNER_DRAWING" }
            ?: error("Current withdrawal truth is ambiguous")
        resolve(withdrawal)
        val receipts = current.filter { it.eventType == "OWNER_FUNDS_IN" }
        receipts.forEach { resolve(it) }
        val received = receipts.fold(java.math.BigDecimal.ZERO) { sum, receipt ->
            sum.add(java.math.BigDecimal.valueOf(receipt.amount!!))
        }
        return withdrawal.amount!! to received.toDouble()
    }

    companion object {
        fun movements(amount: Double, account: Long, parent: EventEntity?, detail: OwnerFundsDetailEntity?): List<Pair<Long, Double>> =
            if (parent == null) listOf(account to -amount)
            else if (account == detail!!.paymentAccountId) listOf(account to (parent.amount!! - amount)).filter { it.second != 0.0 }
            else listOf(detail.paymentAccountId to parent.amount!!, account to -amount).filter { it.second != 0.0 }

        fun receiptMovements(amount: Double, account: Long, parent: EventEntity?, detail: OwnerFundsDetailEntity?): List<Pair<Long, Double>> =
            if (parent == null) listOf(account to amount)
            else if (account == detail!!.paymentAccountId) listOf(account to (amount - parent.amount!!)).filter { it.second != 0.0 }
            else listOf(detail.paymentAccountId to -parent.amount!!, account to amount).filter { it.second != 0.0 }
    }
}
