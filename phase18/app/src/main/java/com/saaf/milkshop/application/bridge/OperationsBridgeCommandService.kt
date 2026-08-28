package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleResult
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseResult
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionResult
import com.saaf.milkshop.domain.YogurtProductionService
import java.time.Instant

internal class OperationsBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun recordPurchase(sessionHandle: String, input: VendorPurchaseInput): VendorPurchaseResult =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.PURCHASE)
            val vendor = database.accountDao().findById(input.vendorAccountId)
            val profile = database.vendorProfileDao().findByAccountId(input.vendorAccountId)
            if (vendor?.category != "Vendor" || profile?.active != true) {
                throw BridgeCommandNotFound("Choose an active supplier")
            }
            if (database.dailyCloseDao().isPostingClosed(input.occurredAt)) {
                throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording the purchase.")
            }
            val result = VendorPurchaseService(database).record(input)
            database.eventActorLinkDao().insert(
                EventActorLinkEntity(
                    eventId = result.eventId,
                    actorUserId = actor.userId,
                    actorNameSnapshot = actor.displayName,
                    actorRoleSnapshot = actor.role,
                    attributedAt = Instant.now().toString(),
                )
            )
            database.securityAuditDao().insert(
                SecurityAuditEntryEntity(
                    occurredAt = Instant.now().toString(),
                    action = ACTION_RECORD_PURCHASE,
                    result = RESULT_SUCCESS,
                    actorUserId = actor.userId,
                    actorNameSnapshot = actor.displayName,
                    actorRoleSnapshot = actor.role,
                    details = "event_id=${result.eventId};vendor_account_id=${input.vendorAccountId}",
                )
            )
            result
        }

    suspend fun recordSale(sessionHandle: String, input: SaleInput): SaleResult =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.SALE_AND_COLLECTION)
            val product = database.productDao().findById(input.productId)
            val productProfile = database.productProfileDao().findByProductId(input.productId)
            if (product == null || productProfile?.archived == true) {
                throw BridgeCommandNotFound("Choose an active product")
            }
            val destinationId = when (val payment = input.payment) {
                is SalePayment.Account -> {
                    val account = database.accountDao().findById(payment.accountId)
                    if (account?.category != "Cash/Bank") throw BridgeCommandNotFound("Choose a valid payment account")
                    payment.accountId
                }
                is SalePayment.OnCredit -> {
                    val account = database.accountDao().findById(payment.customerAccountId)
                    val profile = database.customerProfileDao().findByAccountId(payment.customerAccountId)
                    if (account?.category != "Customer" || profile?.active == false) {
                        throw BridgeCommandNotFound("Choose an active customer")
                    }
                    payment.customerAccountId
                }
            }
            if (database.dailyCloseDao().isPostingClosed(input.occurredAt)) {
                throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording the sale.")
            }
            val result = try {
                SaleService(database).record(input)
            } catch (error: Throwable) {
                if (error.hasMessage("Insufficient stock for this product")) {
                    throw BridgeCommandInsufficientStock("There is not enough ${product.name} in stock for this sale.")
                }
                throw error
            }
            attribute(result.eventId, actor.userId, actor.displayName, actor.role)
            audit(
                ACTION_RECORD_SALE,
                "event_id=${result.eventId};product_id=${input.productId};destination_account_id=$destinationId",
                actor.userId,
                actor.displayName,
                actor.role,
            )
            result
        }

    suspend fun recordProduction(
        sessionHandle: String,
        input: YogurtProductionInput,
    ): YogurtProductionResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.PRODUCTION_AND_SPOILAGE)
        if (database.dailyCloseDao().isPostingClosed(input.occurredAt)) {
            throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording production.")
        }
        val result = try {
            YogurtProductionService(database).record(input)
        } catch (error: Throwable) {
            if (error.hasMessage("Insufficient stock for this product")) {
                throw BridgeCommandInsufficientStock("There is not enough Milk in stock for this production batch.")
            }
            throw error
        }
        attribute(result.eventId, actor.userId, actor.displayName, actor.role)
        audit(
            ACTION_RECORD_PRODUCTION,
            "event_id=${result.eventId}",
            actor.userId,
            actor.displayName,
            actor.role,
        )
        result
    }

    private suspend fun attribute(eventId: Long, actorUserId: Long, actorName: String, actorRole: String) {
        database.eventActorLinkDao().insert(
            EventActorLinkEntity(
                eventId = eventId,
                actorUserId = actorUserId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                attributedAt = Instant.now().toString(),
            )
        )
    }

    private suspend fun audit(
        action: String,
        details: String,
        actorUserId: Long,
        actorName: String,
        actorRole: String,
    ) {
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
        const val ACTION_RECORD_PURCHASE = "bridge_record_purchase"
        const val ACTION_RECORD_SALE = "bridge_record_sale"
        const val ACTION_RECORD_PRODUCTION = "bridge_record_production"
        const val RESULT_SUCCESS = "success"
    }
}

private fun Throwable.hasMessage(expected: String): Boolean =
    generateSequence(this) { it.cause }.any { it.message?.contains(expected, ignoreCase = true) == true }
