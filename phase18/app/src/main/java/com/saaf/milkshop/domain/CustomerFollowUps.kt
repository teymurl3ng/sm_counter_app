package com.saaf.milkshop.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Statement review cadence, independent of a sale's retained payment agreement. */
sealed interface StatementReviewSchedule {
    data class Weekly(val weekday: DayOfWeek=DayOfWeek.SUNDAY): StatementReviewSchedule
    /** Day 0 means last day; 29–31 clamp in short months without changing the preference. */
    data class Monthly(val day: Int): StatementReviewSchedule {init {require(day in 0..31)}}
    data class Interval(val days: Int,val firstDate: LocalDate): StatementReviewSchedule {init {require(days in 1..365)}}
}
data class FollowUpPolicy(val revision: String,val effectiveFrom: LocalDate,val enabled: Boolean=false,
    val statementSchedule: StatementReviewSchedule=StatementReviewSchedule.Weekly(),val repeatDays: Int=7,
    val preferredTime: LocalTime=LocalTime.of(10,0),val quietStart: LocalTime=LocalTime.of(20,0),val quietEnd: LocalTime=LocalTime.of(9,0)) {
    init {require(revision.isNotBlank());require(repeatDays in 1..30);require(quietStart!=quietEnd) {"Choose different quiet-hour start and end times."}}
    fun isQuiet(time: LocalTime): Boolean=if(quietStart<quietEnd) time>=quietStart && time<quietEnd else time>=quietStart || time<quietEnd
}
data class CustomerFollowUpPreference(val revision: String,val effectiveFrom: LocalDate,
    val schedule: StatementReviewSchedule?=null,val paused: Boolean=false,val snoozedUntil: LocalDate?=null)
data class FollowUpAcknowledgment(val statementCycle: String?=null,val reviewedOn: LocalDate?=null)
data class FollowUpCustomer(val accountId: Long,val uid: String,val name: String,val phone: String?,val active: Boolean,
    val balancePaisa: Long,val status: CustomerCreditStatus?)
enum class FollowUpReason(val label: String) { STATEMENT("Statement review"), OVERDUE("Overdue follow-up"), RECORDS("Check records"), OWNER_REVIEW("Owner review") }
data class CustomerFollowUp(val customer: FollowUpCustomer,val reasons: Set<FollowUpReason>,val statementOccurrence: LocalDate?,
    val statementCycle: String?,val lastReviewed: LocalDate?,val postponedUntil: LocalDate?,val paused: Boolean) {
    val actionable: Boolean get()=!paused && postponedUntil==null
}

/** Deterministic decisions only. No clock, storage, scheduling or external message side effects. */
object CustomerFollowUps {
    fun latest(schedule: StatementReviewSchedule,on: LocalDate): LocalDate?=when(schedule) {
        is StatementReviewSchedule.Weekly->on.with(TemporalAdjusters.previousOrSame(schedule.weekday))
        is StatementReviewSchedule.Monthly->{
            fun date(month: LocalDate)=month.withDayOfMonth(if(schedule.day==0) month.lengthOfMonth() else minOf(schedule.day,month.lengthOfMonth()))
            date(on).takeIf {it<=on} ?: date(on.withDayOfMonth(1).minusMonths(1))
        }
        is StatementReviewSchedule.Interval->if(on<schedule.firstDate) null else
            schedule.firstDate.plusDays(ChronoUnit.DAYS.between(schedule.firstDate,on)/schedule.days*schedule.days)
    }
    fun next(schedule: StatementReviewSchedule,after: LocalDate): LocalDate=when(schedule) {
        is StatementReviewSchedule.Weekly->after.with(TemporalAdjusters.next(schedule.weekday))
        is StatementReviewSchedule.Monthly->{
            val current=after.withDayOfMonth(if(schedule.day==0) after.lengthOfMonth() else minOf(schedule.day,after.lengthOfMonth()))
            if(current>after) current else after.withDayOfMonth(1).plusMonths(1).let {it.withDayOfMonth(if(schedule.day==0) it.lengthOfMonth() else minOf(schedule.day,it.lengthOfMonth()))}
        }
        is StatementReviewSchedule.Interval->if(after<schedule.firstDate) schedule.firstDate else
            schedule.firstDate.plusDays((ChronoUnit.DAYS.between(schedule.firstDate,after)/schedule.days+1)*schedule.days)
    }
    fun evaluate(customer: FollowUpCustomer,policy: FollowUpPolicy,preference: CustomerFollowUpPreference?,
        acknowledgment: FollowUpAcknowledgment,today: LocalDate): CustomerFollowUp? {
        require(customer.uid.isNotBlank())
        require(acknowledgment.reviewedOn==null || acknowledgment.reviewedOn<=today) {"Review date is ahead of the business date. Check time and refresh."}
        val overdue=customer.status?.overduePaisa ?: 0L
        require(overdue>=0 && overdue<=customer.balancePaisa.coerceAtLeast(0)) {"Overdue facts do not match the current balance. Refresh the customer."}
        if(!policy.enabled || !customer.active || customer.balancePaisa<=0 || policy.effectiveFrom>today || preference?.effectiveFrom?.let {it>today}==true) return null
        val schedule=preference?.schedule ?: policy.statementSchedule
        val since=maxOf(policy.effectiveFrom,preference?.effectiveFrom ?: policy.effectiveFrom)
        val occurrence=latest(schedule,today)?.takeIf {it>=since}
        // Length-prefixed identifiers avoid collisions when imported UIDs contain punctuation.
        val cycle=occurrence?.let {date->listOf(customer.uid,policy.revision,preference?.revision ?: "shop",date.toString()).joinToString(":") {"${it.length}:$it"}}
        val reasons=linkedSetOf<FollowUpReason>()
        if(cycle!=null && cycle!=acknowledgment.statementCycle) reasons+=FollowUpReason.STATEMENT
        val repeatReady=acknowledgment.reviewedOn?.plusDays(policy.repeatDays.toLong())?.let {it<=today} ?: true
        if(repeatReady) {
            if(overdue>0) reasons+=FollowUpReason.OVERDUE
            if(customer.status==null || customer.status.coverageIssues.isNotEmpty()) reasons+=FollowUpReason.RECORDS
            if(customer.status?.ownerFlagged==true) reasons+=FollowUpReason.OWNER_REVIEW
        }
        if(reasons.isEmpty()) return null
        return CustomerFollowUp(customer,reasons,occurrence,cycle,acknowledgment.reviewedOn,
            preference?.snoozedUntil?.takeIf {it>today},preference?.paused==true)
    }
    fun ordered(rows: List<CustomerFollowUp>): List<CustomerFollowUp> = rows.sortedWith(
        compareBy<CustomerFollowUp> {!it.actionable}.thenBy {when {
            FollowUpReason.RECORDS in it.reasons || FollowUpReason.OWNER_REVIEW in it.reasons->0
            FollowUpReason.OVERDUE in it.reasons->1
            else->2
        }}.thenByDescending {it.customer.status?.oldestOverdueDays ?: -1}.thenBy {it.statementOccurrence ?: LocalDate.MAX}
            .thenBy {it.customer.name.lowercase(java.util.Locale.ROOT)}.thenBy {it.customer.uid})
}
