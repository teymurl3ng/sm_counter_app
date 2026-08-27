package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.OpeningAccountBalance
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3OpeningStateTest {
    private val database = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun storeIdentity_isEditable_andCurrencyRemainsPkr() = runBlocking {
        val service = StoreIdentityService(database)
        service.save(StoreIdentityInput("Saaf Milk", phone = "0300"))
        val updated = service.save(StoreIdentityInput("Saaf Milk Shop", address = "Main Road"))

        assertEquals("Saaf Milk Shop", updated.storeName)
        assertEquals("PKR", updated.currencyCode)
        assertEquals("Main Road", service.get()!!.address)
        assertEquals(1, database.backupDataDao().storeProfiles().size)
    }

    @Test
    fun openingState_postsCanonicalBalances_once_andCorrectionsUseDeltas() = runBlocking {
        StoreIdentityService(database).save(StoreIdentityInput("Opening Test Shop"))
        val customerId = CustomerManagementService(database).create("Opening Customer")
        val vendorId = VendorManagementService(database).create("Opening Vendor")
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val milkId = database.productDao().findByName("Milk")!!.productId
        val service = OpeningStateService(database)

        val original = service.record(
            OpeningStateInput(
                openingDate = "2026-08-27",
                inventoryBalances = listOf(OpeningInventoryBalance(milkId, 50.0)),
                accountBalances = listOf(
                    OpeningAccountBalance(cashId, 1_000.0),
                    OpeningAccountBalance(customerId, 300.0),
                    OpeningAccountBalance(vendorId, 500.0),
                ),
            )
        )
        assertEquals(50.0, stock(milkId), 0.0)
        assertEquals(1_000.0, balance(cashId), 0.0)
        assertEquals(300.0, balance(customerId), 0.0)
        assertEquals(500.0, balance(vendorId), 0.0)

        val corrected = service.correct(
            OpeningStateInput(
                openingDate = "2026-08-27",
                inventoryBalances = listOf(OpeningInventoryBalance(milkId, 40.0)),
                accountBalances = listOf(
                    OpeningAccountBalance(cashId, 1_200.0),
                    OpeningAccountBalance(vendorId, 400.0),
                ),
                notes = "Corrected count",
            )
        )
        assertEquals(original.eventId, database.eventDao().findById(corrected.eventId)!!.correctsEventId)
        assertEquals(-10.0, database.inventoryLogDao().findByEventId(corrected.eventId).single().quantity, 0.0)
        assertEquals(40.0, stock(milkId), 0.0)
        assertEquals(1_200.0, balance(cashId), 0.0)
        assertEquals(0.0, balance(customerId), 0.0)
        assertEquals(400.0, balance(vendorId), 0.0)
        assertEquals(2, service.history().size)
        assertTrue(service.history().single { it.eventId == original.eventId }.superseded)
        assertEquals(corrected.eventId, service.current()!!.eventId)

        var duplicateRejected = false
        try {
            service.record(
                OpeningStateInput("2026-08-27", listOf(OpeningInventoryBalance(milkId, 1.0)))
            )
        } catch (_: IllegalArgumentException) {
            duplicateRejected = true
        }
        assertTrue(duplicateRejected)
    }

    @Test
    fun existingBusinessEvents_blockOpeningState() = runBlocking {
        StoreIdentityService(database).save(StoreIdentityInput("Existing Ledger Shop"))
        val vendorId = VendorManagementService(database).create("Existing Vendor")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-27", 10.0, 4.0, 28.0)
        )
        val milkId = database.productDao().findByName("Milk")!!.productId

        var rejected = false
        try {
            OpeningStateService(database).record(
                OpeningStateInput("2026-08-27", listOf(OpeningInventoryBalance(milkId, 10.0)))
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(0, database.openingStateDao().initialCount())
        assertEquals(10.0, stock(milkId), 0.0)
    }

    private suspend fun stock(productId: Long): Double =
        database.readModelDao().currentStock().single { it.productId == productId }.quantityOnHand

    private suspend fun balance(accountId: Long): Double =
        database.readModelDao().accountBalances().single { it.accountId == accountId }.balance
}
