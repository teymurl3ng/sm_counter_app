package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.UserSecurityService
import java.time.Instant

class BridgeCommandConflict(message: String) : IllegalStateException(message)
class BridgeCommandNotFound(message: String) : IllegalStateException(message)

/**
 * Authenticated application-command boundary for the Phase 7 proof slice.
 * Domain services remain authoritative; this layer adds native permission and
 * durable actor attribution around the existing transaction.
 */
class HybridCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun createProduct(sessionHandle: String, input: ProductCatalogInput): Long =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            if (database.productDao().findByName(input.name.trim()) != null) {
                throw BridgeCommandConflict("A product with this name already exists")
            }
            val productId = ProductCatalogService(database).create(input)
            database.securityAuditDao().insert(
                SecurityAuditEntryEntity(
                    occurredAt = Instant.now().toString(),
                    action = ACTION_CREATE_PRODUCT,
                    result = RESULT_SUCCESS,
                    actorUserId = actor.userId,
                    actorNameSnapshot = actor.displayName,
                    actorRoleSnapshot = actor.role,
                    details = "product_id=$productId",
                )
            )
            productId
        }

    suspend fun updateProduct(sessionHandle: String, productId: Long, input: ProductCatalogInput) =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            database.productDao().findById(productId)
                ?: throw BridgeCommandNotFound("This product no longer exists")
            val duplicate = database.productDao().findByName(input.name.trim())
            if (duplicate != null && duplicate.productId != productId) {
                throw BridgeCommandConflict("A product with this name already exists")
            }
            ProductCatalogService(database).update(productId, input)
            recordProductAudit(
                action = ACTION_UPDATE_PRODUCT,
                productId = productId,
                actorUserId = actor.userId,
                actorName = actor.displayName,
                actorRole = actor.role,
            )
        }

    suspend fun setProductArchived(sessionHandle: String, productId: Long, archived: Boolean) =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            database.productDao().findById(productId)
                ?: throw BridgeCommandNotFound("This product no longer exists")
            ProductCatalogService(database).setArchived(productId, archived)
            recordProductAudit(
                action = if (archived) ACTION_ARCHIVE_PRODUCT else ACTION_RESTORE_PRODUCT,
                productId = productId,
                actorUserId = actor.userId,
                actorName = actor.displayName,
                actorRole = actor.role,
            )
        }

    private suspend fun recordProductAudit(
        action: String,
        productId: Long,
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
                details = "product_id=$productId",
            )
        )
    }

    companion object {
        const val ACTION_CREATE_PRODUCT = "bridge_create_product"
        const val ACTION_UPDATE_PRODUCT = "bridge_update_product"
        const val ACTION_ARCHIVE_PRODUCT = "bridge_archive_product"
        const val ACTION_RESTORE_PRODUCT = "bridge_restore_product"
        const val RESULT_SUCCESS = "success"
    }
}
