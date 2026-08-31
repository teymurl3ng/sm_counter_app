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
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.EmployeeProfileService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
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
class Phase8SalaryPaymentOperationsTest {
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
    fun managerRecordsSalaryWithExactMovementsActorAuditAndFinancialSummary() = runBlocking {
        val owner = security.bootstrapOwner("salary.owner", "Salary Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("salary.manager", "Salary Manager", "manager"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, MANAGER_PASSWORD.toCharArray())
        val manager = security.login("salary.manager", MANAGER_PASSWORD.toCharArray())
        val employeeId = EmployeeManagementService(database).create("Ayesha")
        val cashId = cashAccountId()

        val response = dispatch(
            "command.recordSalaryPayment",
            salaryPayload(employeeId, cashId),
            manager.sessionId,
        )

        assertTrue(response.toString(), response.getBoolean("ok"))
        val eventId = response.getJSONObject("data").getString("eventId").toLong()
        val movements = database.financialLogDao().findByEventId(eventId).associate { it.accountId to it.amount }
        assertEquals(1_350.75, movements.getValue(employeeId), 0.000001)
        assertEquals(-1_350.75, movements.getValue(cashId), 0.000001)
        assertEquals("Salary Manager", database.eventActorLinkDao().find(eventId)?.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_SALARY_PAYMENT &&
                it.actorRoleSnapshot == "manager" &&
                it.details == "event_id=$eventId;employee_account_id=$employeeId;payment_account_id=$cashId"
        })

        val overview = dispatch("query.financialOverview", dateRange(), owner.session.sessionId).getJSONObject("data")
        assertEquals("135075", overview.getJSONObject("totals").getString("salaryPaymentsPaisa"))
    }

    @Test
    fun salaryReferencesExposeOnlyActiveEmployeesToAuthorizedRoles() = runBlocking {
        val owner = security.bootstrapOwner("salary.refs.owner", "Salary Refs Owner", OWNER_PASSWORD.toCharArray())
        val cashierPending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("salary.cashier", "Salary Cashier", "cashier"),
        )
        security.completeUserEnrollment(cashierPending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("salary.cashier", CASHIER_PASSWORD.toCharArray())
        val activeId = EmployeeManagementService(database).create("Active Employee")
        val inactiveId = EmployeeManagementService(database).create("Inactive Employee")
        EmployeeProfileService(database).save(inactiveId, EmployeeProfileInput(active = false))

        val ownerReferences = dispatch("query.transactionReferences", session = owner.session.sessionId).getJSONObject("data")
        val employees = ownerReferences.getJSONArray("employees")
        assertTrue((0 until employees.length()).map(employees::getJSONObject).any { it.getString("accountId") == activeId.toString() })
        assertFalse((0 until employees.length()).map(employees::getJSONObject).any { it.getString("accountId") == inactiveId.toString() })
        assertTrue(ownerReferences.getJSONArray("paymentAccounts").length() > 0)

        val cashierReferences = dispatch("query.transactionReferences", session = cashier.sessionId).getJSONObject("data")
        assertEquals(0, cashierReferences.getJSONArray("employees").length())
    }

    @Test
    fun salaryRejectsUnauthorizedInvalidInactiveAndClosedDayRequestsWithoutPosting() = runBlocking {
        val owner = security.bootstrapOwner("reject.salary.owner", "Reject Salary Owner", OWNER_PASSWORD.toCharArray())
        val cashierPending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("reject.salary.cashier", "Reject Salary Cashier", "cashier"),
        )
        security.completeUserEnrollment(cashierPending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("reject.salary.cashier", CASHIER_PASSWORD.toCharArray())
        val employeeId = EmployeeManagementService(database).create("Inactive Salary Employee")
        val cashId = cashAccountId()

        assertError(dispatch("command.recordSalaryPayment", salaryPayload(employeeId, cashId), cashier.sessionId), "permission_denied")
        assertError(dispatch("command.recordSalaryPayment", salaryPayload(employeeId, cashId).put("amountPaisa", "0"), owner.session.sessionId), "validation_failed")
        assertError(dispatch("command.recordSalaryPayment", salaryPayload(Long.MAX_VALUE, cashId), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordSalaryPayment", salaryPayload(employeeId, employeeId), owner.session.sessionId), "not_found")

        EmployeeProfileService(database).save(employeeId, EmployeeProfileInput(active = false))
        assertError(dispatch("command.recordSalaryPayment", salaryPayload(employeeId, cashId), owner.session.sessionId), "not_found")

        val activeEmployeeId = EmployeeManagementService(database).create("Closed Day Employee")
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )
        assertError(
            dispatch("command.recordSalaryPayment", salaryPayload(activeEmployeeId, cashId).put("occurredAt", EARLIER_DATE), owner.session.sessionId),
            "day_closed",
        )
        assertFalse(database.eventDao().findAllDescending().any { it.eventType == "SALARY_PAYMENT" })
    }

    @Test
    fun packagedSalaryScreenRecordsEmployeePaymentThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val suffix = System.currentTimeMillis()
        val marker = "Device salary payment $suffix"
        val employeeName = "Device Employee $suffix"
        val employeeId = EmployeeManagementService(app.database).create(employeeName)
        val cashId = app.database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val paymentDate = LocalDate.now().plusYears(6).toString()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('operations-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('operations-nav').click(); true")
            waitFor(webView, "document.getElementById('salary-work-tab') !== null", 15_000L)
            evaluate(webView, "document.getElementById('salary-work-tab').click(); true")
            waitFor(webView, "document.getElementById('record-salary-button') !== null")

            setSelect(webView, "salary-employee", employeeId.toString())
            setSelect(webView, "salary-payment-account", cashId.toString())
            setInput(webView, "salary-amount", "1425.50")
            setInput(webView, "salary-date", paymentDate)
            setTextArea(webView, "salary-notes", marker)
            evaluate(webView, "document.getElementById('record-salary-button').click(); true")
            waitFor(webView, "document.getElementById('salary-result') !== null", 20_000L)
            assertTrue(evaluate(webView, "document.getElementById('salary-result').innerText.includes('Salary payment recorded')") == "true")

            val salaryEvent = app.database.eventDao().findAllDescending().single { it.notes == marker }
            val movements = app.database.financialLogDao().findByEventId(salaryEvent.eventId).associate { it.accountId to it.amount }
            assertEquals("SALARY_PAYMENT", salaryEvent.eventType)
            assertEquals(1_425.50, movements.getValue(employeeId), 0.000001)
            assertEquals(-1_425.50, movements.getValue(cashId), 0.000001)
            assertNotNull(app.database.eventActorLinkDao().find(salaryEvent.eventId))

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

    private fun salaryPayload(employeeId: Long, paymentAccountId: Long) = JSONObject()
        .put("employeeAccountId", employeeId.toString())
        .put("paymentAccountId", paymentAccountId.toString())
        .put("amountPaisa", "135075")
        .put("occurredAt", BUSINESS_DATE)
        .put("notes", "Salary payment")

    private fun dateRange() = JSONObject()
        .put("startDateInclusive", BUSINESS_DATE)
        .put("endDateExclusive", "2026-09-01")

    private suspend fun cashAccountId() =
        database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

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
        const val OWNER_PASSWORD = "salary owner private passphrase 2026"
        const val MANAGER_PASSWORD = "salary manager private passphrase 2026"
        const val CASHIER_PASSWORD = "salary cashier private passphrase 2026"
    }
}
