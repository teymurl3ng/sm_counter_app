package com.saaf.milkshop.domain

import com.saaf.milkshop.data.ServerIdentityLinkEntity

/**
 * Account Bridge offline-grace policy. Originally frozen by the owner on
 * 2026-09-25 (ACCOUNT_BRIDGE_PHASE1_SPEC.md §6, 7-day read-only grace);
 * REFORMED 2026-10-08 with Claude review (account/server program, Track 1):
 *
 *  - grace: 14 days from the enrollment's last successful verification —
 *    the clock belongs to the ENROLLMENT (the Room installation-binding
 *    row), not to the owner identity link, so an unresolved link can never
 *    silently disable enforcement;
 *  - post-expiry = DEGRADED mode, not read-only: daily shop entry is the
 *    hard requirement, so operational writes (sales, collections, cash
 *    count/close, deliveries, purchases, expenses, payroll, production,
 *    spoilage, attendance, corrections) stay ALLOWED; only privileged
 *    actions block (restore, user management, enrollment changes, business
 *    settings, owner drawings, reopen, master data) and non-operational
 *    reads;
 *  - expiry never deletes the link, the binding, the local user, or local
 *    data, and never fabricates a server denial;
 *  - a definitive server denial applies its own revocation rules
 *    immediately regardless of remaining grace;
 *  - SUSPENDED/REVOKED link states are RECORDED + DISPLAYED only for now
 *    (ENFORCE_LINK_STATES = false): activating them requires the full
 *    guardrail set — serialized refresh with a rotation-reuse test, two
 *    confirming verdicts minutes apart (gateway 401/403 never counts),
 *    automatic recovery on a later Allowed verdict, a break-glass
 *    passphrase window, and sync-only consequences before any write
 *    blocking.
 *
 * The grace clock is also carried in `server_identity_links` for display;
 * the binding row is authoritative for enforcement.
 */
object ServerAccountPolicy {
    const val GRACE_DAYS = 14L
    const val GRACE_DURATION_MS = GRACE_DAYS * 24L * 60L * 60L * 1000L
    const val REASON_SERVER_GRACE_EXPIRED = "server_grace_expired"
    const val REASON_SERVER_GRACE_RECOVERED = "server_grace_recovered"
    const val VERIFICATION_REFRESH_THROTTLE_MS = 24L * 60L * 60L * 1000L

    /**
     * A1' (Claude review 2026-10-08): server revocation verdicts are
     * recorded and displayed; state enforcement ships OFF until the
     * guardrail set above is built and tested.
     */
    const val ENFORCE_LINK_STATES = false

    object BindingStatus {
        const val PENDING = "pending"
        const val ACTIVE = "active"
        const val SUSPENDED = "suspended"
        const val RESTORED_UNVERIFIED = "restored_unverified"
        const val CONFLICT = "conflict"
    }

    object LinkStatus {
        const val PENDING = "pending"
        const val VERIFIED = "verified"
        const val OFFLINE_GRACE = "offline_grace"
        const val SUSPENDED = "suspended"
        const val REVOKED = "revoked"
        const val RESTORED_UNVERIFIED = "restored_unverified"
        const val CONFLICT = "conflict"
    }

    private fun clockExpired(verifiedAtEpochMs: Long, nowEpochMs: Long): Boolean =
        nowEpochMs - verifiedAtEpochMs > GRACE_DURATION_MS

    /**
     * True when the enrollment's verified entitlement has exceeded the
     * offline grace (degraded-mode trigger). Only an ACTIVE binding carries
     * the clock; UNKNOWN network outcomes never reach this function.
     */
    fun bindingGraceExpired(bindingStatus: String, verifiedAtEpochMs: Long?, nowEpochMs: Long): Boolean {
        if (bindingStatus != BindingStatus.ACTIVE) return false
        val verifiedAt = verifiedAtEpochMs ?: return false
        return clockExpired(verifiedAt, nowEpochMs)
    }

    /**
     * Warning ladder for the Sync screen while still inside grace: 0 = no
     * notice, then 1 (early), 2 (mid), 3 (urgent) at 25% / 50% / 85% of the
     * grace consumed. Proportional so the ladder survives threshold changes.
     */
    fun graceWarningLevel(verifiedAtEpochMs: Long?, nowEpochMs: Long): Int {
        val verifiedAt = verifiedAtEpochMs ?: return 0
        val consumed = (nowEpochMs - verifiedAt).coerceAtLeast(0L)
        return when {
            consumed >= GRACE_DURATION_MS * 85 / 100 -> 3
            consumed >= GRACE_DURATION_MS / 2 -> 2
            consumed >= GRACE_DURATION_MS / 4 -> 1
            else -> 0
        }
    }

