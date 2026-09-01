package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.hybrid.HybridActivity
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase8DashboardReportsTest {
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
    fun ownerReportsUseCanonicalOperationalFinancialAndCostingReads() = runBlocking {
        val owner = security.bootstrapOwner("reports.owner", "Reports Owner", OWNER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Reports Dairy")
        val cashId = cashAccountId(database)
        val milk = database.productDao().findByName("Milk")!!
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, BUSINESS_DATE, 100.0, 4.0, 28.0))
        SaleService(database).record(
            SaleInput(
                milk.productId,
                BUSINESS_DATE,
                "retail",
                190.0,
                quantity = 10.0,
                payment = SalePayment.Account(cashId),
                surchargeEnabled = true,
            ),
        )

        val dashboard = dispatch("query.dashboard", standardRange(), owner.session.sessionId).getJSONObject("data")
        assertEquals("100", dashboard.getJSONObject("totals").getString("purchasedMilkLiters"))
        assertEquals("10", dashboard.getJSONObject("totals").getString("saleQuantity"))
        val cashier = dispatch("query.cashierSummary", standardRange(), owner.session.sessionId).getJSONObject("data")
        assertEquals("190000", cashier.getString("salesValuePaisa"))
        val overview = dispatch("query.financialOverview", standardRange(), owner.session.sessionId).getJSONObject("data")
        assertEquals("190000", overview.getJSONObject("totals").getString("salesValuePaisa"))

        val report = dispatch("query.profitabilityReport", profitabilityRange(), owner.session.sessionId).getJSONObject("data")
        val product = report.getJSONArray("products").getJSONObject(0)
        assertEquals("Milk", product.getString("productName"))
        assertEquals("175769", product.getString("costOfProductsSoldPaisa"))
        assertEquals("14231", product.getString("profitBeforeShopExpensesPaisa"))
        assertEquals("5000", report.getString("deliverySurchargePaisa"))
        assertEquals("19231", report.getString("estimatedRecordedResultPaisa"))
        assertEquals("complete", report.getString("coverageCode"))
        assertEquals("cost_based_management", report.getString("reportingBasisCode"))
        assertEquals("cost_only_no_nrv", report.getString("valuationScopeCode"))
    }

    @Test
    fun missingOpeningCostIsReportedAsUnavailableAndSensitiveReportsStayProtected() = runBlocking {
        val owner = security.bootstrapOwner("limited.owner", "Limited Reports Owner", OWNER_PASSWORD.toCharArray())
        val cashierPending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("limited.cashier", "Limited Cashier", "cashier"))
        security.completeUserEnrollment(cashierPending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val operatorPending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("limited.operator", "Limited Operator", "operator"))
        security.completeUserEnrollment(operatorPending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val cashier = security.login("limited.cashier", CASHIER_PASSWORD.toCharArray())
        val operator = security.login("limited.operator", OPERATOR_PASSWORD.toCharArray())
        StoreIdentityService(database).save(StoreIdentityInput("Reports Test Shop"))
        val milk = database.productDao().findByName("Milk")!!
        OpeningStateService(database).record(
            OpeningStateInput(BUSINESS_DATE, inventoryBalances = listOf(OpeningInventoryBalance(milk.productId, 10.0))),
        )
        SaleService(database).record(
            SaleInput(milk.productId, BUSINESS_DATE, "retail", 200.0, quantity = 2.0, payment = SalePayment.Account(cashAccountId(database))),
        )

        val report = dispatch("query.profitabilityReport", profitabilityRange(), owner.session.sessionId).getJSONObject("data")
        val product = report.getJSONArray("products").getJSONObject(0)
        assertEquals("missing_basis", report.getString("coverageCode"))
        assertTrue(report.isNull("estimatedRecordedResultPaisa"))
        assertTrue(product.isNull("costOfProductsSoldPaisa"))
        assertTrue(dispatch("query.dashboard", standardRange(), operator.sessionId).getBoolean("ok"))
        assertTrue(dispatch("query.cashierSummary", standardRange(), cashier.sessionId).getBoolean("ok"))
        assertError(dispatch("query.financialOverview", standardRange(), cashier.sessionId), "permission_denied")
        assertError(dispatch("query.profitabilityReport", profitabilityRange(), cashier.sessionId), "permission_denied")
        assertError(dispatch("query.cashierSummary", standardRange(), operator.sessionId), "permission_denied")
    }

    @Test
    fun profitabilityBridgeRejectsInvalidRangesAndAdvertisesRuntimeSupport() = runBlocking {
        val owner = security.bootstrapOwner("range.owner", "Range Owner", OWNER_PASSWORD.toCharArray())
        val reversed = JSONObject().put("startInclusive", "2026-09-02").put("endExclusive", "2026-09-01")
        assertError(dispatch("query.profitabilityReport", reversed, owner.session.sessionId), "invalid_request")
        assertError(dispatch("query.profitabilityReport", profitabilityRange().put("extra", true), owner.session.sessionId), "invalid_request")
        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(56, names.size)
        assertTrue(names.contains("query.profitabilityReport"))
    }

    @Test
    fun packagedDashboardExposesReportWorkspaceAndDateControls() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('dashboard-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('dashboard-nav').click(); true")
            waitFor(webView, "document.getElementById('run-report-button') !== null", 20_000L)
            val pageText = evaluate(webView, "document.body.innerText")
            assertTrue(pageText, pageText?.contains("Dashboard") == true)
            assertEquals("true", evaluate(webView, "document.getElementById('report-from-date') !== null && document.getElementById('report-through-date') !== null && document.getElementById('run-report-button') !== null"))
            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
    }

    private fun standardRange() = JSONObject().put("startDateInclusive", BUSINESS_DATE).put("endDateExclusive", END_DATE)
    private fun profitabilityRange() = JSONObject().put("startInclusive", BUSINESS_DATE).put("endExclusive", END_DATE)

    private fun assertError(response: JSONObject, code: String) {
        assertFalse(response.toString(), response.getBoolean("ok"))
        assertEquals(code, response.getJSONObject("error").getString("code"))
    }

    private suspend fun dispatch(action: String, payload: JSONObject = JSONObject(), session: String? = null): JSONObject {
        val request = JSONObject().put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("requestId", UUID.randomUUID().toString()).put("action", action).put("payload", payload)
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
        instrumentation.runOnMainSync {
            webView.evaluateJavascript(script) { result.set(it); latch.countDown() }
        }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    companion object {
        const val BUSINESS_DATE = "2026-08-27"
        const val END_DATE = "2026-08-28"
        const val OWNER_PASSWORD = "dashboard reports owner private passphrase 2026"
        const val CASHIER_PASSWORD = "dashboard reports cashier private passphrase 2026"
        const val OPERATOR_PASSWORD = "dashboard reports operator private passphrase 2026"
        suspend fun cashAccountId(database: MilkShopDatabase): Long = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
    }
}
