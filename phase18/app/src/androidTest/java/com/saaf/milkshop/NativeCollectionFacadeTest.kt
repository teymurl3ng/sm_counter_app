package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.SecurityFailure
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
class NativeCollectionFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeCollectionFacade
    private lateinit var ownerSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeCollectionFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "collection.owner",
            "Collection Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadKeepsBalancesOutOfTheCustomerListAndResolvesOnlyTheSelection() = runBlocking {
        val ahmedId = CustomerManagementService(database).create("Ahmed Contract")
        val bilalId = CustomerManagementService(database).create("Bilal Contract")

        val context = facade.load(ownerSession, DATE)

        assertEquals(setOf(ahmedId, bilalId), context.customers.map { it.accountId }.toSet())
        assertTrue(context.postingOpen)
        assertTrue(context.paymentAccounts.any { it.name == "Cash" })
        assertEquals(0L, facade.loadCustomer(ownerSession, ahmedId, DATE).balancePaisa)
        assertEquals(0L, facade.loadCustomer(ownerSession, bilalId, DATE).balancePaisa)
    }

    @Test
    fun recordAllowsAdvanceAndReturnsVerifiedCustomerAndCashMovements() = runBlocking {
        val customerId = CustomerManagementService(database).create("Advance Contract")
        val context = facade.load(ownerSession, DATE)
        val cashId = context.paymentAccounts.single { it.name == "Cash" }.accountId
        val beforeCash = database.readModelDao().accountBalance(cashId)!!.balance
        val eventCountBefore = database.eventDao().count()

        val submission = facade.record(
            ownerSession,
            NativeCollectionRequest(
                customerAccountId = customerId,
                paymentAccountId = cashId,
                amountPaisa = 62_500L,
                notes = "Advance contract",
            ),
            DATE,
        )

        assertEquals(eventCountBefore + 1, database.eventDao().count())
        assertEquals(62_500L, submission.receipt.amountPaisa)
        assertEquals(-62_500L, submission.receipt.customerBalanceAfterPaisa)
        assertEquals(-625.0, database.readModelDao().accountBalance(customerId)!!.balance, 0.000001)
        assertEquals(beforeCash + 625.0, database.readModelDao().accountBalance(cashId)!!.balance, 0.000001)
        val actor = database.eventActorLinkDao().find(submission.receipt.eventId)
        assertEquals("Collection Owner", actor!!.actorNameSnapshot)
    }

    @Test
    fun operatorCannotLoadOrRecordCollectionData() = runBlocking {
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "collection.operator",
                displayName = "Collection Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        val operator = security.login("collection.operator", OPERATOR_PASSPHRASE.toCharArray())

        val failure = runCatching { facade.load(operator.sessionId, DATE) }.exceptionOrNull()

        assertTrue(failure is SecurityFailure.PermissionDenied)
        assertFalse(runCatching { facade.load(operator.sessionId, DATE) }.isSuccess)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 9, 20)
        const val OWNER_PASSPHRASE = "collection owner passphrase 2026"
        const val OPERATOR_PASSPHRASE = "collection operator passphrase 2026"
    }
}
