package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.CostingPlanningBridgeCommandService
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionService
import com.saaf.milkshop.hybrid.HybridActivity
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase8CostingPlanningTest {
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
    fun planningBridgeVersionsBudgetsAndKeepsCollectionsSeparate() = runBlocking {
        val owner = security.bootstrapOwner("planning.owner", "Planning Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        val first = budgetPayload("2026-09-07", "2026-09-14", "100")
        val firstResult = dispatch("command.upsertBudget", first, session)
        assertTrue(firstResult.getBoolean("ok"))
        assertTrue(firstResult.getJSONObject("data").getLong("salesTargetPaisa") > 0L)
        assertTrue(dispatch("command.upsertBudget", budgetPayload("2026-09-07", "2026-09-14", "120"), session).getBoolean("ok"))
        val copied = dispatch("command.copyPreviousBudget", JSONObject()
            .put("categoryCode", "Milk purchases").put("periodStart", "2026-09-14").put("periodEndExclusive", "2026-09-21"), session)
        assertTrue(copied.toString(), copied.getBoolean("ok"))
        val target = dispatch("command.upsertCollectionsTarget", JSONObject()
            .put("weekStart", "2026-09-07").put("targetAmountPaisa", "40000000"), session)
        assertEquals("40000000", target.getJSONObject("data").getString("targetAmountPaisa"))

        val planning = dispatch("query.planning", session = session).getJSONObject("data")
        val budgets = planning.getJSONArray("budgets")
        assertEquals(3, budgets.length())
        val revisedPeriod = (0 until budgets.length()).map(budgets::getJSONObject).filter { it.getString("periodStart") == "2026-09-07" }
        assertEquals(2, revisedPeriod.size)
        assertTrue(revisedPeriod.any { it.getBoolean("archived") && it.getString("plannedLiters") == "100" })
        val activeBudget = revisedPeriod.single { !it.getBoolean("archived") && it.getString("plannedLiters") == "120" }
        assertEquals("0", activeBudget.getString("actualAmountPaisa"))
        assertEquals((-activeBudget.getLong("plannedAmountPaisa")).toString(), activeBudget.getString("variancePaisa"))
        assertTrue(activeBudget.getLong("salesTargetPaisa") > 0L)
        assertEquals("0", activeBudget.getString("actualSalesPaisa"))
        assertEquals((-activeBudget.getLong("salesTargetPaisa")).toString(), activeBudget.getString("salesVariancePaisa"))
        val savedTarget = planning.getJSONArray("collectionTargets").getJSONObject(0)
        assertEquals("40000000", savedTarget.getString("targetAmountPaisa"))
        assertEquals("0", savedTarget.getString("actualCollectedPaisa"))
        assertEquals("-40000000", savedTarget.getString("variancePaisa"))
        val audits = database.securityAuditDao().all().map { it.action }
        assertTrue(audits.containsAll(listOf(
            CostingPlanningBridgeCommandService.ACTION_UPSERT_BUDGET,
            CostingPlanningBridgeCommandService.ACTION_COPY_BUDGET,
            CostingPlanningBridgeCommandService.ACTION_UPSERT_COLLECTIONS_TARGET,
        )))
        assertError(dispatch("command.upsertBudget", first.put("unexpected", true), session), "invalid_request")
    }

    @Test
    fun costingPolicyAndOpeningCostBasisRoundTripThroughExactBridgeContracts() = runBlocking {
        val owner = security.bootstrapOwner("costing.owner", "Costing Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        StoreIdentityService(database).save(StoreIdentityInput("Costing Test Shop"))
        val milk = database.productDao().findByName("Milk")!!
        val opening = OpeningStateService(database).record(OpeningStateInput("2026-08-27", inventoryBalances = listOf(OpeningInventoryBalance(milk.productId, 10.0))))

        val policy = dispatch("command.approveCostingPolicy", costingPolicyPayload("2026-10"), session)
        assertTrue(policy.toString(), policy.getBoolean("ok"))
        val basis = dispatch("command.recordOpeningCostBasis", JSONObject()
            .put("openingEventId", opening.eventId.toString()).put("productId", milk.productId.toString())
            .put("quantity", "10").put("totalCostPaisa", "150000").put("basisKindCode", "owner_estimate")
            .put("evidenceNotes", "Estimated from the supplier rate used when the shop opened")
            .put("supersedesBasisId", JSONObject.NULL), session)
        assertTrue(basis.toString(), basis.getBoolean("ok"))

        val costing = dispatch("query.costingManagement", session = session).getJSONObject("data")
        assertEquals(5, costing.getJSONObject("codes").getJSONArray("componentCodes").length())
        val savedPolicy = costing.getJSONArray("policies").getJSONObject(0)
        assertEquals("2026-10", savedPolicy.getString("calendarMonth"))
        assertEquals("1000", savedPolicy.getString("conversionCostPaisaPerKg"))
        assertEquals(5, savedPolicy.getJSONArray("components").length())
        val savedBasis = costing.getJSONArray("openingCostBases").getJSONObject(0)
        assertEquals("150000", savedBasis.getString("totalCostPaisa"))
        assertEquals(owner.user.userId.toString(), savedBasis.getString("createdByUserId"))
        assertTrue(database.securityAuditDao().all().map { it.action }.containsAll(listOf(
            CostingPlanningBridgeCommandService.ACTION_APPROVE_COSTING_POLICY,
            CostingPlanningBridgeCommandService.ACTION_RECORD_OPENING_COST_BASIS,
        )))
    }

    @Test
    fun yieldApprovalAndHumanLossReviewAreSeparateAuditedCommands() = runBlocking {
        val owner = security.bootstrapOwner("yield.bridge.owner", "Yield Bridge Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        VendorPurchaseService(database).record(VendorPurchaseInput(vendor(), "2026-07-01", 100.0, 4.0, 28.0))
        val productionIds = (0 until 30).map { offset ->
            YogurtProductionService(database).record(YogurtProductionInput(LocalDate.parse("2026-07-01").plusDays(offset.toLong()).toString(), 1.0, 0.85)).eventId
        }
        val policy = dispatch("command.approveYieldPolicy", JSONObject()
            .put("acceptedProductionEventIds", JSONArray(productionIds.map(Long::toString)))
            .put("effectiveFrom", "2026-07-31").put("supersedesYieldPolicyId", JSONObject.NULL), session)
        assertTrue(policy.toString(), policy.getBoolean("ok"))
        val unusual = YogurtProductionService(database).record(YogurtProductionInput("2026-07-31", 1.0, 0.70)).eventId
        val before = dispatch("query.costingManagement", session = session).getJSONObject("data")
        assertEquals(0, before.getJSONArray("productionLossAssessments").length())
        assertEquals("review_required", findAlert(before, unusual).getString("statusCode"))

        val reviewed = dispatch("command.recordProductionLossAssessment", JSONObject()
            .put("productionEventId", unusual.toString()).put("classificationCode", "abnormal")
            .put("abnormalMilkLiters", "0.2").put("reasonCode", "spill").put("notes", "Measured spill beside vat")
            .put("supersedesAssessmentId", JSONObject.NULL), session)
        assertTrue(reviewed.toString(), reviewed.getBoolean("ok"))
        val after = dispatch("query.costingManagement", session = session).getJSONObject("data")
        val assessment = after.getJSONArray("productionLossAssessments").getJSONObject(0)
        assertEquals(unusual.toString(), assessment.getString("productionEventId"))
        assertEquals("0.2", assessment.getString("abnormalMilkLiters"))
        assertTrue(database.securityAuditDao().all().map { it.action }.containsAll(listOf(
            CostingPlanningBridgeCommandService.ACTION_APPROVE_YIELD_POLICY,
            CostingPlanningBridgeCommandService.ACTION_RECORD_PRODUCTION_LOSS,
        )))
    }

    @Test
    fun costingPlanningRequiresOwnerAuthorityRecentAuthenticationAndAdvertisesAllActions() = runBlocking {
        var now = 8_000_000L
        val sessions = NativeSessionManager { now }
        security = UserSecurityService(database, sessions = sessions, nowEpochMs = { now })
        dispatcher = HybridBridgeDispatcher(database, security)
        val owner = security.bootstrapOwner("guard.owner", "Guard Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(owner.session.sessionId, CreateUserInput("guard.cashier", "Guard Cashier", "cashier"))
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("guard.cashier", CASHIER_PASSWORD.toCharArray())
        assertError(dispatch("query.planning", session = cashier.sessionId), "permission_denied")
        assertError(dispatch("command.upsertCollectionsTarget", JSONObject().put("weekStart", "2026-09-07").put("targetAmountPaisa", "100"), cashier.sessionId), "permission_denied")

        now += NativeSessionManager.RECENT_AUTH_TIMEOUT_MS
        assertError(dispatch("command.approveCostingPolicy", costingPolicyPayload("2026-11"), owner.session.sessionId), "recent_authentication_required")
        assertTrue(dispatch("command.auth.reauthenticate", JSONObject().put("passphrase", OWNER_PASSWORD), owner.session.sessionId).getBoolean("ok"))
        assertTrue(dispatch("command.approveCostingPolicy", costingPolicyPayload("2026-11"), owner.session.sessionId).getBoolean("ok"))
        assertError(dispatch("query.costingManagement", JSONObject().put("extra", true), owner.session.sessionId), "invalid_request")

        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(86, names.size)
        assertTrue(names.containsAll(listOf(
            "query.costingManagement", "query.planning", "command.upsertBudget", "command.copyPreviousBudget",
            "command.upsertCollectionsTarget", "command.approveCostingPolicy", "command.recordOpeningCostBasis",
            "command.approveYieldPolicy", "command.recordProductionLossAssessment",
        )))
    }

    @Test
    fun packagedAppExposesFourOwnerFriendlyCostAndPlanningAreas() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('costing-planning-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('costing-planning-nav').click(); true")
            waitFor(webView, "document.getElementById('budget-plan-form') !== null", 20_000L)
            assertEquals("true", evaluate(webView, "document.getElementById('collections-target-form') !== null"))
            evaluate(webView, "document.getElementById('yogurt-cost-tab').click(); true")
            waitFor(webView, "document.getElementById('costing-policy-form') !== null")
            evaluate(webView, "document.getElementById('opening-cost-tab').click(); true")
            waitFor(webView, "document.getElementById('opening-cost-basis-controls') !== null")
            evaluate(webView, "document.getElementById('yield-review-tab').click(); true")
            waitFor(webView, "document.getElementById('yield-policy-form') !== null")
            assertEquals("true", evaluate(webView, "document.body.innerText.includes('warning, not an automatic loss') || document.body.innerText.includes('never records a loss by itself')"))
            evaluate(webView, "document.getElementById('logout-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null")
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
    }

    private fun budgetPayload(start: String, end: String, liters: String) = JSONObject()
        .put("categoryCode", "Milk purchases").put("periodStart", start).put("periodEndExclusive", end)
        .put("plannedLiters", liters).put("plannedAmountPaisa", JSONObject.NULL)

    private fun costingPolicyPayload(month: String) = JSONObject()
        .put("calendarMonth", month).put("normalCapacityKg", "100").put("reviewReasonCode", "monthly")
        .put("components", JSONArray(listOf(
            component("culture", "1000", "1"), component("packaging", "0", "1"),
            component("variable_energy", "0", "1"), component("direct_labor", "0", "1"),
            component("fixed_production_overhead", "0", "100"),
        ))).put("supersedesPolicyId", JSONObject.NULL)

    private fun component(code: String, amount: String, quantity: String) = JSONObject()
        .put("componentCode", code).put("sourceAmountPaisa", amount).put("basisQuantityKg", quantity)

    private fun findAlert(costing: JSONObject, eventId: Long): JSONObject = (0 until costing.getJSONArray("yieldAlerts").length())
        .map(costing.getJSONArray("yieldAlerts")::getJSONObject).single { it.getString("productionEventId") == eventId.toString() }

    private suspend fun vendor(): Long = database.accountDao().insert(AccountEntity(name = "Vendor: Bridge Cost ${System.nanoTime()}", type = "Liability", category = "Vendor"))

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
        const val OWNER_PASSWORD = "costing planning owner private passphrase 2026"
        const val CASHIER_PASSWORD = "costing planning cashier private passphrase 2026"
    }
}
