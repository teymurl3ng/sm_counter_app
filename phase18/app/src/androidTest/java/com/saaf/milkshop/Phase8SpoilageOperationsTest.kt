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
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
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
class Phase8SpoilageOperationsTest {
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
    fun operatorRecordsSpoilageWithExactStockActorAuditAndReadModels() = runBlocking {
        val owner = security.bootstrapOwner("spoilage.owner", "Spoilage Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("spoilage.operator", "Spoilage Operator", "operator"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("spoilage.operator", OPERATOR_PASSWORD.toCharArray())
        seedMilk(database, 100.0, BUSINESS_DATE)
        val milkId = milkProductId(database)

        val response = dispatch("command.recordSpoilage", spoilagePayload(milkId), operator.sessionId)

        assertTrue(response.toString(), response.getBoolean("ok"))
        val eventId = response.getJSONObject("data").getString("eventId").toLong()
        val event = database.eventDao().findById(eventId)!!
        val detail = database.spoilageDao().findByEventId(eventId)!!
        val inventory = database.inventoryLogDao().findByEventId(eventId).single()
        assertEquals("SPOILAGE", event.eventType)
        assertEquals("Spilled during transfer", event.notes)
        assertEquals(milkId, detail.productId)
        assertEquals(3.25, detail.quantity, 0.000001)
        assertEquals("spill", detail.reason)
        assertEquals(milkId, inventory.productId)
        assertEquals(-3.25, inventory.quantity, 0.000001)
        assertEquals("Spoilage Operator", database.eventActorLinkDao().find(eventId)?.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_SPOILAGE &&
                it.actorRoleSnapshot == "operator" &&
                it.details == "event_id=$eventId;product_id=$milkId"
        })

        val history = dispatch("query.spoilageHistory", session = operator.sessionId).getJSONArray("data")
        assertEquals(1, history.length())
        assertEquals(eventId.toString(), history.getJSONObject(0).getString("eventId"))
        assertEquals("Milk", history.getJSONObject(0).getString("productName"))
        assertEquals("3.25", history.getJSONObject(0).getString("quantity"))
        assertEquals("spill", history.getJSONObject(0).getString("reasonCode"))

        val dashboard = dispatch("query.dashboard", dateRange(), operator.sessionId).getJSONObject("data")
        val milkStock = (0 until dashboard.getJSONArray("stock").length())
            .map(dashboard.getJSONArray("stock")::getJSONObject)
            .single { it.getString("productId") == milkId.toString() }
        assertEquals("96.75", milkStock.getString("quantityOnHand"))
    }

    @Test
    fun spoilageHistoryIsRoleProtectedAndRuntimeAdvertisesBothActions() = runBlocking {
        val owner = security.bootstrapOwner("spoilage.roles.owner", "Spoilage Roles Owner", OWNER_PASSWORD.toCharArray())
        val cashierPending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("spoilage.cashier", "Spoilage Cashier", "cashier"),
        )
        security.completeUserEnrollment(cashierPending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("spoilage.cashier", CASHIER_PASSWORD.toCharArray())
        seedMilk(database, 10.0, BUSINESS_DATE)
        val milkId = milkProductId(database)
        assertTrue(dispatch("command.recordSpoilage", spoilagePayload(milkId), owner.session.sessionId).getBoolean("ok"))

        assertError(dispatch("query.spoilageHistory", session = cashier.sessionId), "permission_denied")
        assertError(dispatch("command.recordSpoilage", spoilagePayload(milkId), cashier.sessionId), "permission_denied")

        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val actionNames = (0 until actions.length()).map(actions::getString)
        assertEquals(65, actionNames.size)
        assertTrue("query.spoilageHistory" in actionNames)
        assertTrue("command.recordSpoilage" in actionNames)
    }

