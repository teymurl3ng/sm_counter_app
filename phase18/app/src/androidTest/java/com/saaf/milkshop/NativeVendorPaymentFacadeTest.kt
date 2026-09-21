package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentFacade
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentRequest
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
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
class NativeVendorPaymentFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeVendorPaymentFacade
    private lateinit var ownerSession: String
    private var vendorId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeVendorPaymentFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "vendor.pay.owner",
            "Vendor Pay Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        vendorId = VendorManagementService(database).create("Dawn Contract Supply")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesActiveVendorsAndDefaultsCash() = runBlocking {
        val context = facade.load(ownerSession, DATE)

        assertTrue(context.postingOpen)
        assertEquals(setOf(vendorId), context.vendors.map { it.accountId }.toSet())
        assertTrue(context.paymentAccounts.any { it.name == "Cash" })
        val cashId = context.paymentAccounts.single { it.name == "Cash" }.accountId
        assertEquals(cashId, context.defaultPaymentAccountId)
    }

    @Test
    fun recordPostsVendorPaymentAndReturnsReceipt() = runBlocking {
        val context = facade.load(ownerSession, DATE)
        val cashId = context.paymentAccounts.single { it.name == "Cash" }.accountId
        val beforeCash = database.readModelDao().accountBalance(cashId)!!.balance
        val eventCountBefore = database.eventDao().count()

        val submission = facade.record(
            ownerSession,
            NativeVendorPaymentRequest(
                vendorAccountId = vendorId,
                paymentAccountId = cashId,
                amountPaisa = 12_550L,
                notes = "Vendor payment contract",
            ),
            DATE,
        )

        assertEquals(eventCountBefore + 1, database.eventDao().count())
        assertEquals(12_550L, submission.receipt.amountPaisa)
        assertEquals(vendorId, submission.receipt.vendor.accountId)
        assertEquals(cashId, submission.receipt.paymentAccount.accountId)
        assertEquals(beforeCash - 125.50, database.readModelDao().accountBalance(cashId)!!.balance, 0.000001)
        val actor = database.eventActorLinkDao().find(submission.receipt.eventId)
        assertEquals("Vendor Pay Owner", actor!!.actorNameSnapshot)
    }

    @Test
    fun operatorCannotLoadOrRecordVendorPayment() = runBlocking {
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "vendor.pay.operator",
                displayName = "Vendor Pay Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        val operator = security.login("vendor.pay.operator", OPERATOR_PASSPHRASE.toCharArray())

        val failure = runCatching { facade.load(operator.sessionId, DATE) }.exceptionOrNull()

        assertTrue(failure is SecurityFailure.PermissionDenied)
        assertFalse(runCatching { facade.load(operator.sessionId, DATE) }.isSuccess)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 9, 20)
        const val OWNER_PASSPHRASE = "vendor pay owner passphrase 2026"
        const val OPERATOR_PASSPHRASE = "vendor pay operator passphrase 2026"
    }
}
