package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.commands.EmployeeBridgeCommandService
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
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
class Phase8EmployeeAttendanceTest {
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
    fun ownerMaintainsEmployeeProfile_withoutCreatingLoginOrEditableBalance() = runBlocking {
        val owner = security.bootstrapOwner("employee.owner", "Employee Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        val created = dispatch("command.createEmployee", JSONObject().put("name", "Ayesha Worker"), session)
        assertTrue(created.getBoolean("ok"))
        val accountId = created.getJSONObject("data").getString("entityId")

        assertTrue(dispatch("command.saveEmployeeProfile", employeeProfilePayload(accountId, true), session).getBoolean("ok"))
        assertTrue(dispatch("command.renameEmployee", JSONObject().put("accountId", accountId).put("name", "Ayesha Supervisor"), session).getBoolean("ok"))

        val employee = employeeFromMasterData(session, accountId)
        assertEquals("Ayesha Supervisor", employee.getString("name"))
        assertEquals("0", employee.getString("canonicalBalancePaisa"))
        assertEquals("Shift lead", employee.getString("roleLabel"))
        assertEquals("4500000", employee.getString("referenceSalaryPaisa"))
        assertEquals("monthly", employee.getString("salaryPeriodCode"))
        assertEquals("2026-08-01", employee.getString("joinDate"))
        assertEquals(1, database.appUserDao().all().size)

        assertTrue(dispatch("command.saveEmployeeProfile", employeeProfilePayload(accountId, false), session).getBoolean("ok"))
        assertFalse(employeeFromMasterData(session, accountId).getBoolean("active"))
        val inactiveAttendance = dispatch("command.recordAttendance", attendancePayload(accountId, "2026-08-28", "present"), session)
        assertFalse(inactiveAttendance.getBoolean("ok"))
        assertEquals("validation_failed", inactiveAttendance.getJSONObject("error").getString("code"))
        assertTrue(dispatch("command.saveEmployeeProfile", employeeProfilePayload(accountId, true), session).getBoolean("ok"))

        val duplicate = dispatch("command.createEmployee", JSONObject().put("name", "ayesha supervisor"), session)
        assertFalse(duplicate.getBoolean("ok"))
        assertEquals("conflict", duplicate.getJSONObject("error").getString("code"))
        val missing = dispatch("command.saveEmployeeProfile", employeeProfilePayload("9223372036854775807", true), session)
        assertFalse(missing.getBoolean("ok"))
        assertEquals("not_found", missing.getJSONObject("error").getString("code"))

        val audits = database.securityAuditDao().all()
        assertTrue(audits.any { it.action == EmployeeBridgeCommandService.ACTION_CREATE_EMPLOYEE })
        assertTrue(audits.any { it.action == EmployeeBridgeCommandService.ACTION_RENAME_EMPLOYEE })
        assertTrue(audits.count { it.action == EmployeeBridgeCommandService.ACTION_SAVE_EMPLOYEE_PROFILE } >= 3)
    }

    @Test
    fun operatorRecordsAndRevisesAttendance_butCannotMaintainEmployeeProfiles() = runBlocking {
        val owner = security.bootstrapOwner("attendance.owner", "Attendance Owner", OWNER_PASSWORD.toCharArray())
        val accountId = dispatch("command.createEmployee", JSONObject().put("name", "Bilal Operator Test"), owner.session.sessionId)
            .getJSONObject("data").getString("entityId")
        assertTrue(dispatch("command.saveEmployeeProfile", employeeProfilePayload(accountId, true), owner.session.sessionId).getBoolean("ok"))
        val pending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("attendance.operator", "Attendance Operator", "operator"))
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("attendance.operator", OPERATOR_PASSWORD.toCharArray())

        val references = dispatch("query.transactionReferences", session = operator.sessionId)
        assertTrue(references.getBoolean("ok"))
        val employees = references.getJSONObject("data").getJSONArray("employees")
        assertTrue((0 until employees.length()).map { employees.getJSONObject(it).getString("name") }.contains("Bilal Operator Test"))

        assertTrue(dispatch("command.recordAttendance", attendancePayload(accountId, "2026-08-28", "present", "On time"), operator.sessionId).getBoolean("ok"))
        assertTrue(dispatch("command.recordAttendance", attendancePayload(accountId, "2026-08-28", "half_day", "Left after lunch"), operator.sessionId).getBoolean("ok"))
        val current = dispatch("query.attendance", JSONObject().put("businessDate", "2026-08-28"), operator.sessionId).getJSONArray("data")
        assertEquals(1, current.length())
        assertEquals("Bilal Operator Test", current.getJSONObject(0).getString("employeeName"))
        assertEquals("half_day", current.getJSONObject(0).getString("statusCode"))
        assertEquals(2, database.attendanceDao().history(accountId.toLong()).size)
        assertTrue(database.attendanceDao().history(accountId.toLong()).first().supersedesId != null)

        val renameDenied = dispatch("command.renameEmployee", JSONObject().put("accountId", accountId).put("name", "Forbidden Rename"), operator.sessionId)
        assertFalse(renameDenied.getBoolean("ok"))
        assertEquals("permission_denied", renameDenied.getJSONObject("error").getString("code"))
        val audit = database.securityAuditDao().all().filter { it.action == EmployeeBridgeCommandService.ACTION_RECORD_ATTENDANCE }
        assertEquals(2, audit.size)
        assertTrue(audit.all { it.actorNameSnapshot == "Attendance Operator" && it.actorRoleSnapshot == "operator" })
    }

