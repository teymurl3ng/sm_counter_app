package com.saaf.milkshop.data

import android.app.Application
import com.saaf.milkshop.domain.OwnerDrawingInput
import com.saaf.milkshop.domain.OwnerDrawingService
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
 * C2 money-core: OWNER_DRAWING booking on the REAL schema (M18). A drawing
 * leaves the drawer with a single negative movement and must stay OUT of the
 * operating-expense sums — that exclusion is the whole design (a drawing is
 * an equity withdrawal, not an expense). All expectations are literals.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OwnerDrawingTest {

    @Test
    fun drawing_booksLiteralNegativeDrawerRow() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val cash = database.accountDao().findByCategory("Cash/Bank")
                .single { it.name == "Cash" }.accountId

            // Hand-computed: one financial row of exactly -250.0 on Cash.
            val result = OwnerDrawingService(database).record(
                OwnerDrawingInput(amount = 250.0, occurredAt = "2026-10-08", notes = "fuel for the personal run"),
            )

            assertEquals(250.0, result.amount, 0.0)
            val event = database.eventDao().findById(result.eventId)!!
            assertEquals("OWNER_DRAWING", event.eventType)
            assertEquals(250.0, event.amount!!, 0.0)
            assertEquals("fuel for the personal run", event.notes)
            val financial = database.financialLogDao().findByEventId(result.eventId)
            assertEquals(1, financial.size)
            assertEquals(cash, financial.single().accountId)
            assertEquals(-250.0, financial.single().amount, 0.0)
        } finally {
            database.close()
        }
    }

    @Test
    fun drawing_isExcludedFromOperatingExpenseSums() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val customer = database.accountDao().insert(
                AccountEntity(name = "Customer: Spike", type = "Asset", category = "Customer"),
            )
            val product = database.productDao().insert(
                ProductEntity(name = "Spike Milk", unit = "Liters"),
            )
            val stockEvent = database.eventDao().insert(
                EventEntity(
                    createdAt = "2026-10-08T06:00:00Z",
                    occurredAt = "2026-10-08",
                    eventType = "OPENING_STATE",
                ),
            )
            database.inventoryLogDao().insert(
                InventoryLogEntity(eventId = stockEvent, productId = product, quantity = 10.0),
            )
            SaleService(database).record(
                SaleInput(
                    productId = product,
                    occurredAt = "2026-10-08",
                    channel = "credit-retail",
                    unitRate = 25.5,
                    quantity = 2.0,
                    payment = SalePayment.OnCredit(customer),
                ),
            )
            val expensesBefore = database.analyticsReadDao().periodSummary("2026-10-01", "2026-10-09").operatingExpenses

            OwnerDrawingService(database).record(
                OwnerDrawingInput(amount = 250.0, occurredAt = "2026-10-08"),
            )
            database.analyticsReadDao().periodSummary("2026-10-01", "2026-10-09").let { summary ->
                assertEquals(expensesBefore, summary.operatingExpenses, 0.0)
                assertEquals(0.0, summary.operatingExpenses, 0.0)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun drawing_onClosedDay_refused() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            // Close 2026-10-06 the bare way (no app users: actor trigger inert).
            val cash = database.accountDao().findByCategory("Cash/Bank")
                .single { it.name == "Cash" }.accountId
            database.backupDataDao().insertPhysicalCashCount(
                PhysicalCashCountEntity(
                    accountId = cash,
                    businessDate = "2026-10-06",
                    countedAmount = 0.0,
                    countedAt = "2026-10-06T22:00:00Z",
                    uid = "count-drawing-1",
                ),
            )
            val countId = database.syncEnvelopeDao()
                .physicalCashCountsByUid(listOf("count-drawing-1")).single().countId
            database.backupDataDao().insertDailyCloseAction(
                DailyCloseActionEntity(
                    businessDate = "2026-10-06",
                    action = "close",
                    physicalCashCountId = countId,
                    actorName = "sync",
                    actorRole = "owner",
                    createdAt = "2026-10-06T23:00:00Z",
                ),
            )

            val error = assertThrows(Exception::class.java) {
                runBlocking {
                    OwnerDrawingService(database).record(
                        OwnerDrawingInput(amount = 100.0, occurredAt = "2026-10-06"),
                    )
                }
            }
            assertTrue(
                (error.cause?.cause?.message ?: error.message)?.contains("Business date is closed") == true,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun drawing_rejectsInvalidAmountsAndAccounts() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val customer = database.accountDao().insert(
                AccountEntity(name = "Customer: Spike", type = "Asset", category = "Customer"),
            )
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    OwnerDrawingService(database).record(
                        OwnerDrawingInput(amount = 0.0, occurredAt = "2026-10-08"),
                    )
                }
            }
            assertThrows(Exception::class.java) {
                runBlocking {
                    OwnerDrawingService(database).record(
                        OwnerDrawingInput(amount = 100.0, paymentAccountId = customer, occurredAt = "2026-10-08"),
                    )
                }
            }
        } finally {
            database.close()
        }
    }
}
