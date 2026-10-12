package com.saaf.milkshop.sync

import android.content.Context

/**
 * Re-baseline state that must survive the restore -> re-enroll gap (go 8).
 *
 *  - [needsFreshIdentity]: every restore/reset marks the installation so the
 *    next enrollment mints a fresh device_id. A restored device must never
 *    push as the identity that wrote its backup — same-origin pushes escape
 *    the server's conflict audit (the v2-after-backup silent-erasure case).
 *  - [prevDeviceId]: the device_id recorded in a format-5 backup. The next
 *    enrollment classifies restored outbox entries against server rows by it:
 *    own-origin entries drop (the server already holds this installation's
 *    later version), foreign-origin entries hold for owner review.
 *  - [restoredRebaseline]: a format-5 restore imported outbox rows, so the
 *    next enrollment runs the classified pull-first flow; formats 3/4 keep
 *    the legacy push-first flow (enqueueAll).
 *  - [liveRebaselinePending]: option-Z live re-baseline (2026-10-04 owner
 *    decision). A PLAIN enrollment enqueued the whole local history
 *    pre-held; the first drain classifies it against server row BODIES
 *    (release new work / drop in-sync rows / hold differing rows for owner
 *    review) before any push. Restore-origin enrollment keeps the
 *    origin-based flow and never sets this flag.
 */
class SyncRebaselineStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var needsFreshIdentity: Boolean
        get() = prefs.getBoolean(KEY_NEEDS_FRESH, false)
        set(value) { check(prefs.edit().putBoolean(KEY_NEEDS_FRESH, value).commit()) }

    var prevDeviceId: String?
        get() = prefs.getString(KEY_PREV_DEVICE, null)?.takeIf { it.isNotEmpty() }
        set(value) { check(prefs.edit().putString(KEY_PREV_DEVICE, value).commit()) }

    var restoredRebaseline: Boolean
        get() = prefs.getBoolean(KEY_RESTORED_REBASELINE, false)
        set(value) { check(prefs.edit().putBoolean(KEY_RESTORED_REBASELINE, value).commit()) }

    var liveRebaselinePending: Boolean
        get() = prefs.getBoolean(KEY_LIVE_REBASELINE, false)
        set(value) { check(prefs.edit().putBoolean(KEY_LIVE_REBASELINE, value).commit()) }

    /** The profile-only historical replay is one-time per server/shop/installation. */
    fun profileCatchupCompletedFor(identityKey: String): Boolean =
        prefs.getString(KEY_PROFILE_CATCHUP, null) == identityKey

    fun markProfileCatchupCompleted(identityKey: String) {
        check(prefs.edit().putString(KEY_PROFILE_CATCHUP, identityKey).commit())
    }

    /** Historical planning rows may have been consumed by a receiver that skipped them. */
    fun planningCatchupCompletedFor(identityKey: String): Boolean =
        prefs.getString(KEY_PLANNING_CATCHUP, null) == identityKey

    fun markPlanningCatchupCompleted(identityKey: String) {
        check(prefs.edit().putString(KEY_PLANNING_CATCHUP, identityKey).commit())
    }

    fun clear() {
        check(prefs.edit().clear().commit())
    }

    private companion object {
        const val PREFS = "sync_rebaseline"
        const val KEY_NEEDS_FRESH = "needs_fresh_identity"
        const val KEY_PREV_DEVICE = "prev_device_id"
        const val KEY_RESTORED_REBASELINE = "restored_rebaseline"
        const val KEY_LIVE_REBASELINE = "live_rebaseline_pending"
        const val KEY_PROFILE_CATCHUP = "profile_catchup_v1"
        const val KEY_PLANNING_CATCHUP = "planning_catchup_v1"
    }
}
