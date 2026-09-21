package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerWorkFacade
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
class NativeOwnerWorkFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerWorkFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerWorkFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.work",
            "Work Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "owner.work.cashier",
                displayName = "Work Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("owner.work.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesCommandLauncherContext() = runBlocking {
        val context = facade.load(ownerSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertTrue(context.shopName.isNotBlank())
        assertTrue(context.activeDeliveryCount >= 0)
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
        private val DATE = LocalDate.parse("2026-09-21")
        private const val OWNER_PASSPHRASE = "Owner-Work-Passphrase-1"
        private const val CASHIER_PASSPHRASE = "Cashier-Work-Passphrase-1"
    }
}
