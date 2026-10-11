package com.saaf.milkshop.presentation.budgets

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.saaf.milkshop.R
import com.saaf.milkshop.data.BudgetDataRepository
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.OPERATING_BUDGET_CATEGORIES
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.AppPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class BudgetUi(
    private val activity: Activity,
    private val budgetRepository: BudgetDataRepository,
    private val uiScope: CoroutineScope,
    private val authorize: suspend (AppPermission) -> Unit,
) {

    /** Refreshes budget presentation after an external data change such as restore. */
    fun refreshAfterExternalDataChange() {
        refreshBudgetSummary()
        refreshBudgetOverview()
    }

    private val budgetPeriodText: TextView = activity.findViewById(R.id.budgetPeriodText)
    private val budgetCategorySpinner: Spinner = activity.findViewById(R.id.budgetCategorySpinner)
    private val budgetPeriodStartInput: EditText = activity.findViewById(R.id.budgetPeriodStartInput)
    private val budgetMilkLitersInput: EditText = activity.findViewById(R.id.budgetMilkLitersInput)
    private val budgetAmountInput: EditText = activity.findViewById(R.id.budgetAmountInput)
    private val saveBudgetButton: Button = activity.findViewById(R.id.saveBudgetButton)
    private val copyPreviousBudgetButton: Button = activity.findViewById(R.id.copyPreviousBudgetButton)
    private val budgetStatusText: TextView = activity.findViewById(R.id.budgetStatusText)
    private val budgetErrorText: TextView = activity.findViewById(R.id.budgetErrorText)
    private val budgetOverviewText: TextView = activity.findViewById(R.id.budgetOverviewText)
    private val budgetSummaryText: TextView = activity.findViewById(R.id.budgetSummaryText)
    private val viewBudgetHistoryButton: Button = activity.findViewById(R.id.viewBudgetHistoryButton)
    private val budgetHistoryText: TextView = activity.findViewById(R.id.budgetHistoryText)
    private val budgetSalesTargetText: TextView = activity.findViewById(R.id.budgetSalesTargetText)
    private val budgetTargetWeekInput: EditText = activity.findViewById(R.id.budgetTargetWeekInput)
    private val budgetCollectionsTargetInput: EditText = activity.findViewById(R.id.budgetCollectionsTargetInput)
    private val saveCollectionsTargetButton: Button = activity.findViewById(R.id.saveCollectionsTargetButton)
    private val budgetCollectionsStatusText: TextView = activity.findViewById(R.id.budgetCollectionsStatusText)

    private fun <T> readableSpinnerAdapter(items: List<T>): ArrayAdapter<T> {
        return object : ArrayAdapter<T>(activity, android.R.layout.simple_spinner_item, items) {
            private fun styleView(view: View): View {
                (view as? TextView)?.apply {
                    setTextColor(activity.getColor(R.color.milk_shop_color_on_surface))
                    textSize = 16f
                    val density = activity.resources.displayMetrics.density
                    val horizontal = (12 * density).toInt()
                    val vertical = (8 * density).toInt()
                    setPadding(horizontal, vertical, horizontal, vertical)
                    setBackgroundColor(activity.getColor(R.color.milk_shop_color_surface))
                }
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                styleView(super.getView(position, convertView, parent))

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                styleView(super.getDropDownView(position, convertView, parent))
        }
    }

    fun setup() {
        val categories = listOf(BUDGET_CATEGORY_MILK_PURCHASES) + OPERATING_BUDGET_CATEGORIES
        budgetCategorySpinner.adapter = readableSpinnerAdapter(categories)

        val today = com.saaf.milkshop.sync.BusinessClock.today()
        budgetPeriodStartInput.setText(today.with(phaseBudgetStart(BUDGET_CATEGORY_MILK_PURCHASES)).toString())
        budgetTargetWeekInput.setText(budgetRepository.currentWeekStart(today).toString())

        budgetCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val category = categories[position]
                val parsed = budgetPeriodStartInput.text.toString().toLocalDateOrNull() ?: com.saaf.milkshop.sync.BusinessClock.today()
                budgetPeriodStartInput.setText(parsed.with(phaseBudgetStart(category)).toString())
                updateBudgetInputsForCategory(category)
                refreshBudgetSummary()
            }
        }

        budgetPeriodStartInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!activity.isFinishing) refreshBudgetSummary()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        budgetTargetWeekInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!activity.isFinishing) refreshTargetSummary()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        saveBudgetButton.setOnClickListener { saveBudgetPlan() }
        copyPreviousBudgetButton.setOnClickListener { copyPreviousBudget() }
        viewBudgetHistoryButton.setOnClickListener { toggleBudgetHistory() }
        saveCollectionsTargetButton.setOnClickListener { saveCollectionsTarget() }

        updateBudgetInputsForCategory(categories.first())
        refreshBudgetSummary()
        refreshTargetSummary()
    }

    private fun phaseBudgetStart(category: String): TemporalAdjuster =
        if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        } else {
            TemporalAdjusters.firstDayOfMonth()
        }

    private fun budgetPeriodEnd(category: String, start: LocalDate): LocalDate =
        if (category == BUDGET_CATEGORY_MILK_PURCHASES) start.plusDays(7) else start.plusMonths(1)

    private fun updateBudgetInputsForCategory(category: String) {
        val isMilk = category == BUDGET_CATEGORY_MILK_PURCHASES
        budgetMilkLitersInput.visibility = if (isMilk) View.VISIBLE else View.GONE
        budgetAmountInput.visibility = if (isMilk) View.GONE else View.VISIBLE
        budgetPeriodText.text = if (isMilk) {
            activity.getString(R.string.budget_period_weekly)
        } else {
            activity.getString(R.string.budget_period_monthly)
        }
        if (isMilk) budgetAmountInput.setText("")
        budgetErrorText.visibility = View.GONE
    }

    private fun saveBudgetPlan() {
        budgetErrorText.visibility = View.GONE
        budgetStatusText.text = ""
        val category = budgetCategorySpinner.selectedItem?.toString() ?: return
        val periodStart = budgetPeriodStartInput.text.toString().toLocalDateOrNull()
        if (periodStart == null) {
            budgetErrorText.text = activity.getString(R.string.budget_error, "Enter period start as YYYY-MM-DD")
            budgetErrorText.visibility = View.VISIBLE
            return
        }
        val end = budgetPeriodEnd(category, periodStart)
        if (!end.isAfter(com.saaf.milkshop.sync.BusinessClock.today())) {
            budgetErrorText.text = activity.getString(R.string.budget_error, "That budget period is already closed")
            budgetErrorText.visibility = View.VISIBLE
            return
        }
        val input = if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
            val liters = budgetMilkLitersInput.text.toString().toDoubleOrNull()
            if (liters == null) {
                budgetErrorText.text = activity.getString(R.string.budget_error, "Enter planned milk liters")
                budgetErrorText.visibility = View.VISIBLE
                return
            }
            BudgetPlanInput(category, periodStart, end, plannedLiters = liters)
        } else {
            val amount = budgetAmountInput.text.toString().toDoubleOrNull()
            if (amount == null) {
                budgetErrorText.text = activity.getString(R.string.budget_error, "Enter planned amount")
                budgetErrorText.visibility = View.VISIBLE
                return
            }
            BudgetPlanInput(category, periodStart, end, plannedAmount = amount)
        }

        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_BUSINESS_SETTINGS)
                budgetRepository.createOrRevise(input)
                withContext(Dispatchers.Main) { budgetStatusText.text = activity.getString(R.string.budget_saved) }
                refreshBudgetSummary()
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    budgetErrorText.text = activity.getString(R.string.budget_error, error.message ?: "Unknown error")
                    budgetErrorText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun copyPreviousBudget() {
        budgetErrorText.visibility = View.GONE
        budgetStatusText.text = ""
        val category = budgetCategorySpinner.selectedItem?.toString() ?: return
        val periodStart = budgetPeriodStartInput.text.toString().toLocalDateOrNull()
        if (periodStart == null) {
            budgetErrorText.text = activity.getString(R.string.budget_error, "Enter period start as YYYY-MM-DD")
            budgetErrorText.visibility = View.VISIBLE
            return
        }
        val end = budgetPeriodEnd(category, periodStart)
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_BUSINESS_SETTINGS)
                budgetRepository.copyPreviousPeriod(category, periodStart, end)
                withContext(Dispatchers.Main) { budgetStatusText.text = activity.getString(R.string.budget_saved) }
                refreshBudgetSummary()
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    budgetErrorText.text = activity.getString(R.string.budget_error, error.message ?: activity.getString(R.string.budget_no_previous))
                    budgetErrorText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveCollectionsTarget() {
        val weekStart = budgetTargetWeekInput.text.toString().toLocalDateOrNull()
        if (weekStart == null) {
            budgetCollectionsStatusText.text = activity.getString(R.string.budget_error, "Enter week start as YYYY-MM-DD")
            return
        }
        val amount = budgetCollectionsTargetInput.text.toString().toDoubleOrNull()
        if (amount == null) {
            budgetCollectionsStatusText.text = activity.getString(R.string.budget_error, "Enter collections target")
            return
        }
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_BUSINESS_SETTINGS)
                budgetRepository.createOrReviseCollectionsTarget(weekStart, amount)
                budgetCollectionsStatusText.text = activity.getString(R.string.budget_collections_saved)
                refreshTargetSummary()
            } catch (error: Exception) {
                budgetCollectionsStatusText.text = activity.getString(R.string.budget_error, error.message ?: "Unable to save collections target")
            }
        }
    }

    private fun refreshBudgetSummary() {
        val category = budgetCategorySpinner.selectedItem?.toString() ?: return
        val start = budgetPeriodStartInput.text.toString().toLocalDateOrNull() ?: return
        val end = budgetPeriodEnd(category, start)
        uiScope.launch {
            try {
                val budget = budgetRepository.activeBudget(category, start)
                val actuals = budgetRepository.budgetActuals(start.toString(), end.toString())
                val actual = when (category) {
                    BUDGET_CATEGORY_MILK_PURCHASES -> actuals.milkPurchaseAmount
                    "Salaries" -> actuals.salaryPayments
                    else -> actuals.expenseByCategory[category] ?: 0.0
                }
                val planned = budget?.plannedAmount
                val text = if (planned == null) {
                    activity.getString(R.string.budget_no_target)
                } else {
                    val variance = actual - planned
                    val pct = if (planned == 0.0) null else (variance / planned) * 100.0
                    buildString {
                        append(activity.getString(R.string.budget_actual_format, formatMoney(actual)))
                        append("\n")
                        append(activity.getString(R.string.budget_variance_format, formatMoney(variance), pct?.let { formatPercent(it) } ?: "—"))
                        if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
                            append("\n")
                            append(activity.getString(R.string.budget_milk_planned_liters, budget.plannedLiters ?: 0.0))
                            append("\nBudget spend: ${formatMoney(planned)}")
                            append("\nSales target from plan: ${formatMoney((budget.plannedLiters ?: 0.0) * (budget.retailRateSnapshot ?: 0.0))}")
                        }
                    }
                }
                budgetSummaryText.text = text
                withContext(Dispatchers.Main) {
                    if (budget?.plannedLiters != null && budgetMilkLitersInput.text.toString() != trimNumber(budget.plannedLiters)) {
                        budgetMilkLitersInput.setText(trimNumber(budget.plannedLiters))
                    } else if (category != BUDGET_CATEGORY_MILK_PURCHASES && planned != null && budgetAmountInput.text.toString() != trimNumber(planned)) {
                        budgetAmountInput.setText(trimNumber(planned))
                    }
                }
                refreshBudgetOverview()
            } catch (error: Exception) {
                budgetSummaryText.text = activity.getString(R.string.budget_error, error.message ?: "Unable to load budget summary")
            }
        }
    }

    private fun refreshBudgetOverview() {
        uiScope.launch {
            try {
                val service = budgetRepository
                val analytics = budgetRepository
                val today = com.saaf.milkshop.sync.BusinessClock.today()
                val rows = mutableListOf<String>()

                val weekStart = service.currentWeekStart(today)
                val weekEnd = weekStart.plusDays(7)
                val milkBudget = service.activeBudget(BUDGET_CATEGORY_MILK_PURCHASES, weekStart)
                val milkActuals = analytics.budgetActuals(weekStart.toString(), weekEnd.toString())
                if (milkBudget == null) {
                    rows += "Milk purchases (this week): No budget set"
                } else {
                    val variance = milkActuals.milkPurchaseAmount - (milkBudget.plannedAmount ?: 0.0)
                    rows += "Milk purchases: ${formatMoney(milkBudget.plannedAmount ?: 0.0)} planned / ${formatMoney(milkActuals.milkPurchaseAmount)} actual / ${formatMoney(variance)} variance"
                }

                val monthStart = today.withDayOfMonth(1)
                val monthEnd = monthStart.plusMonths(1)
                val monthActuals = analytics.budgetActuals(monthStart.toString(), monthEnd.toString())
                for (category in OPERATING_BUDGET_CATEGORIES) {
                    val budget = budgetRepository.activeBudget(category, monthStart)
                    val planned = budget?.plannedAmount
                    val actual = if (category == "Salaries") monthActuals.salaryPayments else monthActuals.expenseByCategory[category] ?: 0.0
                    rows += if (planned == null) {
                        "$category: No budget set"
                    } else {
                        "$category: ${formatMoney(planned)} planned / ${formatMoney(actual)} actual / ${formatMoney(actual - planned)} variance"
                    }
                }
                budgetOverviewText.text = rows.joinToString("\n")
            } catch (error: Exception) {
                budgetOverviewText.text = activity.getString(R.string.budget_error, error.message ?: "Unable to load budget overview")
            }
        }
    }

    private fun toggleBudgetHistory() {
        if (budgetHistoryText.visibility == View.VISIBLE) {
            budgetHistoryText.visibility = View.GONE
            return
        }
        val category = budgetCategorySpinner.selectedItem?.toString() ?: return
        uiScope.launch {
            val budgets = budgetRepository.allBudgets(category)
            budgetHistoryText.text = if (budgets.isEmpty()) {
                activity.getString(R.string.budget_no_target)
            } else {
                budgets.groupBy { it.periodStart }.entries.joinToString("\n\n") { (period, versions) ->
                    buildString {
                        append(period)
                        versions.sortedByDescending { it.budgetId }.forEach { version ->
                            append("\n#${version.budgetId} — ${if (version.archived) "Archived" else "Current"}")
                            append("\nAmount: ${formatMoney(version.plannedAmount ?: 0.0)}")
                            version.plannedLiters?.let { append("\nPlanned liters: ${formatQuantity(it, "L")}") }
                        }
                    }
                }
            }
            budgetHistoryText.visibility = View.VISIBLE
        }
    }

    private fun refreshTargetSummary() {
        val weekStart = budgetTargetWeekInput.text.toString().toLocalDateOrNull() ?: return
        val weekEnd = weekStart.plusDays(7)
        uiScope.launch {
            try {
                val actuals = budgetRepository.budgetActuals(weekStart.toString(), weekEnd.toString())
                val salesTarget = budgetRepository.activeSalesTargetForWeek(weekStart)
                val collectionsTarget = budgetRepository.activeCollectionsTarget(weekStart)
                budgetSalesTargetText.text = if (salesTarget == null) {
                    activity.getString(R.string.budget_no_sales_target)
                } else {
                    val variance = actuals.salesValue - salesTarget
                    val pct = if (salesTarget == 0.0) null else (variance / salesTarget) * 100.0
                    buildString {
                        append(activity.getString(R.string.budget_sales_target, formatMoney(salesTarget)))
                        append("\nActual sales: ${formatMoney(actuals.salesValue)}")
                        append("\nVariance: ${formatMoney(variance)} (${pct?.let { formatPercent(it) } ?: "—"})")
                        append("\n${activity.getString(R.string.budget_combined_revenue, formatMoney(actuals.milkSalesValue + actuals.yogurtSalesValue))}")
                        append("\n${activity.getString(R.string.budget_new_uncollected, formatMoney(actuals.salesValue - actuals.creditCollections))}")
                    }
                }
                if (collectionsTarget == null) {
                    budgetCollectionsStatusText.text = activity.getString(R.string.budget_collections_no_target)
                } else {
                    val variance = actuals.creditCollections - collectionsTarget.targetAmount
                    val pct = if (collectionsTarget.targetAmount == 0.0) null else (variance / collectionsTarget.targetAmount) * 100.0
                    budgetCollectionsStatusText.text = buildString {
                        append(activity.getString(R.string.budget_collections_label))
                        append("\nTarget: ${formatMoney(collectionsTarget.targetAmount)}")
                        append("\nActual: ${formatMoney(actuals.creditCollections)}")
                        append("\nVariance: ${formatMoney(variance)} (${pct?.let { formatPercent(it) } ?: "—"})")
                    }
                    budgetCollectionsTargetInput.setText(trimNumber(collectionsTarget.targetAmount))
                }
            } catch (error: Exception) {
                budgetCollectionsStatusText.text = activity.getString(R.string.budget_error, error.message ?: "Unable to load target summary")
            }
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? = try {
        LocalDate.parse(trim())
    } catch (_: DateTimeParseException) {
        null
    }

    private fun trimNumber(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

    private fun formatMoney(value: Double): String = String.format(Locale.US, "₨ %.2f", value)

    private fun formatQuantity(value: Double, unit: String): String = String.format(Locale.US, "%.2f %s", value, unit)
}
