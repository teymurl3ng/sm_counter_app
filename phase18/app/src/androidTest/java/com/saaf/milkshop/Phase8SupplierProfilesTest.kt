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
class Phase8SupplierProfilesTest {
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
    fun ownerCreatesRenamesAndProfilesSupplier_withoutEditableBalanceOrRate() = runBlocking {
        val owner = security.bootstrapOwner(
            "supplier.owner",
            "Supplier Owner",
            "supplier owner private passphrase 2026".toCharArray(),
        )
        val session = owner.session.sessionId
        val created = dispatch("command.createVendor", JSONObject().put("name", "Morning Dairy"), session)
        assertTrue(created.getBoolean("ok"))
        val accountId = created.getJSONObject("data").getString("entityId")

        assertTrue(dispatch("command.saveVendorProfile", supplierProfilePayload(accountId, active = true), session).getBoolean("ok"))
        assertTrue(dispatch("command.renameVendor", JSONObject().put("accountId", accountId).put("name", "Updated Dairy"), session).getBoolean("ok"))

        val supplier = supplierFromMasterData(session, accountId)
        assertEquals("Updated Dairy", supplier.getString("name"))
        assertEquals("0", supplier.getString("canonicalBalancePaisa"))
        assertEquals("03001234567", supplier.getString("phone"))
        assertEquals("Village Road", supplier.getString("location"))
        assertFalse(supplier.has("milkRate"))
        assertTrue(supplier.getBoolean("active"))

        assertTrue(dispatch("command.saveVendorProfile", supplierProfilePayload(accountId, active = false), session).getBoolean("ok"))
        assertFalse(supplierFromMasterData(session, accountId).getBoolean("active"))
        assertTrue(dispatch("command.saveVendorProfile", supplierProfilePayload(accountId, active = true), session).getBoolean("ok"))
        assertTrue(supplierFromMasterData(session, accountId).getBoolean("active"))

        val duplicate = dispatch("command.createVendor", JSONObject().put("name", "updated dairy"), session)
        assertFalse(duplicate.getBoolean("ok"))
        assertEquals("conflict", duplicate.getJSONObject("error").getString("code"))

        val missing = dispatch("command.saveVendorProfile", supplierProfilePayload("9223372036854775807", true), session)
        assertFalse(missing.getBoolean("ok"))
        assertEquals("not_found", missing.getJSONObject("error").getString("code"))

        val audits = database.securityAuditDao().all()
        assertTrue(audits.any { it.action == HybridCommandService.ACTION_CREATE_VENDOR })
        assertTrue(audits.any { it.action == HybridCommandService.ACTION_RENAME_VENDOR })
        assertTrue(audits.count { it.action == HybridCommandService.ACTION_SAVE_VENDOR_PROFILE } >= 3)
        audits.filter { it.action.startsWith("bridge_") && it.details == "account_id=$accountId" }.forEach {
            assertEquals("Supplier Owner", it.actorNameSnapshot)
            assertEquals("owner", it.actorRoleSnapshot)
        }
    }

