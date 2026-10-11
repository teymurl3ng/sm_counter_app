package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.application.commands.ReconciliationBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.VerifiedDailyReopenInput
import com.saaf.milkshop.hybrid.HybridActivity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class Phase8ReconciliationOperationsTest {
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
    fun physicalCountComparesAgainstCanonicalCashWithoutChangingLedger() = runBlocking {
        val owner = security.bootstrapOwner("count.owner", "Count Owner", OWNER_PASSWORD.toCharArray())
        val cashId = cashAccountId(database)
        val vendorId = VendorManagementService(database).create("Count Dairy")
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, BUSINESS_DATE, 10.0, 4.0, 28.0))
        val milkId = database.productDao().findByName("Milk")!!.productId
        SaleService(database).record(SaleInput(milkId, BUSINESS_DATE, "retail", 200.0, quantity = 1.0, payment = SalePayment.Account(cashId)))
        val eventCount = database.eventDao().count()

        val response = dispatch("command.recordPhysicalCashCount", countPayload(cashId, BUSINESS_DATE, "19000"), owner.session.sessionId)
        assertTrue(response.toString(), response.getBoolean("ok"))
        val result = response.getJSONObject("data")
        val countId = result.getString("countId").toLong()
        assertEquals("19000", result.getString("countedCashPaisa"))
        assertEquals("20000", result.getString("expectedCashPaisa"))
        assertEquals("-1000", result.getString("variancePaisa"))
        assertEquals(eventCount, database.eventDao().count())
        assertEquals(200.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 0.0)

        val archive = dispatch("query.reconciliationArchive", session = owner.session.sessionId).getJSONObject("data")
        val count = archive.getJSONArray("cashCounts").getJSONObject(0)
        assertEquals(countId.toString(), count.getString("countId"))
        assertEquals("Drawer counted independently", count.getString("notes"))
        assertTrue(database.securityAuditDao().all().any { it.action == ReconciliationBridgeCommandService.ACTION_RECORD_CASH_COUNT && it.actorNameSnapshot == "Count Owner" })
    }

    @Test
    fun closeRequiresLatestCountAndVarianceReasonThenProtectsHistory() = runBlocking {
        val owner = security.bootstrapOwner("close.owner", "Close Owner", OWNER_PASSWORD.toCharArray())
        val cashId = cashAccountId(database)
        val first = dispatch("command.recordPhysicalCashCount", countPayload(cashId, BUSINESS_DATE, "9000"), owner.session.sessionId).getJSONObject("data").getString("countId")
        val latest = dispatch("command.recordPhysicalCashCount", countPayload(cashId, BUSINESS_DATE, "10000"), owner.session.sessionId).getJSONObject("data").getString("countId")

        assertError(dispatch("command.closeBusinessDate", closePayload(BUSINESS_DATE, first, "Old count"), owner.session.sessionId), "conflict")
        assertError(dispatch("command.closeBusinessDate", closePayload(BUSINESS_DATE, latest), owner.session.sessionId), "validation_failed")
        val closed = dispatch("command.closeBusinessDate", closePayload(BUSINESS_DATE, latest, "Drawer overage is under review"), owner.session.sessionId)
        assertTrue(closed.toString(), closed.getBoolean("ok"))
        assertEquals("closed", closed.getJSONObject("data").getString("statusCode"))
        assertEquals("10000", closed.getJSONObject("data").getString("variancePaisa"))
        assertError(dispatch("command.recordPhysicalCashCount", countPayload(cashId, BUSINESS_DATE, "10000"), owner.session.sessionId), "day_closed")
        assertError(dispatch("command.recordPhysicalCashCount", countPayload(cashId, EARLIER_DATE, "10000"), owner.session.sessionId), "day_closed")
        assertError(dispatch("command.closeBusinessDate", closePayload(BUSINESS_DATE, latest, "Again"), owner.session.sessionId), "conflict")

        val action = database.dailyCloseDao().currentAction(BUSINESS_DATE)!!
        assertEquals("Close Owner", action.actorName)
        assertEquals("Drawer overage is under review", action.reason)
        assertTrue(database.securityAuditDao().all().any { it.action == ReconciliationBridgeCommandService.ACTION_CLOSE_DATE })
        assertEquals(0, database.eventDao().count())
    }

    @Test
    fun reopenNeedsOwnerRecentAuthenticationAndNewCountBeforeReclose() = runBlocking {
        var now = 1_000_000L
        val sessions = NativeSessionManager { now }
        security = UserSecurityService(database, sessions = sessions, nowEpochMs = { now })
        dispatcher = HybridBridgeDispatcher(database, security)
        val owner = security.bootstrapOwner("reopen.owner", "Reopen Owner", OWNER_PASSWORD.toCharArray())
        val cashierPending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("reopen.cashier", "Reopen Cashier", "cashier"))
        security.completeUserEnrollment(cashierPending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val operatorPending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("reopen.operator", "Reopen Operator", "operator"))
        security.completeUserEnrollment(operatorPending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val cashier = security.login("reopen.cashier", CASHIER_PASSWORD.toCharArray())
        val operator = security.login("reopen.operator", OPERATOR_PASSWORD.toCharArray())
        val cashId = cashAccountId(database)
        val countId = dispatch("command.recordPhysicalCashCount", countPayload(cashId, BUSINESS_DATE, "0"), cashier.sessionId).getJSONObject("data").getString("countId")
        assertTrue(dispatch("command.closeBusinessDate", closePayload(BUSINESS_DATE, countId), cashier.sessionId).getBoolean("ok"))
        assertError(dispatch("query.reconciliationArchive", session = operator.sessionId), "permission_denied")
        assertError(dispatch("command.reopenBusinessDate", reopenPayload(BUSINESS_DATE), cashier.sessionId), "permission_denied")

        now += NativeSessionManager.RECENT_AUTH_TIMEOUT_MS
        assertError(dispatch("command.reopenBusinessDate", reopenPayload(BUSINESS_DATE), owner.session.sessionId), "recent_authentication_required")
        assertTrue(dispatch("command.auth.reauthenticate", JSONObject().put("passphrase", OWNER_PASSWORD), owner.session.sessionId).getBoolean("ok"))
        assertEquals("reopened", dispatch("command.reopenBusinessDate", reopenPayload(BUSINESS_DATE), owner.session.sessionId).getJSONObject("data").getString("statusCode"))
        assertError(dispatch("command.closeBusinessDate", closePayload(BUSINESS_DATE, countId), owner.session.sessionId), "conflict")
        val newCount = dispatch("command.recordPhysicalCashCount", countPayload(cashId, BUSINESS_DATE, "0"), owner.session.sessionId).getJSONObject("data").getString("countId")
        assertEquals("closed", dispatch("command.closeBusinessDate", closePayload(BUSINESS_DATE, newCount), owner.session.sessionId).getJSONObject("data").getString("statusCode"))

        val history = database.dailyCloseDao().history(BUSINESS_DATE)
        assertEquals(listOf("close", "reopen", "close"), history.map { it.action })
        assertEquals(listOf("Reopen Cashier", "Reopen Owner", "Reopen Owner"), history.map { it.actorName })
        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(91, names.size)
        assertTrue(names.containsAll(listOf("query.reconciliationArchive", "command.recordPhysicalCashCount", "command.closeBusinessDate", "command.reopenBusinessDate")))
    }

    @Test
    fun packagedCashCloseScreenCountsClosesAndReopensThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val businessDate = LocalDate.now().plusYears(20).toString()
        val marker = "Device drawer count ${System.currentTimeMillis()}"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('cash-close-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('cash-close-nav').click(); true")
            waitFor(webView, "document.getElementById('cash-count-form') !== null", 20_000L)
            setInput(webView, "cash-count-date", businessDate)
            setInput(webView, "counted-cash-rupees", "0")
            setTextArea(webView, "cash-count-notes", marker)
            evaluate(webView, "document.getElementById('save-cash-count-button').click(); true")
            waitFor(webView, "document.getElementById('reconciliation-date-$businessDate') !== null", 20_000L)
            waitFor(webView, "document.getElementById('close-date-button') !== null", 20_000L)
            if (evaluate(webView, "document.getElementById('cash-variance-reason') !== null") == "true") {
                setTextArea(webView, "cash-variance-reason", "Device test cash difference")
            }
            evaluate(webView, "document.getElementById('close-date-button').click(); true")
            waitFor(webView, "document.getElementById('business-date-status').innerText.includes('Closed and protected')", 20_000L)
            assertEquals("Instrumentation Owner", app.database.dailyCloseDao().currentAction(businessDate)!!.actorName)
            setTextArea(webView, "reopen-reason", "Device test reopens after verifying the lock")
            setInput(webView, "reopen-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('reopen-date-button').click(); true")
            waitFor(webView, "document.getElementById('business-date-status').innerText.includes('Reopened')", 20_000L)
            assertEquals("reopen", app.database.dailyCloseDao().currentAction(businessDate)!!.action)
            assertTrue(app.database.physicalCashCountDao().all().any { it.businessDate == businessDate && it.notes == marker })
            assertTrue(app.database.securityAuditDao().all().any { it.action == ReconciliationBridgeCommandService.ACTION_REOPEN_DATE && it.details?.contains(businessDate) == true })
            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            if (app.database.dailyCloseDao().currentAction(businessDate)?.action == DailyCloseService.ACTION_CLOSE) {
                DailyCloseService(app.database).reopenAuthenticated(
                    replacement.sessionId,
                    app.userSecurity,
                    VerifiedDailyReopenInput(businessDate, "Test cleanup after interrupted packaged cash-close proof"),
                )
            }
            app.currentSessionId = replacement.sessionId
        }
    }

    private fun countPayload(accountId: Long, date: String, countedPaisa: String) = JSONObject()
        .put("accountId", accountId.toString()).put("businessDate", date).put("countedCashPaisa", countedPaisa)
        .put("notes", "Drawer counted independently")

    private fun closePayload(date: String, countId: String, reason: String? = null) = JSONObject()
        .put("businessDate", date).put("physicalCashCountId", countId).apply { if (reason != null) put("varianceReason", reason) }

    private fun reopenPayload(date: String) = JSONObject().put("businessDate", date).put("reason", "Correct omitted shop work")

    private fun assertError(response: JSONObject, code: String) {
        assertFalse(response.toString(), response.getBoolean("ok"))
        assertEquals(code, response.getJSONObject("error").getString("code"))
    }

    private suspend fun dispatch(action: String, payload: JSONObject = JSONObject(), session: String? = null): JSONObject {
        val request = JSONObject().put("contractVersion", BridgeContractSnapshot.VERSION).put("requestId", UUID.randomUUID().toString()).put("action", action).put("payload", payload)
        if (session != null) request.put("sessionHandle", session)
        return JSONObject(dispatcher.dispatch(request.toString()))
    }

    private fun activityWebView(scenario: ActivityScenario<HybridActivity>): WebView {
        val holder = AtomicReference<WebView>(); scenario.onActivity { holder.set(it.webViewForTesting()) }; return holder.get()
    }

    private fun setInput(webView: WebView, id: String, value: String) = evaluate(webView, """(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('input', { bubbles: true })); return input.value; })()""")
    private fun setTextArea(webView: WebView, id: String, value: String) = evaluate(webView, """(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('input', { bubbles: true })); return input.value; })()""")
    private fun waitFor(webView: WebView, expression: String, timeoutMs: Long = 10_000L) { val deadline = SystemClock.uptimeMillis() + timeoutMs; while (SystemClock.uptimeMillis() < deadline) { if (evaluate(webView, "Boolean($expression)") == "true") return; SystemClock.sleep(100) }; throw AssertionError("Web condition did not become true: $expression") }
    private fun evaluate(webView: WebView, script: String): String? { val instrumentation = InstrumentationRegistry.getInstrumentation(); val result = AtomicReference<String?>(); val latch = CountDownLatch(1); instrumentation.runOnMainSync { webView.evaluateJavascript(script) { result.set(it); latch.countDown() } }; assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS)); return result.get() }

    companion object {
        const val BUSINESS_DATE = "2026-09-01"
        const val EARLIER_DATE = "2026-08-31"
        const val OWNER_PASSWORD = "cash reconciliation owner private passphrase 2026"
        const val CASHIER_PASSWORD = "cash reconciliation cashier private passphrase 2026"
        const val OPERATOR_PASSWORD = "cash reconciliation operator private passphrase 2026"
        suspend fun cashAccountId(database: MilkShopDatabase): Long = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
    }
}
