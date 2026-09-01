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
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.VendorProfileService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
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
import kotlin.math.roundToLong

@RunWith(AndroidJUnit4::class)
class Phase8AccountLedgerTest {
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
    fun customerLedgerReturnsCanonicalMovementsRunningBalancesAndPlainName() = runBlocking {
        val owner = security.bootstrapOwner("ledger.owner", "Ledger Owner", OWNER_PASSWORD.toCharArray())
        val customerId = CustomerManagementService(database).create("Corner Cafe")
        val milkId = seedMilk(database, BUSINESS_DATE)
        val sale = SaleService(database).record(
            SaleInput(milkId, BUSINESS_DATE, "credit-retail", 200.0, 2.0, payment = SalePayment.OnCredit(customerId), notes = "Morning milk"),
        )
        val cashId = cashAccountId(database)
        val collection = CreditCollectionService(database).record(
            CreditCollectionInput(customerId, cashId, 150.0, BUSINESS_DATE, "Part payment"),
        )

        val response = dispatch("query.accountLedger", ledgerPayload(customerId), owner.session.sessionId)

        assertTrue(response.toString(), response.getBoolean("ok"))
        val ledger = response.getJSONObject("data")
        assertEquals(customerId.toString(), ledger.getString("accountId"))
        assertEquals("Customer: Corner Cafe", ledger.getString("accountName"))
        assertEquals("Corner Cafe", ledger.getString("displayName"))
        assertEquals("Customer", ledger.getString("categoryCode"))
        assertEquals("25000", ledger.getString("currentBalancePaisa"))
        val entries = ledger.getJSONArray("entries")
        assertEquals(2, entries.length())
        assertLedgerEntry(entries.getJSONObject(0), sale.eventId, "SALE", "40000", "40000")
        assertLedgerEntry(entries.getJSONObject(1), collection.eventId, "CREDIT_COLLECTION", "-15000", "25000")
    }

    @Test
    fun supplierLedgerRetainsInactiveHistoryAndUsesCanonicalPurchaseAmount() = runBlocking {
        val owner = security.bootstrapOwner("supplier.ledger.owner", "Supplier Ledger Owner", OWNER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Reliable Dairy")
        val purchase = VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, BUSINESS_DATE, 10.0, 4.0, 28.0, "Morning receipt"),
        )
        val cashId = cashAccountId(database)
        val payment = VendorPaymentService(database).record(
            VendorPaymentInput(vendorId, cashId, 100.25, BUSINESS_DATE, "Part settlement"),
        )
        VendorProfileService(database).save(vendorId, VendorProfileInput(active = false))

