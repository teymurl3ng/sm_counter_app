package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HistoryBridgeCommandService
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SpoilageInput
import com.saaf.milkshop.domain.SpoilageService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionService
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
class Phase8HistoryCorrectionTest {
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
    fun typedCorrectionsUseExistingNativePurchaseProductionSaleAndSpoilageRules() = runBlocking {
        val owner = security.bootstrapOwner("typed.correction.owner", "Typed Correction Owner", OWNER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("Typed Correction Dairy")
        val purchase = VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, BUSINESS_DATE, 100.0, 4.0, 28.0))
        val milkId = database.productDao().findByName("Milk")!!.productId
        val production = YogurtProductionService(database).record(YogurtProductionInput(BUSINESS_DATE, 10.0, 8.0))
        val cashId = cashAccountId(database)
        val sale = SaleService(database).record(SaleInput(milkId, BUSINESS_DATE, "retail", 200.0, quantity = 5.0, payment = SalePayment.Account(cashId)))
        val spoilage = SpoilageService(database).record(SpoilageInput(milkId, 2.0, BUSINESS_DATE, "spill"))

        val purchaseCorrection = correctedEvent(dispatch("command.correctEvent", purchasePayload(purchase.eventId), owner.session.sessionId))
        val productionCorrection = correctedEvent(dispatch("command.correctEvent", productionPayload(production.eventId), owner.session.sessionId))
        assertError(dispatch("command.correctEvent", salePayload(sale.eventId).put("quantity", "1000"), owner.session.sessionId), "insufficient_stock")
        val saleCorrection = correctedEvent(dispatch("command.correctEvent", salePayload(sale.eventId), owner.session.sessionId))
        val spoilageCorrection = correctedEvent(dispatch("command.correctEvent", spoilagePayload(spoilage.eventId), owner.session.sessionId))

