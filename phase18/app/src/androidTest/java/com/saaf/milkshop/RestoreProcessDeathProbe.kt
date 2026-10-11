package com.saaf.milkshop

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Run only by the host crash controller. Preparation is deliberately killed, not a passing test. */
@RunWith(AndroidJUnit4::class)
class RestoreProcessDeathProbe {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String): File = base.getDatabasePath("audit_restore_case_$name")
        override fun getFilesDir(): File = File(base.filesDir, "audit_restore_case").also { it.mkdirs() }
        override fun getNoBackupFilesDir(): File = File(base.noBackupFilesDir, "audit_restore_case").also { it.mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            base.getSharedPreferences("audit_restore_case_$name", mode)
    }
    private val marker get() = File(fixture.filesDir, "ready.json")
    private fun requireIsolation() {
        check(base.packageName == "com.saaf.milkshop.qa.tests")
        check(fixture.getDatabasePath("milk_shop.db").name == "audit_restore_case_milk_shop.db")
    }
    private fun clearFixture() {
        base.deleteDatabase("audit_restore_case_milk_shop.db")
        marker.delete()
        SyncTokenStore(fixture).clear()
        SyncRebaselineStore(fixture).clear()
        SyncShopBindingStore.clear(fixture)
        SyncServerConfigStore.clear(fixture)
    }
    private suspend fun signalAndWait(stage: String, expectedDevice: String) {
        marker.writeText(JSONObject().put("stage", stage).put("expected_device", expectedDevice).toString())
        // The host waits for this marker, force-stops only the QA target, then
        // starts a new instrumentation process to verify durable recovery.
        while (true) delay(1000)
    }

    @Test fun prepareCrashWindow() = runBlocking {
        requireIsolation()
        val stage = InstrumentationRegistry.getArguments().getString("audit_crash_stage")
        assumeTrue("Requires the opt-in host crash controller", stage == "before_commit" || stage == "after_commit")
        clearFixture()
        val source = MilkShopDatabase.createInMemory()
        val bytes = try {
            UserSecurityService(source).bootstrapOwner("crash.source", "Crash Source",
                "isolated restore process probe 2026".toCharArray())
            VendorManagementService(source).create("Restored marker vendor")
            BackupManager(fixture).buildCompleteSnapshotBytes(source)
        } finally { source.close() }
        val json = readAndVerify(bytes)
        val expectedDevice = json.getJSONObject("metadata").getString("device_id")
        val db = MilkShopDatabase.create(fixture)
        VendorManagementService(db).create("Before crash marker vendor")
        assertTrue("Room must use the isolated fixture database", fixture.getDatabasePath("milk_shop.db").isFile)
        SyncServerConfigStore.save(fixture, SyncServerConfig(SyncServerConfig.ENVIRONMENT_DEV,
            "https://restore-probe.invalid", "sb_publishable_fixture"))
        SyncShopBindingStore.save(fixture, SyncShopBinding(SyncServerConfig.ENVIRONMENT_DEV,
            "fixture-business", "fixture-shop", "Fixture Shop", "fixture-staff", "owner"))
        SyncTokenStore(fixture).save(SyncTokenStore.Session("fixture-access", "fixture-refresh", null))
        val engine = BackupRestoreEngine(
            createSafetySnapshot = {}, // Crash probes isolate the restore commit boundary, not snapshot-file creation.
            buildVerifiedBytes = { current, _, _ ->
                if (stage == "before_commit") signalAndWait(stage, expectedDevice)
                BackupManager(fixture).buildCompleteSnapshotBytes(current)
            },
            capturePreferences = { JSONObject() }, restorePreferences = {},
            rebaselineStore = SyncRebaselineStore(fixture),
            afterRestore = { signalAndWait("after_commit", expectedDevice) },
        )
        engine.restore(db, json, BackupManager.RestoreSelection(true, true, false, security = true), true)
        fail("The host must terminate the preparation process at the requested marker")
    }

    @Test fun verifyRecoveryAfterProcessDeath() = runBlocking {
        requireIsolation()
        val stage = InstrumentationRegistry.getArguments().getString("audit_crash_stage")
        assumeTrue("Requires the opt-in host crash controller", stage == "before_commit" || stage == "after_commit")
        val ready = JSONObject(marker.readText())
        assertEquals(stage, ready.getString("stage"))
        val db = MilkShopDatabase.create(fixture)
        try {
            val flags = SyncRebaselineStore(fixture)
            assertTrue(flags.needsFreshIdentity)
            assertTrue(flags.restoredRebaseline)
            assertEquals(ready.getString("expected_device"), flags.prevDeviceId)
            assertFalse(flags.liveRebaselinePending)
            val accounts = db.accountDao().findByCategory("Vendor").map { it.name.removePrefix("Vendor: ") }
            val queue = db.syncOutboxDao().all()
            assertTrue(queue.isNotEmpty())
            if (stage == "before_commit") {
                assertTrue(accounts.contains("Before crash marker vendor"))
                assertFalse(accounts.contains("Restored marker vendor"))
                assertEquals(0L, db.appUserDao().count())
                assertTrue(queue.any { !it.held })
            } else {
                assertEquals("after_commit", stage)
                assertFalse(accounts.contains("Before crash marker vendor"))
                assertTrue(accounts.contains("Restored marker vendor"))
                assertEquals(1L, db.appUserDao().count())
                assertTrue(queue.all { it.held })
            }
            // Cleanup never ran: a stale encrypted token exists, but the fresh
            // identity fence must prevent even a single network request.
            assertNotNull(SyncTokenStore(fixture).load())
            var networkCalls = 0
            val runtime = SyncRuntime(fixture, db, SyncHttp { networkCalls++; error("Unexpected network access") })
            assertFalse(runtime.isEnrolled())
            assertEquals(SyncPushEngine.DrainOutcome.NotConfigured, runtime.drainOnce())
            assertEquals(0, networkCalls)
        } finally { db.close(); clearFixture() }
    }
}
