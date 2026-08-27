package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SalaryPaymentInput
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase2TransactionTest {
    private val database: MilkShopDatabase = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun vendorPayment_updates_vendor_and_cash_balances() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity( name = "Vendor: Test", type = "Liability", category = "Vendor")
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )

        VendorPaymentService(database).record(
            VendorPaymentInput(vendorId, cashId, 500.0, "2026-08-19")
        )

        val vendorBalance = database.readModelDao().accountBalances().single { it.accountId == vendorId }.balance
        val cashBalance = database.readModelDao().accountBalances().single { it.accountId == cashId }.balance
        assertEquals(17076.92, vendorBalance, 1e-9)
        assertEquals(-500.0, cashBalance, 1e-9)
    }

    @Test
    fun yogurtProduction_moves_measured_quantities() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity(name = "Vendor: Test", type = "Liability", category = "Vendor")
        )
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        YogurtProductionService(database).record(
            YogurtProductionInput("2026-08-19", 17.0, 14.5)
        )

        val stock = database.readModelDao().currentStock()
        assertEquals(83.0, stock.single { it.name == "Milk" }.quantityOnHand, 1e-9)
        assertEquals(14.5, stock.single { it.name == "Yogurt" }.quantityOnHand, 1e-9)
    }

    @Test
    fun sale_by_quantity_calculates_value_and_surcharge() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity(name = "Vendor: Test", type = "Liability", category = "Vendor")
        )
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        val result = SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = "2026-08-19",
                channel = "retail",
                unitRate = 190.0,
                quantity = 5.0,
                payment = SalePayment.Account(cashId),
                surchargeEnabled = true,
            )
        )

        assertEquals(5.0, result.quantity, 0.0)
        assertEquals(950.0, result.value, 1e-9)
        assertEquals(50.0, result.surcharge, 1e-9)
        assertEquals(1000.0, result.total, 1e-9)
        assertEquals(95.0, database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand, 1e-9)
        assertEquals(1000.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
    }

    @Test
    fun sale_by_value_on_credit_creates_customer_receivable() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity(name = "Vendor: Test", type = "Liability", category = "Vendor")
        )
        val customerId = database.accountDao().insert(
            AccountEntity(name = "Customer: Test", type = "Asset", category = "Customer")
        )
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!

        val result = SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = "2026-08-19",
                channel = "credit-retail",
                unitRate = 190.0,
                value = 570.0,
                payment = SalePayment.OnCredit(customerId),
            )
        )

        assertEquals(3.0, result.quantity, 1e-9)
        assertEquals(570.0, result.total, 1e-9)
        assertEquals(570.0, database.readModelDao().accountBalances().single { it.accountId == customerId }.balance, 1e-9)
    }

    @Test
    fun creditCollection_moves_receivable_to_cash() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity(name = "Vendor: Test", type = "Liability", category = "Vendor")
        )
        val customerId = database.accountDao().insert(
            AccountEntity(name = "Customer: Test", type = "Asset", category = "Customer")
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        SaleService(database).record(
            SaleInput(milk.productId, "2026-08-19", "credit-retail", 190.0, value = 570.0, payment = SalePayment.OnCredit(customerId))
        )

        CreditCollectionService(database).record(
            CreditCollectionInput(customerId, cashId, 570.0, "2026-08-19")
        )

        assertEquals(0.0, database.readModelDao().accountBalances().single { it.accountId == customerId }.balance, 1e-9)
        assertEquals(570.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
    }

    @Test
    fun expense_moves_cash_to_category_bucket() = runBlocking {
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        ExpenseService(database).record(
            ExpenseInput("Fuel", cashId, 300.0, "2026-08-19")
        )

        assertEquals(-300.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
        assertEquals(300.0, database.readModelDao().accountBalances().single { it.name == "Expense: Fuel" }.balance, 1e-9)
    }

    @Test
    fun salary_moves_cash_to_employee_account() = runBlocking {
        val employeeId = database.accountDao().insert(
            AccountEntity(name = "Salary: Ahmed", type = "Expense", category = "Salary")
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        SalaryPaymentService(database).record(
            SalaryPaymentInput(employeeId, cashId, 1000.0, "2026-08-19")
        )

        assertEquals(-1000.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
        assertEquals(1000.0, database.readModelDao().accountBalances().single { it.accountId == employeeId }.balance, 1e-9)
    }

    @Test
    fun sale_rolls_back_event_when_stock_insert_fails() = runBlocking {
        val milk = database.productDao().findByName("Milk")!!
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        var failed = false
        try {
            SaleService(database).record(
                SaleInput(
                    productId = milk.productId,
                    occurredAt = "2026-08-19",
                    channel = "retail",
                    unitRate = 190.0,
                    quantity = 1.0,
                    payment = SalePayment.Account(cashId),
                )
            )
        } catch (_: Exception) {
            failed = true
        }

        assertTrue(failed)
        assertNull(database.eventDao().findById(1L))
        assertEquals(emptyList<com.saaf.milkshop.data.InventoryLogEntity>(), database.inventoryLogDao().findByEventId(1L))
        assertEquals(0.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
    }

    @Test
    fun production_rolls_back_when_milk_stock_is_insufficient() = runBlocking {
        var failed = false
        try {
            YogurtProductionService(database).record(
                YogurtProductionInput("2026-08-19", 1.0, 0.85)
            )
        } catch (_: Exception) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(0.0, database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand, 1e-9)
        assertEquals(0.0, database.readModelDao().currentStock().single { it.name == "Yogurt" }.quantityOnHand, 1e-9)
    }

    @Test
    fun sale_rejects_both_quantity_and_value() = runBlocking {
        val milk = database.productDao().findByName("Milk")!!
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        var failed = false
        try {
            SaleService(database).record(
                SaleInput(
                    productId = milk.productId,
                    occurredAt = "2026-08-19",
                    channel = "retail",
                    unitRate = 190.0,
                    quantity = 1.0,
                    value = 190.0,
                    payment = SalePayment.Account(cashId),
                )
            )
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }
}
