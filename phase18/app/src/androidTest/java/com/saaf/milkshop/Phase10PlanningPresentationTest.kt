package com.saaf.milkshop

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.domain.BudgetManagementService
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.hybrid.HybridActivity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase10PlanningPresentationTest {
    @Test
    fun savedMilkPlanShowsNativePlanActualAndSalesComparisons() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val weekStart = LocalDate.now().plusWeeks(4).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
        val saved = BudgetManagementService(app.database).createOrRevise(
            BudgetPlanInput(
                category = "Milk purchases",
                periodStart = weekStart,
                periodEndExclusive = weekStart.plusDays(7),
                plannedLiters = 25.0,
            ),
        )

        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        val driver = HybridWebViewTestDriver.attach(scenario)
        try {
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('costing-planning-nav') !== null", 20_000L)
            driver.evaluate("document.getElementById('costing-planning-nav').click(); true")
            driver.waitFor("document.getElementById('budget-comparison-${saved.budgetId}') !== null", 20_000L)
            assertEquals("true", driver.evaluate("document.getElementById('budget-comparison-${saved.budgetId}').innerText.includes('Actual minus plan')"))
            assertEquals("true", driver.evaluate("document.getElementById('sales-target-${saved.budgetId}').innerText.includes('Sales minus target')"))
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(
                SaafMilkTestRunner.TEST_LOGIN,
                SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray(),
            )
            app.currentSessionId = replacement.sessionId
        }
    }
}
