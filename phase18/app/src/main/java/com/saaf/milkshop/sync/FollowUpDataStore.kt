package com.saaf.milkshop.sync

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.*
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.time.Instant
import java.io.IOException

internal sealed interface FollowUpRpcResult {
    data class Ok(val body: JSONObject): FollowUpRpcResult
    data class Retry(val code: String): FollowUpRpcResult
    data class Refused(val code: String): FollowUpRpcResult
    data object Unsupported: FollowUpRpcResult
}

internal class FollowUpClient(private val http: SyncHttp,private val config: SyncServerConfig,private val tokens: SyncTokenStore) {
    suspend fun call(name: String,body: JSONObject): FollowUpRpcResult {
        require(name in setOf("followup_snapshot_v1","followup_command_v1"))
        val session=tokens.load() ?: return FollowUpRpcResult.Retry("no_session")
        val response=try {http.execute(SyncHttpRequest("POST",config.url("/rest/v1/rpc/$name"),mapOf(
            AccountAuthClient.HEADER_APIKEY to config.publishableKey,AccountAuthClient.HEADER_AUTHORIZATION to "Bearer ${session.accessToken}",
            "Content-Type" to "application/json"),body.toString().toByteArray(Charsets.UTF_8)))}
        catch(_: IOException) {return FollowUpRpcResult.Retry("network")}
        if(response.status==401) return FollowUpRpcResult.Retry("unauthorized")
        if(response.status==404) return FollowUpRpcResult.Unsupported
        if(response.status in setOf(403,409,422)) return FollowUpRpcResult.Refused(code(stableErrorCodeOf(response)))
        if(response.status !in 200..299) return FollowUpRpcResult.Retry("http_${response.status}")
        val json=runCatching {JSONObject(response.bodyText().orEmpty())}.getOrNull() ?: return FollowUpRpcResult.Retry("malformed_response")
        if(json.opt("ok") !is Boolean) return FollowUpRpcResult.Retry("malformed_response")
        if(json.getBoolean("ok")&&name=="followup_snapshot_v1") {
            try {FollowUpWire.snapshot(json,FollowUpWire.text(body,"shop"))} catch(_: Exception) {return FollowUpRpcResult.Retry("malformed_response")}
            anchorFrom(response,json) // Only validated contractual time may update the shared clock.
        } else anchorFrom(response.copy(body=null)) // Receipts contain no contractual clock field.
        return if(json.getBoolean("ok")) FollowUpRpcResult.Ok(json) else FollowUpRpcResult.Refused(code(json.optJSONObject("error")?.optString("code")))
    }
    private fun code(raw: String?)=raw?.takeIf {it.matches(Regex("[a-z][a-z0-9_]{0,100}"))} ?: "followup_refused"
}

internal sealed interface FollowUpLoad {
    data class Loaded(val snapshot: FollowUpRemoteSnapshot): FollowUpLoad
    data class Unavailable(val code: String): FollowUpLoad
}
internal data class CachedFollowUp(val snapshot: FollowUpRemoteSnapshot,val stale: Boolean)
internal enum class FollowUpLiveGrant { NONE, LAUNCH, NOTIFY }
internal sealed interface FollowUpCommandOutcome {
    data class Accepted(val commandUid: String,val receipt: FollowUpReceipt,val grant: FollowUpLiveGrant): FollowUpCommandOutcome
    data class NeedsReview(val commandUid: String,val code: String): FollowUpCommandOutcome
}

