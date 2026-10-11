package com.saaf.milkshop

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.cashclose.*
import com.saaf.milkshop.application.collection.*
import com.saaf.milkshop.application.drawing.*
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

/** Real bundled-driver faults after the command, before receipt construction. */
@RunWith(AndroidJUnit4::class)
class NativeReceiptAtomicityRegressionTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var session: String
    private val date = LocalDate.of(2026, 10, 10)

    @Before fun setup() = runBlocking {
        db = MilkShopDatabase.createInMemory()
        security = UserSecurityService(db)
        session = security.bootstrapOwner("receipt.owner", "Receipt Owner",
            "receipt boundary owner passphrase 2026".toCharArray()).session.sessionId
    }
    @After fun cleanup() { db.close() }

    @Test fun drawingReceiptReadFailureCannotLeaveSavedDrawing() = runBlocking {
        val facade = NativeDrawingFacade(db, security)
        val cash = facade.load(session, date).paymentAccount.accountId
        val beforeEvents = db.eventDao().count()
        val beforeAudits = db.securityAuditDao().all()
        val beforeQueue = db.syncOutboxDao().all()
        val beforeBalance = db.readModelDao().accountBalance(cash)!!.balance
        db.withWriteTransaction { executeSQL("""CREATE TRIGGER fault_drawing_receipt
            AFTER INSERT ON security_audit_entries WHEN NEW.action='bridge_record_owner_drawing'
            BEGIN UPDATE accounts SET name='Receipt fault drawer' WHERE account_id=$cash; END""") }
        val failure = runCatching { facade.record(session, NativeDrawingRequest(12550, null), date) }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(beforeEvents, db.eventDao().count())
        assertEquals(beforeAudits, db.securityAuditDao().all())
        assertEquals(beforeQueue, db.syncOutboxDao().all())
        assertEquals(beforeBalance, db.readModelDao().accountBalance(cash)!!.balance, 0.000001)
        assertEquals("Cash", db.accountDao().findById(cash)!!.name)
    }

    @Test fun collectionReauthorizationFailureCannotLeaveSavedCollection() = runBlocking {
        val customer = CustomerManagementService(db).create("Receipt customer")
        val facade = NativeCollectionFacade(db, security)
        val cash = facade.load(session, date).paymentAccounts.single { it.name == "Cash" }.accountId
        val beforeEvents = db.eventDao().count()
        val beforeAudits = db.securityAuditDao().all()
        val beforeQueue = db.syncOutboxDao().all()
        db.withWriteTransaction { executeSQL("""CREATE TRIGGER fault_collection_receipt
            AFTER INSERT ON security_audit_entries WHEN NEW.action='bridge_record_credit_collection'
            BEGIN UPDATE app_users SET credential_setup_required=1 WHERE login_name='receipt.owner'; END""") }
        val failure = runCatching { facade.record(session, NativeCollectionRequest(customer, cash, 62500, null), date) }.exceptionOrNull()
        assertTrue(failure is SecurityFailure.SessionExpired)
        assertEquals(beforeEvents, db.eventDao().count())
        assertEquals(beforeAudits, db.securityAuditDao().all())
        assertEquals(beforeQueue, db.syncOutboxDao().all())
        assertEquals(0.0, db.readModelDao().accountBalance(customer)!!.balance, 0.000001)
    }

    @Test fun countReceiptReadFailureCannotLeaveSavedCount() = runBlocking {
        val facade = NativeCashCloseFacade(db, security)
        val initial = facade.load(session, date)
        db.withWriteTransaction { executeSQL("""CREATE TRIGGER fault_count_receipt
            AFTER INSERT ON physical_cash_counts
            BEGIN UPDATE accounts SET name='Receipt fault drawer' WHERE account_id=${initial.cashAccountId}; END""") }
        assertTrue(runCatching { facade.recordCount(session, NativeCashCloseCountRequest(initial.expectedCashPaisa, null), date) }.isFailure)
        assertEquals("Cash", db.accountDao().findById(initial.cashAccountId)!!.name)
        assertNull(facade.load(session, date).latestCount)
    }

    private suspend fun prepareSale(): Pair<NativeSaleFacade, NativeSaleRequest> {
        val vendor = VendorManagementService(db).create("Receipt stock farm")
        VendorPurchaseService(db).record(VendorPurchaseInput(vendor, date.toString(), 20.0, 4.0, 28.0))
        val facade = NativeSaleFacade(db, security)
        val context = facade.load(session, date)
        val milk = context.products.single { it.name == "Milk" }.productId
        val cash = context.paymentAccounts.single { it.name == "Cash" }.accountId
        return facade to NativeSaleRequest(milk, NativeSaleEntryMode.QUANTITY, 2.0,
            NativeSalePaymentKind.PAID_NOW, cash, null)
    }

    @Test fun saleReceiptReadFailureRollsBackLedgerAndStock() = runBlocking {
        val (facade, request) = prepareSale()
        val beforeEvents = db.eventDao().count()
        val beforeAudits = db.securityAuditDao().all()
        val beforeQueue = db.syncOutboxDao().all()
        val beforeStock = db.readModelDao().currentStock()
        db.withWriteTransaction { executeSQL("""CREATE TRIGGER fault_sale_receipt
            AFTER INSERT ON security_audit_entries WHEN NEW.action='bridge_record_sale'
            BEGIN UPDATE app_users SET credential_setup_required=1 WHERE login_name='receipt.owner'; END""") }
        val failure = runCatching { facade.record(session, request, date) }.exceptionOrNull()
        assertTrue(failure is SecurityFailure.SessionExpired)
        assertEquals(beforeEvents, db.eventDao().count())
        assertEquals(beforeAudits, db.securityAuditDao().all())
        assertEquals(beforeQueue, db.syncOutboxDao().all())
        assertEquals(beforeStock, db.readModelDao().currentStock())
    }

    @Test fun successfulSaleReturnsReceiptForCommittedLedgerAndFreshTotals() = runBlocking {
        val (facade, request) = prepareSale()
        val beforeEvents = db.eventDao().count()
        val submission = facade.record(session, request, date)
        assertEquals(beforeEvents + 1, db.eventDao().count())
        assertEquals(2.0, submission.receipt.quantity, 0.000001)
        assertEquals(submission.receipt.totalPaisa, submission.refreshedContext.todaySalesPaisa)
        assertEquals(-2.0, db.inventoryLogDao().findByEventId(submission.receipt.eventId).single().quantity, 0.000001)
        assertNotNull(db.eventActorLinkDao().find(submission.receipt.eventId))
    }

    private suspend fun prepareAbsorbedClose(): Pair<NativeCashCloseFacade, Long> {
        val customer = CustomerManagementService(db).create("Absorbed customer")
        val collection = NativeCollectionFacade(db, security)
        val cash = collection.load(session, date.minusDays(1)).paymentAccounts.single { it.name == "Cash" }.accountId
        collection.record(session, NativeCollectionRequest(customer, cash, 10000, null), date.minusDays(1))
        val facade = NativeCashCloseFacade(db, security)
        val loaded = facade.load(session, date)
        assertTrue(loaded.absorbedDays.any { it.businessDate == date.minusDays(1).toString() })
        val count = facade.recordCount(session, NativeCashCloseCountRequest(loaded.expectedCashPaisa, null), date)
        return facade to count.count.countId
    }

    @Test fun failedAbsorptionAuditRollsBackClose() = runBlocking {
        val (facade, count) = prepareAbsorbedClose()
        val beforeAudits = db.securityAuditDao().all()
        val beforeQueue = db.syncOutboxDao().all()
        db.withWriteTransaction { executeSQL("""CREATE TRIGGER fault_seal_audit
            BEFORE INSERT ON security_audit_entries WHEN NEW.action='daily_close_seal_absorbed'
            BEGIN SELECT RAISE(ABORT,'Injected seal audit failure'); END""") }
        assertTrue(runCatching { facade.closeDay(session, NativeCashCloseDayRequest(count, null), date) }.isFailure)
        assertFalse(db.dailyCloseDao().isPostingClosed(date.toString()))
        assertFalse(db.dailyCloseDao().isPostingClosed(date.minusDays(1).toString()))
        assertEquals(beforeAudits, db.securityAuditDao().all())
        assertEquals(beforeQueue, db.syncOutboxDao().all())
    }

    @Test fun successfulAbsorptionReturnsReceiptAndDurableSealEvidence() = runBlocking {
        val (facade, count) = prepareAbsorbedClose()
        val submission = facade.closeDay(session, NativeCashCloseDayRequest(count, null), date)
        assertFalse(submission.refreshedContext.postingOpen)
        assertEquals(date.toString(), submission.receipt.businessDate)
        assertTrue(db.dailyCloseDao().isPostingClosed(date.minusDays(1).toString()))
        assertEquals(listOf(date.minusDays(1).toString()), db.securityAuditDao().absorbedDaysSealedBy(date.toString()))
    }
}