    /**
     * Link-clock variant kept for display/record flows (the link row mirrors
     * the binding clock; the ROOM binding is authoritative for enforcement).
     * Clock-carrying link statuses: VERIFIED, OFFLINE_GRACE.
     */
    fun writeBlockedByGrace(status: String, verifiedAtEpochMs: Long?, nowEpochMs: Long): Boolean {
        if (status != LinkStatus.VERIFIED && status != LinkStatus.OFFLINE_GRACE) return false
        val verifiedAt = verifiedAtEpochMs ?: return false
        return clockExpired(verifiedAt, nowEpochMs)
    }

    /**
     * Reads that always survive (H5). In degraded mode the operational
     * writes below ALSO survive — daily entry is the hard requirement —
     * while privileged actions block.
     */
    val GRACE_READ_PERMISSIONS: Set<AppPermission> = setOf(
        AppPermission.VIEW_OPERATIONAL,
        AppPermission.VIEW_FULL_FINANCIAL,
        AppPermission.VIEW_SECURITY_AUDIT,
    )

    /**
     * Degraded-mode survivor writes (Claude review 2026-10-08: "allow
     * sales, cash and payments"). Everything NOT listed here and NOT a
     * VIEW_* read blocks on expiry — i.e. the privileged family: restore,
     * user management, enrollment-adjacent settings, owner drawings,
     * reopen, master data, export.
     */
    val GRACE_OPERATIONAL_PERMISSIONS: Set<AppPermission> = setOf(
        AppPermission.SALE_AND_COLLECTION,
        AppPermission.PURCHASE,
        AppPermission.SUPPLIER_PAYMENT,
        AppPermission.EXPENSE,
        AppPermission.SALARY_PAYMENT,
        AppPermission.PRODUCTION_AND_SPOILAGE,
        AppPermission.DELIVERY,
        AppPermission.ATTENDANCE,
        AppPermission.CORRECTION,
        AppPermission.CASH_COUNT_AND_CLOSE,
    )

    enum class GraceDecision { ALLOW, BLOCK_WRITE }

    /** Pure decision — JVM-tested. Not expired ⇒ everything allows. */
    fun graceDecision(permission: AppPermission, expired: Boolean): GraceDecision =
        if (!expired ||
            permission in GRACE_READ_PERMISSIONS ||
            permission in GRACE_OPERATIONAL_PERMISSIONS
        ) {
            GraceDecision.ALLOW
        } else {
            GraceDecision.BLOCK_WRITE
        }

    /**
     * H13: true when a successful authenticated server exchange should
     * refresh a clock — for the LINK (clock-carrying statuses VERIFIED /
     * OFFLINE_GRACE) and for the BINDING (status ACTIVE). At most one
     * refresh per throttle window. Pure — JVM-tested.
     */
    fun shouldRefreshVerification(status: String, verifiedAtEpochMs: Long?, nowEpochMs: Long): Boolean {
        if (status != LinkStatus.VERIFIED && status != LinkStatus.OFFLINE_GRACE) return false
        val verifiedAt = verifiedAtEpochMs ?: return false
        return nowEpochMs - verifiedAt >= VERIFICATION_REFRESH_THROTTLE_MS
    }

    fun shouldRefreshBindingVerification(bindingStatus: String, verifiedAtEpochMs: Long?, nowEpochMs: Long): Boolean {
        if (bindingStatus != BindingStatus.ACTIVE) return false
        val verifiedAt = verifiedAtEpochMs ?: return false
        return nowEpochMs - verifiedAt >= VERIFICATION_REFRESH_THROTTLE_MS
    }

    /** The full requirePermission verdict on the BINDING clock. */
    fun graceWriteBlockedBinding(
        bindingStatus: String,
        verifiedAtEpochMs: Long?,
        permission: AppPermission,
        nowEpochMs: Long,
    ): Boolean =
        bindingGraceExpired(bindingStatus, verifiedAtEpochMs, nowEpochMs) &&
            graceDecision(permission, expired = true) == GraceDecision.BLOCK_WRITE

    /** Link-clock variant (display/record flows). */
    fun graceWriteBlocked(
        link: ServerIdentityLinkEntity,
        permission: AppPermission,
        nowEpochMs: Long,
    ): Boolean =
        writeBlockedByGrace(link.status, link.verifiedAtEpochMs, nowEpochMs) &&
            graceDecision(permission, expired = true) == GraceDecision.BLOCK_WRITE

    /**
     * A1': when the flag ships ON, a SUSPENDED/REVOKED link blocks
     * everything except the read family. Display-only until then.
     */
    fun linkStateBlocked(status: String, expired: Boolean): Boolean =
        ENFORCE_LINK_STATES &&
            expired &&
            (status == LinkStatus.SUSPENDED || status == LinkStatus.REVOKED)
}
