package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerProductsFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
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
class NativeOwnerProductsFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerProductsFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerProductsFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.products",
            "Products Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "products.cashier",
                displayName = "Products Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("products.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadProductsListsActiveCatalogOnly() = runBlocking {
        val catalog = ProductCatalogService(database)
        val creamId = catalog.create(
            ProductCatalogInput("Phase Cream", "Kg", defaultSaleRate = 500.0, notes = "Fresh"),
        )
        catalog.create(ProductCatalogInput("Archived Cream", "Kg", defaultSaleRate = 400.0))
        val archived = catalog.list(includeArchived = true).first { it.name == "Archived Cream" }
        catalog.setArchived(archived.productId, true)

        val snapshot = facade.loadProducts(ownerSession)
        assertTrue(snapshot.products.any { it.productId == creamId && it.name == "Phase Cream" })
        assertFalse(snapshot.products.any { it.name == "Archived Cream" })
        val cream = snapshot.products.first { it.productId == creamId }
        assertEquals("Kg", cream.unit)
        assertEquals("Active", cream.statusLabel)
        assertEquals("Stock tracked", cream.trackingLabel)
        assertTrue(cream.rateLabel!!.contains("500"))
        assertEquals("Fresh", cream.notes)
    }

    @Test
    fun loadDetailReturnsActiveProductFacts() = runBlocking {
        val catalog = ProductCatalogService(database)
        val productId = catalog.create(
            ProductCatalogInput(
                "Owner Products Detail Milk",
                "Liters",
                defaultSaleRate = 120.0,
                inventoryTracking = true,
            ),
        )
        val detail = facade.loadDetail(ownerSession, productId)
        assertEquals("Owner Products Detail Milk", detail.row.name)
        assertEquals("Liters", detail.row.unit)
        assertEquals("Active", detail.row.statusLabel)
    }

    @Test
    fun createProductThenListContains() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput(
                "Owner Create Milk",
                "Liters",
                defaultSaleRate = 95.0,
                inventoryTracking = true,
                notes = "Create slice",
            ),
        )
        val snapshot = facade.loadProducts(ownerSession)
        assertTrue(
            snapshot.products.any {
                it.productId == productId &&
                    it.name == "Owner Create Milk" &&
                    it.unit == "Liters"
            },
        )
        val created = snapshot.products.first { it.productId == productId }
        assertEquals("Active", created.statusLabel)
        assertEquals("Stock tracked", created.trackingLabel)
        assertTrue(created.rateLabel!!.contains("95"))
        assertEquals("Create slice", created.notes)
    }

    @Test
    fun createProductRejectsNonOwner() = runBlocking {
        val rejected = runCatching {
            facade.createProduct(
                cashierSession,
                ProductCatalogInput("Blocked Product", "Kg"),
            )
        }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.loadProducts(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    companion object {
        private const val OWNER_PASSPHRASE = "owner products passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "products cashier passphrase 2026!!"
    }
}