    @Test
    fun cashierCannotCreateRenameOrChangeSupplierProfile() = runBlocking {
        val owner = security.bootstrapOwner(
            "supplier.permission.owner",
            "Supplier Permission Owner",
            "supplier permission owner passphrase 2026".toCharArray(),
        )
        val created = dispatch("command.createVendor", JSONObject().put("name", "Protected Dairy"), owner.session.sessionId)
        val accountId = created.getJSONObject("data").getString("entityId")
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("supplier.cashier", "Supplier Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, "supplier cashier passphrase 2026".toCharArray())
        val cashier = security.login("supplier.cashier", "supplier cashier passphrase 2026".toCharArray())

        val createDenied = dispatch("command.createVendor", JSONObject().put("name", "Cashier Dairy"), cashier.sessionId)
        val renameDenied = dispatch("command.renameVendor", JSONObject().put("accountId", accountId).put("name", "Cashier Rename"), cashier.sessionId)
        val profileDenied = dispatch("command.saveVendorProfile", supplierProfilePayload(accountId, false), cashier.sessionId)
        listOf(createDenied, renameDenied, profileDenied).forEach {
            assertFalse(it.getBoolean("ok"))
            assertEquals("permission_denied", it.getJSONObject("error").getString("code"))
        }
        assertEquals("Vendor: Protected Dairy", database.accountDao().findById(accountId.toLong())!!.name)
        assertTrue(database.vendorProfileDao().findByAccountId(accountId.toLong())!!.active)
        assertEquals(null, database.accountDao().findByCategory("Vendor").find { it.name == "Vendor: Cashier Dairy" })
    }

    @Test
    fun packagedSuppliersScreen_addsEditsDeactivatesAndRestoresThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val originalName = "Device Supplier ${System.currentTimeMillis()}"
        val updatedName = "$originalName Updated"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('suppliers-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('suppliers-nav').click(); true")
            waitFor(webView, "document.getElementById('add-supplier-button') !== null")

            evaluate(webView, "document.getElementById('add-supplier-button').click(); true")
            waitFor(webView, "document.getElementById('supplier-name') !== null")
            assertEquals("true", evaluate(webView, "document.getElementById('supplier-balance') === null"))
            assertEquals("true", evaluate(webView, "document.getElementById('supplier-milk-rate') === null"))
            setInput(webView, "supplier-name", originalName)
            setInput(webView, "supplier-phone", "03005550123")
            setInput(webView, "supplier-location", "Device Village")
            evaluate(webView, "document.getElementById('save-supplier-button').click(); true")
            waitFor(webView, "document.getElementById('native-supplier-list')?.innerText.includes(${JSONObject.quote(originalName)}) === true", 15_000L)

            val account = app.database.accountDao().findByCategory("Vendor").single { it.name == "Vendor: $originalName" }
            val accountId = account.accountId
            var profile = app.database.vendorProfileDao().findByAccountId(accountId)
            assertNotNull(profile)
            assertEquals("03005550123", profile!!.phone)
            assertEquals("Device Village", profile.location)

            evaluate(webView, "document.getElementById('edit-supplier-$accountId').click(); true")
            waitFor(webView, "document.getElementById('supplier-name') !== null")
            setInput(webView, "supplier-name", updatedName)
            setInput(webView, "supplier-phone", "03009999888")
            evaluate(webView, "document.getElementById('save-supplier-button').click(); true")
            waitFor(webView, "document.getElementById('native-supplier-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === true", 15_000L)
            assertEquals("Vendor: $updatedName", app.database.accountDao().findById(accountId)!!.name)
            profile = app.database.vendorProfileDao().findByAccountId(accountId)
            assertEquals("03009999888", profile!!.phone)

            evaluate(webView, "document.getElementById('deactivate-supplier-$accountId').click(); true")
            waitFor(webView, "document.getElementById('confirm-supplier-deactivate') !== null")
            evaluate(webView, "document.getElementById('confirm-supplier-deactivate').click(); true")
            waitFor(webView, "document.getElementById('native-supplier-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === false", 15_000L)
            assertFalse(app.database.vendorProfileDao().findByAccountId(accountId)!!.active)

            evaluate(webView, "document.getElementById('inactive-suppliers-tab').click(); true")
            waitFor(webView, "document.getElementById('native-supplier-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === true")
            evaluate(webView, "document.getElementById('restore-supplier-$accountId').click(); true")
            waitFor(webView, "document.getElementById('native-supplier-list')?.innerText.includes(${JSONObject.quote(updatedName)}) === false", 15_000L)
            assertTrue(app.database.vendorProfileDao().findByAccountId(accountId)!!.active)

            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
    }

    private suspend fun supplierFromMasterData(session: String, accountId: String): JSONObject {
        val suppliers = dispatch("query.masterData", session = session).getJSONObject("data").getJSONArray("vendors")
        return (0 until suppliers.length()).map { suppliers.getJSONObject(it) }.single { it.getString("accountId") == accountId }
    }

    private fun supplierProfilePayload(accountId: String, active: Boolean): JSONObject = JSONObject()
        .put("accountId", accountId)
        .put("phone", "03001234567")
        .put("location", "Village Road")
        .put("notes", "Morning collection")
        .put("active", active)

    private suspend fun dispatch(action: String, payload: JSONObject = JSONObject(), session: String? = null): JSONObject {
        val request = JSONObject().put("contractVersion", BridgeContractSnapshot.VERSION).put("requestId", UUID.randomUUID().toString()).put("action", action).put("payload", payload)
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
