package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
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
class Phase8ExpenseOperationsTest {
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
    fun cashierGetsLimitedReferencesAndRecordsCanonicalExpenseWithActorAudit() = runBlocking {
        val owner = security.bootstrapOwner("expense.owner", "Expense Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("expense.cashier", "Expense Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("expense.cashier", CASHIER_PASSWORD.toCharArray())
        val category = database.expenseCategoryProfileDao().all().single { it.displayName == "Electricity" }
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        val references = dispatch("query.transactionReferences", session = cashier.sessionId).getJSONObject("data")
        val categoryNames = (0 until references.getJSONArray("expenseCategories").length()).map {
            references.getJSONArray("expenseCategories").getJSONObject(it).getString("displayName")
        }
        assertTrue(categoryNames.contains("Electricity"))
        assertTrue(references.getJSONArray("paymentAccounts").length() > 0)
        assertError(dispatch("query.financialOverview", dateRange(), cashier.sessionId), "permission_denied")

        val response = dispatch("command.recordExpense", expensePayload(category.accountId, cashId), cashier.sessionId)
        assertTrue(response.toString(), response.getBoolean("ok"))
        val eventId = response.getJSONObject("data").getString("eventId").toLong()
        val movements = database.financialLogDao().findByEventId(eventId).associate { it.accountId to it.amount }
        assertEquals(-125.50, movements.getValue(cashId), 0.000001)
        assertEquals(125.50, movements.getValue(category.accountId), 0.000001)
        val actor = database.eventActorLinkDao().find(eventId)
        assertEquals("Expense Cashier", actor?.actorNameSnapshot)
        assertTrue(database.securityAuditDao().all().any {
            it.action == OperationsBridgeCommandService.ACTION_RECORD_EXPENSE &&
                it.actorRoleSnapshot == "cashier" &&
                it.details == "event_id=$eventId;expense_category_account_id=${category.accountId};payment_account_id=$cashId"
        })

        val overview = dispatch("query.financialOverview", dateRange(), owner.session.sessionId).getJSONObject("data")
        assertEquals("12550", overview.getJSONObject("totals").getString("operatingExpensesPaisa"))
    }

    @Test
    fun expenseRejectsBadAmountWrongAccountsAndArchivedCategoryWithoutPosting() = runBlocking {
        val owner = security.bootstrapOwner("invalid.expense.owner", "Invalid Expense Owner", OWNER_PASSWORD.toCharArray())
        val category = database.expenseCategoryProfileDao().all().single { it.displayName == "Fuel" }
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        assertError(dispatch("command.recordExpense", expensePayload(category.accountId, cashId).put("amountPaisa", "0"), owner.session.sessionId), "validation_failed")
        assertError(dispatch("command.recordExpense", expensePayload(999_999L, cashId), owner.session.sessionId), "not_found")
        assertError(dispatch("command.recordExpense", expensePayload(category.accountId, category.accountId), owner.session.sessionId), "not_found")
        ExpenseCategoryManagementService(database).setArchived(category.accountId, true)
        assertError(dispatch("command.recordExpense", expensePayload(category.accountId, cashId), owner.session.sessionId), "not_found")
        val references = dispatch("query.transactionReferences", session = owner.session.sessionId).getJSONObject("data")
        assertFalse((0 until references.getJSONArray("expenseCategories").length()).any {
            references.getJSONArray("expenseCategories").getJSONObject(it).getString("accountId") == category.accountId.toString()
        })
        assertFalse(database.eventDao().findAllDescending().any { it.eventType == "EXPENSE" })
    }

    @Test
    fun expenseRejectsOperatorAndClosedHistory() = runBlocking {
        val owner = security.bootstrapOwner("closed.expense.owner", "Closed Expense Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("expense.operator", "Expense Operator", "operator"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("expense.operator", OPERATOR_PASSWORD.toCharArray())
        val category = database.expenseCategoryProfileDao().all().single { it.displayName == "Water" }
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        assertError(dispatch("command.recordExpense", expensePayload(category.accountId, cashId), operator.sessionId), "permission_denied")
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, BUSINESS_DATE, 0.0))
        DailyCloseService(database).closeAuthenticated(
            owner.session.sessionId,
            security,
            VerifiedDailyCloseInput(BUSINESS_DATE, count.count.countId),
        )
        assertError(
            dispatch("command.recordExpense", expensePayload(category.accountId, cashId).put("occurredAt", EARLIER_DATE), owner.session.sessionId),
            "day_closed",
        )
        assertFalse(database.eventDao().findAllDescending().any { it.eventType == "EXPENSE" })
    }

    @Test
    fun packagedDailyWorkScreenRecordsExpenseThroughRoom() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val marker = "Device expense ${System.currentTimeMillis()}"
        val categoryName = "Device Expense ${System.currentTimeMillis()}"
        val categoryId = ExpenseCategoryManagementService(app.database).create(categoryName)
        val cashId = app.database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val expenseDate = LocalDate.now().plusYears(5).toString()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('operations-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('operations-nav').click(); true")
            waitFor(webView, "document.getElementById('expense-work-tab') !== null", 15_000L)
            evaluate(webView, "document.getElementById('expense-work-tab').click(); true")
            waitFor(webView, "document.getElementById('record-expense-button') !== null")
            setSelect(webView, "expense-category", categoryId.toString())
            setSelect(webView, "expense-payment-account", cashId.toString())
            setInput(webView, "expense-amount", "875.25")
            setInput(webView, "expense-date", expenseDate)
            setTextArea(webView, "expense-notes", marker)
            evaluate(webView, "document.getElementById('record-expense-button').click(); true")
            waitFor(webView, "document.getElementById('expense-result') !== null", 20_000L)

            val event = app.database.eventDao().findAllDescending().single { it.notes == marker }
            assertEquals("EXPENSE", event.eventType)
            val movements = app.database.financialLogDao().findByEventId(event.eventId).associate { it.accountId to it.amount }
            assertEquals(-875.25, movements.getValue(cashId), 0.000001)
            assertEquals(875.25, movements.getValue(categoryId), 0.000001)
            assertNotNull(app.database.eventActorLinkDao().find(event.eventId))
            assertTrue(evaluate(webView, "document.getElementById('expense-result').innerText.includes('PKR 875.25') && document.getElementById('expense-result').innerText.includes(${JSONObject.quote(categoryName)})") == "true")
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

    private fun expensePayload(categoryId: Long, paymentAccountId: Long) = JSONObject()
        .put("expenseCategoryAccountId", categoryId.toString())
        .put("paymentAccountId", paymentAccountId.toString())
        .put("amountPaisa", "12550")
        .put("occurredAt", BUSINESS_DATE)
        .put("notes", "Recorded expense")

    private fun dateRange() = JSONObject()
        .put("startDateInclusive", BUSINESS_DATE)
        .put("endDateExclusive", "2026-08-29")

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
        const val BUSINESS_DATE = "2026-08-28"
        const val EARLIER_DATE = "2026-08-27"
        const val OWNER_PASSWORD = "expense owner private passphrase 2026"
        const val CASHIER_PASSWORD = "expense cashier private passphrase 2026"
        const val OPERATOR_PASSWORD = "expense operator private passphrase 2026"
    }
}
