package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.*
import androidx.room3.withReadTransaction
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.sync.BusinessClock
import java.math.BigDecimal
import java.time.LocalDate

data class CustomerCreditView(
    val status: CustomerCreditStatus,
    val term: CreditPaymentTerm,
    val revisionUid: String,
    val enabled: Boolean,
    val activeSetUids: Set<String>,
    val history: List<String>,
    val pending: List<CustomerCreditPending>,
) {
    val label: String get() = status.kind.label + if(status.ownerFlagged) " · Owner flagged" else ""
    val explanation: String get() = when {
        status.coverageIssues.isNotEmpty() -> "Some debt cannot be dated or its agreement is unavailable. Review the account history." +
            if(status.overduePaisa>0) " Known overdue: PKR ${"%,.2f".format(java.util.Locale.US,status.overduePaisa/100.0)} (partial)." else ""
        status.overduePaisa > 0 -> "PKR ${"%,.2f".format(java.util.Locale.US,status.overduePaisa/100.0)} overdue · oldest ${status.oldestOverdueDays} days."
        status.nextDeadline != null -> "Next unpaid deadline: ${status.nextDeadline}."
        status.kind == CreditStatusKind.ADVANCE -> "The customer has an advance balance."
        status.kind == CreditStatusKind.SETTLED -> "No unpaid debt."
        else -> "Within the recorded payment terms."
    }
}
val CreditStatusKind.label: String get() = when(this) {
    CreditStatusKind.NEEDS_REVIEW -> "Needs review"
    else -> name.lowercase().replaceFirstChar { it.uppercase() }
}
data class CustomerCreditPending(val uid: String,val kind: String,val description: String,val held: Boolean)
sealed interface CustomerCreditCommand {
    data class Terms(val expectedRevision: String,val term: CreditPaymentTerm): CustomerCreditCommand
    data class Flag(val reason: String): CustomerCreditCommand
    data class Clear(val reviewedSets: Set<String>,val reason: String): CustomerCreditCommand
    data class Retry(val uid: String): CustomerCreditCommand
    data class DiscardTerm(val uid: String): CustomerCreditCommand
    data object Enable : CustomerCreditCommand
}

/** One bounded set of reads for the entire list; no full-ledger query per customer row. */
internal class CustomerCreditViews(private val db: MilkShopDatabase) {
    suspend fun load(balances: Map<Long,Double>, asOf: LocalDate = BusinessClock.today()): Map<Long,CustomerCreditView> = db.withReadTransaction {
        val data=db.backupDataDao()
        val accounts=data.accounts().associateBy { it.accountId }
        val events=data.events().associateBy { it.eventId }
        val movements=data.financialLogs().groupBy { it.accountId }
        val dao=db.creditMetadataDao()
        val revisions=dao.revisions().associateBy { it.revisionUid }
        val heads=dao.heads().associateBy { it.customerUid }
        val agreements=dao.agreements().groupBy { it.customerUid }
        val actions=dao.actions().groupBy { it.customerUid }
        val pending=dao.pending()
        val state=dao.state()
        balances.mapValues { (id,balance) ->
            val uid=requireNotNull(accounts[id]).uid
            val customerPending=pending.filter { org.json.JSONObject(it.payloadJson).optString("customer_uid")==uid }
            val pendingUids=customerPending.map { it.operationUid }.toSet()
            // A pending/restored CLEAR is not evidence that the server accepted a risk judgment.
            val acceptedActions=actions[uid].orEmpty().filterNot { it.actionUid in pendingUids }
            val flag=CreditFlagReviewCalculator.calculate(uid,acceptedActions.map { it.toAction() })
            val canonical=money(balance)
            val allocation=try {
                val sums=movements[id].orEmpty().groupBy { it.eventId }.mapValues { (_,rows) ->
                    rows.fold(BigDecimal.ZERO) { sum,row -> sum+BigDecimal.valueOf(row.amount) }
                }
                val facts=linkedMapOf<Long,CreditAgeFact>()
                for(eventId in sums.keys) {
                    var next: Long?=eventId
                    val visited=hashSetOf<Long>()
                    while(next!=null) {
                        require(visited.add(next))
                        val event=requireNotNull(events[next])
                        if(event.correctsEventId==null && event.eventType!="OPENING_STATE") require(event.eventId in sums)
                        facts[event.eventId]=CreditAgeFact(event.eventId,event.eventUid,event.correctsEventId,event.eventType,event.occurredAt,sums[event.eventId]?:BigDecimal.ZERO)
                        next=event.correctsEventId
                    }
                }
                CustomerCreditAging.allocate(facts.values.toList(),canonical,asOf)
            } catch (_: Exception) { CreditAllocation(canonical,CreditAge(false),emptyList()) }
            val evidence=agreements[uid].orEmpty().associate { row -> row.rootSaleUid to when(row.evidence) {
                CreditMetadataStore.LEGACY -> CreditTermEvidence.LegacyWeekly
                CreditMetadataStore.RECORDED -> CreditTermEvidence.Recorded(CreditSaleAgreement(row.rootSaleUid,uid,
                    CreditPaymentTerm(CreditTermKind.valueOf(row.termKind),row.termValue),row.revisionUid,row.contractVersion))
                else -> CreditTermEvidence.Unavailable
            } }
            val revision=heads[uid]?.let { revisions[it.revisionUid] }
            CustomerCreditView(CustomerCreditStatusCalculator.calculate(allocation,uid,evidence,asOf,flag.flagged),
                revision?.let { CreditPaymentTerm(CreditTermKind.valueOf(it.termKind),it.termValue) }?:CreditPaymentTerm.Weekly,
                revision?.revisionUid?:CreditMetadataStore.DEFAULT_REVISION,state?.enabled==true && state.contractVersion==1,
                flag.activeSetUids,
                revisions.values.filter { it.customerUid==uid }.map { "${it.acceptedAt.take(10)} · Accepted terms: ${CreditPaymentTerm(CreditTermKind.valueOf(it.termKind),it.termValue).label} · ${it.localActorUid}" }+
                    actions[uid].orEmpty().map { "${it.businessDate} · ${it.kind} · ${it.reason} · ${if(it.actionUid in pendingUids) "Pending server acceptance" else "Accepted"} · ${it.localActorUid}" },
                customerPending.map { CustomerCreditPending(it.operationUid,it.kind,
                    if(it.kind=="TERM_REVISION") "Payment terms proposal" else "Owner flag ${org.json.JSONObject(it.payloadJson).optString("kind").lowercase()}",it.held) })
        }
    }
}
