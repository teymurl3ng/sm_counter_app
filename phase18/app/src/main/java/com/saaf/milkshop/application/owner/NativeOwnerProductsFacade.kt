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
    val defaultSaleRate: Double?,
    val inventoryTracking: Boolean,
    val archived: Boolean,
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
 * Owner Products boundary: active/archived catalog list + read detail + create/update/archive/unarchive
 * from [ProductCatalogService]. Does not invent costing/stock formulas or call bridge JSON.
 */
class NativeOwnerProductsFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val catalog: ProductCatalogService = ProductCatalogService(database),
) {
    suspend fun loadProducts(
        sessionHandle: String,
        includeArchived: Boolean = false,
    ): NativeOwnerProductsSnapshot {
        requireOwnerMasterData(sessionHandle)
        val products = if (includeArchived) {
            catalog.list(includeArchived = true)
                .filter { it.archived }
                .map { it.toRow() }
        } else {
            catalog.list(includeArchived = false).map { it.toRow() }
        }
        return NativeOwnerProductsSnapshot(products = products)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        productId: Long,
    ): NativeOwnerProductDetail {
        requireOwnerMasterData(sessionHandle)
        val item = catalog.list(includeArchived = true)
            .firstOrNull { it.productId == productId }
            ?: error("This product was not found in the catalog.")
        return NativeOwnerProductDetail(row = item.toRow())
    }

    suspend fun createProduct(
        sessionHandle: String,
        input: ProductCatalogInput,
    ): Long {
        requireOwnerMasterData(sessionHandle)
        return catalog.create(input)
    }

    suspend fun updateProduct(
        sessionHandle: String,
        productId: Long,
        input: ProductCatalogInput,
    ) {
        requireOwnerMasterData(sessionHandle)
        catalog.update(productId, input)
    }

    suspend fun archiveProduct(
        sessionHandle: String,
        productId: Long,
    ) {
        requireOwnerMasterData(sessionHandle)
        catalog.setArchived(productId, true)
    }

    suspend fun unarchiveProduct(
        sessionHandle: String,
        productId: Long,
    ) {
        requireOwnerMasterData(sessionHandle)
        catalog.setArchived(productId, false)
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
        defaultSaleRate = defaultSaleRate,
        inventoryTracking = inventoryTracking,
        archived = archived,
        rateLabel = defaultSaleRate?.let { rate ->
            String.format(Locale.US, "PKR %,.2f / %s", rate, unit)
        },
        statusLabel = if (archived) "Archived" else "Active",
        trackingLabel = if (inventoryTracking) "Stock tracked" else "Not stock tracked",
        notes = notes,
    )
}
