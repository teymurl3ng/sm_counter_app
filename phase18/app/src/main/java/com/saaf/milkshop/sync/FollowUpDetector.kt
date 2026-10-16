package com.saaf.milkshop.sync

import androidx.room3.withReadTransaction
import com.saaf.milkshop.application.owner.CustomerFollowUpReader
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

internal interface FollowUpAlertSink {fun allowed(): Boolean;fun post();fun cancel()}
internal enum class FollowUpDetection { CHECKED, RETRY, STOPPED }

/** Caller owns SyncOperationGate and live authenticated transport. Never manufactures a local login. */
internal class FollowUpDetector(private val db: MilkShopDatabase,private val attention: FollowUpAttentionStore,
    private val alerts: FollowUpAlertSink,private val trusted: ()->Boolean=TrustedTime::isAnchored) {
    suspend fun check(binding: FollowUpBinding,call: suspend (String,JSONObject)->FollowUpRpcResult): FollowUpDetection {
        val response=FollowUpDataStore(db).refresh(binding,call)
        if(response !is FollowUpLoad.Loaded) {
            if(response is FollowUpLoad.Unavailable && response.code in setOf("followup_not_authorized","followup_protocol_unavailable")) {
                attention.clear(binding.key);alerts.cancel();return FollowUpDetection.STOPPED
            }
            attention.stale(binding.key);return FollowUpDetection.RETRY
        }
        val remote=response.snapshot;val policy=remote.policy
        if(policy==null||!policy.enabled) {
            attention.update(binding.key) {it.copy(count=0,stale=false,checkedOn=remote.heads.date?.toString())}
            alerts.cancel();return FollowUpDetection.STOPPED
        }
        val zone=remote.zone;val day=remote.heads.date
        if(zone==null||day==null||!trusted()) {attention.stale(binding.key);return FollowUpDetection.RETRY}
        val count=db.withReadTransaction {
            val credit=db.creditMetadataDao().state()
            val fresh=(db.syncPullStateDao().find()?.lastServerSeq ?: 0L)==remote.heads.sync &&
                credit?.shopUid==binding.shop&&credit.lastServerSeq==remote.heads.credit&&credit.enabled&&
                db.syncOutboxDao().count()==0L&&db.syncRebaselineHoldDao().count()==0&&
                db.syncParkedRowDao().count()==0&&db.creditMetadataDao().pending().isEmpty()
            if(!fresh) null else CustomerFollowUpReader.acceptedAutomationFacts(db,day).count {customer->
                CustomerFollowUps.evaluate(customer,policy,remote.preferences[customer.uid],remote.acknowledgments[customer.uid] ?: FollowUpAcknowledgment(),day)?.actionable==true
            }
        }
        if(count==null) {attention.stale(binding.key);return FollowUpDetection.RETRY}
        attention.update(binding.key) {it.copy(count=count,stale=false,checkedOn=day.toString())}
        if(count==0) {alerts.cancel();return FollowUpDetection.CHECKED}
        val time=Instant.ofEpochMilli(remote.serverTime).atZone(zone).toLocalTime()
        if(remote.primaryInstallation!=binding.installation||remote.primaryActor!=binding.staff||time<policy.preferredTime||policy.isQuiet(time)||!alerts.allowed()) return FollowUpDetection.CHECKED
        if(attention.current(binding.key).attemptedOn==day.toString()) return FollowUpDetection.CHECKED
        // Durable before any network reservation or notification. A crash may miss an alert, never repeat one.
        attention.update(binding.key) {it.copy(attemptedOn=day.toString())}
        val old=db.followUpDataDao().commands().firstOrNull {row->row.bindingKey==binding.key&&runCatching {
            val p=JSONObject(row.operationJson);FollowUpWire.operation(p)=="DIGEST"&&p.getJSONObject("expected_heads").getString("business_date")==day.toString()
        }.getOrDefault(false)}
        val operation=old?.let {JSONObject(it.operationJson)} ?: JSONObject().put("kind","DIGEST").put("has_work",true)
            .put("expected_heads",JSONObject().put("sync",remote.heads.sync).put("credit",remote.heads.credit).put("metadata",remote.heads.metadata)
                .put("policy_revision",remote.heads.policyRevision ?: JSONObject.NULL).put("business_date",day.toString()))
        val result=FollowUpDataStore(db).execute(binding,old?.commandUid ?: UUID.randomUUID().toString(),operation,call)
        if(result is FollowUpCommandOutcome.Accepted&&result.grant==FollowUpLiveGrant.NOTIFY) alerts.post()
        if(result is FollowUpCommandOutcome.NeedsReview) {
            if(result.code=="followup_not_authorized") {attention.clear(binding.key);alerts.cancel()} else attention.stale(binding.key)
        }
        // Lost/denied acknowledgments do not retry an alert. A later run still recalculates persistent attention.
        return FollowUpDetection.CHECKED
    }
}
