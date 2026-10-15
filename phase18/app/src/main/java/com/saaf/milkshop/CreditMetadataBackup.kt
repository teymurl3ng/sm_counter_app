package com.saaf.milkshop

import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.CreditMetadataStore
import com.saaf.milkshop.domain.CreditTermKind
import com.saaf.milkshop.domain.CreditPaymentTerm
import com.saaf.milkshop.domain.toAction
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Included in the existing encrypted, transactional backup path, never a second backup format. */
internal object CreditMetadataBackup {
    private val collections = listOf("credit_term_revisions", "credit_term_heads", "credit_sale_agreements",
        "credit_review_actions", "credit_metadata_pending", "credit_metadata_sync_state")

    suspend fun write(db: MilkShopDatabase, data: JSONObject, includeSecurity: Boolean) {
        val dao = db.creditMetadataDao()
        val revisions = dao.revisions()
        val actions = dao.actions()
        val pending = dao.pending()
        val agreements = dao.agreements()
        val state = dao.state()
        require(includeSecurity || (revisions.isEmpty() && actions.isEmpty() && pending.isEmpty() &&
            agreements.all { it.evidence == CreditMetadataStore.LEGACY } && state?.enabled != true)) {
            "Credit agreements and review history require an encrypted security-bearing backup."
        }
        data.put(collections[0], JSONArray(revisions.map { r -> JSONObject().put("revision_uid", r.revisionUid)
            .put("customer_uid", r.customerUid).put("prior_revision_uid", r.priorRevisionUid).put("term_kind", r.termKind)
            .put("term_value", r.termValue).put("local_actor_uid", r.localActorUid).put("accepted_at", r.acceptedAt).put("contract_version", r.contractVersion) }))
        data.put(collections[1], JSONArray(dao.heads().map { JSONObject().put("customer_uid", it.customerUid).put("revision_uid", it.revisionUid) }))
        data.put(collections[2], JSONArray(agreements.map { r -> JSONObject().put("root_sale_uid", r.rootSaleUid).put("customer_uid", r.customerUid)
            .put("evidence", r.evidence).put("term_kind", r.termKind).put("term_value", r.termValue).put("revision_uid", r.revisionUid).put("contract_version", r.contractVersion) }))
        data.put(collections[3], JSONArray(actions.map { r -> JSONObject().put("action_uid", r.actionUid).put("customer_uid", r.customerUid)
            .put("kind", r.kind).put("clears_set_uid", r.clearsSetUid ?: JSONObject.NULL).put("reason", r.reason).put("local_actor_uid", r.localActorUid)
            .put("occurred_at", r.occurredAt).put("business_date", r.businessDate).put("contract_version", r.contractVersion) }))
        data.put(collections[4], JSONArray(pending.map { r -> JSONObject().put("operation_uid", r.operationUid).put("kind", r.kind)
            .put("payload_json", r.payloadJson).put("created_at", r.createdAt).put("held", r.held) }))
        data.put(collections[5], JSONArray(listOfNotNull(state).map { r -> JSONObject().put("state_id", r.stateId).put("shop_uid", r.shopUid)
            .put("last_server_seq", r.lastServerSeq).put("contract_version", r.contractVersion).put("enabled", r.enabled) }))
    }

    suspend fun clearForRestore(db: MilkShopDatabase, execute: suspend (String) -> Unit) {
        CreditMetadataMigrationSql.immutableTriggerNames.forEach { execute("DROP TRIGGER IF EXISTS $it") }
        val dao = db.creditMetadataDao()
        dao.clearPending(); dao.clearState(); dao.clearHeads(); dao.clearActions(); dao.clearAgreements(); dao.clearRevisions()
    }