    @Test
    fun cashierCannotReadOrRecordAttendance() = runBlocking {
        val owner = security.bootstrapOwner("cashier.attendance.owner", "Cashier Attendance Owner", OWNER_PASSWORD.toCharArray())
        val accountId = dispatch("command.createEmployee", JSONObject().put("name", "Protected Employee"), owner.session.sessionId)
            .getJSONObject("data").getString("entityId")
        assertTrue(dispatch("command.saveEmployeeProfile", employeeProfilePayload(accountId, true), owner.session.sessionId).getBoolean("ok"))
        val pending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("attendance.cashier", "Attendance Cashier", "cashier"))
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("attendance.cashier", CASHIER_PASSWORD.toCharArray())

        val readDenied = dispatch("query.attendance", JSONObject().put("businessDate", "2026-08-28"), cashier.sessionId)
        val recordDenied = dispatch("command.recordAttendance", attendancePayload(accountId, "2026-08-28", "present"), cashier.sessionId)
        listOf(readDenied, recordDenied).forEach {
            assertFalse(it.getBoolean("ok"))
            assertEquals("permission_denied", it.getJSONObject("error").getString("code"))
        }
        assertTrue(database.attendanceDao().history(accountId.toLong()).isEmpty())
    }

    @Test
    fun packagedEmployeesScreen_createsProfileAndRevisesAttendanceThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val employeeName = "Device Employee ${System.currentTimeMillis()}"
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('employees-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('employees-nav').click(); true")
            waitFor(webView, "document.getElementById('add-employee-button') !== null")

            evaluate(webView, "document.getElementById('add-employee-button').click(); true")
            waitFor(webView, "document.getElementById('employee-name') !== null")
            assertEquals("true", evaluate(webView, "document.getElementById('employee-balance') === null"))
            assertEquals("true", evaluate(webView, "document.getElementById('employee-login-name') === null"))
            setInput(webView, "employee-name", employeeName)
            setInput(webView, "employee-role", "Milk handler")
            setInput(webView, "employee-phone", "03005550999")
            setInput(webView, "employee-reference-salary", "42000")
            setInput(webView, "employee-join-date", "2026-08-01")
            evaluate(webView, "document.getElementById('save-employee-button').click(); true")
            waitFor(webView, "document.getElementById('native-employee-list')?.innerText.includes(${JSONObject.quote(employeeName)}) === true", 15_000L)

            val account = app.database.accountDao().findByCategory("Salary").single { it.name == "Employee: $employeeName" }
            val accountId = account.accountId
            val profile = app.database.employeeProfileDao().findByAccountId(accountId)
            assertNotNull(profile)
            assertEquals("Milk handler", profile!!.role)
            assertEquals(42000.0, profile.referenceSalary!!, 0.000001)
            assertEquals("2026-08-01", profile.joinDate)

            evaluate(webView, "document.getElementById('attendance-tab').click(); true")
            waitFor(webView, "document.getElementById('save-attendance-$accountId') !== null", 15_000L)
            setSelect(webView, "attendance-status-$accountId", "half_day")
            setInput(webView, "attendance-notes-$accountId", "Device half day")
            evaluate(webView, "document.getElementById('save-attendance-$accountId').click(); true")
            waitFor(webView, "document.getElementById('save-attendance-$accountId')?.innerText === 'Revise'", 15_000L)
            setSelect(webView, "attendance-status-$accountId", "absent")
            setInput(webView, "attendance-notes-$accountId", "Revised on device")
            evaluate(webView, "document.getElementById('save-attendance-$accountId').click(); true")
            waitFor(webView, "document.getElementById('native-attendance-list')?.innerText.includes('Already recorded') === true", 15_000L)

            val history = app.database.attendanceDao().history(accountId)
            assertEquals(2, history.size)
            assertEquals("absent", history.first().status)
            assertEquals("Revised on device", history.first().notes)
            assertTrue(history.last().archived)

            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
    }

    private suspend fun employeeFromMasterData(session: String, accountId: String): JSONObject {
        val employees = dispatch("query.masterData", session = session).getJSONObject("data").getJSONArray("employees")
        return (0 until employees.length()).map { employees.getJSONObject(it) }.single { it.getString("accountId") == accountId }
    }

    private fun employeeProfilePayload(accountId: String, active: Boolean): JSONObject = JSONObject()
        .put("accountId", accountId).put("roleLabel", "Shift lead").put("phone", "03001234567")
        .put("referenceSalaryPaisa", "4500000").put("salaryPeriodCode", "monthly")
        .put("joinDate", "2026-08-01").put("active", active)

    private fun attendancePayload(accountId: String, date: String, status: String, notes: String? = null): JSONObject =
        JSONObject().put("employeeAccountId", accountId).put("businessDate", date).put("statusCode", status).apply {
            if (notes != null) put("notes", notes)
        }

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

    companion object {
        const val OWNER_PASSWORD = "employee attendance owner passphrase 2026"
        const val OPERATOR_PASSWORD = "employee attendance operator passphrase 2026"
        const val CASHIER_PASSWORD = "employee attendance cashier passphrase 2026"
    }
}