        val ledger = dispatch("query.accountLedger", ledgerPayload(vendorId), owner.session.sessionId).getJSONObject("data")
        val purchasePaisa = (purchase.amountOwed * 100.0).roundToLong()
        assertEquals("Reliable Dairy", ledger.getString("displayName"))
        assertEquals("Vendor", ledger.getString("categoryCode"))
        assertEquals((purchasePaisa - 10025L).toString(), ledger.getString("currentBalancePaisa"))
        val entries = ledger.getJSONArray("entries")
        assertEquals(2, entries.length())
        assertLedgerEntry(entries.getJSONObject(0), purchase.eventId, "PURCHASE_RECEIPT", purchasePaisa.toString(), purchasePaisa.toString())
        assertLedgerEntry(entries.getJSONObject(1), payment.eventId, "VENDOR_PAYMENT", "-10025", (purchasePaisa - 10025L).toString())
    }

    @Test
    fun ledgerRejectsUnauthorizedMissingAndNonPartyAccountsAndAdvertisesOneAction() = runBlocking {
        val owner = security.bootstrapOwner("reject.ledger.owner", "Reject Ledger Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("ledger.cashier", "Ledger Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("ledger.cashier", CASHIER_PASSWORD.toCharArray())
        val customerId = CustomerManagementService(database).create("Restricted Customer")

        assertError(dispatch("query.accountLedger"), "unauthenticated")
        assertError(dispatch("query.accountLedger", JSONObject(), owner.session.sessionId), "invalid_request")
        assertError(dispatch("query.accountLedger", ledgerPayload(customerId), cashier.sessionId), "permission_denied")
        assertError(dispatch("query.accountLedger", ledgerPayload(Long.MAX_VALUE), owner.session.sessionId), "not_found")
        assertError(dispatch("query.accountLedger", ledgerPayload(cashAccountId(database)), owner.session.sessionId), "not_found")

        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(55, names.size)
        assertTrue(names.contains("query.accountLedger"))
    }

    @Test
    fun packagedAccountsScreenShowsHistoryAndRecordsAContextualPayment() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val suffix = System.currentTimeMillis()
        val customerName = "Device Ledger Customer $suffix"
        val marker = "Device ledger collection $suffix"
        val customerId = CustomerManagementService(app.database).create(customerName)
        val businessDate = LocalDate.now().plusYears(9).toString()
        val milkId = seedMilk(app.database, businessDate)
        SaleService(app.database).record(
            SaleInput(milkId, businessDate, "credit-retail", 200.0, 2.0, payment = SalePayment.OnCredit(customerId), notes = "Device credit sale $suffix"),
        )
        val cashId = cashAccountId(app.database)
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('accounts-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('accounts-nav').click(); true")
            waitFor(webView, "document.getElementById('ledger-account-$customerId') !== null", 20_000L)
            evaluate(webView, "document.getElementById('ledger-account-$customerId').click(); true")
            waitFor(webView, "document.getElementById('account-ledger-detail') !== null", 20_000L)
            assertTrue(evaluate(webView, "document.getElementById('account-ledger-detail').innerText.includes(${JSONObject.quote(customerName)})") == "true")
            assertTrue(evaluate(webView, "document.getElementById('account-ledger-detail').innerText.includes('Customer owes shop')") == "true")
            assertTrue(evaluate(webView, "document.getElementById('account-ledger-entries').innerText.includes('Credit sale')") == "true")

            evaluate(webView, "document.getElementById('account-payment-$customerId').click(); true")
            waitFor(webView, "document.getElementById('record-payment-button') !== null")
            assertTrue(evaluate(webView, "document.getElementById('payment-party').value === '$customerId'") == "true")
            setSelect(webView, "payment-account", cashId.toString())
            setInput(webView, "payment-amount", "50.25")
            setInput(webView, "payment-date", businessDate)
            setTextArea(webView, "payment-notes", marker)
            evaluate(webView, "document.getElementById('record-payment-button').click(); true")
            waitFor(webView, "document.getElementById('payment-result') !== null", 20_000L)
            evaluate(webView, "document.getElementById('accounts-history-tab').click(); true")
            waitFor(webView, "document.getElementById('account-ledger-entries').innerText.includes(${JSONObject.quote(marker)})", 20_000L)
            assertTrue(evaluate(webView, "document.getElementById('account-ledger-entries').innerText.includes('Customer payment received')") == "true")

            val event = app.database.eventDao().findAllDescending().single { it.notes == marker }
            val movement = app.database.financialLogDao().findByEventId(event.eventId).associate { it.accountId to it.amount }
            assertEquals("CREDIT_COLLECTION", event.eventType)
            assertEquals(-50.25, movement.getValue(customerId), 0.000001)
            assertEquals(50.25, movement.getValue(cashId), 0.000001)
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

    private fun assertLedgerEntry(entry: JSONObject, eventId: Long, type: String, movement: String, balance: String) {
        assertEquals(eventId.toString(), entry.getString("eventId"))
        assertEquals(type, entry.getString("eventTypeCode"))
        assertEquals(movement, entry.getString("movementPaisa"))
        assertEquals(balance, entry.getString("balanceAfterPaisa"))
        assertFalse(entry.getBoolean("isCorrection"))
        assertFalse(entry.getBoolean("isSuperseded"))
    }

    private fun ledgerPayload(accountId: Long) = JSONObject().put("accountId", accountId.toString())

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
        const val BUSINESS_DATE = "2026-08-31"
        const val OWNER_PASSWORD = "account ledger owner private passphrase 2026"
        const val CASHIER_PASSWORD = "account ledger cashier private passphrase 2026"

        suspend fun seedMilk(database: MilkShopDatabase, date: String): Long {
            val vendorId = VendorManagementService(database).create("Ledger Stock Farm ${System.nanoTime()}")
            VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, date, 20.0, 4.0, 28.0))
            return database.productDao().findByName("Milk")!!.productId
        }

        suspend fun cashAccountId(database: MilkShopDatabase): Long =
            database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
    }
}
