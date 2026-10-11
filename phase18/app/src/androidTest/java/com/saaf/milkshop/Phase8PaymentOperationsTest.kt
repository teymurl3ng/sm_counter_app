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
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.CustomerProfileService
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.VendorProfileService
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
class Phase8PaymentOperationsTest {
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
    fun managerRecordsSupplierPaymentWithExactMovementsActorAndFinancialSummary() = runBlocking {
        val owner = security.bootstrapOwner("payment.owner", "Payment Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("payment.manager", "Payment Manager", "manager"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, MANAGER_PASSWORD.toCharArray())
        val manager = security.login("payment.manager", MANAGER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Reliable Dairy")
        val cashId = cashAccountId()

        val response = dispatch(
            "command.recordVendorPayment",
            vendorPaymentPayload(vendorId, cashId),
            manager.sessionId,
        )

        assertTrue(response.toString(), response.getBoolean("ok"))
        val eventId = response.getJSONObject("data").getString("eventId").toLong()
        val movements = database.financialLogDao().findByEventId(eventId).associate { it.accountId to it.amount }
        assertEquals(-100.25, movements.getValue(vendorId), 0.000001)
        assertEquals(-100.25, movements.getValue(cashId), 0.000001)
        assertEquals("Payment Manager", database.eventActorLinkDao().find(eventId)?.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_VENDOR_PAYMENT &&
                it.actorRoleSnapshot == "manager" &&
                it.details == "event_id=$eventId;vendor_account_id=$vendorId;payment_account_id=$cashId"
        })

        val overview = dispatch("query.financialOverview", dateRange(), owner.session.sessionId).getJSONObject("data")
        assertEquals("10025", overview.getJSONObject("totals").getString("vendorPaymentsPaisa"))
        val vendor = findAccount(overview.getJSONArray("vendors"), vendorId)
        assertEquals("-10025", vendor.getString("canonicalBalancePaisa"))
    }

    @Test
    fun cashierCollectsCustomerCreditWithExactMovementsAndLimitedFinancialAccess() = runBlocking {
        val owner = security.bootstrapOwner("collection.owner", "Collection Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("collection.cashier", "Collection Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("collection.cashier", CASHIER_PASSWORD.toCharArray())
        val customerId = CustomerManagementService(database).create("Corner Cafe")
        val cashId = cashAccountId()

        val references = dispatch("query.transactionReferences", session = cashier.sessionId).getJSONObject("data")
        assertEquals(customerId.toString(), findAccount(references.getJSONArray("customers"), customerId).getString("accountId"))
        assertEquals(0, references.getJSONArray("vendors").length())
        assertError(dispatch("query.financialOverview", dateRange(), cashier.sessionId), "permission_denied")

        val response = dispatch(
            "command.recordCreditCollection",
            customerCollectionPayload(customerId, cashId),
            cashier.sessionId,
        )
        assertTrue(response.toString(), response.getBoolean("ok"))
        val eventId = response.getJSONObject("data").getString("eventId").toLong()
        val movements = database.financialLogDao().findByEventId(eventId).associate { it.accountId to it.amount }
        assertEquals(-80.50, movements.getValue(customerId), 0.000001)
        assertEquals(80.50, movements.getValue(cashId), 0.000001)
        assertEquals("Collection Cashier", database.eventActorLinkDao().find(eventId)?.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_CREDIT_COLLECTION &&
                it.actorRoleSnapshot == "cashier" &&
                it.details == "event_id=$eventId;customer_account_id=$customerId;payment_account_id=$cashId"
        })

        val summary = dispatch("query.cashierSummary", dateRange(), cashier.sessionId).getJSONObject("data")
        assertEquals("8050", summary.getString("creditCollectionsPaisa"))
        assertEquals("8050", findAccount(summary.getJSONArray("paymentAccounts"), cashId).getString("canonicalBalancePaisa"))
    }

    @Test
    fun paymentsRejectUnauthorizedInvalidInactiveAndClosedDayRequestsWithoutPosting() = runBlocking {
        val owner = security.bootstrapOwner("reject.payment.owner", "Reject Payment Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("payment.operator", "Payment Operator", "operator"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("payment.operator", OPERATOR_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Inactive Dairy")
        val customerId = CustomerManagementService(database).create("Inactive Customer")
        val cashId = cashAccountId()

        assertError(dispatch("command.recordVendorPayment", vendorPaymentPayload(vendorId, cashId), operator.sessionId), "permission_denied")
        assertError(dispatch("command.recordCreditCollection", customerCollectionPayload(customerId, cashId), operator.sessionId), "permission_denied")
        assertError(dispatch("command.recordVendorPayment", vendorPaymentPayload(vendorId, cashId).put("amountPaisa", "0"), owner.session.sessionId), "validation_failed")
        assertError(dispatch("command.recordCreditCollection", customerCollectionPayload(customerId, customerId), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordVendorPayment", vendorPaymentPayload(Long.MAX_VALUE, cashId), owner.session.sessionId), "not_found")

        VendorProfileService(database).save(vendorId, VendorProfileInput(active = false))
        CustomerProfileService(database).save(customerId, CustomerProfileInput(active = false))
        assertError(dispatch("command.recordVendorPayment", vendorPaymentPayload(vendorId, cashId), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordCreditCollection", customerCollectionPayload(customerId, cashId), owner.session.sessionId), "not_found")

        val activeVendorId = VendorManagementService(database).create("Closed Dairy")
        val activeCustomerId = CustomerManagementService(database).create("Closed Customer")
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )
        assertError(
            dispatch("command.recordVendorPayment", vendorPaymentPayload(activeVendorId, cashId).put("occurredAt", EARLIER_DATE), owner.session.sessionId),
            "day_closed",
        )
        assertError(
            dispatch("command.recordCreditCollection", customerCollectionPayload(activeCustomerId, cashId).put("occurredAt", EARLIER_DATE), owner.session.sessionId),
            "day_closed",
        )
        assertFalse(database.eventDao().findAllDescending().any {
            it.eventType == "VENDOR_PAYMENT" || it.eventType == "CREDIT_COLLECTION"
        })
    }

    @Test
    fun packagedPaymentsScreenRecordsSupplierAndCustomerMoneyThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val suffix = System.currentTimeMillis()
        val supplierMarker = "Device supplier payment $suffix"
        val customerMarker = "Device customer collection $suffix"
        val vendorName = "Device Dairy $suffix"
        val customerName = "Device Customer $suffix"
        val vendorId = VendorManagementService(app.database).create(vendorName)
        val customerId = CustomerManagementService(app.database).create(customerName)
        val cashId = app.database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val paymentDate = LocalDate.now().plusYears(6).toString()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('operations-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('operations-nav').click(); true")
            waitFor(webView, "document.getElementById('payments-work-tab') !== null", 15_000L)
            evaluate(webView, "document.getElementById('payments-work-tab').click(); true")
            waitFor(webView, "document.getElementById('record-payment-button') !== null")

            setSelect(webView, "payment-party", vendorId.toString())
            setSelect(webView, "payment-account", cashId.toString())
            setInput(webView, "payment-amount", "210.75")
            setInput(webView, "payment-date", paymentDate)
            setTextArea(webView, "payment-notes", supplierMarker)
            evaluate(webView, "document.getElementById('record-payment-button').click(); true")
            waitFor(webView, "document.getElementById('payment-result') !== null", 20_000L)
            assertTrue(evaluate(webView, "document.getElementById('payment-result').innerText.includes('Supplier payment recorded')") == "true")

            evaluate(webView, "document.getElementById('payment-mode-customer').click(); true")
            waitFor(webView, "document.getElementById('payment-party') !== null")
            setSelect(webView, "payment-party", customerId.toString())
            setSelect(webView, "payment-account", cashId.toString())
            setInput(webView, "payment-amount", "95.50")
            setInput(webView, "payment-date", paymentDate)
            setTextArea(webView, "payment-notes", customerMarker)
            evaluate(webView, "document.getElementById('record-payment-button').click(); true")
            waitFor(webView, "document.getElementById('payment-result') !== null", 20_000L)
            assertTrue(evaluate(webView, "document.getElementById('payment-result').innerText.includes('Customer collection recorded')") == "true")

            val supplierEvent = app.database.eventDao().findAllDescending().single { it.notes == supplierMarker }
            val supplierMovements = app.database.financialLogDao().findByEventId(supplierEvent.eventId).associate { it.accountId to it.amount }
            assertEquals("VENDOR_PAYMENT", supplierEvent.eventType)
            assertEquals(-210.75, supplierMovements.getValue(vendorId), 0.000001)
            assertEquals(-210.75, supplierMovements.getValue(cashId), 0.000001)
            assertNotNull(app.database.eventActorLinkDao().find(supplierEvent.eventId))

            val customerEvent = app.database.eventDao().findAllDescending().single { it.notes == customerMarker }
            val customerMovements = app.database.financialLogDao().findByEventId(customerEvent.eventId).associate { it.accountId to it.amount }
            assertEquals("CREDIT_COLLECTION", customerEvent.eventType)
            assertEquals(-95.50, customerMovements.getValue(customerId), 0.000001)
            assertEquals(95.50, customerMovements.getValue(cashId), 0.000001)
            assertNotNull(app.database.eventActorLinkDao().find(customerEvent.eventId))

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

    private fun vendorPaymentPayload(vendorId: Long, paymentAccountId: Long) = JSONObject()
        .put("vendorAccountId", vendorId.toString())
        .put("paymentAccountId", paymentAccountId.toString())
        .put("amountPaisa", "10025")
        .put("occurredAt", BUSINESS_DATE)
        .put("notes", "Supplier payment")

    private fun customerCollectionPayload(customerId: Long, paymentAccountId: Long) = JSONObject()
        .put("customerAccountId", customerId.toString())
        .put("paymentAccountId", paymentAccountId.toString())
        .put("amountPaisa", "8050")
        .put("occurredAt", BUSINESS_DATE)
        .put("notes", "Customer collection")

    private fun dateRange() = JSONObject()
        .put("startDateInclusive", BUSINESS_DATE)
        .put("endDateExclusive", "2026-08-29")

    private suspend fun cashAccountId() =
        database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

    private fun findAccount(array: org.json.JSONArray, accountId: Long): JSONObject =
        (0 until array.length()).map(array::getJSONObject).single { it.getString("accountId") == accountId.toString() }

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
        const val EARLIER_DATE = "2026-08-27"
        const val OWNER_PASSWORD = "payment owner private passphrase 2026"
        const val MANAGER_PASSWORD = "payment manager private passphrase 2026"
        const val CASHIER_PASSWORD = "payment cashier private passphrase 2026"
        const val OPERATOR_PASSWORD = "payment operator private passphrase 2026"
    }
}
