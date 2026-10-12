package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductProfileEntity
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
class SyncProfileCatchupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val createdAt = "2026-10-01T08:00:00Z"

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

    @Test fun pagedReplayRestoresOnlyProfilesWithoutMovingOrdinaryCursor() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val milk = db.productDao().findByName("Milk")!!
            db.syncOutboxDao().deleteAll()
            db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 77, updatedAt = createdAt))
            val product = row(2, "products", milk.uid, productPayload("Milk", 210.0))
            val customer = row(3, "accounts", "u1_catchup_customer", customerPayload())
            val calls = mutableListOf<Long>()
            val result = SyncProfileCatchup(db).replay { since ->
                calls += since
                when (since) {
                    0L -> SyncPullOutcome.Fetched(SyncPullBatch(listOf(
                        row(1, "budget_plans", "ignored-plan", JSONObject()), product), 2), true)
                    2L -> SyncPullOutcome.Fetched(SyncPullBatch(listOf(customer), 3), false)
                    else -> error("Unexpected cursor $since")
                }
            }
            assertEquals(SyncProfileCatchup.Outcome.Done(2, 2, 0), result)
            assertEquals(listOf(0L, 2L), calls)
            assertEquals(77L, db.syncPullStateDao().find()!!.lastServerSeq)
            assertEquals(210.0, db.productProfileDao().findByProductId(milk.productId)!!.defaultSaleRate)
            val received = db.syncEnvelopeDao().accountsByUid(listOf("u1_catchup_customer")).single()
            assertEquals("0300", db.customerProfileDao().findByAccountId(received.accountId)?.phone)
            assertEquals(0, db.syncOutboxDao().countByTableAndUid("products", milk.uid))
            assertEquals(0, db.syncOutboxDao().countByTableAndUid("accounts", received.uid))
        } finally { db.close() }
    }

    @Test fun queuedLocalProfileEditIsParkedAndFetchFailureCanRetryFromZero() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val milk = db.productDao().findByName("Milk")!!
            db.productProfileDao().save(ProductProfileEntity(milk.productId, 190.0, true, false, null, createdAt))
            assertEquals(1, db.syncOutboxDao().countByTableAndUid("products", milk.uid))
            db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 55, updatedAt = createdAt))
            val remote = row(9, "products", milk.uid, productPayload("Milk", 250.0))
            val first = SyncProfileCatchup(db).replay { since ->
                if (since == 0L) SyncPullOutcome.Fetched(SyncPullBatch(listOf(remote), 9), true)
                else SyncPullOutcome.RetryLater("network")
            }
            assertEquals(SyncProfileCatchup.Outcome.RetryLater("network"), first)
            assertEquals(190.0, db.productProfileDao().findByProductId(milk.productId)?.defaultSaleRate)
            val second = SyncProfileCatchup(db).replay { since ->
                if (since == 0L) SyncPullOutcome.Fetched(SyncPullBatch(listOf(remote), 9), false)
                else error("Replay must restart at zero")
            }
            assertEquals(SyncProfileCatchup.Outcome.Done(1, 1, 1), second)
            assertEquals(55L, db.syncPullStateDao().find()!!.lastServerSeq)
            assertEquals(190.0, db.productProfileDao().findByProductId(milk.productId)?.defaultSaleRate)
            assertNotNull(db.syncParkedRowDao().find("products", milk.uid))
            assertEquals(1, db.syncOutboxDao().countByTableAndUid("products", milk.uid))
        } finally { db.close() }
    }

    @Test fun ordinaryPullRetriesCatchupOnceThenRemembersSuccess() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.syncOutboxDao().deleteAll()
            db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 40, updatedAt = createdAt))
            var catchupFails = true
            var catchupCalls = 0
            val remote = row(5, "products", "u1_catchup_remote_product", productPayload("Cultured cream", 300.0))
            val runtime = SyncRuntime(context, db, SyncHttp { request ->
                if (request.url.contains("sync_pull")) {
                    val since = JSONObject(String(request.body!!)).getLong("since_seq")
                    if (since == 0L) {
                        catchupCalls++
                        if (catchupFails) SyncHttpResponse(503, null)
                        else response(remote)
                    } else {
                        assertEquals(40L, since)
                        response()
                    }
                } else SyncHttpResponse(503, null)
            })
            assertEquals(SyncRuntime.SyncPullSummary.RetryLater("profile_catchup_server_503"), runtime.pullOnce())
            assertEquals(40L, db.syncPullStateDao().find()!!.lastServerSeq)
            assertNull(db.syncEnvelopeDao().productsByUid(listOf(remote.rowUid)).firstOrNull())
            catchupFails = false
            assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
            val received = db.syncEnvelopeDao().productsByUid(listOf(remote.rowUid)).single()
            assertEquals(300.0, db.productProfileDao().findByProductId(received.productId)?.defaultSaleRate)
            assertEquals(40L, db.syncPullStateDao().find()!!.lastServerSeq)
            assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
            assertEquals(3, catchupCalls) // Profile and planning each verify once after the ordinary pull.
        } finally { db.close() }
    }

    @Test fun freshCursorZeroStillVerifiesProfilesOnceWithoutCreatingDuplicates() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.syncOutboxDao().deleteAll()
            var zeroPulls = 0
            val remote = row(5, "products", "u1_fresh_profile_product", productPayload("Kefir", 185.0))
            val runtime = SyncRuntime(context, db, SyncHttp { request ->
                if (request.url.contains("sync_pull")) {
                    when (JSONObject(String(request.body!!)).getLong("since_seq")) {
                        0L -> { zeroPulls++; response(remote) }
                        5L -> response()
                        else -> error("Unexpected cursor")
                    }
                } else SyncHttpResponse(503, null)
            })
            assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
            assertEquals(3, zeroPulls) // Normal pull then one-time profile and planning verification.
            assertEquals(5L, db.syncPullStateDao().find()!!.lastServerSeq)
            assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
            assertEquals(3, zeroPulls)
            assertEquals(1, db.syncEnvelopeDao().productsByUid(listOf(remote.rowUid)).size)
        } finally { db.close() }
    }

    @Test fun malformedProfileRefusalDoesNotFinishCatchup() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.syncOutboxDao().deleteAll()
            val bad = row(1, "products", "u1_bad_catchup_profile", productPayload("Bad", -5.0))
            val result = SyncProfileCatchup(db).replay {
                SyncPullOutcome.Fetched(SyncPullBatch(listOf(bad), 1), false)
            }
            assertEquals(SyncProfileCatchup.Outcome.RetryLater("profile_record_refused"), result)
            assertTrue(db.syncEnvelopeDao().productsByUid(listOf(bad.rowUid)).isEmpty())
        } finally { db.close() }
    }

    private fun productPayload(name: String, rate: Double) = JSONObject()
        .put("name", name).put("unit", "Kg")
        .put("profile", JSONObject().put("product_id", 9999).put("default_sale_rate", rate)
            .put("inventory_tracking", true).put("archived", false)
            .put("notes", JSONObject.NULL).put("created_at", createdAt))

    private fun customerPayload() = JSONObject().put("name", "Customer: Catchup")
        .put("type", "Asset").put("category", "Customer")
        .put("profile", JSONObject().put("account_id", 9999).put("phone", "0300")
            .put("area_route", JSONObject.NULL).put("address", JSONObject.NULL)
            .put("notes", JSONObject.NULL).put("credit_limit", JSONObject.NULL)
            .put("daily_quota", JSONObject.NULL).put("active", true).put("created_at", createdAt))

    private fun row(seq: Long, table: String, uid: String, payload: JSONObject) = SyncPulledRecord(
        seq, "shop", table, uid, payload, "sender")

    private fun response(vararg rows: SyncPulledRecord): SyncHttpResponse {
        val array = JSONArray()
        rows.forEach { row -> array.put(JSONObject().put("server_seq", row.serverSeq)
            .put("shop_id", row.shopId).put("table", row.tableName).put("uid", row.rowUid)
            .put("payload", row.payload).put("origin_device", row.originDevice)) }
        return SyncHttpResponse(200, JSONObject().put("ok", true).put("records", array).toString().toByteArray())
    }
}
