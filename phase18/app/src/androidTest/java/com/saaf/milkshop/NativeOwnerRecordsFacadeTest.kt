package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.application.owner.NativeOwnerRecordsFacade
import com.saaf.milkshop.application.owner.NativeRecordsGroup
import com.saaf.milkshop.data.DailyCloseActionEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NativeOwnerRecordsFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerRecordsFacade
    private lateinit var collectionFacade: NativeCollectionFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String
    private lateinit var managerSession: String

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
        val cashierPending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "records.cashier",
                displayName = "Records Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(cashierPending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("records.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
        val managerPending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "records.manager",
                displayName = "Records Manager",
                role = AppRole.MANAGER.wireValue,
            ),
        )
        security.completeUserEnrollment(managerPending.enrollmentToken, MANAGER_PASSPHRASE.toCharArray())
        managerSession = security.login("records.manager", MANAGER_PASSPHRASE.toCharArray()).sessionId
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
        assertTrue(detail.canCorrect)
        assertNotNull(detail.correctionPrefill)
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
    fun correctionsGroupFiltersLineage() = runBlocking {
        val customerId = CustomerManagementService(database).create("Correction Filter Customer")
        val cashId = collectionFacade.load(ownerSession, DATE)
            .paymentAccounts.single { it.name == "Cash" }.accountId
        val originalId = collectionFacade.record(
            ownerSession,
            NativeCollectionRequest(customerId, cashId, 20_000L, "Original"),
            DATE,
        ).receipt.eventId
        val correction = facade.correct(
            ownerSession,
            CorrectionInput.Monetary(originalId, DATE.toString(), 180.0, "Adjusted"),
        )
        val snapshot = facade.loadHistory(ownerSession, DATE)
        val corrections = facade.filterRecords(
            snapshot.records,
            query = "",
            group = NativeRecordsGroup.Corrections,
        )
        assertEquals(setOf(originalId, correction.eventId), corrections.map { it.eventId }.toSet())
        assertTrue(corrections.any { it.isCorrection })
        assertTrue(corrections.any { it.isSuperseded })
    }

    @Test
    fun correctPostsMonetaryDeltaAndRefreshesDetail() = runBlocking {
        val customerId = CustomerManagementService(database).create("Correct Customer")
        val cashId = collectionFacade.load(ownerSession, DATE)
            .paymentAccounts.single { it.name == "Cash" }.accountId
        val originalId = collectionFacade.record(
            ownerSession,
            NativeCollectionRequest(customerId, cashId, 25_000L, "Collect"),
            DATE,
        ).receipt.eventId

        val result = facade.correct(
            ownerSession,
            CorrectionInput.Monetary(originalId, DATE.toString(), 200.0, "Corrected amount"),
        )
        assertTrue(result.eventId > originalId)
        val detail = facade.loadDetail(ownerSession, result.eventId)
        assertTrue(detail.row.isCorrection)
        assertEquals(originalId, detail.row.correctsEventId)
        assertTrue(detail.canCorrect)

        val originalDetail = facade.loadDetail(ownerSession, originalId)
        assertTrue(originalDetail.row.isSuperseded)
        assertFalse(originalDetail.canCorrect)
    }

    @Test
    fun managerCanCorrect() = runBlocking {
        val customerId = CustomerManagementService(database).create("Manager Correct Customer")
        val cashId = collectionFacade.load(managerSession, DATE)
            .paymentAccounts.single { it.name == "Cash" }.accountId
        val originalId = collectionFacade.record(
            managerSession,
            NativeCollectionRequest(customerId, cashId, 15_000L, "Manager collect"),
            DATE,
        ).receipt.eventId
        val result = facade.correct(
            managerSession,
            CorrectionInput.Monetary(originalId, DATE.toString(), 140.0, null),
        )
        assertTrue(result.eventId > originalId)
        val detail = facade.loadDetail(managerSession, result.eventId)
        assertTrue(detail.row.isCorrection)
    }

    @Test
    fun correctRejectsAlreadyReplaced() = runBlocking {
        val customerId = CustomerManagementService(database).create("Replaced Customer")
        val cashId = collectionFacade.load(ownerSession, DATE)
            .paymentAccounts.single { it.name == "Cash" }.accountId
        val originalId = collectionFacade.record(
            ownerSession,
            NativeCollectionRequest(customerId, cashId, 11_000L, null),
            DATE,
        ).receipt.eventId
        facade.correct(
            ownerSession,
            CorrectionInput.Monetary(originalId, DATE.toString(), 100.0, null),
        )
        val failure = runCatching {
            facade.correct(
                ownerSession,
                CorrectionInput.Monetary(originalId, DATE.toString(), 90.0, null),
            )
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertTrue(failure!!.message!!.contains("already replaced", ignoreCase = true))
    }

    @Test
    fun correctRejectsClosedDay() = runBlocking {
        val customerId = CustomerManagementService(database).create("Closed Day Customer")
        val cashId = collectionFacade.load(ownerSession, DATE)
            .paymentAccounts.single { it.name == "Cash" }.accountId
        val originalId = collectionFacade.record(
            ownerSession,
            NativeCollectionRequest(customerId, cashId, 13_000L, null),
            DATE,
        ).receipt.eventId
        database.dailyCloseDao().insert(
            DailyCloseActionEntity(
                businessDate = DATE.toString(),
                action = "close",
                actorName = "Records Owner",
                actorRole = AppRole.OWNER.wireValue,
                createdAt = Instant.now().toString(),
            ),
        )
        val failure = runCatching {
            facade.correct(
                ownerSession,
                CorrectionInput.Monetary(originalId, DATE.toString(), 120.0, null),
            )
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertTrue(failure!!.message!!.contains("closed", ignoreCase = true))
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
        private const val MANAGER_PASSPHRASE = "owner records manager passphrase 2026!!"
    }
}
