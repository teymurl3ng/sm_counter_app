package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.*
import com.saaf.milkshop.sync.BusinessClock
import com.saaf.milkshop.sync.TrustedTime
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/** Owner command boundary. Not exposed in UI until the compatible metadata protocol is ready. */
internal class CreditReviewService(private val db: MilkShopDatabase, private val security: UserSecurityService) {
    private val dao get() = db.creditMetadataDao()
    private val store = CreditMetadataStore(db)
    private suspend fun owner(session: String): AppUserEntity {
        val actor = security.requirePermission(session, AppPermission.MANAGE_MASTER_DATA)
        if (actor.role != AppRole.OWNER.wireValue) throw SecurityFailure.PermissionDenied()
        return actor
    }
    private suspend fun ready() {
        val state = dao.state()
        check(state?.enabled == true && state.contractVersion == 1) { "Credit terms and review are not enabled on this installation." }
    }
    private fun now() = Instant.ofEpochMilli(TrustedTime.trustedNowEpochMs() ?: System.currentTimeMillis()).toString()

    suspend fun proposeTerms(session: String, customerUid: String, expectedRevisionUid: String,
        term: CreditPaymentTerm, operationUid: String = UUID.randomUUID().toString()) = db.withWriteTransaction {
        val actor = owner(session)
        ready()
        store.requireCustomer(customerUid)
        require(operationUid.isNotBlank())
        require((dao.head(customerUid)?.revisionUid ?: CreditMetadataStore.DEFAULT_REVISION) == expectedRevisionUid) { "credit_terms_revision_conflict" }
        val body = JSONObject().put("customer_uid", customerUid).put("prior_revision_uid", expectedRevisionUid)
            .put("revision_uid", operationUid).put("term_kind", term.kind.name).put("term_value", term.value)
            .put("local_actor_uid", "local-user:${actor.userId}").put("contract_version", 1).toString()
        val existing = dao.pending(operationUid)
        require(existing == null || (existing.kind == "TERM_REVISION" && existing.payloadJson == body)) { "Conflicting credit proposal identity." }
        if (existing == null) dao.insertPending(CreditMetadataPendingEntity(operationUid, "TERM_REVISION", body, now()))
        // No accepted head changes until the server acknowledges this proposal.
    }

    suspend fun setFlag(session: String, customerUid: String, reason: String,
        actionUid: String = UUID.randomUUID().toString()) = db.withWriteTransaction {
        val actor = owner(session)
        ready()
        store.requireCustomer(customerUid)
        val prior = dao.action(actionUid)
        val row = CreditReviewActionEntity(actionUid, customerUid, "SET", null, reason.trim(), "local-user:${actor.userId}",
            prior?.occurredAt ?: now(), prior?.businessDate ?: BusinessClock.today().toString())
        store.applyAction(row)
        queue(row)
    }

    suspend fun clearFlag(session: String, customerUid: String, reviewedSetUids: Set<String>, reason: String,
        operationUid: String = UUID.randomUUID().toString()) = db.withWriteTransaction {
        val actor = owner(session)
        ready()
        require(operationUid.isNotBlank() && reviewedSetUids.isNotEmpty())
        store.requireCustomer(customerUid)
        for (target in reviewedSetUids.sorted()) {
            val uid = UUID.nameUUIDFromBytes("$operationUid\u0000$target".toByteArray(Charsets.UTF_8)).toString()
            val prior = dao.action(uid)
            val row = CreditReviewActionEntity(uid, customerUid, "CLEAR", target, reason.trim(), "local-user:${actor.userId}",
                prior?.occurredAt ?: now(), prior?.businessDate ?: BusinessClock.today().toString())
            store.applyAction(row)
            queue(row)
        }
    }

    private suspend fun queue(row: CreditReviewActionEntity) {
        val body = JSONObject().put("action_uid", row.actionUid).put("customer_uid", row.customerUid).put("kind", row.kind)
            .put("clears_set_uid", row.clearsSetUid ?: JSONObject.NULL).put("reason", row.reason)
            .put("local_actor_uid", row.localActorUid).put("occurred_at", row.occurredAt).put("business_date", row.businessDate)
            .put("contract_version", 1).toString()
        val prior = dao.pending(row.actionUid)
        require(prior == null || (prior.kind == "REVIEW_ACTION" && prior.payloadJson == body)) { "Conflicting credit queue identity." }
        if (prior == null) dao.insertPending(CreditMetadataPendingEntity(row.actionUid, "REVIEW_ACTION", body, row.occurredAt))
    }
}
