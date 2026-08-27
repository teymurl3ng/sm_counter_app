package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AccountLedgerService
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3NativeExpansionTest {
    private val database: MilkShopDatabase = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun customerLedger_reusesCanonicalMovements_andExposesCorrectionRelationship() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity(name = "Vendor: Ledger Source", type = "Liability", category = "Vendor")
        )
        val customerId = database.accountDao().insert(
            AccountEntity(name = "Customer: Ledger Test", type = "Asset", category = "Customer")
        )
        val cashId = database.accountDao().findByCategory("Cash/Bank")
            .single { it.name == "Cash" }.accountId

        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2099-01-01", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!

        val originalSale = SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = "2099-01-02",
                channel = "credit-retail",
                unitRate = 190.0,
                quantity = 3.0,
                payment = SalePayment.OnCredit(customerId),
            )
        )

        CorrectionService(database).correct(
            CorrectionInput.Sale(
                originalEventId = originalSale.eventId,
                occurredAt = "2099-01-02",
                unitRate = 190.0,
                quantity = 4.0,
                channel = "credit-retail",
            )
        )

        CreditCollectionService(database).record(
            CreditCollectionInput(
                customerAccountId = customerId,
                paymentAccountId = cashId,
                amount = 200.0,
                occurredAt = "2099-01-03",
            )
        )

        val ledger = AccountLedgerService(database).get(customerId)

        assertEquals("Ledger Test", ledger.displayName)
        assertEquals("Customer", ledger.category)
        assertEquals(560.0, ledger.currentBalance, 1e-9)
        assertEquals(3, ledger.entries.size)

        val original = ledger.entries[0]
        val correction = ledger.entries[1]
        val collection = ledger.entries[2]

        assertEquals(570.0, original.movementAmount, 1e-9)
        assertTrue(original.isSuperseded)
        assertFalse(original.isCorrection)

        assertEquals(190.0, correction.movementAmount, 1e-9)
        assertTrue(correction.isCorrection)
        assertFalse(correction.isSuperseded)
        assertEquals(original.eventId, correction.correctsEventId)

        assertEquals(-200.0, collection.movementAmount, 1e-9)
        assertEquals(560.0, collection.balanceAfter, 1e-9)
    }

    @Test
    fun accountLedger_rejectsNonCustomerVendorAccounts() = runBlocking {
        val cashId = database.accountDao().findByCategory("Cash/Bank")
            .single { it.name == "Cash" }.accountId
        var failed = false
        try {
            AccountLedgerService(database).get(cashId)
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }
}
