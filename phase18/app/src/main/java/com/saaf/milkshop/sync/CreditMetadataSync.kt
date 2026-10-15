package com.saaf.milkshop.sync

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.CreditMetadataStore
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Separate cursor and queue. A failed dependency leaves its cursor before that record for retry. */
internal class CreditMetadataSync(private val db: MilkShopDatabase) {
    private val dao get() = db.creditMetadataDao()
    private val store = CreditMetadataStore(db)
    sealed interface Outcome {
        data object Done : Outcome
        data class Retry(val reason: String) : Outcome
        data class Halted(val code: String) : Outcome
    }

    suspend fun exchange(shop: String, call: suspend (String, JSONObject) -> CreditRpcResult,
        statusOnly: Boolean = false): Outcome {
        val previous = dao.state()
        val required = previous?.enabled == true || dao.pending().isNotEmpty()
        if (previous != null && previous.shopUid != shop) return Outcome.Halted("credit_shop_mismatch")
        val status = call("credit_contract_status_v1", JSONObject().put("shop",shop))
        if (status !is CreditRpcResult.Ok) {
            if (!required) return Outcome.Done // Older servers do not gate ordinary inactive-shop sync.
            if (status == CreditRpcResult.Unsupported && previous != null) dao.saveState(previous.copy(contractVersion=0))
            return outcome(status)
        }
        val enabled = try {
            CreditMetadataWire.version(status.body)
            require(CreditMetadataWire.text(status.body,"shop_id") == shop)
            status.body.get("enabled") as Boolean
        } catch (_: Exception) { return if (required) Outcome.Retry("credit_malformed_status") else Outcome.Done }
        if (previous?.enabled == true && !enabled) {
            dao.saveState(previous.copy(contractVersion=0))
            return Outcome.Halted("credit_required_contract_missing")
        }
        val state = previous ?: CreditMetadataSyncStateEntity(shopUid=shop)
        dao.saveState(state.copy(enabled=enabled, contractVersion=if (enabled) state.contractVersion else 1))
        if (!enabled) return if (dao.pending().isEmpty()) Outcome.Done else Outcome.Halted("credit_contract_not_enabled")
        if (statusOnly) return Outcome.Done

        // Restore discarded capability: replay from zero on every interrupted validation attempt.
        val revalidate = dao.state()!!.contractVersion == 0
        val confirmedRevisions = mutableSetOf<String>()
        if (revalidate) dao.saveState(dao.state()!!.copy(lastServerSeq=0))
        while (true) {
            val cursor = dao.state()!!.lastServerSeq
            val result = call("credit_metadata_pull_v1",JSONObject().put("shop",shop).put("since_seq",cursor).put("max_rows",100))
            if (result !is CreditRpcResult.Ok) return outcome(result)
            val records = try {
                CreditMetadataWire.version(result.body)
                result.body.getJSONArray("records").also { require(it.length() <= 100) }
            } catch (_: Exception) { return Outcome.Retry("credit_malformed_feed") }
            var prior = cursor
            for (i in 0 until records.length()) {
                try {
                    val record = records.getJSONObject(i)
                    val seq = CreditMetadataWire.long(record,"server_seq")
                    require(seq > prior && CreditMetadataWire.text(record,"shop_id") == shop)
                    val kind = CreditMetadataWire.text(record,"kind")
                    val uid = CreditMetadataWire.text(record,"uid")
                    val body = record.getJSONObject("payload")
                    db.withWriteTransaction {
                        apply(kind,uid,body)
                        acknowledge(kind,uid,body)
                        dao.saveState(dao.state()!!.copy(lastServerSeq=seq))
                    }
                    if (kind == "TERM_REVISION") confirmedRevisions += uid
                    prior = seq
                } catch (cancelled: CancellationException) { throw cancelled }
                  catch (_: Exception) {
                    dao.saveState(dao.state()!!.copy(contractVersion=0))
                    return Outcome.Retry("credit_dependency_or_body_refused")
                }
            }
            if (records.length() < 100) break
        }
        if (revalidate && dao.revisions().any { it.revisionUid !in confirmedRevisions }) {
            return Outcome.Halted("credit_restored_revision_unconfirmed")
        }
        dao.saveState(dao.state()!!.copy(contractVersion=1))

        // SET precedes CLEAR even when their UUIDs sort in the opposite order.
        val pending = dao.pending().filterNot { it.held }.sortedWith(compareBy<CreditMetadataPendingEntity> {
            if (it.kind == "REVIEW_ACTION" && JSONObject(it.payloadJson).optString("kind") == "CLEAR") 1 else 0
        }.thenBy { it.createdAt }.thenBy { it.operationUid })
        for (entry in pending) {
            val sent = JSONObject(entry.payloadJson)
            val result = call("credit_metadata_push_v1", JSONObject().put("shop",shop).put("operations",JSONArray().put(
                JSONObject().put("kind",entry.kind).put("uid",entry.operationUid).put("payload",sent))))
            if (result is CreditRpcResult.Refused) {
                hold(entry,result.code)
                return Outcome.Halted(result.code)
            }
            if (result !is CreditRpcResult.Ok) return outcome(result)
            try {
                val accepted = result.body.getJSONArray("accepted")
                require(accepted.length() == 1)
                val ack = accepted.getJSONObject(0)
                require(CreditMetadataWire.text(ack,"kind") == entry.kind && CreditMetadataWire.text(ack,"uid") == entry.operationUid)
                val body = ack.getJSONObject("payload")
                require(sameProposal(entry.kind, sent,body)) { "Credit acknowledgment body differs" }
                db.withWriteTransaction<Unit> {
                    apply(entry.kind,entry.operationUid,body)
                    acknowledge(entry.kind,entry.operationUid,body)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { return Outcome.Retry("credit_invalid_ack") }
        }
        return Outcome.Done
    }

    private fun outcome(r: CreditRpcResult): Outcome = when(r) {
        is CreditRpcResult.Retry -> Outcome.Retry(r.reason)
        is CreditRpcResult.Refused -> Outcome.Halted(r.code)
        CreditRpcResult.Unsupported -> Outcome.Retry("credit_protocol_unavailable")
        is CreditRpcResult.Ok -> error("Expected unsuccessful result")
    }
    private suspend fun apply(kind: String, uid: String, p: JSONObject) {
        when(kind) {
            "TERM_REVISION" -> store.applyAcceptedRevision(CreditMetadataWire.revision(p).also { require(it.revisionUid == uid) })
            "REVIEW_ACTION" -> store.applyAction(CreditMetadataWire.action(p).also { require(it.actionUid == uid) })
            "LEGACY_AGREEMENT" -> store.applyAgreement(CreditMetadataWire.agreement(p).also {
                require(it.rootSaleUid == uid && it.evidence == CreditMetadataStore.LEGACY)
            })
            else -> error("Unsupported credit metadata kind")
        }
    }
    private fun sameProposal(kind: String, proposed: JSONObject, accepted: JSONObject): Boolean {
        val body = JSONObject(accepted.toString())
        if (kind == "TERM_REVISION") body.remove("accepted_at")
        return jsonStructurallyEqual(proposed,body)
    }
    private suspend fun acknowledge(kind: String, uid: String, body: JSONObject) {
        val local = dao.pending(uid) ?: return
        require(local.kind == kind && sameProposal(kind,JSONObject(local.payloadJson),body)) { "Credit identity conflict" }
        dao.deletePending(uid) // includes restored held work only if an identical server acceptance exists
    }
    private suspend fun hold(entry: CreditMetadataPendingEntity, code: String) = db.withWriteTransaction {
        dao.setHeld(entry.operationUid,true)
        db.securityAuditDao().insert(SecurityAuditEntryEntity(occurredAt=Instant.now().toString(),
            action="credit_sync_held",result="warning",reasonCode=code,details="kind=${entry.kind};uid=${entry.operationUid}"))
    }
}
