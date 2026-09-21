package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogItem
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.UserSecurityService
import java.util.Locale

data class NativeOwnerProductRow(
    val productId: Long,
    val name: String,
    val unit: String,
    val rateLabel: String?,
    val statusLabel: String,
    val trackingLabel: String,
    val notes: String?,
)

data class NativeOwnerProductDetail(
    val row: NativeOwnerProductRow,
)

data class NativeOwnerProductsSnapshot(
    val products: List<NativeOwnerProductRow>,
)

/**
 * Owner Products boundary: active catalog list + read detail + create from
 * [ProductCatalogService]. Does not invent costing/stock formulas or call bridge JSON.
 */
class NativeOwnerProductsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val catalog: ProductCatalogService = ProductCatalogService(database),
) {
    suspend fun loadProducts(sessionHandle: String): NativeOwnerProductsSnapshot {
        requireOwnerMasterData(sessionHandle)
        val products = catalog.list(includeArchived = false).map { it.toRow() }
        return NativeOwnerProductsSnapshot(products = products)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        productId: Long,
    ): NativeOwnerProductDetail {
        requireOwnerMasterData(sessionHandle)
        val item = catalog.list(includeArchived = false)
            .firstOrNull { it.productId == productId }
            ?: error("This product is not in the active catalog.")
        return NativeOwnerProductDetail(row = item.toRow())
    }

    suspend fun createProduct(
        sessionHandle: String,
        input: ProductCatalogInput,
    ): Long {
        requireOwnerMasterData(sessionHandle)
        return catalog.create(input)
    }

    private suspend fun requireOwnerMasterData(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Products is only available to Owner."
        }
    }

    private fun ProductCatalogItem.toRow(): NativeOwnerProductRow = NativeOwnerProductRow(
        productId = productId,
        name = name,
        unit = unit,
        rateLabel = defaultSaleRate?.let { rate ->
            String.format(Locale.US, "PKR %,.2f / %s", rate, unit)
        },
        statusLabel = if (archived) "Archived" else "Active",
        trackingLabel = if (inventoryTracking) "Stock tracked" else "Not stock tracked",
        notes = notes,
    )
}
