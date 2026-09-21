package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerReportsCoverageKind
import com.saaf.milkshop.application.owner.NativeOwnerReportsFacade
import com.saaf.milkshop.application.owner.NativeOwnerReportsPreset
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class NativeOwnerReportsFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerReportsFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerReportsFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.reports",
            "Reports Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "reports.cashier",
                displayName = "Reports Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("reports.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadMapsThisMonthRangeAndHonestEmptyCoverage() = runBlocking {
        val (start, end) = facade.rangeForPreset(NativeOwnerReportsPreset.ThisMonth, TODAY)
        val snapshot = facade.load(ownerSession, start, end, NativeOwnerReportsPreset.ThisMonth, TODAY)
        assertEquals(NativeOwnerReportsPreset.ThisMonth, snapshot.preset)
        assertEquals(start, snapshot.startInclusive)
        assertEquals(end, snapshot.endExclusive)
        assertEquals(YearMonth.from(TODAY).atDay(1).toString(), start)
        assertTrue(snapshot.shopName.isNotBlank())
        assertEquals(NativeOwnerReportsCoverageKind.Empty, snapshot.coverageKind)
        assertTrue(snapshot.deliverySurchargeLabel.contains("PKR"))
        assertTrue(snapshot.operatingExpensesLabel.contains("PKR"))
        assertTrue(snapshot.salaryPaymentsLabel.contains("PKR"))
        assertTrue(snapshot.products.isEmpty())
    }

    @Test
    fun rangeForLastMonthIsPreviousCalendarMonth() {
        val (start, end) = facade.rangeForPreset(NativeOwnerReportsPreset.LastMonth, TODAY)
        val last = YearMonth.from(TODAY).minusMonths(1)
        assertEquals(last.atDay(1).toString(), start)
        assertEquals(YearMonth.from(TODAY).atDay(1).toString(), end)
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val (start, end) = facade.rangeForPreset(NativeOwnerReportsPreset.ThisMonth, TODAY)
        val rejected = runCatching {
            facade.load(cashierSession, start, end, NativeOwnerReportsPreset.ThisMonth, TODAY)
        }
        assertTrue(rejected.isFailure)
    }

    companion object {
        private val TODAY = LocalDate.of(2026, 9, 21)
        private const val OWNER_PASSPHRASE = "owner reports passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "reports cashier passphrase 2026!!"
    }
}
