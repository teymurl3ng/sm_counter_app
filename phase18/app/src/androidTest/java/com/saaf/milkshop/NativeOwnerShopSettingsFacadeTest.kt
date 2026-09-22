package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.StoreIdentityInput
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
class NativeOwnerShopSettingsFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerShopSettingsFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerShopSettingsFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.shop.settings",
            "Shop Settings Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "shop.settings.cashier",
                displayName = "Shop Settings Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("shop.settings.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadReturnsShopIdentityAndRates() = runBlocking {
        val snapshot = facade.load(ownerSession)
        assertEquals("Saaf Milk", snapshot.shopName)
        assertEquals("PKR", snapshot.currencyCode)
        assertTrue(snapshot.vendorMilkRateLabel.contains("PKR"))
        assertTrue(snapshot.retailMilkRateLabel.contains("PKR"))
        assertTrue(snapshot.milkRateVendor > 0.0)
        assertTrue(snapshot.tsStandard > 0.0)
    }

    @Test
    fun saveIdentityUpdatesSnapshot() = runBlocking {
        facade.saveIdentity(
            ownerSession,
            StoreIdentityInput(
                storeName = "Owner Edit Shop",
                phone = "03001234567",
                address = "Edit Street",
            ),
        )
        val snapshot = facade.load(ownerSession)
        assertEquals("Owner Edit Shop", snapshot.shopName)
        assertEquals("03001234567", snapshot.phone)
        assertEquals("Edit Street", snapshot.address)
    }

    @Test
    fun updateRatesUpdatesSnapshot() = runBlocking {
        val before = facade.load(ownerSession)
        facade.updateRates(
            ownerSession,
            BusinessSettings(
                milkRateVendor = before.milkRateVendor + 1.0,
                deliveryFeeVendor = before.deliveryFeeVendor,
                tsStandard = before.tsStandard,
                retailRateMilk = before.retailRateMilk,
                retailRateYogurt = before.retailRateYogurt,
                defaultDeliverySurcharge = before.defaultDeliverySurcharge,
            ),
        )
        val after = facade.load(ownerSession)
        assertEquals(before.milkRateVendor + 1.0, after.milkRateVendor, 1e-9)
    }

    @Test
    fun createExpenseCategoryThenListContains() = runBlocking {
        val accountId = facade.createExpenseCategory(ownerSession, "Generator Fuel")
        val snapshot = facade.load(ownerSession)
        assertTrue(snapshot.expenseCategories.any { it.accountId == accountId && it.name == "Generator Fuel" })
    }

    @Test
    fun createExpenseCategoryDuplicateSurfacesError() = runBlocking {
        facade.createExpenseCategory(ownerSession, "Unique Fuel")
        val rejected = runCatching { facade.createExpenseCategory(ownerSession, "Unique Fuel") }
        assertTrue(rejected.isFailure)
        assertTrue(
            rejected.exceptionOrNull()?.message?.contains("already exists", ignoreCase = true) == true,
        )
    }

    @Test
    fun setExpenseCategoryArchivedUpdatesStatus() = runBlocking {
        val accountId = facade.createExpenseCategory(ownerSession, "Archive Fuel")
        facade.setExpenseCategoryArchived(ownerSession, accountId, archived = true)
        val archived = facade.loadCategoryDetail(ownerSession, accountId)
        assertFalse(archived.active)
        assertEquals("Inactive", archived.statusLabel)
        facade.setExpenseCategoryArchived(ownerSession, accountId, archived = false)
        val restored = facade.loadCategoryDetail(ownerSession, accountId)
        assertTrue(restored.active)
        assertEquals("Active", restored.statusLabel)
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.load(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun saveIdentityRejectsNonOwner() = runBlocking {
        val rejected = runCatching {
            facade.saveIdentity(cashierSession, StoreIdentityInput(storeName = "Blocked"))
        }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun createExpenseCategoryRejectsNonOwner() = runBlocking {
        val rejected = runCatching {
            facade.createExpenseCategory(cashierSession, "Blocked Category")
        }
        assertTrue(rejected.isFailure)
    }

    companion object {
        private const val OWNER_PASSPHRASE = "owner shop settings passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "shop settings cashier passphrase 2026!!"
    }
}
