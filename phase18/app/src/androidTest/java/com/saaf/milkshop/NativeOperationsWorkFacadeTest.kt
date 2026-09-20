package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeOperationsAttendanceStatus
import com.saaf.milkshop.application.operations.NativeOperationsWorkFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
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
    fun loadRejectsWhenSessionMissingOperationalPermission() = runBlocking {
        // Use a session that cannot VIEW_OPERATIONAL — none of the seeded roles lack it,
        // so revoke by logging out and attempting with a stale handle.
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
