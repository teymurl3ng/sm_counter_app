package com.saaf.milkshop

import android.net.Uri
import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.application.owner.NativeCorrectionKind
import com.saaf.milkshop.application.owner.NativeOwnerRecordsFacade
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DrawingRecordingRemedyTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val name = "drawing-remedy-migration.db"
    @get:Rule val helper = MigrationTestHelper(instrumentation = instrumentation,
        file = context.getDatabasePath(name), driver = BundledSQLiteDriver(), databaseClass = MilkShopDatabase::class,
        databaseFactory = { Room.databaseBuilder<MilkShopDatabase>(context, name).setDriver(BundledSQLiteDriver())
            .addMigrations(MilkShopDatabase.MIGRATION_13_14).build() }, autoMigrationSpecs = emptyList())

    @Test fun lowerHigherCancelAndRestoreKeepHistoryAndProfit() = runBlocking<Unit> {
        fixture { db, security, session, cash, original ->
            val facade = NativeOwnerRecordsFacade(db, security)
            val before = facade.loadDetail(session, original)
            assertTrue(before.canCorrect)
            assertEquals(NativeCorrectionKind.Drawing, before.correctionPrefill!!.kind)
            assertEquals("", before.correctionPrefill!!.notes)
            assertEquals("Cash", before.correctionPrefill!!.drawingPaymentAccountName)
            var current = original
            for ((amount, delta) in listOf(200.0 to 50.0, 300.0 to -100.0, 0.0 to 300.0, 125.5 to -125.5)) {
                val result = facade.correct(session, correction(current, amount))
                assertEquals(delta, db.financialLogDao().findByEventId(result.eventId).single().amount, 0.0)
                assertEquals(current, db.eventDao().findById(result.eventId)!!.correctsEventId)
                assertEquals(amount, db.eventDao().findById(result.eventId)!!.amount!!, 0.0)
                assertEquals("Remedy Owner", db.eventActorLinkDao().find(result.eventId)!!.actorNameSnapshot)
                if (amount == 0.0) assertEquals("Cancelled", facade.loadDetail(session, result.eventId).row.stateLabel)
                current = result.eventId
            }
            assertEquals(250.0, db.eventDao().findById(original)!!.amount!!, 0.0)
            assertEquals(-250.0, db.financialLogDao().findByEventId(original).single().amount, 0.0)
            assertEquals(874.5, db.readModelDao().accountBalance(cash)!!.balance, 0.0)
            val summary = db.analyticsReadDao().periodSummary("2026-10-01", "2026-11-01")
            assertEquals(0.0, summary.operatingExpenses, 0.0)
            assertTrue(db.backupDataDao().inventoryLogs().isEmpty())
        }
    }

    @Test fun badInputsDuplicatesAndInjectedActorFailureLeaveNoPartialWrite() = runBlocking<Unit> {
        fixture { db, security, session, _, original ->
            val facade = NativeOwnerRecordsFacade(db, security)
            val initial = snapshot(db)
            val invalid = listOf(correction(original, 250.0), correction(original, -1.0),
                correction(original, Double.NaN), correction(original, Double.POSITIVE_INFINITY),
                correction(original, 1.005), correction(original, 1e17), correction(original, 200.0).copy(notes = " "),
                correction(original, 200.0).copy(occurredAt = "2026-10-11"))
            for (input in invalid) {
                assertTrue(input.toString(), runCatching { facade.correct(session, input) }.isFailure)
                assertEquals(initial, snapshot(db))
            }
            val child = facade.correct(session, correction(original, 200.0)).eventId
            val after = snapshot(db)
            assertTrue(runCatching { facade.correct(session, correction(original, 0.0)) }.isFailure)
            assertEquals(after, snapshot(db))
            db.withWriteTransaction { executeSQL("""CREATE TRIGGER fail_remedy_actor BEFORE INSERT ON event_actor_links
                BEGIN SELECT RAISE(ABORT, 'injected actor failure'); END""") }
            assertTrue(runCatching { facade.correct(session, correction(child, 100.0)) }.isFailure)
            assertEquals(after, snapshot(db))
        }
    }

    @Test fun managerAndCashierCannotCorrectOwnerWithdrawal() = runBlocking<Unit> {
        fixture { db, security, session, _, original ->
            val facade = NativeOwnerRecordsFacade(db, security)
            for (role in listOf("manager", "cashier")) {
                val pending = security.beginUserEnrollment(session,
                    CreateUserInput("remedy.$role", "Remedy $role", role))
                security.completeUserEnrollment(pending.enrollmentToken, PASSWORD.toCharArray())
                val other = security.login("remedy.$role", PASSWORD.toCharArray()).sessionId
                val before = snapshot(db)
                assertTrue(runCatching { facade.correct(other, correction(original, 0.0)) }.isFailure)
                assertEquals(before, snapshot(db))
                if (role == "manager") assertFalse(facade.loadDetail(other, original).canCorrect)
            }
        }
    }

    @Test fun closedDateCannotBeBypassedByMovingCorrectionToToday() = runBlocking<Unit> {
        fixture(date = LocalDate.of(2026, 10, 11)) { db, security, session, cash, original ->
            val count = PhysicalCashCountService(db).record(PhysicalCashCountInput(cash, "2026-10-11", 750.0))
            DailyCloseService(db).closeAuthenticated(session, security,
                VerifiedDailyCloseInput("2026-10-11", count.count.countId))
            val before = snapshot(db)
            val facade = NativeOwnerRecordsFacade(db, security)
            for (date in listOf("2026-10-11", "2026-10-12")) {
                assertTrue(runCatching { facade.correct(session, correction(original, 0.0).copy(occurredAt = date)) }.isFailure)
                assertEquals(before, snapshot(db))
            }
        }
    }

    @Test fun sqlRejectsWrongDeltaDifferentAccountExtraRowsAndInventory() = runBlocking<Unit> {
        fixture { db, _, _, cash, original ->
            val child = db.eventDao().insert(EventEntity(createdAt = "2026-10-12T08:00:00Z", occurredAt = DATE.toString(),
                eventType = "OWNER_DRAWING", amount = 200.0, correctsEventId = original, notes = "Correct mistaken amount"))
            val bank = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Bank" }.accountId
            for ((account, amount) in listOf(cash to 51.0, cash to -50.0, bank to 50.0)) {
                assertTrue(runCatching { db.financialLogDao().insert(FinancialLogEntity(eventId = child, accountId = account, amount = amount)) }.isFailure)
                assertTrue(db.financialLogDao().findByEventId(child).isEmpty())
            }
            db.financialLogDao().insert(FinancialLogEntity(eventId = child, accountId = cash, amount = 50.0))
            assertTrue(runCatching { db.financialLogDao().insert(FinancialLogEntity(eventId = child, accountId = cash, amount = 50.0)) }.isFailure)
            val milk = db.productDao().findByName("Milk")!!.productId
            assertTrue(runCatching { db.inventoryLogDao().insert(InventoryLogEntity(eventId = child, productId = milk, quantity = 1.0)) }.isFailure)
            val unlinked = db.eventDao().insert(EventEntity(createdAt = "2026-10-12T08:01:00Z", occurredAt = DATE.toString(),
                eventType = "OWNER_DRAWING", amount = 50.0))
            assertTrue(runCatching { db.financialLogDao().insert(FinancialLogEntity(eventId = unlinked, accountId = cash, amount = 50.0)) }.isFailure)
            val ambiguous = db.eventDao().insert(EventEntity(createdAt = "2026-10-12T08:02:00Z", occurredAt = DATE.toString(),
                eventType = "OWNER_DRAWING", amount = 50.0))
            db.financialLogDao().insert(FinancialLogEntity(eventId = ambiguous, accountId = cash, amount = -50.0))
            db.financialLogDao().insert(FinancialLogEntity(eventId = ambiguous, accountId = bank, amount = -50.0))
            val ambiguousChild = db.eventDao().insert(EventEntity(createdAt = "2026-10-12T08:03:00Z", occurredAt = DATE.toString(),
                eventType = "OWNER_DRAWING", amount = 25.0, correctsEventId = ambiguous, notes = "Ambiguous history"))
            assertTrue(runCatching { db.financialLogDao().insert(FinancialLogEntity(eventId = ambiguousChild, accountId = cash, amount = 25.0)) }.isFailure)
        }
    }

    @Test fun backupAndUidReplayKeepCancellationChainIdempotently() = runBlocking<Unit> {
        fixture { source, security, session, cash, original ->
            val facade = NativeOwnerRecordsFacade(source, security)
            val corrected = facade.correct(session, correction(original, 200.0)).eventId
            val cancelled = facade.correct(session, correction(corrected, 0.0)).eventId
            val expected = source.backupDataDao().events().associate { it.eventUid to it.amount }
            val target = MilkShopDatabase.createInMemory()
            val replayed = MilkShopDatabase.createInMemory()
            val file = File(context.cacheDir, "drawing-remedy.milkbackup")
            try {
                val manager = BackupManager(context)
                manager.exportSnapshot(source, Uri.fromFile(file)).getOrThrow()
                manager.restore(target, Uri.fromFile(file), BackupManager.RestoreSelection(true, false, false)).getOrThrow()
                assertEquals(expected, target.backupDataDao().events().associate { it.eventUid to it.amount })
                assertEquals(1000.0, target.readModelDao().accountBalance(cash)!!.balance, 0.0)
                replayed.accountDao().findByCategory("Cash/Bank")
                replayed.syncOutboxDao().deleteAll()
                val records = SyncEnvelopeFactory(source).build(source.syncOutboxDao().nextBatch(100)).envelopes.mapIndexed { i, e ->
                    SyncPulledRecord((i + 1).toLong(), null, e.getString("table"), e.getString("uid"), e.getJSONObject("payload"), "remedy-source")
                }
                val report = SyncApplyEngine(replayed).apply(records)
                assertEquals(report.toString(), 0, report.parked)
                assertEquals(0, report.conflicts)
                assertEquals(0, replayed.securityAuditDao().all().count { it.action == "sync_apply_skipped" })
                val replayCash = replayed.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
                assertEquals(1000.0, replayed.readModelDao().accountBalance(replayCash)!!.balance, 0.0)
                assertEquals(expected, replayed.backupDataDao().events().associate { it.eventUid to it.amount })
                val cancelledUid = source.eventDao().findById(cancelled)!!.eventUid
                val remapped = replayed.syncEnvelopeDao().eventsByUid(listOf(cancelledUid)).single()
                val correctedUid = source.eventDao().findById(corrected)!!.eventUid
                assertEquals(replayed.syncEnvelopeDao().eventsByUid(listOf(correctedUid)).single().eventId, remapped.correctsEventId)
                val count = replayed.backupDataDao().events().size
                SyncApplyEngine(replayed).apply(records)
                assertEquals(count, replayed.backupDataDao().events().size)
            } finally { target.close(); replayed.close(); file.delete() }
        }
    }

    @Test fun migration13To14PreservesOldWithdrawalAndAllowsLinkedCancellation() = runBlocking<Unit> {
        context.deleteDatabase(name)
        helper.createDatabase(13).use { c ->
            c.execSQL("INSERT INTO accounts(account_id,name,type,category,uid) VALUES (1,'Cash','Asset','Cash/Bank','migration-cash')")
            c.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,amount,surcharge_applied,event_uid) VALUES (1,'2026-10-12T06:00:00Z','2026-10-12','OWNER_DRAWING',250,0,'migration-draw')")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (1,1,-250)")
        }
        helper.runMigrationsAndValidate(14, listOf(MilkShopDatabase.MIGRATION_13_14)).use { c ->
            c.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,amount,surcharge_applied,corrects_event_id,notes,event_uid) VALUES (2,'2026-10-12T07:00:00Z','2026-10-12','OWNER_DRAWING',0,0,1,'Withdrawal never happened','migration-cancel')")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (2,1,250)")
            c.prepare("SELECT amount FROM financial_log WHERE event_id = 1").use { stmt ->
                assertTrue(stmt.step()); assertEquals(-250.0, stmt.getDouble(0), 0.0)
            }
        }
    }

    private fun correction(id: Long, amount: Double) = CorrectionInput.OwnerDrawing(id, DATE.toString(), amount, "Correct mistaken recording")
    private suspend fun snapshot(db: MilkShopDatabase) = listOf(db.backupDataDao().events().size.toLong(),
        db.backupDataDao().financialLogs().size.toLong(), db.syncOutboxDao().count(), db.securityAuditDao().all().size.toLong())
    private suspend fun fixture(date: LocalDate = DATE, block: suspend (MilkShopDatabase, UserSecurityService, String, Long, Long) -> Unit) {
        val db = MilkShopDatabase.createInMemory()
        try {
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("remedy.owner", "Remedy Owner", PASSWORD.toCharArray()).session.sessionId
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val opening = db.eventDao().insert(EventEntity(createdAt = "2026-10-11T05:00:00Z", occurredAt = date.toString(), eventType = "OPENING_STATE"))
            db.financialLogDao().insert(FinancialLogEntity(eventId = opening, accountId = cash, amount = 1000.0))
            val original = OperationsBridgeCommandService(db, security).recordOwnerDrawing(session, 250.0, cash, date.toString(), "Original withdrawal").eventId
            block(db, security, session, cash, original)
        } finally { db.close() }
    }
    companion object {
        private val DATE = LocalDate.of(2026, 10, 12)
        private const val PASSWORD = "drawing fixture passphrase 2026"
    }
}
