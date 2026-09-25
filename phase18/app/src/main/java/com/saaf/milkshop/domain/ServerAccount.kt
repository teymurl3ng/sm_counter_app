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
}