    @Test
    fun spoilageRejectsInvalidUnavailableInsufficientAndClosedDayRequestsWithoutPosting() = runBlocking {
        val owner = security.bootstrapOwner("reject.spoilage.owner", "Reject Spoilage Owner", OWNER_PASSWORD.toCharArray())
        seedMilk(database, 5.0, EARLIER_DATE)
        val milkId = milkProductId(database)
        val catalog = ProductCatalogService(database)
        val nonTrackedId = catalog.create(ProductCatalogInput("Non-stock Service", "Kg", inventoryTracking = false))
        val archivedId = catalog.create(ProductCatalogInput("Archived Cream", "Kg"))
        catalog.setArchived(archivedId, true)

        assertError(dispatch("command.recordSpoilage", spoilagePayload(milkId).put("quantity", "0"), owner.session.sessionId), "validation_failed")
        assertError(dispatch("command.recordSpoilage", spoilagePayload(milkId).put("reasonCode", ""), owner.session.sessionId), "validation_failed")
        assertError(dispatch("command.recordSpoilage", spoilagePayload(Long.MAX_VALUE), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordSpoilage", spoilagePayload(nonTrackedId), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordSpoilage", spoilagePayload(archivedId), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordSpoilage", spoilagePayload(milkId).put("quantity", "6"), owner.session.sessionId), "insufficient_stock")

        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )
        assertError(
            dispatch("command.recordSpoilage", spoilagePayload(milkId).put("occurredAt", EARLIER_DATE), owner.session.sessionId),
            "day_closed",
        )
        assertFalse(database.eventDao().findAllDescending().any { it.eventType == "SPOILAGE" })
    }

    @Test
    fun packagedSpoilageScreenRecordsAndRefreshesHistoryThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val suffix = System.currentTimeMillis()
        val marker = "Device spoilage $suffix"
        val spoilageDate = LocalDate.now().plusYears(7).toString()
        seedMilk(app.database, 12.0, spoilageDate)
        val milkId = milkProductId(app.database)
        val stockBefore = currentStock(app.database, milkId)
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('operations-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('operations-nav').click(); true")
            waitFor(webView, "document.getElementById('spoilage-work-tab') !== null", 15_000L)
            evaluate(webView, "document.getElementById('spoilage-work-tab').click(); true")
            waitFor(webView, "document.getElementById('record-spoilage-button') !== null")

            setSelect(webView, "spoilage-product", milkId.toString())
            setInput(webView, "spoilage-quantity", "2.75")
            setSelect(webView, "spoilage-reason", "spill")
            setInput(webView, "spoilage-date", spoilageDate)
            setTextArea(webView, "spoilage-notes", marker)
            evaluate(webView, "document.getElementById('record-spoilage-button').click(); true")
            waitFor(webView, "document.getElementById('spoilage-result') !== null", 20_000L)
            waitFor(webView, "document.getElementById('spoilage-history') !== null && document.getElementById('spoilage-history').innerText.includes(${JSONObject.quote(marker)})", 20_000L)

            val event = app.database.eventDao().findAllDescending().single { it.notes == marker }
            val detail = app.database.spoilageDao().findByEventId(event.eventId)!!
            val movement = app.database.inventoryLogDao().findByEventId(event.eventId).single()
            assertEquals("SPOILAGE", event.eventType)
            assertEquals(milkId, detail.productId)
            assertEquals(2.75, detail.quantity, 0.000001)
            assertEquals("spill", detail.reason)
            assertEquals(-2.75, movement.quantity, 0.000001)
            assertEquals(stockBefore - 2.75, currentStock(app.database, milkId), 0.000001)
            assertNotNull(app.database.eventActorLinkDao().find(event.eventId))
            assertTrue(evaluate(webView, "document.getElementById('spoilage-result').innerText.includes('2.75 Liters')") == "true")

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

    private fun spoilagePayload(productId: Long) = JSONObject()
        .put("productId", productId.toString())
        .put("quantity", "3.25")
        .put("occurredAt", BUSINESS_DATE)
        .put("reasonCode", "spill")
        .put("notes", "Spilled during transfer")

    private fun dateRange() = JSONObject()
        .put("startDateInclusive", BUSINESS_DATE)
        .put("endDateExclusive", "2026-09-01")

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
        const val EARLIER_DATE = "2026-08-30"
        const val OWNER_PASSWORD = "spoilage owner private passphrase 2026"
        const val OPERATOR_PASSWORD = "spoilage operator private passphrase 2026"
        const val CASHIER_PASSWORD = "spoilage cashier private passphrase 2026"

        suspend fun seedMilk(database: MilkShopDatabase, liters: Double, date: String) {
            val vendorId = VendorManagementService(database).create("Spoilage Stock Farm ${System.nanoTime()}")
            VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, date, liters, 4.0, 28.0))
        }

        suspend fun milkProductId(database: MilkShopDatabase): Long =
            database.productDao().findByName("Milk")!!.productId

        suspend fun currentStock(database: MilkShopDatabase, productId: Long): Double =
            database.readModelDao().currentStock().single { it.productId == productId }.quantityOnHand
    }
}
