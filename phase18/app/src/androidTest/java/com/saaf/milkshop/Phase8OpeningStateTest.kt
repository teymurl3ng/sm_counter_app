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
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.hybrid.HybridActivity
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
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
class Phase8OpeningStateTest {
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
    fun ownerRecordsExactOpeningState_afterFreshPassphrase_withActorAndAudit() = runBlocking {
        var now = 1_000_000L
        val sessions = NativeSessionManager { now }
        security = UserSecurityService(database, sessions = sessions, nowEpochMs = { now })
        dispatcher = HybridBridgeDispatcher(database, security)
        val owner = security.bootstrapOwner("opening.owner", "Opening Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        saveStore(session)
        val milkId = database.productDao().findByName("Milk")!!.productId
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        now += NativeSessionManager.RECENT_AUTH_TIMEOUT_MS

        val stale = dispatch("command.recordOpeningState", openingPayload(milkId, "12.75", cashId, "345678"), session)
        assertFalse(stale.getBoolean("ok"))
        assertEquals("recent_authentication_required", stale.getJSONObject("error").getString("code"))
        assertEquals(0, database.eventDao().count())

        assertTrue(dispatch("command.auth.reauthenticate", JSONObject().put("passphrase", OWNER_PASSWORD), session).getBoolean("ok"))
        val recorded = dispatch("command.recordOpeningState", openingPayload(milkId, "12.75", cashId, "345678"), session)
        assertTrue(recorded.getBoolean("ok"))
        val eventId = recorded.getJSONObject("data").getString("eventId").toLong()
        val settings = dispatch("query.businessSettings", session = session).getJSONObject("data").getJSONObject("openingState")
        assertEquals(eventId.toString(), settings.getString("eventId"))
        assertEquals("12.75", settings.getJSONArray("inventoryBalances").getJSONObject(0).getString("quantity"))
        assertEquals("345678", settings.getJSONArray("accountBalances").getJSONObject(0).getString("balancePaisa"))
        assertEquals("Existing shop figures", settings.getString("notes"))
        assertEquals("Opening Owner", database.eventActorLinkDao().find(eventId)!!.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any { it.action == SettingsBridgeCommandService.ACTION_RECORD_OPENING_STATE && it.details == "event_id=$eventId" })
    }

    @Test
    fun correctionRestatesTheCompleteSnapshot_butLedgersReceiveOnlyDifferences() = runBlocking {
        val owner = security.bootstrapOwner("correction.owner", "Correction Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        saveStore(session)
        val milkId = database.productDao().findByName("Milk")!!.productId
        val paymentAccounts = database.accountDao().findByCategory("Cash/Bank")
        val cashId = paymentAccounts.single { it.name == "Cash" }.accountId
        val bankId = paymentAccounts.single { it.name == "Bank" }.accountId
        val original = dispatch("command.recordOpeningState", openingPayload(milkId, "12", cashId, "10000"), session)
        val originalId = original.getJSONObject("data").getString("eventId").toLong()

        val correctedPayload = JSONObject()
            .put("openingDate", "2026-08-01")
            .put("inventoryBalances", JSONArray().put(JSONObject().put("productId", milkId.toString()).put("quantity", "9")))
            .put("accountBalances", JSONArray()
                .put(JSONObject().put("accountId", cashId.toString()).put("balancePaisa", "15000"))
                .put(JSONObject().put("accountId", bankId.toString()).put("balancePaisa", "2000")))
            .put("notes", "Counted again")
        val corrected = dispatch("command.correctOpeningState", correctedPayload, session)
        assertTrue(corrected.getBoolean("ok"))
        val correctionId = corrected.getJSONObject("data").getString("eventId").toLong()

        assertEquals(originalId, database.eventDao().findById(correctionId)!!.correctsEventId)
        assertEquals(-3.0, database.inventoryLogDao().findByEventId(correctionId).single().quantity, 0.000001)
        val moneyDeltas = database.financialLogDao().findByEventId(correctionId).associate { it.accountId to it.amount }
        assertEquals(50.0, moneyDeltas.getValue(cashId), 0.000001)
        assertEquals(20.0, moneyDeltas.getValue(bankId), 0.000001)
        assertEquals(2, OpeningStateService(database).history().size)
        val current = OpeningStateService(database).current()!!
        assertEquals(9.0, current.inventoryBalances.single().quantity, 0.000001)
        assertEquals(setOf(cashId, bankId), current.accountBalances.map { it.accountId }.toSet())
        assertNotNull(database.eventActorLinkDao().find(correctionId))
        assertTrue(database.securityAuditDao().all().any { it.action == SettingsBridgeCommandService.ACTION_CORRECT_OPENING_STATE })
    }

    @Test
    fun wrongPassphraseUnauthorizedRoleAndLateOpeningAreRejectedWithoutArtificialEntry() = runBlocking {
        val owner = security.bootstrapOwner("protected.owner", "Protected Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        saveStore(session)
        val pending = security.beginUserEnrollment(session, CreateUserInput("protected.manager", "Protected Manager", "manager"))
        security.completeUserEnrollment(pending.enrollmentToken, MANAGER_PASSWORD.toCharArray())
        val manager = security.login("protected.manager", MANAGER_PASSWORD.toCharArray())
        val milkId = database.productDao().findByName("Milk")!!.productId
        val cashId = database.accountDao().findByCategory("Cash/Bank").first().accountId
        val payload = openingPayload(milkId, "1", cashId, "100")

        val wrongPassword = dispatch("command.auth.reauthenticate", JSONObject().put("passphrase", "definitely wrong passphrase"), session)
        assertFalse(wrongPassword.getBoolean("ok"))
        assertEquals("authentication_failed", wrongPassword.getJSONObject("error").getString("code"))
        val denied = dispatch("command.recordOpeningState", payload, manager.sessionId)
        assertFalse(denied.getBoolean("ok"))
        assertEquals("permission_denied", denied.getJSONObject("error").getString("code"))
        assertEquals(0, database.eventDao().count())

        ExpenseService(database).record(ExpenseInput("Rent", cashId, 1.0, "2026-08-01"))
        val late = dispatch("command.recordOpeningState", payload, session)
        assertFalse(late.getBoolean("ok"))
        assertEquals("conflict", late.getJSONObject("error").getString("code"))
        assertEquals(0, database.openingStateDao().initialCount())
    }

    @Test
    fun packagedOpeningScreen_explainsDirectionsAndRequiresPassphraseBeforeSubmission() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        if (app.database.storeProfileDao().get() == null) {
            StoreIdentityService(app.database).save(StoreIdentityInput("Device Test Shop", null, null))
        }
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('settings-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('settings-nav').click(); true")
            waitFor(webView, "document.getElementById('opening-state-tab') !== null")
            evaluate(webView, "document.getElementById('opening-state-tab').click(); true")
            waitFor(webView, "document.getElementById('review-opening-state-button') !== null || document.getElementById('correct-opening-state-button') !== null", 15_000L)
            assertFalse(evaluate(webView, "document.body.innerText.includes('Recorded money total')") == "true")
            if (evaluate(webView, "document.getElementById('opening-account-balance-explanation') !== null") == "true") {
                assertTrue(evaluate(webView, "document.getElementById('opening-account-balance-explanation').innerText.includes('not combined into one total')") == "true")
            }
            if (evaluate(webView, "document.getElementById('correct-opening-state-button') !== null") == "true") {
                evaluate(webView, "document.getElementById('correct-opening-state-button').click(); true")
                waitFor(webView, "document.getElementById('review-opening-state-button') !== null")
            }
            assertTrue(evaluate(webView, "document.body.innerText.includes('Customer owes shop') && document.body.innerText.includes('Shop owes supplier')") == "true")
            evaluate(webView, "document.querySelector('[id^=opening-product-]').value = '1'; document.querySelector('[id^=opening-product-]').dispatchEvent(new Event('input', { bubbles: true })); document.getElementById('confirm-opening-state').click(); document.getElementById('review-opening-state-button').click(); true")
            waitFor(webView, "document.getElementById('opening-passphrase') !== null")
            assertTrue(evaluate(webView, "document.getElementById('opening-passphrase').type === 'password'") == "true")
            evaluate(webView, "document.getElementById('logout-button').click(); true")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
        Unit
    }

    private suspend fun saveStore(session: String) {
        val payload = JSONObject().put("storeName", "Opening Test Shop").put("phone", JSONObject.NULL).put("address", JSONObject.NULL)
        assertTrue(dispatch("command.saveStoreIdentity", payload, session).getBoolean("ok"))
    }

    private fun openingPayload(productId: Long, quantity: String, accountId: Long, balancePaisa: String) = JSONObject()
        .put("openingDate", "2026-08-01")
        .put("inventoryBalances", JSONArray().put(JSONObject().put("productId", productId.toString()).put("quantity", quantity)))
        .put("accountBalances", JSONArray().put(JSONObject().put("accountId", accountId.toString()).put("balancePaisa", balancePaisa)))
        .put("notes", "Existing shop figures")

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
        const val OWNER_PASSWORD = "opening owner private passphrase 2026"
        const val MANAGER_PASSWORD = "opening manager private passphrase 2026"
    }
}
