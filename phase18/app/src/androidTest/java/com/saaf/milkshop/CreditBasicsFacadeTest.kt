package com.saaf.milkshop

import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.application.readmodels.money
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate
import java.time.Instant

class CreditBasicsFacadeTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeSaleFacade
    private lateinit var session: String
    private var customer = 0L
    private var milk = 0L
    private var cash = 0L
    private val date = LocalDate.parse("2026-10-15")
    @Before fun setup() = runBlocking {
        db = MilkShopDatabase.createInMemory()
        security = UserSecurityService(db)
        session = security.bootstrapOwner("credit.owner", "Credit Owner", "credit-owner-passphrase".toCharArray()).session.sessionId
        customer = CustomerManagementService(db).create("Weekly customer")
        val vendor = VendorManagementService(db).create("Fixture supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(vendor, date.toString(), 100.0, 4.0, 28.0))
        milk = db.productDao().findByName("Milk")!!.productId
        cash = db.accountDao().findByCategory("Cash/Bank").first { it.name == "Cash" }.accountId
        facade = NativeSaleFacade(db, security)
    }
    @After fun close() { db.close() }
    private fun request(amount: Double = 100.0, surcharge: Double? = null) = NativeSaleRequest(milk,
        NativeSaleEntryMode.AMOUNT, amount, NativeSalePaymentKind.CUSTOMER_ACCOUNT, null, customer,
        surchargeEnabled = surcharge != null, surchargeAmountRs = surcharge)
    private suspend fun limit(value: Double?) = CustomerProfileService(db).save(customer,
        CustomerProfileInput(phone = "+92 300 1234567", areaRoute = "North", creditLimit = value))
    private suspend fun sale(value: Double, day: String = "2026-10-01") = SaleService(db).record(
        SaleInput(milk, day, "credit-retail", 100.0, value = value, payment = SalePayment.OnCredit(customer)))

    @Test fun nullZeroAndEqualLimitsAreDistinctAndUnacknowledgedWarningDoesNotWrite() = runBlocking {
        limit(null)
        assertNull(facade.previewCredit(session, request(), date).warningKey)
        limit(100.0)
        assertNull(facade.previewCredit(session, request(), date).warningKey)
        limit(0.0)
        val before = db.eventDao().count()
        val warning = runCatching { facade.record(session, request(), date) }.exceptionOrNull() as CreditWarningRequired
        assertEquals(before, db.eventDao().count())
        assertEquals(10000L, warning.warning.summary.projectedBalancePaisa)
        val saved = facade.record(session, request().copy(creditAcknowledgment = warning.warning.summary.warningKey), date)
        assertEquals(10000L, saved.receipt.totalPaisa)
        assertEquals(before + 1, db.eventDao().count())
    }
    @Test fun surchargeCrossingAndChangedSnapshotRequireFreshAcknowledgement() = runBlocking {
        sale(800.0)
        limit(1000.0)
        val draft = request(180.0, 50.0)
        val original = facade.previewCredit(session, draft, date)
        assertEquals(23000L, original.saleTotalPaisa)
        assertEquals(103000L, original.projectedBalancePaisa)
        val before = db.eventDao().count()
        limit(950.0)
        val changed = runCatching { facade.record(session, draft.copy(creditAcknowledgment = original.warningKey), date) }
            .exceptionOrNull() as CreditWarningRequired
        assertNotEquals(original.warningKey, changed.warning.summary.warningKey)
        assertEquals(before, db.eventDao().count())
        val saved = facade.record(session, draft.copy(creditAcknowledgment = changed.warning.summary.warningKey), date)
        assertEquals(23000L, saved.receipt.totalPaisa)
        assertEquals(103000L, money(db.readModelDao().accountBalance(customer)!!.balance))
    }
    @Test fun interveningCollectionCanRemoveWarningAndAdvanceOffsetsNewSale() = runBlocking {
        sale(800.0); limit(900.0)
        val draft = request(180.0)
        val warned = facade.previewCredit(session, draft, date)
        assertNotNull(warned.warningKey)
        CreditCollectionService(db).record(CreditCollectionInput(customer, cash, 100.0, date.toString()))
        facade.record(session, draft.copy(creditAcknowledgment = warned.warningKey), date)
        assertEquals(88000L, money(db.readModelDao().accountBalance(customer)!!.balance))
        CreditCollectionService(db).record(CreditCollectionInput(customer, cash, 930.0, date.toString()))
        val summary = facade.previewCredit(session, request(80.0), date)
        assertEquals(-5000L, summary.balancePaisa)
        assertEquals(3000L, summary.projectedBalancePaisa)
        assertNull(summary.warningKey)
    }
    @Test fun fractionalAggregateUsesCanonicalProjectionInsteadOfRoundedOperands() = runBlocking {
        sale(0.004); limit(0.0)
        val summary = facade.previewCredit(session, request(0.004), date)
        assertEquals(0L, summary.balancePaisa)
        assertEquals(0L, summary.saleTotalPaisa)
        assertEquals(1L, summary.projectedBalancePaisa)
        assertNotNull(summary.warningKey)
    }
    @Test fun correctionsAndZeroDeltaAncestorPreserveAgeAndCustomerDetail() = runBlocking {
        val first = sale(100.0)
        sale(100.0, "2026-10-10")
        val zero = CorrectionService(db).correct(CorrectionInput.Sale(first.eventId, "2026-10-01", 100.0, value = 100.0))
        CorrectionService(db).correct(CorrectionInput.Sale(zero.eventId, "2026-10-01", 100.0, value = 160.0))
        CreditCollectionService(db).record(CreditCollectionInput(customer, cash, 120.0, "2026-10-12"))
        val summary = facade.previewCredit(session, request(1.0), date)
        assertEquals(14000L, summary.balancePaisa)
        assertEquals(14L, summary.age.oldestKnownDays)
        assertTrue(summary.age.available)
        val detail = NativeOwnerCustomersFacade(db, security).loadDetail(session, customer)
        assertNotNull(detail.creditAge)
    }
    @Test fun unavailableAgeDoesNotDisableValidLimitCheck() = runBlocking {
        // A current balance containing a future-dated sale cannot claim complete as-of aging.
        sale(800.0, date.plusDays(1).toString())
        limit(900.0)
        val summary = facade.previewCredit(session, request(200.0), date)
        assertFalse(summary.age.available)
        assertFalse(summary.checkUnavailable)
        assertNotNull(summary.warningKey)
    }
    @Test fun invalidLimitRequiresUnavailableCheckConfirmationAndStillPostsOnlyOnce() = runBlocking {
        // Represents legacy/synced invalid profile data; do not change production validation.
        db.customerProfileDao().save(CustomerProfileEntity(accountId = customer, creditLimit = Double.POSITIVE_INFINITY, createdAt = date.toString()))
        val summary = facade.previewCredit(session, request(), date)
        assertTrue(summary.checkUnavailable)
        val before = db.eventDao().count()
        assertTrue(runCatching { facade.record(session, request(), date) }.exceptionOrNull() is CreditWarningRequired)
        assertEquals(before, db.eventDao().count())
        facade.record(session, request().copy(creditAcknowledgment = summary.warningKey), date)
        assertEquals(before + 1, db.eventDao().count())
    }
    @Test fun archivedCustomerAndExpiredSessionCannotUseAcknowledgement() = runBlocking {
        limit(0.0)
        val draft = request()
        val summary = facade.previewCredit(session, draft, date)
        CustomerProfileService(db).save(customer, CustomerProfileInput(active = false))
        assertTrue(runCatching { facade.record(session, draft.copy(creditAcknowledgment = summary.warningKey), date) }.isFailure)
        security.logout(session)
        assertTrue(runCatching { facade.previewCredit(session, draft, date) }.isFailure)
    }
}
