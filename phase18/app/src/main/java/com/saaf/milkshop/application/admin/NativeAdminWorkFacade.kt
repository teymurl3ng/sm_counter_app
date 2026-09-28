package com.saaf.milkshop.application.admin

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.UserSecurityService
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

data class NativeAdminWorkContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val shopName: String,
    /** Accounts waiting for private credential enrollment. */
    val setupRequiredCount: Int,
    /** First setup-required user id, for direct deep links (null when none). */
    val firstSetupRequiredUserId: Long?,
    /** Active products missing a default sale rate (blocks priced sales). */
    val productsMissingRateCount: Int,
    /** True when the catalog has no active products. */
    val catalogEmpty: Boolean,
    /** Staff accounts (non-Owner/Admin) without an employee salary link. */
    val unlinkedStaffCount: Int,
)

/**
 * Admin Work-home boundary: setup / administration health + secondary daily launcher context.
 * Uses permissions Admin actually holds (VIEW_OPERATIONAL, MANAGE_STAFF_USERS, MANAGE_MASTER_DATA).
 * Never calls Owner-only restore / privileged-user gates.
 */
class NativeAdminWorkFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val catalog: ProductCatalogService = ProductCatalogService(database),
) {
    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeAdminWorkContext {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(actor.role)
        require(role == AppRole.ADMIN) {
            "Admin Work is only available to Administrator."
        }

        security.requirePermission(sessionHandle, AppPermission.MANAGE_STAFF_USERS)
        val users = security.listUsers(sessionHandle)
        val setupRequiredCount = users.count { it.credentialSetupRequired }
        val firstSetupRequiredUserId = users.firstOrNull { it.credentialSetupRequired }?.userId
        val unlinkedStaffCount = users.count { user ->
            val userRole = runCatching { AppRole.parse(user.role) }.getOrNull() ?: return@count false
            userRole != AppRole.OWNER &&
                userRole != AppRole.ADMIN &&
                user.employeeAccountId == null
        }

        security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        val activeProducts = catalog.list(includeArchived = false)
        val productsMissingRateCount = activeProducts.count { it.defaultSaleRate == null }

        val businessDate = date.toString()
        return NativeAdminWorkContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            shopName = database.storeProfileDao().get()?.storeName?.takeIf { it.isNotBlank() }
                ?: "Saaf Milk",
            setupRequiredCount = setupRequiredCount,
            firstSetupRequiredUserId = firstSetupRequiredUserId,
            productsMissingRateCount = productsMissingRateCount,
            catalogEmpty = activeProducts.isEmpty(),
            unlinkedStaffCount = unlinkedStaffCount,
        )
    }
}