        assertEquals(purchase.eventId, purchaseCorrection.correctsEventId)
        assertEquals(4.2, purchaseCorrection.fatPercentage!!, 0.0)
        assertEquals(production.eventId, productionCorrection.correctsEventId)
        assertEquals(9.0, productionCorrection.milkLitersUsed!!, 0.0)
        assertEquals(sale.eventId, saleCorrection.correctsEventId)
        assertEquals(3.0, saleCorrection.saleQuantity!!, 0.0)
        assertEquals(600.0, saleCorrection.saleValue!!, 0.000001)
        assertEquals(spoilage.eventId, spoilageCorrection.correctsEventId)
        assertEquals(1.0, database.spoilageDao().findByEventId(spoilageCorrection.eventId)!!.quantity, 0.0)
    }

    @Test
    fun historyAndDetailExposeActorStockAndRoleFilteredMoney() = runBlocking {
        val owner = security.bootstrapOwner("history.owner", "History Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("history.cashier", "History Cashier", "cashier"))
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("history.cashier", CASHIER_PASSWORD.toCharArray())
        val vendorId = VendorManagementService(database).create("History Dairy")
        val response = dispatch("command.recordPurchase", JSONObject()
            .put("vendorAccountId", vendorId.toString()).put("occurredAt", BUSINESS_DATE)
            .put("rawLiters", "10").put("fatPercent", "4").put("lrReading", "28").put("notes", "History proof"), owner.session.sessionId)
        val eventId = response.getJSONObject("data").getString("eventId").toLong()

        val history = dispatch("query.eventHistory", session = cashier.sessionId).getJSONArray("data")
        val item = (0 until history.length()).map(history::getJSONObject).single { it.getString("eventId") == eventId.toString() }
        assertEquals("PURCHASE_RECEIPT", item.getString("eventTypeCode"))
        assertEquals("History Owner", item.getJSONObject("actor").getString("displayName"))
        val cashierDetail = dispatch("query.eventDetail", detailPayload(eventId), cashier.sessionId).getJSONObject("data")
        assertTrue(cashierDetail.isNull("financialMovements"))
        assertEquals("10", cashierDetail.getJSONArray("inventoryMovements").getJSONObject(0).getString("quantity"))
        val ownerDetail = dispatch("query.eventDetail", detailPayload(eventId), owner.session.sessionId).getJSONObject("data")
        assertEquals("History Dairy", ownerDetail.getJSONArray("financialMovements").getJSONObject(0).getString("accountName"))
    }

    @Test
    fun correctionsRejectUnauthorizedInvalidAndForkedRequestsAndAdvertiseThreeActions() = runBlocking {
        val owner = security.bootstrapOwner("guard.correction.owner", "Guard Correction Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("guard.cashier", "Guard Cashier", "cashier"))
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("guard.cashier", CASHIER_PASSWORD.toCharArray())
        val original = ExpenseService(database).record(ExpenseInput("Fuel", cashAccountId(database), 100.0, BUSINESS_DATE))
        val valid = monetaryPayload(original.eventId, "75.00")

        assertError(dispatch("query.eventHistory"), "unauthenticated")
        assertError(dispatch("query.eventDetail", detailPayload(Long.MAX_VALUE), owner.session.sessionId), "not_found")
        assertError(dispatch("command.correctEvent", valid, cashier.sessionId), "permission_denied")
        assertError(dispatch("command.correctEvent", JSONObject(valid.toString()).put("quantity", "1"), owner.session.sessionId), "invalid_request")
        assertError(dispatch("command.correctEvent", salePayload(original.eventId), owner.session.sessionId), "validation_failed")
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashAccountId(database), EARLIER_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(EARLIER_DATE, count.count.countId, "Fixture cash variance for closed-day correction guard"),
        )
        assertError(dispatch("command.correctEvent", JSONObject(valid.toString()).put("occurredAt", EARLIER_DATE), owner.session.sessionId), "day_closed")
        val correctedId = dispatch("command.correctEvent", valid, owner.session.sessionId).getJSONObject("data").getString("eventId").toLong()
        assertEquals(75.0, database.eventDao().findById(correctedId)!!.amount!!, 0.0)
        assertError(dispatch("command.correctEvent", valid, owner.session.sessionId), "conflict")
        assertEquals("Guard Correction Owner", database.eventActorLinkDao().find(correctedId)!!.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any { it.action == HistoryBridgeCommandService.ACTION_CORRECT_EVENT && it.details?.contains("original_event_id=${original.eventId}") == true })

        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(65, names.size)
        assertTrue(names.containsAll(listOf("query.eventHistory", "query.eventDetail", "command.correctEvent")))
    }

    @Test
    fun packagedHistoryScreenShowsDetailAndCorrectsExpenseThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val suffix = System.currentTimeMillis()
        val originalMarker = "Device correction original $suffix"
        val correctionMarker = "Device correction saved $suffix"
        val businessDate = LocalDate.now().plusYears(10).toString()
        val cashId = cashAccountId(app.database)
        val original = ExpenseService(app.database).record(ExpenseInput("Fuel", cashId, 100.0, businessDate, originalMarker))
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('history-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('history-nav').click(); true")
            waitFor(webView, "document.getElementById('history-event-${original.eventId}') !== null", 20_000L)
            evaluate(webView, "document.getElementById('history-event-${original.eventId}').click(); true")
            waitFor(webView, "document.getElementById('history-detail') !== null", 20_000L)
            assertTrue(evaluate(webView, "document.getElementById('history-detail').innerText.includes('Shop expense')") == "true")
            assertTrue(evaluate(webView, "document.getElementById('history-detail').innerText.includes('PKR 100.00')") == "true")
            evaluate(webView, "document.getElementById('correct-event-${original.eventId}').click(); true")
            waitFor(webView, "document.getElementById('correction-form') !== null")
            setInput(webView, "correction-amount", "75.00")
            setTextArea(webView, "correction-notes", correctionMarker)
            evaluate(webView, "document.getElementById('save-correction-button').click(); true")
            waitFor(webView, "document.getElementById('history-detail').innerText.includes('Correction of #${original.eventId}')", 20_000L)
            assertTrue(evaluate(webView, "document.getElementById('history-detail').innerText.includes(${JSONObject.quote(correctionMarker)})") == "true")

            val correction = app.database.eventDao().findAllDescending().single { it.notes == correctionMarker }
            assertEquals(original.eventId, correction.correctsEventId)
            assertEquals(75.0, correction.amount!!, 0.0)
            val movements = app.database.financialLogDao().findByEventId(correction.eventId)
            assertEquals(25.0, movements.single { it.accountId == cashId }.amount, 0.000001)
            assertEquals("Instrumentation Owner", app.database.eventActorLinkDao().find(correction.eventId)!!.actorNameSnapshot)
            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
    }

    private suspend fun correctedEvent(response: JSONObject) = database.eventDao().findById(response.getJSONObject("data").getString("eventId").toLong())!!
    private fun detailPayload(eventId: Long) = JSONObject().put("eventId", eventId.toString())
    private fun common(type: String, eventId: Long) = JSONObject().put("correctionTypeCode", type).put("originalEventId", eventId.toString()).put("occurredAt", BUSINESS_DATE)
    private fun purchasePayload(eventId: Long) = common("purchase", eventId).put("rawLiters", "100").put("fatPercent", "4.2").put("lrReading", "28")
    private fun productionPayload(eventId: Long) = common("production", eventId).put("milkLitersUsed", "9").put("yogurtKgProduced", "7.5")
    private fun salePayload(eventId: Long) = common("sale", eventId).put("unitRatePaisa", "20000").put("quantity", "3").put("surchargePaisa", "0").put("channelCode", "retail")
    private fun spoilagePayload(eventId: Long) = common("spoilage", eventId).put("quantity", "1").put("reasonCode", "spill")
    private fun monetaryPayload(eventId: Long, amount: String) = common("monetary", eventId).put("amountPaisa", amount.replace(".", ""))

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
        const val OWNER_PASSWORD = "history correction owner private passphrase 2026"
        const val CASHIER_PASSWORD = "history correction cashier private passphrase 2026"
        suspend fun cashAccountId(database: MilkShopDatabase): Long = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
    }
}
