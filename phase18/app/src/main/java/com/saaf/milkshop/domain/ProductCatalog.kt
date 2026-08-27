package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductEntity
import com.saaf.milkshop.data.ProductProfileEntity
import java.time.Instant

data class ProductCatalogItem(
    val productId: Long,
    val name: String,
    val unit: String,
    val defaultSaleRate: Double?,
    val inventoryTracking: Boolean,
    val archived: Boolean,
    val notes: String?,
)

data class ProductCatalogInput(
    val name: String,
    val unit: String,
    val defaultSaleRate: Double? = null,
    val inventoryTracking: Boolean = true,
    val notes: String? = null,
)

class ProductCatalogService(private val database: MilkShopDatabase) {
    suspend fun list(includeArchived: Boolean = false): List<ProductCatalogItem> {
        val profiles = database.productProfileDao().all().associateBy { it.productId }
        return database.productDao().all().map { product ->
            val profile = profiles[product.productId]
            ProductCatalogItem(
                productId = product.productId,
                name = product.name,
                unit = product.unit,
                defaultSaleRate = profile?.defaultSaleRate,
                inventoryTracking = profile?.inventoryTracking ?: true,
                archived = profile?.archived ?: false,
                notes = profile?.notes,
            )
        }.filter { includeArchived || !it.archived }
    }

    suspend fun create(input: ProductCatalogInput): Long = database.withWriteTransaction {
        val clean = validate(input)
        val productId = database.productDao().insert(
            ProductEntity(name = clean.name, unit = clean.unit)
        )
        database.productProfileDao().save(
            ProductProfileEntity(
                productId = productId,
                defaultSaleRate = clean.defaultSaleRate,
                inventoryTracking = clean.inventoryTracking,
                notes = clean.notes,
                createdAt = Instant.now().toString(),
            )
        )
        productId
    }

    suspend fun update(productId: Long, input: ProductCatalogInput) = database.withWriteTransaction {
        val existing = database.productDao().findById(productId)
            ?: error("Product $productId does not exist")
        val clean = validate(input)
        check(database.productDao().updateIdentity(productId, clean.name, clean.unit) == 1) {
            "Product $productId could not be updated"
        }
        val profile = database.productProfileDao().findByProductId(productId)
        database.productProfileDao().save(
            ProductProfileEntity(
                productId = productId,
                defaultSaleRate = clean.defaultSaleRate,
                inventoryTracking = clean.inventoryTracking,
                archived = profile?.archived ?: false,
                notes = clean.notes,
                createdAt = profile?.createdAt ?: Instant.now().toString(),
            )
        )
        existing
    }

    suspend fun setArchived(productId: Long, archived: Boolean) = database.withWriteTransaction {
        database.productDao().findById(productId) ?: error("Product $productId does not exist")
        val profile = database.productProfileDao().findByProductId(productId)
            ?: ProductProfileEntity(productId = productId, createdAt = Instant.now().toString())
        database.productProfileDao().save(profile.copy(archived = archived))
    }

    private fun validate(input: ProductCatalogInput): ProductCatalogInput {
        val name = input.name.trim()
        require(name.isNotEmpty()) { "Enter a product name" }
        require(!name.contains('\n')) { "Product name cannot contain a new line" }
        require(input.unit in SUPPORTED_UNITS) { "Product unit must be Liters or Kg" }
        require(input.defaultSaleRate == null || input.defaultSaleRate > 0.0) {
            "Default sale rate must be greater than zero"
        }
        return input.copy(name = name, notes = input.notes.cleanOptional())
    }

    private companion object {
        val SUPPORTED_UNITS = setOf("Liters", "Kg")
    }
}

internal fun String?.cleanOptional(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

internal suspend fun requireActiveProduct(database: MilkShopDatabase, productId: Long): ProductEntity =
    (database.productDao().findById(productId)
        ?: error("Product $productId does not exist")).also {
        require(database.productProfileDao().findByProductId(productId)?.archived != true) {
            "Product $productId is archived"
        }
    }
