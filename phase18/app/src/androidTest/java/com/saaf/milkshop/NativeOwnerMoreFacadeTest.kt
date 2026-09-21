package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerMoreFacade
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
class NativeOwnerMoreFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerMoreFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerMoreFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.more",
            "More Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "more.cashier",
                displayName = "More Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("more.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesOwnerContextWithoutCashCountFields() = runBlocking {
        val context = facade.load(ownerSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertTrue(context.shopName.isNotBlank())
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("cash") })
        assertFalse(fields.any { it.contains("amount") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("expected") })
        assertFalse(fields.any { it.contains("variance") })
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.load(cashierSession, DATE) }
        assertTrue(rejected.isFailure)
        assertTrue(
            rejected.exceptionOrNull()?.message?.contains("Owner", ignoreCase = true) == true,
        )
    }

    @Test
    fun loadSecurityReturnsDefaultWhenQuickUnlockUnavailable() = runBlocking {
        val securityStatus = facade.loadSecurity(ownerSession)
        assertFalse(securityStatus.quickPinEnabled)
        assertFalse(securityStatus.phoneSecurityEnabled)
        assertFalse(securityStatus.phoneSecurityAvailable)
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 21)
        private const val OWNER_PASSPHRASE = "owner more passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "more cashier passphrase 2026!!"
    }
}
