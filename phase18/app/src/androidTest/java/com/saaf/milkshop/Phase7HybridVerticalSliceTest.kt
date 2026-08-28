package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
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
class Phase7HybridVerticalSliceTest {
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
    fun canonicalMessages_bootstrapAuthenticateWriteReadAuditAndLogout() = runBlocking {
        val info = dispatch("system.bridgeInfo")
        assertTrue(info.getBoolean("ok"))
        assertEquals(BridgeContractSnapshot.VERSION, info.getJSONObject("data").getInt("contractVersion"))
        assertEquals(BridgeContractSnapshot.CONTRACT_SHA256, info.getJSONObject("data").getString("contractSha256"))

        val status = dispatch("query.bootstrapStatus")
        assertTrue(status.getJSONObject("data").getBoolean("needsOwnerBootstrap"))

        val ownerPassphrase = "a private owner passphrase 2026"
        val bootstrap = dispatch(
            "command.auth.bootstrapOwner",
            JSONObject()
                .put("loginName", "phase7.owner")
                .put("displayName", "Phase 7 Owner")
                .put("passphrase", ownerPassphrase),
        )
        assertTrue(bootstrap.getBoolean("ok"))
        assertFalse(bootstrap.toString().contains(ownerPassphrase))
        val bootstrapData = bootstrap.getJSONObject("data")
        val session = bootstrapData.getString("sessionHandle")
        assertTrue(bootstrapData.getString("oneTimeRecoveryCode").isNotBlank())
        assertEquals("owner", bootstrapData.getJSONObject("currentSession").getString("roleCode"))

        val actorInjection = dispatch(
            "command.createProduct",
            productPayload("Forbidden Actor Product").put("actorRole", "owner"),
            session,
        )
        assertFalse(actorInjection.getBoolean("ok"))
        assertEquals("invalid_request", actorInjection.getJSONObject("error").getString("code"))

        val created = dispatch("command.createProduct", productPayload("Bridge Test Cream"), session)
        assertTrue(created.getBoolean("ok"))
        val productId = created.getJSONObject("data").getString("entityId").toLong()
        assertNotNull(database.productDao().findById(productId))

        val masterData = dispatch("query.masterData", session = session)
        val products = masterData.getJSONObject("data").getJSONArray("products")
        assertTrue((0 until products.length()).any { products.getJSONObject(it).getString("name") == "Bridge Test Cream" })
        val audit = database.securityAuditDao().all().last { it.action == HybridCommandService.ACTION_CREATE_PRODUCT }
        assertEquals("Phase 7 Owner", audit.actorNameSnapshot)
        assertEquals("owner", audit.actorRoleSnapshot)

        assertTrue(dispatch("command.auth.logout", session = session).getBoolean("ok"))
        val afterLogout = dispatch("query.masterData", session = session)
        assertFalse(afterLogout.getBoolean("ok"))
        assertEquals("session_expired", afterLogout.getJSONObject("error").getString("code"))
    }

    @Test
    fun protectedWrite_rejectsCashierDirectBridgeAttempt() = runBlocking {
        val owner = security.bootstrapOwner(
            "permission.owner",
            "Permission Owner",
            "permission owner passphrase 2026".toCharArray(),
        )
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("phase7.cashier", "Phase 7 Cashier", "cashier"),
        )
        security.completeUserEnrollment(
            pending.enrollmentToken,
            "cashier private passphrase 2026".toCharArray(),
        )
        val cashier = security.login("phase7.cashier", "cashier private passphrase 2026".toCharArray())

        val response = dispatch("command.createProduct", productPayload("Cashier Must Not Create"), cashier.sessionId)
        assertFalse(response.getBoolean("ok"))
        assertEquals("permission_denied", response.getJSONObject("error").getString("code"))
        assertEquals(null, database.productDao().findByName("Cashier Must Not Create"))
    }

    @Test
    fun packagedReact_loginCreateProductResumeLogout_roundTripsThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val productName = "Hybrid Device Product ${System.currentTimeMillis()}"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            assertEquals(HybridActivity.HYBRID_ENTRY_URL, evaluate(webView, "location.href")?.trim('"'))
            instrumentation.runOnMainSync {
                assertFalse(webView.settings.allowFileAccess)
                assertFalse(webView.settings.allowContentAccess)
                assertFalse(webView.settings.domStorageEnabled)
            }
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('signed-in-user')?.innerText.includes('Instrumentation Owner') === true", 20_000L)

            evaluate(webView, "document.getElementById('add-product-button').click(); true")
            waitFor(webView, "document.getElementById('product-name') !== null")
            setInput(webView, "product-name", productName)
            setInput(webView, "product-rate", "123.45")
            evaluate(webView, "document.getElementById('save-product-button').click(); true")
            waitFor(
                webView,
                "document.getElementById('native-product-list')?.innerText.includes(${JSONObject.quote(productName)}) === true",
                15_000L,
            )

            val saved = app.database.productDao().findByName(productName)
            assertNotNull(saved)
            val profile = app.database.productProfileDao().findByProductId(saved!!.productId)
            assertEquals(123.45, profile!!.defaultSaleRate!!, 0.000001)
            val audit = app.database.securityAuditDao().all().last { it.action == HybridCommandService.ACTION_CREATE_PRODUCT }
            assertEquals("Instrumentation Owner", audit.actorNameSnapshot)

            scenario.moveToState(Lifecycle.State.CREATED)
            SystemClock.sleep(200)
            instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
            instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
            SystemClock.sleep(250)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitFor(webView, "document.getElementById('signed-in-user') !== null")

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

    private suspend fun dispatch(
        action: String,
        payload: JSONObject = JSONObject(),
        session: String? = null,
    ): JSONObject {
        val request = JSONObject()
            .put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("requestId", UUID.randomUUID().toString())
            .put("action", action)
            .put("payload", payload)
        if (session != null) request.put("sessionHandle", session)
        return JSONObject(dispatcher.dispatch(request.toString()))
    }

    private fun productPayload(name: String): JSONObject = JSONObject()
        .put("name", name)
        .put("unitCode", "Kg")
        .put("defaultSaleRatePaisaPerUnit", "12345")
        .put("inventoryTracking", true)
        .put("notes", JSONObject.NULL)

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