/** Caller owns authenticated access and the sync gate. This never launches or schedules anything. */
internal class FollowUpDataStore(private val db: MilkShopDatabase) {
    private val dao get()=db.followUpDataDao()
    suspend fun cached(binding: FollowUpBinding): CachedFollowUp? {
        val row=dao.cache(binding.key) ?: return null
        return try {CachedFollowUp(FollowUpWire.snapshot(JSONObject(row.snapshotJson),binding.shop),row.stale)}
        catch(_: Exception) {dao.dropCache(binding.key);null}
    }
    suspend fun refresh(binding: FollowUpBinding,call: suspend (String,JSONObject)->FollowUpRpcResult): FollowUpLoad {
        dao.invalidate(binding.key)
        val response=try {call("followup_snapshot_v1",JSONObject().put("shop",binding.shop))} catch(_: IOException) {FollowUpRpcResult.Retry("network")}
        if(response !is FollowUpRpcResult.Ok) {
            if(response is FollowUpRpcResult.Refused) dao.dropCache(binding.key)
            return FollowUpLoad.Unavailable(error(response))
        }
        return try {
            val snapshot=FollowUpWire.snapshot(response.body,binding.shop)
            db.withWriteTransaction {
                val previous=dao.cache(binding.key)?.let {runCatching {FollowUpWire.snapshot(JSONObject(it.snapshotJson),binding.shop)}.getOrNull()}
                if(previous!=null) require(snapshot.serverTime>=previous.serverTime&&snapshot.heads.sync>=previous.heads.sync&&
                    snapshot.heads.credit>=previous.heads.credit&&snapshot.heads.metadata>=previous.heads.metadata)
                dao.saveCache(FollowUpCacheEntity(binding.key,snapshot.json,false))
            }
            FollowUpLoad.Loaded(snapshot)
        } catch(cancelled: CancellationException) {throw cancelled}
        catch(_: Exception) {FollowUpLoad.Unavailable("followup_invalid_snapshot")}
    }
    suspend fun execute(binding: FollowUpBinding,commandUid: String,operation: JSONObject,
        call: suspend (String,JSONObject)->FollowUpRpcResult): FollowUpCommandOutcome {
        FollowUpWire.uuid(commandUid)
        val proposal=JSONObject(operation.toString());FollowUpWire.operation(proposal)
        val recovery=db.withWriteTransaction {
            val previous=dao.command(commandUid)
            if(previous!=null) require(previous.bindingKey==binding.key&&jsonStructurallyEqual(JSONObject(previous.operationJson),proposal)) {"Follow-up request identity changed"}
            else dao.insertCommand(FollowUpPendingEntity(commandUid,binding.key,proposal.toString(),Instant.now().toString()))
            dao.invalidate(binding.key)
            previous!=null
        }
        val body=JSONObject().put("shop",binding.shop).put("installation",binding.installation)
            .put("command_uid",commandUid).put("operation",proposal)
        // A cancellation leaves the already-durable request present: the next invocation is recovery-only.
        val response=try {call("followup_command_v1",body)} catch(cancelled: CancellationException) {throw cancelled}
        catch(_: IOException) {FollowUpRpcResult.Retry("network")}
        if(response !is FollowUpRpcResult.Ok) {
            val code=error(response);dao.error(commandUid,code)
            if(response is FollowUpRpcResult.Refused&&code=="followup_not_authorized") dao.dropCache(binding.key)
            return FollowUpCommandOutcome.NeedsReview(commandUid,code)
        }
        val receipt=try {FollowUpWire.receipt(response.body,proposal)} catch(_: Exception) {
            dao.error(commandUid,"followup_invalid_ack")
            return FollowUpCommandOutcome.NeedsReview(commandUid,"followup_invalid_ack")
        }
        // Durable completion precedes a possible in-memory side effect. A crash may lose it, never replay it.
        dao.complete(commandUid)
        val grant=when {recovery||receipt.replayed->FollowUpLiveGrant.NONE;receipt.launch->FollowUpLiveGrant.LAUNCH;receipt.notify->FollowUpLiveGrant.NOTIFY;else->FollowUpLiveGrant.NONE}
        return FollowUpCommandOutcome.Accepted(commandUid,receipt.copy(launch=grant==FollowUpLiveGrant.LAUNCH,notify=grant==FollowUpLiveGrant.NOTIFY),grant)
    }
    private fun error(r: FollowUpRpcResult): String=when(r) {
        is FollowUpRpcResult.Retry->r.code
        is FollowUpRpcResult.Refused->r.code
        FollowUpRpcResult.Unsupported->"followup_protocol_unavailable"
        is FollowUpRpcResult.Ok->error("Expected unsuccessful response")
    }
}
