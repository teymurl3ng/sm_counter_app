package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room3.withWriteTransaction
import androidx.room3.executeSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncRuntimeRecoveryRegressionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = SyncServerConfig("dev", "https://example.invalid", "public-test-key")
    private val oldVerified = System.currentTimeMillis() - 20L * 86_400_000
    @Before fun setup() {
        SyncScheduler.suppressSchedulingForTest()
        SyncRebaselineStore(context).clear()
        SyncServerConfigStore.save(context, config)
        SyncShopBindingStore.save(context, SyncShopBinding("dev", "business", "shop", "Shop", "staff", "cashier"))
        SyncTokenStore(context).save(SyncTokenStore.Session("test", null, null))
    }
    @After fun cleanup() {
        SyncRebaselineStore(context).clear()
        SyncServerConfigStore.clear(context)
        SyncShopBindingStore.clear(context)
        SyncTokenStore(context).clear()
    }
    private suspend fun bind(db: MilkShopDatabase) {
        db.serverAccountDao().insertBinding(AccountInstallationBindingEntity(environment = "dev",
            businessId = "business", shopId = "shop", shopNameSnapshot = "Shop", status = "active",
            verifiedAtEpochMs = oldVerified, createdAt = "2026-10-10", updatedAt = "2026-10-10"))
    }
    @Test fun failedPartialRebaselineNeverPushesAndRestartsFromZero() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            bind(db)
            db.syncOutboxDao().deleteAll()
            SyncBaseline.enqueueAllHeld(db)
            SyncRebaselineStore(context).liveRebaselinePending = true
            var pullCalls = 0; var pushes = 0; var fail = true
            val cursors = mutableListOf<Long>()
            val batch = JSONArray()
            repeat(100) { i -> batch.put(JSONObject().put("server_seq", i+1).put("shop_id", "shop")
                .put("table", "products").put("uid", "remote-$i").put("origin_device", "remote")
                .put("payload", JSONObject().put("name", "Remote $i").put("unit", "Liters"))) }
            val runtime = SyncRuntime(context, db, SyncHttp { request ->
                when {
                    request.url.contains("sync_pull") -> {
                        cursors += JSONObject(String(request.body!!)).getLong("since_seq")
                        pullCalls++
                        if (fail && pullCalls == 1) SyncHttpResponse(200, JSONObject().put("ok", true).put("records", batch).toString().toByteArray())
                        else if (fail) SyncHttpResponse(503, null)
                        else SyncHttpResponse(200, """{"ok":true,"records":[]}""".toByteArray())
                    }
                    request.url.contains("sync_push") -> { pushes++; SyncHttpResponse(200, """{"ok":true}""".toByteArray()) }
                    else -> SyncHttpResponse(503, null)
                }
            })
            assertTrue(runtime.drainOnce() is SyncPushEngine.DrainOutcome.RetryLater)
            assertEquals(0, pushes)
            assertTrue(SyncRebaselineStore(context).liveRebaselinePending)
            assertTrue(db.syncOutboxDao().all().all { it.held })
            fail = false
            runtime.drainOnce()
            assertEquals(0L, cursors.last())
            assertFalse(SyncRebaselineStore(context).liveRebaselinePending)
            assertTrue(pushes > 0)
        } finally { db.close() }
    }
    @Test fun offlineDisabledMalformedAndWrongShopNeverRenewGrace() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            bind(db); db.syncOutboxDao().deleteAll()
            val valid = JSONObject().put("staff_id", "staff").put("role", "cashier").put("active", true)
                .put("terminated_at", JSONObject.NULL).put("business_id", "business")
                .put("shops", JSONArray().put(JSONObject().put("id", "shop")))
            val replies = listOf(SyncHttpResponse(503, null),
                SyncHttpResponse(200, JSONObject(valid.toString()).put("active", false).toString().toByteArray()),
                SyncHttpResponse(200, JSONObject(valid.toString()).apply { remove("active") }.toString().toByteArray()),
                SyncHttpResponse(200, JSONObject(valid.toString()).put("shops", JSONArray()).toString().toByteArray()))
            for (reply in replies) {
                SyncRuntime(context, db, SyncHttp { reply }).drainOnce()
                assertEquals(oldVerified, db.serverAccountDao().findBinding()!!.verifiedAtEpochMs)
            }
            SyncRuntime(context, db, SyncHttp { SyncHttpResponse(200, valid.toString().toByteArray()) }).drainOnce()
            assertTrue(db.serverAccountDao().findBinding()!!.verifiedAtEpochMs!! > oldVerified)
        } finally { db.close() }
    }
    @Test fun unfinishedRestoreIdentityCannotUseOldStoredTokens() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            SyncRebaselineStore(context).needsFreshIdentity = true
            var calls = 0
            val runtime = SyncRuntime(context, db, SyncHttp { calls++; SyncHttpResponse(200, null) })
            assertFalse(runtime.isEnrolled())
            assertTrue(runtime.drainOnce() is SyncPushEngine.DrainOutcome.NotConfigured)
            assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.NotEnrolled)
            assertEquals(0, calls)
        } finally { db.close() }
    }

    @Test fun profileProofCannotRenewAnotherIdentityAndAuditFailureCannotPartiallyRenewBinding() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            bind(db); db.syncOutboxDao().deleteAll()
            val user = db.appUserDao().insert(AppUserEntity(loginName="unrelated.owner",displayName="Other owner",role="owner",createdAt="2026-10-10",updatedAt="2026-10-10"))
            db.serverAccountDao().insertIdentityLink(ServerIdentityLinkEntity(localUserId=user,serverStaffId="unrelated-staff",businessId="business",status="verified",verifiedAtEpochMs=oldVerified))
            val proof="""{"staff_id":"staff","business_id":"business","role":"cashier","active":true,"shops":[{"id":"shop"}]}"""
            val runtime=SyncRuntime(context,db,SyncHttp { SyncHttpResponse(200,proof.toByteArray()) })
            runtime.drainOnce()
            assertTrue(db.serverAccountDao().findBinding()!!.verifiedAtEpochMs!! > oldVerified)
            assertEquals(oldVerified,db.serverAccountDao().findIdentityLink(user)!!.verifiedAtEpochMs)
            val binding=db.serverAccountDao().findBinding()!!
            db.serverAccountDao().updateBindingStatus(binding.bindingId,"active",oldVerified,"2026-10-10")
            db.withWriteTransaction { executeSQL("""CREATE TRIGGER reject_test_recovery_audit
                BEFORE INSERT ON security_audit_entries WHEN NEW.action='server_verification_recovered'
                BEGIN SELECT RAISE(ABORT,'Injected recovery audit failure'); END""") }
            runtime.drainOnce()
            assertEquals(oldVerified,db.serverAccountDao().findBinding()!!.verifiedAtEpochMs)
        } finally { db.close() }
    }
}
