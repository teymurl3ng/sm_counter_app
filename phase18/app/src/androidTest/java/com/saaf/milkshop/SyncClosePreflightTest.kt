package com.saaf.milkshop

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseDayRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseFacade
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.Instant
import org.json.JSONObject
import kotlinx.coroutines.CancellationException

/** Isolated close import boundary: no remote Auth, radio change or ordinary database. */
@RunWith(AndroidJUnit4::class)
class SyncClosePreflightTest {
    @Test fun verifiedCashierCloseMustReachAnotherUserInitializedPhone() = runBlocking {
        verifyReceiver(initialized = true)
    }

    @Test fun emptyReceiverControlAcceptsTheSameVerifiedCloseEnvelope() = runBlocking {
        verifyReceiver(initialized = false)
    }

    @Test fun refusedCloseIsRetainedUntilCorrectedWithoutWeakeningLocalActors() = runBlocking {
        verifyReceiver(initialized = true) { target, records, day ->
            val close = records.single { it.tableName == "daily_close_actions" }
            SyncApplyEngine(target).apply(records.filter { it !== close })
            val guard = target.syncApplyDao().verifiedCloseActorTriggerSql()
            val money = target.backupDataDao().financialLogs()
            // Parsed but semantically invalid: fails inside the insertion,
            // after the local-login guard has been temporarily removed.
            val bad = close.copy(serverSeq = close.serverSeq + 100,
                payload = JSONObject(close.payload.toString()).put("business_date", day.plusDays(1).toString()))
            val refusal = SyncApplyEngine(target).apply(listOf(bad))
            assertEquals(1, refusal.parked)
            assertEquals(0, refusal.skipped)
            assertTrue(target.backupDataDao().dailyCloseActions().isEmpty())
            assertEquals(bad.payload.toString(), target.syncParkedRowDao().find(close.tableName, close.rowUid)!!.payloadJson)
            assertEquals(guard, target.syncApplyDao().verifiedCloseActorTriggerSql())
            SyncApplyEngine(target).apply(emptyList())
            assertNotNull(target.syncParkedRowDao().find(close.tableName, close.rowUid))
            assertLocalActorsRefused(target, day.plusDays(2))
            val corrected = close.copy(serverSeq = bad.serverSeq + 1)
            assertEquals(0, SyncApplyEngine(target).apply(listOf(corrected)).skipped)
            assertEquals(0, target.syncParkedRowDao().count())
            assertEquals(money, target.backupDataDao().financialLogs())
            assertEquals(guard, target.syncApplyDao().verifiedCloseActorTriggerSql())
            assertLocalActorsRefused(target, day.plusDays(3))
        }
    }

    @Test fun immutableReplayCannotDiscardANewerRefusedBody() = runBlocking {
        verifyReceiver(initialized = true) { target, records, _ ->
            val close = records.single { it.tableName == "daily_close_actions" }
            SyncApplyEngine(target).apply(records)
            val history = target.backupDataDao().dailyCloseActions()
            val bad = close.copy(serverSeq = close.serverSeq + 100,
                payload = JSONObject(close.payload.toString()).put("actor_name", "Changed immutable author"))
            assertEquals(1, SyncApplyEngine(target).apply(listOf(bad)).parked)
            SyncApplyEngine(target).apply(listOf(close))
            assertEquals(bad.serverSeq, target.syncParkedRowDao().find(close.tableName, close.rowUid)!!.serverSeq)
            assertEquals(history, target.backupDataDao().dailyCloseActions())
            SyncApplyEngine(target).apply(listOf(close.copy(serverSeq = bad.serverSeq + 1)))
            assertEquals(0, target.syncParkedRowDao().count())
            assertEquals(history, target.backupDataDao().dailyCloseActions())
        }
    }

    @Test fun missingCountRetriesWhenParentArrivesAndCancellationRollsBack() = runBlocking {
        verifyReceiver(initialized = true) { target, records, _ ->
            val close = records.single { it.tableName == "daily_close_actions" }
            val count = records.single { it.tableName == "physical_cash_counts" }
            SyncApplyEngine(target).apply(records.filter { it !== close && it !== count })
            val guard = target.syncApplyDao().verifiedCloseActorTriggerSql()
            assertEquals(1, SyncApplyEngine(target).apply(listOf(close)).parked)
            val parked = target.syncParkedRowDao().all()
            val aborted = runCatching {
                target.withWriteTransaction {
                    SyncApplyEngine(target).apply(listOf(count))
                    assertTrue(target.dailyCloseDao().isPostingClosed(close.payload.getString("business_date")))
                    throw CancellationException("Synthetic transaction interruption")
                }
            }.exceptionOrNull()
            assertTrue(aborted is CancellationException)
            assertTrue(target.backupDataDao().physicalCashCounts().isEmpty())
            assertTrue(target.backupDataDao().dailyCloseActions().isEmpty())
            assertEquals(parked, target.syncParkedRowDao().all())
            assertEquals(guard, target.syncApplyDao().verifiedCloseActorTriggerSql())
            SyncApplyEngine(target).apply(listOf(count))
            assertEquals(0, target.syncParkedRowDao().count())
            assertEquals(guard, target.syncApplyDao().verifiedCloseActorTriggerSql())
        }
    }

