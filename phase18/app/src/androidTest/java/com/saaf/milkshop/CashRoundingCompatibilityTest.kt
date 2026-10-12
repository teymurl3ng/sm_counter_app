package com.saaf.milkshop

import android.net.Uri
import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.sync.SyncApplyEngine
import com.saaf.milkshop.sync.SyncEnvelopeFactory
import com.saaf.milkshop.sync.SyncPulledRecord
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class CashRoundingCompatibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val name = "cash-rounding-migration.db"
    @get:Rule val helper = MigrationTestHelper(
        instrumentation = instrumentation, file = context.getDatabasePath(name),
        driver = BundledSQLiteDriver(), databaseClass = MilkShopDatabase::class,
        databaseFactory = { migratedDatabase() }, autoMigrationSpecs = emptyList())

    private fun migratedDatabase() = Room.databaseBuilder<MilkShopDatabase>(context, name)
        .setDriver(BundledSQLiteDriver()).addMigrations(MilkShopDatabase.MIGRATION_12_13, MilkShopDatabase.MIGRATION_13_14, MilkShopDatabase.MIGRATION_14_15).build()

    @Test fun migrationKeepsLedgerCountsAndHistoricalCloseResult() = runBlocking<Unit> {
        context.deleteDatabase(name)
        helper.createDatabase(12).use { c ->
            c.execSQL("INSERT INTO accounts(account_id,name,type,category,uid) VALUES (1,'Cash','Asset','Cash/Bank','cash-history')")
            c.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,event_uid,surcharge_applied) VALUES (1,'2026-10-10T06:00:00Z','2026-10-10','OPENING_STATE','event-history',0)")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES (1,1,1.005)")
            c.execSQL("INSERT INTO physical_cash_counts(count_id,account_id,business_date,counted_amount,counted_at,uid) VALUES (1,1,'2026-10-10',1.0,'2026-10-10T07:00:00Z','count-history')")
            c.execSQL(DatabaseSql.legacyDailyCloseTrigger)
            c.execSQL("INSERT INTO daily_close_actions(action_id,business_date,action,physical_cash_count_id,actor_name,actor_role,created_at,uid) VALUES (1,'2026-10-10','close',1,'History cashier','cashier','2026-10-10T08:00:00Z','close-history')")
        }
        helper.runMigrationsAndValidate(13, listOf(MilkShopDatabase.MIGRATION_12_13)).close()
        val db = migratedDatabase()
        try {
            assertEquals(1.005, db.financialLogDao().findByEventId(1).single().amount, 0.0)
            assertEquals(1.01, db.readModelDao().accountBalance(1)!!.balance, 0.0)
            assertEquals(1.0, db.physicalCashCountDao().findById(1)!!.countedAmount, 0.0)
            val record = DailyCloseService(db).current("2026-10-10")!!
            assertEquals(1, record.action.moneyRoundingVersion)
            assertEquals("close-history", record.action.uid)
            assertNull(record.action.reason)
            assertEquals(1.0, record.expectedCash!!, 0.0)
            assertEquals(0.0, record.variance!!, 0.0)
            val savedCount = PhysicalCashCountService(db).latest(1, "2026-10-10")!!
            assertEquals(1.0, savedCount.expectedBalance, 0.0)
            assertEquals(0.0, savedCount.variance, 0.0)
        } finally { db.close() }
    }

    @Test fun codecKeepsExplicitVersionAndDefaultsMissingVersionToLegacy() {
        val action = DailyCloseActionEntity(actionId = 1, businessDate = "2026-10-10",
            action = "close", physicalCashCountId = 1, actorName = "Cashier", actorRole = "cashier",
            createdAt = "2026-10-10T08:00:00Z", uid = "codec-close")
        val json = dailyCloseActionJson(action, false)
        assertEquals(2, readDailyCloseActions(JSONArray().put(json)).single().moneyRoundingVersion)
        json.remove("money_rounding_version")
        assertEquals(1, readDailyCloseActions(JSONArray().put(json)).single().moneyRoundingVersion)
    }

    @Test fun backupsRestoreLegacyMissingPolicyAndNewPolicyWithoutRewritingFacts() = runBlocking<Unit> {
        for (version in listOf(1, 2)) {
            val source = MilkShopDatabase.createInMemory()
            val target = MilkShopDatabase.createInMemory()
            val file = File(context.cacheDir, "cash-policy-$version.milkbackup")
            try {
                val cash = seedHalfPaisaClose(source, version)
                val manager = BackupManager(context)
                manager.exportSnapshot(source, Uri.fromFile(file)).getOrThrow()
                if (version == 1) {
                    // A genuine old-format fixture: no rule field, valid integrity hash.
                    val root = JSONObject(file.readText())
                    root.getJSONObject("data").getJSONArray("daily_close_actions")
                        .getJSONObject(0).remove("money_rounding_version")
                    root.remove("integrity_sha256")
                    val hash = MessageDigest.getInstance("SHA-256").digest(root.toString().toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }
                    root.put("integrity_sha256", hash)
                    file.writeText(root.toString())
                }
                manager.restore(target, Uri.fromFile(file), BackupManager.RestoreSelection(
                    business = true, planning = false, preferences = false)).getOrThrow()
                assertRestoredFacts(target, cash, version)
            } finally { source.close(); target.close(); file.delete() }
        }
    }

    @Test fun syncReplaysBothClosePoliciesAndLegacyMissingFieldIdempotently() = runBlocking<Unit> {
        for (version in listOf(1, 2)) {
            val source = MilkShopDatabase.createInMemory()
            val target = MilkShopDatabase.createInMemory()
            try {
                seedHalfPaisaClose(source, version)
                // Only fixture seed entries: model an already synchronized destination.
                target.accountDao().findByCategory("Cash/Bank")
                target.syncOutboxDao().deleteAll()
                val envelopes = SyncEnvelopeFactory(source).build(source.syncOutboxDao().nextBatch(100)).envelopes
                val records = envelopes.mapIndexed { i, envelope ->
                    val payload = envelope.getJSONObject("payload")
                    if (version == 1 && envelope.getString("table") == "daily_close_actions") {
                        assertEquals(1, payload.getInt("money_rounding_version"))
                        payload.remove("money_rounding_version")
                    }
                    SyncPulledRecord((i + 1).toLong(), null, envelope.getString("table"),
                        envelope.getString("uid"), payload, "cash-fixture-source")
                }
                val report = SyncApplyEngine(target).apply(records)
                assertTrue(report.toString(), report.applied >= 3)
                assertEquals(0, target.securityAuditDao().all().count { it.action == "sync_apply_skipped" })
                assertEquals(0, report.conflicts)
                assertEquals(0, report.parked)
                val cash = target.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
                assertRestoredFacts(target, cash, version)
                SyncApplyEngine(target).apply(records)
                assertEquals(1, target.dailyCloseDao().all().size)
                assertEquals(1, target.backupDataDao().financialLogs().size)
            } finally { source.close(); target.close() }
        }
    }

    private suspend fun seedHalfPaisaClose(db: MilkShopDatabase, version: Int): Long {
        val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val event = db.eventDao().insert(EventEntity(createdAt = "2026-10-10T06:00:00Z",
            occurredAt = "2026-10-10", eventType = "OPENING_STATE"))
        db.financialLogDao().insert(FinancialLogEntity(eventId = event, accountId = cash, amount = 1.005))
        val count = db.physicalCashCountDao().insert(PhysicalCashCountEntity(accountId = cash,
            businessDate = "2026-10-10", countedAmount = if (version == 1) 1.0 else 1.01,
            countedAt = "2026-10-10T07:00:00Z"))
        db.dailyCloseDao().insert(DailyCloseActionEntity(businessDate = "2026-10-10", action = "close",
            physicalCashCountId = count, actorName = "Fixture cashier", actorRole = "cashier",
            createdAt = "2026-10-10T08:00:00Z", moneyRoundingVersion = version))
        return cash
    }

    private suspend fun assertRestoredFacts(db: MilkShopDatabase, cash: Long, version: Int) {
        assertEquals(1.005, db.backupDataDao().financialLogs().single().amount, 0.0)
        assertEquals(1.01, db.readModelDao().accountBalance(cash)!!.balance, 0.0)
        val record = DailyCloseService(db).current("2026-10-10")!!
        assertEquals(version, record.action.moneyRoundingVersion)
        assertNull(record.action.reason)
        assertEquals(if (version == 1) 1.0 else 1.01, record.expectedCash!!, 0.0)
        assertEquals(0.0, record.variance!!, 0.0)
        val savedCount = PhysicalCashCountService(db).latest(cash, "2026-10-10")!!
        assertEquals(if (version == 1) 1.0 else 1.01, savedCount.expectedBalance, 0.0)
        assertEquals(0.0, savedCount.variance, 0.0)
    }
}
