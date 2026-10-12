package com.saaf.milkshop.application.sale

import android.app.Application
import com.saaf.milkshop.application.activity.NativeActivityFacade
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.application.expense.NativeExpenseFacade
import com.saaf.milkshop.application.expense.NativeExpenseRequest
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Real schema with the framework driver on the JVM; expectations are literal, not recomputed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NativeSaleMoneyBoundaryTest {
    @Test
    fun catalogHalfPaisaUsesTheSameRuleAsFinancialReads() = runBlocking<Unit> {
        fixture { db, security, session, product, _ ->
            val profile = db.productProfileDao().findByProductId(product)!!
            db.productProfileDao().save(profile.copy(defaultSaleRate = 2.125))
            assertEquals(213L, NativeSaleFacade(db, security).load(session, DATE)
                .products.single { it.productId == product }.ratePaisaPerUnit)
            assertEquals(2.125, db.productProfileDao().findByProductId(product)!!.defaultSaleRate!!, 0.0)
        }
    }

    @Test
    fun amountQuantityAndSurchargeReceiptsMatchActivityAndRawLedger() = runBlocking<Unit> {
        // Each case has its own database: rounding of an aggregate is a separate boundary.
        val cases = listOf(
            request(NativeSaleEntryMode.AMOUNT, 2.125) to 213L,
            request(NativeSaleEntryMode.QUANTITY, 0.5) to 213L, // 0.5 x 4.25
            request(NativeSaleEntryMode.QUANTITY, 1.0).copy(
                surchargeEnabled = true, surchargeAmountRs = 0.875) to 513L, // 4.25 + .875
            request(NativeSaleEntryMode.AMOUNT, 61.25) to 6_125L,
        )
        for ((entry, expected) in cases) {
            fixture { db, security, session, product, cash ->
                val saved = NativeSaleFacade(db, security).record(session,
                    entry.copy(productId = product, paymentAccountId = cash), DATE)
                assertEquals(expected, saved.receipt.totalPaisa)
                assertEquals(expected, saved.refreshedContext.todaySalesPaisa)
                assertEquals(expected, saved.refreshedContext.defaultPaymentAccountBalancePaisa)
                val activity = NativeActivityFacade(db, security)
                assertEquals(expected, activity.loadToday(session, DATE).records.single().amountPaisa)
                assertEquals(expected, activity.loadDetail(session, saved.receipt.eventId, DATE).summary.amountPaisa)
                val event = db.eventDao().findById(saved.receipt.eventId)!!
                val row = db.financialLogDao().findByEventId(saved.receipt.eventId).single()
                assertEquals(expected, money(row.amount))
                assertEquals(event.saleValue!! + event.surchargeApplied!!, row.amount, 0.0)
                if (entry.enteredValue == 2.125) assertEquals(2.125, row.amount, 0.0)
            }
        }
    }

    @Test
    fun cashViewUsesTheSameDecimalRuleAsReceipts() = runBlocking<Unit> {
        fixture { db, security, session, product, cash ->
            val saved = NativeSaleFacade(db, security).record(session,
                request(NativeSaleEntryMode.AMOUNT, 2.125)
                    .copy(productId = product, paymentAccountId = cash), DATE)
            assertEquals(2.125, db.financialLogDao().findByEventId(saved.receipt.eventId).single().amount, 0.0)
            assertEquals(213L, saved.receipt.totalPaisa)
            assertEquals(2.13, db.readModelDao().accountBalance(cash)!!.balance, 0.0)
            assertEquals(213L, saved.refreshedContext.defaultPaymentAccountBalancePaisa)
        }
    }

    @Test
    fun invalidOrOverflowingSaleLeavesNoPartialRecords() = runBlocking<Unit> {
        fixture { db, security, session, product, cash ->
            val facade = NativeSaleFacade(db, security)
            val eventsBefore = db.eventDao().findAllDescending().size
            val outboxBefore = db.syncOutboxDao().count()
            val entries = listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0)
                .map { request(NativeSaleEntryMode.QUANTITY, it) } +
                request(NativeSaleEntryMode.QUANTITY, 0.5).copy(
                    surchargeEnabled = true, surchargeAmountRs = 1e17)
            for (entry in entries) {
                var rejected = false
                try {
                    facade.record(session, entry
                        .copy(productId = product, paymentAccountId = cash), DATE)
                } catch (_: IllegalArgumentException) { rejected = true
                } catch (_: ArithmeticException) { rejected = true }
                assertTrue("Must reject $entry", rejected)
                assertEquals(eventsBefore, db.eventDao().findAllDescending().size)
                assertEquals(outboxBefore, db.syncOutboxDao().count())
                assertEquals(0L, facade.load(session, DATE).defaultPaymentAccountBalancePaisa)
            }
        }
    }

    @Test
    fun integerPaisaCollectionsAndExpensesKeepTheirLiteralAmounts() = runBlocking<Unit> {
        fixture { db, security, session, _, cash ->
            val customer = CustomerManagementService(db).create("Boundary customer")
            val collected = NativeCollectionFacade(db, security).record(session,
                NativeCollectionRequest(customer, cash, 213L, null), DATE)
            assertEquals(213L, collected.receipt.amountPaisa)
            assertEquals(213L, money(db.financialLogDao().findByEventId(collected.receipt.eventId)
                .single { it.accountId == cash }.amount))
            val expenses = NativeExpenseFacade(db, security)
            val category = expenses.load(session, DATE).categories.first().accountId
            val spent = expenses.record(session, NativeExpenseRequest(category, cash, 101L, null), DATE)
            assertEquals(101L, spent.receipt.amountPaisa)
            assertEquals(-101L, money(db.financialLogDao().findByEventId(spent.receipt.eventId)
                .single { it.accountId == cash }.amount))
            assertEquals(112L, NativeSaleFacade(db, security).load(session, DATE).defaultPaymentAccountBalancePaisa)
        }
    }

    @Test
    fun correctionKeepsUnroundedTruthAndOriginalLedgerIntact() = runBlocking<Unit> {
        fixture { db, security, session, product, cash ->
            val sale = NativeSaleFacade(db, security).record(session,
                request(NativeSaleEntryMode.AMOUNT, 2.125)
                    .copy(productId = product, paymentAccountId = cash), DATE)
            val corrected = CorrectionService(db).correct(CorrectionInput.Sale(
                originalEventId = sale.receipt.eventId, occurredAt = DATE.toString(),
                unitRate = 4.25, value = 2.0))
            assertEquals(2.125, db.financialLogDao().findByEventId(sale.receipt.eventId).single().amount, 0.0)
            assertEquals(-0.125, db.financialLogDao().findByEventId(corrected.eventId).single().amount, 0.0)
            assertEquals(-13L, money(-0.125))
            assertEquals(200L, NativeSaleFacade(db, security).load(session, DATE).defaultPaymentAccountBalancePaisa)
        }
    }

    private fun request(mode: NativeSaleEntryMode, value: Double) = NativeSaleRequest(
        productId = 0L, entryMode = mode, enteredValue = value,
        paymentKind = NativeSalePaymentKind.PAID_NOW, paymentAccountId = null, customerAccountId = null)

    private suspend fun fixture(block: suspend (MilkShopDatabase, UserSecurityService, String, Long, Long) -> Unit) {
        val db = MilkShopDatabase.createInMemory(driver = null)
        try {
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("rounding.owner", "Rounding owner",
                "test rounding passphrase".toCharArray()).session.sessionId
            val product = db.productDao().insert(ProductEntity(name = "Boundary product", unit = "Liters"))
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
