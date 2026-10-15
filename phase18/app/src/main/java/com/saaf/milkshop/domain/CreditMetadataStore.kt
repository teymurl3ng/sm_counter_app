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
        require(movements.isNotEmpty() && movements.all { it.accountId == customer.accountId } && movements.sumOf { it.amount } > 0) {
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

    /** Until compatible transport is ready, normal credit sales retain explicit legacy terms. */
    suspend fun recordLegacySale(eventId: Long, customerAccountId: Long) {
        check(dao.state()?.enabled != true) { "Credit contract posting is not connected yet." }
        val event = requireNotNull(db.eventDao().findById(eventId))
        val customer = requireNotNull(db.accountDao().findById(customerAccountId))
        applyAgreement(CreditSaleAgreementEntity(event.eventUid, customer.uid, LEGACY, "NET_DAYS", 7, DEFAULT_REVISION))
    }

    companion object {
        const val DEFAULT_REVISION = "weekly-default-v1"
        const val LEGACY = "LEGACY_WEEKLY"
        const val RECORDED = "RECORDED"
    }
}

internal fun CreditReviewActionEntity.toAction() = CreditFlagAction(actionUid, customerUid,
    CreditFlagActionKind.valueOf(kind), clearsSetUid, reason, localActorUid, Instant.parse(occurredAt))