    @Test fun sequentialCloseReopenAndRecountHistoryRemapsEveryLink() = runBlocking {
        verifyReceiver(initialized = true, includeReclose = true)
    }

    @Test fun queuedCloseReopenAndRecountMustImportInQueueOrder() = runBlocking {
        verifyReceiver(initialized = true, includeReclose = true, coalescedHistory = true)
    }

    private suspend fun assertLocalActorsRefused(target: MilkShopDatabase, day: LocalDate) {
        val cash = target.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
        val countId = target.physicalCashCountDao().insert(PhysicalCashCountEntity(
            accountId = cash.accountId, businessDate = day.toString(), countedAmount = 10000.0,
            countedAt = day.toString() + "T23:59:59Z"))
        val owner = target.appUserDao().all().single()
        val before = target.backupDataDao().dailyCloseActions()
        for (actorId in listOf(null, owner.userId + 10000)) {
            val failure = runCatching {
                target.backupDataDao().insertDailyCloseAction(DailyCloseActionEntity(
                    businessDate = day.toString(), action = "close", physicalCashCountId = countId,
                    actorName = owner.displayName, actorRole = owner.role, actorUserId = actorId,
                    createdAt = Instant.now().toString()))
            }.exceptionOrNull()
            assertNotNull("Ordinary unverified local close must still fail", failure)
            assertTrue(failure!!.message.orEmpty().contains("verified user"))
        }
        assertEquals(before, target.backupDataDao().dailyCloseActions())
    }

