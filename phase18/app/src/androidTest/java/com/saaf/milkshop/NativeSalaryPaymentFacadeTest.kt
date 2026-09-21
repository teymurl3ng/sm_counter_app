package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentFacade
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentRequest
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.EmployeeManagementService
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
class NativeSalaryPaymentFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeSalaryPaymentFacade
    private lateinit var ownerSession: String
    private var employeeId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeSalaryPaymentFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "salary.pay.owner",
            "Salary Pay Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        employeeId = EmployeeManagementService(database).create("Ayesha Contract")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesActiveEmployeesAndDefaultsCash() = runBlocking {
        val context = facade.load(ownerSession, DATE)

        assertTrue(context.postingOpen)
        assertEquals(setOf(employeeId), context.employees.map { it.accountId }.toSet())
        assertTrue(context.paymentAccounts.any { it.name == "Cash" })
        val cashId = context.paymentAccounts.single { it.name == "Cash" }.accountId
        assertEquals(cashId, context.defaultPaymentAccountId)
    }

    @Test
    fun recordPostsSalaryPaymentAndReturnsReceipt() = runBlocking {
        val context = facade.load(ownerSession, DATE)
        val cashId = context.paymentAccounts.single { it.name == "Cash" }.accountId
        val beforeCash = database.readModelDao().accountBalance(cashId)!!.balance
        val eventCountBefore = database.eventDao().count()

        val submission = facade.record(
            ownerSession,
            NativeSalaryPaymentRequest(
                employeeAccountId = employeeId,
                paymentAccountId = cashId,
                amountPaisa = 13_507L,
                notes = "Salary payment contract",
            ),
            DATE,
        )

        assertEquals(eventCountBefore + 1, database.eventDao().count())
        assertEquals(13_507L, submission.receipt.amountPaisa)
        assertEquals(employeeId, submission.receipt.employee.accountId)
        assertEquals(cashId, submission.receipt.paymentAccount.accountId)
        assertEquals(beforeCash - 135.07, database.readModelDao().accountBalance(cashId)!!.balance, 0.000001)
        val actor = database.eventActorLinkDao().find(submission.receipt.eventId)
        assertEquals("Salary Pay Owner", actor!!.actorNameSnapshot)
    }

    @Test
    fun operatorCannotLoadOrRecordSalaryPayment() = runBlocking {
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "salary.pay.operator",
                displayName = "Salary Pay Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        val operator = security.login("salary.pay.operator", OPERATOR_PASSPHRASE.toCharArray())

        val failure = runCatching { facade.load(operator.sessionId, DATE) }.exceptionOrNull()

        assertTrue(failure is SecurityFailure.PermissionDenied)
        assertFalse(runCatching { facade.load(operator.sessionId, DATE) }.isSuccess)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 9, 20)
        const val OWNER_PASSPHRASE = "salary pay owner passphrase 2026"
        const val OPERATOR_PASSPHRASE = "salary pay operator passphrase 2026"
    }
}
