package com.saaf.milkshop.domain

import java.time.Instant

enum class CreditFlagActionKind { SET, CLEAR }

/** An immutable review judgment; neither a ledger movement nor a verified staff identity. */
data class CreditFlagAction(
    val actionUid: String,
    val customerUid: String,
    val kind: CreditFlagActionKind,
    val clearsSetUid: String?,
    val reason: String,
    val localActorUid: String,
    val occurredAt: Instant,
) {
    init {
        require(actionUid.isNotBlank() && customerUid.isNotBlank() && localActorUid.isNotBlank())
        require(reason.isNotBlank() && reason.length <= 500) { "Enter a reason of up to 500 characters." }
        require(if (kind == CreditFlagActionKind.SET) clearsSetUid == null
            else !clearsSetUid.isNullOrBlank() && clearsSetUid != actionUid)
    }
}

data class CreditFlagReview(
    val activeSetUids: Set<String>,
    val missingSetUids: Set<String>,
) {
    val complete: Boolean get() = missingSetUids.isEmpty()
    val flagged: Boolean get() = activeSetUids.isNotEmpty()
}

/** Observed SETs can be cleared; a concurrent unseen SET always survives. No clock winner. */
object CreditFlagReviewCalculator {
    fun calculate(customerUid: String, actions: List<CreditFlagAction>): CreditFlagReview {
        require(customerUid.isNotBlank())
        val byUid = linkedMapOf<String, CreditFlagAction>()
        for (action in actions) {
            require(action.customerUid == customerUid) { "Review actions must belong to this customer." }
            val prior = byUid.putIfAbsent(action.actionUid, action)
            require(prior == null || prior == action) { "Conflicting review action identity." }
        }
        val missing = linkedSetOf<String>()
        val cleared = linkedSetOf<String>()
        for (action in byUid.values.filter { it.kind == CreditFlagActionKind.CLEAR }) {
            val targetUid = requireNotNull(action.clearsSetUid)
            val target = byUid[targetUid]
            if (target == null) missing += targetUid
            else {
                require(target.kind == CreditFlagActionKind.SET) { "A clear action must refer to a SET." }
                cleared += targetUid
            }
        }
        val active = byUid.values.filter { it.kind == CreditFlagActionKind.SET && it.actionUid !in cleared }
            .map { it.actionUid }.toSet()
        return CreditFlagReview(active, missing.toSet())
    }
}
