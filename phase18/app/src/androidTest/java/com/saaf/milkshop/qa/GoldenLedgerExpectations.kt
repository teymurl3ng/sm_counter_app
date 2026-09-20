package com.saaf.milkshop.qa

import java.util.Properties

/** Captured totals shared by the instrumented golden regression. */
object GoldenLedgerExpectations {
    private val props: Properties by lazy {
        val loader = checkNotNull(GoldenLedgerExpectations::class.java.classLoader) {
            "Missing class loader for golden ledger expectations"
        }
        val stream = requireNotNull(
            loader.getResourceAsStream("golden_ledger_expectations.properties"),
        ) { "Missing golden_ledger_expectations.properties on androidTest classpath" }
        Properties().apply { stream.use { load(it) } }
    }

    val date: String get() = props.getProperty("date")
    val milkStockAfter: Double get() = props.getProperty("milk_stock_after").toDouble()
    val customerBalanceAfter: Double get() = props.getProperty("customer_balance_after").toDouble()
    val cashBalanceAfter: Double get() = props.getProperty("cash_balance_after").toDouble()
    val fuelExpenseBalanceAfter: Double get() = props.getProperty("fuel_expense_balance_after").toDouble()
    val firstCashSaleTotal: Double get() = props.getProperty("first_cash_sale_total").toDouble()
}
