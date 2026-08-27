package com.saaf.milkshop.domain

import com.saaf.milkshop.data.AccountBalanceView
import com.saaf.milkshop.data.AnalyticsPeriodRow
import com.saaf.milkshop.data.AnalyticsBudgetActualRow
import com.saaf.milkshop.data.CurrentStockView
import com.saaf.milkshop.data.MilkShopDatabase

/**
 * Read-only analytics foundation for reporting layers built above the
 * transaction core. It derives metrics from canonical event/read-model truth
 * and never writes accounting state.
 */
data class AnalyticsPeriodSummary(
    val startDateInclusive: String,
    val endDateExclusive: String,
    val eventCount: Long,
    val salesValue: Double,
    val saleQuantity: Double,
    val purchaseAmount: Double,
    val purchasedMilkLiters: Double,
    val producedYogurtKg: Double,
    val productionMilkUsedLiters: Double,
    val vendorPayments: Double,
    val creditCollections: Double,
    val operatingExpenses: Double,
    val salaryPayments: Double,
)


data class AnalyticsBudgetActuals(
    val milkPurchaseAmount: Double,
    val salesValue: Double,
    val milkSalesValue: Double,
    val yogurtSalesValue: Double,
    val creditCollections: Double,
    val salaryPayments: Double,
    val expenseByCategory: Map<String, Double>,
)

data class AnalyticsCurrentPosition(
    val stock: List<CurrentStockView>,
    val accountBalances: List<AccountBalanceView>,
)

class AnalyticsService(
    private val database: MilkShopDatabase,
) {
    suspend fun periodSummary(
        startDateInclusive: String,
        endDateExclusive: String,
    ): AnalyticsPeriodSummary {
        require(startDateInclusive < endDateExclusive) {
            "Analytics period start must be before its end"
        }

        val row = database.analyticsReadDao().periodSummary(
            startDateInclusive = startDateInclusive,
            endDateExclusive = endDateExclusive,
        )

        return AnalyticsPeriodSummary(
            startDateInclusive = startDateInclusive,
            endDateExclusive = endDateExclusive,
            eventCount = row.eventCount,
            salesValue = row.salesValue,
            saleQuantity = row.saleQuantity,
            purchaseAmount = row.purchaseAmount,
            purchasedMilkLiters = row.purchasedMilkLiters,
            producedYogurtKg = row.producedYogurtKg,
            productionMilkUsedLiters = row.productionMilkUsedLiters,
            vendorPayments = row.vendorPayments,
            creditCollections = row.creditCollections,
            operatingExpenses = row.operatingExpenses,
            salaryPayments = row.salaryPayments,
        )
    }

    suspend fun budgetActuals(
        startDateInclusive: String,
        endDateExclusive: String,
    ): AnalyticsBudgetActuals {
        require(startDateInclusive < endDateExclusive) {
            "Analytics period start must be before its end"
        }
        val row = database.analyticsReadDao().budgetActuals(startDateInclusive, endDateExclusive)
        return AnalyticsBudgetActuals(
            milkPurchaseAmount = row.milkPurchaseAmount,
            salesValue = row.salesValue,
            milkSalesValue = row.milkSalesValue,
            yogurtSalesValue = row.yogurtSalesValue,
            creditCollections = row.creditCollections,
            salaryPayments = row.salaryPayments,
            expenseByCategory = mapOf(
                "Rent" to row.rent,
                "Electricity" to row.electricity,
                "Internet" to row.internet,
                "Water" to row.water,
                "Fuel" to row.fuel,
                "Poly bags" to row.polyBags,
                "LPG" to row.lpg,
                "Other" to row.other,
            ),
        )
    }

    suspend fun currentPosition(): AnalyticsCurrentPosition =
        AnalyticsCurrentPosition(
            stock = database.readModelDao().currentStock(),
            accountBalances = database.readModelDao().accountBalances(),
        )
}
