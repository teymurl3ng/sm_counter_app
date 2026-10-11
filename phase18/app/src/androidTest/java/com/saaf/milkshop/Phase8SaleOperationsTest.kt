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
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
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
class Phase8SaleOperationsTest {
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
    fun cashierRecordsQuantitySaleToCashWithNativeDefaultSurchargeAndSummaries() = runBlocking {
        val owner = security.bootstrapOwner("sale.owner", "Sale Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("sale.cashier", "Sale Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("sale.cashier", CASHIER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Sale Stock Farm")
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, BUSINESS_DATE, 20.0, 4.0, 28.0))
        val milkId = database.productDao().findByName("Milk")!!.productId
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        val response = dispatch(
            "command.recordSale",
            salePayload(milkId, cashId = cashId).put("surchargeEnabled", true),
            cashier.sessionId,
        )

        assertTrue(response.toString(), response.getBoolean("ok"))
        val data = response.getJSONObject("data")
        val eventId = data.getString("eventId").toLong()
        assertEquals("2", data.getString("quantity"))
        assertEquals("38000", data.getString("valuePaisa"))
        assertEquals("5000", data.getString("surchargePaisa"))
        assertEquals("43000", data.getString("totalPaisa"))
        assertEquals(-2.0, database.inventoryLogDao().findByEventId(eventId).single().quantity, 0.000001)
        assertEquals(430.0, database.financialLogDao().findByEventId(eventId).single().amount, 0.000001)
        assertEquals(cashId, database.financialLogDao().findByEventId(eventId).single().accountId)
        assertEquals("retail", database.eventDao().findById(eventId)!!.channel)
        assertEquals("Sale Cashier", database.eventActorLinkDao().find(eventId)!!.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_SALE && it.actorRoleSnapshot == "cashier"
        })

