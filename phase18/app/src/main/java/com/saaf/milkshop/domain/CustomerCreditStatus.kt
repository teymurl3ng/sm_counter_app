package com.saaf.milkshop.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CreditStatusKind { SETTLED, ADVANCE, GOOD, WATCH, LATE, BAD, NEEDS_REVIEW }
enum class CreditCoverageIssue { ALLOCATION_UNAVAILABLE, UNDATED_OPENING_DEBT, TERMS_UNAVAILABLE, AGREEMENT_MISMATCH, INVALID_ALLOCATION }

data class CustomerCreditStatus(
    val kind: CreditStatusKind,
    val overduePaisa: Long = 0,
    val oldestOverdueDays: Long? = null,
    val nextDeadline: LocalDate? = null,
    val coverageIssues: Set<CreditCoverageIssue> = emptySet(),
    val usesLegacyWeekly: Boolean = false,
    /** Independent review judgment: never changes the debt or computed category. */
    val ownerFlagged: Boolean = false,
) {
    val needsBadFilter: Boolean get() = kind == CreditStatusKind.BAD || ownerFlagged
}

/** Current-debt status under the agreed terms; not a lifetime payment-reliability score. */
object CustomerCreditStatusCalculator {
    fun calculate(
        allocation: CreditAllocation,
        customerUid: String,
        termsByRootSale: Map<String, CreditTermEvidence>,
        asOf: LocalDate,
        ownerFlagged: Boolean = false,
    ): CustomerCreditStatus {
        fun simple(kind: CreditStatusKind) = CustomerCreditStatus(kind, ownerFlagged = ownerFlagged)
        // An authoritative zero/advance does not imply historically reliable payment behavior.
        if (allocation.balancePaisa <= 0) return simple(if (allocation.balancePaisa == 0L) CreditStatusKind.SETTLED else CreditStatusKind.ADVANCE)
        if (!allocation.age.available) return simple(CreditStatusKind.NEEDS_REVIEW)
            .copy(coverageIssues = setOf(CreditCoverageIssue.ALLOCATION_UNAVAILABLE))
        val lots = allocation.unpaidLots
        val valid = customerUid.isNotBlank() && lots.map { it.rootSaleUid }.distinct().size == lots.size &&
            lots.all { it.rootSaleUid.isNotBlank() && it.remainingPaisa > 0 && (it.saleDate == null || !it.saleDate.isAfter(asOf)) } &&
            runCatching { lots.fold(0L) { sum, lot -> Math.addExact(sum, lot.remainingPaisa) } == allocation.balancePaisa }.getOrDefault(false)
        if (!valid) return simple(CreditStatusKind.NEEDS_REVIEW)
            .copy(coverageIssues = setOf(CreditCoverageIssue.INVALID_ALLOCATION))

        val issues = linkedSetOf<CreditCoverageIssue>()
        var overdue = 0L
        var oldest: Long? = null
        var upcoming: LocalDate? = null
        var legacy = false
        for (lot in lots) {
            val saleDate = lot.saleDate
            if (saleDate == null) { issues += CreditCoverageIssue.UNDATED_OPENING_DEBT; continue }
            val term = when (val evidence = termsByRootSale[lot.rootSaleUid]) {
                CreditTermEvidence.LegacyWeekly -> { legacy = true; CreditPaymentTerm.Weekly }
                is CreditTermEvidence.Recorded -> {
                    val agreement = evidence.agreement
                    if (agreement.rootSaleUid != lot.rootSaleUid || agreement.customerUid != customerUid) {
                        issues += CreditCoverageIssue.AGREEMENT_MISMATCH
                        continue
                    }
                    agreement.term
                }
                CreditTermEvidence.Unavailable, null -> { issues += CreditCoverageIssue.TERMS_UNAVAILABLE; continue }
            }
            val due = runCatching { term.dueDate(saleDate) }.getOrNull()
            if (due == null) { issues += CreditCoverageIssue.TERMS_UNAVAILABLE; continue }
            val days = ChronoUnit.DAYS.between(due, asOf)
            if (days > 0) {
                overdue = Math.addExact(overdue, lot.remainingPaisa)
                oldest = maxOf(oldest ?: 0, days)
            } else if (upcoming == null || due.isBefore(upcoming)) upcoming = due
        }
        val kind = when {
            issues.isNotEmpty() -> CreditStatusKind.NEEDS_REVIEW
            (oldest ?: 0) >= 15 -> CreditStatusKind.BAD
            oldest != null -> CreditStatusKind.LATE
            upcoming != null && ChronoUnit.DAYS.between(asOf, upcoming) <= 2 -> CreditStatusKind.WATCH
            else -> CreditStatusKind.GOOD
        }
        return CustomerCreditStatus(kind, overdue, oldest, upcoming, issues.toSet(), legacy, ownerFlagged)
    }
}
