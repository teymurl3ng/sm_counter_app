package com.saaf.milkshop

import com.saaf.milkshop.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class CreditPaymentStatusTest {
    private val customer = "customer-uid"
    private val sale = LocalDate.parse("2026-10-01")
    private fun fact(id: Long, uid: String, amount: String, date: LocalDate = sale,
        type: String = "SALE", parent: Long? = null) = CreditAgeFact(id, uid, parent, type, date.toString(), BigDecimal(amount))
    private fun agreement(uid: String, term: CreditPaymentTerm = CreditPaymentTerm.Weekly) =
        CreditTermEvidence.Recorded(CreditSaleAgreement(uid, customer, term, "revision-for-$uid"))
    private fun allocation(date: LocalDate, facts: List<CreditAgeFact> = listOf(fact(1, "sale-1", "100")), balance: Long = 10000) =
        CustomerCreditAging.allocate(facts, balance, date)

    @Test fun weeklyStatusChangesAtEverySpecifiedBoundary() {
        val cases = mapOf("2026-10-01" to CreditStatusKind.GOOD, "2026-10-05" to CreditStatusKind.GOOD,
            "2026-10-06" to CreditStatusKind.WATCH, "2026-10-08" to CreditStatusKind.WATCH,
            "2026-10-09" to CreditStatusKind.LATE, "2026-10-22" to CreditStatusKind.LATE, "2026-10-23" to CreditStatusKind.BAD)
        for ((day, expected) in cases) {
            val date = LocalDate.parse(day)
            val result = CustomerCreditStatusCalculator.calculate(allocation(date), customer, mapOf("sale-1" to agreement("sale-1")), date)
            assertEquals(day, expected, result.kind)
            assertEquals(day, if (day >= "2026-10-09") 10000L else 0L, result.overduePaisa)
        }
    }
    @Test fun calendarMonthClampsAndIsDifferentFromThirtyDays() {
        assertEquals(LocalDate.parse("2027-02-28"), CreditPaymentTerm.Monthly.dueDate(LocalDate.parse("2027-01-31")))
        assertEquals(LocalDate.parse("2028-02-29"), CreditPaymentTerm.Monthly.dueDate(LocalDate.parse("2028-01-31")))
        assertEquals(LocalDate.parse("2027-03-02"), CreditPaymentTerm(CreditTermKind.NET_DAYS, 30).dueDate(LocalDate.parse("2027-01-31")))
        assertEquals(LocalDate.parse("2027-01-31"), CreditPaymentTerm.Monthly.dueDate(LocalDate.parse("2026-12-31")))
        for (bad in listOf(0, -1, 366)) assertTrue(runCatching { CreditPaymentTerm(CreditTermKind.NET_DAYS, bad) }.isFailure)
        assertTrue(runCatching { CreditPaymentTerm(CreditTermKind.CALENDAR_MONTHS, 2) }.isFailure)
    }
    @Test fun newerWeeklyDebtCanBeOverdueWhileOldestMonthlyDebtIsCurrentAndFifoStaysTheSame() {
        val date = LocalDate.parse("2026-10-20")
        val rows = listOf(fact(1, "older-monthly", "100"), fact(2, "newer-weekly", "80", LocalDate.parse("2026-10-10")),
            fact(3, "payment", "-120", date, "CREDIT_COLLECTION"))
        val allocated = allocation(date, rows, 6000)
        assertEquals(listOf(UnpaidCreditLot("newer-weekly", LocalDate.parse("2026-10-10"), 6000)), allocated.unpaidLots)
        val terms = mapOf("older-monthly" to agreement("older-monthly", CreditPaymentTerm.Monthly), "newer-weekly" to agreement("newer-weekly"))
        val unpaid = allocation(date, rows.take(2), 18000)
        val status = CustomerCreditStatusCalculator.calculate(unpaid, customer, terms, date)
        assertEquals(CreditStatusKind.LATE, status.kind)
        assertEquals(8000L, status.overduePaisa)
        assertEquals(3L, status.oldestOverdueDays)
        assertEquals(LocalDate.parse("2026-11-01"), status.nextDeadline)
        assertEquals(6000L, CustomerCreditStatusCalculator.calculate(allocated, customer, terms, date).overduePaisa)
    }
    @Test fun correctedRootKeepsItsTermAndFuturePreferenceCannotRewriteIt() {
        val date = LocalDate.parse("2026-10-20")
        val rows = listOf(fact(1, "original-weekly", "100"), fact(2, "correction", "60", parent = 1),
            fact(3, "future-monthly", "50", LocalDate.parse("2026-10-10")))
        val result = CustomerCreditStatusCalculator.calculate(allocation(date, rows, 21000), customer,
            mapOf("original-weekly" to agreement("original-weekly"), "future-monthly" to agreement("future-monthly", CreditPaymentTerm.Monthly)), date)
        assertEquals(16000L, result.overduePaisa)
        assertEquals(12L, result.oldestOverdueDays)
        assertEquals(CreditStatusKind.LATE, result.kind)
    }
    @Test fun unknownOpeningDebtKeepsKnownOverduePartialInformationWithoutGoodClassification() {
        val date = LocalDate.parse("2026-10-20")
        val rows = listOf(fact(1, "opening", "50", type = "OPENING_STATE"), fact(2, "weekly", "100"))
        val status = CustomerCreditStatusCalculator.calculate(allocation(date, rows, 15000), customer, mapOf("weekly" to agreement("weekly")), date)
        assertEquals(CreditStatusKind.NEEDS_REVIEW, status.kind)
        assertEquals(setOf(CreditCoverageIssue.UNDATED_OPENING_DEBT), status.coverageIssues)
        assertEquals(10000L, status.overduePaisa)
    }
    @Test fun missingOrWrongAgreementNeverBecomesSilentLegacy() {
        val date = LocalDate.parse("2026-10-04")
        val allocated = allocation(date)
        val missing = CustomerCreditStatusCalculator.calculate(allocated, customer, emptyMap(), date)
        assertEquals(CreditStatusKind.NEEDS_REVIEW, missing.kind)
        assertFalse(missing.usesLegacyWeekly)
        val explicitLegacy = CustomerCreditStatusCalculator.calculate(allocated, customer, mapOf("sale-1" to CreditTermEvidence.LegacyWeekly), date)
        assertEquals(CreditStatusKind.GOOD, explicitLegacy.kind)
        assertTrue(explicitLegacy.usesLegacyWeekly)
        val wrong = CustomerCreditStatusCalculator.calculate(allocated, customer,
            mapOf("sale-1" to CreditTermEvidence.Recorded(CreditSaleAgreement("sale-1", "other-customer", CreditPaymentTerm.Weekly, "r"))), date)
        assertEquals(setOf(CreditCoverageIssue.AGREEMENT_MISMATCH), wrong.coverageIssues)
        assertTrue(runCatching { CreditSaleAgreement("sale-1", customer, CreditPaymentTerm.Weekly, "r", 2) }.isFailure)
    }
    @Test fun completePaymentAndAdvanceKeepOwnerFlagSeparate() {
        val date = LocalDate.parse("2026-10-20")
        for ((payment, balance, kind) in listOf(Triple("-100", 0L, CreditStatusKind.SETTLED), Triple("-120", -2000L, CreditStatusKind.ADVANCE))) {
            val rows = listOf(fact(1, "sale", "100"), fact(2, "paid", payment, date, "CREDIT_COLLECTION"))
            val status = CustomerCreditStatusCalculator.calculate(allocation(date, rows, balance), customer, emptyMap(), date, ownerFlagged = true)
            assertEquals(kind, status.kind)
            assertTrue(status.ownerFlagged)
            assertTrue(status.needsBadFilter)
            assertEquals(0L, status.overduePaisa)
        }
    }
    @Test fun rawDustDoesNotCreateAnOlderOverdueLot() {
        val date = LocalDate.parse("2026-10-15")
        val rows = listOf(fact(1, "dust", "0.004"), fact(2, "visible", "1", LocalDate.parse("2026-10-14")))
        val allocated = allocation(date, rows, 100)
        assertEquals(listOf("visible"), allocated.unpaidLots.map { it.rootSaleUid })
        val status = CustomerCreditStatusCalculator.calculate(allocated, customer, mapOf("visible" to agreement("visible")), date)
        assertEquals(CreditStatusKind.GOOD, status.kind)
        assertEquals(0L, status.overduePaisa)
    }
    @Test fun malformedAllocationAndIncompleteHistoryNeedReview() {
        val date = LocalDate.parse("2026-10-15")
        val incomplete = allocation(date, listOf(fact(1, "missing-root", "100", parent = 99)), 10000)
        assertEquals(setOf(CreditCoverageIssue.ALLOCATION_UNAVAILABLE),
            CustomerCreditStatusCalculator.calculate(incomplete, customer, emptyMap(), date).coverageIssues)
        val invalid = CreditAllocation(10000, CreditAge(true), listOf(UnpaidCreditLot("bad", sale, Long.MAX_VALUE), UnpaidCreditLot("bad2", sale, Long.MAX_VALUE)))
        assertEquals(setOf(CreditCoverageIssue.INVALID_ALLOCATION), CustomerCreditStatusCalculator.calculate(invalid, customer, emptyMap(), date).coverageIssues)
    }
    private fun flag(uid: String, clears: String? = null) = CreditFlagAction(uid, customer,
        if (clears == null) CreditFlagActionKind.SET else CreditFlagActionKind.CLEAR, clears,
        "Reviewed by owner", "local-owner", Instant.parse("2026-10-15T08:00:00Z"))
    @Test fun concurrentSetSurvivesClearAndTransportOrderDoesNotMatter() {
        val actions = listOf(flag("a"), flag("b"), flag("clear-a", "a"))
        val expected = CreditFlagReview(setOf("b"), emptySet())
        assertEquals(expected, CreditFlagReviewCalculator.calculate(customer, actions))
        assertEquals(expected, CreditFlagReviewCalculator.calculate(customer, actions.reversed() + actions))
        assertEquals(CreditFlagReview(emptySet(), emptySet()), CreditFlagReviewCalculator.calculate(customer, actions + flag("clear-b", "b")))
    }
    @Test fun flagDependencyMissingIsIncompleteAndIdentityConflictIsRefused() {
        val clear = flag("clear-a", "a")
        assertEquals(CreditFlagReview(emptySet(), setOf("a")), CreditFlagReviewCalculator.calculate(customer, listOf(clear)))
        assertTrue(runCatching { CreditFlagReviewCalculator.calculate(customer, listOf(flag("a"), flag("a").copy(reason = "different body"))) }.isFailure)
        assertTrue(runCatching { CreditFlagReviewCalculator.calculate("other", listOf(flag("a"))) }.isFailure)
        assertTrue(runCatching { CreditFlagReviewCalculator.calculate(customer, listOf(flag("a"), clear, flag("clear-clear", "clear-a"))) }.isFailure)
        assertTrue(runCatching { flag("a").copy(reason = " ") }.isFailure)
    }
}
