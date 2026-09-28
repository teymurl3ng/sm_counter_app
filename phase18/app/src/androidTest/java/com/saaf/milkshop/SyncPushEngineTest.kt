package com.saaf.milkshop

import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.SyncPushEngine
import com.saaf.milkshop.sync.SyncPushResult
import com.saaf.milkshop.sync.SyncPushTransport
import com.saaf.milkshop.sync.SyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Phase 3: the outbox drain engine. Strict order, transient retry, and the
 * halt-at-head rule — a row the server permanently refuses stays at the head
 * of the queue so nothing behind it (its correction, the close covering it)
 * can overtake it; fixing the cause resumes the drain with nothing lost.
 */
@RunWith(AndroidJUnit4::class)
class SyncPushEngineTest {

    private fun openDatabase(): MilkShopDatabase = MilkShopDatabase.createInMemory()

    private suspend fun enqueueEvents(database: MilkShopDatabase, count: Int) {
        repeat(count) { index ->
            database.eventDao().insert(
                EventEntity(
                    createdAt = "2026-09-28T10:0$index:00Z",
                    occurredAt = "2026-09-28",
                    eventType = "SALE",
                    saleQuantity = 1.0,
                    saleValue = 190.0,
                    unitRate = 190.0,
                ),
            )
        }
    }

    @Test
    fun drainsInStrictOrder_andEmptiesTheQueue() = runBlocking {
        val database = openDatabase()
        try {
            enqueueEvents(database, 3)
            val initial = database.syncOutboxDao().count()
            val seenIds = mutableListOf<Long>()
            val engine = SyncPushEngine(
                database,
                SyncPushTransport { rows ->
                    seenIds += rows.map { it.outboxId }
                    SyncPushResult.Success(rows.size)
                },
                batchSize = 7,
            )

            val outcome = engine.drainOnce()

            assertTrue(outcome is SyncPushEngine.DrainOutcome.Drained)
            assertEquals(initial.toInt(), (outcome as SyncPushEngine.DrainOutcome.Drained).pushed)
            assertEquals(0L, database.syncOutboxDao().count())
            assertEquals(seenIds.size, seenIds.distinct().size)
            assertEquals(seenIds, seenIds.sorted())
        } finally {
            database.close()
        }
    }

    @Test
    fun transientFailure_leavesQueueIntactForRetry() = runBlocking {
        val database = openDatabase()
        try {
            enqueueEvents(database, 2)
            val initial = database.syncOutboxDao().count()
            val engine = SyncPushEngine(database, SyncPushTransport { SyncPushResult.Transient("timeout") })

            val outcome = engine.drainOnce()

            assertTrue(outcome is SyncPushEngine.DrainOutcome.RetryLater)
            assertEquals(initial, database.syncOutboxDao().count())
        } finally {
            database.close()
        }
    }

    @Test
    fun permanentFailure_haltsAtHead_andResumesAfterFix() = runBlocking {
        val database = openDatabase()
        try {
            enqueueEvents(database, 3)
            val refusedUid = database.backupDataDao().events()[1].eventUid
            var refused = refusedUid
            var haltReasons = 0
            val engine = SyncPushEngine(
                database,
                SyncPushTransport { rows ->
                    when {
                        rows.any { it.rowUid == refused } ->
                            SyncPushResult.Permanent("policy_rejected", "row refused")
                        else -> SyncPushResult.Success(rows.size)
                    }
                },
                haltedListener = { _, _, _ -> haltReasons += 1 },
            )

            val outcome = engine.drainOnce()

            // Everything ahead of the refused row left; it and its dependent
            // stay queued, in order, at the head.
            assertTrue(outcome is SyncPushEngine.DrainOutcome.Halted)
            assertEquals("policy_rejected", (outcome as SyncPushEngine.DrainOutcome.Halted).reasonCode)
            assertEquals(2L, database.syncOutboxDao().count())
            val remaining = database.syncOutboxDao().nextBatch(10)
            assertEquals(refusedUid, remaining.first().rowUid)
            assertEquals(1, haltReasons)
            assertEquals(
                1,
                database.backupDataDao().securityAuditEntries()
                    .count { it.action == SyncPushEngine.ACTION_SYNC_PUSH_HALTED },
            )

            // The cause is fixed: the next drain retries the head and the
            // queue finishes.
            refused = ""
            val resumed = engine.drainOnce()
            assertTrue(resumed is SyncPushEngine.DrainOutcome.Drained)
            assertEquals(0L, database.syncOutboxDao().count())
            assertEquals(3L, database.eventDao().count().toLong())
        } finally {
            database.close()
        }
    }

    @Test
    fun missingTransport_isANoOp() = runBlocking {
        val database = openDatabase()
        try {
            enqueueEvents(database, 1)
            val initial = database.syncOutboxDao().count()
            val engine = SyncPushEngine(database, null)

            assertEquals(SyncPushEngine.DrainOutcome.NotConfigured, engine.drainOnce())
            assertEquals(initial, database.syncOutboxDao().count())
        } finally {
            database.close()
        }
    }

    @Test
    fun schedulerKick_recordsWorkWaiting() {
        val before = SyncScheduler.kickCountSnapshot()
        SyncScheduler.requestPush()
        SyncScheduler.requestPush()
        assertEquals(before + 2, SyncScheduler.kickCountSnapshot())
        assertTrue(SyncScheduler.lastKickEpochMsSnapshot() > 0)
    }
}
