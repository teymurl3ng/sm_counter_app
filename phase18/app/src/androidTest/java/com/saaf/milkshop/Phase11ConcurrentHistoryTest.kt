package com.saaf.milkshop

import android.os.SystemClock
import android.util.Log
import androidx.room3.withWriteTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.HistoryDataRepository
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.UserSecurityService
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11ConcurrentHistoryTest {
    @Test
    fun backupReadTransaction_excludesConcurrentWriteAndCorrectionAsOneUnit() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = MilkShopDatabase.createInMemory()
        val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var payload: ByteArray? = null
        try {
            val cashId = cashAccountId(database)
            val original = ExpenseService(database).record(
                ExpenseInput("Fuel", cashId, 100.0, "2026-09-04", "before snapshot"),
            )
            var writer = writerScope.async { Unit }
            val manager = BackupManager(context) {
                writer = writerScope.async {
                    ExpenseService(database).record(
                        ExpenseInput("Water", cashId, 40.0, "2026-09-04", "concurrent write"),
                    )
                    CorrectionService(database).correct(
                        CorrectionInput.Monetary(
                            originalEventId = original.eventId,
                            occurredAt = "2026-09-04",
                            amount = 75.0,
                            notes = "concurrent correction",
                        ),
                    )
                    Unit
                }
                assertNull(withTimeoutOrNull(150) { writer.await() })
            }

            payload = manager.buildCompleteSnapshotBytes(database)
            writer.await()

            val data = readAndVerify(payload).getJSONObject("data")
            assertEquals(1, data.getJSONArray("events").length())
            assertEquals(2, data.getJSONArray("financial_logs").length())
            assertEquals(3, database.eventDao().count())
            assertNotNull(database.eventDao().findById(original.eventId))
            assertTrue(database.eventDao().findAllDescending().any { it.correctsEventId == original.eventId })
        } finally {
            payload?.fill(0)
            writerScope.cancel()
            database.close()
        }
    }

    @Test
    fun backupDuringOpenCorrectionTransaction_neverSerializesPartialCorrection() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = MilkShopDatabase.createInMemory()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val correctionHeaderWritten = CompletableDeferred<Unit>()
        val releaseCorrection = CompletableDeferred<Unit>()
        var payload: ByteArray? = null
        try {
            val cashId = cashAccountId(database)
            val expenseId = database.accountDao().findByCategory("Operating Expense")
                .single { it.name == "Expense: Fuel" }
                .accountId
            val original = ExpenseService(database).record(
                ExpenseInput("Fuel", cashId, 100.0, "2026-09-04", "original"),
            )
            val writer = scope.async {
                database.withWriteTransaction {
                    val correctionId = database.eventDao().insert(
                        EventEntity(
                            createdAt = "2026-09-04T12:00:00Z",
                            occurredAt = "2026-09-04",
                            eventType = "EXPENSE",
                            correctsEventId = original.eventId,
                            amount = 75.0,
                            notes = "paused correction",
                        ),
                    )
                    correctionHeaderWritten.complete(Unit)
                    releaseCorrection.await()
                    database.financialLogDao().insert(
                        FinancialLogEntity(eventId = correctionId, accountId = cashId, amount = 25.0),
                    )
                    database.financialLogDao().insert(
                        FinancialLogEntity(eventId = correctionId, accountId = expenseId, amount = -25.0),
                    )
                    correctionId
                }
            }

            correctionHeaderWritten.await()
            val backup = scope.async { BackupManager(context).buildCompleteSnapshotBytes(database) }
            val completedAgainstOldSnapshot = withTimeoutOrNull(250) { backup.await() }
            releaseCorrection.complete(Unit)
            payload = completedAgainstOldSnapshot ?: backup.await()
            val correctionId = writer.await()

            val data = readAndVerify(payload).getJSONObject("data")
            val serializedEvents = data.getJSONArray("events")
            val serializedLogs = data.getJSONArray("financial_logs")
            val hasCorrection = (0 until serializedEvents.length())
                .map(serializedEvents::getJSONObject)
                .any { it.getLong("event_id") == correctionId }
            val correctionLogCount = (0 until serializedLogs.length())
                .map(serializedLogs::getJSONObject)
                .count { it.getLong("event_id") == correctionId }

            assertTrue(
                "Backup must contain either the state before the correction or its complete committed state",
                (!hasCorrection && correctionLogCount == 0) || (hasCorrection && correctionLogCount == 2),
            )
            assertEquals(2, database.eventDao().count())
            assertEquals(2, database.financialLogDao().findByEventId(correctionId).size)
        } finally {
            releaseCorrection.complete(Unit)
            payload?.fill(0)
            scope.cancel()
            database.close()
        }
    }

    @Test
    fun fiveThousandEventHistory_meetsQueryMemoryBackupAndRepeatedRecoveryBounds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val restored = MilkShopDatabase.createInMemory()
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val existingBackups = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val sourcePassphrase = "phase eleven load owner passphrase 2026".toCharArray()
        val replacedPassphrase = "phase eleven load replaced passphrase 2026".toCharArray()
        var payload: ByteArray? = null
        try {
            val sourceSecurity = UserSecurityService(source)
            val owner = sourceSecurity.bootstrapOwner("load.owner", "Load Owner", sourcePassphrase)
            val cashId = cashAccountId(source)
            val expenseId = source.accountDao().findByCategory("Operating Expense")
                .single { it.name == "Expense: Fuel" }
                .accountId

            val seedMs = elapsedMs {
                seedLargeExpenseHistory(source, owner.user.userId, cashId, expenseId)
            }
            assertEquals(LARGE_EVENT_COUNT, source.eventDao().count())

            val history = HistoryDataRepository(source)
            var historyCount = 0
            val historyMs = elapsedMs { historyCount = history.allEvents().size }
            assertEquals(LARGE_EVENT_COUNT, historyCount)

            var detailLines = 0
            val detailMs = elapsedMs {
                val detail = history.detailData(1L)
                detailLines = detail.inventoryLines.size + detail.financialLines.size
            }
            assertEquals(2, detailLines)

            var effectiveEventCount = 0L
            val analyticsMs = elapsedMs {
                effectiveEventCount = source.analyticsReadDao()
                    .periodSummary("2025-01-01", "2026-01-02")
                    .eventCount
            }
            assertEquals(ORIGINAL_EVENT_COUNT.toLong(), effectiveEventCount)

            var ledgerRows = 0
            val ledgerMs = elapsedMs {
                ledgerRows = source.financialLogDao().findAccountLedger(cashId).size
            }
            assertEquals(LARGE_EVENT_COUNT, ledgerRows)

            val heapBeforeBackup = usedHeapBytes()
            val manager = BackupManager(context)
            val backupMs = elapsedMs { payload = manager.buildCompleteSnapshotBytes(source) }
            val heapWithPayload = usedHeapBytes()
            assertNotNull(payload)
            assertTrue(payload!!.size < MAX_PAYLOAD_BYTES)
            assertTrue((heapWithPayload - heapBeforeBackup).coerceAtLeast(0) < MAX_HEAP_GROWTH_BYTES)

            val restoredSessions = NativeSessionManager()
            val replacedOwner = UserSecurityService(restored, restoredSessions).bootstrapOwner(
                "load.replaced",
                "Load Replaced",
                replacedPassphrase,
            )
            var firstRestoreSucceeded = false
            val firstRestoreMs = elapsedMs {
                firstRestoreSucceeded = manager.restoreCompleteSnapshotReplacing(
                    restored,
                    payload!!,
                    restoredSessions,
                    "phase11-load-generation-1.milkbackup",
                ).isSuccess
            }
            assertTrue(firstRestoreSucceeded)
            assertTrue(runCatching { restoredSessions.require(replacedOwner.session.sessionId) }.isFailure)
            assertEquals(LARGE_EVENT_COUNT, restored.eventDao().count())
            assertEquals(LARGE_EVENT_COUNT, restored.backupDataDao().eventActorLinks().size)

            val recoveredSecurity = UserSecurityService(restored, restoredSessions)
            val recoveredSession = recoveredSecurity.login(owner.user.loginName, sourcePassphrase)
            ExpenseService(restored).record(
                ExpenseInput("Water", cashAccountId(restored), 11.0, "2026-09-04", "between restores"),
            )
            assertEquals(LARGE_EVENT_COUNT + 1, restored.eventDao().count())

            var secondRestoreSucceeded = false
            val secondRestoreMs = elapsedMs {
                secondRestoreSucceeded = manager.restoreCompleteSnapshotReplacing(
                    restored,
                    payload!!,
                    restoredSessions,
                    "phase11-load-generation-2.milkbackup",
                ).isSuccess
            }
            assertTrue(secondRestoreSucceeded)
            assertTrue(runCatching { restoredSessions.require(recoveredSession.sessionId) }.isFailure)
            assertEquals(LARGE_EVENT_COUNT, restored.eventDao().count())
            assertFalse(restored.eventDao().findAllDescending().any { it.notes == "between restores" })

            assertMeasuredBound("seed", seedMs, MAX_SEED_MS)
            assertMeasuredBound("history", historyMs, MAX_QUERY_MS)
            assertMeasuredBound("detail", detailMs, MAX_QUERY_MS)
            assertMeasuredBound("analytics", analyticsMs, MAX_QUERY_MS)
            assertMeasuredBound("ledger", ledgerMs, MAX_QUERY_MS)
            assertMeasuredBound("backup", backupMs, MAX_BACKUP_MS)
            assertMeasuredBound("first restore", firstRestoreMs, MAX_RESTORE_MS)
            assertMeasuredBound("second restore", secondRestoreMs, MAX_RESTORE_MS)

            val metrics = JSONObject()
                .put("events", LARGE_EVENT_COUNT)
                .put("financialRows", LARGE_EVENT_COUNT * 2)
                .put("actorRows", LARGE_EVENT_COUNT)
                .put("seedMs", seedMs)
                .put("historyMs", historyMs)
                .put("detailMs", detailMs)
                .put("analyticsMs", analyticsMs)
                .put("ledgerMs", ledgerMs)
                .put("backupMs", backupMs)
                .put("firstRestoreMs", firstRestoreMs)
                .put("secondRestoreMs", secondRestoreMs)
                .put("payloadBytes", payload!!.size)
                .put("heapGrowthBytes", (heapWithPayload - heapBeforeBackup).coerceAtLeast(0))
                .put("maxHeapBytes", Runtime.getRuntime().maxMemory())
            Log.i(METRICS_TAG, metrics.toString())
            println("$METRICS_TAG ${metrics}")
        } finally {
            payload?.fill(0)
            sourcePassphrase.fill('\u0000')
            replacedPassphrase.fill('\u0000')
            source.close()
            restored.close()
            backupDir.listFiles()?.filter { it.name !in existingBackups }?.forEach { it.delete() }
        }
    }

    private suspend fun seedLargeExpenseHistory(
        database: MilkShopDatabase,
        actorUserId: Long,
        cashId: Long,
        expenseId: Long,
    ) {
        val events = ArrayList<EventEntity>(LARGE_EVENT_COUNT)
        val financialLogs = ArrayList<FinancialLogEntity>(LARGE_EVENT_COUNT * 2)
        val actorLinks = ArrayList<EventActorLinkEntity>(LARGE_EVENT_COUNT)
        val start = LocalDate.of(2025, 1, 1)

        for (index in 1..ORIGINAL_EVENT_COUNT) {
            val eventId = index.toLong()
            val amount = 100.0 + (index % 17)
            val occurredAt = start.plusDays((index % 365).toLong()).toString()
            val createdAt = "${occurredAt}T12:00:00Z"
            events += EventEntity(
                eventId = eventId,
                createdAt = createdAt,
                occurredAt = occurredAt,
                eventType = "EXPENSE",
                amount = amount,
                notes = "large-history-original-$index",
            )
            financialLogs += FinancialLogEntity(eventId = eventId, accountId = cashId, amount = -amount)
            financialLogs += FinancialLogEntity(eventId = eventId, accountId = expenseId, amount = amount)
            actorLinks += actorLink(eventId, actorUserId, createdAt)
        }

        for (index in 1..CORRECTION_EVENT_COUNT) {
            val eventId = (ORIGINAL_EVENT_COUNT + index).toLong()
            val originalId = index.toLong()
            val originalAmount = 100.0 + (index % 17)
            val occurredAt = start.plusDays((index % 365).toLong()).toString()
            val createdAt = "${occurredAt}T13:00:00Z"
            events += EventEntity(
                eventId = eventId,
                createdAt = createdAt,
                occurredAt = occurredAt,
                eventType = "EXPENSE",
                correctsEventId = originalId,
                amount = originalAmount + CORRECTION_DELTA,
                notes = "large-history-correction-$index",
            )
            financialLogs += FinancialLogEntity(eventId = eventId, accountId = cashId, amount = -CORRECTION_DELTA)
            financialLogs += FinancialLogEntity(eventId = eventId, accountId = expenseId, amount = CORRECTION_DELTA)
            actorLinks += actorLink(eventId, actorUserId, createdAt)
        }

        database.withWriteTransaction {
            val backupDao = database.backupDataDao()
            backupDao.insertEvents(events)
            backupDao.insertFinancialLogs(financialLogs)
            backupDao.insertEventActorLinks(actorLinks)
        }
    }

    private fun actorLink(eventId: Long, actorUserId: Long, attributedAt: String) = EventActorLinkEntity(
        eventId = eventId,
        actorUserId = actorUserId,
        actorNameSnapshot = "Load Owner",
        actorRoleSnapshot = "owner",
        attributedAt = attributedAt,
    )

    private suspend fun cashAccountId(database: MilkShopDatabase): Long =
        database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

    private inline fun elapsedMs(block: () -> Unit): Long {
        val started = SystemClock.elapsedRealtime()
        block()
        return SystemClock.elapsedRealtime() - started
    }

    private fun usedHeapBytes(): Long = Runtime.getRuntime().run { totalMemory() - freeMemory() }

    private fun assertMeasuredBound(label: String, actualMs: Long, maximumMs: Long) {
        assertTrue("$label took ${actualMs}ms; bound is ${maximumMs}ms", actualMs <= maximumMs)
    }

    private companion object {
        const val ORIGINAL_EVENT_COUNT = 4_500
        const val CORRECTION_EVENT_COUNT = 500
        const val LARGE_EVENT_COUNT = ORIGINAL_EVENT_COUNT + CORRECTION_EVENT_COUNT
        const val CORRECTION_DELTA = 5.0
        const val MAX_PAYLOAD_BYTES = 64 * 1024 * 1024
        const val MAX_HEAP_GROWTH_BYTES = 192L * 1024 * 1024
        const val MAX_SEED_MS = 60_000L
        const val MAX_QUERY_MS = 15_000L
        const val MAX_BACKUP_MS = 60_000L
        const val MAX_RESTORE_MS = 120_000L
        const val METRICS_TAG = "Phase11Metrics"
    }
}
