package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeRecordSpoilageFacade
import com.saaf.milkshop.application.operations.NativeRecordSpoilageRequest
import com.saaf.milkshop.application.operations.NativeSpoilageReason
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
class NativeRecordSpoilageFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeRecordSpoilageFacade
    private lateinit var ownerSession: String
    private lateinit var operatorSession: String
    private var vendorId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            database = MilkShopDatabase.createInMemory()
            security = UserSecurityService(database)
            facade = NativeRecordSpoilageFacade(database, security)
            ownerSession = security.bootstrapOwner(
                "spoilage.owner",
                "Spoilage Owner",
                OWNER_PASSPHRASE.toCharArray(),
            ).session.sessionId
            val pending = security.beginUserEnrollment(
                ownerSession,
                CreateUserInput(
                    loginName = "spoilage.operator",
                    displayName = "Spoilage Operator",
                    role = AppRole.OPERATOR.wireValue,
                ),
            )
            security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
            operatorSession = security.login("spoilage.operator", OPERATOR_PASSPHRASE.toCharArray()).sessionId
            vendorId = VendorManagementService(database).create("Spoilage Supply")
            VendorPurchaseService(database).record(
                VendorPurchaseInput(
                    vendorAccountId = vendorId,
                    occurredAt = DATE.toString(),
                    rawLiters = 40.0,
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
    fun loadExposesProductsWithoutRatesOrMargins() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertTrue(context.products.isNotEmpty())
        assertTrue(context.products.any { it.name.equals("Milk", ignoreCase = true) })
        assertEquals(NativeSpoilageReason.entries.toList(), context.reasons)
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("margin") })
        assertFalse(fields.any { it.contains("cost") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("amount") })
        assertFalse(fields.any { it.contains("loss") })
    }

    @Test
    fun recordReturnsPhysicalRemovalWithoutFinance() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        val milk = context.products.single { it.name.equals("Milk", ignoreCase = true) }
        val beforeEvents = database.eventDao().count()
        val submission = facade.record(
            operatorSession,
            NativeRecordSpoilageRequest(
                productId = milk.productId,
                quantity = 2.0,
                reasonCode = NativeSpoilageReason.SPOILED.code,
                notes = null,
            ),
            DATE,
        )
        assertEquals(beforeEvents + 1, database.eventDao().count())
        assertEquals(2.0, submission.receipt.quantity, 0.0001)
        assertEquals(NativeSpoilageReason.SPOILED.code, submission.receipt.reasonCode)
        assertTrue(submission.receipt.remainingLabel.contains("L", ignoreCase = true))
        val fields = submission.receipt.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("cost") })
        assertFalse(fields.any { it.contains("margin") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("loss") })
        val actor = database.eventActorLinkDao().find(submission.receipt.eventId)
        assertEquals("Spoilage Operator", actor!!.actorNameSnapshot)
    }

    @Test
    fun insufficientStockKeepsDraftFriendlyMessage() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        val milk = context.products.single { it.name.equals("Milk", ignoreCase = true) }
        try {
            facade.record(
                operatorSession,
                NativeRecordSpoilageRequest(
                    productId = milk.productId,
                    quantity = milk.onHand + 5.0,
                    reasonCode = NativeSpoilageReason.SPILL.code,
                    notes = null,
                ),
                DATE,
            )
            error("Expected insufficient stock failure")
        } catch (error: IllegalStateException) {
            assertTrue(error.message!!.contains("Not enough", ignoreCase = true))
        }
    }

    @Test
    fun otherReasonRequiresNote() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        val milk = context.products.single { it.name.equals("Milk", ignoreCase = true) }
        try {
            facade.record(
                operatorSession,
                NativeRecordSpoilageRequest(
                    productId = milk.productId,
                    quantity = 1.0,
                    reasonCode = NativeSpoilageReason.OTHER.code,
                    notes = "   ",
                ),
                DATE,
            )
            error("Expected note required failure")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("note", ignoreCase = true))
        }
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 20)
        private const val OWNER_PASSPHRASE = "spoilage owner passphrase 2026!!"
        private const val OPERATOR_PASSPHRASE = "spoilage operator passphrase 2026!!"
    }
}
