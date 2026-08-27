package com.saaf.milkshop.data

import com.saaf.milkshop.domain.AnalyticsBudgetActuals
import com.saaf.milkshop.domain.AnalyticsService
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.BudgetManagementService
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.BudgetPlanResult
import com.saaf.milkshop.domain.CollectionsTargetResult
import java.time.LocalDate

/**
 * Application-facing boundary for Budget screen read/write orchestration.
 * It composes the existing domain services without changing their authority.
 */
class BudgetDataRepository(
    database: MilkShopDatabase,
) {
    private val budgetService = BudgetManagementService(database)
    private val analyticsService = AnalyticsService(database)

    fun currentWeekStart(today: LocalDate = LocalDate.now()): LocalDate =
        budgetService.currentWeekStart(today)

    suspend fun createOrRevise(input: BudgetPlanInput): BudgetPlanResult =
        budgetService.createOrRevise(input)

    suspend fun copyPreviousPeriod(
        category: String,
        periodStart: LocalDate,
        periodEndExclusive: LocalDate,
    ): BudgetPlanResult = budgetService.copyPreviousPeriod(category, periodStart, periodEndExclusive)

    suspend fun createOrReviseCollectionsTarget(weekStart: LocalDate, amount: Double): CollectionsTargetResult =
        budgetService.createOrReviseCollectionsTarget(weekStart, amount)

    suspend fun activeBudget(category: String, periodStart: LocalDate) =
        budgetService.activeBudget(category, periodStart)

    suspend fun allBudgets(category: String) =
        budgetService.allBudgets(category)

    suspend fun activeSalesTargetForWeek(weekStart: LocalDate): Double? =
        budgetService.activeSalesTargetForWeek(weekStart)

    suspend fun activeCollectionsTarget(weekStart: LocalDate) =
        budgetService.activeCollectionsTarget(weekStart)

    suspend fun budgetActuals(
        startDateInclusive: String,
        endDateExclusive: String,
    ): AnalyticsBudgetActuals = analyticsService.budgetActuals(startDateInclusive, endDateExclusive)
}
