package com.saaf.milkshop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.YearMonth
import org.json.JSONObject

/** Real disposable UI installation, never an ordinary or preview database. */
class StaffAcceptanceSetupTest {
    @Test fun prepareEmptyStaffFixtureAndVerifyNativeLaunch() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.saaf.milkshop.acceptance")
        check(!BuildConfig.ENABLE_NATIVE_PREVIEW_FIXTURES)
        val app = context.applicationContext as MilkShopApplication
        val db = app.database
        val security = app.userSecurity
        assertEquals("Never reset an existing acceptance fixture", 0L, db.appUserDao().count())
        assertEquals(0, db.eventDao().count())
        assertNull(SyncServerConfigStore.load(context))
        SyncScheduler.suppressSchedulingForTest()
        val day = BusinessClock.today()
        val owner = security.bootstrapOwner("day.owner", "Day Owner", PASSPHRASE.toCharArray()).session
        for (role in listOf("cashier", "operator")) {
            val pending = security.beginUserEnrollment(owner.sessionId,
                CreateUserInput("day.$role", "Day ${role.replaceFirstChar { it.uppercase() }}", role))
            security.completeUserEnrollment(pending.enrollmentToken, PASSPHRASE.toCharArray())
        }
        VendorManagementService(db).create("Day Supplier")
        CustomerManagementService(db).create("Day Customer")
        StoreIdentityService(db).save(StoreIdentityInput("Shop Day QA"))
        BusinessSettingsService(db).update(BusinessSettings(170.0, 11.0, 13.0, 190.0, 280.0, 50.0))
        val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
        OpeningStateService(db).record(OpeningStateInput(day.toString(),
            accountBalances = listOf(OpeningAccountBalance(cash.accountId, 10000.0))))
        security.reauthenticate(owner.sessionId, PASSPHRASE.toCharArray())
        CostingPolicyService(db, security).approve(owner.sessionId, CostingPolicyInput(
            calendarMonth = YearMonth.from(day).toString(), normalCapacityKg = BigDecimal("1000"),
            reviewReason = "monthly", components = listOf(
                "culture" to 100000L, "packaging" to 200000L, "variable_energy" to 300000L,
                "direct_labor" to 400000L, "fixed_production_overhead" to 500000L,
            ).map { (code, amount) -> ConversionCostComponentInput(code, amount, BigDecimal("1000")) },
            notes = "Synthetic allocation inputs; no new cash expense",
        ))
        security.logout(owner.sessionId)
        app.currentSessionId = null
        val before = listOf(db.backupDataDao().events(), db.backupDataDao().financialLogs(),
            db.backupDataDao().inventoryLogs(), db.backupDataDao().accounts(), db.appUserDao().all())
        ActivityScenario.launch<NativePreviewActivity>(Intent(context, NativePreviewActivity::class.java)).use {
            kotlinx.coroutines.delay(1000)
        }
        assertEquals(before, listOf(db.backupDataDao().events(), db.backupDataDao().financialLogs(),
            db.backupDataDao().inventoryLogs(), db.backupDataDao().accounts(), db.appUserDao().all()))
        assertEquals(1, db.eventDao().count())
        assertEquals(10000.0, db.readModelDao().accountBalances().single { it.name == "Cash" }.balance, 0.0)
        assertTrue(db.readModelDao().currentStock().all { it.quantityOnHand == 0.0 })
        assertEquals(setOf("day.owner", "day.cashier", "day.operator"), db.appUserDao().all().map { it.loginName }.toSet())
        assertNull(SyncServerConfigStore.load(context))
        File(context.filesDir, "staff-fixture.json").writeText(JSONObject()
            .put("package", context.packageName).put("date", day.toString())
            .put("shop", "Shop Day QA").put("cashPkr", 10000).put("stockEmpty", true)
            .put("nativeLaunchPreservedFixture", true).put("cloudConfigured", false).toString(2))
    }
    companion object { const val PASSPHRASE = "1234567890qwert" }
}
