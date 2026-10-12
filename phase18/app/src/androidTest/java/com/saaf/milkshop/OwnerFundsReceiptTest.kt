package com.saaf.milkshop

import android.net.Uri
import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.commands.HistoryBridgeCommandService
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.application.owner.NativeCorrectionPrefill
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

@RunWith(AndroidJUnit4::class)
class OwnerFundsReceiptTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val migrationName = "owner-funds-receipt-migration.db"
    @get:Rule val helper = MigrationTestHelper(instrumentation = instrumentation,
        file = context.getDatabasePath(migrationName), driver = BundledSQLiteDriver(), databaseClass = MilkShopDatabase::class,
        databaseFactory = { Room.databaseBuilder<MilkShopDatabase>(context, migrationName).setDriver(BundledSQLiteDriver())
            .addMigrations(MilkShopDatabase.MIGRATION_15_16).build() }, autoMigrationSpecs = emptyList())

    @Test fun receiptCorrectionsCancelAndRestoreOnlyMoveCashBank() = runBlocking<Unit> {
        fixture { f ->
            val first = f.receive(100.0, f.cash)
            val second = f.receive(150.0, f.bank)
            f.balances(850.0, 650.0)
            assertEquals(f.root, f.db.ownerFundsDao().find(first)!!.withdrawalRootEventId)
            val reduced = f.correctReceipt(first, 80.0)
            assertEquals(listOf(f.cash to -20.0), f.legs(reduced))
            f.balances(830.0, 650.0)
            val moved = f.correctReceipt(reduced, 80.0, f.bank)
            assertEquals(listOf(f.cash to -80.0, f.bank to 80.0).sortedBy { it.first }, f.legs(moved).sortedBy { it.first })
            f.balances(750.0, 730.0)
            val cancelled = f.correctReceipt(moved, 0.0)
            assertEquals(listOf(f.bank to -80.0), f.legs(cancelled))
            f.balances(750.0, 650.0)
            val restored = f.correctReceipt(cancelled, 50.0)
            assertEquals(listOf(f.bank to 50.0), f.legs(restored))
            f.balances(750.0, 700.0)
            assertEquals(250.0, f.db.eventDao().findById(f.root)!!.amount!!, 0.0)
            assertEquals(0.0, f.db.analyticsReadDao().periodSummary("2026-10-01", "2026-11-01").operatingExpenses, 0.0)
            assertTrue(f.db.backupDataDao().inventoryLogs().isEmpty())
            assertEquals(150.0, f.db.eventDao().findById(second)!!.amount!!, 0.0)
        }
    }

    @Test fun recordsFacadeShowsLinkedFactsBuildsInputsAndKeepsManagerReadOnly() = runBlocking<Unit> {
        fixture { f ->
            val facade = NativeOwnerRecordsFacade(f.db, f.security)
            val receipt = facade.recordOwnerFundsReceipt(f.session, OwnerFundsReceiptInput(
                f.root, 40.0, f.cash, DATE, "Money returned", "records-ui-receipt")).eventId
            val withdrawal = facade.loadDetail(f.session, f.root)
            assertEquals(250.0, withdrawal.ownerFunds!!.currentWithdrawal, 0.0)
            assertEquals(40.0, withdrawal.ownerFunds!!.receivedTotal, 0.0)
            assertEquals(listOf(receipt), withdrawal.ownerFunds!!.linkedReceipts.map { it.eventId })
            assertTrue(withdrawal.ownerFunds!!.canRecordReceipt)
            val movedInput = recordsInput(facade, withdrawal.correctionPrefill!!, "200", f.bank)
                as CorrectionInput.OwnerDrawing
            assertEquals(f.bank, movedInput.paymentAccountId)
            assertEquals(250.0, movedInput.reviewedWithdrawalAmount!!, 0.0)
            val moved = facade.correct(f.session, movedInput).eventId
            val refreshed = facade.refreshReceiptContext(f.session, f.root)
            assertEquals(200.0, refreshed.currentWithdrawal, 0.0)
            assertEquals(40.0, refreshed.receivedTotal, 0.0)
            assertTrue(refreshed.canRecordReceipt)
            val received = facade.loadDetail(f.session, receipt)
            assertTrue(received.ownerFunds!!.isReceipt)
            assertEquals(f.root, received.ownerFunds!!.rootEventId)
            assertEquals("Owner money received", received.row.taskLabel)
            val excessInput = recordsInput(facade, received.correctionPrefill!!, "300", f.bank, true)
                as CorrectionInput.OwnerFundsIn
            assertEquals(f.bank, excessInput.paymentAccountId)
            assertTrue(excessInput.acknowledgeExcess)
            facade.correct(f.session, excessInput)
            val current = facade.loadDetail(f.session, moved)
            assertEquals(300.0, current.ownerFunds!!.receivedTotal, 0.0)
            val pending = f.security.beginUserEnrollment(f.session, CreateUserInput("records.manager", "Records Manager", "manager"))
            f.security.completeUserEnrollment(pending.enrollmentToken, PASSWORD.toCharArray())
            val manager = f.security.login("records.manager", PASSWORD.toCharArray()).sessionId
            val readOnly = facade.loadDetail(manager, moved)
            assertFalse(readOnly.canCorrect)
            assertFalse(readOnly.ownerFunds!!.canRecordReceipt)
            assertTrue(runCatching { facade.refreshReceiptContext(manager, f.root) }.isFailure)
        }
    }

    private fun recordsInput(facade: NativeOwnerRecordsFacade, prefill: NativeCorrectionPrefill,
        amount: String, account: Long, acknowledge: Boolean = false): CorrectionInput = facade.buildCorrectionInput(
        prefill = prefill, notes = "Correct owner funds", purchaseRawLiters = "", purchaseFat = "",
        purchaseLr = "", productionMilk = "", productionYogurt = "", saleRate = "",
        saleSurcharge = "", saleQuantityMode = true, saleQuantity = "", saleValue = "",
        saleChannel = null, monetaryAmount = amount, spoilageQuantity = "", spoilageReason = "",
        ownerFundsAccountId = account, acknowledgeExcess = acknowledge)

    @Test fun excessRequiresCurrentReviewAndLaterWithdrawalCorrectionKeepsActualReceipts() = runBlocking<Unit> {
        fixture { f ->
            val before = f.db.eventDao().count()
            assertTrue(runCatching { f.receive(300.0, f.cash) }.isFailure)
            assertEquals(before, f.db.eventDao().count())
            val receipt = f.receive(300.0, f.cash, reviewedWithdrawal = 250.0, reviewedReceived = 0.0, ack = true)
            f.balances(1050.0, 500.0)
            val wrongContext = CorrectionInput.OwnerDrawing(f.root, DATE, 200.0, "Correct withdrawal", f.cash,
                reviewedWithdrawalAmount = 250.0, reviewedReceivedTotal = 200.0, acknowledgeExcess = true)
            assertTrue(runCatching { f.history.correct(f.session, wrongContext) }.isFailure)
            val corrected = f.history.correct(f.session, wrongContext.copy(reviewedReceivedTotal = 300.0)).eventId
            f.balances(1100.0, 500.0)
            val cancelled = f.history.correct(f.session, CorrectionInput.OwnerDrawing(corrected, DATE, 0.0,
                "Withdrawal never happened", f.cash, 200.0, 300.0, true)).eventId
            f.balances(1300.0, 500.0)
            assertEquals(300.0, f.db.eventDao().findById(receipt)!!.amount!!, 0.0)
            assertEquals(300.0, OwnerFundsTruth(f.db).currentTotals(f.root).second, 0.0)
            assertTrue(runCatching { f.receive(10.0, f.cash, reviewedWithdrawal = 0.0,
                reviewedReceived = 300.0, ack = true) }.isFailure)
            assertEquals(f.root, f.db.ownerFundsDao().find(cancelled)!!.withdrawalRootEventId)
        }
    }

    @Test fun stableUidDatesRolesAndMalformedAmountsDoNotDuplicate() = runBlocking<Unit> {
        fixture { f ->
            val uid = "receipt-stable-retry"
            val input = OwnerFundsReceiptInput(f.root, 25.0, f.cash, DATE, "Actually received", uid)
            val first = f.operations.recordOwnerFundsReceipt(f.session, input)
            val before = f.db.eventDao().count()
            val repeated = f.operations.recordOwnerFundsReceipt(f.session, input)
            assertTrue(repeated.alreadySaved)
            assertEquals(first.eventId, repeated.eventId)
            assertEquals(before, f.db.eventDao().count())
            assertTrue(runCatching { f.operations.recordOwnerFundsReceipt(f.session, input.copy(amount = 30.0)) }.isFailure)
            for (invalid in listOf(input.copy(amount = 1.005, submissionUid = "bad-paisa"),
                input.copy(amount = Double.NaN, submissionUid = "bad-nan"),
                input.copy(occurredAt = "2026-10-09", submissionUid = "bad-prior"),
                input.copy(occurredAt = "2099-01-01", submissionUid = "bad-future"),
                input.copy(paymentAccountId = -1, submissionUid = "bad-account"))) {
                assertTrue(invalid.toString(), runCatching { f.operations.recordOwnerFundsReceipt(f.session, invalid) }.isFailure)
            }
            for (role in listOf("manager", "cashier")) {
                val pending = f.security.beginUserEnrollment(f.session, CreateUserInput("receipt.$role", "Receipt $role", role))
                f.security.completeUserEnrollment(pending.enrollmentToken, PASSWORD.toCharArray())
                val session = f.security.login("receipt.$role", PASSWORD.toCharArray()).sessionId
                assertTrue(runCatching { f.operations.recordOwnerFundsReceipt(session, input.copy(submissionUid = "deny-$role")) }.isFailure)
                assertTrue(runCatching { f.history.correct(session, CorrectionInput.OwnerFundsIn(first.eventId, DATE, 20.0, "No permission")) }.isFailure)
            }
            assertEquals(before, f.db.eventDao().count())
        }
    }

    @Test fun closedWithdrawalCanReceiveLaterButClosedReceivingDateCannotChange() = runBlocking<Unit> {
        fixture(rootDate = "2026-10-10") { f ->
            val rootCount = PhysicalCashCountService(f.db).record(PhysicalCashCountInput(f.cash, "2026-10-10", 750.0))
            DailyCloseService(f.db).closeAuthenticated(f.session, f.security,
                VerifiedDailyCloseInput("2026-10-10", rootCount.count.countId))
            val receipt = f.receive(100.0, f.cash)
            f.balances(850.0, 500.0)
            assertEquals(DATE, f.db.eventDao().findById(receipt)!!.occurredAt)
            val dayCount = PhysicalCashCountService(f.db).record(PhysicalCashCountInput(f.cash, DATE, 850.0))
            DailyCloseService(f.db).closeAuthenticated(f.session, f.security,
                VerifiedDailyCloseInput(DATE, dayCount.count.countId))
            assertTrue(runCatching { f.receive(20.0, f.bank) }.isFailure)
            assertTrue(runCatching { f.correctReceipt(receipt, 80.0) }.isFailure)
            assertTrue(runCatching { f.history.correct(f.session,
                CorrectionInput.OwnerFundsIn(receipt, "2026-10-11", 80.0, "Wrong date edit")) }.isFailure)
            f.balances(850.0, 500.0)
        }
    }

    @Test fun staleExcessReviewAndUnsupportedReplayPreserveWholeReceipt() = runBlocking<Unit> {
        fixture { f ->
            f.receive(100.0, f.cash)
            val before = f.db.eventDao().count()
            assertTrue(runCatching { f.receive(300.0, f.bank, 250.0, 0.0, true) }.isFailure)
            assertEquals(before, f.db.eventDao().count())
            val receipt = f.receive(300.0, f.bank, 250.0, 100.0, true)
            val uid = f.db.eventDao().findById(receipt)!!.eventUid
            val sourceRecord = SyncEnvelopeFactory(f.db).build(f.db.syncOutboxDao().nextBatch(100)).envelopes
                .single { it.getString("uid") == uid }
            val target = MilkShopDatabase.createInMemory()
            try {
                target.syncOutboxDao().deleteAll()
                val record = SyncPulledRecord(99, null, "events", uid, sourceRecord.getJSONObject("payload"), "receipt-source")
                SyncApplyEngine(target).apply(listOf(record))
                assertNotNull(target.syncParkedRowDao().find("events", uid))
                assertTrue(target.syncEnvelopeDao().eventsByUid(listOf(uid)).isEmpty())
                val malformed = org.json.JSONObject(record.payload.toString()).put("owner_funds_contract_version", 99)
                SyncApplyEngine(target).apply(listOf(record.copy(serverSeq = 100, payload = malformed)))
                assertNotNull(target.syncParkedRowDao().find("events", uid))
                assertTrue(target.syncEnvelopeDao().eventsByUid(listOf(uid)).isEmpty())
            } finally { target.close() }
        }
    }

    @Test fun version15UpgradeKeepsWithdrawalAndAddsReceiptGuard() = runBlocking<Unit> {
        context.deleteDatabase(migrationName)
        helper.createDatabase(15).use { c ->
            c.execSQL("INSERT INTO accounts(account_id,name,type,category,uid) VALUES (101,'Migration Cash','Asset','Cash/Bank','receipt-migration-cash')")
            c.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,amount,surcharge_applied,event_uid) VALUES (201,'2026-10-12T05:00:00Z','$DATE','OWNER_DRAWING',250,0,'receipt-migration-root')")
            c.execSQL("INSERT INTO owner_funds_details(event_id,payment_account_id,withdrawal_root_event_id) VALUES (201,101,201)")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (201,101,-250)")
        }
        helper.runMigrationsAndValidate(16, listOf(MilkShopDatabase.MIGRATION_15_16)).use { c ->
            c.prepare("SELECT amount FROM events WHERE event_id=201").use { assertTrue(it.step()); assertEquals(250.0, it.getDouble(0), 0.0) }
            c.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,amount,surcharge_applied,event_uid) VALUES (202,'2026-10-12T06:00:00Z','$DATE','OWNER_FUNDS_IN',100,0,'receipt-migration-in')")
            c.execSQL("INSERT INTO owner_funds_details(event_id,payment_account_id,withdrawal_root_event_id) VALUES (202,101,201)")
            assertTrue(runCatching { c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (202,101,-100)") }.isFailure)
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (202,101,100)")
            assertTrue(runCatching { c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (202,101,1)") }.isFailure)
        }
    }

    @Test fun ordinaryAndEncryptedBackupPreserveReceiptFacts() = runBlocking<Unit> {
        fixture { f ->
            val receipt = f.receive(300.0, f.bank, 250.0, 0.0, true)
            f.history.correct(f.session, CorrectionInput.OwnerFundsIn(receipt, DATE, 280.0,
                "Correct actual amount", reviewedWithdrawalAmount = 250.0,
                reviewedReceivedTotal = 300.0, acknowledgeExcess = true))
            val file = File(context.cacheDir, "owner-receipt-roundtrip.milkbackup")
            val encrypted = File(context.cacheDir, "owner-receipt-encrypted.milkbackup")
            val manager = BackupManager(context)
            val target = MilkShopDatabase.createInMemory()
            val secured = MilkShopDatabase.createInMemory()
            val password = "receipt backup phrase 2026".toCharArray()
            try {
                manager.exportSnapshot(f.db, Uri.fromFile(file)).getOrThrow()
                manager.restore(target, Uri.fromFile(file), BackupManager.RestoreSelection(true, false, false)).getOrThrow()
                assertEquals(f.db.ownerFundsDao().all(), target.ownerFundsDao().all())
                assertEquals(f.db.backupDataDao().financialLogs(), target.backupDataDao().financialLogs())
                manager.exportEncryptedSnapshot(f.db, Uri.fromFile(encrypted), password).getOrThrow()
                manager.restoreEncrypted(secured, Uri.fromFile(encrypted),
                    BackupManager.RestoreSelection(true, false, false, security = true),
                    password, NativeSessionManager()).getOrThrow()
                assertEquals(f.db.ownerFundsDao().all(), secured.ownerFundsDao().all())
                assertEquals(f.db.backupDataDao().events(), secured.backupDataDao().events())
            } finally { password.fill('\u0000'); target.close(); secured.close(); file.delete(); encrypted.delete() }
        }
    }

    @Test fun uidReplayRemapsReceiptRootAndAccountWithoutDuplication() = runBlocking<Unit> {
        fixture { f ->
            val receipt = f.receive(100.0, f.cash)
            val corrected = f.correctReceipt(receipt, 80.0, f.bank)
            val target = MilkShopDatabase.createInMemory()
            try {
                target.accountDao().insert(AccountEntity(name = "Unrelated account", type = "Asset", category = "Cash/Bank"))
                target.syncOutboxDao().deleteAll()
                val records = SyncEnvelopeFactory(f.db).build(f.db.syncOutboxDao().nextBatch(100)).envelopes
                    .mapIndexed { i, envelope -> SyncPulledRecord((i + 1).toLong(), null,
                        envelope.getString("table"), envelope.getString("uid"), envelope.getJSONObject("payload"), "receipt-source") }
                val report = SyncApplyEngine(target).apply(records)
                val reasons = target.securityAuditDao().all().filter { it.action == "sync_apply_parked" }.map { it.reasonCode }
                assertEquals("$report; reasons=$reasons", 0, report.parked)
                val uid = f.db.eventDao().findById(corrected)!!.eventUid
                val mapped = target.syncEnvelopeDao().eventsByUid(listOf(uid)).single()
                val detail = target.ownerFundsDao().find(mapped.eventId)!!
                val rootUid = f.db.eventDao().findById(f.root)!!.eventUid
                assertEquals(rootUid, target.eventDao().findById(detail.withdrawalRootEventId)!!.eventUid)
                val bankUid = f.db.accountDao().findById(f.bank)!!.uid
                assertEquals(bankUid, target.accountDao().findById(detail.paymentAccountId)!!.uid)
                assertEquals(80.0, mapped.amount!!, 0.0)
                val before = target.eventDao().count() to target.backupDataDao().financialLogs().size
                SyncApplyEngine(target).apply(records)
                assertEquals(before, target.eventDao().count() to target.backupDataDao().financialLogs().size)
                assertEquals(0L, target.syncOutboxDao().count())
            } finally { target.close() }
        }
    }

    private data class Fixture(val db: MilkShopDatabase, val security: UserSecurityService, val session: String,
        val cash: Long, val bank: Long, val root: Long) {
        val operations get() = OperationsBridgeCommandService(db, security)
        val history get() = HistoryBridgeCommandService(db, security)
        suspend fun receive(amount: Double, account: Long, reviewedWithdrawal: Double? = null,
            reviewedReceived: Double? = null, ack: Boolean = false): Long =
            operations.recordOwnerFundsReceipt(session, OwnerFundsReceiptInput(root, amount, account, DATE,
                reviewedWithdrawalAmount = reviewedWithdrawal, reviewedReceivedTotal = reviewedReceived,
                acknowledgeExcess = ack)).eventId
        suspend fun correctReceipt(id: Long, amount: Double, account: Long? = null): Long =
            history.correct(session, CorrectionInput.OwnerFundsIn(id, DATE, amount, "Fix mistaken receipt", account)).eventId
        suspend fun legs(id: Long) = db.financialLogDao().findByEventId(id).map { it.accountId to it.amount }
        suspend fun balances(cashAmount: Double, bankAmount: Double) {
            assertEquals(cashAmount, db.readModelDao().accountBalance(cash)!!.balance, 0.0)
            assertEquals(bankAmount, db.readModelDao().accountBalance(bank)!!.balance, 0.0)
        }
    }

    private suspend fun fixture(rootDate: String = DATE, block: suspend (Fixture) -> Unit) {
        val db = MilkShopDatabase.createInMemory()
        try {
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("receipt.owner", "Receipt Owner", PASSWORD.toCharArray()).session.sessionId
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val bank = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Bank" }.accountId
            val opening = db.eventDao().insert(EventEntity(createdAt = "2026-10-10T05:00:00Z", occurredAt = rootDate, eventType = "OPENING_STATE"))
            db.financialLogDao().insert(FinancialLogEntity(eventId = opening, accountId = cash, amount = 1000.0))
            db.financialLogDao().insert(FinancialLogEntity(eventId = opening, accountId = bank, amount = 500.0))
            val root = OperationsBridgeCommandService(db, security).recordOwnerDrawing(session, 250.0, cash, rootDate, "Original withdrawal").eventId
            block(Fixture(db, security, session, cash, bank, root))
        } finally { db.close() }
    }
    companion object { private const val DATE = "2026-10-12"; private const val PASSWORD = "receipt fixture password 2026" }
}
