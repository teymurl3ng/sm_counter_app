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
import com.saaf.milkshop.application.commands.HistoryBridgeCommandService
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.application.owner.NativeOwnerRecordsFacade
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OwnerFundsAccountCorrectionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val migrationName = "owner-funds-account-migration.db"
    @get:Rule val helper = MigrationTestHelper(instrumentation = instrumentation,
        file = context.getDatabasePath(migrationName), driver = BundledSQLiteDriver(), databaseClass = MilkShopDatabase::class,
        databaseFactory = { Room.databaseBuilder<MilkShopDatabase>(context, migrationName).setDriver(BundledSQLiteDriver())
            .addMigrations(MilkShopDatabase.MIGRATION_14_15).build() }, autoMigrationSpecs = emptyList())

    @Test fun accountOnlyAndCombinedCorrectionUseLatestTruthAndKeepHistory() = runBlocking<Unit> {
        fixture { f ->
            val moved = f.correct(f.original, 250.0, f.bank)
            val detail = NativeOwnerRecordsFacade(f.db, f.security).loadDetail(f.session, moved)
            assertTrue(detail.canCorrect)
            assertEquals("Bank", detail.correctionPrefill!!.drawingPaymentAccountName)
            assertEquals(listOf(f.cash to 250.0, f.bank to -250.0).sortedBy { it.first }, f.legs(moved).sortedBy { it.first })
            f.balances(1000.0, 250.0)
            val lowered = f.correct(moved, 200.0)
            assertEquals(listOf(f.bank to 50.0), f.legs(lowered))
            f.balances(1000.0, 300.0)
            val smaller = f.correct(lowered, 150.0)
            val cancelled = f.correct(smaller, 0.0)
            f.balances(1000.0, 500.0)
            val restored = f.correct(cancelled, 125.5)
            assertEquals(listOf(f.bank to -125.5), f.legs(restored))
            f.balances(1000.0, 374.5)
            assertEquals(listOf(f.cash to -250.0), f.legs(f.original))
            assertEquals(250.0, f.db.eventDao().findById(f.original)!!.amount!!, 0.0)
            assertEquals(f.original, f.db.ownerFundsDao().find(restored)!!.withdrawalRootEventId)
            assertEquals(f.bank, f.db.ownerFundsDao().find(restored)!!.paymentAccountId)
            assertEquals("Account Owner", f.db.eventActorLinkDao().find(restored)!!.actorNameSnapshot)
            assertEquals(0.0, f.db.analyticsReadDao().periodSummary("2026-10-01", "2026-11-01").operatingExpenses, 0.0)
            assertTrue(f.db.backupDataDao().inventoryLogs().isEmpty())
        }
        fixture { f ->
            val moved = f.correct(f.original, 200.0, f.bank)
            assertEquals(listOf(f.cash to 250.0, f.bank to -200.0).sortedBy { it.first }, f.legs(moved).sortedBy { it.first })
            f.balances(1000.0, 300.0)
            val cancelled = f.correct(moved, 0.0)
            val restored = f.correct(cancelled, 125.5, f.cash)
            assertEquals(listOf(f.cash to -125.5), f.legs(restored))
            f.balances(874.5, 500.0)
        }
    }

    @Test fun invalidAccountTruthAndSecondLegFailureRollbackEverything() = runBlocking<Unit> {
        fixture { f ->
            val invalidAccount = f.db.accountDao().insert(AccountEntity(name = "Test customer", type = "Asset", category = "Customer"))
            val before = snapshot(f.db)
            val inputs = listOf(
                f.input(f.original, 250.0, f.cash), f.input(f.original, 200.0, invalidAccount),
                f.input(f.original, 200.0, -1), f.input(f.original, 0.0, f.bank),
                f.input(f.original, Double.NaN, f.bank), f.input(f.original, Double.POSITIVE_INFINITY, f.bank),
                f.input(f.original, 1.005, f.bank), f.input(f.original, -1.0, f.bank), f.input(f.original, 1e17, f.bank),
                f.input(f.original, 200.0, f.bank).copy(notes = " "),
                f.input(f.original, 200.0, f.bank).copy(occurredAt = "2026-10-11"),
            )
            for (input in inputs) {
                assertTrue(input.toString(), runCatching { f.command.correct(f.session, input) }.isFailure)
                assertEquals(before, snapshot(f.db))
            }
            f.db.withWriteTransaction { executeSQL("""CREATE TRIGGER fail_second_owner_leg BEFORE INSERT ON financial_log
                WHEN NEW.account_id = ${f.bank} AND NEW.amount < 0
                BEGIN SELECT RAISE(ABORT, 'injected second leg failure'); END""") }
            assertTrue(runCatching { f.correct(f.original, 200.0, f.bank) }.isFailure)
            assertEquals(before, snapshot(f.db))
            f.db.withWriteTransaction {
                executeSQL("DROP TRIGGER fail_second_owner_leg")
                executeSQL("""CREATE TRIGGER fail_owner_actor BEFORE INSERT ON event_actor_links
                    BEGIN SELECT RAISE(ABORT, 'injected actor failure'); END""")
            }
            assertTrue(runCatching { f.correct(f.original, 200.0, f.bank) }.isFailure)
            assertEquals(before, snapshot(f.db))
        }
    }

    @Test fun rolesDuplicatesAndClosedDateKeepExistingPolicy() = runBlocking<Unit> {
        fixture { f ->
            for (role in listOf("manager", "cashier", "admin")) {
                val pending = f.security.beginUserEnrollment(f.session, CreateUserInput("account.$role", "Account $role", role))
                f.security.completeUserEnrollment(pending.enrollmentToken, PASSWORD.toCharArray())
                val other = f.security.login("account.$role", PASSWORD.toCharArray()).sessionId
                val before = snapshot(f.db)
                val result = runCatching { f.command.correct(other, f.input(f.original, 200.0, f.bank)) }
                if (role == "admin") assertTrue(result.toString(), result.isSuccess)
                else { assertTrue(result.isFailure); assertEquals(before, snapshot(f.db)) }
            }
            val after = snapshot(f.db)
            assertTrue(runCatching { f.correct(f.original, 150.0, f.bank) }.isFailure)
            assertEquals(after, snapshot(f.db))
        }
        fixture { f ->
            val drawer = f.db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val count = PhysicalCashCountService(f.db).record(PhysicalCashCountInput(drawer, DATE, 0.0))
            DailyCloseService(f.db).closeAuthenticated(f.session, f.security, VerifiedDailyCloseInput(DATE, count.count.countId))
            val before = snapshot(f.db)
            for (date in listOf(DATE, "2026-10-11")) {
                assertTrue(runCatching { f.command.correct(f.session, f.input(f.original, 200.0, f.bank).copy(occurredAt = date)) }.isFailure)
                assertEquals(before, snapshot(f.db))
            }
        }
    }

    @Test fun sqlGuardsAndCompleteValidatorRejectWrongExtraOrMissingLegs() = runBlocking<Unit> {
        fixture { f ->
            val child = f.db.eventDao().insert(EventEntity(createdAt = "2026-10-12T10:00:00Z", occurredAt = DATE,
                eventType = "OWNER_DRAWING", amount = 200.0, correctsEventId = f.original, notes = "Fix account"))
            f.db.ownerFundsDao().insert(OwnerFundsDetailEntity(child, f.bank, f.original))
            for ((account, amount) in listOf(f.cash to 50.0, f.cash to -250.0, f.bank to 200.0, f.bank to -201.0)) {
                assertTrue(runCatching { f.db.financialLogDao().insert(FinancialLogEntity(eventId = child, accountId = account, amount = amount)) }.isFailure)
            }
            f.db.financialLogDao().insert(FinancialLogEntity(eventId = child, accountId = f.cash, amount = 250.0))
            assertTrue(runCatching { OwnerFundsTruth(f.db).resolve(f.db.eventDao().findById(child)!!) }.isFailure)
            assertTrue(runCatching { f.db.financialLogDao().insert(FinancialLogEntity(eventId = child, accountId = f.cash, amount = 250.0)) }.isFailure)
            val milk = f.db.productDao().findByName("Milk")!!.productId
            assertTrue(runCatching { f.db.inventoryLogDao().insert(InventoryLogEntity(eventId = child, productId = milk, quantity = 1.0)) }.isFailure)
            f.db.financialLogDao().insert(FinancialLogEntity(eventId = child, accountId = f.bank, amount = -200.0))
            assertEquals(f.bank, OwnerFundsTruth(f.db).resolve(f.db.eventDao().findById(child)!!).paymentAccountId)
            assertTrue(runCatching { f.db.withWriteTransaction { executeSQL("UPDATE owner_funds_details SET payment_account_id = ${f.cash} WHERE event_id = $child") } }.isFailure)
        }
    }

    @Test fun ambiguousLegacyAccountStaysReadableAndCannotBeCorrectedByGuessing() = runBlocking<Unit> {
        fixture { f ->
            f.db.withWriteTransaction {
                executeSQL("DELETE FROM owner_funds_details WHERE event_id = ${f.original}")
                executeSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (${f.original},${f.bank},-250)")
            }
            val detail = NativeOwnerRecordsFacade(f.db, f.security).loadDetail(f.session, f.original)
            assertEquals("Account needs review", detail.row.stateLabel)
            assertFalse(detail.canCorrect)
            assertTrue(detail.financialMovements!!.size >= 2)
            val before = snapshot(f.db)
            assertTrue(runCatching { f.correct(f.original, 200.0, f.bank) }.isFailure)
            assertEquals(before, snapshot(f.db))
        }
    }

    @Test fun migrationPreservesLegacyRowsAndDoesNotGuessAmbiguousAccount() = runBlocking<Unit> {
        context.deleteDatabase(migrationName)
        helper.createDatabase(14).use { c ->
            c.execSQL("INSERT INTO accounts(account_id,name,type,category,uid) VALUES (1,'Cash','Asset','Cash/Bank','migration-cash'),(2,'Bank','Asset','Cash/Bank','migration-bank')")
            c.execSQL("""INSERT INTO events(event_id,created_at,occurred_at,event_type,amount,surcharge_applied,corrects_event_id,notes,event_uid) VALUES
                (9,'2026-10-12T06:00:00Z','$DATE','OWNER_DRAWING',250,0,NULL,NULL,'root'),
                (3,'2026-10-12T07:00:00Z','$DATE','OWNER_DRAWING',200,0,9,'Correct amount','child'),
                (4,'2026-10-12T08:00:00Z','$DATE','OWNER_DRAWING',50,0,NULL,NULL,'ambiguous')""")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (9,1,-250),(3,1,50),(4,1,-50),(4,2,-50)")
        }
        helper.runMigrationsAndValidate(15, listOf(MilkShopDatabase.MIGRATION_14_15)).use { c ->
            c.prepare("SELECT event_id,payment_account_id,withdrawal_root_event_id FROM owner_funds_details ORDER BY event_id").use {
                assertTrue(it.step()); assertEquals(3L, it.getLong(0)); assertEquals(1L, it.getLong(1)); assertEquals(9L, it.getLong(2))
                assertTrue(it.step()); assertEquals(9L, it.getLong(0)); assertFalse(it.step())
            }
            c.prepare("SELECT COUNT(*),SUM(amount) FROM financial_log").use {
                assertTrue(it.step()); assertEquals(4L, it.getLong(0)); assertEquals(-300.0, it.getDouble(1), 0.0)
            }
            c.execSQL("""INSERT INTO events(event_id,created_at,occurred_at,event_type,amount,surcharge_applied,corrects_event_id,notes,event_uid)
                VALUES (10,'2026-10-12T09:00:00Z','$DATE','OWNER_DRAWING',200,0,3,'Correct account','account-child')""")
            c.execSQL("INSERT INTO owner_funds_details VALUES (10,2,9)")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (10,1,200),(10,2,-200)")
        }
    }

    @Test fun newBackupRoundTripAndIncompleteRestoreAreAtomic() = runBlocking<Unit> {
        fixture { f ->
            f.correct(f.original, 200.0, f.bank)
            val target = MilkShopDatabase.createInMemory()
            val file = File(context.cacheDir, "owner-account-roundtrip.milkbackup")
            val bad = File(context.cacheDir, "owner-account-incomplete.milkbackup")
            try {
                val manager = BackupManager(context)
                manager.exportSnapshot(f.db, Uri.fromFile(file)).getOrThrow()
                manager.restore(target, Uri.fromFile(file), BackupManager.RestoreSelection(true, false, false)).getOrThrow()
                assertEquals(f.db.ownerFundsDao().all(), target.ownerFundsDao().all())
                assertEquals(f.db.backupDataDao().financialLogs(), target.backupDataDao().financialLogs())
                val json = JSONObject(file.readText())
                val rows = json.getJSONObject("data").getJSONArray("financial_logs")
                rows.remove(rows.length() - 1)
                writeVerified(bad, json)
                val before = snapshot(target)
                assertTrue(manager.restore(target, Uri.fromFile(bad), BackupManager.RestoreSelection(true, false, false)).isFailure)
                assertEquals(before, snapshot(target))
            } finally { target.close(); file.delete(); bad.delete() }
        }
    }

    @Test fun supportedLegacyBackupInfersAccountWithoutLosingFacts() = runBlocking<Unit> {
        fixture { f ->
            f.correct(f.original, 200.0)
            val target = MilkShopDatabase.createInMemory()
            val file = File(context.cacheDir, "owner-account-legacy.milkbackup")
            try {
                val manager = BackupManager(context)
                manager.exportSnapshot(f.db, Uri.fromFile(file)).getOrThrow()
                val json = JSONObject(file.readText())
                json.getJSONObject("metadata").put("schema_version", 10)
                json.getJSONObject("data").remove("owner_funds_details")
                writeVerified(file, json)
                manager.restore(target, Uri.fromFile(file), BackupManager.RestoreSelection(true, false, false)).getOrThrow()
                assertEquals(f.db.backupDataDao().financialLogs(), target.backupDataDao().financialLogs())
                assertEquals(f.db.ownerFundsDao().all(), target.ownerFundsDao().all())
            } finally { target.close(); file.delete() }
        }
    }

    @Test fun encryptedBackupPreservesAccountsHistoryAndAttribution() = runBlocking<Unit> {
        fixture { f ->
            f.correct(f.original, 200.0, f.bank)
            val target = MilkShopDatabase.createInMemory()
            val file = File(context.cacheDir, "owner-account-encrypted.milkbackup")
            val password = "owner funds backup phrase 2026".toCharArray()
            try {
                val manager = BackupManager(context)
                manager.exportEncryptedSnapshot(f.db, Uri.fromFile(file), password).getOrThrow()
                manager.restoreEncrypted(target, Uri.fromFile(file), BackupManager.RestoreSelection(true, false, false, security = true),
                    password, NativeSessionManager()).getOrThrow()
                assertEquals(f.db.ownerFundsDao().all(), target.ownerFundsDao().all())
                assertEquals(f.db.backupDataDao().financialLogs(), target.backupDataDao().financialLogs())
                assertEquals(f.db.backupDataDao().eventActorLinks(), target.backupDataDao().eventActorLinks())
                assertEquals(f.db.backupDataDao().events(), target.backupDataDao().events())
            } finally { password.fill('\u0000'); target.close(); file.delete() }
        }
    }

    @Test fun legacyWireAndContradictoryMetadataHaveBoundedReplayBehavior() = runBlocking<Unit> {
        fixture { f ->
            val changed = f.correct(f.original, 200.0)
            val target = MilkShopDatabase.createInMemory()
            try {
                target.syncOutboxDao().deleteAll()
                val legacy = envelopes(f.db).map { record ->
                    val payload = JSONObject(record.payload.toString())
                    payload.remove("owner_funds_details"); payload.remove("owner_funds_contract_version")
                    record.copy(payload = payload)
                }
                SyncApplyEngine(target).apply(legacy)
                assertTrue(target.syncParkedRowDao().all().isEmpty())
                val uid = f.db.eventDao().findById(changed)!!.eventUid
                val mapped = target.syncEnvelopeDao().eventsByUid(listOf(uid)).single()
                val originalDetail = target.ownerFundsDao().find(mapped.eventId)!!
                val before = snapshot(target)
                val modern = envelopes(f.db).single { it.rowUid == uid }
                for ((key, value) in listOf("payment_account_uid" to "missing-account", "withdrawal_root_event_uid" to "missing-root",
                    "payment_account_uid" to f.db.accountDao().findById(f.bank)!!.uid)) {
                    val bad = JSONObject(modern.payload.toString())
                    bad.getJSONObject("owner_funds_details").put(key, value)
                    SyncApplyEngine(target).apply(listOf(modern.copy(payload = bad)))
                    assertNotNull(target.syncParkedRowDao().find("events", uid))
                    assertEquals(before, snapshot(target))
                    assertEquals(originalDetail, target.ownerFundsDao().find(mapped.eventId))
                    SyncApplyEngine(target).apply(listOf(modern))
                    assertNull(target.syncParkedRowDao().find("events", uid))
                }
            } finally { target.close() }
        }
    }

    @Test fun uidReplayDifferentIdsRepairsOnlyMissingMetadataAndIsIdempotent() = runBlocking<Unit> {
        fixture { f ->
            val changed = f.correct(f.original, 200.0, f.bank)
            val latest = f.correct(changed, 150.0)
            val target = MilkShopDatabase.createInMemory()
            try {
                target.accountDao().insert(AccountEntity(name = "Unrelated account", type = "Asset", category = "Cash/Bank"))
                target.syncOutboxDao().deleteAll()
                val records = envelopes(f.db)
                val report = SyncApplyEngine(target).apply(records)
                assertEquals(report.toString(), 0, report.parked)
                assertEquals(0, target.syncParkedRowDao().all().size)
                val uid = f.db.eventDao().findById(latest)!!.eventUid
                val mapped = target.syncEnvelopeDao().eventsByUid(listOf(uid)).single()
                val mappedCash = target.syncEnvelopeDao().accountsByUid(listOf(f.db.accountDao().findById(f.cash)!!.uid)).single().accountId
                assertNotEquals(f.cash, mappedCash)
                assertEquals(1000.0, target.readModelDao().accountBalance(mappedCash)!!.balance, 0.0)
                assertEquals(350.0, target.readModelDao().accountBalance(f.bank)!!.balance, 0.0)
                val expected = snapshot(target)
                SyncApplyEngine(target).apply(records)
                assertEquals(expected, snapshot(target))
                target.withWriteTransaction { executeSQL("DELETE FROM owner_funds_details WHERE event_id = ${mapped.eventId}") }
                SyncApplyEngine(target).apply(records)
                assertNotNull(target.ownerFundsDao().find(mapped.eventId))
                assertEquals(expected, snapshot(target))
                assertEquals(0L, target.syncOutboxDao().count())
            } finally { target.close() }
        }
    }

    @Test fun missingDependenciesInvalidBodiesAndPartialReplayPreserveWholeRecord() = runBlocking<Unit> {
        fixture { f ->
            val changed = f.correct(f.original, 200.0, f.bank)
            val target = MilkShopDatabase.createInMemory()
            try {
                target.syncOutboxDao().deleteAll()
                val records = envelopes(f.db)
                val changedUid = f.db.eventDao().findById(changed)!!.eventUid
                val changedRecord = records.single { it.rowUid == changedUid }
                SyncApplyEngine(target).apply(listOf(changedRecord))
                assertNotNull(target.syncParkedRowDao().find("events", changedUid))
                assertTrue(target.syncEnvelopeDao().eventsByUid(listOf(changedUid)).isEmpty())
                SyncApplyEngine(target).apply(records.filter { it.rowUid != changedUid })
                assertNull(target.syncParkedRowDao().find("events", changedUid))
                val mapped = target.syncEnvelopeDao().eventsByUid(listOf(changedUid)).single()
                val before = snapshot(target)
                val badPayload = JSONObject(changedRecord.payload.toString())
                badPayload.getJSONArray("financial_logs").remove(1)
                SyncApplyEngine(target).apply(listOf(changedRecord.copy(payload = badPayload)))
                assertNotNull(target.syncParkedRowDao().find("events", changedUid))
                assertEquals(before, snapshot(target))
                SyncApplyEngine(target).apply(listOf(changedRecord))
                assertNull(target.syncParkedRowDao().find("events", changedUid))
                target.withWriteTransaction { executeSQL("DELETE FROM financial_log WHERE event_id = ${mapped.eventId} AND amount < 0") }
                SyncApplyEngine(target).apply(listOf(changedRecord))
                assertEquals(2, target.financialLogDao().findByEventId(mapped.eventId).size)
                // Reinserted children get new local log IDs; their money facts must still match.
                assertEquals(listOf(250.0, -200.0), target.financialLogDao().findByEventId(mapped.eventId).map { it.amount })
                val repaired = snapshot(target)
                val unsupported = JSONObject(changedRecord.payload.toString()).put("owner_funds_contract_version", 99)
                SyncApplyEngine(target).apply(listOf(changedRecord.copy(payload = unsupported)))
                assertNotNull(target.syncParkedRowDao().find("events", changedUid))
                assertEquals(repaired, snapshot(target))
            } finally { target.close() }
        }
    }

    private data class Fixture(val db: MilkShopDatabase, val security: UserSecurityService, val session: String,
        val cash: Long, val bank: Long, val original: Long) {
        val command get() = HistoryBridgeCommandService(db, security)
        fun input(id: Long, amount: Double, account: Long? = null) = CorrectionInput.OwnerDrawing(id, DATE, amount, "Fix recorded account or amount", account)
        suspend fun correct(id: Long, amount: Double, account: Long? = null) = command.correct(session, input(id, amount, account)).eventId
        suspend fun legs(id: Long) = db.financialLogDao().findByEventId(id).map { it.accountId to it.amount }
        suspend fun balances(cashAmount: Double, bankAmount: Double) {
            assertEquals(cashAmount, db.readModelDao().accountBalance(cash)!!.balance, 0.0)
            assertEquals(bankAmount, db.readModelDao().accountBalance(bank)!!.balance, 0.0)
        }
    }
    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val db = MilkShopDatabase.createInMemory()
        try {
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("account.owner", "Account Owner", PASSWORD.toCharArray()).session.sessionId
            val cash = db.accountDao().insert(AccountEntity(name = "Test Cash", type = "Asset", category = "Cash/Bank"))
            val bank = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Bank" }.accountId
            val opening = db.eventDao().insert(EventEntity(createdAt = "2026-10-11T05:00:00Z", occurredAt = DATE, eventType = "OPENING_STATE"))
            db.financialLogDao().insert(FinancialLogEntity(eventId = opening, accountId = cash, amount = 1000.0))
            db.financialLogDao().insert(FinancialLogEntity(eventId = opening, accountId = bank, amount = 500.0))
            val original = OperationsBridgeCommandService(db, security).recordOwnerDrawing(session, 250.0, cash, DATE, "Original withdrawal").eventId
            block(Fixture(db, security, session, cash, bank, original))
        } finally { db.close() }
    }
    private suspend fun snapshot(db: MilkShopDatabase): List<Any> = listOf(
        db.backupDataDao().events(), db.backupDataDao().financialLogs(), db.ownerFundsDao().all(),
        db.syncOutboxDao().all(), db.backupDataDao().eventActorLinks(),
        db.securityAuditDao().all().filterNot { it.action.startsWith("sync_") },
    )
    private suspend fun envelopes(db: MilkShopDatabase) = SyncEnvelopeFactory(db).build(db.syncOutboxDao().nextBatch(100)).envelopes.mapIndexed { i, e ->
        SyncPulledRecord((i + 1).toLong(), null, e.getString("table"), e.getString("uid"), e.getJSONObject("payload"), "owner-account-fixture")
    }
    private fun writeVerified(file: File, json: JSONObject) {
        json.remove("integrity_sha256")
        json.put("integrity_sha256", sha256(json.toString().toByteArray(Charsets.UTF_8)))
        file.writeText(json.toString())
    }
    companion object {
        private const val DATE = "2026-10-12"
        private const val PASSWORD = "account correction fixture 2026"
    }
}
