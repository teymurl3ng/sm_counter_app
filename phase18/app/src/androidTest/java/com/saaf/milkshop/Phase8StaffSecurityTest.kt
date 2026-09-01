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
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.hybrid.HybridActivity
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase8StaffSecurityTest {
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
    fun bridgeCreatesIdentityThenCompletesPrivateEnrollmentAndAuditsIt() = runBlocking {
        val owner = security.bootstrapOwner("staff.owner", "Staff Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        val started = dispatch("command.auth.beginUserEnrollment", JSONObject()
            .put("loginName", "shop.cashier").put("displayName", "Shop Cashier")
            .put("roleCode", "cashier").put("employeeAccountId", JSONObject.NULL), session)
        assertTrue(started.toString(), started.getBoolean("ok"))
        val challenge = started.getJSONObject("data")
        val userId = challenge.getString("targetUserId")

        val usersBefore = dispatch("query.users", session = session).getJSONArray("data")
        val pending = (0 until usersBefore.length()).map(usersBefore::getJSONObject).single { it.getString("userId") == userId }
        assertEquals("cashier", pending.getString("roleCode"))
        assertTrue(pending.getBoolean("setupRequired"))
        assertTrue(pending.isNull("employeeAccountId"))

        val completed = dispatch("command.auth.completeEnrollment", JSONObject()
            .put("enrollmentToken", challenge.getString("enrollmentToken")).put("passphrase", CASHIER_PASSWORD))
        assertFalse(completed.getJSONObject("data").getBoolean("setupRequired"))
        assertTrue(dispatch("command.auth.login", JSONObject().put("loginName", "shop.cashier").put("passphrase", CASHIER_PASSWORD)).getBoolean("ok"))

        val audit = dispatch("query.securityAudit", session = session).getJSONArray("data")
        val actions = (0 until audit.length()).map { audit.getJSONObject(it).getString("actionCode") }
        assertTrue(actions.containsAll(listOf("create_user", "complete_enrollment", "login")))
        assertError(dispatch("query.users", JSONObject().put("unexpected", true), session), "invalid_request")
    }

    @Test
    fun administratorControlsOrdinaryStaffButCannotControlPowerfulAccounts() = runBlocking {
        val owner = security.bootstrapOwner("authority.owner", "Authority Owner", OWNER_PASSWORD.toCharArray())
        val ownerSession = owner.session.sessionId
        val adminPending = security.beginUserEnrollment(ownerSession, CreateUserInput("shop.admin", "Shop Admin", "admin"))
        security.completeUserEnrollment(adminPending.enrollmentToken, ADMIN_PASSWORD.toCharArray())
        val adminSession = security.login("shop.admin", ADMIN_PASSWORD.toCharArray()).sessionId

        val managerStarted = dispatch("command.auth.beginUserEnrollment", JSONObject()
            .put("loginName", "shop.manager").put("displayName", "Shop Manager")
            .put("roleCode", "manager").put("employeeAccountId", JSONObject.NULL), adminSession)
        val managerChallenge = managerStarted.getJSONObject("data")
        dispatch("command.auth.completeEnrollment", JSONObject().put("enrollmentToken", managerChallenge.getString("enrollmentToken")).put("passphrase", MANAGER_PASSWORD))
        val managerId = managerChallenge.getString("targetUserId")
        val managerSession = security.login("shop.manager", MANAGER_PASSWORD.toCharArray()).sessionId

        assertError(dispatch("command.auth.beginUserEnrollment", JSONObject()
            .put("loginName", "second.admin").put("displayName", "Second Admin")
            .put("roleCode", "admin").put("employeeAccountId", JSONObject.NULL), adminSession), "permission_denied")
        assertTrue(dispatch("command.auth.reauthenticate", JSONObject().put("passphrase", ADMIN_PASSWORD), adminSession).getBoolean("ok"))
        assertTrue(dispatch("command.auth.changeUserRole", JSONObject().put("targetUserId", managerId).put("roleCode", "cashier"), adminSession).getBoolean("ok"))
        assertError(dispatch("query.currentSession", session = managerSession), "session_expired")

        val reset = dispatch("command.auth.beginCredentialReset", JSONObject().put("targetUserId", managerId), adminSession)
        assertTrue(reset.toString(), reset.getBoolean("ok"))
        assertError(dispatch("command.auth.login", JSONObject().put("loginName", "shop.manager").put("passphrase", MANAGER_PASSWORD)), "authentication_failed")
        val users = dispatch("query.users", session = adminSession).getJSONArray("data")
        assertTrue((0 until users.length()).map(users::getJSONObject).single { it.getString("userId") == managerId }.getBoolean("setupRequired"))
    }

    @Test
    fun ownPassphraseChangeAndOwnerRecoveryRotateEveryCredential() = runBlocking {
        val owner = security.bootstrapOwner("recovery.owner", "Recovery Owner", OWNER_PASSWORD.toCharArray())
        val firstSession = owner.session.sessionId
        assertTrue(dispatch("command.auth.changeOwnPassphrase", JSONObject()
            .put("currentPassphrase", OWNER_PASSWORD).put("newPassphrase", NEW_OWNER_PASSWORD), firstSession).getBoolean("ok"))
        assertError(dispatch("query.currentSession", session = firstSession), "session_expired")
        assertError(dispatch("command.auth.login", JSONObject().put("loginName", "recovery.owner").put("passphrase", OWNER_PASSWORD)), "authentication_failed")

        val recovered = dispatch("command.auth.recoverOwner", JSONObject()
            .put("loginName", "recovery.owner").put("recoveryCode", owner.recoveryCode)
            .put("newPassphrase", RECOVERED_PASSWORD))
        assertTrue(recovered.toString(), recovered.getBoolean("ok"))
        val data = recovered.getJSONObject("data")
        assertEquals("Recovery Owner", data.getJSONObject("currentSession").getString("displayName"))
        assertNotEquals(owner.recoveryCode, data.getString("replacementRecoveryCode"))
        assertTrue(dispatch("query.currentSession", session = data.getString("sessionHandle")).getBoolean("ok"))
        assertError(dispatch("command.auth.recoverOwner", JSONObject()
            .put("loginName", "recovery.owner").put("recoveryCode", owner.recoveryCode)
            .put("newPassphrase", "another private owner passphrase 2026")), "authentication_failed")
    }

    @Test
    fun permissionsLastOwnerSafeguardsAndAllSeventyFiveActionsAreEnforced() = runBlocking {
        val owner = security.bootstrapOwner("guard.owner", "Guard Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        val cashier = security.beginUserEnrollment(session, CreateUserInput("guard.cashier", "Guard Cashier", "cashier"))
        security.completeUserEnrollment(cashier.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashierSession = security.login("guard.cashier", CASHIER_PASSWORD.toCharArray()).sessionId
        assertError(dispatch("query.users", session = cashierSession), "permission_denied")
        assertError(dispatch("query.securityAudit", session = cashierSession), "permission_denied")
        assertError(dispatch("command.auth.setUserEnabled", JSONObject().put("targetUserId", owner.user.userId.toString()).put("enabled", false), session), "validation_failed")
        assertError(dispatch("command.auth.changeUserRole", JSONObject().put("targetUserId", owner.user.userId.toString()).put("roleCode", "manager"), session), "validation_failed")

        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(75, names.size)
        assertTrue(names.containsAll(listOf(
            "query.users", "query.securityAudit", "command.auth.changeOwnPassphrase", "command.auth.recoverOwner",
            "command.auth.beginUserEnrollment", "command.auth.resumeUserEnrollment", "command.auth.completeEnrollment",
            "command.auth.changeUserRole", "command.auth.setUserEnabled", "command.auth.beginCredentialReset",
        )))
    }

    @Test
    fun packagedAppCompletesPrivateStaffSetupAndShowsSecurityHistory() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val loginName = "ui.staff.${System.currentTimeMillis()}"
        val displayName = "UI Staff ${System.currentTimeMillis()}"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('access-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('access-nav').click(); true")
            waitFor(webView, "document.getElementById('new-user-form') !== null", 20_000L)
            assertEquals("true", evaluate(webView, "document.getElementById('security-account-tab') !== null && document.getElementById('security-audit-tab') !== null"))
            setInput(webView, "new-user-display-name", displayName)
            setInput(webView, "new-user-login-name", loginName)
            setSelect(webView, "new-user-role", "cashier")
            evaluate(webView, "document.getElementById('begin-user-enrollment-button').click(); true")
            waitFor(webView, "document.getElementById('private-setup-ready') !== null", 20_000L)
            evaluate(webView, "document.getElementById('private-setup-ready').click(); true")
            setInput(webView, "private-passphrase", UI_STAFF_PASSWORD)
            setInput(webView, "private-confirm-passphrase", UI_STAFF_PASSWORD)
            evaluate(webView, "document.getElementById('complete-enrollment-button').click(); true")
            waitFor(webView, "document.getElementById('private-setup-ready') === null && document.body.innerText.includes('can now sign in')", 20_000L)
            assertFalse(app.database.appUserDao().findByLoginName(loginName)!!.credentialSetupRequired)
            evaluate(webView, "document.getElementById('security-audit-tab').click(); true")
            waitFor(webView, "document.getElementById('security-audit-list') !== null")
            assertEquals("true", evaluate(webView, "document.body.innerText.includes('App account created') && document.body.innerText.includes(${JSONObject.quote(displayName)})"))
            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('recover-owner-link') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
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
        instrumentation.runOnMainSync { webView.evaluateJavascript(script) { result.set(it); latch.countDown() } }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    private companion object {
        const val OWNER_PASSWORD = "staff security owner private passphrase 2026"
        const val NEW_OWNER_PASSWORD = "changed owner private passphrase for 2026"
        const val RECOVERED_PASSWORD = "recovered owner private passphrase for 2026"
        const val ADMIN_PASSWORD = "staff security administrator private passphrase 2026"
        const val CASHIER_PASSWORD = "staff security cashier private passphrase 2026"
        const val MANAGER_PASSWORD = "staff security manager private passphrase 2026"
        const val UI_STAFF_PASSWORD = "private cashier passphrase entered by staff 2026"
    }
}
