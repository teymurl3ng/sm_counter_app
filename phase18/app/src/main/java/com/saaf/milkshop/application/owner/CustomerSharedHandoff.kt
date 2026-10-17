package com.saaf.milkshop.application.owner

import androidx.room3.withReadTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import com.saaf.milkshop.statements.ReminderIntents
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class CustomerSharedRequest(val uid: String,val kind: String)
data class CustomerSharedHandoffState(val attemptUid: String?=null,val phase: String?=null,val canRelease: Boolean=false,
    val canResolve: Boolean=false,val pending: List<CustomerSharedRequest> = emptyList(),val blockedReason: String?=null)
data class CustomerSharedHandoffAccess(
    val load: suspend (Long)->CustomerSharedHandoffState,
    val open: suspend (Long,CustomerReminder,String,String,suspend ()->Unit)->CustomerSharedHandoffState,
    val recover: suspend (Long,String)->CustomerSharedHandoffState,
    val release: suspend (Long,String)->CustomerSharedHandoffState,
    val resolve: suspend (Long,String,String,Boolean)->CustomerSharedHandoffState,
    val setAside: suspend (Long,String,Boolean)->CustomerSharedHandoffState = {_,_,_->error("Action review is unavailable.")},
)

/** Explicit coordinated mode only. Ordinary factory/mode selection is the remaining B integration. */
internal class CustomerSharedHandoff(private val db: MilkShopDatabase,private val security: UserSecurityService,
    private val runtime: SyncRuntime,private val session: String,
    private val sync: suspend ()->Unit = {runtime.drainOnce();runtime.pullOnce()}) {
    val access=CustomerSharedHandoffAccess(::load,::open,::recover,::release,::resolve,::setAside)
    private val kinds=setOf("ACQUIRE","RENEW","START","RELEASE","RESOLVE")
    private data class Facts(val reminder: CustomerReminder,val active: Boolean,val fresh: Boolean,val cycle: String)
    private suspend fun <T> run(work: suspend (FollowUpSessionAccess)->T): T {
        security.requirePermission(session,AppPermission.VIEW_FULL_FINANCIAL)
        sync() // Public sync methods acquire their own gate; never call them while holding it below.
        return runtime.withFollowUpSession(session,work)
    }
    private suspend fun remote(a: FollowUpSessionAccess): FollowUpRemoteSnapshot {
        val result=FollowUpDataStore(db).refresh(a.binding,a.call)
        check(result is FollowUpLoad.Loaded) {"Connect to check follow-up records before opening a message app."}
        val r=result.snapshot
        check(r.policy!=null&&r.zone!=null&&r.heads.date!=null&&TrustedTime.isAnchored()) {"Follow-up records are unavailable. Refresh the shop settings."}
        return r
    }
    private suspend fun facts(r: FollowUpRemoteSnapshot,id: Long): Facts=db.withReadTransaction {
        val day=requireNotNull(r.heads.date)
        val reminder=CustomerCommunicationService(db,security).reminder(session,id,day)
        val active=db.customerProfileDao().findByAccountId(id)?.active ?: true
        val credit=db.creditMetadataDao().state()
        val fresh=(db.syncPullStateDao().find()?.lastServerSeq ?: 0L)==r.heads.sync&&credit?.shopUid==r.shop&&credit.lastServerSeq==r.heads.credit&&credit.enabled&&
            db.syncOutboxDao().count()==0L&&db.syncRebaselineHoldDao().count()==0&&db.syncParkedRowDao().count()==0&&db.creditMetadataDao().pending().isEmpty()
        val customer=CustomerFollowUpReader(db,security).load(session,day).single {it.accountId==id}
        val row=CustomerFollowUps.evaluate(customer,requireNotNull(r.policy),r.preferences[customer.uid],r.acknowledgments[customer.uid] ?: FollowUpAcknowledgment(),day)
        Facts(reminder,active,fresh,row?.statementCycle ?: "manual:$day")
    }
    private suspend fun state(a: FollowUpSessionAccess,r: FollowUpRemoteSnapshot,id: Long): CustomerSharedHandoffState {
        val f=facts(r,id);val uid=f.reminder.customerUid
        val claim=r.claims.singleOrNull {it.customerUid==uid&&
            (it.phase=="HANDOFF_STARTED"||(it.leaseUntil ?: Instant.MIN)>Instant.ofEpochMilli(r.serverTime))}
        val own=claim?.let {it.actor==a.binding.staff&&it.installation==a.binding.installation}==true
        val rows=db.followUpDataDao().commands().filter {it.bindingKey==a.binding.key}
        val pending=rows.mapNotNull {row->runCatching {
            val p=JSONObject(row.operationJson);val kind=FollowUpWire.operation(p)
            if(kind in kinds&&p.getString("customer_uid")==uid) CustomerSharedRequest(row.commandUid,kind) else null
        }.getOrNull()}
        val unfinished=rows.any {runCatching {FollowUpWire.operation(JSONObject(it.operationJson))!="DIGEST"}.getOrDefault(true)}
        val pref=r.preferences[uid]
        val reason=when {
            !f.fresh->"Sync current records before opening a customer message."
            pending.isNotEmpty()->"Check the unfinished message action before trying again."
            unfinished->"Check the unfinished shop or customer save before opening a message."
            claim?.phase=="HANDOFF_STARTED"->"Review the previous message attempt before opening another."
            claim!=null->if(own) "A preparation is active on this device. Cancel it before starting again." else "Another device is preparing this customer's message."
            r.policy?.enabled!=true->"Follow-up reminders are off. Review the shop settings before opening a message app."
            !f.active->"This customer is inactive. Review the customer record first."
            f.reminder.message==null->"No payment reminder is needed for the current balance."
            pref?.paused==true||pref?.snoozedUntil?.let {it>r.heads.date}==true->"Follow-ups for this customer are paused or postponed. Resume them before opening a message app."
            else->null
        }
        return CustomerSharedHandoffState(claim?.attemptUid,claim?.phase,own&&claim?.phase=="PREPARING",
            f.fresh&&claim?.phase=="HANDOFF_STARTED"&&(own||a.owner),pending,reason)
    }
    private fun heads(h: FollowUpHeads)=JSONObject().put("sync",h.sync).put("credit",h.credit).put("metadata",h.metadata)
        .put("policy_revision",h.policyRevision ?: JSONObject.NULL).put("business_date",h.date?.toString() ?: JSONObject.NULL)
    private fun operation(kind: String,r: FollowUpRemoteSnapshot,uid: String,attempt: String)=JSONObject().put("kind",kind)
        .put("customer_uid",uid).put("attempt_uid",attempt).apply {
            if(kind in setOf("ACQUIRE","START","RESOLVE")) put("expected_heads",heads(r.heads)).put("expected_customer_revision",r.customerRevisions[uid] ?: JSONObject.NULL)
        }
    private suspend fun execute(a: FollowUpSessionAccess,p: JSONObject): FollowUpCommandOutcome.Accepted {
        val result=FollowUpDataStore(db).execute(a.binding,UUID.randomUUID().toString(),p,a.call)
        check(result is FollowUpCommandOutcome.Accepted) {"The message action needs checking. Check its result before trying again."}
        return result
    }
    private suspend fun load(id: Long)=run {a->state(a,remote(a),id)}
    private suspend fun open(id: Long,reviewed: CustomerReminder,recipient: String,channel: String,launch: suspend ()->Unit): CustomerSharedHandoffState=run {a->
        require(channel in setOf("SMS","WhatsApp"));ReminderIntents.phone(recipient)
        val first=remote(a);val before=state(a,first,id)
        check(before.blockedReason==null) {requireNotNull(before.blockedReason)}
        val expected=facts(first,id);check(expected.reminder==reviewed) {"Records changed. Refresh the message and check the recipient again."}
        val uid=reviewed.customerUid;val attempt=UUID.randomUUID().toString()
        val acquired=execute(a,operation("ACQUIRE",first,uid,attempt).put("cycle",expected.cycle))
        check(!acquired.receipt.replayed) {"Check the preparation result before trying again."}
        val ready=remote(a);val claim=ready.claims.singleOrNull {it.customerUid==uid}
        check(claim?.attemptUid==attempt&&claim.phase=="PREPARING"&&claim.actor==a.binding.staff&&claim.installation==a.binding.installation&&
            (claim.leaseUntil ?: Instant.MIN)>Instant.ofEpochMilli(ready.serverTime)) {"The preparation changed. Refresh before trying again."}
        check(first.heads.sync==ready.heads.sync&&first.heads.credit==ready.heads.credit&&first.heads.policyRevision==ready.heads.policyRevision&&
            first.heads.date==ready.heads.date&&first.customerRevisions[uid]==ready.customerRevisions[uid]) {"Synced records changed. Review them before continuing."}
        val current=facts(ready,id)
        check(current.fresh&&current.active&&current.reminder==reviewed) {"Records changed. Refresh the message and check the recipient again."}
        val started=execute(a,operation("START",ready,uid,attempt).put("channel",if(channel=="SMS") "SMS" else "WHATSAPP"))
        check(started.grant==FollowUpLiveGrant.LAUNCH) {"The last attempt is unconfirmed. Review it before opening another message."}
        currentCoroutineContext().ensureActive();a.validate()
        val final=facts(ready,id)
        check(final.fresh&&final.active&&final.reminder==reviewed) {"Records changed before the message app opened. Review this attempt before trying again."}
        currentCoroutineContext().ensureActive()
        launch() // Consumed here; no launch token escapes or survives a restart.
        CustomerSharedHandoffState(attempt,"HANDOFF_STARTED",canResolve=true,
            blockedReason="Message app opened. Sending is not confirmed. Review this attempt before opening another message.")
    }
    private suspend fun recover(id: Long,request: String): CustomerSharedHandoffState=run {a->
        val r=remote(a);val uid=facts(r,id).reminder.customerUid
        val row=db.followUpDataDao().command(request) ?: error("This unfinished action is no longer listed. Refresh first.")
        check(row.bindingKey==a.binding.key) {"This action belongs to another shop or device."}
        val p=JSONObject(row.operationJson);val kind=FollowUpWire.operation(p)
        check(kind in kinds&&p.getString("customer_uid")==uid) {"This action belongs to a different task."}
        FollowUpDataStore(db).execute(a.binding,request,p,a.call) // Recovery can never invoke a launcher.
        state(a,remote(a),id)
    }
    private suspend fun setAside(id: Long,request: String,acknowledged: Boolean): CustomerSharedHandoffState=run {a->
        require(acknowledged) {"Confirm that removing this copy does not undo a synced change."}
        val r=remote(a);val uid=facts(r,id).reminder.customerUid
        val row=db.followUpDataDao().command(request) ?: error("This unfinished action is no longer listed. Refresh first.")
        check(row.bindingKey==a.binding.key) {"This action belongs to another shop or device."}
        val p=JSONObject(row.operationJson)
        check(FollowUpWire.operation(p) in kinds&&p.getString("customer_uid")==uid) {"This action belongs to a different task."}
        // Removing local bookkeeping never releases a lease, resolves a START or grants a launch.
        db.followUpDataDao().complete(request)
        state(a,r,id)
    }
    private suspend fun release(id: Long,attempt: String): CustomerSharedHandoffState=run {a->
        val r=remote(a);val current=state(a,r,id)
        check(current.attemptUid==attempt&&current.canRelease&&current.pending.isEmpty()) {"Refresh and check the exact preparation before cancelling it."}
        execute(a,operation("RELEASE",r,facts(r,id).reminder.customerUid,attempt))
        state(a,remote(a),id)
    }
    private suspend fun resolve(id: Long,attempt: String,outcome: String,acknowledged: Boolean): CustomerSharedHandoffState=run {a->
        require(acknowledged&&outcome in setOf("REVIEWED","REPORTED_NOT_OPENED","RETRY_UNCERTAIN")) {"Confirm what you know about this message attempt."}
        val r=remote(a);val current=state(a,r,id)
        check(current.attemptUid==attempt&&current.canResolve&&current.pending.isEmpty()) {"The message attempt changed or needs Owner review. Refresh first."}
        execute(a,operation("RESOLVE",r,facts(r,id).reminder.customerUid,attempt).put("outcome",outcome).put("acknowledge_uncertainty",true))
        state(a,remote(a),id)
    }
}
