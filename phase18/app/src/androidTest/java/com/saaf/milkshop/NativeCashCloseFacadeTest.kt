package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseDayRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NativeCashCloseFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeCashCloseFacade
    private lateinit var ownerSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeCashCloseFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "cash.close.owner",
            "Cash Close Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesCashAccountExpectedAndOpenPosting() = runBlocking {
        val context = facade.load(ownerSession, DATE)

        assertTrue(context.postingOpen)
        assertEquals("Cash", context.cashAccountName)
        assertEquals(DATE.toString(), context.businessDate)
        assertNotNull(context.cashAccountId)
        assertEquals(null, context.latestCount)
        assertNull(context.reopenDeferredNotice)
    }

    @Test
    fun recordCountThenCloseDayReturnsReceipt() = runBlocking {
        val loaded = facade.load(ownerSession, DATE)
        val expected = loaded.expectedCashPaisa

        val counted = facade.recordCount(
            ownerSession,
            NativeCashCloseCountRequest(
                countedCashPaisa = expected,
                notes = "Even drawer",
            ),
            DATE,
        )

        assertEquals(expected, counted.count.countedCashPaisa)
        assertEquals(0L, counted.count.variancePaisa)
        assertNotNull(counted.refreshedContext.latestCount)

        val closed = facade.closeDay(
            ownerSession,
            NativeCashCloseDayRequest(
                physicalCashCountId = counted.count.countId,
                varianceReason = null,
            ),
            DATE,
        )

        assertEquals(DATE.toString(), closed.receipt.businessDate)
        assertFalse(closed.refreshedContext.postingOpen)
        assertNull(closed.refreshedContext.reopenDeferredNotice)
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.toString()))
    }

    @Test
    fun closeThenReopenRestoresOpenPosting() = runBlocking {
        val loaded = facade.load(ownerSession, DATE)
        val counted = facade.recordCount(
            ownerSession,
            NativeCashCloseCountRequest(
                countedCashPaisa = loaded.expectedCashPaisa,
                notes = "Even drawer",
            ),
            DATE,
        )
        facade.closeDay(
            ownerSession,
            NativeCashCloseDayRequest(
                physicalCashCountId = counted.count.countId,
                varianceReason = null,
            ),
            DATE,
        )
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.toString()))

        val passphrase = OWNER_PASSPHRASE.toCharArray()
        val reopened = facade.reopenDay(
            ownerSession,
            reason = "Correct a verified cash posting",
            passphrase = passphrase,
            date = DATE,
        )

        assertTrue(passphrase.all { it == '\u0000' })
        assertEquals(DATE.toString(), reopened.businessDate)
        assertTrue(reopened.refreshedContext.postingOpen)
        assertNull(reopened.refreshedContext.reopenDeferredNotice)
        assertFalse(database.dailyCloseDao().isPostingClosed(DATE.toString()))
        assertEquals("reopen", database.dailyCloseDao().currentAction(DATE.toString())!!.action)
    }

    @Test
    fun reopenRejectsBlankReasonAndWrongPassphrase() = runBlocking {
        val loaded = facade.load(ownerSession, DATE)
        val counted = facade.recordCount(
            ownerSession,
            NativeCashCloseCountRequest(
                countedCashPaisa = loaded.expectedCashPaisa,
                notes = null,
            ),
            DATE,
        )
        facade.closeDay(
            ownerSession,
            NativeCashCloseDayRequest(
                physicalCashCountId = counted.count.countId,
                varianceReason = null,
            ),
            DATE,
        )

        val blank = runCatching {
            facade.reopenDay(ownerSession, "  ", OWNER_PASSPHRASE.toCharArray(), DATE)
        }.exceptionOrNull()
        assertTrue(blank is IllegalArgumentException)

        val wrongSecret = "definitely wrong passphrase".toCharArray()
        val wrong = runCatching {
            facade.reopenDay(ownerSession, "Need to reopen", wrongSecret, DATE)
        }.exceptionOrNull()
        assertTrue(wrongSecret.all { it == '\u0000' })
        assertTrue(wrong is SecurityFailure.Authentication)
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.toString()))
    }

    @Test
    fun varianceRequiresReasonBeforeClose() = runBlocking {
        val loaded = facade.load(ownerSession, DATE)
        val counted = facade.recordCount(
            ownerSession,
            NativeCashCloseCountRequest(
                countedCashPaisa = loaded.expectedCashPaisa + 500L,
                notes = null,
            ),
            DATE,
        )
        assertTrue(NativeCashCloseFacade.needsVarianceReason(counted.count.variancePaisa))

        val failure = runCatching {
            facade.closeDay(
                ownerSession,
                NativeCashCloseDayRequest(
                    physicalCashCountId = counted.count.countId,
                    varianceReason = null,
                ),
                DATE,
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("variance", ignoreCase = true))
    }

    @Test
    fun operatorCannotLoadCashClose() = runBlocking {
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "cash.close.operator",
                displayName = "Cash Close Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        val operator = security.login("cash.close.operator", OPERATOR_PASSPHRASE.toCharArray())

        val failure = runCatching { facade.load(operator.sessionId, DATE) }.exceptionOrNull()

        assertTrue(failure is SecurityFailure.PermissionDenied)
    }

    private suspend fun cashierSession(): String {
        val pending = security.beginUserEnrollment(ownerSession, CreateUserInput(
            loginName = "close.cashier", displayName = "Closing cashier", role = AppRole.CASHIER.wireValue,
        ))
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        return security.login("close.cashier", OPERATOR_PASSPHRASE.toCharArray()).sessionId
    }

    @Test
    fun cashierClosesOwnDayNextDateOpensAndReopenIsDenied() = runBlocking {
        val session = cashierSession()
        val count = facade.recordCount(session, NativeCashCloseCountRequest(
            countedCashPaisa = facade.load(session, DATE).expectedCashPaisa, notes = null,
        ), DATE).count
        facade.closeDay(session, NativeCashCloseDayRequest(count.countId, null), DATE)
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.toString()))
        assertTrue(facade.load(session, DATE.plusDays(1)).postingOpen)
        val failure = runCatching {
            facade.reopenDay(session, "Accidental close", OPERATOR_PASSPHRASE.toCharArray(), DATE)
        }.exceptionOrNull()
        assertTrue(failure is SecurityFailure.PermissionDenied)
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.toString()))
    }

    @Test
    fun cashMovementAfterCountRefusesCloseWithoutPartialWrite() = runBlocking {
        val session = cashierSession()
        val count = facade.recordCount(session, NativeCashCloseCountRequest(0L, null), DATE).count
        val expense = com.saaf.milkshop.application.expense.NativeExpenseFacade(database, security)
        val context = expense.load(session, DATE)
        expense.record(session, com.saaf.milkshop.application.expense.NativeExpenseRequest(
            expenseCategoryAccountId = context.categories.first().accountId,
            paymentAccountId = requireNotNull(context.defaultPaymentAccountId),
            amountPaisa = 100L, notes = "After count",
        ), DATE)
        val events = database.eventDao().count()
        val failure = runCatching {
            facade.closeDay(session, NativeCashCloseDayRequest(count.countId, null), DATE)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("count", ignoreCase = true))
        assertFalse(database.dailyCloseDao().isPostingClosed(DATE.toString()))
        assertEquals(events, database.eventDao().count())
    }

    @Test
    fun pendingDeliveryRefusesCloseUntilResolved() = runBlocking {
        val session = cashierSession()
        val customer = com.saaf.milkshop.domain.CustomerManagementService(database).create("Close customer")
        val milk = database.productDao().all().first { it.name == "Milk" }
        val delivery = com.saaf.milkshop.domain.DeliveryService(database)
        val id = delivery.create(com.saaf.milkshop.domain.DeliveryInput(
            customerAccountId = customer, productId = milk.productId,
            quantity = 1.0, businessDate = DATE.toString(),
        ))
        val count = facade.recordCount(session, NativeCashCloseCountRequest(0L, null), DATE).count
        val failure = runCatching {
            facade.closeDay(session, NativeCashCloseDayRequest(count.countId, null), DATE)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertFalse(database.dailyCloseDao().isPostingClosed(DATE.toString()))
        facade.resolvePendingDelivery(session, id, DATE)
        facade.closeDay(session, NativeCashCloseDayRequest(count.countId, null), DATE)
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.toString()))
    }

    @Test
    fun earlierOpenDeliveryRefusesCloseUntilCashierResolvesIt() = runBlocking {
        val session = cashierSession()
        val customer = com.saaf.milkshop.domain.CustomerManagementService(database).create("Close customer")
        val milk = database.productDao().all().first { it.name == "Milk" }
        val delivery = com.saaf.milkshop.domain.DeliveryService(database)
        val id = delivery.create(com.saaf.milkshop.domain.DeliveryInput(
            customerAccountId = customer, productId = milk.productId,
            quantity = 1.0, businessDate = DATE.minusDays(1).toString(),
        ))
        val count = facade.recordCount(session, NativeCashCloseCountRequest(0L, null), DATE).count
        val eventsBefore = database.eventDao().count()
        assertTrue(facade.load(session, DATE).pendingDeliveries.single().label.contains(DATE.minusDays(1).toString()))
        val failure = runCatching {
            facade.closeDay(session, NativeCashCloseDayRequest(count.countId, null), DATE)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertFalse(database.dailyCloseDao().isPostingClosed(DATE.toString()))
        assertEquals(eventsBefore, database.eventDao().count())
        assertFalse(database.dailyCloseDao().isPostingClosed(DATE.minusDays(1).toString()))
        facade.resolvePendingDelivery(session, id, DATE)
        facade.closeDay(session, NativeCashCloseDayRequest(count.countId, null), DATE)
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.toString()))
        assertTrue(database.dailyCloseDao().isPostingClosed(DATE.minusDays(1).toString()))
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 9, 20)
        const val OWNER_PASSPHRASE = "cash close owner passphrase 2026"
        const val OPERATOR_PASSPHRASE = "cash close operator passphrase 2026"
    }
}
