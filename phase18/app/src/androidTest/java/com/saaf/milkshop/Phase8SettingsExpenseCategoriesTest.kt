package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.application.bridge.SettingsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.BusinessSettingsService
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.hybrid.HybridActivity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class Phase8SettingsExpenseCategoriesTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var dispatcher: HybridBridgeDispatcher

    @Before
    fun setUp() {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        dispatcher = HybridBridgeDispatcher(database, security)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun ownerSavesEditableStoreIdentityAndExactPkrSettings() = runBlocking {
        val owner = security.bootstrapOwner("settings.owner", "Settings Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        val identity = JSONObject().put("storeName", "Saaf Model Town").put("phone", "03001234567").put("address", "Main Market")
        assertTrue(dispatch("command.saveStoreIdentity", identity, session).getBoolean("ok"))
        assertTrue(dispatch("command.updateBusinessSettings", settingsPayload(), session).getBoolean("ok"))

        val data = dispatch("query.businessSettings", session = session).getJSONObject("data")
        assertEquals("Saaf Model Town", data.getJSONObject("store").getString("storeName"))
        assertEquals("PKR", data.getJSONObject("store").getString("currencyCode"))
        assertEquals("18125", data.getString("vendorMilkRatePaisaPerUnit"))
        assertEquals("550", data.getString("vendorDeliveryFeePaisaPerUnit"))
        assertEquals("13.75", data.getString("tsStandardPercent"))
        assertEquals("23500", data.getString("retailMilkRatePaisaPerUnit"))
        assertEquals("26000", data.getString("retailYogurtRatePaisaPerUnit"))
        assertEquals("4500", data.getString("defaultDeliverySurchargePaisa"))

        val invalid = settingsPayload().put("retailMilkRatePaisaPerUnit", "0")
        val rejected = dispatch("command.updateBusinessSettings", invalid, session)
        assertFalse(rejected.getBoolean("ok"))
        assertEquals("validation_failed", rejected.getJSONObject("error").getString("code"))
        assertEquals(235.0, BusinessSettingsService(database).get().retailRateMilk, 0.000001)

        val audits = database.securityAuditDao().all()
        assertTrue(audits.any { it.action == SettingsBridgeCommandService.ACTION_SAVE_STORE_IDENTITY })
        assertTrue(audits.any { it.action == SettingsBridgeCommandService.ACTION_UPDATE_BUSINESS_SETTINGS })
        assertTrue(audits.filter { it.action.startsWith("bridge_") }.all { it.actorNameSnapshot == "Settings Owner" })
    }

    @Test
    fun managerMaintainsExpenseCategories_butCannotChangeShopWideSettings() = runBlocking {
        val owner = security.bootstrapOwner("category.owner", "Category Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("category.manager", "Category Manager", "manager"))
        security.completeUserEnrollment(pending.enrollmentToken, MANAGER_PASSWORD.toCharArray())
        val manager = security.login("category.manager", MANAGER_PASSWORD.toCharArray())

        val created = dispatch("command.createExpenseCategory", JSONObject().put("displayName", "Generator Fuel"), manager.sessionId)
        assertTrue(created.getBoolean("ok"))
        val accountId = created.getJSONObject("data").getString("entityId")
        assertTrue(dispatch("command.setExpenseCategoryArchived", JSONObject().put("accountId", accountId).put("archived", true), manager.sessionId).getBoolean("ok"))
        assertFalse(database.expenseCategoryProfileDao().findByAccountId(accountId.toLong())!!.active)
        assertTrue(dispatch("command.setExpenseCategoryArchived", JSONObject().put("accountId", accountId).put("archived", false), manager.sessionId).getBoolean("ok"))
        assertTrue(database.expenseCategoryProfileDao().findByAccountId(accountId.toLong())!!.active)

        val duplicate = dispatch("command.createExpenseCategory", JSONObject().put("displayName", "generator fuel"), manager.sessionId)
        assertFalse(duplicate.getBoolean("ok"))
        assertEquals("conflict", duplicate.getJSONObject("error").getString("code"))
        val missing = dispatch("command.setExpenseCategoryArchived", JSONObject().put("accountId", "9223372036854775807").put("archived", true), manager.sessionId)
        assertFalse(missing.getBoolean("ok"))
        assertEquals("not_found", missing.getJSONObject("error").getString("code"))

        val settingsDenied = dispatch("command.updateBusinessSettings", settingsPayload(), manager.sessionId)
        assertFalse(settingsDenied.getBoolean("ok"))
        assertEquals("permission_denied", settingsDenied.getJSONObject("error").getString("code"))
        assertEquals(1, database.accountDao().findByCategory("Operating Expense").count { it.name == "Expense: Generator Fuel" })
        assertTrue(database.securityAuditDao().all().any { it.action == SettingsBridgeCommandService.ACTION_CREATE_EXPENSE_CATEGORY && it.actorRoleSnapshot == "manager" })
    }

    @Test
    fun operatorCannotChangeExpenseCategoriesOrBusinessSettings() = runBlocking {
        val owner = security.bootstrapOwner("settings.permission.owner", "Settings Permission Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("settings.operator", "Settings Operator", "operator"))
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("settings.operator", OPERATOR_PASSWORD.toCharArray())

        val categoryDenied = dispatch("command.createExpenseCategory", JSONObject().put("displayName", "Forbidden Category"), operator.sessionId)
        val settingsDenied = dispatch("query.businessSettings", session = operator.sessionId)
        listOf(categoryDenied, settingsDenied).forEach {
            assertFalse(it.getBoolean("ok"))
            assertEquals("permission_denied", it.getJSONObject("error").getString("code"))
        }
        assertFalse(database.expenseCategoryProfileDao().all().any { it.displayName == "Forbidden Category" })
    }

    @Test
    fun packagedSettingsScreen_savesIdentityRatesAndCategoryLifecycleThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val storeName = "Device Shop ${System.currentTimeMillis()}"
        val categoryName = "Device Expense ${System.currentTimeMillis()}"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('settings-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('settings-nav').click(); true")
            waitFor(webView, "document.getElementById('store-name') !== null", 15_000L)

            setInput(webView, "store-name", storeName)
            setInput(webView, "store-phone", "03005550111")
            setTextArea(webView, "store-address", "Device Main Market")
            evaluate(webView, "document.getElementById('save-store-identity-button').click(); true")
            waitFor(webView, "document.querySelector('[role=status]')?.innerText.includes('Shop details saved') === true", 15_000L)
            val stored = app.database.storeProfileDao().get()
            assertNotNull(stored)
            assertEquals(storeName, stored!!.storeName)
            assertEquals("PKR", stored.currencyCode)

            setInput(webView, "vendor-milk-rate", "181.25")
            setInput(webView, "vendor-delivery-fee", "5.50")
            setInput(webView, "ts-standard-percent", "13.75")
            setInput(webView, "retail-milk-rate", "235")
            setInput(webView, "retail-yogurt-rate", "260")
            setInput(webView, "default-delivery-surcharge", "45")
            evaluate(webView, "document.getElementById('save-business-rates-button').click(); true")
            waitFor(webView, "document.querySelector('[role=status]')?.innerText.includes('Shop-wide rates saved') === true", 15_000L)
            val rates = BusinessSettingsService(app.database).get()
            assertEquals(181.25, rates.milkRateVendor, 0.000001)
            assertEquals(13.75, rates.tsStandard, 0.000001)
            assertEquals(260.0, rates.retailRateYogurt, 0.000001)

            evaluate(webView, "document.getElementById('expense-categories-tab').click(); true")
            waitFor(webView, "document.getElementById('add-expense-category-button') !== null")
            evaluate(webView, "document.getElementById('add-expense-category-button').click(); true")
            waitFor(webView, "document.getElementById('expense-category-name') !== null")
            setInput(webView, "expense-category-name", categoryName)
            evaluate(webView, "document.getElementById('save-expense-category-button').click(); true")
            waitFor(webView, "document.getElementById('native-expense-category-list')?.innerText.includes(${JSONObject.quote(categoryName)}) === true", 15_000L)
            val profile = app.database.expenseCategoryProfileDao().all().single { it.displayName == categoryName }

            evaluate(webView, "document.getElementById('archive-expense-category-${profile.accountId}').click(); true")
            waitFor(webView, "document.getElementById('confirm-expense-category-archive') !== null")
            evaluate(webView, "document.getElementById('confirm-expense-category-archive').click(); true")
            waitFor(webView, "document.getElementById('native-expense-category-list')?.innerText.includes(${JSONObject.quote(categoryName)}) === false", 15_000L)
            assertFalse(app.database.expenseCategoryProfileDao().findByAccountId(profile.accountId)!!.active)

            evaluate(webView, "document.getElementById('inactive-expense-categories-tab').click(); true")
            waitFor(webView, "document.getElementById('restore-expense-category-${profile.accountId}') !== null")
            evaluate(webView, "document.getElementById('restore-expense-category-${profile.accountId}').click(); true")
            waitFor(webView, "document.getElementById('native-expense-category-list')?.innerText.includes(${JSONObject.quote(categoryName)}) === false", 15_000L)
            assertTrue(app.database.expenseCategoryProfileDao().findByAccountId(profile.accountId)!!.active)

            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
    }

    private fun settingsPayload(): JSONObject = JSONObject()
        .put("vendorMilkRatePaisaPerUnit", "18125").put("vendorDeliveryFeePaisaPerUnit", "550")
        .put("tsStandardPercent", "13.75").put("retailMilkRatePaisaPerUnit", "23500")
        .put("retailYogurtRatePaisaPerUnit", "26000").put("defaultDeliverySurchargePaisa", "4500")

    private suspend fun dispatch(action: String, payload: JSONObject = JSONObject(), session: String? = null): JSONObject {
        val request = JSONObject().put("contractVersion", BridgeContractSnapshot.VERSION).put("requestId", UUID.randomUUID().toString()).put("action", action).put("payload", payload)
        if (session != null) request.put("sessionHandle", session)
        return JSONObject(dispatcher.dispatch(request.toString()))
    }

    private fun activityWebView(scenario: ActivityScenario<HybridActivity>): WebView {
        val holder = AtomicReference<WebView>()
        scenario.onActivity { holder.set(it.webViewForTesting()) }
        return holder.get()
    }

    private fun setInput(webView: WebView, id: String, value: String) {
        evaluate(webView, """(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('input', { bubbles: true })); return input.value; })()""")
    }

    private fun setTextArea(webView: WebView, id: String, value: String) {
        evaluate(webView, """(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('input', { bubbles: true })); return input.value; })()""")
    }

    private fun waitFor(webView: WebView, expression: String, timeoutMs: Long = 10_000L) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (evaluate(webView, "Boolean($expression)") == "true") return
            SystemClock.sleep(100)
        }
        throw AssertionError("Web condition did not become true: $expression")
    }

    private fun evaluate(webView: WebView, script: String): String? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<String?>()
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { webView.evaluateJavascript(script) { result.set(it); latch.countDown() } }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    companion object {
        const val OWNER_PASSWORD = "settings owner private passphrase 2026"
        const val MANAGER_PASSWORD = "settings manager private passphrase 2026"
        const val OPERATOR_PASSWORD = "settings operator private passphrase 2026"
    }
}
