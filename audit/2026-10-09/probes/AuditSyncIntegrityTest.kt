package com.saaf.milkshop.data

import android.app.Application
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Audit regression probes. Assertions describe the required integrity behavior. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AuditSyncIntegrityTest {
    @Test
    fun remappedLocalIdsMustNotBecomeBusinessConflicts() {
        val original = JSONObject().put("product_id", 7).put("uid", "same-product")
            .put("name", "Milk").put("unit", "Liters")
        val remapped = JSONObject(original.toString()).put("product_id", 41)
        assertTrue("The same UID and business fields became a conflict solely because the local primary key changed",
            SyncRebaseline.livePayloadsEqual(original, remapped))
    }

    @Test
    fun failedLedgerChildMustRollBackIncomingEvent() = runBlocking<Unit> {
        val db = MilkShopDatabase.createInMemory(driver = null)
        try {
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
            db.syncOutboxDao().deleteAll()
            val payload = JSONObject()
                .put("created_at", "2026-10-09T08:00:00Z")
                .put("occurred_at", "2026-10-09")
                .put("event_type", "EXPENSE")
                .put("amount", 10.0)
                .put("financial_logs", JSONArray().put(JSONObject()
                    .put("account_uid", cash.uid).put("amount", 0.0)))
            val report = SyncApplyEngine(db).apply(listOf(
                SyncPulledRecord(1, "audit-shop", "events", "audit-bad-child", payload, "audit-device"),
            ))
            assertEquals(1, report.skipped)
            val audit = db.securityAuditDao().all()
            println("AUDIT_CHILD_FAILURE: " + audit.joinToString { it.details.orEmpty() })
            assertTrue("Probe did not reach the intended child constraint: $audit",
                audit.any { it.details?.contains("Financial amount cannot be zero") == true })
            assertTrue("Rejected event header survived its failed financial child",
                db.syncEnvelopeDao().eventsByUid(listOf("audit-bad-child")).isEmpty())
        } finally { db.close() }
    }

    @Test
    fun transientFailureAfterProgressMustRequestRetry() = runBlocking<Unit> {
        val db = MilkShopDatabase.createInMemory(driver = null)
        try {
            db.productDao().all() // initialize database and seeds
            db.syncOutboxDao().deleteAll()
            db.productDao().insert(ProductEntity(name = "Audit A", unit = "Liters"))
            db.productDao().insert(ProductEntity(name = "Audit B", unit = "Liters"))
            var calls = 0
            val engine = SyncPushEngine(db, SyncPushTransport { _, rows ->
                if (++calls == 1) SyncPushResult.Success(rows.size) else SyncPushResult.Transient("network")
            }, batchSize = 1)
            val outcome = engine.drainOnce()
            assertEquals(1L, db.syncOutboxDao().count())
            assertTrue("Remaining queue needs WorkManager retry; got $outcome",
                outcome is SyncPushEngine.DrainOutcome.RetryLater)
        } finally { db.close() }
    }
}
