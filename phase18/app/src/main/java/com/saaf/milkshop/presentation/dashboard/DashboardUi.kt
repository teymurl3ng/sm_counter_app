package com.saaf.milkshop.presentation.dashboard

import android.app.Activity
import android.content.SharedPreferences
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.saaf.milkshop.domain.AnalyticsPeriodSummary
import com.saaf.milkshop.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

class DashboardUi(
    private val activity: Activity,
    private val uiScope: CoroutineScope,
    private val preferences: SharedPreferences,
    private val formatMoney: (Double) -> String,
    private val formatQuantity: (Double, String) -> String,
    private val viewModel: DashboardViewModel,
) {
    private val milkStockText = activity.findViewById<TextView>(R.id.milkStockText)
    private val yogurtStockText = activity.findViewById<TextView>(R.id.yogurtStockText)
    private val cashBalanceText = activity.findViewById<TextView>(R.id.cashBalanceText)
    private val jazzCashBalanceText = activity.findViewById<TextView>(R.id.jazzCashBalanceText)
    private val bankBalanceText = activity.findViewById<TextView>(R.id.bankBalanceText)
    private val vendorBalancesText = activity.findViewById<TextView>(R.id.vendorBalancesText)
    private val customerBalancesText = activity.findViewById<TextView>(R.id.customerBalancesText)
    private val analyticsPeriodLabelText = activity.findViewById<TextView>(R.id.analyticsPeriodLabelText)
    private val analyticsTodayButton = activity.findViewById<Button>(R.id.analyticsTodayButton)
    private val analyticsWeekButton = activity.findViewById<Button>(R.id.analyticsWeekButton)
    private val analyticsMonthButton = activity.findViewById<Button>(R.id.analyticsMonthButton)
    private val analyticsYearButton = activity.findViewById<Button>(R.id.analyticsYearButton)
    private val analyticsSalesCardText = activity.findViewById<TextView>(R.id.analyticsSalesCardText)
    private val analyticsOperationsCardText = activity.findViewById<TextView>(R.id.analyticsOperationsCardText)
    private val analyticsCashflowCardText = activity.findViewById<TextView>(R.id.analyticsCashflowCardText)
    private val analyticsProductionCardText = activity.findViewById<TextView>(R.id.analyticsProductionCardText)
    private val analyticsDashboardErrorText = activity.findViewById<TextView>(R.id.analyticsDashboardErrorText)

    private enum class AnalyticsPeriod { TODAY, WEEK, MONTH, YEAR }

    fun setup() {
        analyticsTodayButton.setOnClickListener { selectAnalyticsPeriod(AnalyticsPeriod.TODAY) }
        analyticsWeekButton.setOnClickListener { selectAnalyticsPeriod(AnalyticsPeriod.WEEK) }
        analyticsMonthButton.setOnClickListener { selectAnalyticsPeriod(AnalyticsPeriod.MONTH) }
        analyticsYearButton.setOnClickListener { selectAnalyticsPeriod(AnalyticsPeriod.YEAR) }
        viewModel.selectPeriod(loadPreferredAnalyticsPeriod().toViewModelPeriod())
    }

    fun reloadPreferredAnalyticsPeriod() {
        viewModel.selectPeriod(loadPreferredAnalyticsPeriod().toViewModelPeriod())
    }

    suspend fun refreshDashboard() {
        viewModel.refreshSnapshot()
        renderState()
        refreshAnalyticsDashboard(viewModel.state.period.toDashboardPeriod())
    }

    private fun loadPreferredAnalyticsPeriod(): AnalyticsPeriod = when (
        preferences.getString("default_reporting_period", "month")
    ) {
        "today" -> AnalyticsPeriod.TODAY
        "week" -> AnalyticsPeriod.WEEK
        "year" -> AnalyticsPeriod.YEAR
        else -> AnalyticsPeriod.MONTH
    }

    private fun selectAnalyticsPeriod(period: AnalyticsPeriod) {
        viewModel.selectPeriod(period.toViewModelPeriod())
        uiScope.launch {
            refreshAnalyticsDashboard(period)
        }
    }

    private suspend fun refreshAnalyticsDashboard(period: AnalyticsPeriod) {
        val today = com.saaf.milkshop.sync.BusinessClock.today()
        val (start, end, _) = when (period) {
            AnalyticsPeriod.TODAY -> Triple(today, today.plusDays(1), activity.getString(R.string.analytics_period_today))
            AnalyticsPeriod.WEEK -> {
                val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
                Triple(startOfWeek, startOfWeek.plusDays(7), activity.getString(R.string.analytics_period_week))
            }
            AnalyticsPeriod.MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                Triple(startOfMonth, startOfMonth.plusMonths(1), activity.getString(R.string.analytics_period_month))
            }
            AnalyticsPeriod.YEAR -> {
                val startOfYear = today.withDayOfYear(1)
                Triple(startOfYear, startOfYear.plusYears(1), activity.getString(R.string.analytics_period_year))
            }
        }

        viewModel.refreshAnalytics(start.toString(), end.toString())
        renderState()
    }

    private fun renderAnalyticsSummary(summary: AnalyticsPeriodSummary) {
        analyticsSalesCardText.text = activity.getString(
            R.string.analytics_sales_card,
            formatMoney(summary.salesValue),
            formatQuantity(summary.saleQuantity, "units"),
        )
        analyticsOperationsCardText.text = activity.getString(
            R.string.analytics_operations_card,
            formatMoney(summary.purchaseAmount),
            formatMoney(summary.vendorPayments),
            formatMoney(summary.creditCollections),
        )
        analyticsCashflowCardText.text = activity.getString(
            R.string.analytics_cashflow_card,
            formatMoney(summary.operatingExpenses),
            formatMoney(summary.salaryPayments),
        )
        analyticsProductionCardText.text = activity.getString(
            R.string.analytics_production_card,
            summary.purchasedMilkLiters.formatNumber(),
            summary.productionMilkUsedLiters.formatNumber(),
            summary.producedYogurtKg.formatNumber(),
        )
    }

    private fun renderState() {
        val current = viewModel.state
        milkStockText.text = formatQuantity(current.milkStock, "L")
        yogurtStockText.text = formatQuantity(current.yogurtStock, "kg")
        cashBalanceText.text = formatMoney(current.cashBalance)
        jazzCashBalanceText.text = formatMoney(current.jazzCashBalance)
        bankBalanceText.text = formatMoney(current.bankBalance)
        vendorBalancesText.text = if (current.vendorBalances.isEmpty()) {
            activity.getString(R.string.no_vendor_accounts)
        } else {
            current.vendorBalances.joinToString("\n") { "${it.name}: ${formatMoney(it.balance)}" }
        }
        customerBalancesText.text = if (current.customerBalances.isEmpty()) {
            activity.getString(R.string.no_customer_accounts)
        } else {
            current.customerBalances.joinToString("\n") { "${it.name}: ${formatMoney(it.balance)}" }
        }

        current.analyticsSummary?.let(::renderAnalyticsSummary)
        analyticsDashboardErrorText.text = activity.getString(R.string.analytics_dashboard_error)
        analyticsDashboardErrorText.visibility = if (current.analyticsError) View.VISIBLE else View.GONE

        val start = current.startDate
        val endExclusive = current.endDateExclusive
        if (start != null && endExclusive != null) {
            val label = when (current.period) {
                DashboardViewModel.Period.TODAY -> activity.getString(R.string.analytics_period_today)
                DashboardViewModel.Period.WEEK -> activity.getString(R.string.analytics_period_week)
                DashboardViewModel.Period.MONTH -> activity.getString(R.string.analytics_period_month)
                DashboardViewModel.Period.YEAR -> activity.getString(R.string.analytics_period_year)
            }
            val endDate = java.time.LocalDate.parse(endExclusive).minusDays(1)
            analyticsPeriodLabelText.text = activity.getString(
                R.string.analytics_report_period,
                label,
                start,
                endDate.toString(),
            )
        }
    }

    private fun AnalyticsPeriod.toViewModelPeriod(): DashboardViewModel.Period = when (this) {
        AnalyticsPeriod.TODAY -> DashboardViewModel.Period.TODAY
        AnalyticsPeriod.WEEK -> DashboardViewModel.Period.WEEK
        AnalyticsPeriod.MONTH -> DashboardViewModel.Period.MONTH
        AnalyticsPeriod.YEAR -> DashboardViewModel.Period.YEAR
    }

    private fun DashboardViewModel.Period.toDashboardPeriod(): AnalyticsPeriod = when (this) {
        DashboardViewModel.Period.TODAY -> AnalyticsPeriod.TODAY
        DashboardViewModel.Period.WEEK -> AnalyticsPeriod.WEEK
        DashboardViewModel.Period.MONTH -> AnalyticsPeriod.MONTH
        DashboardViewModel.Period.YEAR -> AnalyticsPeriod.YEAR
    }

    private fun Double.formatNumber(): String = String.format(Locale.US, "%.2f", this)
}
