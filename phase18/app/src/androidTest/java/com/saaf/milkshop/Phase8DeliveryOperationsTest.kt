package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.commands.DeliveryBridgeCommandService
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
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
class Phase8DeliveryOperationsTest {
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
    fun cashierCreatesPendingDeliveryWithExactReadModelAndAudit() = runBlocking {
        val owner = security.bootstrapOwner("delivery.owner", "Delivery Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("delivery.cashier", "Delivery Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("delivery.cashier", CASHIER_PASSWORD.toCharArray())
        val customerId = CustomerManagementService(database).create("Corner Cafe")
        val milkId = database.productDao().findByName("Milk")!!.productId

        val response = dispatch("command.createDelivery", deliveryPayload(customerId, milkId), cashier.sessionId)

        assertTrue(response.toString(), response.getBoolean("ok"))
        val deliveryId = response.getJSONObject("data").getString("entityId").toLong()
        val saved = database.deliveryDao().findById(deliveryId)!!
        assertEquals(customerId, saved.customerAccountId)
        assertEquals(milkId, saved.productId)
        assertEquals(3.5, saved.quantity, 0.000001)
        assertEquals("Route Green", saved.route)
        assertEquals(BUSINESS_DATE, saved.businessDate)
        assertEquals("Leave at back counter", saved.notes)
        assertEquals("pending", database.deliveryDao().latestStatus(deliveryId)!!.status)
        assertTrue(database.securityAuditDao().all().any {
            it.action == DeliveryBridgeCommandService.ACTION_CREATE_DELIVERY &&
                it.actorNameSnapshot == "Delivery Cashier" &&
                it.details == "delivery_id=$deliveryId"
        })

        val deliveries = dispatch("query.deliveries", session = cashier.sessionId).getJSONArray("data")
        assertEquals(1, deliveries.length())
        val item = deliveries.getJSONObject(0)
        assertEquals(deliveryId.toString(), item.getString("deliveryId"))
        assertEquals("Corner Cafe", item.getString("customerName"))
        assertEquals("Milk", item.getString("productName"))
        assertEquals("3.5", item.getString("quantity"))
        assertEquals("pending", item.getString("statusCode"))
        assertTrue(item.isNull("saleEventId"))
    }

    @Test
    fun saleLinkStatusAndArchivePreserveCanonicalSaleAndStatusHistory() = runBlocking {
        val owner = security.bootstrapOwner("lifecycle.owner", "Lifecycle Owner", OWNER_PASSWORD.toCharArray())
        val customerId = CustomerManagementService(database).create("Wholesale Hotel")
        val milkId = seedMilkAndReturnProduct(database, BUSINESS_DATE)
        val sale = SaleService(database).record(
            SaleInput(
                productId = milkId,
                occurredAt = BUSINESS_DATE,
                channel = "credit-wholesale",
                unitRate = 200.0,
                quantity = 2.0,
                payment = SalePayment.OnCredit(customerId),
            )
        )
        val customerBalanceBefore = database.readModelDao().accountBalances().single { it.accountId == customerId }.balance
        val eventCountBefore = database.eventDao().count()
        val deliveryId = dispatch(
            "command.createDelivery",
            deliveryPayload(customerId, milkId).put("quantity", "2"),
            owner.session.sessionId,
        ).getJSONObject("data").getString("entityId").toLong()

        assertTrue(dispatch(
            "command.linkDeliverySale",
            JSONObject().put("deliveryId", deliveryId.toString()).put("saleEventId", sale.eventId.toString()),
            owner.session.sessionId,
        ).getBoolean("ok"))
        assertTrue(dispatch(
            "command.transitionDelivery",
            JSONObject().put("deliveryId", deliveryId.toString()).put("statusCode", "delivered").put("notes", "Received by manager"),
            owner.session.sessionId,
        ).getBoolean("ok"))

        assertEquals(eventCountBefore, database.eventDao().count())
        assertEquals(customerBalanceBefore, database.readModelDao().accountBalances().single { it.accountId == customerId }.balance, 0.0)
        val record = database.deliveryDao().findById(deliveryId)!!
        assertEquals(sale.eventId, record.saleEventId)
        val history = database.deliveryDao().statusHistory(deliveryId)
        assertEquals(listOf("pending", "delivered"), history.map { it.status })
        assertEquals("Received by manager", history.last().notes)
        assertError(
            dispatch(
                "command.transitionDelivery",
                JSONObject().put("deliveryId", deliveryId.toString()).put("statusCode", "cancelled"),
                owner.session.sessionId,
            ),
            "conflict",
        )

        assertTrue(dispatch(
            "command.archiveDelivery",
            JSONObject().put("deliveryId", deliveryId.toString()),
            owner.session.sessionId,
        ).getBoolean("ok"))
        assertTrue(database.deliveryDao().findById(deliveryId)!!.archived)
        assertEquals(0, dispatch("query.deliveries", session = owner.session.sessionId).getJSONArray("data").length())
        assertTrue(database.securityAuditDao().all().map { it.action }.containsAll(listOf(
            DeliveryBridgeCommandService.ACTION_LINK_DELIVERY_SALE,
            DeliveryBridgeCommandService.ACTION_TRANSITION_DELIVERY,
            DeliveryBridgeCommandService.ACTION_ARCHIVE_DELIVERY,
        )))
    }

    @Test
    fun deliveriesRejectInvalidMismatchAndClosedDayChangesAndAdvertiseFiveActions() = runBlocking {
        val owner = security.bootstrapOwner("reject.delivery.owner", "Reject Delivery Owner", OWNER_PASSWORD.toCharArray())
        val customerId = CustomerManagementService(database).create("Correct Customer")
        val otherCustomerId = CustomerManagementService(database).create("Other Customer")
        val milkId = seedMilkAndReturnProduct(database, BUSINESS_DATE)
        val sale = SaleService(database).record(
            SaleInput(milkId, BUSINESS_DATE, "credit-wholesale", 200.0, 1.0, payment = SalePayment.OnCredit(customerId))
        )

        assertError(dispatch("query.deliveries"), "unauthenticated")
        assertError(dispatch("command.createDelivery", deliveryPayload(otherCustomerId, milkId).put("quantity", "0"), owner.session.sessionId), "validation_failed")
        assertError(
            dispatch("command.createDelivery", deliveryPayload(otherCustomerId, milkId).put("saleEventId", sale.eventId.toString()), owner.session.sessionId),
            "validation_failed",
        )

        val deliveryId = dispatch(
            "command.createDelivery",
            deliveryPayload(otherCustomerId, milkId),
            owner.session.sessionId,
        ).getJSONObject("data").getString("entityId").toLong()
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )
        assertError(dispatch("command.createDelivery", deliveryPayload(otherCustomerId, milkId), owner.session.sessionId), "day_closed")
        assertError(
            dispatch("command.transitionDelivery", JSONObject().put("deliveryId", deliveryId.toString()).put("statusCode", "delivered"), owner.session.sessionId),
            "day_closed",
        )
        assertError(
            dispatch("command.archiveDelivery", JSONObject().put("deliveryId", deliveryId.toString()), owner.session.sessionId),
            "day_closed",
        )
        assertFalse(database.deliveryDao().findById(deliveryId)!!.archived)
        assertEquals("pending", database.deliveryDao().latestStatus(deliveryId)!!.status)

        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(91, names.size)
        assertTrue(names.containsAll(listOf(
            "query.deliveries",
            "command.createDelivery",
            "command.transitionDelivery",
            "command.linkDeliverySale",
            "command.archiveDelivery",
        )))
    }

