package com.saaf.milkshop.nativeui.owner

import androidx.room3.withReadTransaction
import com.saaf.milkshop.application.owner.CustomerFollowUpReader
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import org.json.JSONObject
import java.time.*
import java.util.UUID

/** One instance per signed-in workspace; all mutations recheck current authorization. */
internal class FollowUpWorkspaceBinding(private val db: MilkShopDatabase,private val security: UserSecurityService,
    private val runtime: SyncRuntime,private val session: String,
    private val notificationsAllowed: ()->Boolean={false},private val hasMessageApp: ()->Boolean={false}) {
    private data class Basis(val token: String,val binding: FollowUpBinding,val remote: FollowUpRemoteSnapshot,
        val customers: List<FollowUpCustomer>,val online: Boolean,val reviewReady: Boolean)
    private var basis: Basis?=null
    val access=FollowUpWorkspaceAccess(load=::load,command=::save,recover=::recover,reviewRequest=::reviewRequest)
    private val metadataKinds=setOf("POLICY","PREFERENCE","REVIEW","SNOOZE")

    suspend fun load(): FollowUpWorkspaceSnapshot=runtime.withFollowUpSession(session) { read(it) }
    private suspend fun read(a: FollowUpSessionAccess): FollowUpWorkspaceSnapshot {
        basis=null
        val store=FollowUpDataStore(db)
        val response=store.refresh(a.binding,a.call)
        val remote=(response as? FollowUpLoad.Loaded)?.snapshot ?: store.cached(a.binding)?.snapshot
            ?: error("Follow-ups could not refresh. Connect and try again.")
        val online=response is FollowUpLoad.Loaded && remote.zone!=null && remote.heads.date!=null
        val today=if(online) requireNotNull(remote.heads.date) else Instant.ofEpochMilli(TrustedTime.trustedNowEpochMs() ?: System.currentTimeMillis())
            .atZone(remote.zone ?: BusinessClock.currentZone()).toLocalDate()
        val (customers,fresh)=facts(remote,today)
        val pending=db.followUpDataDao().commands().filter {it.bindingKey==a.binding.key}.mapNotNull {row->
            val kind=runCatching {FollowUpWire.operation(JSONObject(row.operationJson))}.getOrNull()
            if(kind in metadataKinds) FollowUpUnfinishedSave(row.commandUid,requireNotNull(kind)) else null
        }
        val b=Basis(UUID.randomUUID().toString(),a.binding,remote,customers,online,online&&fresh)
        basis=b
        return FollowUpWorkspaceSnapshot(remote.policy ?: FollowUpPolicy("not-configured",today),today,customers,
            remote.preferences,remote.acknowledgments,
            freshness=if(!online) "Saved on this device · Sync needed" else if(fresh) "Checked against synced records" else "Sync needed before reviewing customer work",
            online=online,owner=a.owner,primaryDevice=when(remote.primaryInstallation) {null->"Not selected";a.binding.installation->"This device";else->"Another device"},
            notificationsAllowed=notificationsAllowed(),shopZone=remote.zone?.id ?: "Shop timezone unavailable",hasMessageApp=hasMessageApp(),
            saveToken=b.token,reviewReady=b.reviewReady,unfinished=pending,configured=remote.policy!=null,
            activity=followUpActivity(remote,a.binding,customers),requests=followUpRequests(db.followUpDataDao().commands(),a,customers,online),
            claims=followUpClaims(remote,a,customers))
    }
    private suspend fun facts(remote: FollowUpRemoteSnapshot,today: LocalDate)=db.withReadTransaction {
        val customers=CustomerFollowUpReader(db,security).load(session,today)
        val credit=db.creditMetadataDao().state()
        val fresh=(db.syncPullStateDao().find()?.lastServerSeq ?: 0L)==remote.heads.sync &&
            credit?.shopUid==remote.shop && credit.lastServerSeq==remote.heads.credit && credit.enabled &&
            db.syncOutboxDao().count()==0L && db.syncRebaselineHoldDao().count()==0 &&
            db.syncParkedRowDao().all().isEmpty() && db.creditMetadataDao().pending().isEmpty()
        customers to fresh
    }
    private fun heads(h: FollowUpHeads)=JSONObject().put("sync",h.sync).put("credit",h.credit).put("metadata",h.metadata)
        .put("policy_revision",h.policyRevision ?: JSONObject.NULL).put("business_date",h.date?.toString() ?: JSONObject.NULL)
    private fun schedule(s: StatementReviewSchedule?): Any=when(s) {
        null->JSONObject.NULL
        is StatementReviewSchedule.Weekly->JSONObject().put("kind","WEEKLY").put("weekday",s.weekday.value)
        is StatementReviewSchedule.Monthly->JSONObject().put("kind","MONTHLY").put("day",s.day)
        is StatementReviewSchedule.Interval->JSONObject().put("kind","INTERVAL").put("days",s.days).put("first_date",s.firstDate.toString())
    }
    private suspend fun save(command: FollowUpCommand,token: String): FollowUpWorkspaceSave=runtime.withFollowUpSession(session) { a->
        val b=basis ?: error("Refresh follow-ups before saving.")
        check(b.token==token&&b.binding==a.binding&&b.online) {"This review changed. Refresh follow-ups before saving."}
        check(db.followUpDataDao().commands().none {it.bindingKey==a.binding.key}) {"Check the unfinished save first, or remove it from this device."}
        val op=when(command) {
            is FollowUpCommand.Policy->{
                check(a.owner) {"Only Owner can change these shop settings."}
                check(command.value.revision==(b.remote.policy?.revision ?: "not-configured")) {"The shop schedule changed. Refresh first."}
                check(!command.value.enabled||command.phonesUpdated) {"Confirm that all sales devices are updated."}
                val v=command.value
                JSONObject().put("kind","POLICY").put("expected_revision",b.remote.heads.policyRevision ?: JSONObject.NULL)
                    .put("make_primary",command.makePrimary).put("phones_updated",command.phonesUpdated)
                    .put("value",JSONObject().put("enabled",v.enabled).put("weekday",(v.statementSchedule as StatementReviewSchedule.Weekly).weekday.value)
                        .put("repeat_days",v.repeatDays).put("preferred_time",v.preferredTime.toString()).put("quiet_start",v.quietStart.toString()).put("quiet_end",v.quietEnd.toString()))
            }
            else->{
                val uid=when(command) {is FollowUpCommand.Preference->command.customerUid;is FollowUpCommand.Reviewed->command.customerUid;is FollowUpCommand.Snooze->command.customerUid;else->error("Unsupported save")}
                val customer=b.customers.singleOrNull {it.uid==uid} ?: error("Customer is no longer available.")
                val today=requireNotNull(b.remote.heads.date)
                val (current,fresh)=facts(b.remote,today)
                check(current.singleOrNull {it.uid==uid}==customer) {"Customer records changed. Refresh and review again."}
                val p=JSONObject().put("customer_uid",uid).put("expected_customer_revision",b.remote.customerRevisions[uid] ?: JSONObject.NULL)
                when(command) {
                    is FollowUpCommand.Preference->p.put("kind","PREFERENCE").put("value",JSONObject().put("schedule",schedule(command.value.schedule)).put("paused",command.value.paused))
                    else->{
                        check(b.reviewReady&&fresh) {"Sync current records before reviewing customer work."}
                        val row=CustomerFollowUps.evaluate(customer,b.remote.policy ?: error("Enable follow-ups first."),b.remote.preferences[uid],
                            b.remote.acknowledgments[uid] ?: FollowUpAcknowledgment(),today)
                        check(row!=null&&row.actionable) {"This customer no longer needs this follow-up. Refresh first."}
                        p.put("expected_heads",heads(b.remote.heads))
                        if(command is FollowUpCommand.Reviewed) {
                            check(command.on==today&&command.expectedCycle==row.statementCycle) {"Review date or statement changed. Refresh first."}
                            p.put("kind","REVIEW").put("cycle",row.statementCycle ?: "overdue:$today")
                        } else {
                            val until=(command as FollowUpCommand.Snooze).until
                            require(until>today&&until<=today.plusDays(365)) {"Choose a snooze date within the next year."}
                            p.put("kind","SNOOZE").put("until",until.toString())
                        }
                    }
                }
            }
        }
        val result=FollowUpDataStore(db).execute(a.binding,UUID.randomUUID().toString(),op,a.call)
        FollowUpWorkspaceSave(read(a),result is FollowUpCommandOutcome.Accepted)
    }
    private suspend fun recover(uid: String,setAside: Boolean): FollowUpWorkspaceSave=runtime.withFollowUpSession(session) { a->
        val row=db.followUpDataDao().command(uid) ?: error("This unfinished save is no longer present. Refresh first.")
        check(row.bindingKey==a.binding.key) {"This save belongs to another shop or sign-in."}
        val op=JSONObject(row.operationJson);val kind=FollowUpWire.operation(op)
        check(kind in metadataKinds) {"Review this message or alert under Activity & unfinished actions."}
        check(kind!="POLICY"||a.owner) {"Only Owner can check an unfinished shop-settings save."}
        if(setAside) {
            // Explicit local bookkeeping only. Never claims to cancel or undo server acceptance.
            db.followUpDataDao().complete(uid)
            FollowUpWorkspaceSave(read(a),true)
        } else {
            val result=FollowUpDataStore(db).execute(a.binding,uid,op,a.call)
            FollowUpWorkspaceSave(read(a),result is FollowUpCommandOutcome.Accepted)
        }
    }
    private suspend fun reviewRequest(uid: String,setAside: Boolean): FollowUpWorkspaceSave=runtime.withFollowUpSession(session) {a->
        // New central review is online-only. A cached mode cannot authorize replay/foreign-restore removal.
        val current=read(a)
        check(current.online) {"Connect to check the latest synced records before reviewing this action."}
        val row=db.followUpDataDao().command(uid) ?: error("This action is no longer listed. Refresh first.")
        val same=row.bindingKey==a.binding.key
        val operation=runCatching {JSONObject(row.operationJson).also {FollowUpWire.operation(it)}}.getOrNull()
        val kind=operation?.getString("kind")
        check(same||setAside&&a.owner) {"This action was saved under another connection. You cannot retry it here."}
        check(kind!="POLICY"||a.owner) {"Only Owner can check a shop-settings change."}
        a.validate()
        if(setAside) {
            db.followUpDataDao().complete(uid) // Local copy only; never releases a claim or confirms an outcome.
            FollowUpWorkspaceSave(read(a),true)
        } else {
            val result=FollowUpDataStore(db).execute(a.binding,uid,requireNotNull(operation),a.call)
            // Deliberately ignore every live side-effect grant. The durable existing row is recovery-only.
            FollowUpWorkspaceSave(read(a),result is FollowUpCommandOutcome.Accepted)
        }
    }
}
