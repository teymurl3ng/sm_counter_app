package com.saaf.milkshop.data

import android.app.Application
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseDayRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseFacade
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * C2 money-core: post-H8/H9 close semantics on the REAL schema (Robolectric,
 * framework driver). Characterizes CURRENT behavior — seal gates, the
 * absorb-and-seal honesty rows, and reopen reversal. (The sync-side
 * closed-day refusal lives in SyncApplyClosedDayRefusalTest: it needs its own
 * Robolectric sandbox — see the note there.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DailyCloseTest {

    /** 2026-10-06 is a Tuesday, 2026-10-08 a Thursday (labels render "Tue 6 Oct"). */

    @Test
    fun close_sealsDay_blocksWrites_reopenRestoresPosting() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val security = seedOwner(database)
            val session = security.login(OWNER_LOGIN, PASSPHRASE.toCharArray())
            val facade = NativeCashCloseFacade(database, security)
            val day = LocalDate.parse("2026-10-08")

            val openContext = facade.load(session.sessionId, day)
            assertTrue(openContext.postingOpen)

            // Zero-variance count: empty ledger expects 0.0, count 0.0.
            val counted = facade.recordCount(
                session.sessionId,
                NativeCashCloseCountRequest(0L, null),
                day,
            )
            assertEquals(0L, counted.count.countedCashPaisa)
            assertEquals(0L, counted.count.expectedCashPaisa)
            assertEquals(0L, counted.count.variancePaisa)

            val closed = facade.closeDay(
                session.sessionId,
                NativeCashCloseDayRequest(counted.count.countId, null),
                day,
            )
            assertEquals(0L, closed.receipt.countedCashPaisa)
            assertEquals(0L, closed.receipt.expectedCashPaisa)
            assertEquals(0L, closed.receipt.variancePaisa)
            assertTrue(closed.receipt.actionId > 0)
            assertFalse(closed.refreshedContext.postingOpen)

            // Post-close gates: counting and closing the sealed day are refused.
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    facade.recordCount(session.sessionId, NativeCashCloseCountRequest(0L, null), day)
                }
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    facade.closeDay(
                        session.sessionId,
                        NativeCashCloseDayRequest(counted.count.countId, null),
                        day,
                    )
                }
            }

            // Reopen gates: blank reason refused; wrong passphrase refused at
            // re-authentication; the day stays sealed after both.
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { facade.reopenDay(session.sessionId, "   ", PASSPHRASE.toCharArray(), day) }
            }
            assertThrows(SecurityFailure.Authentication::class.java) {
                runBlocking {
                    facade.reopenDay(session.sessionId, "correction needed", "wrong-passphrase".toCharArray(), day)
                }
            }
            assertFalse(facade.load(session.sessionId, day).postingOpen)

            // Full re-authentication + reason reopens the day.
            val reopened = facade.reopenDay(session.sessionId, "correction needed", PASSPHRASE.toCharArray(), day)
            assertTrue(reopened.refreshedContext.postingOpen)
            assertTrue(facade.load(session.sessionId, day).postingOpen)
        } finally {
            database.close()
        }
    }

    @Test
    fun close_absorbsEarlierOpenDay_recordsSealRows_reopenReversesSeal() = runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            val security = seedOwner(database)
            val session = security.login(OWNER_LOGIN, PASSPHRASE.toCharArray())
            val facade = NativeCashCloseFacade(database, security)
            val day = LocalDate.parse("2026-10-08")

            val customer = database.accountDao().insert(
                AccountEntity(name = "Customer: Spike", type = "Asset", category = "Customer"),
            )
            val product = database.productDao().insert(
                ProductEntity(name = "Spike Milk", unit = "Liters"),
            )
            stock(database, product, 5.0)
            // Two credit sales leave 2026-10-06 open with two events.
            repeat(2) {
                SaleService(database).record(
                    SaleInput(
                        productId = product,
                        occurredAt = "2026-10-06",
                        channel = "credit-retail",
                        unitRate = 25.5,
                        quantity = 1.0,
                        payment = SalePayment.OnCredit(customer),
                    ),
                )
            }

            val context = facade.load(session.sessionId, day)
            assertEquals(listOf("2026-10-06"), context.absorbedDays.map { it.businessDate })
            // Opening stock + two sales all sit on the absorbed day.
            assertEquals(3, context.absorbedDays.single().eventCount)
            assertEquals(
                "This count covers Thu 8 Oct + Tue 6 Oct — the variance is multi-day.",
                context.coverageNote,
            )

            val counted = facade.recordCount(session.sessionId, NativeCashCloseCountRequest(0L, null), day)
            val closed = facade.closeDay(
                session.sessionId,
                NativeCashCloseDayRequest(counted.count.countId, null),
                day,
            )
            assertFalse(closed.refreshedContext.postingOpen)
            assertEquals(
                "This also reopens Tue 6 Oct — they were sealed by Thu 8 Oct's close.",
                closed.refreshedContext.reopenNote,
            )

            // H8 honesty: one seal audit row per absorbed day, recording HOW
            // the day was sealed (by which close, with how many events).
            val seals = database.securityAuditDao().all().filter { it.action == "daily_close_seal_absorbed" }
            assertEquals(1, seals.size)
            assertEquals("2026-10-06", seals.single().reasonCode!!)
            assertTrue(seals.single().details!!.contains("sealed_by=2026-10-08"))
            assertTrue(seals.single().details!!.contains("events=3"))
            assertEquals(listOf("2026-10-06"), database.securityAuditDao().absorbedDaysSealedBy("2026-10-08"))

            // Reopen reverses the seal — the earlier day is absorbable again —
            // while the seal audit rows remain as immutable evidence.
            val reopened = facade.reopenDay(session.sessionId, "late correction", PASSPHRASE.toCharArray(), day)
            assertTrue(reopened.refreshedContext.postingOpen)
            val refreshed = facade.load(session.sessionId, day)
            assertEquals(listOf("2026-10-06"), refreshed.absorbedDays.map { it.businessDate })
            assertEquals(3, refreshed.absorbedDays.single().eventCount)
            assertEquals(
                1,
                database.securityAuditDao().all().count { it.action == "daily_close_seal_absorbed" },
            )
            // And the reopened absorbed day accepts writes again.
            SaleService(database).record(
                SaleInput(
                    productId = product,
                    occurredAt = "2026-10-06",
                    channel = "credit-retail",
                    unitRate = 25.5,
                    quantity = 1.0,
                    payment = SalePayment.OnCredit(customer),
                ),
            )
        } finally {
            database.close()
        }
    }

    private suspend fun seedOwner(database: MilkShopDatabase): UserSecurityService {
        val security = UserSecurityService(database)
        security.bootstrapOwner(OWNER_LOGIN, "Spike Owner", PASSPHRASE.toCharArray())
        return security
    }

    private suspend fun stock(database: MilkShopDatabase, productId: Long, quantity: Double) {
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

    private companion object {
        const val OWNER_LOGIN = "spike.owner"
        const val PASSPHRASE = "correct horse battery staple"
    }
}
