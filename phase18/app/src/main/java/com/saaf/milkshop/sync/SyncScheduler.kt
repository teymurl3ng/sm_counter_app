package com.saaf.milkshop.sync

/**
 * Push kick hook (Phase 3). Every committed write at the command choke
 * points calls [requestPush]. Until push activation (sync Phase 6) the hook
 * is deliberately inert — it only records that work is waiting, so the
 * wiring can be observed and tested before any network exists. A kick is
 * fire-and-forget after the transaction commits; entry never waits on it
 * (D9), and a missed kick on a rare write is self-healing because any later
 * kick drains the whole queue from the head.
 */
object SyncScheduler {

    @Volatile private var kickCount: Long = 0
    @Volatile private var lastKickEpochMs: Long = 0

    fun requestPush() {
        kickCount += 1
        lastKickEpochMs = System.currentTimeMillis()
    }

    fun kickCountSnapshot(): Long = kickCount

    fun lastKickEpochMsSnapshot(): Long = lastKickEpochMs
}
