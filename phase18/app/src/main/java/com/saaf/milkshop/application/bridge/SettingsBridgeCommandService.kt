package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BusinessSettingsService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import java.time.Instant

internal class SettingsBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun saveStoreIdentity(sessionHandle: String, input: StoreIdentityInput) = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        StoreIdentityService(database).save(input)
        audit(ACTION_SAVE_STORE_IDENTITY, "store_profile=1", actor.userId, actor.displayName, actor.role)
    }

    suspend fun updateBusinessSettings(sessionHandle: String, input: BusinessSettings) = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        BusinessSettingsService(database).update(input)
        audit(ACTION_UPDATE_BUSINESS_SETTINGS, "settings=6", actor.userId, actor.displayName, actor.role)
    }

    suspend fun createExpenseCategory(sessionHandle: String, name: String): Long = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        val clean = name.trim().removePrefix("Expense: ").trim()
        val duplicate = database.expenseCategoryProfileDao().all().any { it.displayName.equals(clean, ignoreCase = true) }
        if (duplicate) throw BridgeCommandConflict("An expense category with this name already exists")
        val accountId = ExpenseCategoryManagementService(database).create(clean)
        audit(ACTION_CREATE_EXPENSE_CATEGORY, "account_id=$accountId", actor.userId, actor.displayName, actor.role)
        accountId
    }

    suspend fun setExpenseCategoryArchived(sessionHandle: String, accountId: Long, archived: Boolean) =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
            val account = database.accountDao().findById(accountId)
            val profile = database.expenseCategoryProfileDao().findByAccountId(accountId)
            if (account?.category != "Operating Expense" || profile == null) {
                throw BridgeCommandNotFound("This expense category no longer exists")
            }
            ExpenseCategoryManagementService(database).setArchived(accountId, archived)
            audit(
                ACTION_SET_EXPENSE_CATEGORY_ARCHIVED,
                "account_id=$accountId;archived=$archived",
                actor.userId,
                actor.displayName,
                actor.role,
            )
        }

    private suspend fun audit(action: String, details: String, actorUserId: Long, actorName: String, actorRole: String) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.now().toString(), action = action, result = RESULT_SUCCESS,
                actorUserId = actorUserId, actorNameSnapshot = actorName, actorRoleSnapshot = actorRole, details = details,
            )
        )
    }

    companion object {
        const val ACTION_SAVE_STORE_IDENTITY = "bridge_save_store_identity"
        const val ACTION_UPDATE_BUSINESS_SETTINGS = "bridge_update_business_settings"
        const val ACTION_CREATE_EXPENSE_CATEGORY = "bridge_create_expense_category"
        const val ACTION_SET_EXPENSE_CATEGORY_ARCHIVED = "bridge_set_expense_category_archived"
        const val RESULT_SUCCESS = "success"
    }
}
