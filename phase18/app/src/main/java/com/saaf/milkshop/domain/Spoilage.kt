package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SpoilageDetailEntity
import com.saaf.milkshop.data.SpoilageHistoryRow
import java.time.Instant
import java.time.LocalDate

data class SpoilageInput(
    val productId: Long,
    val quantity: Double,
    val occurredAt: String,
    val reason: String,
    val notes: String? = null,
)

data class SpoilageResult(val eventId: Long, val quantity: Double)

class SpoilageService(private val database: MilkShopDatabase) {
    suspend fun history(includeSuperseded: Boolean = false): List<SpoilageHistoryRow> =
        database.spoilageDao().history().filter { includeSuperseded || !it.superseded }

    suspend fun record(input: SpoilageInput): SpoilageResult = database.withWriteTransaction {
        require(input.quantity > 0.0) { "Spoilage quantity must be greater than zero" }
        LocalDate.parse(input.occurredAt)
        val reason = input.reason.trim()
        require(reason.isNotEmpty()) { "Enter a spoilage reason" }
        database.productDao().findById(input.productId)
            ?: error("Product ${input.productId} does not exist")
        val profile = database.productProfileDao().findByProductId(input.productId)
        require(profile?.inventoryTracking != false) { "Product is not inventory tracked" }
        require(profile?.archived != true) { "Archived product cannot record spoilage" }

        val eventId = database.eventDao().insert(
            EventEntity(
                createdAt = Instant.now().toString(),
                occurredAt = input.occurredAt,
                eventType = EVENT_TYPE_SPOILAGE,
                notes = input.notes.cleanOptional(),
            )
        )
        database.spoilageDao().insert(
            SpoilageDetailEntity(eventId, input.productId, input.quantity, reason)
        )
        database.inventoryLogDao().insert(
            InventoryLogEntity(eventId = eventId, productId = input.productId, quantity = -input.quantity)
        )
        SpoilageResult(eventId, input.quantity)
    }

    companion object {
        const val EVENT_TYPE_SPOILAGE = "SPOILAGE"
    }
}
