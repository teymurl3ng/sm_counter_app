package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.VendorPurchaseCalculator
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase1RoomTest {
    private val database: MilkShopDatabase = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun vendorPurchaseCalculator_matches_spec() {
        val result = VendorPurchaseCalculator.calculate(
            rawLiters = 100.0,
            fatPercentage = 4.0,
            lrReading = 28.0,
            milkRate = 170.0,
            deliveryFee = 11.0,
            tsStandard = 13.0,
        )

        assertEquals(8.6, result.snfPercentage, 1e-9)
        assertEquals(12.6, result.tsPercentage, 1e-9)
        assertEquals(96.92307692307692, result.adjustedLiters, 1e-9)
        assertEquals(17576.923076923078, result.amountOwed, 1e-9)
    }

    @Test
    fun vendorPurchase_is_atomic_and_updates_canonical_views() = runBlocking {
        val vendorId = database.accountDao().insert(
            AccountEntity(
                name = "Vendor: Test Dairy",
                type = "Liability",
                category = "Vendor",
            )
        )

        val result = VendorPurchaseService(database).record(
            VendorPurchaseInput(
                vendorAccountId = vendorId,
                occurredAt = "2026-08-18",
                rawLiters = 100.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
            )
        )

        val event = database.eventDao().findById(result.eventId)
        assertNotNull(event)
        assertEquals(100.0, event!!.rawLiters!!, 0.0)
        assertEquals(result.amountOwed, event.amountOwed!!, 1e-9)

        val stock = database.readModelDao().currentStock()
            .single { it.name == "Milk" }
        assertEquals(100.0, stock.quantityOnHand, 1e-9)

        val vendorBalance = database.readModelDao().accountBalances()
            .single { it.accountId == vendorId }
        assertEquals(17576.92, vendorBalance.balance, 1e-9)

        val audit = database.readModelDao().purchaseAudit(result.eventId)
        assertNotNull(audit)
        assertEquals(result.amountOwed, audit!!.expectedAmountOwed!!, 1e-9)
    }

    @Test
    fun negative_stock_is_rejected_by_database_integrity_trigger() = runBlocking {
        val saleEventId = database.eventDao().insert(
            EventEntity(
                createdAt = "2026-08-18T00:00:00Z",
                occurredAt = "2026-08-18",
                eventType = "SALE",
                saleQuantity = 1.0,
                saleValue = 190.0,
                unitRate = 190.0,
            )
        )
        val milk = database.productDao().findByName("Milk")!!

        var failed = false
        try {
            database.inventoryLogDao().insert(
                InventoryLogEntity(
                    eventId = saleEventId,
                    productId = milk.productId,
                    quantity = -1.0,
                )
            )
        } catch (_: Exception) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(emptyList<InventoryLogEntity>(), database.inventoryLogDao().findByEventId(saleEventId))
    }

    @Test
    fun invalid_vendor_account_is_rejected_before_any_event_is_written() = runBlocking {
        val cash = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }

        val before = database.eventDao().findById(1L)
        assertNull(before)

        var failed = false
        try {
            VendorPurchaseService(database).record(
                VendorPurchaseInput(
                    vendorAccountId = cash.accountId,
                    occurredAt = "2026-08-18",
                    rawLiters = 100.0,
                    fatPercentage = 4.0,
                    lrReading = 28.0,
                )
            )
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
        assertNull(database.eventDao().findById(1L))
    }
}
