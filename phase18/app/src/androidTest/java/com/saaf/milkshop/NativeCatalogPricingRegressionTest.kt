package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NativeCatalogPricingRegressionTest {
    @Test fun catalogProductsRemainVisibleAndPriceFollowsIdAfterRename() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("catalog.owner", "Catalog owner", "catalog test owner secret".toCharArray()).session.sessionId
            val date = LocalDate.of(2026, 10, 10)
            val vendor = VendorManagementService(db).create("Catalog farm")
            VendorPurchaseService(db).record(VendorPurchaseInput(vendor, date.toString(), 20.0, 4.0, 28.0))
            val facade = NativeSaleFacade(db, security)
            val original = facade.load(session, date)
            val milk = original.products.single { it.name == "Milk" }
            val catalog = ProductCatalogService(db)
            val cheese = catalog.create(ProductCatalogInput("Cheese", "Kg", defaultSaleRate = 425.0))
            val unpriced = catalog.create(ProductCatalogInput("Cream", "Kg"))
            catalog.update(milk.productId, ProductCatalogInput("Milkshake", "Liters", defaultSaleRate = 73.0))
            val loaded = facade.load(session, date)
            assertEquals(42_500L, loaded.products.single { it.productId == cheese }.ratePaisaPerUnit)
            assertEquals(0L, loaded.products.single { it.productId == unpriced }.ratePaisaPerUnit)
            assertEquals(7_300L, loaded.products.single { it.productId == milk.productId }.ratePaisaPerUnit)
            val cash = loaded.paymentAccounts.single { it.name == "Cash" }.accountId
            val posted = facade.record(session, NativeSaleRequest(milk.productId,
                NativeSaleEntryMode.QUANTITY, 2.0, NativeSalePaymentKind.PAID_NOW, cash, null), date)
            assertEquals(14_600L, posted.receipt.totalPaisa)
            val before = db.eventDao().count()
            val rejected = runCatching { facade.record(session, NativeSaleRequest(unpriced,
                NativeSaleEntryMode.QUANTITY, 1.0, NativeSalePaymentKind.PAID_NOW, cash, null), date) }
            assertTrue(rejected.isFailure)
            assertEquals(before, db.eventDao().count())
            catalog.setArchived(cheese, true)
            assertFalse(facade.load(session, date).products.any { it.productId == cheese })
        } finally { db.close() }
    }
}