    /** Caller owns the restore transaction; any malformed dependency/body rolls back everything. */
    suspend fun restore(db: MilkShopDatabase, data: JSONObject, schema: Int, execute: suspend (String) -> Unit) {
        if (schema < 13) {
            require(collections.none { data.has(it) }) { "Credit metadata cannot use a legacy backup schema." }
            execute(CreditMetadataMigrationSql.BACKFILL_LEGACY)
            CreditMetadataMigrationSql.immutableTriggers.forEach { execute(it) }
            return
        }
        fun rows(key: String): List<JSONObject> {
            val array = data.getJSONArray(key)
            return (0 until array.length()).map { array.getJSONObject(it) }
        }
        val store = CreditMetadataStore(db)
        val dao = db.creditMetadataDao()
        val revisions = rows(collections[0]).map { CreditTermRevisionEntity(it.creditText("revision_uid"), it.creditText("customer_uid"),
            it.creditText("prior_revision_uid"), it.creditText("term_kind"), it.creditInt("term_value"), it.creditText("local_actor_uid"),
            it.creditText("accepted_at"), it.creditInt("contract_version")) }
        require(revisions.map { it.revisionUid }.distinct().size == revisions.size)
        val unresolved = revisions.toMutableList()
        while (unresolved.isNotEmpty()) {
            val ready = unresolved.filter { it.priorRevisionUid == CreditMetadataStore.DEFAULT_REVISION || dao.revision(it.priorRevisionUid) != null }
            require(ready.isNotEmpty()) { "Credit revision history is incomplete or cyclic." }
            ready.forEach { store.applyAcceptedRevision(it, advanceHead = false) }
            unresolved.removeAll(ready.toSet())
        }
        val heads = rows(collections[1]).map { CreditTermHeadEntity(it.creditText("customer_uid"), it.creditText("revision_uid")) }
        require(heads.map { it.customerUid }.distinct().size == heads.size)
        for (head in heads) {
            val revision = requireNotNull(dao.revision(head.revisionUid))
            require(revision.customerUid == head.customerUid)
            // A restored accepted customer history must have one terminal head, not hidden branches.
            val customerRevisions = revisions.filter { it.customerUid == head.customerUid }
            val parents = customerRevisions.map { it.priorRevisionUid }.toSet()
            require(customerRevisions.filter { it.revisionUid !in parents }.map { it.revisionUid } == listOf(head.revisionUid))
            dao.saveHead(head)
        }
        require(revisions.map { it.customerUid }.toSet() == heads.map { it.customerUid }.toSet())
        val agreements = rows(collections[2]).map { CreditSaleAgreementEntity(it.creditText("root_sale_uid"), it.creditText("customer_uid"),
            it.creditText("evidence"), it.creditText("term_kind"), it.creditInt("term_value"), it.creditText("revision_uid"), it.creditInt("contract_version")) }
        require(agreements.map { it.rootSaleUid }.distinct().size == agreements.size)
        agreements.forEach { store.applyAgreement(it) }
        val actions = rows(collections[3]).map { CreditReviewActionEntity(it.creditText("action_uid"), it.creditText("customer_uid"),
            it.creditText("kind"), if (it.isNull("clears_set_uid")) null else it.creditText("clears_set_uid"), it.creditText("reason"),
            it.creditText("local_actor_uid"), it.creditText("occurred_at"), it.creditText("business_date"), it.creditInt("contract_version")) }
        require(actions.map { it.actionUid }.distinct().size == actions.size)
        actions.sortedBy { if (it.kind == "SET") 0 else 1 }.forEach { store.applyAction(it) }
        val pending = rows(collections[4]).map { CreditMetadataPendingEntity(it.creditText("operation_uid"), it.creditText("kind"),
            it.creditText("payload_json"), it.creditText("created_at"), true) }
        require(pending.map { it.operationUid }.distinct().size == pending.size)
        for (row in pending) {
            require(row.operationUid.isNotBlank())
            Instant.parse(row.createdAt)
            val body = JSONObject(row.payloadJson)
            val customerUid = body.creditText("customer_uid")
            store.requireCustomer(customerUid)
            require(body.creditInt("contract_version") == 1)
            when (row.kind) {
                "TERM_REVISION" -> {
                    require(body.creditText("revision_uid") == row.operationUid && body.creditText("local_actor_uid").isNotBlank())
                    CreditPaymentTerm(CreditTermKind.valueOf(body.creditText("term_kind")), body.creditInt("term_value"))
                    val prior = body.creditText("prior_revision_uid")
                    require(prior == CreditMetadataStore.DEFAULT_REVISION || dao.revision(prior)?.customerUid == customerUid)
                }
                "REVIEW_ACTION" -> {
                    val action = requireNotNull(dao.action(row.operationUid))
                    require(body.creditText("action_uid") == action.actionUid && customerUid == action.customerUid && body.creditText("kind") == action.kind &&
                        body.creditText("reason") == action.reason && body.creditText("local_actor_uid") == action.localActorUid &&
                        body.creditText("occurred_at") == action.occurredAt && body.creditText("business_date") == action.businessDate &&
                        (if (body.isNull("clears_set_uid")) null else body.creditText("clears_set_uid")) == action.clearsSetUid)
                    action.toAction()
                }
                else -> error("Unsupported pending credit operation.")
            }
            dao.insertPending(row)
        }
        val states = rows(collections[5])
        require(states.size <= 1)
        states.singleOrNull()?.let {
            require(it.creditInt("state_id") == 1 && it.creditText("shop_uid").isNotBlank() && it.creditLong("last_server_seq") >= 0 && it.creditInt("contract_version") in 0..1)
            // Keep the requirement, discard cached capability and cursor: re-enrollment/catch-up is mandatory.
            dao.saveState(CreditMetadataSyncStateEntity(shopUid = it.creditText("shop_uid"), enabled = it.creditBoolean("enabled")))
        }
        require((revisions.isEmpty() && actions.isEmpty() && pending.isEmpty() && agreements.all { it.evidence == CreditMetadataStore.LEGACY }) || dao.state()?.enabled == true) {
            "Contracted credit history is missing its required-contract marker."
        }
        CreditMetadataMigrationSql.immutableTriggers.forEach { execute(it) }
    }
}

private fun JSONObject.creditText(key: String): String = (get(key) as? String) ?: error("Credit $key must be text.")
private fun JSONObject.creditLong(key: String): Long {
    val value = get(key)
    require(value is Int || value is Long) { "Credit $key must be an integer." }
    return (value as Number).toLong()
}
private fun JSONObject.creditInt(key: String): Int {
    val value = creditLong(key)
    require(value in Int.MIN_VALUE..Int.MAX_VALUE) { "Credit $key is outside the supported range." }
    return value.toInt()
}
private fun JSONObject.creditBoolean(key: String): Boolean = (get(key) as? Boolean) ?: error("Credit $key must be boolean.")
