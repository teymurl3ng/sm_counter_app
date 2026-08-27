package com.saaf.milkshop

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
import com.saaf.milkshop.domain.SalaryPaymentInput
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorPurchaseCalculator
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3CorrectionTest {
    private val database: MilkShopDatabase = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun purchaseCorrection_storesCorrectedTruth_and_appliesOnlyDelta() = runBlocking {
        val vendorId = createVendor("Vendor: Purchase Correction")
        val original = VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.5, 28.0)
        )

        val correction = CorrectionService(database).correct(
            CorrectionInput.Purchase(
                originalEventId = original.eventId,
                occurredAt = "2026-08-19",
                rawLiters = 100.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
            )
        )

        val originalEvent = database.eventDao().findById(original.eventId)!!
        val correctedEvent = database.eventDao().findById(correction.eventId)!!
        assertEquals(4.5, originalEvent.fatPercentage!!, 0.0)
        assertEquals(4.0, correctedEvent.fatPercentage!!, 0.0)
        assertEquals(original.eventId, correctedEvent.correctsEventId!!)
        assertEquals(17576.923076923078, correctedEvent.amountOwed!!, 1e-9)

        val correctedFinancial = database.financialLogDao().findByEventId(correction.eventId)
        assertEquals(1, correctedFinancial.size)
        assertEquals(
            correctedEvent.amountOwed!! - originalEvent.amountOwed!!,
            correctedFinancial.single().amount,
            1e-9,
        )

        val stock = database.readModelDao().currentStock().single { it.name == "Milk" }
        assertEquals(100.0, stock.quantityOnHand, 1e-9)
        val vendorBalance = database.readModelDao().accountBalances().single { it.accountId == vendorId }
        assertEquals(17576.92, vendorBalance.balance, 1e-9)
    }

    @Test
    fun productionCorrection_usesMeasuredCorrectedTruth_and_deltaMovements() = runBlocking {
        val vendorId = createVendor("Vendor: Production Correction")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val original = YogurtProductionService(database).record(
            YogurtProductionInput("2026-08-19", 17.0, 14.5)
        )

        val correction = CorrectionService(database).correct(
            CorrectionInput.Production(
                originalEventId = original.eventId,
                occurredAt = "2026-08-19",
                milkLitersUsed = 15.0,
                yogurtKgProduced = 13.0,
            )
        )

        val correctedEvent = database.eventDao().findById(correction.eventId)!!
        assertEquals(15.0, correctedEvent.milkLitersUsed!!, 0.0)
        assertEquals(13.0, correctedEvent.yogurtKgProduced!!, 0.0)

        val correctionLogs = database.inventoryLogDao().findByEventId(correction.eventId)
        val milk = database.productDao().findByName("Milk")!!
        val yogurt = database.productDao().findByName("Yogurt")!!
        assertEquals(2.0, correctionLogs.single { it.productId == milk.productId }.quantity, 1e-9)
        assertEquals(-1.5, correctionLogs.single { it.productId == yogurt.productId }.quantity, 1e-9)

        val stock = database.readModelDao().currentStock()
        assertEquals(85.0, stock.single { it.name == "Milk" }.quantityOnHand, 1e-9)
        assertEquals(13.0, stock.single { it.name == "Yogurt" }.quantityOnHand, 1e-9)
    }

    @Test
    fun saleCorrection_changesStockAndPaymentByDelta() = runBlocking {
        val vendorId = createVendor("Vendor: Sale Correction")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val original = SaleService(database).record(
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

        val correction = CorrectionService(database).correct(
            CorrectionInput.Sale(
                originalEventId = original.eventId,
                occurredAt = "2026-08-19",
                unitRate = 190.0,
                quantity = 3.0,
                surchargeAmount = 0.0,
            )
        )

        val correctedEvent = database.eventDao().findById(correction.eventId)!!
        assertEquals(3.0, correctedEvent.saleQuantity!!, 0.0)
        assertEquals(570.0, correctedEvent.saleValue!!, 1e-9)
        assertEquals(0.0, correctedEvent.surchargeApplied, 1e-9)

        val inventoryDelta = database.inventoryLogDao().findByEventId(correction.eventId).single().quantity
        val financialDelta = database.financialLogDao().findByEventId(correction.eventId).single().amount
        assertEquals(2.0, inventoryDelta, 1e-9)
        assertEquals(-430.0, financialDelta, 1e-9)

        assertEquals(97.0, database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand, 1e-9)
        assertEquals(570.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
    }

    @Test
    fun vendorPaymentCorrection_adjustsBothPayableAndCashByDelta() = runBlocking {
        val vendorId = createVendor("Vendor: Payment Correction")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val original = VendorPaymentService(database).record(
            VendorPaymentInput(vendorId, cashId, 500.0, "2026-08-19")
        )

        val correction = CorrectionService(database).correct(
            CorrectionInput.Monetary(original.eventId, "2026-08-19", 400.0)
        )

        val correctedEvent = database.eventDao().findById(correction.eventId)!!
        assertEquals(400.0, correctedEvent.amount!!, 0.0)
        val logs = database.financialLogDao().findByEventId(correction.eventId)
        assertEquals(2, logs.size)
        assertEquals(100.0, logs.single { it.accountId == vendorId }.amount, 1e-9)
        assertEquals(100.0, logs.single { it.accountId == cashId }.amount, 1e-9)
        assertEquals(17176.92, database.readModelDao().accountBalances().single { it.accountId == vendorId }.balance, 1e-9)
        assertEquals(-400.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
    }

    @Test
    fun creditCollectionCorrection_adjustsReceivableAndCashByDelta() = runBlocking {
        val vendorId = createVendor("Vendor: Collection Correction")
        val customerId = createCustomer("Customer: Collection Correction")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        val originalSale = SaleService(database).record(
            SaleInput(
                milk.productId,
                "2026-08-19",
                "credit-retail",
                190.0,
                quantity = 5.0,
                payment = SalePayment.OnCredit(customerId),
            )
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val original = CreditCollectionService(database).record(
            CreditCollectionInput(customerId, cashId, originalSale.total, "2026-08-19")
        )

        val correction = CorrectionService(database).correct(
            CorrectionInput.Monetary(original.eventId, "2026-08-19", 800.0)
        )

        val logs = database.financialLogDao().findByEventId(correction.eventId)
        assertEquals(2, logs.size)
        assertEquals(150.0, logs.single { it.accountId == customerId }.amount, 1e-9)
        assertEquals(-150.0, logs.single { it.accountId == cashId }.amount, 1e-9)
        assertEquals(150.0, database.readModelDao().accountBalances().single { it.accountId == customerId }.balance, 1e-9)
        assertEquals(800.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
    }

    @Test
    fun expenseCorrection_adjustsCashAndExpenseByDelta() = runBlocking {
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val original = ExpenseService(database).record(
            ExpenseInput("Fuel", cashId, 100.0, "2026-08-19")
        )
        val expenseId = database.accountDao().findByCategory("Operating Expense")
            .single { it.name == "Expense: Fuel" }.accountId

        val correction = CorrectionService(database).correct(
            CorrectionInput.Monetary(original.eventId, "2026-08-19", 75.0)
        )

        val logs = database.financialLogDao().findByEventId(correction.eventId)
        assertEquals(2, logs.size)
        assertEquals(25.0, logs.single { it.accountId == cashId }.amount, 1e-9)
        assertEquals(-25.0, logs.single { it.accountId == expenseId }.amount, 1e-9)
        assertEquals(-75.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
        assertEquals(75.0, database.readModelDao().accountBalances().single { it.accountId == expenseId }.balance, 1e-9)
    }

    @Test
    fun salaryCorrection_adjustsCashAndSalaryByDelta() = runBlocking {
        val employeeId = database.accountDao().insert(
            AccountEntity(name = "Salary: Test", type = "Expense", category = "Salary")
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val original = SalaryPaymentService(database).record(
            SalaryPaymentInput(employeeId, cashId, 1000.0, "2026-08-19")
        )

        val correction = CorrectionService(database).correct(
            CorrectionInput.Monetary(original.eventId, "2026-08-19", 900.0)
        )

        val logs = database.financialLogDao().findByEventId(correction.eventId)
        assertEquals(2, logs.size)
        assertEquals(100.0, logs.single { it.accountId == cashId }.amount, 1e-9)
        assertEquals(-100.0, logs.single { it.accountId == employeeId }.amount, 1e-9)
        assertEquals(-900.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
        assertEquals(900.0, database.readModelDao().accountBalances().single { it.accountId == employeeId }.balance, 1e-9)
    }

    @Test
    fun wrongCorrectionEventType_isRejected_withoutWritingCorrection() = runBlocking {
        val vendorId = createVendor("Vendor: Wrong Type")
        val original = VendorPaymentService(database).let {
            val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            VendorPurchaseService(database).record(
                VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
            )
            it.record(VendorPaymentInput(vendorId, cashId, 500.0, "2026-08-19"))
        }

        val eventCountBefore = database.eventDao().count()
        var failed = false
        try {
            CorrectionService(database).correct(
                CorrectionInput.Sale(
                    originalEventId = original.eventId,
                    occurredAt = "2026-08-19",
                    unitRate = 190.0,
                    quantity = 1.0,
                )
            )
        } catch (_: Exception) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(eventCountBefore, database.eventDao().count())
        assertTrue(database.eventDao().findById(original.eventId) != null)
    }

    @Test
    fun moneyOnlyEvents_storeTheirAmountOnTheEventHeader() = runBlocking {
        val vendorId = createVendor("Vendor: Amount Header")
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val vendorPayment = VendorPaymentService(database).record(
            VendorPaymentInput(vendorId, cashId, 500.0, "2026-08-19")
        )
        val expense = ExpenseService(database).record(
            ExpenseInput("Fuel", cashId, 100.0, "2026-08-19")
        )

        assertEquals(500.0, database.eventDao().findById(vendorPayment.eventId)!!.amount!!, 0.0)
        assertEquals(100.0, database.eventDao().findById(expense.eventId)!!.amount!!, 0.0)
    }


    @Test
    fun correctionChain_usesCorrectedTruth_notPreviousDelta() = runBlocking {
        val vendorId = createVendor("Vendor: Correction Chain")
        val original = VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.5, 28.0)
        )
        val firstCorrection = CorrectionService(database).correct(
            CorrectionInput.Purchase(original.eventId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val secondCorrection = CorrectionService(database).correct(
            CorrectionInput.Purchase(firstCorrection.eventId, "2026-08-19", 100.0, 4.2, 28.0)
        )

        val firstEvent = database.eventDao().findById(firstCorrection.eventId)!!
        val secondEvent = database.eventDao().findById(secondCorrection.eventId)!!
        assertEquals(original.eventId, firstEvent.correctsEventId!!)
        assertEquals(firstCorrection.eventId, secondEvent.correctsEventId!!)

        val expectedSecond = VendorPurchaseCalculator.calculate(
            100.0, 4.2, 28.0,
            firstEvent.milkRateUsed!!,
            firstEvent.deliveryFeeUsed!!,
            firstEvent.tsStandardUsed!!,
        )
        assertEquals(expectedSecond.amountOwed, secondEvent.amountOwed!!, 1e-9)

        val finalVendor = database.readModelDao().accountBalances().single { it.accountId == vendorId }.balance
        assertEquals(17896.0, finalVendor, 1e-9)
    }

    @Test
    fun correctionThatWouldCreateNegativeStock_rollsBackEntireCorrection() = runBlocking {
        val vendorId = createVendor("Vendor: Correction Rollback")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-19", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val original = SaleService(database).record(
            SaleInput(
                milk.productId,
                "2026-08-19",
                "retail",
                190.0,
                quantity = 5.0,
                payment = SalePayment.Account(cashId),
            )
        )

        val eventCountBefore = database.eventDao().count()
        var failed = false
        try {
            CorrectionService(database).correct(
                CorrectionInput.Sale(
                    originalEventId = original.eventId,
                    occurredAt = "2026-08-19",
                    unitRate = 190.0,
                    quantity = 150.0,
                )
            )
        } catch (_: Exception) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(eventCountBefore, database.eventDao().count())
        assertEquals(95.0, database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand, 1e-9)
        assertEquals(950.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
    }

    private suspend fun createVendor(name: String): Long =
        database.accountDao().insert(AccountEntity(name = name, type = "Liability", category = "Vendor"))

    private suspend fun createCustomer(name: String): Long =
        database.accountDao().insert(AccountEntity(name = name, type = "Asset", category = "Customer"))
}
