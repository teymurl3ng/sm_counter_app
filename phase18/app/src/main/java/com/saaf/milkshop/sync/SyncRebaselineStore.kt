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

    fun clear() {
        check(prefs.edit().clear().commit())
    }

    private companion object {
        const val PREFS = "sync_rebaseline"
        const val KEY_NEEDS_FRESH = "needs_fresh_identity"
        const val KEY_PREV_DEVICE = "prev_device_id"
        const val KEY_RESTORED_REBASELINE = "restored_rebaseline"
    }
}
