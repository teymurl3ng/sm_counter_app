package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.more.NativeMoreFacade
import com.saaf.milkshop.data.MilkShopDatabase
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
class NativeMoreFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeMoreFacade
    private lateinit var ownerSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeMoreFacade(database, security, quickUnlock = null)
        ownerSession = security.bootstrapOwner(
            "more.owner",
            "More Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesCashDrawerWithoutExpectedBalance() = runBlocking {
        val context = facade.load(ownerSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertEquals("Cash", context.cashAccountName)
        assertTrue(context.cashAccountId > 0L)
    }

    @Test
    fun blindCashCountReturnsCountedAmountWithoutExpectedOrVarianceFields() = runBlocking {
        val receipt = facade.recordBlindCashCount(
            sessionHandle = ownerSession,
            countedCashPaisa = 12_500L,
            notes = "Drawer check",
            date = DATE,
        )
        assertTrue(receipt.countId > 0L)
        assertEquals(DATE.toString(), receipt.businessDate)
        assertEquals(12_500L, receipt.countedCashPaisa)
        // Projection type has no expected/variance members — compile-time + value check above.
        val fields = receipt.javaClass.declaredFields.map { it.name }.toSet()
        assertFalse(fields.any { it.contains("expected", ignoreCase = true) })
        assertFalse(fields.any { it.contains("variance", ignoreCase = true) })
    }

    @Test
    fun securityStatusIsOffWhenQuickUnlockUnavailable() = runBlocking {
        val status = facade.loadSecurity(ownerSession)
        assertFalse(status.quickPinEnabled)
        assertFalse(status.phoneSecurityEnabled)
        assertFalse(status.phoneSecurityAvailable)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 9, 20)
        const val OWNER_PASSPHRASE = "more owner passphrase 2026"
    }
}
