package com.saaf.milkshop.data

/**
 * Read-only period aggregate returned by the analytics query layer.
 * These values are derived from effective (leaf) events; corrected originals
 * are excluded so a correction chain contributes only its latest truth.
 */
data class AnalyticsPeriodRow(
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


data class AnalyticsBudgetActualRow(
    val milkPurchaseAmount: Double,
    val salesValue: Double,
    val milkSalesValue: Double,
    val yogurtSalesValue: Double,
    val creditCollections: Double,
    val salaryPayments: Double,
    val rent: Double,
    val electricity: Double,
    val internet: Double,
    val water: Double,
    val fuel: Double,
    val polyBags: Double,
    val lpg: Double,
    val other: Double,
)
