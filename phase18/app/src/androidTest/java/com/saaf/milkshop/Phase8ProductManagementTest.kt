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
class Phase8ProductManagementTest {
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
    fun ownerCanUpdateArchiveAndRestoreProduct_withDurableActorAudit() = runBlocking {
        val owner = security.bootstrapOwner(
            "phase8.owner",
            "Phase 8 Owner",
            "phase eight private owner passphrase".toCharArray(),
        )
        val session = owner.session.sessionId
        val created = dispatch("command.createProduct", productPayload("Phase 8 Cream"), session)
        val productId = created.getJSONObject("data").getString("entityId")

        val updated = dispatch(
            "command.updateProduct",
            productPayload("Phase 8 Premium Cream")
                .put("productId", productId)
                .put("unitCode", "Liters")
                .put("defaultSaleRatePaisaPerUnit", "45678")
                .put("inventoryTracking", false)
                .put("notes", "Updated through the bridge"),
            session,
        )
        assertTrue(updated.getBoolean("ok"))
        assertEquals("Phase 8 Premium Cream", database.productDao().findById(productId.toLong())!!.name)
        val profile = database.productProfileDao().findByProductId(productId.toLong())!!
        assertEquals(456.78, profile.defaultSaleRate!!, 0.000001)
        assertFalse(profile.inventoryTracking)

        val archived = dispatch(
            "command.setProductArchived",
            JSONObject().put("productId", productId).put("archived", true),
            session,
        )
        assertTrue(archived.getBoolean("ok"))
        assertTrue(database.productProfileDao().findByProductId(productId.toLong())!!.archived)

        val restored = dispatch(
            "command.setProductArchived",
            JSONObject().put("productId", productId).put("archived", false),
            session,
        )
        assertTrue(restored.getBoolean("ok"))
        assertFalse(database.productProfileDao().findByProductId(productId.toLong())!!.archived)

        val actions = database.securityAuditDao().all().map { it.action }
        assertTrue(HybridCommandService.ACTION_UPDATE_PRODUCT in actions)
        assertTrue(HybridCommandService.ACTION_ARCHIVE_PRODUCT in actions)
        assertTrue(HybridCommandService.ACTION_RESTORE_PRODUCT in actions)
        database.securityAuditDao().all()
            .filter { it.action in setOf(HybridCommandService.ACTION_UPDATE_PRODUCT, HybridCommandService.ACTION_ARCHIVE_PRODUCT, HybridCommandService.ACTION_RESTORE_PRODUCT) }
            .forEach {
                assertEquals("Phase 8 Owner", it.actorNameSnapshot)
                assertEquals("owner", it.actorRoleSnapshot)
            }

        val missing = dispatch(
            "command.updateProduct",
            productPayload("Missing product").put("productId", "9223372036854775807"),
            session,
        )
        assertFalse(missing.getBoolean("ok"))
        assertEquals("not_found", missing.getJSONObject("error").getString("code"))
    }

    @Test
    fun cashierCannotUpdateOrArchiveProduct() = runBlocking {
        val owner = security.bootstrapOwner(
            "phase8.permission.owner",
            "Phase 8 Permission Owner",
            "phase eight permission owner passphrase".toCharArray(),
        )
        val created = dispatch("command.createProduct", productPayload("Protected Phase 8 Product"), owner.session.sessionId)
        val productId = created.getJSONObject("data").getString("entityId")
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("phase8.cashier", "Phase 8 Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, "phase eight cashier passphrase".toCharArray())
        val cashier = security.login("phase8.cashier", "phase eight cashier passphrase".toCharArray())

        val update = dispatch(
            "command.updateProduct",
            productPayload("Cashier rename forbidden").put("productId", productId),
            cashier.sessionId,
        )
        assertFalse(update.getBoolean("ok"))
        assertEquals("permission_denied", update.getJSONObject("error").getString("code"))

        val archive = dispatch(
            "command.setProductArchived",
            JSONObject().put("productId", productId).put("archived", true),
            cashier.sessionId,
        )
        assertFalse(archive.getBoolean("ok"))
        assertEquals("permission_denied", archive.getJSONObject("error").getString("code"))
        assertEquals("Protected Phase 8 Product", database.productDao().findById(productId.toLong())!!.name)
        assertFalse(database.productProfileDao().findByProductId(productId.toLong())!!.archived)
    }

    @Test
    fun packagedProductsScreen_addsEditsArchivesAndRestoresThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val originalName = "Phase 8 Device Product ${System.currentTimeMillis()}"
        val updatedName = "$originalName Updated"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('add-product-button') !== null", 20_000L)

            evaluate(webView, "document.getElementById('add-product-button').click(); true")
            waitFor(webView, "document.getElementById('product-name') !== null")
            setInput(webView, "product-name", originalName)
            setInput(webView, "product-rate", "210.25")
            evaluate(webView, "document.getElementById('save-product-button').click(); true")
            waitFor(webView, "document.getElementById('native-product-list')?.innerText.includes(${JSONObject.quote(originalName)}) === true", 15_000L)

            val created = app.database.productDao().findByName(originalName)
            assertNotNull(created)
            val productId = created!!.productId
            evaluate(webView, "document.getElementById('edit-product-$productId').click(); true")
            waitFor(webView, "document.getElementById('product-name') !== null")
            setInput(webView, "product-name", updatedName)
            setInput(webView, "product-rate", "321.09")
            evaluate(webView, "document.getElementById('save-product-button').click(); true")
            waitFor(webView, "document.getElementById('native-product-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === true", 15_000L)
            assertEquals(updatedName, app.database.productDao().findById(productId)!!.name)
            assertEquals(321.09, app.database.productProfileDao().findByProductId(productId)!!.defaultSaleRate!!, 0.000001)

            evaluate(webView, "document.getElementById('archive-product-$productId').click(); true")
            waitFor(webView, "document.getElementById('confirm-product-archive') !== null")
            evaluate(webView, "document.getElementById('confirm-product-archive').click(); true")
            waitFor(webView, "document.getElementById('native-product-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === false", 15_000L)
            assertTrue(app.database.productProfileDao().findByProductId(productId)!!.archived)

            evaluate(webView, "document.getElementById('archived-products-tab').click(); true")
            waitFor(webView, "document.getElementById('native-product-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === true")
            evaluate(webView, "document.getElementById('restore-product-$productId').click(); true")
            waitFor(webView, "document.getElementById('native-product-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === false", 15_000L)
            assertFalse(app.database.productProfileDao().findByProductId(productId)!!.archived)

            val actions = app.database.securityAuditDao().all().map { it.action }
            assertTrue(HybridCommandService.ACTION_UPDATE_PRODUCT in actions)
            assertTrue(HybridCommandService.ACTION_ARCHIVE_PRODUCT in actions)
            assertTrue(HybridCommandService.ACTION_RESTORE_PRODUCT in actions)

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

    private suspend fun dispatch(action: String, payload: JSONObject = JSONObject(), session: String? = null): JSONObject {
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
