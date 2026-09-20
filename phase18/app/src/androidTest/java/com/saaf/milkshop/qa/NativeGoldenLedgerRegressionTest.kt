package com.saaf.milkshop.qa

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deterministic golden-ledger regression against EXISTING production services.
 * Expected totals live in phase18/qa/golden_ledger_expectations.properties.
 */
@RunWith(AndroidJUnit4::class)
class NativeGoldenLedgerRegressionTest {
    private val database: MilkShopDatabase = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun goldenScenario_saleCollectionCorrectionExpense_matchCapturedAuthoritativeTotals() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity(name = "Vendor: Golden", type = "Liability", category = "Vendor"),
        )
        val customerId = database.accountDao().insert(
            AccountEntity(name = "Customer: Golden", type = "Asset", category = "Customer"),
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val milk = database.productDao().findByName("Milk")!!
        val date = GoldenLedgerExpectations.date

        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, date, 100.0, 4.0, 28.0),
        )

        val cashSale = SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = date,
                channel = "retail",
                unitRate = 190.0,
                quantity = 5.0,
                payment = SalePayment.Account(cashId),
                surchargeEnabled = true,
            ),
        )
        assertEquals(GoldenLedgerExpectations.firstCashSaleTotal, cashSale.total, 1e-9)

        SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = date,
                channel = "credit-retail",
                unitRate = 190.0,
                value = 570.0,
                payment = SalePayment.OnCredit(customerId),
            ),
        )

        CreditCollectionService(database).record(
            CreditCollectionInput(customerId, cashId, 570.0, date),
        )

        val saleToCorrect = SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = date,
                channel = "retail",
                unitRate = 190.0,
                quantity = 5.0,
                payment = SalePayment.Account(cashId),
                surchargeEnabled = true,
            ),
        )
        CorrectionService(database).correct(
            CorrectionInput.Sale(
                originalEventId = saleToCorrect.eventId,
                occurredAt = date,
                unitRate = 190.0,
                quantity = 3.0,
                surchargeAmount = 0.0,
            ),
        )

        ExpenseService(database).record(
            ExpenseInput("Fuel", cashId, 300.0, date),
        )

        assertEquals(
            GoldenLedgerExpectations.milkStockAfter,
            database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
            1e-9,
        )
        assertEquals(
            GoldenLedgerExpectations.customerBalanceAfter,
            database.readModelDao().accountBalances().single { it.accountId == customerId }.balance,
            1e-9,
        )
        assertEquals(
            GoldenLedgerExpectations.cashBalanceAfter,
            database.readModelDao().accountBalances().single { it.accountId == cashId }.balance,
            1e-9,
        )
        assertEquals(
            GoldenLedgerExpectations.fuelExpenseBalanceAfter,
            database.readModelDao().accountBalances().single { it.name == "Expense: Fuel" }.balance,
            1e-9,
        )
    }
}
