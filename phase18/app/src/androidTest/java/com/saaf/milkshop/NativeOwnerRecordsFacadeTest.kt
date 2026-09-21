package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.application.owner.NativeOwnerRecordsFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
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
class NativeOwnerRecordsFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerRecordsFacade
    private lateinit var collectionFacade: NativeCollectionFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerRecordsFacade(database, security)
        collectionFacade = NativeCollectionFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.records",
            "Records Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "records.cashier",
                displayName = "Records Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("records.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun historyIsEmptyUntilWorkIsRecorded() = runBlocking {
        val snapshot = facade.loadHistory(ownerSession, DATE)
        assertTrue(snapshot.records.isEmpty())
        assertEquals(DATE.toString(), snapshot.context.businessDate)
        assertTrue(snapshot.context.postingOpen)
    }

    @Test
    fun historyIsShopWideAcrossActors() = runBlocking {
        val customerId = CustomerManagementService(database).create("Records Customer")
        val cashId = collectionFacade.load(cashierSession, DATE)
            .paymentAccounts.single { it.name == "Cash" }.accountId
        val cashierEvent = collectionFacade.record(
            cashierSession,
            NativeCollectionRequest(customerId, cashId, 25_000L, "Cashier note"),
            DATE,
        ).receipt.eventId
        val ownerEvent = collectionFacade.record(
            ownerSession,
            NativeCollectionRequest(customerId, cashId, 10_000L, "Owner note"),
            DATE,
        ).receipt.eventId

        val snapshot = facade.loadHistory(ownerSession, DATE)
        assertEquals(setOf(cashierEvent, ownerEvent), snapshot.records.map { it.eventId }.toSet())
        assertTrue(snapshot.records.any { it.actorLabel == "Records Cashier" })
        assertTrue(snapshot.records.any { it.actorLabel == "Records Owner" })

        val detail = facade.loadDetail(ownerSession, cashierEvent)
        assertEquals(cashierEvent, detail.row.eventId)
        assertEquals("Cashier note", detail.notes)
        assertTrue(detail.financialMovements != null)
    }

    @Test
    fun searchFilterMatchesActorAndNotes() = runBlocking {
        val customerId = CustomerManagementService(database).create("Filter Customer")
        val cashId = collectionFacade.load(ownerSession, DATE)
            .paymentAccounts.single { it.name == "Cash" }.accountId
        collectionFacade.record(
            ownerSession,
            NativeCollectionRequest(customerId, cashId, 12_000L, "Alpha note"),
            DATE,
        )
        collectionFacade.record(
            cashierSession,
            NativeCollectionRequest(customerId, cashId, 8_000L, "Beta note"),
            DATE,
        )
        val snapshot = facade.loadHistory(ownerSession, DATE)
        val filtered = facade.filterRecords(snapshot.records, "alpha")
        assertEquals(1, filtered.size)
        assertTrue(filtered.single().notes!!.contains("Alpha", ignoreCase = true))
        assertTrue(facade.filterRecords(snapshot.records, "zzz-missing").isEmpty())
    }

    @Test
    fun loadRejectsNonOwnerSession() = runBlocking {
        val failure = runCatching { facade.loadHistory(cashierSession, DATE) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("Owner"))
    }

    @Test
    fun loadRejectsWhenSessionMissing() = runBlocking {
        security.logout(ownerSession)
        val failure = runCatching { facade.loadHistory(ownerSession, DATE) }.exceptionOrNull()
        assertTrue(failure != null)
        assertFalse(failure is IllegalArgumentException && failure.message!!.contains("only available"))
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 21)
        private const val OWNER_PASSPHRASE = "owner records passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "owner records cashier passphrase 2026!!"
    }
}
