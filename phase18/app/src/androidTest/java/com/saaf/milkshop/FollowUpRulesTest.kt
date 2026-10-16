package com.saaf.milkshop

import com.saaf.milkshop.domain.*
import org.junit.Test
import org.junit.Assert.*
import java.time.*

class FollowUpRulesTest {
    private fun date(s: String)=LocalDate.parse(s)
    private val sunday=date("2026-10-18")
    private val policy=FollowUpPolicy("policy-v1",date("2026-10-16"),true)
    private val good=FollowUpCustomer(7,"customer","Cafe",null,true,15000,CustomerCreditStatus(CreditStatusKind.GOOD))
    @Test fun sundayStatementsNeverReplaceAgreedDueDates() {
        val due=CreditPaymentTerm.Weekly.dueDate(date("2026-10-16"));assertEquals(date("2026-10-23"),due)
        assertNull(CustomerFollowUps.evaluate(good,policy,null,FollowUpAcknowledgment(),date("2026-10-16")))
        val row=CustomerFollowUps.evaluate(good,policy,null,FollowUpAcknowledgment(),sunday)!!
        assertEquals(setOf(FollowUpReason.STATEMENT),row.reasons);assertEquals(sunday,row.statementOccurrence)
        val late=good.copy(status=CustomerCreditStatus(CreditStatusKind.LATE,5000,1))
        assertTrue(CustomerFollowUps.evaluate(late,policy,null,FollowUpAcknowledgment(),date("2026-10-24"))!!.reasons.contains(FollowUpReason.OVERDUE))
        assertEquals(due,CreditPaymentTerm.Weekly.dueDate(date("2026-10-16")))
    }
    @Test fun monthlyClampingAndAnchoredIntervalsSurviveMissedRunsAndLeapYears() {
        val monthly=StatementReviewSchedule.Monthly(31)
        assertEquals(date("2027-02-28"),CustomerFollowUps.latest(monthly,date("2027-03-15")))
        assertEquals(date("2027-03-31"),CustomerFollowUps.next(monthly,date("2027-02-28")))
        assertEquals(date("2028-02-29"),CustomerFollowUps.next(monthly,date("2028-01-31")))
        assertEquals(date("2027-02-28"),CreditPaymentTerm.Monthly.dueDate(date("2027-01-31")))
        assertEquals(date("2027-04-30"),CustomerFollowUps.latest(StatementReviewSchedule.Monthly(0),date("2027-04-30")))
        val interval=StatementReviewSchedule.Interval(10,date("2026-10-20"))
        assertNull(CustomerFollowUps.latest(interval,date("2026-10-19")))
        assertEquals(date("2026-11-19"),CustomerFollowUps.latest(interval,date("2026-11-24")))
        assertEquals(date("2026-11-29"),CustomerFollowUps.next(interval,date("2026-11-24")))
        assertEquals(date("2026-10-20"),CustomerFollowUps.next(interval,date("2026-10-01")))
    }
    @Test fun reviewsMergeReasonsSuppressRepeatsAndKeepStableCycleIdentity() {
        val facts=good.copy(status=CustomerCreditStatus(CreditStatusKind.NEEDS_REVIEW,5000,3,coverageIssues=setOf(CreditCoverageIssue.UNDATED_OPENING_DEBT),ownerFlagged=true))
        val row=CustomerFollowUps.evaluate(facts,policy,null,FollowUpAcknowledgment(),sunday)!!
        assertEquals(4,row.reasons.size)
        assertEquals(row,CustomerFollowUps.evaluate(facts,policy,null,FollowUpAcknowledgment(),sunday))
        val ack=FollowUpAcknowledgment(row.statementCycle,sunday)
        assertNull(CustomerFollowUps.evaluate(facts,policy,null,ack,sunday.plusDays(6)))
        val next=CustomerFollowUps.evaluate(facts,policy,null,ack,sunday.plusDays(7))!!
        assertEquals(4,next.reasons.size);assertNotEquals(row.statementCycle,next.statementCycle)
        assertNotEquals(row.statementCycle,CustomerFollowUps.evaluate(facts,policy.copy(revision="policy-v2"),null,ack,sunday)!!.statementCycle)
        val preference=CustomerFollowUpPreference("customer-v2",sunday,StatementReviewSchedule.Weekly())
        assertNotEquals(row.statementCycle,CustomerFollowUps.evaluate(facts,policy,preference,ack,sunday)!!.statementCycle)
    }
    @Test fun paymentPauseSnoozeActivationAndUnknownDebtStayTruthful() {
        val unknown=good.copy(status=null)
        assertEquals(setOf(FollowUpReason.RECORDS),CustomerFollowUps.evaluate(unknown,policy,null,FollowUpAcknowledgment(),date("2026-10-16"))!!.reasons)
        for(c in listOf(good.copy(balancePaisa=0),good.copy(balancePaisa=-100),good.copy(active=false))) {
            assertNull(CustomerFollowUps.evaluate(c,policy,null,FollowUpAcknowledgment(),sunday))
        }
        assertNull(CustomerFollowUps.evaluate(good,policy.copy(enabled=false),null,FollowUpAcknowledgment(),sunday))
        assertNull(CustomerFollowUps.evaluate(good,policy.copy(effectiveFrom=sunday.plusDays(1)),null,FollowUpAcknowledgment(),sunday))
        val snooze=CustomerFollowUpPreference("pref",sunday,snoozedUntil=sunday.plusDays(1))
        assertFalse(CustomerFollowUps.evaluate(good,policy,snooze,FollowUpAcknowledgment(),sunday)!!.actionable)
        assertTrue(CustomerFollowUps.evaluate(good,policy,snooze,FollowUpAcknowledgment(),sunday.plusDays(1))!!.actionable)
        assertFalse(CustomerFollowUps.evaluate(good,policy,snooze.copy(paused=true),FollowUpAcknowledgment(),sunday)!!.actionable)
    }
    @Test fun invalidFactsAndQuietHourBoundariesAreExplicit() {
        assertTrue(policy.isQuiet(LocalTime.of(20,0)));assertTrue(policy.isQuiet(LocalTime.of(8,59)));assertFalse(policy.isQuiet(LocalTime.of(9,0)))
        val daytime=policy.copy(quietStart=LocalTime.of(12,0),quietEnd=LocalTime.of(14,0))
        assertTrue(daytime.isQuiet(LocalTime.of(12,0)));assertFalse(daytime.isQuiet(LocalTime.of(14,0)))
        assertTrue(runCatching {policy.copy(quietEnd=policy.quietStart)}.isFailure)
        assertTrue(runCatching {policy.copy(repeatDays=0)}.isFailure)
        assertTrue(runCatching {StatementReviewSchedule.Interval(366,sunday)}.isFailure)
        assertTrue(runCatching {StatementReviewSchedule.Monthly(32)}.isFailure)
        assertTrue(runCatching {CustomerFollowUps.evaluate(good,policy,null,FollowUpAcknowledgment(reviewedOn=sunday.plusDays(1)),sunday)}.isFailure)
        assertTrue(runCatching {CustomerFollowUps.evaluate(good.copy(status=CustomerCreditStatus(CreditStatusKind.LATE,99999)),policy,null,FollowUpAcknowledgment(),sunday)}.isFailure)
    }
}
