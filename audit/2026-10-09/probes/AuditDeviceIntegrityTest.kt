package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.SyncApplyEngine
import com.saaf.milkshop.sync.SyncPulledRecord
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Audit-only probes against the production BundledSQLiteDriver. No network. */
@RunWith(AndroidJUnit4::class)
class AuditDeviceIntegrityTest {
    @Test fun failedLedgerChildMustRollBackIncomingEvent() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
            db.syncOutboxDao().deleteAll()
            val payload = JSONObject().put("created_at", "2026-10-09T08:00:00Z")
                .put("occurred_at", "2026-10-09").put("event_type", "EXPENSE").put("amount", 10.0)
                .put("financial_logs", JSONArray().put(JSONObject().put("account_uid", cash.uid).put("amount", 0.0)))
            val report = SyncApplyEngine(db).apply(listOf(SyncPulledRecord(1, "audit-shop", "events", "audit-bad-child", payload, "audit-device")))
            assertEquals(1, report.skipped)
            assertTrue("Probe must reach the zero-financial-child constraint",
                db.securityAuditDao().all().any { it.details?.contains("Financial amount cannot be zero") == true })
            assertTrue("Rejected event header survived its failed financial child",
                db.syncEnvelopeDao().eventsByUid(listOf("audit-bad-child")).isEmpty())
        } finally { db.close() }
    }

    @Test fun lateDependencyMustRecoverAfterFiveUnrelatedBatches() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
            db.syncOutboxDao().deleteAll()
            val engine = SyncApplyEngine(db)
            val event = JSONObject().put("created_at", "2026-10-09T08:00:00Z")
                .put("occurred_at", "2026-10-09").put("event_type", "EXPENSE").put("amount", 10.0)
                .put("financial_logs", JSONArray()
                    .put(JSONObject().put("account_uid", "audit-late-account").put("amount", 10.0))
                    .put(JSONObject().put("account_uid", cash.uid).put("amount", -10.0)))
            engine.apply(listOf(SyncPulledRecord(1, "audit-shop", "events", "audit-late-event", event, "audit-device")))
            for (i in 2..6) {
                engine.apply(listOf(SyncPulledRecord(i.toLong(), "audit-shop", "products", "audit-interim-$i",
                    JSONObject().put("name", "Audit interim $i").put("unit", "Liters"), "audit-device")))
            }
            assertEquals(5, db.syncParkedRowDao().all().single { it.rowUid == "audit-late-event" }.retryCount)
            engine.apply(listOf(SyncPulledRecord(7, "audit-shop", "accounts", "audit-late-account",
                JSONObject().put("name", "Audit expense").put("type", "Expense").put("category", "Operating Expense"), "audit-device")))
            assertTrue("Dependency arrived but the valid event remains permanently parked",
                db.syncEnvelopeDao().eventsByUid(listOf("audit-late-event")).isNotEmpty())
        } finally { db.close() }
    }
}
