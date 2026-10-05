package com.saaf.milkshop.domain

import com.saaf.milkshop.data.ServerIdentityLinkEntity

/**
 * Account Bridge offline-grace policy, frozen by the owner on 2026-09-25
 * (ACCOUNT_BRIDGE_PHASE1_SPEC.md §6):
 *
 *  - grace: 7 days from the link's last successful verification;
 *  - post-expiry tier: writes blocked, reads allowed — enforced at the
 *    native command boundary, never in UI code;
 *  - expiry never deletes the link, the local user, or local data, and
 *    never fabricates a server denial;
 *  - a definitive server denial applies its own revocation rules
 *    immediately regardless of remaining grace.
 *
 * Enforcement is dormant for unlinked users and for link states that no
 * current flow can produce; only a previously verified entitlement
 * carries the grace clock.
 */
object ServerAccountPolicy {
    const val GRACE_DAYS = 7L
    const val GRACE_DURATION_MS = GRACE_DAYS * 24L * 60L * 60L * 1000L
    const val REASON_SERVER_GRACE_EXPIRED = "server_grace_expired"
    const val REASON_SERVER_GRACE_RECOVERED = "server_grace_recovered"
    const val VERIFICATION_REFRESH_THROTTLE_MS = 24L * 60L * 60L * 1000L

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

    /**
     * True when the linked user's verified entitlement has exceeded the
     * offline grace, which blocks write commands until a successful
     * re-verification. `UNKNOWN` network outcomes never reach this
     * function; they preserve the last verified state unchanged.
     */
    fun writeBlockedByGrace(status: String, verifiedAtEpochMs: Long?, nowEpochMs: Long): Boolean {
        if (status != LinkStatus.VERIFIED && status != LinkStatus.OFFLINE_GRACE) return false
        val verifiedAt = verifiedAtEpochMs ?: return false
        return nowEpochMs - verifiedAt > GRACE_DURATION_MS
    }

    fun linkWriteBlocked(link: ServerIdentityLinkEntity, nowEpochMs: Long): Boolean =
        writeBlockedByGrace(link.status, link.verifiedAtEpochMs, nowEpochMs)

    /**
     * H5 (audit 2026-10-02; spec §6 "writes blocked, reads allowed"): the
     * read-family permissions that survive offline-grace expiry. Everything
     * else stays blocked until the link verifies again — including
     * CREATE_EXPORT_BACKUP (an export is not an ERP write, but it is also
     * not a read) and reads gated on management permissions such as the
     * Users list (MANAGE_STAFF_USERS).
     */
    val GRACE_READ_PERMISSIONS: Set<AppPermission> = setOf(
        AppPermission.VIEW_OPERATIONAL,
        AppPermission.VIEW_FULL_FINANCIAL,
        AppPermission.VIEW_SECURITY_AUDIT,
    )

    enum class GraceDecision { ALLOW, BLOCK_WRITE }

    /** Pure decision — JVM-tested. Not expired ⇒ everything allows. */
    fun graceDecision(permission: AppPermission, expired: Boolean): GraceDecision =
        if (!expired || permission in GRACE_READ_PERMISSIONS) {
            GraceDecision.ALLOW
        } else {
            GraceDecision.BLOCK_WRITE
        }

    /**
     * H13: true when a successful authenticated server exchange should
     * refresh this link's verification clock — clock-carrying statuses only
     * (VERIFIED, OFFLINE_GRACE), a timestamp must exist, and at most one
     * refresh per throttle window. Other statuses (restored-unverified,
     * pending, …) stay untouched: enrollment links them. Pure — JVM-tested.
     */
    fun shouldRefreshVerification(status: String, verifiedAtEpochMs: Long?, nowEpochMs: Long): Boolean {
        if (status != LinkStatus.VERIFIED && status != LinkStatus.OFFLINE_GRACE) return false
        val verifiedAt = verifiedAtEpochMs ?: return false
        return nowEpochMs - verifiedAt >= VERIFICATION_REFRESH_THROTTLE_MS
    }

    /** The full requirePermission verdict: does THIS permission block for THIS link now? */
    fun graceWriteBlocked(
        link: ServerIdentityLinkEntity,
        permission: AppPermission,
        nowEpochMs: Long,
    ): Boolean =
        writeBlockedByGrace(link.status, link.verifiedAtEpochMs, nowEpochMs) &&
            graceDecision(permission, expired = true) == GraceDecision.BLOCK_WRITE
}