    @Test
    fun packagedDeliveryScreenCreatesCompletesAndArchivesThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val suffix = System.currentTimeMillis()
        val customerName = "Device Delivery Customer $suffix"
        val customerId = CustomerManagementService(app.database).create(customerName)
        val milkId = app.database.productDao().findByName("Milk")!!.productId
        val deliveryDate = LocalDate.now().plusYears(8).toString()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('deliveries-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('deliveries-nav').click(); true")
            waitFor(webView, "document.getElementById('create-delivery-button') !== null", 15_000L)
            setSelect(webView, "delivery-customer", customerId.toString())
            setSelect(webView, "delivery-product", milkId.toString())
            setInput(webView, "delivery-quantity", "4.25")
            setInput(webView, "delivery-date", deliveryDate)
            setInput(webView, "delivery-route", "Device Route $suffix")
            setTextArea(webView, "delivery-notes", "Device delivery note $suffix")
            evaluate(webView, "document.getElementById('create-delivery-button').click(); true")
            waitFor(webView, "document.getElementById('delivery-created-result') !== null", 20_000L)

            val delivery = app.database.deliveryDao().activeDeliveries().single { it.notes == "Device delivery note $suffix" }
            waitFor(webView, "document.querySelector('[data-delivery-id=\"${delivery.deliveryId}\"]') !== null", 20_000L)
            setInput(webView, "delivery-status-notes-${delivery.deliveryId}", "Handed to customer")
            evaluate(webView, "document.getElementById('mark-delivered-${delivery.deliveryId}').click(); true")
            waitFor(webView, "document.getElementById('archive-delivery-${delivery.deliveryId}') !== null", 20_000L)
            assertEquals(listOf("pending", "delivered"), app.database.deliveryDao().statusHistory(delivery.deliveryId).map { it.status })

            waitFor(webView, "document.getElementById('archive-delivery-${delivery.deliveryId}').disabled === false", 20_000L)
            evaluate(webView, "document.getElementById('archive-delivery-${delivery.deliveryId}').click(); true")
            waitFor(webView, "document.querySelector('[data-delivery-id=\"${delivery.deliveryId}\"]') === null", 20_000L)
            assertTrue(app.database.deliveryDao().findById(delivery.deliveryId)!!.archived)
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

    private fun deliveryPayload(customerId: Long, productId: Long) = JSONObject()
        .put("customerAccountId", customerId.toString())
        .put("productId", productId.toString())
        .put("quantity", "3.5")
        .put("route", "Route Green")
        .put("businessDate", BUSINESS_DATE)
        .put("saleEventId", JSONObject.NULL)
        .put("notes", "Leave at back counter")

    private suspend fun seedMilkAndReturnProduct(database: MilkShopDatabase, date: String): Long {
        val vendorId = VendorManagementService(database).create("Delivery Stock Farm ${System.nanoTime()}")
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, date, 20.0, 4.0, 28.0))
        return database.productDao().findByName("Milk")!!.productId
    }

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
        const val OWNER_PASSWORD = "delivery owner private passphrase 2026"
        const val CASHIER_PASSWORD = "delivery cashier private passphrase 2026"
    }
}
