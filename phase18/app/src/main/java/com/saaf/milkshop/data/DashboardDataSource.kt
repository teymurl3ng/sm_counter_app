package com.saaf.milkshop.data

import com.saaf.milkshop.domain.AnalyticsPeriodSummary

/**
 * Read-side source consumed by the Dashboard presentation layer.
 * Implementations own persistence/service access; the ViewModel owns presentation state.
 */
interface DashboardDataSource {
    data class Row(
        val name: String,
        val category: String?,
        val balance: Double,
    )

    data class Snapshot(
        val milkStock: Double,
        val yogurtStock: Double,
        val cashBalance: Double,
        val jazzCashBalance: Double,
        val bankBalance: Double,
        val vendorBalances: List<Row>,
        val customerBalances: List<Row>,
    )

    suspend fun loadSnapshot(): Snapshot

    suspend fun loadAnalyticsSummary(
        startDate: String,
        endDateExclusive: String,
    ): AnalyticsPeriodSummary
}
