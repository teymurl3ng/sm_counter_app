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

/** Ledger regression tests against the production BundledSQLiteDriver. No network. */
@RunWith(AndroidJUnit4::class)
class SyncIntegrityRegressionTest {
    @Test fun existingPartialLedgerIsFilledOnceWithoutReplacingHistory() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
            db.syncOutboxDao().deleteAll()
            val engine = SyncApplyEngine(db)
            val header = JSONObject().put("created_at", "2026-10-09T08:00:00Z")
                .put("occurred_at", "2026-10-09").put("event_type", "EXPENSE").put("amount", 10.0)
            val record = SyncPulledRecord(1, "shop", "events", "legacy-partial", header, "remote")
            engine.apply(listOf(record))
            val id = db.syncEnvelopeDao().eventsByUid(listOf(record.rowUid)).single().eventId
            val full = JSONObject(header.toString()).put("financial_logs", JSONArray()
                .put(JSONObject().put("account_uid", cash.uid).put("amount", -10.0)))
            engine.apply(listOf(record.copy(serverSeq = 2, payload = full)))
            engine.apply(listOf(record.copy(serverSeq = 3, payload = full)))
            assertEquals(1, db.financialLogDao().findByEventId(id).size)
            assertEquals(-10.0, db.financialLogDao().findByEventId(id).single().amount, 0.0)
            val changed = JSONObject(full.toString()).put("amount", 50.0)
            assertEquals(1, engine.apply(listOf(record.copy(serverSeq = 4, payload = changed))).skipped)
            assertEquals(10.0, db.syncEnvelopeDao().eventsByUid(listOf(record.rowUid)).single().amount!!, 0.0)
        } finally { db.close() }
    }

    @Test fun deliveryWaitsForItsSaleInsteadOfDroppingTheLink() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val product = db.productDao().findByName("Milk")!!
            val customerId = db.accountDao().insert(com.saaf.milkshop.data.AccountEntity(
                name = "Customer: Linked delivery", type = "Asset", category = "Customer"))
            val customer = db.accountDao().findById(customerId)!!
            db.syncOutboxDao().deleteAll()
            val engine = SyncApplyEngine(db)
            val payload = JSONObject().put("customer_uid", customer.uid).put("product_uid", product.uid)
                .put("sale_event_uid", "late-sale").put("quantity", 1.0).put("business_date", "2026-10-09")
                .put("created_at", "2026-10-09T08:00:00Z")
            engine.apply(listOf(SyncPulledRecord(1, "shop", "deliveries", "linked-delivery", payload, "remote")))
            assertTrue(db.syncEnvelopeDao().deliveriesByUid(listOf("linked-delivery")).isEmpty())
            val sale = JSONObject().put("created_at", "2026-10-09T08:00:00Z")
                .put("occurred_at", "2026-10-09").put("event_type", "SALE")
                .put("unit_rate", 100.0).put("sale_quantity", 1.0).put("sale_value", 100.0)
            engine.apply(listOf(SyncPulledRecord(2, "shop", "events", "late-sale", sale, "remote")))
            val delivery = db.syncEnvelopeDao().deliveriesByUid(listOf("linked-delivery")).single()
            assertEquals(db.syncEnvelopeDao().eventsByUid(listOf("late-sale")).single().eventId, delivery.saleEventId)
        } finally { db.close() }
    }

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