        val range = JSONObject().put("startDateInclusive", BUSINESS_DATE).put("endDateExclusive", "2026-08-29")
        val summary = dispatch("query.cashierSummary", range, cashier.sessionId).getJSONObject("data")
        // The cashier total includes the posted surcharge, matching the canonical cash movement.
        assertEquals("43000", summary.getString("salesValuePaisa"))
        val cash = (0 until summary.getJSONArray("paymentAccounts").length())
            .map { summary.getJSONArray("paymentAccounts").getJSONObject(it) }
            .single { it.getString("name") == "Cash" }
        assertEquals("43000", cash.getString("canonicalBalancePaisa"))
        val dashboard = dispatch("query.dashboard", range, cashier.sessionId).getJSONObject("data")
        assertEquals("2", dashboard.getJSONObject("totals").getString("saleQuantity"))
    }

    @Test
    fun ownerRecordsValueSaleOnCustomerCreditAndNativeDerivesQuantity() = runBlocking {
        val owner = security.bootstrapOwner("credit.owner", "Credit Owner", OWNER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Credit Stock Farm")
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, BUSINESS_DATE, 10.0, 4.0, 28.0))
        val customerId = CustomerManagementService(database).create("Neighbour Shop")
        val milkId = database.productDao().findByName("Milk")!!.productId
        val payload = salePayload(milkId, customerId = customerId)
            .put("channelCode", "credit-wholesale")
            .put("unitRatePaisa", "20000")
            .put("quantity", JSONObject.NULL)
            .put("valuePaisa", "50000")
            .put("paymentKindCode", "credit")

        val response = dispatch("command.recordSale", payload, owner.session.sessionId)

        assertTrue(response.toString(), response.getBoolean("ok"))
        val data = response.getJSONObject("data")
        assertEquals("2.5", data.getString("quantity"))
        assertEquals("50000", data.getString("totalPaisa"))
        val eventId = data.getString("eventId").toLong()
        assertEquals(customerId, database.financialLogDao().findByEventId(eventId).single().accountId)
        assertEquals(500.0, database.financialLogDao().findByEventId(eventId).single().amount, 0.000001)
        val references = dispatch("query.transactionReferences", session = owner.session.sessionId).getJSONObject("data")
        assertTrue((0 until references.getJSONArray("customers").length()).map {
            references.getJSONArray("customers").getJSONObject(it).getString("name")
        }.contains("Neighbour Shop"))
    }

    @Test
    fun saleRejectsInsufficientStockClosedHistoryInvalidChannelAndUnauthorizedOperator() = runBlocking {
        val owner = security.bootstrapOwner("reject.sale.owner", "Reject Sale Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("reject.sale.operator", "Reject Sale Operator", "operator"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("reject.sale.operator", OPERATOR_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Small Stock Farm")
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, EARLIER_DATE, 1.0, 4.0, 28.0))
        val milkId = database.productDao().findByName("Milk")!!.productId
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        assertError(dispatch("command.recordSale", salePayload(milkId, cashId = cashId), operator.sessionId), "permission_denied")
        assertError(dispatch("command.recordSale", salePayload(milkId, cashId = cashId).put("quantity", "2"), owner.session.sessionId), "insufficient_stock")
        assertError(dispatch("command.recordSale", salePayload(milkId, cashId = cashId).put("channelCode", "cash_retail"), owner.session.sessionId), "validation_failed")

        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )
        val earlierSale = salePayload(milkId, cashId = cashId)
            .put("occurredAt", EARLIER_DATE)
            .put("quantity", "0.5")
        assertError(dispatch("command.recordSale", earlierSale, owner.session.sessionId), "day_closed")
        assertFalse(database.eventDao().findAllDescending().any { it.eventType == "SALE" })
    }

    @Test
    fun packagedDailyWorkScreenRecordsPaidSaleThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val marker = "Device sale ${System.currentTimeMillis()}"
        val vendorId = VendorManagementService(app.database).create("Device Sale Farm ${System.currentTimeMillis()}")
        val saleDate = LocalDate.now().plusYears(3).toString()
        VendorPurchaseService(app.database).record(VendorPurchaseInput(vendorId, saleDate, 10.0, 4.0, 28.0))
        val milkId = app.database.productDao().findByName("Milk")!!.productId
        val cashId = app.database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('operations-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('operations-nav').click(); true")
            waitFor(webView, "document.getElementById('sale-work-tab') !== null", 15_000L)
            evaluate(webView, "document.getElementById('sale-work-tab').click(); true")
            waitFor(webView, "document.getElementById('record-sale-button') !== null")
            setSelect(webView, "sale-product", milkId.toString())
            setInput(webView, "sale-date", saleDate)
            setInput(webView, "sale-unit-rate", "200")
            setInput(webView, "sale-quantity", "1.25")
            setSelect(webView, "sale-payment-account-select", cashId.toString())
            setTextArea(webView, "sale-notes", marker)
            evaluate(webView, "document.getElementById('record-sale-button').click(); true")
            waitFor(webView, "document.getElementById('sale-result') !== null", 20_000L)

            val event = app.database.eventDao().findAllDescending().single { it.notes == marker }
            assertEquals("SALE", event.eventType)
            assertEquals(-1.25, app.database.inventoryLogDao().findByEventId(event.eventId).single().quantity, 0.000001)
            assertEquals(250.0, app.database.financialLogDao().findByEventId(event.eventId).single().amount, 0.000001)
            assertEquals(cashId, app.database.financialLogDao().findByEventId(event.eventId).single().accountId)
            assertNotNull(app.database.eventActorLinkDao().find(event.eventId))
            assertTrue(evaluate(webView, "document.getElementById('sale-result').innerText.includes('PKR 250.00')") == "true")
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

    private fun salePayload(productId: Long, cashId: Long? = null, customerId: Long? = null) = JSONObject()
        .put("productId", productId.toString())
        .put("occurredAt", BUSINESS_DATE)
        .put("channelCode", "retail")
        .put("unitRatePaisa", "19000")
        .put("quantity", "2")
        .put("valuePaisa", JSONObject.NULL)
        .put("paymentKindCode", "account")
        .put("paymentAccountId", cashId?.toString() ?: JSONObject.NULL)
        .put("customerAccountId", customerId?.toString() ?: JSONObject.NULL)
        .put("surchargeEnabled", false)
        .put("surchargePaisa", JSONObject.NULL)
        .put("notes", "Recorded sale")

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
        throw AssertionError("Web condition did not become true: $expression; visible page: " + evaluate(webView, "document.body.innerText.slice(0, 1500)"))
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
        const val OWNER_PASSWORD = "sale owner private passphrase 2026"
        const val CASHIER_PASSWORD = "sale cashier private passphrase 2026"
        const val OPERATOR_PASSWORD = "sale operator private passphrase 2026"
    }
}
