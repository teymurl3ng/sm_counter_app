package com.saaf.milkshop.data

import com.saaf.milkshop.domain.AnalyticsPeriodSummary
import com.saaf.milkshop.domain.AnalyticsService

/**
 * Read-side data orchestration for the Dashboard presentation layer.
 *
 * This class owns database/read-model access and analytics-service construction;
 * it does not define business rules or calculations.
 */
class DashboardDataRepository(
    private val database: MilkShopDatabase,
) : DashboardDataSource {
    override suspend fun loadSnapshot(): DashboardDataSource.Snapshot {
        val stocks = database.readModelDao().currentStock()
        val balances = database.readModelDao().accountBalances()

        val milk = stocks.firstOrNull { it.name == "Milk" }?.quantityOnHand ?: 0.0
        val yogurt = stocks.firstOrNull { it.name == "Yogurt" }?.quantityOnHand ?: 0.0

        val cash = balances.firstOrNull { it.name == "Cash" }?.balance ?: 0.0
        val jazzCash = balances.firstOrNull { it.name == "JazzCash" }?.balance ?: 0.0
        val bank = balances.firstOrNull { it.name == "Bank" }?.balance ?: 0.0

        return DashboardDataSource.Snapshot(
            milkStock = milk,
            yogurtStock = yogurt,
            cashBalance = cash,
            jazzCashBalance = jazzCash,
            bankBalance = bank,
            vendorBalances = balances
                .filter { it.category == "Vendor" }
                .map { DashboardDataSource.Row(it.name, it.category, it.balance) },
            customerBalances = balances
                .filter { it.category == "Customer" }
                .map { DashboardDataSource.Row(it.name, it.category, it.balance) },
        )
    }

    override suspend fun loadAnalyticsSummary(
        startDate: String,
        endDateExclusive: String,
    ): AnalyticsPeriodSummary = AnalyticsService(database)
        .periodSummary(startDate, endDateExclusive)
}
