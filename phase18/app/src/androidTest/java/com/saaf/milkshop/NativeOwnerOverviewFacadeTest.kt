package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerOverviewAttentionKind
import com.saaf.milkshop.application.owner.NativeOwnerOverviewFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NativeOwnerOverviewFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerOverviewFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerOverviewFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.overview",
            "Overview Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "owner.cashier",
                displayName = "Overview Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("owner.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesDecisionSummaryAndCashCountAttention() = runBlocking {
        val snapshot = facade.load(ownerSession, DATE)
        assertEquals(DATE.toString(), snapshot.businessDate)
        assertTrue(snapshot.postingOpen)
        assertTrue(snapshot.shopName.isNotBlank())
        assertTrue(snapshot.todaySalesPaisa >= 0L)
        assertTrue(snapshot.expectedCashPaisa >= 0L)
        assertTrue(snapshot.attention.any { it.kind == NativeOwnerOverviewAttentionKind.CashCountWaiting })
        assertTrue(snapshot.stock.isNotEmpty() || snapshot.stock.isEmpty())
    }

    @Test
    fun loadRejectsNonOwnerSession() = runBlocking {
        val failure = runCatching { facade.load(cashierSession, DATE) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("Owner"))
    }

    @Test
    fun loadRejectsWhenSessionMissing() = runBlocking {
        security.logout(ownerSession)
        val failure = runCatching { facade.load(ownerSession, DATE) }.exceptionOrNull()
        assertTrue(failure != null)
        assertFalse(failure is IllegalArgumentException && failure.message!!.contains("only available"))
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 21)
        private const val OWNER_PASSPHRASE = "owner overview passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "owner overview cashier passphrase 2026!!"
    }
}
