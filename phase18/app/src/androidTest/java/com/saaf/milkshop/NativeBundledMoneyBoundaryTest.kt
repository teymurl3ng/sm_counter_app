package com.saaf.milkshop

import androidx.room3.useReaderConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.activity.NativeActivityFacade
import com.saaf.milkshop.application.sale.NativeSaleEntryMode
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.application.sale.NativeSalePaymentKind
import com.saaf.milkshop.application.sale.NativeSaleRequest
import com.saaf.milkshop.data.CanonicalMoneySql
import com.saaf.milkshop.data.DailyCloseActionEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductEntity
import com.saaf.milkshop.data.ProductProfileEntity
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Real production bundled driver, isolated in-memory data; never opens the installed shop DB. */
@RunWith(AndroidJUnit4::class)
class NativeBundledMoneyBoundaryTest {
    @Test
    fun canonicalSqlHalfPaisaAndSignedValuesMatchDecimalRule() = runBlocking<Unit> {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.useReaderConnection { connection ->
                connection.usePrepared("SELECT sqlite_version()") { statement ->
                    assertTrue(statement.step())
                    println("MONEY_BOUNDARY bundled sqlite_version=${statement.getText(0)}")
                }
                val cases = listOf(2.125 to 2.13, -2.125 to -2.13, 1.005 to 1.01,
                    -1.005 to -1.01, 0.005 to 0.01, -0.005 to -0.01,
                    2.124 to 2.12, 2.126 to 2.13, 61.25 to 61.25,
                    Math.nextDown(1.005) to 1.0, Math.nextUp(1.005) to 1.01,
                    Math.nextDown(-1.005) to -1.01, Math.nextUp(-1.005) to -1.0,
                    1e12 to 1e12, -1e12 to -1e12, 1e-20 to 0.0)
                for ((input, expected) in cases) {
                    connection.usePrepared("SELECT ${CanonicalMoneySql.ROUND_VALUE} FROM (SELECT ? AS money_raw)") { statement ->
                        statement.bindDouble(1, input)
                        assertTrue(statement.step())
                        val actual = statement.getDouble(0)
                        println("MONEY_BOUNDARY SQL input=$input expected=$expected actual=$actual")
                        assertEquals("Canonical HALF_UP($input)", expected, actual, 0.0)
                    }
                }
            }
        } finally { db.close() }
    }

    @Test
    fun quantitySaleAtOnePointZeroZeroFiveMustAgreeWithCash() = runBlocking<Unit> {
        fixture { db, security, session, product, cash ->
            // Literal: 0.5 L x Rs 2.01 = Rs 1.005; decimal HALF_UP = 101 paisa.
            val saved = NativeSaleFacade(db, security).record(session,
                request(NativeSaleEntryMode.QUANTITY, 0.5).copy(productId = product,
                    paymentAccountId = cash, wholesale = true, wholesaleRatePaisa = 201L), DATE)
            val raw = db.financialLogDao().findByEventId(saved.receipt.eventId).single().amount
            val activity = NativeActivityFacade(db, security)
                .loadDetail(session, saved.receipt.eventId, DATE).summary.amountPaisa
            val counted = PhysicalCashCountService(db).record(
                PhysicalCashCountInput(cash, DATE.toString(), 1.01))
            println("MONEY_BOUNDARY quantity=.5 rate=2.01 raw=$raw receipt=${saved.receipt.totalPaisa}" +
                " activity=$activity cash=${saved.refreshedContext.defaultPaymentAccountBalancePaisa}" +
                " countExpected=${counted.expectedBalance} variance=${counted.variance}")
            assertEquals(1.005, raw, 0.0)
            assertEquals(101L, saved.receipt.totalPaisa)
            assertEquals(101L, activity)
            assertEquals("Cash must match the receipt for this single sale", 101L,
                saved.refreshedContext.defaultPaymentAccountBalancePaisa)
            assertEquals(1.01, counted.expectedBalance, 0.0)
            assertEquals(0.0, counted.variance, 0.0)
            val closed = DailyCloseService(db).closeAuthenticated(session, security,
                VerifiedDailyCloseInput(DATE.toString(), counted.count.countId))
            assertEquals(1.01, closed.expectedCash!!, 0.0)
            assertEquals(2, closed.action.moneyRoundingVersion)
        }
    }

    @Test
    fun sqlGuardRejectsOnePaisaShortageAndInvalidPolicy() = runBlocking<Unit> {
        fixture { db, security, session, product, cash ->
            NativeSaleFacade(db, security).record(session,
                request(NativeSaleEntryMode.QUANTITY, 0.5).copy(productId = product,
                    paymentAccountId = cash, wholesale = true, wholesaleRatePaisa = 201L), DATE)
            val count = PhysicalCashCountService(db).record(
                PhysicalCashCountInput(cash, DATE.toString(), 1.0)).count
            for (version in listOf(2, 99)) {
                val failure = runCatching { db.dailyCloseDao().insert(DailyCloseActionEntity(
                    businessDate = DATE.toString(), action = "close", physicalCashCountId = count.countId,
                    actorName = "Bundled rounding owner", actorRole = "owner", createdAt = count.countedAt,
                    actorUserId = db.appUserDao().findByLoginName("bundled.rounding.owner")!!.userId,
                    moneyRoundingVersion = version)) }.exceptionOrNull()
                assertTrue("SQL guard must refuse policy=$version", failure != null)
                assertTrue(failure.toString(), failure.toString().contains(
                    if (version == 2) "variance" else "rounding version"))
                assertTrue(db.dailyCloseDao().all().isEmpty())
            }
        }
    }

    @Test
    fun saleReceiptsActivityCashAndCountAgreeForAmountQuantityAndSurcharge() = runBlocking<Unit> {
        val cases = listOf(
            request(NativeSaleEntryMode.AMOUNT, 2.125) to 213L,
            request(NativeSaleEntryMode.QUANTITY, 0.5) to 213L,
            request(NativeSaleEntryMode.QUANTITY, 1.0).copy(
                surchargeEnabled = true, surchargeAmountRs = 0.875) to 513L)
        for ((entry, expected) in cases) {
            fixture { db, security, session, product, cash ->
                val saved = NativeSaleFacade(db, security).record(session,
                    entry.copy(productId = product, paymentAccountId = cash), DATE)
                assertEquals(expected, saved.receipt.totalPaisa)
                assertEquals(expected, saved.refreshedContext.todaySalesPaisa)
                assertEquals(expected, saved.refreshedContext.defaultPaymentAccountBalancePaisa)
                assertEquals(expected, NativeActivityFacade(db, security)
                    .loadDetail(session, saved.receipt.eventId, DATE).summary.amountPaisa)
                // Literal expectations: these are not recomputed through production conversion.
                val expectedRupees = if (expected == 213L) 2.13 else 5.13
                val counted = PhysicalCashCountService(db).record(
                    PhysicalCashCountInput(cash, DATE.toString(), expectedRupees))
                assertEquals(expectedRupees, counted.expectedBalance, 0.0)
                assertEquals(0.0, counted.variance, 0.0)
                val closed = DailyCloseService(db).closeAuthenticated(session, security,
                    VerifiedDailyCloseInput(DATE.toString(), counted.count.countId))
                assertEquals(expectedRupees, closed.expectedCash!!, 0.0)
                assertEquals(0.0, closed.variance!!, 0.0)
                assertEquals("closed", closed.status)
            }
        }
    }

    @Test
    fun halfPaisaCorrectionPreservesHistoryAndWrongCountStillNeedsReason() = runBlocking<Unit> {
        fixture { db, security, session, product, cash ->
            val facade = NativeSaleFacade(db, security)
            val saved = facade.record(session, request(NativeSaleEntryMode.AMOUNT, 2.125)
                .copy(productId = product, paymentAccountId = cash), DATE)
            val wrong = PhysicalCashCountService(db).record(
                PhysicalCashCountInput(cash, DATE.toString(), 2.12))
            assertEquals(2.13, wrong.expectedBalance, 0.0)
            var denied = false
            try {
                DailyCloseService(db).closeAuthenticated(session, security,
                    VerifiedDailyCloseInput(DATE.toString(), wrong.count.countId))
            } catch (_: IllegalArgumentException) { denied = true }
            assertTrue("A one-paisa shortage must still require a reason", denied)
            assertTrue(db.dailyCloseDao().currentAction(DATE.toString()) == null)
            val correction = CorrectionService(db).correct(CorrectionInput.Sale(
                originalEventId = saved.receipt.eventId, occurredAt = DATE.toString(),
                unitRate = 4.25, value = 2.0))
            assertEquals(2.125, db.financialLogDao().findByEventId(saved.receipt.eventId).single().amount, 0.0)
            assertEquals(-0.125, db.financialLogDao().findByEventId(correction.eventId).single().amount, 0.0)
            assertEquals(200L, facade.load(session, DATE).defaultPaymentAccountBalancePaisa)
            val count = PhysicalCashCountService(db).record(
                PhysicalCashCountInput(cash, DATE.toString(), 2.0))
            val closed = DailyCloseService(db).closeAuthenticated(session, security,
                VerifiedDailyCloseInput(DATE.toString(), count.count.countId))
            assertEquals(2.0, closed.expectedCash!!, 0.0)
            assertEquals(0.0, closed.variance!!, 0.0)
        }
    }

    private fun request(mode: NativeSaleEntryMode, value: Double) = NativeSaleRequest(
        productId = 0L, entryMode = mode, enteredValue = value,
        paymentKind = NativeSalePaymentKind.PAID_NOW, paymentAccountId = null, customerAccountId = null)

    private suspend fun fixture(block: suspend (MilkShopDatabase, UserSecurityService, String, Long, Long) -> Unit) {
        val db = MilkShopDatabase.createInMemory()
        try {
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("bundled.rounding.owner", "Bundled rounding owner",
                "test bundled rounding passphrase".toCharArray()).session.sessionId
            val product = db.productDao().insert(ProductEntity(name = "Bundled boundary product", unit = "Liters"))
            db.productProfileDao().save(ProductProfileEntity(productId = product,
                defaultSaleRate = 4.25, createdAt = "2026-10-11T06:00:00Z"))
            val opening = db.eventDao().insert(EventEntity(createdAt = "2026-10-11T06:00:00Z",
                occurredAt = DATE.toString(), eventType = "OPENING_STATE"))
            db.inventoryLogDao().insert(InventoryLogEntity(eventId = opening, productId = product, quantity = 100.0))
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            block(db, security, session, product, cash)
        } finally { db.close() }
    }

    companion object { private val DATE = LocalDate.of(2026, 10, 11) }
}
