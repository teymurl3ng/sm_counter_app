package com.saaf.milkshop.data

import android.app.Application
import com.saaf.milkshop.domain.AccountLedgerService
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * C2 money-core: correction ledger behavior on the REAL schema (Robolectric,
 * framework driver). The float-equality gate (`financialDelta != 0.0`) is
 * what decides whether a correction writes ledger rows: a correction whose
 * corrected truth exactly equals the original is legitimate and writes NONE;
 * any real difference writes exact delta rows and never touches the
 * original's rows. All expectations are hand-computed literals.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CorrectionTest {

    @Test
    fun equalFloatCorrection_writesNoLedgerRows() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val customer = seedCustomerWithSale(database, quantity = 2.0, rate = 25.5)

            // Hand-computed: corrected value 51.0 == original 51.0, surcharge
            // 0.0 == 0.0 -> total delta 0.0; inventory -2.0 == -2.0. The gate
            // skips both rows; the correction event still records the truth.
            val correction = CorrectionService(database).correct(
                CorrectionInput.Sale(
                    originalEventId = customer.second,
                    occurredAt = "2026-10-07",
                    unitRate = 25.5,
                    value = 51.0,
                ),
            )

            val corrEvent = database.eventDao().findById(correction.eventId)!!
            assertEquals("SALE", corrEvent.eventType)
            assertEquals(51.0, corrEvent.saleValue!!, 0.0)
            assertEquals(2.0, corrEvent.saleQuantity!!, 0.0)
            assertEquals(0.0, corrEvent.surchargeApplied!!, 0.0)
            assertTrue(database.financialLogDao().findByEventId(correction.eventId).isEmpty())
            assertTrue(database.inventoryLogDao().findByEventId(correction.eventId).isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun realCorrection_writesExactDeltaRows_originalsUntouched() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val (customer, saleId) = seedCustomerWithSale(database, quantity = 3.0, rate = 25.5)
            val originalFinancial = database.financialLogDao().findByEventId(saleId).single()

            // Hand-computed: corrected 2.5 x 25.5 = 63.75 vs original 76.5
            // -> financial delta -12.75; inventory -2.5 - (-3.0) = +0.5.
            val correction = CorrectionService(database).correct(
                CorrectionInput.Sale(
                    originalEventId = saleId,
                    occurredAt = "2026-10-07",
                    unitRate = 25.5,
                    quantity = 2.5,
                ),
            )

            val corrEvent = database.eventDao().findById(correction.eventId)!!
            assertEquals(2.5, corrEvent.saleQuantity!!, 0.0)
            assertEquals(63.75, corrEvent.saleValue!!, 0.0)
            assertEquals(0.0, corrEvent.surchargeApplied!!, 0.0)

            val corrFinancial = database.financialLogDao().findByEventId(correction.eventId)
            assertEquals(1, corrFinancial.size)
            assertEquals(customer, corrFinancial.single().accountId)
            assertEquals(-12.75, corrFinancial.single().amount, 0.0)

            val corrInventory = database.inventoryLogDao().findByEventId(correction.eventId)
            assertEquals(1, corrInventory.size)
            assertEquals(0.5, corrInventory.single().quantity, 0.0)

            // The original's own rows are immutable history: still exactly one
            // 76.5 movement owned by the original event.
            val originalRows = database.financialLogDao().findByEventId(saleId)
            assertEquals(1, originalRows.size)
            assertEquals(76.5, originalRows.single().amount, 0.0)
            assertEquals(originalFinancial.logId, originalRows.single().logId)

            // The customer ledger read view shows both movements in booking
            // order with running balances: 76.5 then 63.75.
            val ledger = AccountLedgerService(database).get(customer)
            assertEquals(
                listOf(76.5, -12.75),
                ledger.entries.map { it.movementAmount },
            )
            assertEquals(
                listOf(76.5, 63.75),
                ledger.entries.map { it.balanceAfter },
            )
            assertTrue(ledger.entries.last().isCorrection)
        } finally {
            database.close()
        }
    }

    @Test
    fun negativeSurcharge_rejected() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val (_, saleId) = seedCustomerWithSale(database, quantity = 1.0, rate = 25.5)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    CorrectionService(database).correct(
                        CorrectionInput.Sale(
                            originalEventId = saleId,
                            occurredAt = "2026-10-07",
                            unitRate = 25.5,
                            quantity = 1.0,
                            surchargeAmount = -1.0,
                        ),
                    )
                }
            }
        } finally {
            database.close()
        }
    }

    private suspend fun seedCustomerWithSale(
        database: MilkShopDatabase,
        quantity: Double,
        rate: Double,
    ): Pair<Long, Long> {
        val customer = database.accountDao().insert(
            AccountEntity(name = "Customer: Spike", type = "Asset", category = "Customer"),
        )
        val product = database.productDao().insert(
            ProductEntity(name = "Spike Milk", unit = "Liters"),
        )
        val stockEvent = database.eventDao().insert(
            EventEntity(
                createdAt = "2026-10-06T06:00:00Z",
                occurredAt = "2026-10-06",
                eventType = "OPENING_STATE",
            ),
        )
        database.inventoryLogDao().insert(
            InventoryLogEntity(eventId = stockEvent, productId = product, quantity = 10.0),
        )
        val sale = SaleService(database).record(
            SaleInput(
                productId = product,
                occurredAt = "2026-10-06",
                channel = "credit-retail",
                unitRate = rate,
                quantity = quantity,
                payment = SalePayment.OnCredit(customer),
            ),
        )
        return customer to sale.eventId
    }
}
