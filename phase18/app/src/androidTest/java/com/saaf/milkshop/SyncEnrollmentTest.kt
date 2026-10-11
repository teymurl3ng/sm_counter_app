package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.BusinessClock
import com.saaf.milkshop.sync.SyncEnrollment
import com.saaf.milkshop.sync.SyncHttp
import com.saaf.milkshop.sync.SyncHttpRequest
import com.saaf.milkshop.sync.SyncHttpResponse
import com.saaf.milkshop.sync.SyncScheduler
import com.saaf.milkshop.sync.SyncServerConfig
import com.saaf.milkshop.sync.SyncServerConfigStore
import com.saaf.milkshop.sync.SyncShopBindingStore
import com.saaf.milkshop.sync.SyncTokenStore
import com.saaf.milkshop.sync.TrustedTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

/**
 * Phase 6: enrollment writes config + session + binding and arms the push
 * scheduler only on a definitive ALLOWED; every other verdict leaves zero
 * state behind. Scheduling is suppressed so tests never touch WorkManager.
 */
@RunWith(AndroidJUnit4::class)
class SyncEnrollmentTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = SyncServerConfig(
        environment = SyncServerConfig.ENVIRONMENT_DEV,
        baseUrl = "https://saafmilk-dev.example",
        publishableKey = "sb_publishable_test",
    )

    @Before
    fun setUp() {
        TrustedTime.resetForTest(context)
        TrustedTime.warmUp(context)
        SyncScheduler.suppressSchedulingForTest()
        com.saaf.milkshop.sync.SyncRebaselineStore(context).clear()
        runBlocking { SyncEnrollment(context, fakeAuthHttp()).unenroll() }
    }

    @After
    fun tearDown() {
        runBlocking { SyncEnrollment(context, fakeAuthHttp()).unenroll() }
        TrustedTime.resetForTest(context)
        com.saaf.milkshop.sync.SyncRebaselineStore(context).clear()
        BusinessClock.setZone(ZoneId.systemDefault().id)
    }

    private fun fakeAuthHttp() = SyncHttp { SyncHttpResponse(500, null) }

    private fun allowedHttp(active: Boolean = true, role: String = "owner") = SyncHttp { request ->
        when {
            request.url.contains("/auth/v1/token") -> SyncHttpResponse(
                200,
                """{"access_token":"jwt-1","refresh_token":"r-1","expires_in":3600}""".toByteArray(),
            )
            request.url.contains("my_profile") -> SyncHttpResponse(
                200,
                """
                {"staff_id":"11111111-1111-4111-8111-111111111111","role":"$role","active":$active,
                 "terminated_at":null,"business_id":"22222222-2222-4222-8222-222222222222",
                 "shops":[{"id":"249b9c78-475d-483d-b252-314dc5784ff9","name":"Recovery Shop A","timezone":"Asia/Karachi"}]}
                """.trimIndent().toByteArray(),
            )
            else -> SyncHttpResponse(500, null)
        }
    }

    @Test
    fun enrollment_persistsBindingSessionConfig_andSetsZone() = runBlocking {
        val database = MilkShopDatabase.createInMemory()
        try {
            database.appUserDao().insert(com.saaf.milkshop.data.AppUserEntity(
                loginName = "test.owner", displayName = "Owner", role = "owner",
                createdAt = "2026-10-10T08:00:00Z", updatedAt = "2026-10-10T08:00:00Z",
            ))
            val outcome = SyncEnrollment(context, allowedHttp(), database)
                .enroll(config, "Owner", "123456789012345".toCharArray())

            assertTrue(outcome.toString(), outcome is SyncEnrollment.Outcome.Enrolled)
            outcome as SyncEnrollment.Outcome.Enrolled
            assertEquals("249b9c78-475d-483d-b252-314dc5784ff9", outcome.binding.shopId)
            assertEquals("Asia/Karachi", outcome.shopTimezone)

            assertNotNull(SyncServerConfigStore.load(context))
            assertEquals("Asia/Karachi", BusinessClock.currentZone().id)
            val binding = SyncShopBindingStore.load(context)!!
            assertEquals("owner", binding.role)
            assertEquals("11111111-1111-4111-8111-111111111111", binding.staffId)
            assertEquals("jwt-1", SyncTokenStore(context).load()?.accessToken)
            // Re-baseline: the full local history is enqueued for the new server.
            assertTrue(database.syncOutboxDao().count() > 0)
            // The pull cursor restarts at zero for the new environment.
            assertEquals(0L, database.syncPullStateDao().find()?.lastServerSeq)
        } finally {
            database.close()
        }
    }

    @Test
    fun ambiguousShop_withoutChoice_listsShops_andSavesNothing() = runBlocking {
        val twoShops = SyncHttp { request ->
            when {
                request.url.contains("/auth/v1/token") -> SyncHttpResponse(
                    200,
                    """{"access_token":"jwt-1","refresh_token":"r-1"}""".toByteArray(),
                )
                request.url.contains("my_profile") -> SyncHttpResponse(
                    200,
                    """{"staff_id":"s1","role":"cashier","active":true,"business_id":"b1",
                       "shops":[{"id":"shop-a","name":"A"},{"id":"shop-b","name":"B"}]}""".toByteArray(),
                )
                else -> SyncHttpResponse(500, null)
            }
        }
        val outcome = SyncEnrollment(context, twoShops).enroll(config, "Owner", "x".toCharArray())

        assertTrue(outcome is SyncEnrollment.Outcome.ShopsAvailable)
        assertEquals(2, (outcome as SyncEnrollment.Outcome.ShopsAvailable).shops.size)
        assertNull(SyncServerConfigStore.load(context))
        assertNull(SyncShopBindingStore.load(context))
        assertNull(SyncTokenStore(context).load())
    }

    @Test
    fun failedEnrollment_leavesZeroState() = runBlocking {
        val disabled = allowedHttp(active = false)
        val outcome = SyncEnrollment(context, disabled).enroll(config, "Owner", "x".toCharArray())
        assertTrue(outcome is SyncEnrollment.Outcome.Failed)

        val invalid = SyncHttp { request ->
            if (request.url.contains("/auth/v1/token")) {
                SyncHttpResponse(400, """{"error_code":"invalid_credentials"}""".toByteArray())
            } else {
                SyncHttpResponse(500, null)
            }
        }
        assertTrue(
            SyncEnrollment(context, invalid).enroll(config, "Owner", "x".toCharArray())
                is SyncEnrollment.Outcome.Failed,
        )

        val offline = SyncHttp { throw java.io.IOException("offline") }
        assertTrue(
            SyncEnrollment(context, offline).enroll(config, "Owner", "x".toCharArray())
                is SyncEnrollment.Outcome.Failed,
        )

        assertNull(SyncServerConfigStore.load(context))
        assertNull(SyncShopBindingStore.load(context))
        assertNull(SyncTokenStore(context).load())
    }

    @Test
    fun unenroll_clearsEverything() = runBlocking {
        SyncEnrollment(context, allowedHttp(role = "cashier")).enroll(config, "Counter", "123456789012345".toCharArray())
        assertNotNull(SyncShopBindingStore.load(context))

        SyncEnrollment(context, fakeAuthHttp()).unenroll()

        assertNull(SyncServerConfigStore.load(context))
        assertNull(SyncShopBindingStore.load(context))
        assertNull(SyncTokenStore(context).load())
    }

    @Test
    fun unauthorizedSelectionAndCrossShopReenrollmentLeaveExistingBindingUntouched() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val enrollment = SyncEnrollment(context, allowedHttp(role = "cashier"), db)
            assertEquals(SyncEnrollment.Outcome.Failed("shop_not_authorized"),
                enrollment.enroll(config, "Counter", "x".toCharArray(), "unlisted-shop"))
            assertNull(SyncTokenStore(context).load())
            assertNull(db.serverAccountDao().findBinding())
            val binding = com.saaf.milkshop.data.AccountInstallationBindingEntity(
                environment = config.environment, businessId = "22222222-2222-4222-8222-222222222222",
                shopId = "different-shop", shopNameSnapshot = "Existing", status = "active",
                createdAt = "2026-10-10", updatedAt = "2026-10-10")
            db.serverAccountDao().insertBinding(binding)
            assertEquals(SyncEnrollment.Outcome.Failed("installation_shop_mismatch"),
                enrollment.enroll(config, "Counter", "x".toCharArray()))
            assertEquals("different-shop", db.serverAccountDao().findBinding()!!.shopId)
            assertNull(SyncTokenStore(context).load())
            assertNull(SyncServerConfigStore.load(context))
        } finally { db.close() }
    }
}
