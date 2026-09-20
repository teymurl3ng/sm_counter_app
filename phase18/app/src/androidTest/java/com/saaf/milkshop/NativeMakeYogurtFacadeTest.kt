package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeMakeYogurtFacade
import com.saaf.milkshop.application.operations.NativeMakeYogurtRequest
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
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
class NativeMakeYogurtFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeMakeYogurtFacade
    private lateinit var ownerSession: String
    private lateinit var operatorSession: String
    private var vendorId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            database = MilkShopDatabase.createInMemory()
            security = UserSecurityService(database)
            facade = NativeMakeYogurtFacade(database, security)
            ownerSession = security.bootstrapOwner(
                "yogurt.owner",
                "Yogurt Owner",
                OWNER_PASSPHRASE.toCharArray(),
            ).session.sessionId
            val pending = security.beginUserEnrollment(
                ownerSession,
                CreateUserInput(
                    loginName = "yogurt.operator",
                    displayName = "Yogurt Operator",
                    role = AppRole.OPERATOR.wireValue,
                ),
            )
            security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
            operatorSession = security.login("yogurt.operator", OPERATOR_PASSPHRASE.toCharArray()).sessionId
            vendorId = VendorManagementService(database).create("Yogurt Supply")
            VendorPurchaseService(database).record(
                VendorPurchaseInput(
                    vendorAccountId = vendorId,
                    occurredAt = DATE.toString(),
                    rawLiters = 50.0,
                    fatPercentage = 4.0,
                    lrReading = 28.0,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesStockWithoutRatesOrMargins() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertTrue(context.milkOnHandLiters > 0.0)
        assertTrue(context.milkOnHandLabel.contains("milk", ignoreCase = true))
        assertTrue(context.yogurtOnHandLabel.contains("yogurt", ignoreCase = true))
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("margin") })
        assertFalse(fields.any { it.contains("cost") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("amount") })
    }

    @Test
    fun recordReturnsPhysicalConversionWithoutFinance() = runBlocking {
        val beforeEvents = database.eventDao().count()
        val submission = facade.record(
            operatorSession,
            NativeMakeYogurtRequest(
                milkLitersUsed = 20.0,
                yogurtKgProduced = 15.0,
                notes = "Morning batch",
            ),
            DATE,
        )
        assertEquals(beforeEvents + 1, database.eventDao().count())
        assertEquals(20.0, submission.receipt.milkLitersUsed, 0.0001)
        assertEquals(15.0, submission.receipt.yogurtKgProduced, 0.0001)
        assertTrue(submission.receipt.milkOnHandLabel.contains("milk", ignoreCase = true))
        assertTrue(submission.receipt.yogurtOnHandLabel.contains("yogurt", ignoreCase = true))
        val fields = submission.receipt.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("cost") })
        assertFalse(fields.any { it.contains("margin") })
        assertFalse(fields.any { it.contains("paisa") })
        val actor = database.eventActorLinkDao().find(submission.receipt.eventId)
        assertEquals("Yogurt Operator", actor!!.actorNameSnapshot)
    }

    @Test
    fun insufficientMilkKeepsDraftFriendlyMessage() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        assertFalse(facade.canRecord(context, milkLitersUsed = context.milkOnHandLiters + 5.0, yogurtKgProduced = 1.0))
        try {
            facade.record(
                operatorSession,
                NativeMakeYogurtRequest(
                    milkLitersUsed = context.milkOnHandLiters + 5.0,
                    yogurtKgProduced = 1.0,
                    notes = null,
                ),
                DATE,
            )
            error("Expected insufficient milk failure")
        } catch (error: IllegalStateException) {
            assertTrue(error.message!!.contains("Not enough milk", ignoreCase = true))
        }
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 20)
        private const val OWNER_PASSPHRASE = "yogurt owner passphrase 2026!!"
        private const val OPERATOR_PASSPHRASE = "yogurt operator passphrase 2026!!"
    }
}
