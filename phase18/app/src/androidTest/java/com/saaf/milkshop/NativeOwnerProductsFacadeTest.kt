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
    fun updateProductThenDetailReflectsChanges() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput(
                "Owner Edit Milk",
                "Liters",
                defaultSaleRate = 100.0,
                inventoryTracking = true,
                notes = "Before",
            ),
        )
        facade.updateProduct(
            ownerSession,
            productId,
            ProductCatalogInput(
                "Owner Edit Milk Updated",
                "Kg",
                defaultSaleRate = 150.0,
                inventoryTracking = false,
                notes = "After",
            ),
        )
        val detail = facade.loadDetail(ownerSession, productId)
        assertEquals("Owner Edit Milk Updated", detail.row.name)
        assertEquals("Kg", detail.row.unit)
        assertEquals(150.0, detail.row.defaultSaleRate!!, 0.001)
        assertEquals(false, detail.row.inventoryTracking)
        assertEquals("Not stock tracked", detail.row.trackingLabel)
        assertTrue(detail.row.rateLabel!!.contains("150"))
        assertEquals("After", detail.row.notes)
        assertEquals("Active", detail.row.statusLabel)
    }

    @Test
    fun archiveProductThenAbsentFromActiveList() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Archive Milk", "Liters", defaultSaleRate = 80.0),
        )
        facade.archiveProduct(ownerSession, productId)
        val snapshot = facade.loadProducts(ownerSession)
        assertFalse(snapshot.products.any { it.productId == productId })
        val archived = facade.loadProducts(ownerSession, includeArchived = true)
        assertTrue(archived.products.any { it.productId == productId && it.archived })
        val detail = facade.loadDetail(ownerSession, productId)
        assertEquals("Archived", detail.row.statusLabel)
        assertTrue(detail.row.archived)
    }

    @Test
    fun loadDetailResolvesArchivedProduct() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Archived Detail", "Kg", defaultSaleRate = 70.0),
        )
        facade.archiveProduct(ownerSession, productId)
        val detail = facade.loadDetail(ownerSession, productId)
        assertEquals("Owner Archived Detail", detail.row.name)
        assertEquals("Archived", detail.row.statusLabel)
        assertTrue(detail.row.archived)
    }

    @Test
    fun archiveThenUnarchiveReturnsToActiveList() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Unarchive Milk", "Liters", defaultSaleRate = 85.0),
        )
        facade.archiveProduct(ownerSession, productId)
        assertFalse(facade.loadProducts(ownerSession).products.any { it.productId == productId })
        assertTrue(
            facade.loadProducts(ownerSession, includeArchived = true)
                .products.any { it.productId == productId },
        )
        facade.unarchiveProduct(ownerSession, productId)
        val active = facade.loadProducts(ownerSession)
        assertTrue(active.products.any { it.productId == productId && !it.archived })
        assertFalse(
            facade.loadProducts(ownerSession, includeArchived = true)
                .products.any { it.productId == productId },
        )
        val detail = facade.loadDetail(ownerSession, productId)
        assertEquals("Active", detail.row.statusLabel)
        assertFalse(detail.row.archived)
    }

    @Test
    fun loadProductsArchivedFilterExcludesActive() = runBlocking {
        val activeId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Active Filter", "Liters"),
        )
        val archivedId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Archived Filter", "Kg"),
        )
        facade.archiveProduct(ownerSession, archivedId)
        val archivedOnly = facade.loadProducts(ownerSession, includeArchived = true)
        assertTrue(archivedOnly.products.all { it.archived })
        assertTrue(archivedOnly.products.any { it.productId == archivedId })
        assertFalse(archivedOnly.products.any { it.productId == activeId })
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
    fun updateProductRejectsNonOwner() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Gate Milk", "Kg"),
        )
        val rejected = runCatching {
            facade.updateProduct(
                cashierSession,
                productId,
                ProductCatalogInput("Blocked Update", "Kg"),
            )
        }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun archiveProductRejectsNonOwner() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Gate Archive", "Kg"),
        )
        val rejected = runCatching {
            facade.archiveProduct(cashierSession, productId)
        }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun unarchiveProductRejectsNonOwner() = runBlocking {
        val productId = facade.createProduct(
            ownerSession,
            ProductCatalogInput("Owner Gate Unarchive", "Kg"),
        )
        facade.archiveProduct(ownerSession, productId)
        val rejected = runCatching {
            facade.unarchiveProduct(cashierSession, productId)
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
