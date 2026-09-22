package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerEmployeesFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.EmployeeProfileService
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerEmployeesFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerEmployeesFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerEmployeesFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.employees",
            "Employees Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "employees.cashier",
                displayName = "Employees Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("employees.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadEmployeesListsAccountsWithBalanceAndStatus() = runBlocking {
        val management = EmployeeManagementService(database)
        val activeId = management.create("Ayesha Khan")
        val inactiveId = management.create("Former Helper")
        EmployeeProfileService(database).save(
            activeId,
            EmployeeProfileInput(role = "Delivery rider", phone = "0300"),
        )
        EmployeeProfileService(database).save(
            inactiveId,
            EmployeeProfileInput(role = "Shop helper", active = false),
        )

        val snapshot = facade.loadEmployees(ownerSession)
        assertTrue(snapshot.employees.any { it.accountId == activeId && it.name == "Ayesha Khan" })
        assertTrue(snapshot.employees.any { it.accountId == inactiveId && it.statusLabel == "Inactive" })
        val active = snapshot.employees.first { it.accountId == activeId }
        assertEquals("Active", active.statusLabel)
        assertEquals("Delivery rider", active.role)
        assertEquals("0300", active.phone)
        assertTrue(active.balanceLabel.contains("PKR"))
    }

    @Test
    fun loadDetailReturnsProfileFacts() = runBlocking {
        val accountId = EmployeeManagementService(database).create("Owner Employees Detail Rider")
        EmployeeProfileService(database).save(
            accountId,
            EmployeeProfileInput(
                role = "Delivery rider",
                phone = "0301",
                referenceSalary = 35_000.0,
                salaryPeriod = "monthly",
                joinDate = "2026-01-15",
            ),
        )
        val detail = facade.loadDetail(ownerSession, accountId)
        assertEquals("Owner Employees Detail Rider", detail.row.name)
        assertEquals("Active", detail.row.statusLabel)
        assertEquals("Delivery rider", detail.row.role)
        assertEquals("0301", detail.row.phone)
        assertEquals(35_000.0, detail.referenceSalary!!, 0.001)
        assertEquals("PKR 35,000.00", detail.referenceSalaryLabel)
        assertEquals("monthly", detail.salaryPeriod)
        assertEquals("2026-01-15", detail.joinDate)
        assertTrue(detail.active)
    }

    @Test
    fun createEmployeeThenListContains() = runBlocking {
        val accountId = facade.createEmployee(ownerSession, "Owner Create Rider")
        val snapshot = facade.loadEmployees(ownerSession)
        assertTrue(
            snapshot.employees.any {
                it.accountId == accountId && it.name == "Owner Create Rider"
            },
        )
        val created = snapshot.employees.first { it.accountId == accountId }
        assertEquals("Active", created.statusLabel)
        assertTrue(created.balanceLabel.contains("PKR"))
    }

    @Test
    fun updateEmployeeRenamesAndSavesProfile() = runBlocking {
        val accountId = facade.createEmployee(ownerSession, "Owner Edit Rider")
        facade.updateEmployee(
            ownerSession,
            accountId,
            "Owner Edit Rider Updated",
            EmployeeProfileInput(
                role = "Shop helper",
                phone = "0322",
                referenceSalary = 40_000.0,
                salaryPeriod = "monthly",
                joinDate = "2026-03-01",
                active = true,
            ),
        )
        val detail = facade.loadDetail(ownerSession, accountId)
        assertEquals("Owner Edit Rider Updated", detail.row.name)
        assertEquals("Shop helper", detail.row.role)
        assertEquals("0322", detail.row.phone)
        assertEquals(40_000.0, detail.referenceSalary!!, 0.001)
        assertEquals("monthly", detail.salaryPeriod)
        assertEquals("2026-03-01", detail.joinDate)
        assertTrue(detail.active)
        assertEquals("Active", detail.row.statusLabel)
    }

    @Test
    fun updateEmployeeCanDeactivateViaActiveFlag() = runBlocking {
        val accountId = facade.createEmployee(ownerSession, "Owner Deactivate Rider")
        facade.updateEmployee(
            ownerSession,
            accountId,
            "Owner Deactivate Rider",
            EmployeeProfileInput(active = false),
        )
        val detail = facade.loadDetail(ownerSession, accountId)
        assertFalse(detail.active)
        assertEquals("Inactive", detail.row.statusLabel)
        val snapshot = facade.loadEmployees(ownerSession)
        assertTrue(
            snapshot.employees.any {
                it.accountId == accountId && it.statusLabel == "Inactive"
            },
        )
    }

    @Test
    fun createEmployeeRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.createEmployee(cashierSession, "Blocked Employee") }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun updateEmployeeRejectsNonOwner() = runBlocking {
        val accountId = facade.createEmployee(ownerSession, "Owner Gate Rider")
        val rejected = runCatching {
            facade.updateEmployee(
                cashierSession,
                accountId,
                "Blocked Rename",
                EmployeeProfileInput(phone = "0000"),
            )
        }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.loadEmployees(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    companion object {
        private const val OWNER_PASSPHRASE = "owner employees passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "employees cashier passphrase 2026!!"
    }
}
