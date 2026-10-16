package com.saaf.milkshop.sync

import android.content.Context

/**
 * Push kick hook. Every committed write at the command choke points calls
 * [requestPush]; once the installation is enrolled (sync Phase 6) each kick
 * schedules the background drain worker — fire-and-forget after the
 * transaction commits, entry never waits on it (D9). A missed kick on a
 * rare write is self-healing because any later kick drains the whole queue
 * from the head. Until enrollment the hook stays inert (counters only).
 */
object SyncScheduler {

    @Volatile private var kickCount: Long = 0
    @Volatile private var lastKickEpochMs: Long = 0
    @Volatile private var appContext: Context? = null
    @Volatile private var schedulingEnabled: Boolean = false
    @Volatile private var suppressedForTest: Boolean = false

    /** Called at app start: enables scheduling only for an enrolled device. */
    fun warmUp(context: Context) {
        appContext = context.applicationContext
        schedulingEnabled = SyncServerConfigStore.load(context) != null &&
            SyncShopBindingStore.load(context) != null
    }

    /** Enrollment success switches the kicks on; unenroll switches them off. */
    fun setSchedulingEnabled(enabled: Boolean) {
        schedulingEnabled = enabled
        appContext?.let(FollowUpIntegration::changed)
    }

    fun requestPush() {
        kickCount += 1
        lastKickEpochMs = System.currentTimeMillis()
        if(!suppressedForTest) appContext?.let(FollowUpIntegration::changed)
        if (!schedulingEnabled || suppressedForTest) return
        val context = appContext ?: return
        runCatching { SyncPushWorker.schedule(context) }
    }

    fun kickCountSnapshot(): Long = kickCount

    fun lastKickEpochMsSnapshot(): Long = lastKickEpochMs

    /** Test-only: counters keep counting, but nothing is scheduled. */
    fun suppressSchedulingForTest() {
        suppressedForTest = true
        schedulingEnabled = false
    }
    internal fun isSuppressedForTest()=suppressedForTest
}
