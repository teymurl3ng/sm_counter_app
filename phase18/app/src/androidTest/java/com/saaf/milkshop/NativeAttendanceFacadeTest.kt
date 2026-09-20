package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeAttendanceFacade
import com.saaf.milkshop.application.operations.NativeAttendanceLink
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.EmployeeManagementService
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
class NativeAttendanceFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeAttendanceFacade
    private lateinit var ownerSession: String
    private lateinit var operatorSession: String
    private var employeeId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            database = MilkShopDatabase.createInMemory()
            security = UserSecurityService(database)
            facade = NativeAttendanceFacade(database, security)
            ownerSession = security.bootstrapOwner(
                "attendance.owner",
                "Attendance Owner",
                OWNER_PASSPHRASE.toCharArray(),
            ).session.sessionId
            employeeId = EmployeeManagementService(database).create("Attendance Staff")
            val pending = security.beginUserEnrollment(
                ownerSession,
                CreateUserInput(
                    loginName = "attendance.operator",
                    displayName = "Attendance Operator",
                    role = AppRole.OPERATOR.wireValue,
                    employeeAccountId = employeeId,
                ),
            )
            security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
            operatorSession = security.login("attendance.operator", OPERATOR_PASSPHRASE.toCharArray()).sessionId
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesLinkedEmployeeWithoutFinanceFields() = runBlocking {
        val context = facade.load(operatorSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        val linked = context.link as NativeAttendanceLink.Linked
        assertEquals(employeeId, linked.employeeAccountId)
        assertTrue(linked.employeeName.contains("Attendance Staff", ignoreCase = true))
        assertEquals(null, linked.todayStatus)
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("salary") })
        assertFalse(fields.any { it.contains("balance") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("amount") })
        assertFalse(fields.any { it.contains("rate") })
    }

    @Test
    fun markPresentWritesTodayStatus() = runBlocking {
        val refreshed = facade.markPresent(operatorSession, DATE)
        val linked = refreshed.link as NativeAttendanceLink.Linked
        assertEquals("Present", linked.todayStatus)
        assertTrue(linked.attendanceId != null)
        val again = facade.markPresent(operatorSession, DATE)
        assertEquals(linked.attendanceId, (again.link as NativeAttendanceLink.Linked).attendanceId)
    }

    @Test
    fun unlinkedAccountShowsUnlinkedWithoutMark() = runBlocking {
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "attendance.unlinked",
                displayName = "Unlinked Operator",
                role = AppRole.OPERATOR.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
        val session = security.login("attendance.unlinked", OPERATOR_PASSPHRASE.toCharArray()).sessionId
        val context = facade.load(session, DATE)
        assertEquals(NativeAttendanceLink.Unlinked, context.link)
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 20)
        private const val OWNER_PASSPHRASE = "attendance owner passphrase 2026!!"
        private const val OPERATOR_PASSPHRASE = "attendance operator passphrase 2026!!"
    }
}
