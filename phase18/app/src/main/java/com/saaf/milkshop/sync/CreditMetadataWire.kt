package com.saaf.milkshop.sync

import com.saaf.milkshop.data.*
import org.json.JSONObject

/** Strict protocol-1 codec: JSON strings/fractions never coerce into contractual integers. */
internal object CreditMetadataWire {
    fun text(p: JSONObject, key: String): String = (p.get(key) as? String)
        ?.also { require(it.isNotBlank()) { "Empty credit $key" } } ?: error("Credit $key must be text")
    fun long(p: JSONObject, key: String): Long {
        val v = p.get(key)
        require(v is Int || v is Long) { "Credit $key must be an integer" }
        return (v as Number).toLong()
    }
    fun version(p: JSONObject) { require(long(p, "contract_version") == 1L) { "Unsupported credit contract" } }
    private fun int(p: JSONObject, key: String) = long(p, key).also { require(it in 1..365) }.toInt()
    private fun keys(p: JSONObject, expected: Set<String>) { require(p.keys().asSequence().toSet() == expected) { "Unexpected credit fields" } }
    private val agreementKeys = setOf("root_sale_uid", "customer_uid", "evidence", "term_kind", "term_value", "revision_uid", "contract_version")
    private val revisionKeys = setOf("revision_uid", "customer_uid", "prior_revision_uid", "term_kind", "term_value", "local_actor_uid", "contract_version", "accepted_at")
    private val actionKeys = setOf("action_uid", "customer_uid", "kind", "clears_set_uid", "reason", "local_actor_uid", "occurred_at", "business_date", "contract_version")
    fun agreement(p: JSONObject): CreditSaleAgreementEntity {
        keys(p, agreementKeys); version(p)
        return CreditSaleAgreementEntity(text(p,"root_sale_uid"),text(p,"customer_uid"),text(p,"evidence"),
            text(p,"term_kind"),int(p,"term_value"),text(p,"revision_uid"))
    }
    fun agreement(row: CreditSaleAgreementEntity) = JSONObject().put("root_sale_uid",row.rootSaleUid)
        .put("customer_uid",row.customerUid).put("evidence",row.evidence).put("term_kind",row.termKind)
        .put("term_value",row.termValue).put("revision_uid",row.revisionUid).put("contract_version",row.contractVersion)
    fun revision(p: JSONObject): CreditTermRevisionEntity {
        keys(p, revisionKeys); version(p)
        return CreditTermRevisionEntity(text(p,"revision_uid"),text(p,"customer_uid"),text(p,"prior_revision_uid"),
            text(p,"term_kind"),int(p,"term_value"),text(p,"local_actor_uid"),text(p,"accepted_at"))
    }
    fun action(p: JSONObject): CreditReviewActionEntity {
        keys(p, actionKeys); version(p)
        return CreditReviewActionEntity(text(p,"action_uid"),text(p,"customer_uid"),text(p,"kind"),
            if (p.isNull("clears_set_uid")) null else text(p,"clears_set_uid"),text(p,"reason"),
            text(p,"local_actor_uid"),text(p,"occurred_at"),text(p,"business_date"))
    }
}
