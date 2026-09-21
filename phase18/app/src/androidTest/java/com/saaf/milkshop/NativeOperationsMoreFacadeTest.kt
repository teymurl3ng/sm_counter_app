package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeOperationsMoreFacade
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NativeOperationsMoreFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOperationsMoreFacade
    private lateinit var receiveFacade: NativeReceiveMilkFacade
    private lateinit var ownerSession: String
    private lateinit var operatorSession: String
    private var vendorId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            database = MilkShopDatabase.createInMemory()
            security = UserSecurityService(database)
            facade = NativeOperationsMoreFacade(database, security)
            receiveFacade = NativeReceiveMilkFacade(database, security)
            ownerSession = security.bootstrapOwner(
                "opsmore.owner",
                "Ops More Owner",
                OWNER_PASSPHRASE.toCharArray(),
            ).session.sessionId
            val pending = security.beginUserEnrollment(
                ownerSession,
                CreateUserInput(
                    loginName = "opsmore.operator",
                    displayName = "Ops More Operator",
                    role = AppRole.OPERATOR.wireValue,
                ),
            )
            security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
            operatorSession = security.login("opsmore.operator", OPERATOR_PASSPHRASE.toCharArray()).sessionId
            vendorId = VendorManagementService(database).create("Ops More Dairy")
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesPostingContextWithoutCashFields() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("cash") })
        assertFalse(fields.any { it.contains("amount") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("expected") })
        assertFalse(fields.any { it.contains("variance") })
    }

    @Test
    fun todayWorkListsOwnPhysicalReceiveWithoutMoney() = runBlocking {
        receiveFacade.record(
            operatorSession,
            NativeReceiveMilkRequest(
                vendorAccountId = vendorId,
                rawLiters = 12.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
                notes = null,
            ),
            DATE,
        )
        val today = facade.loadTodayWork(operatorSession, DATE)
        assertEquals(1, today.records.size)
        val row = today.records.single()
        assertEquals("Receive milk", row.taskLabel)
        assertTrue(row.detailLabel.contains("12"))
        val fields = row.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("amount") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("balance") })
        assertFalse(fields.any { it.contains("cost") })
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 20)
        private const val OWNER_PASSPHRASE = "ops more owner passphrase 2026!!"
        private const val OPERATOR_PASSPHRASE = "ops more operator passphrase 2026!!"
    }
}
