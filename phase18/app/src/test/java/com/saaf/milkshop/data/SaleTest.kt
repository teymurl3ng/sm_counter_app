package com.saaf.milkshop.data

import android.app.Application
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * C2 money-core: sale booking on the REAL schema (Robolectric, framework
 * driver — see RobolectricDbSpikeTest for the driver-fidelity record).
 *
 * Every expected number below is a hand-computed literal; the suite must
 * never recompute expectations through the code under test (C2 verdict).
 * All fixture numbers are dyadic rationals (25.5, 3.0, 51.0, 10.25 ...) so
 * float equality is exact and the literals are honest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SaleTest {

    @Test
    fun cashSale_booksLiteralLedgerRows() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            // Production always books cash into the seeded "Cash" drawer account.
            val cash = database.accountDao().findByCategory("Cash/Bank")
                .single { it.name == "Cash" }.accountId
            val product = database.productDao().insert(
                ProductEntity(name = "Spike Milk", unit = "Liters"),
            )
            stockProduct(database, product, 10.0)

            // Hand-computed: 3.0 x 25.5 = 76.5; no surcharge; total 76.5.
            val result = SaleService(database).record(
                SaleInput(
                    productId = product,
                    occurredAt = "2026-10-06",
                    channel = "retail",
                    unitRate = 25.5,
                    quantity = 3.0,
                    payment = SalePayment.Account(cash),
                ),
            )

            assertEquals(3.0, result.quantity, 0.0)
            assertEquals(76.5, result.value, 0.0)
            assertEquals(0.0, result.surcharge, 0.0)
            assertEquals(76.5, result.total, 0.0)

            val event = database.eventDao().findById(result.eventId)!!
            assertEquals(3.0, event.saleQuantity!!, 0.0)
            assertEquals(76.5, event.saleValue!!, 0.0)
            assertEquals(25.5, event.unitRate!!, 0.0)
            assertEquals(0.0, event.surchargeApplied!!, 0.0)

            assertEquals(
                listOf(-3.0),
                database.inventoryLogDao().findByEventId(result.eventId).map { it.quantity },
            )
            val financial = database.financialLogDao().findByEventId(result.eventId)
            assertEquals(1, financial.size)
            assertEquals(cash, financial.single().accountId)
            assertEquals(76.5, financial.single().amount, 0.0)
        } finally {
            database.close()
        }
    }

    @Test
    fun creditSale_booksTotalIncludingSurchargeOntoCustomerLedger() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val customer = database.accountDao().insert(
                AccountEntity(name = "Customer: Spike", type = "Asset", category = "Customer"),
            )
            val product = database.productDao().insert(
                ProductEntity(name = "Spike Milk", unit = "Liters"),
            )
            stockProduct(database, product, 10.0)

            // Hand-computed: value 51.0 / rate 25.5 = 2.0; surcharge 10.25;
            // total 61.25 — the surcharge rides the SAME single ledger row.
            val result = SaleService(database).record(
                SaleInput(
                    productId = product,
                    occurredAt = "2026-10-06",
                    channel = "credit-retail",
                    unitRate = 25.5,
                    value = 51.0,
                    payment = SalePayment.OnCredit(customer),
                    surchargeEnabled = true,
                    surchargeAmount = 10.25,
                ),
            )

            assertEquals(2.0, result.quantity, 0.0)
            assertEquals(51.0, result.value, 0.0)
            assertEquals(10.25, result.surcharge, 0.0)
            assertEquals(61.25, result.total, 0.0)

            val event = database.eventDao().findById(result.eventId)!!
            assertEquals(2.0, event.saleQuantity!!, 0.0)
            assertEquals(51.0, event.saleValue!!, 0.0)
            assertEquals(10.25, event.surchargeApplied!!, 0.0)

            assertEquals(
                listOf(-2.0),
                database.inventoryLogDao().findByEventId(result.eventId).map { it.quantity },
            )
            val financial = database.financialLogDao().findByEventId(result.eventId)
            assertEquals(1, financial.size)
            assertEquals(customer, financial.single().accountId)
            assertEquals(61.25, financial.single().amount, 0.0)
        } finally {
            database.close()
        }
    }

    /** Opening stock so sales never trip the negative-stock trigger. */
    private suspend fun stockProduct(database: MilkShopDatabase, productId: Long, quantity: Double) {
        val eventId = database.eventDao().insert(
            EventEntity(
                createdAt = "2026-10-06T06:00:00Z",
                occurredAt = "2026-10-06",
                eventType = "OPENING_STATE",
            ),
        )
        database.inventoryLogDao().insert(
            InventoryLogEntity(eventId = eventId, productId = productId, quantity = quantity),
        )
    }
}
