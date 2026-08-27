package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.CollectionsTargetEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.Instant

const val BUDGET_CATEGORY_MILK_PURCHASES = "Milk purchases"
val OPERATING_BUDGET_CATEGORIES = ExpenseService.EXPENSE_CATEGORIES

data class BudgetPlanInput(
    val category: String,
    val periodStart: LocalDate,
    val periodEndExclusive: LocalDate,
    val plannedLiters: Double? = null,
    val plannedAmount: Double? = null,
)

data class BudgetPlanResult(
    val budgetId: Long,
    val plannedAmount: Double,
    val salesTarget: Double?,
)

data class CollectionsTargetResult(
    val targetId: Long,
    val targetAmount: Double,
)

class BudgetManagementService(
    private val database: MilkShopDatabase,
) {
    suspend fun activeBudget(category: String, periodStart: LocalDate): BudgetPlanEntity? =
        database.budgetPlanDao().findActive(category, periodStart.toString())

    suspend fun allBudgets(category: String): List<BudgetPlanEntity> =
        database.budgetPlanDao().findAllForCategory(category)

    suspend fun createOrRevise(input: BudgetPlanInput): BudgetPlanResult =
        database.withWriteTransaction {
            require(input.category == BUDGET_CATEGORY_MILK_PURCHASES || input.category in OPERATING_BUDGET_CATEGORIES) {
                "Unknown budget category"
            }
            require(input.periodStart < input.periodEndExclusive) { "Budget period must have an end after its start" }
            if (input.category == BUDGET_CATEGORY_MILK_PURCHASES) {
                require(input.periodStart.dayOfWeek == java.time.DayOfWeek.MONDAY) { "Milk budgets must start on Monday" }
                require(input.periodEndExclusive == input.periodStart.plusDays(7)) { "Milk budgets must cover exactly one week" }
            } else {
                require(input.periodStart.dayOfMonth == 1) { "Operating-cost budgets must start on the first day of the month" }
                require(input.periodEndExclusive == input.periodStart.plusMonths(1)) { "Operating-cost budgets must cover exactly one month" }
            }
            require(input.periodEndExclusive.isAfter(LocalDate.now())) { "Closed budget periods cannot be created or revised" }

            val existing = database.budgetPlanDao().findActive(input.category, input.periodStart.toString())
            if (existing != null) {
                database.budgetPlanDao().archive(existing.budgetId)
            }

            val settings = BusinessSettingsService(database).get()
            val isMilk = input.category == BUDGET_CATEGORY_MILK_PURCHASES
            if (isMilk) {
                val liters = input.plannedLiters ?: error("Planned milk liters are required")
                require(liters >= 0.0) { "Planned milk liters cannot be negative" }
                val amount = liters * settings.milkRateVendor
                val entity = BudgetPlanEntity(
                    category = input.category,
                    periodStart = input.periodStart.toString(),
                    periodEndExclusive = input.periodEndExclusive.toString(),
                    plannedLiters = liters,
                    plannedAmount = amount,
                    vendorRateSnapshot = settings.milkRateVendor,
                    retailRateSnapshot = settings.retailRateMilk,
                    createdAt = Instant.now().toString(),
                    supersedesId = existing?.budgetId,
                    archived = false,
                )
                val id = database.budgetPlanDao().insert(entity)
                return@withWriteTransaction BudgetPlanResult(id, amount, liters * settings.retailRateMilk)
            }

            val amount = input.plannedAmount ?: error("Planned budget amount is required")
            require(amount >= 0.0) { "Budget amount cannot be negative" }
            val entity = BudgetPlanEntity(
                category = input.category,
                periodStart = input.periodStart.toString(),
                periodEndExclusive = input.periodEndExclusive.toString(),
                plannedAmount = amount,
                createdAt = Instant.now().toString(),
                supersedesId = existing?.budgetId,
                archived = false,
            )
            val id = database.budgetPlanDao().insert(entity)
            BudgetPlanResult(id, amount, null)
        }

    suspend fun copyPreviousPeriod(category: String, periodStart: LocalDate, periodEndExclusive: LocalDate): BudgetPlanResult {
        val previousEnd = periodStart
        val previous = database.budgetPlanDao().findLatestBefore(category, previousEnd.toString())
            ?: error("No previous budget exists for this category")
        return createOrRevise(
            BudgetPlanInput(
                category = category,
                periodStart = periodStart,
                periodEndExclusive = periodEndExclusive,
                plannedLiters = previous.plannedLiters,
                plannedAmount = previous.plannedAmount,
            )
        )
    }

    suspend fun activeSalesTargetForWeek(weekStart: LocalDate): Double? =
        database.budgetPlanDao().findActive(BUDGET_CATEGORY_MILK_PURCHASES, weekStart.toString())
            ?.let { budget ->
                val liters = budget.plannedLiters ?: return@let null
                val rate = budget.retailRateSnapshot ?: return@let null
                liters * rate
            }

    suspend fun activeCollectionsTarget(weekStart: LocalDate): CollectionsTargetEntity? =
        database.collectionsTargetDao().findActive(weekStart.toString())

    suspend fun createOrReviseCollectionsTarget(weekStart: LocalDate, amount: Double): CollectionsTargetResult =
        database.withWriteTransaction {
            require(amount >= 0.0) { "Collections target cannot be negative" }
            require(weekStart.dayOfWeek == java.time.DayOfWeek.MONDAY) { "Collections targets must start on Monday" }
            val end = weekStart.plusDays(7)
            require(end.isAfter(LocalDate.now())) { "Closed collection target periods cannot be revised" }
            val existing = database.collectionsTargetDao().findActive(weekStart.toString())
            if (existing != null) database.collectionsTargetDao().archive(existing.targetId)
            val id = database.collectionsTargetDao().insert(
                CollectionsTargetEntity(
                    weekStart = weekStart.toString(),
                    weekEndExclusive = end.toString(),
                    targetAmount = amount,
                    createdAt = Instant.now().toString(),
                    supersedesId = existing?.targetId,
                    archived = false,
                )
            )
            CollectionsTargetResult(id, amount)
        }

    fun currentWeekStart(today: LocalDate = LocalDate.now()): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

    fun currentMonthStart(today: LocalDate = LocalDate.now()): LocalDate =
        today.withDayOfMonth(1)
}
