package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.CustomerProfileService
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.VendorProfileService
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

    suspend fun createCustomer(sessionHandle: String, name: String): Long =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            requireUniqueCustomerName(name)
            val accountId = CustomerManagementService(database).create(name)
            recordMasterDataAudit(
                action = ACTION_CREATE_CUSTOMER,
                entityName = "account_id",
                entityId = accountId,
                actorUserId = actor.userId,
                actorName = actor.displayName,
                actorRole = actor.role,
            )
            accountId
        }

    suspend fun renameCustomer(sessionHandle: String, accountId: Long, name: String) =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            requireCustomer(accountId)
            requireUniqueCustomerName(name, exceptAccountId = accountId)
            CustomerManagementService(database).rename(accountId, name)
            recordMasterDataAudit(
                action = ACTION_RENAME_CUSTOMER,
                entityName = "account_id",
                entityId = accountId,
                actorUserId = actor.userId,
                actorName = actor.displayName,
                actorRole = actor.role,
            )
        }

    suspend fun saveCustomerProfile(
        sessionHandle: String,
        accountId: Long,
        input: CustomerProfileInput,
    ) = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        requireCustomer(accountId)
        CustomerProfileService(database).save(accountId, input)
        recordMasterDataAudit(
            action = ACTION_SAVE_CUSTOMER_PROFILE,
            entityName = "account_id",
            entityId = accountId,
            actorUserId = actor.userId,
            actorName = actor.displayName,
            actorRole = actor.role,
        )
    }

    suspend fun createVendor(sessionHandle: String, name: String): Long =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            requireUniqueVendorName(name)
            val accountId = VendorManagementService(database).create(name)
            recordMasterDataAudit(
                action = ACTION_CREATE_VENDOR,
                entityName = "account_id",
                entityId = accountId,
                actorUserId = actor.userId,
                actorName = actor.displayName,
                actorRole = actor.role,
            )
            accountId
        }

    suspend fun renameVendor(sessionHandle: String, accountId: Long, name: String) =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            requireVendor(accountId)
            requireUniqueVendorName(name, exceptAccountId = accountId)
            VendorManagementService(database).rename(accountId, name)
            recordMasterDataAudit(
                action = ACTION_RENAME_VENDOR,
                entityName = "account_id",
                entityId = accountId,
                actorUserId = actor.userId,
                actorName = actor.displayName,
                actorRole = actor.role,
            )
        }

    suspend fun saveVendorProfile(
        sessionHandle: String,
        accountId: Long,
        input: VendorProfileInput,
    ) = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        requireVendor(accountId)
        VendorProfileService(database).save(accountId, input)
        recordMasterDataAudit(
            action = ACTION_SAVE_VENDOR_PROFILE,
            entityName = "account_id",
            entityId = accountId,
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
        recordMasterDataAudit(
            action = action,
            entityName = "product_id",
            entityId = productId,
            actorUserId = actorUserId,
            actorName = actorName,
            actorRole = actorRole,
        )
    }

    private suspend fun recordMasterDataAudit(
        action: String,
        entityName: String,
        entityId: Long,
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
                details = "$entityName=$entityId",
            )
        )
    }

    private suspend fun requireCustomer(accountId: Long) {
        val account = database.accountDao().findById(accountId)
            ?: throw BridgeCommandNotFound("This customer no longer exists")
        if (account.category != "Customer") {
            throw BridgeCommandNotFound("This customer no longer exists")
        }
    }

    private suspend fun requireVendor(accountId: Long) {
        val account = database.accountDao().findById(accountId)
            ?: throw BridgeCommandNotFound("This supplier no longer exists")
        if (account.category != "Vendor") {
            throw BridgeCommandNotFound("This supplier no longer exists")
        }
    }

    private suspend fun requireUniqueCustomerName(name: String, exceptAccountId: Long? = null) {
        val candidate = name.trim().removePrefix("Customer: ").trim()
        val duplicate = database.accountDao().findByCategory("Customer").any { account ->
            account.accountId != exceptAccountId &&
                account.name.removePrefix("Customer: ").trim().equals(candidate, ignoreCase = true)
        }
        if (duplicate) throw BridgeCommandConflict("A customer with this name already exists")
    }

    private suspend fun requireUniqueVendorName(name: String, exceptAccountId: Long? = null) {
        val candidate = name.trim().removePrefix("Vendor: ").trim()
        val duplicate = database.accountDao().findByCategory("Vendor").any { account ->
            account.accountId != exceptAccountId &&
                account.name.removePrefix("Vendor: ").trim().equals(candidate, ignoreCase = true)
        }
        if (duplicate) throw BridgeCommandConflict("A supplier with this name already exists")
    }

    companion object {
        const val ACTION_CREATE_PRODUCT = "bridge_create_product"
        const val ACTION_UPDATE_PRODUCT = "bridge_update_product"
        const val ACTION_ARCHIVE_PRODUCT = "bridge_archive_product"
        const val ACTION_RESTORE_PRODUCT = "bridge_restore_product"
        const val ACTION_CREATE_CUSTOMER = "bridge_create_customer"
        const val ACTION_RENAME_CUSTOMER = "bridge_rename_customer"
        const val ACTION_SAVE_CUSTOMER_PROFILE = "bridge_save_customer_profile"
        const val ACTION_CREATE_VENDOR = "bridge_create_vendor"
        const val ACTION_RENAME_VENDOR = "bridge_rename_vendor"
        const val ACTION_SAVE_VENDOR_PROFILE = "bridge_save_vendor_profile"
        const val RESULT_SUCCESS = "success"
    }
}
