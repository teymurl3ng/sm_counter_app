package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeReceiveMilkFacade
import com.saaf.milkshop.application.operations.NativeReceiveMilkRequest
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
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
class NativeReceiveMilkFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeReceiveMilkFacade
    private lateinit var ownerSession: String
    private lateinit var operatorSession: String
    private var vendorId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeReceiveMilkFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "receive.owner",
            "Receive Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "receive.operator",
                displayName = "Receive Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        operatorSession = security.login("receive.operator", OPERATOR_PASSPHRASE.toCharArray()).sessionId
        vendorId = VendorManagementService(database).create("Dawn Contract Supply")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesSuppliersWithoutBalancesOrRates() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertEquals(setOf(vendorId), context.vendors.map { it.accountId }.toSet())
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("fee") })
        assertFalse(fields.any { it.contains("owed") })
        assertFalse(fields.any { it.contains("payable") })
        assertFalse(fields.any { it.contains("balance") })
        assertFalse(fields.any { it.contains("amount") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("cash") })
    }

    @Test
    fun previewAndRecordReturnPhysicalValuesWithoutAmountOwed() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        val preview = facade.preview(context, rawLiters = 40.0, fatPercentage = 4.0, lrReading = 28.0)
        assertNotNull(preview)
        assertTrue(preview!!.snfPercentage > 0.0)
        assertTrue(preview.tsPercentage > 0.0)
        assertTrue(preview.adjustedLiters > 0.0)

        val beforeEvents = database.eventDao().count()
        val submission = facade.record(
            operatorSession,
            NativeReceiveMilkRequest(
                vendorAccountId = vendorId,
                rawLiters = 40.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
                notes = "Morning intake",
            ),
            DATE,
        )
        assertEquals(beforeEvents + 1, database.eventDao().count())
        assertEquals(vendorId, submission.receipt.vendor.accountId)
        assertEquals(40.0, submission.receipt.rawLiters, 0.0001)
        assertTrue(submission.receipt.milkOnHandLabel.contains("milk", ignoreCase = true))
        val receiptFields = submission.receipt.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(receiptFields.any { it.contains("owed") })
        assertFalse(receiptFields.any { it.contains("rate") })
        assertFalse(receiptFields.any { it.contains("fee") })
        assertFalse(receiptFields.any { it.contains("amount") })
        assertFalse(receiptFields.any { it.contains("paisa") })
        val actor = database.eventActorLinkDao().find(submission.receipt.eventId)
        assertEquals("Receive Operator", actor!!.actorNameSnapshot)
        // Domain still stores amount owed — Operator projection must not expose it.
        assertNotNull(database.eventDao().findById(submission.receipt.eventId)!!.amountOwed)
    }

    @Test
    fun cashierCannotLoadReceiveMilk() = runBlocking {
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "receive.cashier",
                displayName = "Receive Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        val cashier = security.login("receive.cashier", CASHIER_PASSPHRASE.toCharArray())
        val failed = runCatching { facade.load(cashier.sessionId, DATE) }.isFailure
        assertTrue(failed)
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 20)
        private const val OWNER_PASSPHRASE = "receive owner passphrase 2026!!"
        private const val OPERATOR_PASSPHRASE = "receive operator passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "receive cashier passphrase 2026!!"
    }
}
