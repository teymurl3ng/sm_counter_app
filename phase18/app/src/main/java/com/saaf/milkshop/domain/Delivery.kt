package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.DeliveryEntity
import com.saaf.milkshop.data.DeliveryStatusEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant
import java.time.LocalDate

data class DeliveryInput(
    val customerAccountId: Long,
    val productId: Long,
    val quantity: Double,
    val route: String? = null,
    val businessDate: String,
    val saleEventId: Long? = null,
    val notes: String? = null,
)

data class DeliveryRecord(
    val delivery: DeliveryEntity,
    val status: String,
    val statusHistory: List<DeliveryStatusEntity>,
)

class DeliveryService(private val database: MilkShopDatabase) {
    suspend fun create(input: DeliveryInput): Long = database.withWriteTransaction {
        requireActiveProfile(database, input.customerAccountId, "Customer")
        require(input.quantity > 0.0) { "Delivery quantity must be greater than zero" }
        LocalDate.parse(input.businessDate)
        requireActiveProduct(database, input.productId)
        input.saleEventId?.let { validateSaleLink(it, input.productId, input.customerAccountId) }

        val now = Instant.now().toString()
        val deliveryId = database.deliveryDao().insert(
            DeliveryEntity(
                customerAccountId = input.customerAccountId,
                productId = input.productId,
                quantity = input.quantity,
                route = input.route.cleanOptional(),
                businessDate = input.businessDate,
                saleEventId = input.saleEventId,
                notes = input.notes.cleanOptional(),
                createdAt = now,
            )
        )
        database.deliveryDao().insertStatus(
            DeliveryStatusEntity(
                deliveryId = deliveryId,
                status = STATUS_PENDING,
                occurredAt = now,
            )
        )
        deliveryId
    }

    suspend fun linkSale(deliveryId: Long, saleEventId: Long) = database.withWriteTransaction {
        val delivery = database.deliveryDao().findById(deliveryId)
            ?: error("Delivery $deliveryId does not exist")
        check(delivery.saleEventId == null) { "Delivery $deliveryId is already linked to a sale" }
        validateSaleLink(saleEventId, delivery.productId, delivery.customerAccountId)
        check(database.deliveryDao().linkSale(deliveryId, saleEventId) == 1) {
            "Delivery $deliveryId could not be linked to the sale"
        }
    }

    suspend fun transition(
        deliveryId: Long,
        status: String,
        occurredAt: String = Instant.now().toString(),
        notes: String? = null,
    ): Long = database.withWriteTransaction {
        require(status in TERMINAL_STATUSES) { "Delivery status must be delivered or cancelled" }
        val delivery = database.deliveryDao().findById(deliveryId)
            ?: error("Delivery $deliveryId does not exist")
        require(!delivery.archived) { "Archived delivery cannot change status" }
        val current = database.deliveryDao().latestStatus(deliveryId)
            ?: error("Delivery $deliveryId has no status history")
        require(current.status == STATUS_PENDING) { "Only a pending delivery can change status" }
        database.deliveryDao().insertStatus(
            DeliveryStatusEntity(
                deliveryId = deliveryId,
                status = status,
                occurredAt = occurredAt,
                notes = notes.cleanOptional(),
            )
        )
    }

    suspend fun archive(deliveryId: Long) = database.withWriteTransaction {
        database.deliveryDao().findById(deliveryId) ?: error("Delivery $deliveryId does not exist")
        check(database.deliveryDao().archive(deliveryId) == 1) { "Delivery $deliveryId is already archived" }
    }

    suspend fun get(deliveryId: Long): DeliveryRecord {
        val delivery = database.deliveryDao().findById(deliveryId)
            ?: error("Delivery $deliveryId does not exist")
        val statuses = database.deliveryDao().statusHistory(deliveryId)
        return DeliveryRecord(delivery, statuses.lastOrNull()?.status ?: error("Delivery has no status"), statuses)
    }

    private suspend fun validateSaleLink(saleEventId: Long, productId: Long, customerAccountId: Long) {
        val event = database.eventDao().findById(saleEventId)
            ?: error("Sale event $saleEventId does not exist")
        require(event.eventType == SaleService.EVENT_TYPE_SALE) { "Delivery can link only to a SALE event" }
        val inventory = database.inventoryLogDao().findByEventId(saleEventId)
        require(inventory.any { it.productId == productId }) { "Delivery product does not match the linked sale" }
        val customerMovements = database.financialLogDao().findByEventId(saleEventId).filter { movement ->
            database.accountDao().findById(movement.accountId)?.category == "Customer"
        }
        require(customerMovements.isEmpty() || customerMovements.any { it.accountId == customerAccountId }) {
            "Delivery customer does not match the linked credit sale"
        }
    }

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_CANCELLED = "cancelled"
        val TERMINAL_STATUSES = setOf(STATUS_DELIVERED, STATUS_CANCELLED)
    }
}
