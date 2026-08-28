package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.application.bridge.HybridCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
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
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class Phase8CustomerProfilesTest {
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
    fun ownerCreatesRenamesAndProfilesCustomer_withoutEditableBalance() = runBlocking {
        val owner = security.bootstrapOwner(
            "customer.owner",
            "Customer Owner",
            "customer owner private passphrase 2026".toCharArray(),
        )
        val session = owner.session.sessionId
        val created = dispatch(
            "command.createCustomer",
            JSONObject().put("name", "Morning Route Customer"),
            session,
        )
        assertTrue(created.getBoolean("ok"))
        val accountId = created.getJSONObject("data").getString("entityId")

        val profileSaved = dispatch(
            "command.saveCustomerProfile",
            customerProfilePayload(accountId, active = true),
            session,
        )
        assertTrue(profileSaved.getBoolean("ok"))
        val renamed = dispatch(
            "command.renameCustomer",
            JSONObject().put("accountId", accountId).put("name", "Updated Route Customer"),
            session,
        )
        assertTrue(renamed.getBoolean("ok"))

        val customer = customerFromMasterData(session, accountId)
        assertEquals("Updated Route Customer", customer.getString("name"))
        assertEquals("0", customer.getString("canonicalBalancePaisa"))
        assertEquals("03001234567", customer.getString("phone"))
        assertEquals("Route Blue", customer.getString("areaRoute"))
        assertEquals("500000", customer.getString("creditLimitPaisa"))
        assertEquals("2.5", customer.getString("dailyQuota"))
        assertTrue(customer.getBoolean("active"))

        assertTrue(dispatch("command.saveCustomerProfile", customerProfilePayload(accountId, active = false), session).getBoolean("ok"))
        assertFalse(customerFromMasterData(session, accountId).getBoolean("active"))
        assertTrue(dispatch("command.saveCustomerProfile", customerProfilePayload(accountId, active = true), session).getBoolean("ok"))
        assertTrue(customerFromMasterData(session, accountId).getBoolean("active"))

        val duplicate = dispatch(
            "command.createCustomer",
            JSONObject().put("name", "updated route customer"),
            session,
        )
        assertFalse(duplicate.getBoolean("ok"))
        assertEquals("conflict", duplicate.getJSONObject("error").getString("code"))

        val missing = dispatch(
            "command.saveCustomerProfile",
            customerProfilePayload("9223372036854775807", active = true),
            session,
        )
        assertFalse(missing.getBoolean("ok"))
        assertEquals("not_found", missing.getJSONObject("error").getString("code"))

        val audits = database.securityAuditDao().all()
        assertTrue(audits.any { it.action == HybridCommandService.ACTION_CREATE_CUSTOMER })
        assertTrue(audits.any { it.action == HybridCommandService.ACTION_RENAME_CUSTOMER })
        assertTrue(audits.count { it.action == HybridCommandService.ACTION_SAVE_CUSTOMER_PROFILE } >= 3)
        audits.filter { it.action.startsWith("bridge_") && it.details == "account_id=$accountId" }.forEach {
            assertEquals("Customer Owner", it.actorNameSnapshot)
            assertEquals("owner", it.actorRoleSnapshot)
        }
    }

    @Test
    fun cashierCannotCreateRenameOrChangeCustomerProfile() = runBlocking {
        val owner = security.bootstrapOwner(
            "customer.permission.owner",
            "Customer Permission Owner",
            "customer permission owner passphrase 2026".toCharArray(),
        )
        val created = dispatch(
            "command.createCustomer",
            JSONObject().put("name", "Protected Customer"),
            owner.session.sessionId,
        )
        val accountId = created.getJSONObject("data").getString("entityId")
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("customer.cashier", "Customer Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, "customer cashier passphrase 2026".toCharArray())
        val cashier = security.login("customer.cashier", "customer cashier passphrase 2026".toCharArray())

        val createDenied = dispatch(
            "command.createCustomer",
            JSONObject().put("name", "Cashier Customer"),
            cashier.sessionId,
        )
        val renameDenied = dispatch(
            "command.renameCustomer",
            JSONObject().put("accountId", accountId).put("name", "Cashier Rename"),
            cashier.sessionId,
        )
        val profileDenied = dispatch(
            "command.saveCustomerProfile",
            customerProfilePayload(accountId, active = false),
            cashier.sessionId,
        )
        listOf(createDenied, renameDenied, profileDenied).forEach {
            assertFalse(it.getBoolean("ok"))
            assertEquals("permission_denied", it.getJSONObject("error").getString("code"))
        }
        assertEquals("Customer: Protected Customer", database.accountDao().findById(accountId.toLong())!!.name)
        assertTrue(database.customerProfileDao().findByAccountId(accountId.toLong())!!.active)
        assertEquals(null, database.accountDao().findByCategory("Customer").find { it.name == "Customer: Cashier Customer" })
    }

    @Test
    fun packagedCustomersScreen_addsEditsDeactivatesAndRestoresThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val originalName = "Device Customer ${System.currentTimeMillis()}"
        val updatedName = "$originalName Updated"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('customers-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('customers-nav').click(); true")
            waitFor(webView, "document.getElementById('add-customer-button') !== null")

            evaluate(webView, "document.getElementById('add-customer-button').click(); true")
            waitFor(webView, "document.getElementById('customer-name') !== null")
            assertEquals("true", evaluate(webView, "document.getElementById('customer-balance') === null"))
            setInput(webView, "customer-name", originalName)
            setInput(webView, "customer-phone", "03005550123")
            setInput(webView, "customer-route", "Device Route")
            setInput(webView, "customer-credit-limit", "7500.50")
            setInput(webView, "customer-daily-quota", "3.25")
            evaluate(webView, "document.getElementById('save-customer-button').click(); true")
            waitFor(webView, "document.getElementById('native-customer-list')?.innerText.includes(${JSONObject.quote(originalName)}) === true", 15_000L)

            val account = app.database.accountDao().findByCategory("Customer").single { it.name == "Customer: $originalName" }
            val accountId = account.accountId
            var profile = app.database.customerProfileDao().findByAccountId(accountId)
            assertNotNull(profile)
            assertEquals("03005550123", profile!!.phone)
            assertEquals(7500.50, profile.creditLimit!!, 0.000001)
            assertEquals(3.25, profile.dailyQuota!!, 0.000001)

            evaluate(webView, "document.getElementById('edit-customer-$accountId').click(); true")
            waitFor(webView, "document.getElementById('customer-name') !== null")
            setInput(webView, "customer-name", updatedName)
            setInput(webView, "customer-phone", "03009999888")
            evaluate(webView, "document.getElementById('save-customer-button').click(); true")
            waitFor(webView, "document.getElementById('native-customer-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === true", 15_000L)
            assertEquals("Customer: $updatedName", app.database.accountDao().findById(accountId)!!.name)
            profile = app.database.customerProfileDao().findByAccountId(accountId)
            assertEquals("03009999888", profile!!.phone)

            evaluate(webView, "document.getElementById('deactivate-customer-$accountId').click(); true")
            waitFor(webView, "document.getElementById('confirm-customer-deactivate') !== null")
            evaluate(webView, "document.getElementById('confirm-customer-deactivate').click(); true")
            waitFor(webView, "document.getElementById('native-customer-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === false", 15_000L)
            assertFalse(app.database.customerProfileDao().findByAccountId(accountId)!!.active)

            evaluate(webView, "document.getElementById('inactive-customers-tab').click(); true")
            waitFor(webView, "document.getElementById('native-customer-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === true")
            evaluate(webView, "document.getElementById('restore-customer-$accountId').click(); true")
            waitFor(webView, "document.getElementById('native-customer-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === false", 15_000L)
            assertTrue(app.database.customerProfileDao().findByAccountId(accountId)!!.active)

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

    private suspend fun customerFromMasterData(session: String, accountId: String): JSONObject {
        val customers = dispatch("query.masterData", session = session).getJSONObject("data").getJSONArray("customers")
        return (0 until customers.length())
            .map { customers.getJSONObject(it) }
            .single { it.getString("accountId") == accountId }
    }

    private fun customerProfilePayload(accountId: String, active: Boolean): JSONObject = JSONObject()
        .put("accountId", accountId)
        .put("phone", "03001234567")
        .put("areaRoute", "Route Blue")
        .put("address", "Street 7")
        .put("notes", "Morning delivery")
        .put("creditLimitPaisa", "500000")
        .put("dailyQuota", "2.5")
        .put("active", active)

    private suspend fun dispatch(action: String, payload: JSONObject = JSONObject(), session: String? = null): JSONObject {
        val request = JSONObject()
            .put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("requestId", UUID.randomUUID().toString())
            .put("action", action)
            .put("payload", payload)
        if (session != null) request.put("sessionHandle", session)
        return JSONObject(dispatcher.dispatch(request.toString()))
    }

    private fun activityWebView(scenario: ActivityScenario<HybridActivity>): WebView {
        val holder = AtomicReference<WebView>()
        scenario.onActivity { holder.set(it.webViewForTesting()) }
        return holder.get()
    }

    private fun setInput(webView: WebView, id: String, value: String) {
        val script = """
            (() => {
              const input = document.getElementById(${JSONObject.quote(id)});
              const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
              setter.call(input, ${JSONObject.quote(value)});
              input.dispatchEvent(new Event('input', { bubbles: true }));
              return input.value;
            })()
        """.trimIndent()
        evaluate(webView, script)
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
            webView.evaluateJavascript(script) {
                result.set(it)
                latch.countDown()
            }
        }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }
}
