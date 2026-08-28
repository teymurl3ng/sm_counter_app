package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseResult
import com.saaf.milkshop.domain.VendorPurchaseService
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
            if (database.dailyCloseDao().currentAction(input.occurredAt)?.action == "close") {
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

    companion object {
        const val ACTION_RECORD_PURCHASE = "bridge_record_purchase"
        const val RESULT_SUCCESS = "success"
    }
}
