package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeOperationsAttendanceStatus
import com.saaf.milkshop.application.operations.NativeOperationsWorkFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class NativeOperationsWorkFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOperationsWorkFacade
    private lateinit var ownerSession: String
    private lateinit var operatorSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOperationsWorkFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "ops.owner",
            "Ops Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "ops.operator",
                displayName = "Ops Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        operatorSession = security.login("ops.operator", OPERATOR_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesPhysicalContextWithoutFinanceFields() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertTrue(context.milkProductPresent)
        assertTrue(context.milkOnHandLabel.isNotBlank())
        assertEquals(0, context.activeDeliveryCount)
        assertEquals(NativeOperationsAttendanceStatus.Unlinked, context.attendance)
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("payable") })
        assertFalse(fields.any { it.contains("variance") })
        assertFalse(fields.any { it.contains("margin") })
        assertFalse(fields.any { it.contains("loss") })
        assertFalse(fields.any { it.contains("cash") })
        assertFalse(fields.any { it.contains("balance") })
    }

    @Test
    fun loadUsesEffectivePurchaseNotSupersededOriginal() = runBlocking {
        val vendorId = VendorManagementService(database).create("Ops Dairy")
        val purchase = VendorPurchaseService(database).record(
            VendorPurchaseInput(
                vendorAccountId = vendorId,
                occurredAt = DATE.toString(),
                rawLiters = 40.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
            ),
        )
        CorrectionService(database).correct(
            CorrectionInput.Purchase(
                originalEventId = purchase.eventId,
                occurredAt = DATE.toString(),
                rawLiters = 45.0,
                fatPercentage = 4.1,
                lrReading = 28.2,
            ),
        )
        val correction = database.eventDao().findAllDescending().single {
            it.correctsEventId == purchase.eventId
        }
        val context = facade.load(operatorSession, DATE)
        val expected = runCatching {
            OffsetDateTime.parse(correction.createdAt).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalTime()
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        }.getOrElse { correction.createdAt }
        assertEquals(expected, context.latestReceiptLabel)
        assertNotEquals(purchase.eventId, correction.eventId)
    }

    @Test
    fun loadRejectsWhenSessionMissingOperationalPermission() = runBlocking {
        security.logout(operatorSession)
        val failure = runCatching { facade.load(operatorSession, DATE) }.exceptionOrNull()
        assertTrue(failure != null)
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 9, 20)
        const val OWNER_PASSPHRASE = "ops owner passphrase 2026"
        const val OPERATOR_PASSPHRASE = "ops operator passphrase 2026"
    }
}
