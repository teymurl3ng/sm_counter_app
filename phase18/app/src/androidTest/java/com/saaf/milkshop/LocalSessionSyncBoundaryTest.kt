package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.application.authentication.NativeAuthenticationFacade
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalSessionSyncBoundaryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = SyncServerConfig("dev", "https://synthetic.invalid", "sb_publishable_test")
    private fun allowed() = SyncHttp { request -> when {
        request.url.contains("/auth/v1/token") -> SyncHttpResponse(200,
            """{"access_token":"synthetic-token","refresh_token":"synthetic-refresh","expires_in":3600}""".toByteArray())
        request.url.contains("my_profile") -> SyncHttpResponse(200,
            """{"staff_id":"11111111-1111-4111-8111-111111111111","username":"synthetic.counter","role":"cashier","active":true,"terminated_at":null,"business_id":"22222222-2222-4222-8222-222222222222","shops":[{"id":"33333333-3333-4333-8333-333333333333","name":"Synthetic Shop","shop_timezone":"Asia/Karachi"}]}""".toByteArray())
        else -> SyncHttpResponse(200, "{}".toByteArray())
    } }

    @Test fun localLockEndsSessionButPreservesTransportAndUnsentRecords() = boundary(stopSync = false)
    @Test fun stopSyncWithUnavailableRemoteLogoutPreservesLocalRowsAndUnsentWork() = boundary(stopSync = true)

    private fun boundary(stopSync: Boolean) = runBlocking {
        check(context.packageName == "com.saaf.milkshop.qa.tests")
        val database = MilkShopDatabase.createInMemory()
        val sessions = NativeSessionManager()
        val security = UserSecurityService(database, sessions)
        val phrase = "synthetic counter owner phrase 2026".toCharArray()
        SyncScheduler.suppressSchedulingForTest()
        SyncEnrollment(context, SyncHttp { SyncHttpResponse(503, null) }).unenroll()
        SyncRebaselineStore(context).clear()
        try {
            val owner = security.bootstrapOwner("local.boundary", "Synthetic Owner", phrase)
            val vendor = VendorManagementService(database).create("Synthetic Vendor")
            VendorPurchaseService(database).record(VendorPurchaseInput(vendor, "2026-10-13", 10.0, 4.0, 28.0))
            assertTrue(SyncEnrollment(context, allowed(), database)
                .enroll(config, "synthetic.counter", "123456789012345".toCharArray()) is SyncEnrollment.Outcome.Enrolled)
            val dao = database.backupDataDao()
            val beforeEvents = dao.events(); val beforeInventory = dao.inventoryLogs(); val beforeFinancial = dao.financialLogs()
            val beforeUsers = dao.appUsers(); val beforeBinding = database.serverAccountDao().findBinding()
            val beforeOutbox = database.syncOutboxDao().all()
            val beforeLinks = database.serverAccountDao().identityLinks()
            val token = SyncTokenStore(context).load(); val fileBinding = SyncShopBindingStore.load(context)
            assertTrue(beforeEvents.isNotEmpty()); assertTrue(beforeOutbox.isNotEmpty())
            assertNotNull(beforeBinding); assertTrue(beforeLinks.isEmpty())
            val requests = mutableListOf<SyncHttpRequest>()
            if (stopSync) {
                SyncEnrollment(context, SyncHttp { request -> requests += request; SyncHttpResponse(503, null) }, database).unenroll()
                assertEquals(1, requests.size)
                assertTrue(requests.single().url.endsWith("/auth/v1/logout?scope=local"))
                assertNull(SyncServerConfigStore.load(context)); assertNull(SyncTokenStore(context).load())
                assertNull(SyncShopBindingStore.load(context))
                assertEquals(owner.user.userId, sessions.require(owner.session.sessionId).userId)
                assertFalse(SyncRuntime(context, database, allowed()).isEnrolled())
                assertTrue(SyncRuntime(context, database, allowed()).drainOnce() is SyncPushEngine.DrainOutcome.NotConfigured)
            } else {
                NativeAuthenticationFacade(database, security).signOut(owner.session.sessionId)
                assertTrue(runCatching { sessions.require(owner.session.sessionId) }.isFailure)
                assertEquals(config, SyncServerConfigStore.load(context))
                assertEquals(token, SyncTokenStore(context).load()); assertEquals(fileBinding, SyncShopBindingStore.load(context))
                assertTrue(SyncRuntime(context, database, allowed()).isEnrolled())
            }
            assertEquals(beforeEvents, dao.events()); assertEquals(beforeInventory, dao.inventoryLogs())
            assertEquals(beforeFinancial, dao.financialLogs()); assertEquals(beforeUsers, dao.appUsers())
            assertEquals(beforeBinding, database.serverAccountDao().findBinding())
            assertEquals(beforeLinks, database.serverAccountDao().identityLinks())
            assertEquals(beforeOutbox, database.syncOutboxDao().all())
        } finally {
            database.close(); phrase.fill('\u0000')
            SyncEnrollment(context, SyncHttp { SyncHttpResponse(503, null) }).unenroll()
            SyncRebaselineStore(context).clear()
            BusinessClock.setZone(java.time.ZoneId.systemDefault().id)
            TrustedTime.resetForTest(context)
        }
    }
}
