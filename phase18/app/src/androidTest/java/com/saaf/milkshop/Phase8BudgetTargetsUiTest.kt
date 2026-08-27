package com.saaf.milkshop

import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.domain.BudgetManagementService
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.BusinessSettingsService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RunWith(AndroidJUnit4::class)
class Phase8BudgetTargetsUiTest {

    @Test
    fun milkBudget_snapshotsRates_andDerivesSalesTarget() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val db = app.database
            val settings = BusinessSettingsService(db).get()
            val week = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(1)
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) {
                    it.findViewById<EditText>(R.id.budgetPeriodStartInput).text.isNotBlank()
                }
                scenario.onActivity { activity ->
                    activity.findViewById<EditText>(R.id.budgetPeriodStartInput).setText(week.toString())
                    activity.findViewById<EditText>(R.id.budgetMilkLitersInput).setText("1000")
                    activity.findViewById<Button>(R.id.saveBudgetButton).performClick()
                }
                waitForCondition(scenario) {
                    it.findViewById<TextView>(R.id.budgetStatusText).text.toString()
                        .contains("current version")
                }
                val budget = BudgetManagementService(db)
                    .activeBudget(BUDGET_CATEGORY_MILK_PURCHASES, week)!!
                assertEquals(1000.0, budget.plannedLiters!!, 1e-9)
                assertEquals(settings.milkRateVendor, budget.vendorRateSnapshot!!, 1e-9)
                assertEquals(settings.retailRateMilk, budget.retailRateSnapshot!!, 1e-9)
                assertEquals(1000.0 * settings.milkRateVendor, budget.plannedAmount!!, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun collectionsTarget_isSeparateWeeklyTarget() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val db = app.database
            val week = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(2)
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) {
                    it.findViewById<EditText>(R.id.budgetTargetWeekInput).text.isNotBlank()
                }
                scenario.onActivity { activity ->
                    activity.findViewById<EditText>(R.id.budgetTargetWeekInput).setText(week.toString())
                    activity.findViewById<EditText>(R.id.budgetCollectionsTargetInput).setText("400000")
                    activity.findViewById<Button>(R.id.saveCollectionsTargetButton).performClick()
                }
                waitForCondition(scenario) {
                    it.findViewById<TextView>(R.id.budgetCollectionsStatusText).text.toString()
                        .contains("Target")
                }
                assertEquals(400000.0, BudgetManagementService(db)
                    .activeCollectionsTarget(week)!!.targetAmount, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun revision_preservesPreviousVersion_andUpdatesCurrent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val db = app.database
            val week = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(3)
            val service = BudgetManagementService(db)

            service.createOrRevise(
                BudgetPlanInput(
                    BUDGET_CATEGORY_MILK_PURCHASES,
                    week,
                    week.plusDays(7),
                    plannedLiters = 800.0,
                )
            )
            val first = service.activeBudget(BUDGET_CATEGORY_MILK_PURCHASES, week)!!

            service.createOrRevise(
                BudgetPlanInput(
                    BUDGET_CATEGORY_MILK_PURCHASES,
                    week,
                    week.plusDays(7),
                    plannedLiters = 900.0,
                )
            )
            val current = service.activeBudget(BUDGET_CATEGORY_MILK_PURCHASES, week)!!
            val all = service.allBudgets(BUDGET_CATEGORY_MILK_PURCHASES)
                .filter { it.periodStart == week.toString() }

            assertTrue(current.budgetId != first.budgetId)
            assertEquals(900.0, current.plannedLiters!!, 1e-9)
            assertEquals(2, all.size)
            assertTrue(all.any { it.budgetId == first.budgetId && it.archived })
        }
    }

    private suspend fun waitForCondition(
        scenario: ActivityScenario<MainActivity>,
        timeoutMs: Long = 5000,
        condition: (MainActivity) -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var matched = false
            scenario.onActivity { activity -> matched = condition(activity) }
            if (matched) return
            delay(50)
        }
        throw AssertionError("UI condition was not met within ${timeoutMs}ms")
    }
}
