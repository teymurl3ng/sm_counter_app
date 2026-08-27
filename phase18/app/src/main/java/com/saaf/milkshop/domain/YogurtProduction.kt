package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventDao
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.InventoryLogDao
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductDao
import java.time.Instant

/** Inputs for spec §6. Both quantities are measured at the time of the batch. */
data class YogurtProductionInput(
    val occurredAt: String,
    val milkLitersUsed: Double,
    val yogurtKgProduced: Double,
    val notes: String? = null,
)

data class YogurtProductionResult(
    val eventId: Long,
    val milkLitersUsed: Double,
    val yogurtKgProduced: Double,
)

class YogurtProductionService(
    private val database: MilkShopDatabase,
    private val productDao: ProductDao = database.productDao(),
    private val eventDao: EventDao = database.eventDao(),
    private val inventoryLogDao: InventoryLogDao = database.inventoryLogDao(),
) {
    suspend fun record(input: YogurtProductionInput): YogurtProductionResult =
        database.withWriteTransaction {
            require(input.milkLitersUsed > 0.0) { "Milk liters used must be greater than zero" }
            require(input.yogurtKgProduced > 0.0) { "Yogurt kg produced must be greater than zero" }

            val milk = productDao.findByName("Milk")
                ?: error("Seeded Milk product is missing")
            val yogurt = productDao.findByName("Yogurt")
                ?: error("Seeded Yogurt product is missing")
            requireActiveProduct(database, milk.productId)
            requireActiveProduct(database, yogurt.productId)

            val eventId = eventDao.insert(
                EventEntity(
                    createdAt = Instant.now().toString(),
                    occurredAt = input.occurredAt,
                    eventType = EVENT_TYPE_PRODUCTION,
                    milkLitersUsed = input.milkLitersUsed,
                    yogurtKgProduced = input.yogurtKgProduced,
                    notes = input.notes,
                )
            )

            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = eventId,
                    productId = milk.productId,
                    quantity = -input.milkLitersUsed,
                )
            )

            inventoryLogDao.insert(
                InventoryLogEntity(
                    eventId = eventId,
                    productId = yogurt.productId,
                    quantity = input.yogurtKgProduced,
                )
            )

            YogurtProductionResult(
                eventId = eventId,
                milkLitersUsed = input.milkLitersUsed,
                yogurtKgProduced = input.yogurtKgProduced,
            )
        }

    companion object {
        const val EVENT_TYPE_PRODUCTION = "PRODUCTION"
    }
}
