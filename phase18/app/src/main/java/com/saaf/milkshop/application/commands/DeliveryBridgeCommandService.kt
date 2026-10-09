package com.saaf.milkshop.application.commands

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.application.bridge.BridgeCommandConflict
import com.saaf.milkshop.application.bridge.BridgeCommandDayClosed
import com.saaf.milkshop.application.bridge.BridgeCommandNotFound
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.DeliveryInput
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.BusinessDateGuard
import com.saaf.milkshop.sync.SyncScheduler
import java.time.Instant

internal class DeliveryBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun create(sessionHandle: String, input: DeliveryInput): Long = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.DELIVERY)
        requireOpenDate(input.businessDate, "creating a delivery")
        requireActiveCustomer(input.customerAccountId)
        requireActiveProduct(input.productId)
        if (input.saleEventId != null) requireSale(input.saleEventId)
        val id = DeliveryService(database).create(input)
        audit(ACTION_CREATE_DELIVERY, "delivery_id=$id", actor.userId, actor.displayName, actor.role)
        id
    }.also { SyncScheduler.requestPush() }

    suspend fun transition(sessionHandle: String, deliveryId: Long, status: String, notes: String?) =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.DELIVERY)
            val delivery = requireDelivery(deliveryId)
            requireOpenDate(delivery.businessDate, "changing this delivery")
            if (delivery.archived) throw BridgeCommandConflict("This delivery is archived")
            val current = database.deliveryDao().latestStatus(deliveryId)
                ?: throw BridgeCommandConflict("This delivery has no current status")
            if (current.status != DeliveryService.STATUS_PENDING) {
                throw BridgeCommandConflict("Only a pending delivery can be changed")
            }
            DeliveryService(database).transition(deliveryId, status, notes = notes)
            audit(
                ACTION_TRANSITION_DELIVERY,
                "delivery_id=$deliveryId;status=$status",
                actor.userId,
                actor.displayName,
                actor.role,
            )
        }.also { SyncScheduler.requestPush() }

    suspend fun linkSale(sessionHandle: String, deliveryId: Long, saleEventId: Long) = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.DELIVERY)
        val delivery = requireDelivery(deliveryId)
        if (delivery.saleEventId != null) throw BridgeCommandConflict("This delivery is already linked to a sale")
        requireSale(saleEventId)
        DeliveryService(database).linkSale(deliveryId, saleEventId)
        audit(
            ACTION_LINK_DELIVERY_SALE,
            "delivery_id=$deliveryId;sale_event_id=$saleEventId",
            actor.userId,
            actor.displayName,
            actor.role,
        )
    }.also { SyncScheduler.requestPush() }

    suspend fun archive(sessionHandle: String, deliveryId: Long) = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.DELIVERY)
        val delivery = requireDelivery(deliveryId)
        requireOpenDate(delivery.businessDate, "archiving this delivery")
        if (delivery.archived) throw BridgeCommandConflict("This delivery is already archived")
        DeliveryService(database).archive(deliveryId)
        audit(ACTION_ARCHIVE_DELIVERY, "delivery_id=$deliveryId", actor.userId, actor.displayName, actor.role)
    }.also { SyncScheduler.requestPush() }

    private suspend fun requireDelivery(deliveryId: Long) = database.deliveryDao().findById(deliveryId)
        ?: throw BridgeCommandNotFound("This delivery no longer exists")

    private suspend fun requireActiveCustomer(accountId: Long) {
        val account = database.accountDao().findById(accountId)
        val profile = database.customerProfileDao().findByAccountId(accountId)
        if (account?.category != "Customer" || profile?.active == false) {
            throw BridgeCommandNotFound("Choose an active customer")
        }
    }

    private suspend fun requireActiveProduct(productId: Long) {
        val product = database.productDao().findById(productId)
        val profile = database.productProfileDao().findByProductId(productId)
        if (product == null || profile?.archived == true) throw BridgeCommandNotFound("Choose an active product")
    }

    private suspend fun requireSale(eventId: Long) {
        val event = database.eventDao().findById(eventId)
        if (event?.eventType != "SALE") throw BridgeCommandNotFound("Choose a valid sale record")
    }

    private suspend fun requireOpenDate(businessDate: String, operation: String) {
        BusinessDateGuard.requirePlannedWithinHorizon(businessDate)
        if (database.dailyCloseDao().isPostingClosed(businessDate)) {
            throw BridgeCommandDayClosed("This business day is closed. Reopen it before $operation.")
        }
    }

    private suspend fun audit(action: String, details: String, actorUserId: Long, actorName: String, actorRole: String) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.now().toString(),
                action = action,
                result = RESULT_SUCCESS,
                actorUserId = actorUserId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                details = details,
            )
        )
    }

    companion object {
        const val ACTION_CREATE_DELIVERY = "bridge_create_delivery"
        const val ACTION_TRANSITION_DELIVERY = "bridge_transition_delivery"
        const val ACTION_LINK_DELIVERY_SALE = "bridge_link_delivery_sale"
        const val ACTION_ARCHIVE_DELIVERY = "bridge_archive_delivery"
        const val RESULT_SUCCESS = "success"
    }
}
