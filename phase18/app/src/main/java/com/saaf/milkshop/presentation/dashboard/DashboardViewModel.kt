package com.saaf.milkshop.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.saaf.milkshop.data.DashboardDataSource
import com.saaf.milkshop.domain.AnalyticsPeriodSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation state for Dashboard. No Android views or persistence live here. */
class DashboardViewModel(private val dataRepository: DashboardDataSource) : ViewModel() {
    enum class Period { TODAY, WEEK, MONTH, YEAR }

    data class BalanceRow(val name: String, val category: String?, val balance: Double)

    data class State(
        val period: Period = Period.MONTH,
        val startDate: String? = null,
        val endDateExclusive: String? = null,
        val analyticsSummary: AnalyticsPeriodSummary? = null,
        val analyticsError: Boolean = false,
        val milkStock: Double = 0.0,
        val yogurtStock: Double = 0.0,
        val cashBalance: Double = 0.0,
        val jazzCashBalance: Double = 0.0,
        val bankBalance: Double = 0.0,
        val vendorBalances: List<BalanceRow> = emptyList(),
        val customerBalances: List<BalanceRow> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _state.asStateFlow()
    val state: State get() = _state.value


    suspend fun refreshSnapshot() {
        val snapshot = dataRepository.loadSnapshot()
        setStockAndBalances(
            milkStock = snapshot.milkStock,
            yogurtStock = snapshot.yogurtStock,
            cashBalance = snapshot.cashBalance,
            jazzCashBalance = snapshot.jazzCashBalance,
            bankBalance = snapshot.bankBalance,
            vendorBalances = snapshot.vendorBalances.map { BalanceRow(it.name, it.category, it.balance) },
            customerBalances = snapshot.customerBalances.map { BalanceRow(it.name, it.category, it.balance) },
        )
    }

    suspend fun refreshAnalytics(
        startDate: String,
        endDateExclusive: String,
    ) {
        try {
            val summary = dataRepository.loadAnalyticsSummary(startDate, endDateExclusive)
            setAnalyticsRange(startDate, endDateExclusive)
            setAnalyticsResult(summary)
        } catch (_: Exception) {
            setAnalyticsError()
        }
    }

    fun selectPeriod(period: Period) {
        _state.value = state.copy(period = period, analyticsError = false)
    }

    fun setStockAndBalances(
        milkStock: Double,
        yogurtStock: Double,
        cashBalance: Double,
        jazzCashBalance: Double,
        bankBalance: Double,
        vendorBalances: List<BalanceRow>,
        customerBalances: List<BalanceRow>,
    ) {
        _state.value = state.copy(
            milkStock = milkStock,
            yogurtStock = yogurtStock,
            cashBalance = cashBalance,
            jazzCashBalance = jazzCashBalance,
            bankBalance = bankBalance,
            vendorBalances = vendorBalances,
            customerBalances = customerBalances,
        )
    }

    fun setAnalyticsRange(startDate: String, endDateExclusive: String) {
        _state.value = state.copy(startDate = startDate, endDateExclusive = endDateExclusive)
    }

    fun setAnalyticsResult(summary: AnalyticsPeriodSummary) {
        _state.value = state.copy(analyticsSummary = summary, analyticsError = false)
    }

    fun setAnalyticsError() {
        _state.value = state.copy(analyticsSummary = null, analyticsError = true)
    }
}