    private suspend fun verifyReceiver(initialized: Boolean, includeReclose: Boolean = false, coalescedHistory: Boolean = false,
        probe: suspend (MilkShopDatabase, List<SyncPulledRecord>, LocalDate) -> Unit = { _, _, _ -> }) {
        SyncScheduler.suppressSchedulingForTest()
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        val day = LocalDate.now(BusinessClock.currentZone())
        try {
            val security = UserSecurityService(source)
            val owner = security.bootstrapOwner("close.source.owner", "Preflight Owner", OWNER.toCharArray())
            val pending = security.beginUserEnrollment(owner.session.sessionId,
                CreateUserInput("close.source.cashier", "Preflight Cashier", "cashier"))
            security.completeUserEnrollment(pending.enrollmentToken, CASHIER.toCharArray())
            val cashier = security.login("close.source.cashier", CASHIER.toCharArray())
            val cash = source.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
            StoreIdentityService(source).save(StoreIdentityInput("Close receive preflight"))
            OpeningStateService(source).record(OpeningStateInput(day.toString(),
                accountBalances = listOf(OpeningAccountBalance(cash.accountId, 10000.0))))
            val movements = listOf(source.backupDataDao().events(), source.backupDataDao().financialLogs(), source.backupDataDao().inventoryLogs())
            val facade = NativeCashCloseFacade(source, security)
            val count = facade.recordCount(cashier.sessionId, NativeCashCloseCountRequest(1_000_000, "Synthetic even drawer"), day).count
            facade.closeDay(cashier.sessionId, NativeCashCloseDayRequest(count.countId, null), day)
            // This acceptance scenario syncs each action before the next one.
            // Keep the actual factory output from each stage, before later
            // counts exist. Bulk queued history ordering has a separate finding.
            val stagedEnvelopes = mutableListOf<JSONObject>()
            var sentThrough = 0L
            suspend fun captureStage() {
                val queued = source.syncOutboxDao().all().filter { it.outboxId > sentThrough }
                stagedEnvelopes += SyncEnvelopeFactory(source).build(queued).envelopes
                sentThrough = queued.maxOfOrNull { it.outboxId } ?: sentThrough
            }
            captureStage()
            var finalCountId = count.countId
            if (includeReclose) {
                facade.reopenDay(owner.session.sessionId, "Synthetic verified recount", OWNER.toCharArray(), day)
                captureStage()
                assertNotNull(runCatching {
                    facade.closeDay(cashier.sessionId, NativeCashCloseDayRequest(count.countId, null), day)
                }.exceptionOrNull())
                kotlinx.coroutines.delay(10)
                finalCountId = facade.recordCount(cashier.sessionId,
                    NativeCashCloseCountRequest(1_000_000, "Recount after reopening"), day).count.countId
                facade.closeDay(cashier.sessionId, NativeCashCloseDayRequest(finalCountId, null), day)
                captureStage()
            }
            assertEquals(movements, listOf(source.backupDataDao().events(), source.backupDataDao().financialLogs(), source.backupDataDao().inventoryLogs()))
            val close = source.dailyCloseDao().currentAction(day.toString())!!
            assertEquals("cashier", close.actorRole)
            assertEquals(cashier.userId, close.actorUserId)
            assertTrue(source.dailyCloseDao().isPostingClosed(day.toString()))

            target.withWriteTransaction {
                for (table in listOf("physical_cash_counts", "daily_close_actions", "app_users")) {
                    executeSQL("DELETE FROM sqlite_sequence WHERE name='$table'")
                    executeSQL("INSERT INTO sqlite_sequence(name,seq) VALUES('$table',1000)")
                }
            }
            if (initialized) UserSecurityService(target).bootstrapOwner(
                "close.target.owner", "Different Receiver Owner", OWNER.toCharArray())
            val usersBefore = target.appUserDao().all()
            assertTrue(usersBefore.none { it.userId == cashier.userId })
            target.syncOutboxDao().deleteAll() // Receiver fixture only; not an enrollment workaround.
            val envelopes = if (coalescedHistory) SyncEnvelopeFactory(source).build(source.syncOutboxDao().all()).envelopes else stagedEnvelopes
            val records = envelopes.mapIndexed { index, row -> SyncPulledRecord(index + 1L, null,
                row.getString("table"), row.getString("uid"), row.getJSONObject("payload"), "source-phone") }
            val guardBefore = target.syncApplyDao().verifiedCloseActorTriggerSql()
            probe(target, records, day)
            val localOutboxBefore = target.syncOutboxDao().all()
            val report = SyncApplyEngine(target).apply(records)
            val audits = target.securityAuditDao().all().filter { it.action.contains("sync") }
            assertEquals("Close import refusals: ${audits.map { it.details }}", 0, report.skipped)
            assertEquals(0, report.conflicts)
            val receivedCount = target.syncEnvelopeDao().physicalCashCountsByUid(
                listOf(source.physicalCashCountDao().findById(finalCountId)!!.uid)).single()
            assertNotEquals(finalCountId, receivedCount.countId)
            val received = target.dailyCloseDao().currentAction(day.toString())
            assertNotNull("Verified remote close must control receiver posting: ${audits.map { it.reasonCode }}", received)
            assertNotEquals(close.actionId, received!!.actionId)
            assertEquals(receivedCount.countId, received.physicalCashCountId)
            assertEquals(close.actorName, received.actorName)
            assertEquals(close.actorRole, received.actorRole)
            assertEquals(close.createdAt, received.createdAt)
            assertEquals(null, received.actorUserId)
            assertTrue(target.dailyCloseDao().isPostingClosed(day.toString()))
            assertEquals(usersBefore, target.appUserDao().all())
            assertEquals(guardBefore, target.syncApplyDao().verifiedCloseActorTriggerSql())
            assertEquals(0, target.syncParkedRowDao().count())
            if (coalescedHistory) {
                assertEquals(source.syncOutboxDao().all().map { it.tableName to it.rowUid },
                    envelopes.map { it.getString("table") to it.getString("uid") })
            }
            if (includeReclose) {
                val sourceHistory = source.backupDataDao().dailyCloseActions()
                val receivedHistory = target.backupDataDao().dailyCloseActions()
                assertEquals(listOf("close", "reopen", "close"), receivedHistory.map { it.action })
                for (original in sourceHistory) {
                    val receivedAction = receivedHistory.single { it.uid == original.uid }
                    assertEquals(original.copy(actionId = receivedAction.actionId, actorUserId = null,
                        physicalCashCountId = original.physicalCashCountId?.let { originalId ->
                            val countUid = source.physicalCashCountDao().findById(originalId)!!.uid
                            target.syncEnvelopeDao().physicalCashCountsByUid(listOf(countUid)).single().countId
                        }, supersedesActionId = original.supersedesActionId?.let { originalId ->
                            val parentUid = sourceHistory.single { it.actionId == originalId }.uid
                            receivedHistory.single { it.uid == parentUid }.actionId
                        }), receivedAction)
                }
            }
            val before = listOf(target.backupDataDao().events(), target.backupDataDao().financialLogs(),
                target.backupDataDao().inventoryLogs(), target.backupDataDao().physicalCashCounts(), target.backupDataDao().dailyCloseActions())
            assertEquals(0, SyncApplyEngine(target).apply(records).skipped)
            assertEquals(before, listOf(target.backupDataDao().events(), target.backupDataDao().financialLogs(),
                target.backupDataDao().inventoryLogs(), target.backupDataDao().physicalCashCounts(), target.backupDataDao().dailyCloseActions()))
            assertEquals(10000.0, target.readModelDao().accountBalances().single { it.name == "Cash" }.balance, 0.0)
            assertEquals(localOutboxBefore, target.syncOutboxDao().all())
        } finally { source.close(); target.close() }
    }

    companion object {
        private const val OWNER = "synthetic close owner phrase 2026"
        private const val CASHIER = "synthetic close cashier phrase 2026"
    }
}
