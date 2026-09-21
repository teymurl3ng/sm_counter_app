package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.expense.NativeExpenseFacade
import com.saaf.milkshop.application.expense.NativeExpenseRequest
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
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
class NativeExpenseFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeExpenseFacade
    private lateinit var ownerSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeExpenseFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "expense.owner",
            "Expense Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesActiveCategoriesAndDefaultsCash() = runBlocking {
        val context = facade.load(ownerSession, DATE)

        assertTrue(context.postingOpen)
        assertTrue(context.categories.any { it.name == "Electricity" })
        assertTrue(context.paymentAccounts.any { it.name == "Cash" })
        val cashId = context.paymentAccounts.single { it.name == "Cash" }.accountId
        assertEquals(cashId, context.defaultPaymentAccountId)
    }

    @Test
    fun recordPostsExpenseAndReturnsReceipt() = runBlocking {
        val context = facade.load(ownerSession, DATE)
        val category = context.categories.single { it.name == "Fuel" }
        val cashId = context.paymentAccounts.single { it.name == "Cash" }.accountId
        val beforeCash = database.readModelDao().accountBalance(cashId)!!.balance
        val eventCountBefore = database.eventDao().count()

        val submission = facade.record(
            ownerSession,
            NativeExpenseRequest(
                expenseCategoryAccountId = category.accountId,
                paymentAccountId = cashId,
                amountPaisa = 12_550L,
                notes = "Expense contract",
            ),
            DATE,
        )

        assertEquals(eventCountBefore + 1, database.eventDao().count())
        assertEquals(12_550L, submission.receipt.amountPaisa)
        assertEquals("Fuel", submission.receipt.category.name)
        assertEquals(cashId, submission.receipt.paymentAccount.accountId)
        assertEquals(beforeCash - 125.50, database.readModelDao().accountBalance(cashId)!!.balance, 0.000001)
        assertEquals(
            125.50,
            database.readModelDao().accountBalance(category.accountId)!!.balance,
            0.000001,
        )
        val actor = database.eventActorLinkDao().find(submission.receipt.eventId)
        assertEquals("Expense Owner", actor!!.actorNameSnapshot)
    }

    @Test
    fun operatorCannotLoadOrRecordExpense() = runBlocking {
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "expense.operator",
                displayName = "Expense Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        val operator = security.login("expense.operator", OPERATOR_PASSPHRASE.toCharArray())

        val failure = runCatching { facade.load(operator.sessionId, DATE) }.exceptionOrNull()

        assertTrue(failure is SecurityFailure.PermissionDenied)
        assertFalse(runCatching { facade.load(operator.sessionId, DATE) }.isSuccess)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 9, 20)
        const val OWNER_PASSPHRASE = "expense owner passphrase 2026"
        const val OPERATOR_PASSPHRASE = "expense operator passphrase 2026"
    }
}
