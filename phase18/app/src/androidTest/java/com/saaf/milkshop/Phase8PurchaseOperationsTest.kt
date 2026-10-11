package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
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
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class Phase8PurchaseOperationsTest {
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
    fun operatorRecordsPurchaseWithNativeCalculationStockPayableActorAndDashboard() = runBlocking {
        val owner = security.bootstrapOwner("purchase.owner", "Purchase Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("purchase.operator", "Purchase Operator", "operator"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("purchase.operator", OPERATOR_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Quality Farm")

        val response = dispatch("command.recordPurchase", purchasePayload(vendorId), operator.sessionId)

        assertTrue(response.toString(), response.getBoolean("ok"))
        val data = response.getJSONObject("data")
        val eventId = data.getString("eventId").toLong()
        assertEquals(8.6, data.getString("snfPercent").toDouble(), 0.000001)
        assertEquals(12.6, data.getString("tsPercent").toDouble(), 0.000001)
        assertEquals(19.3846153846, data.getString("adjustedLiters").toDouble(), 0.000001)
        assertEquals("351538", data.getString("amountOwedPaisa"))
        assertEquals(20.0, database.inventoryLogDao().findByEventId(eventId).single().quantity, 0.000001)
        assertEquals(3515.3846153846, database.financialLogDao().findByEventId(eventId).single().amount, 0.000001)
        assertEquals("Purchase Operator", database.eventActorLinkDao().find(eventId)!!.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_PURCHASE &&
                it.actorRoleSnapshot == "operator" && it.details?.contains("event_id=$eventId") == true
        })

        val dashboard = dispatch(
            "query.dashboard",
            JSONObject().put("startDateInclusive", BUSINESS_DATE).put("endDateExclusive", "2026-08-29"),
            operator.sessionId,
        ).getJSONObject("data")
        assertEquals("20", dashboard.getJSONObject("totals").getString("purchasedMilkLiters"))
        assertEquals("1", dashboard.getJSONObject("totals").getString("eventCount"))
        val milk = (0 until dashboard.getJSONArray("stock").length())
            .map { dashboard.getJSONArray("stock").getJSONObject(it) }
            .single { it.getString("name") == "Milk" }
        assertEquals("20", milk.getString("quantityOnHand"))
    }

    @Test
    fun purchaseRejectsBadInputMissingSupplierAndUnauthorizedCashierWithoutPosting() = runBlocking {
        val owner = security.bootstrapOwner("reject.owner", "Reject Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("reject.cashier", "Reject Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("reject.cashier", CASHIER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Valid Farm")

        val badQuantity = purchasePayload(vendorId).put("rawLiters", "0")
        assertError(dispatch("command.recordPurchase", badQuantity, owner.session.sessionId), "validation_failed")
        assertError(dispatch("command.recordPurchase", purchasePayload(Long.MAX_VALUE), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordPurchase", purchasePayload(vendorId), cashier.sessionId), "permission_denied")
        assertEquals(0, database.eventDao().count())
    }

    @Test
    fun closedBusinessDateReturnsDayClosedWithoutPosting() = runBlocking {
        val owner = security.bootstrapOwner("close.owner", "Close Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        val vendorId = VendorManagementService(database).create("Closed Day Farm")
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            session,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )

        assertError(dispatch("command.recordPurchase", purchasePayload(vendorId), session), "day_closed")
        assertEquals(0, database.eventDao().count())
    }

    @Test
    fun packagedDailyWorkScreenRecordsPurchaseThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val marker = "Device purchase ${System.currentTimeMillis()}"
        val vendorName = "Device Farm ${System.currentTimeMillis()}"
        val vendorId = VendorManagementService(app.database).create(vendorName)
        val purchaseDate = LocalDate.now().plusYears(2).toString()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('operations-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('operations-nav').click(); true")
            waitFor(webView, "document.getElementById('record-purchase-button') !== null", 15_000L)
            setSelect(webView, "purchase-supplier", vendorId.toString())
            setInput(webView, "purchase-date", purchaseDate)
            setInput(webView, "purchase-raw-liters", "14.5")
            setInput(webView, "purchase-fat-percent", "4")
            setInput(webView, "purchase-lr-reading", "28")
            setTextArea(webView, "purchase-notes", marker)
            evaluate(webView, "document.getElementById('record-purchase-button').click(); true")
            waitFor(webView, "document.getElementById('purchase-result') !== null", 20_000L)

            val event = app.database.eventDao().findAllDescending().single { it.notes == marker }
            assertEquals("PURCHASE_RECEIPT", event.eventType)
            assertEquals(14.5, app.database.inventoryLogDao().findByEventId(event.eventId).single().quantity, 0.000001)
            assertEquals(vendorId, app.database.financialLogDao().findByEventId(event.eventId).single().accountId)
            assertNotNull(app.database.eventActorLinkDao().find(event.eventId))
            assertTrue(evaluate(webView, "document.getElementById('purchase-result').innerText.includes('Amount now owed')") == "true")
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

    private fun purchasePayload(vendorId: Long) = JSONObject()
        .put("vendorAccountId", vendorId.toString())
        .put("occurredAt", BUSINESS_DATE)
        .put("rawLiters", "20")
        .put("fatPercent", "4")
        .put("lrReading", "28")
        .put("notes", "Morning collection")

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

    private fun setSelect(webView: WebView, id: String, value: String) {
        evaluate(webView, """(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('change', { bubbles: true })); return input.value; })()""")
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
        const val OWNER_PASSWORD = "purchase owner private passphrase 2026"
        const val OPERATOR_PASSWORD = "purchase operator private passphrase 2026"
        const val CASHIER_PASSWORD = "purchase cashier private passphrase 2026"
    }
}
