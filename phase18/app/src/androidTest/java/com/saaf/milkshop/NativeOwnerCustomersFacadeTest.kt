package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.CustomerProfileService
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerCustomersFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerCustomersFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerCustomersFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.customers",
            "Customers Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "customers.cashier",
                displayName = "Customers Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("customers.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadCustomersListsAccountsWithBalanceAndStatus() = runBlocking {
        val management = CustomerManagementService(database)
        val activeId = management.create("Corner Cafe")
        val inactiveId = management.create("Old Route Shop")
        CustomerProfileService(database).save(
            activeId,
            CustomerProfileInput(phone = "0300", areaRoute = "Route A", creditLimit = 5_000.0),
        )
        CustomerProfileService(database).save(
            inactiveId,
            CustomerProfileInput(areaRoute = "Old Route", active = false),
        )

        val snapshot = facade.loadCustomers(ownerSession)
        assertTrue(snapshot.customers.any { it.accountId == activeId && it.name == "Corner Cafe" })
        assertTrue(snapshot.customers.any { it.accountId == inactiveId && it.statusLabel == "Inactive" })
        val cafe = snapshot.customers.first { it.accountId == activeId }
        assertEquals("Active", cafe.statusLabel)
        assertEquals("0300", cafe.phone)
        assertEquals("Route A", cafe.areaRoute)
        assertTrue(cafe.balanceLabel.contains("PKR"))
    }

    @Test
    fun loadDetailReturnsProfileFacts() = runBlocking {
        val accountId = CustomerManagementService(database).create("Owner Customers Detail Cafe")
        CustomerProfileService(database).save(
            accountId,
            CustomerProfileInput(
                phone = "0301",
                areaRoute = "Route B",
                address = "Main Market",
                notes = "Friday collect",
                creditLimit = 12_000.0,
                dailyQuota = 25.0,
            ),
        )
        val detail = facade.loadDetail(ownerSession, accountId)
        assertEquals("Owner Customers Detail Cafe", detail.row.name)
        assertEquals("Active", detail.row.statusLabel)
        assertEquals("0301", detail.row.phone)
        assertEquals("Route B", detail.row.areaRoute)
        assertEquals("Main Market", detail.address)
        assertEquals("Friday collect", detail.notes)
        assertTrue(detail.creditLimitLabel!!.contains("12,000"))
        assertTrue(detail.dailyQuotaLabel!!.contains("25"))
    }

    @Test
    fun createCustomerThenListContains() = runBlocking {
        val accountId = facade.createCustomer(ownerSession, "Owner Create Cafe")
        val snapshot = facade.loadCustomers(ownerSession)
        assertTrue(
            snapshot.customers.any {
                it.accountId == accountId && it.name == "Owner Create Cafe"
            },
        )
        val created = snapshot.customers.first { it.accountId == accountId }
        assertEquals("Active", created.statusLabel)
        assertTrue(created.balanceLabel.contains("PKR"))
    }

    @Test
    fun createCustomerRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.createCustomer(cashierSession, "Blocked Customer") }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.loadCustomers(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    companion object {
        private const val OWNER_PASSPHRASE = "owner customers passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "customers cashier passphrase 2026!!"
    }
}
