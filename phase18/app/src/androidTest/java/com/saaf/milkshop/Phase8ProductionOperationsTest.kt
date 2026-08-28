package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.application.bridge.OperationsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import com.saaf.milkshop.domain.YieldPolicyService
import com.saaf.milkshop.hybrid.HybridActivity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class Phase8ProductionOperationsTest {
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
    fun operatorRecordsBatchWithCanonicalStockActorAuditAndDashboard() = runBlocking {
        val owner = security.bootstrapOwner("production.owner", "Production Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("production.operator", "Production Operator", "operator"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("production.operator", OPERATOR_PASSWORD.toCharArray())
        seedMilk(20.0, BUSINESS_DATE)

        val response = dispatch("command.recordProduction", productionPayload(), operator.sessionId)

        assertTrue(response.toString(), response.getBoolean("ok"))
        val eventId = response.getJSONObject("data").getString("eventId").toLong()
        val movements = database.inventoryLogDao().findByEventId(eventId).associate { movement ->
            database.productDao().findById(movement.productId)!!.name to movement.quantity
        }
        assertEquals(-10.0, movements.getValue("Milk"), 0.000001)
        assertEquals(8.0, movements.getValue("Yogurt"), 0.000001)
        assertEquals("Production Operator", database.eventActorLinkDao().find(eventId)!!.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_PRODUCTION &&
                it.actorRoleSnapshot == "operator" && it.details == "event_id=$eventId"
        })

        val range = JSONObject().put("startDateInclusive", BUSINESS_DATE).put("endDateExclusive", "2026-08-29")
        val dashboard = dispatch("query.dashboard", range, operator.sessionId).getJSONObject("data")
        val totals = dashboard.getJSONObject("totals")
        assertEquals("10", totals.getString("productionMilkUsedLiters"))
        assertEquals("8", totals.getString("producedYogurtKg"))
        val stock = (0 until dashboard.getJSONArray("stock").length())
            .map { dashboard.getJSONArray("stock").getJSONObject(it) }
            .associate { it.getString("name") to it.getString("quantityOnHand") }
        assertEquals("10", stock.getValue("Milk"))
        assertEquals("8", stock.getValue("Yogurt"))
    }

    @Test
    fun newBatchIsAvailableForYieldReviewButDoesNotPostALossAutomatically() = runBlocking {
        val owner = security.bootstrapOwner("yield.bridge.owner", "Yield Bridge Owner", OWNER_PASSWORD.toCharArray())
        seedMilk(10.0, BUSINESS_DATE)
        val payload = productionPayload().put("milkLitersUsed", "1").put("yogurtKgProduced", "0.7")

        val response = dispatch("command.recordProduction", payload, owner.session.sessionId)
        val eventId = response.getJSONObject("data").getString("eventId").toLong()
        val assessment = YieldPolicyService(database, security).assess(eventId)

        assertEquals("calibration_pending", assessment.status)
        assertNull(assessment.policyId)
        assertNull(database.productionLossAssessmentDao().activeForEvent(eventId))
    }

    @Test
    fun productionRejectsBadQuantityInsufficientMilkClosedHistoryAndUnauthorizedCashier() = runBlocking {
        val owner = security.bootstrapOwner("reject.production.owner", "Reject Production Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("reject.production.cashier", "Reject Production Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("reject.production.cashier", CASHIER_PASSWORD.toCharArray())
        seedMilk(1.0, EARLIER_DATE)

        assertError(dispatch("command.recordProduction", productionPayload(), cashier.sessionId), "permission_denied")
        assertError(dispatch("command.recordProduction", productionPayload().put("milkLitersUsed", "0"), owner.session.sessionId), "validation_failed")
        assertError(dispatch("command.recordProduction", productionPayload().put("milkLitersUsed", "2"), owner.session.sessionId), "insufficient_stock")

        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )
        val earlier = productionPayload().put("occurredAt", EARLIER_DATE)
            .put("milkLitersUsed", "0.5").put("yogurtKgProduced", "0.4")
        assertError(dispatch("command.recordProduction", earlier, owner.session.sessionId), "day_closed")
        assertFalse(database.eventDao().findAllDescending().any { it.eventType == "PRODUCTION" })
    }

    @Test
    fun packagedDailyWorkScreenRecordsProductionThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val marker = "Device production ${System.currentTimeMillis()}"
        val productionDate = LocalDate.now().plusYears(4).toString()
        val vendorId = VendorManagementService(app.database).create("Device Production Farm ${System.currentTimeMillis()}")
        VendorPurchaseService(app.database).record(VendorPurchaseInput(vendorId, productionDate, 12.0, 4.0, 28.0))
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('operations-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('operations-nav').click(); true")
            waitFor(webView, "document.getElementById('production-work-tab') !== null", 15_000L)
            evaluate(webView, "document.getElementById('production-work-tab').click(); true")
            waitFor(webView, "document.getElementById('record-production-button') !== null")
            setInput(webView, "production-date", productionDate)
            setInput(webView, "production-milk-used", "3")
            setInput(webView, "production-yogurt-produced", "2.4")
            setTextArea(webView, "production-notes", marker)
            evaluate(webView, "document.getElementById('record-production-button').click(); true")
            waitFor(webView, "document.getElementById('production-result') !== null", 20_000L)

            val event = app.database.eventDao().findAllDescending().single { it.notes == marker }
            assertEquals("PRODUCTION", event.eventType)
            val movements = app.database.inventoryLogDao().findByEventId(event.eventId).associate { movement ->
                app.database.productDao().findById(movement.productId)!!.name to movement.quantity
            }
            assertEquals(-3.0, movements.getValue("Milk"), 0.000001)
            assertEquals(2.4, movements.getValue("Yogurt"), 0.000001)
            assertNotNull(app.database.eventActorLinkDao().find(event.eventId))
            assertTrue(evaluate(webView, "document.getElementById('production-result').innerText.includes('3 liters') && document.getElementById('production-result').innerText.includes('2.4 kg')") == "true")
            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(
                SaafMilkTestRunner.TEST_LOGIN,
                SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray(),
            )
            app.currentSessionId = replacement.sessionId
        }
    }

    private suspend fun seedMilk(liters: Double, date: String) {
        val vendorId = VendorManagementService(database).create("Production Stock Farm ${System.nanoTime()}")
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, date, liters, 4.0, 28.0))
    }

    private fun productionPayload() = JSONObject()
        .put("occurredAt", BUSINESS_DATE)
        .put("milkLitersUsed", "10")
        .put("yogurtKgProduced", "8")
        .put("notes", "Morning yogurt batch")

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
        instrumentation.runOnMainSync {
            webView.evaluateJavascript(script) { result.set(it); latch.countDown() }
        }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    companion object {
        const val BUSINESS_DATE = "2026-08-28"
        const val EARLIER_DATE = "2026-08-27"
        const val OWNER_PASSWORD = "production owner private passphrase 2026"
        const val OPERATOR_PASSWORD = "production operator private passphrase 2026"
        const val CASHIER_PASSWORD = "production cashier private passphrase 2026"
    }
}
