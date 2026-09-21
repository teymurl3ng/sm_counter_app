package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.VendorProfileService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerSuppliersFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerSuppliersFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerSuppliersFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.suppliers",
            "Suppliers Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "suppliers.cashier",
                displayName = "Suppliers Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("suppliers.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadSuppliersListsAccountsWithBalanceAndStatus() = runBlocking {
        val management = VendorManagementService(database)
        val activeId = management.create("Dawn Dairy")
        val inactiveId = management.create("Old Canal Farm")
        VendorProfileService(database).save(
            activeId,
            VendorProfileInput(phone = "0300", location = "Canal Road"),
        )
        VendorProfileService(database).save(
            inactiveId,
            VendorProfileInput(location = "Old Route", active = false),
        )

        val snapshot = facade.loadSuppliers(ownerSession)
        assertTrue(snapshot.suppliers.any { it.accountId == activeId && it.name == "Dawn Dairy" })
        assertTrue(snapshot.suppliers.any { it.accountId == inactiveId && it.statusLabel == "Inactive" })
        val dairy = snapshot.suppliers.first { it.accountId == activeId }
        assertEquals("Active", dairy.statusLabel)
        assertEquals("0300", dairy.phone)
        assertEquals("Canal Road", dairy.location)
        assertTrue(dairy.balanceLabel.contains("PKR"))
    }

    @Test
    fun loadDetailReturnsProfileFacts() = runBlocking {
        val accountId = VendorManagementService(database).create("Owner Suppliers Detail Dairy")
        VendorProfileService(database).save(
            accountId,
            VendorProfileInput(
                phone = "0301",
                location = "Farm Gate",
                notes = "Morning pickups",
            ),
        )
        val detail = facade.loadDetail(ownerSession, accountId)
        assertEquals("Owner Suppliers Detail Dairy", detail.row.name)
        assertEquals("Active", detail.row.statusLabel)
        assertEquals("0301", detail.row.phone)
        assertEquals("Farm Gate", detail.row.location)
        assertEquals("Morning pickups", detail.notes)
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.loadSuppliers(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    companion object {
        private const val OWNER_PASSPHRASE = "owner suppliers passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "suppliers cashier passphrase 2026!!"
    }
}
