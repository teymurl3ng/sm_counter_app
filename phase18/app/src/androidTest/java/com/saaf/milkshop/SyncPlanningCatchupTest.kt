package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.CollectionsTargetEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncPullStateEntity
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncPlanningCatchupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val createdAt = "2029-12-01T00:00:00Z"

    @Before fun setup() {
        SyncScheduler.suppressSchedulingForTest()
        SyncRebaselineStore(context).clear()
        SyncServerConfigStore.save(context, SyncServerConfig("dev", "https://example.invalid", "public-test-key"))
        SyncShopBindingStore.save(context, SyncShopBinding("dev", "business", "shop", "Shop", "staff", "cashier"))
        SyncTokenStore(context).save(SyncTokenStore.Session("test", null, null))
    }

    @After fun cleanup() {
        SyncRebaselineStore(context).clear()
        SyncServerConfigStore.clear(context)
        SyncShopBindingStore.clear(context)
        SyncTokenStore(context).clear()
    }

    @Test fun pagedReplayRecoversSkippedPlanningWithoutMovingCursor() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.syncOutboxDao().deleteAll()
            db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 77, updatedAt = createdAt))
            val child = row(2, "budget_plans", "budget-child", budgetPayload(120.0, false, "budget-parent"))
            val parent = row(9, "budget_plans", "budget-parent", budgetPayload(100.0, true))
            val target = row(10, "collections_targets", "target", targetPayload(27000.0))
            val calls = mutableListOf<Long>()
            val result = SyncPlanningCatchup(db).replay { since ->
                calls += since
                when (since) {
                    0L -> SyncPullOutcome.Fetched(SyncPullBatch(listOf(
                        row(1, "products", "ignored", JSONObject()), child), 2), true)
                    2L -> SyncPullOutcome.Fetched(SyncPullBatch(listOf(parent, target), 10), false)
                    else -> error("Unexpected cursor $since")
                }
            }
            assertEquals(SyncPlanningCatchup.Outcome.Done(2, 3, 1), result)
            assertEquals(listOf(0L, 2L), calls)
            assertEquals(77L, db.syncPullStateDao().find()!!.lastServerSeq)
            assertEquals(0, db.syncParkedRowDao().count())
            assertEquals("budget-child", db.budgetPlanDao().findActive("Milk purchases", "2030-01-07")?.uid)
            assertEquals("target", db.collectionsTargetDao().findActive("2030-01-07")?.uid)
            assertEquals(0L, db.syncOutboxDao().count())
            val repeated = SyncPlanningCatchup(db).replay { since ->
                when (since) {
                    0L -> SyncPullOutcome.Fetched(SyncPullBatch(listOf(child), 2), true)
                    2L -> SyncPullOutcome.Fetched(SyncPullBatch(listOf(parent, target), 10), false)
                    else -> error("Unexpected cursor $since")
                }
            }
            assertEquals(SyncPlanningCatchup.Outcome.Done(2, 3, 0), repeated)
            assertEquals(77L, db.syncPullStateDao().find()!!.lastServerSeq)
        } finally { db.close() }
    }

    @Test fun pendingLocalPlanStaysQueuedAndRemoteVersionStaysParked() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.collectionsTargetDao().insert(CollectionsTargetEntity(
                weekStart = "2030-01-07", weekEndExclusive = "2030-01-14", targetAmount = 23000.0,
                createdAt = createdAt, uid = "local-target",
            ))
            db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 55, updatedAt = createdAt))
            val remote = row(5, "collections_targets", "local-target", targetPayload(27000.0))
            val result = SyncPlanningCatchup(db).replay {
                SyncPullOutcome.Fetched(SyncPullBatch(listOf(remote), 5), false)
            }
            assertEquals(SyncPlanningCatchup.Outcome.Done(1, 1, 1), result)
            assertEquals(23000.0, db.collectionsTargetDao().findActive("2030-01-07")!!.targetAmount, 0.0)
            assertNotNull(db.syncParkedRowDao().find("collections_targets", "local-target"))
            assertEquals(1, db.syncOutboxDao().countByTableAndUid("collections_targets", "local-target"))
            assertEquals(55L, db.syncPullStateDao().find()!!.lastServerSeq)
        } finally { db.close() }
    }

    @Test fun runtimeRemembersSuccessfulReplayAndRetriesNetworkFailure() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.syncOutboxDao().deleteAll()
            db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 40, updatedAt = createdAt))
            val key = listOf("dev", "https://example.invalid", "shop", SyncIdentity.warmUp(context)).joinToString("|")
            SyncRebaselineStore(context).markProfileCatchupCompleted(key)
            val remote = row(5, "collections_targets", "remote-target", targetPayload(27000.0))
            var fail = true
            var zeroCalls = 0
            val runtime = SyncRuntime(context, db, SyncHttp { request ->
                if (!request.url.contains("sync_pull")) return@SyncHttp SyncHttpResponse(503, null)
                when (JSONObject(String(request.body!!)).getLong("since_seq")) {
                    0L -> {
                        zeroCalls++
                        if (fail) SyncHttpResponse(503, null) else response(remote)
                    }
                    40L -> response()
                    else -> error("Unexpected cursor")
                }
            })
            assertEquals(SyncRuntime.SyncPullSummary.RetryLater("planning_catchup_server_503"), runtime.pullOnce())
            assertNull(db.collectionsTargetDao().findActive("2030-01-07"))
            fail = false
            assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
            assertEquals("remote-target", db.collectionsTargetDao().findActive("2030-01-07")?.uid)
            assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
            assertEquals(2, zeroCalls)
            assertEquals(40L, db.syncPullStateDao().find()!!.lastServerSeq)
        } finally { db.close() }
    }

    private fun budgetPayload(liters: Double, archived: Boolean, parent: String? = null) = JSONObject()
        .put("category", "Milk purchases").put("period_start", "2030-01-07")
        .put("period_end_exclusive", "2030-01-14").put("planned_liters", liters)
        .put("planned_amount", liters * 180.0).put("vendor_rate_snapshot", 180.0)
        .put("retail_rate_snapshot", 200.0).put("created_at", createdAt)
        .put("archived", archived).put("supersedes_id", if (parent == null) JSONObject.NULL else 9999)
        .apply { parent?.let { put("supersedes_uid", it) } }

    private fun targetPayload(amount: Double) = JSONObject()
        .put("week_start", "2030-01-07").put("week_end_exclusive", "2030-01-14")
        .put("target_amount", amount).put("created_at", createdAt)
        .put("archived", false).put("supersedes_id", JSONObject.NULL)

    private fun row(seq: Long, table: String, uid: String, payload: JSONObject) =
        SyncPulledRecord(seq, "shop", table, uid, payload, "sender")

    private fun response(vararg rows: SyncPulledRecord): SyncHttpResponse {
        val array = JSONArray()
        rows.forEach { row -> array.put(JSONObject().put("server_seq", row.serverSeq)
            .put("shop_id", row.shopId).put("table", row.tableName).put("uid", row.rowUid)
            .put("payload", row.payload).put("origin_device", row.originDevice)) }
        return SyncHttpResponse(200, JSONObject().put("ok", true).put("records", array).toString().toByteArray())
    }
}
