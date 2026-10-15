package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.*
import java.time.Instant
import java.time.LocalDate

/** Internal validated persistence boundary. Remote callers still need protocol authorization. */
internal class CreditMetadataStore(private val db: MilkShopDatabase) {
    private val dao get() = db.creditMetadataDao()

    suspend fun requireCustomer(uid: String): AccountEntity =
        requireNotNull(db.syncEnvelopeDao().accountsByUid(listOf(uid)).singleOrNull()) { "Credit customer is missing." }
            .also { require(it.category == "Customer") { "Credit metadata requires a customer." } }

    fun term(kind: String, value: Int) = CreditPaymentTerm(CreditTermKind.valueOf(kind), value)

    suspend fun applyAcceptedRevision(row: CreditTermRevisionEntity, advanceHead: Boolean = true) = db.withWriteTransaction {
        requireCustomer(row.customerUid)
        require(row.contractVersion == 1 && row.revisionUid.isNotBlank() && row.revisionUid != DEFAULT_REVISION && row.localActorUid.isNotBlank())
        Instant.parse(row.acceptedAt)
        term(row.termKind, row.termValue)
        if (row.priorRevisionUid != DEFAULT_REVISION) {
            val parent = requireNotNull(dao.revision(row.priorRevisionUid)) { "Credit revision dependency is missing." }
            require(parent.customerUid == row.customerUid)
        }
        val existing = dao.revision(row.revisionUid)
        require(existing == null || existing == row) { "Conflicting credit revision identity." }
        // Replaying an old accepted revision cannot move the current head backwards.
        if (existing != null) return@withWriteTransaction
        val current = dao.head(row.customerUid)?.revisionUid ?: DEFAULT_REVISION
        if (advanceHead) require(current == row.priorRevisionUid) { "credit_terms_revision_conflict" }
        dao.insertRevision(row)
        if (advanceHead) dao.saveHead(CreditTermHeadEntity(row.customerUid, row.revisionUid))
    }

    suspend fun applyAgreement(row: CreditSaleAgreementEntity) = db.withWriteTransaction {
        val customer = requireCustomer(row.customerUid)
        val event = requireNotNull(db.syncEnvelopeDao().eventsByUid(listOf(row.rootSaleUid)).singleOrNull()) { "Credit sale is missing." }
        require(event.eventType == "SALE" && event.correctsEventId == null) { "Agreement must belong to a root sale." }
        val movements = db.financialLogDao().findByEventId(event.eventId)
        require(movements.isNotEmpty() && movements.all { it.accountId == customer.accountId && it.amount.isFinite() && it.amount > 0 } ) {
            "Agreement does not match the sale customer."
        }
        require(row.contractVersion == 1)
        val agreed = term(row.termKind, row.termValue)
        when (row.evidence) {
            LEGACY -> require(agreed == CreditPaymentTerm.Weekly && row.revisionUid == DEFAULT_REVISION)
            RECORDED -> if (row.revisionUid == DEFAULT_REVISION) require(agreed == CreditPaymentTerm.Weekly)
                else {
                    val revision = requireNotNull(dao.revision(row.revisionUid)) { "Credit agreement revision is missing." }
                    require(revision.customerUid == row.customerUid && term(revision.termKind, revision.termValue) == agreed)
                }
            else -> error("Unsupported credit evidence.")
        }
        val existing = dao.agreement(row.rootSaleUid)
        require(existing == null || existing == row) { "credit_agreement_conflict" }
        if (existing == null) dao.insertAgreement(row)
    }

    suspend fun applyAction(row: CreditReviewActionEntity) = db.withWriteTransaction {
        requireCustomer(row.customerUid)
        require(row.contractVersion == 1)
        LocalDate.parse(row.businessDate)
        val action = row.toAction()
        if (action.kind == CreditFlagActionKind.CLEAR) {
            val target = requireNotNull(dao.action(requireNotNull(row.clearsSetUid))) { "Credit flag dependency is missing." }
            require(target.customerUid == row.customerUid && target.kind == CreditFlagActionKind.SET.name)
        }
        val existing = dao.action(row.actionUid)
        require(existing == null || existing == row) { "credit_review_identity_conflict" }
        if (existing == null) dao.insertAction(row)
    }

    suspend fun evidence(customerUid: String): Map<String, CreditTermEvidence> = dao.agreementsFor(customerUid)
        .associate { row -> row.rootSaleUid to when (row.evidence) {
            LEGACY -> if (row.contractVersion == 1 && row.termKind == "NET_DAYS" && row.termValue == 7 && row.revisionUid == DEFAULT_REVISION)
                CreditTermEvidence.LegacyWeekly else CreditTermEvidence.Unavailable
            RECORDED -> runCatching { CreditTermEvidence.Recorded(CreditSaleAgreement(row.rootSaleUid, row.customerUid,
                term(row.termKind, row.termValue), row.revisionUid, row.contractVersion)) }.getOrDefault(CreditTermEvidence.Unavailable)
            else -> CreditTermEvidence.Unavailable
        } }

    /** Called inside the sale transaction: the accepted cached revision is the agreed term. */
    suspend fun recordSale(eventId: Long, customerAccountId: Long) {
        val event = requireNotNull(db.eventDao().findById(eventId))
        val customer = requireNotNull(db.accountDao().findById(customerAccountId))
        val state = dao.state()
        if (state?.enabled != true) {
            applyAgreement(CreditSaleAgreementEntity(event.eventUid, customer.uid, LEGACY, "NET_DAYS", 7, DEFAULT_REVISION))
            return
        }
        check(state.contractVersion == 1) { "Sync this installation before recording agreed credit terms." }
        db.serverAccountDao().findBinding()?.let { check(it.shopId == state.shopUid) { "Credit shop binding differs." } }
        val revision = dao.head(customer.uid)?.let { requireNotNull(dao.revision(it.revisionUid)) }
        applyAgreement(CreditSaleAgreementEntity(event.eventUid, customer.uid, RECORDED,
            revision?.termKind ?: "NET_DAYS", revision?.termValue ?: 7, revision?.revisionUid ?: DEFAULT_REVISION))
    }

    companion object {
        const val DEFAULT_REVISION = "weekly-default-v1"
        const val LEGACY = "LEGACY_WEEKLY"
        const val RECORDED = "RECORDED"
    }
}

internal fun CreditReviewActionEntity.toAction() = CreditFlagAction(actionUid, customerUid,
    CreditFlagActionKind.valueOf(kind), clearsSetUid, reason, localActorUid, Instant.parse(occurredAt))
