package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.UserSecurityService
import java.time.LocalDate

data class NativeOwnerWorkContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val shopName: String,
    val activeDeliveryCount: Int,
)

/**
 * Owner Work-home boundary: command launcher context only.
 */
class NativeOwnerWorkFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
) {
    suspend fun load(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeOwnerWorkContext {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Work is only available to Owner."
        }
        val businessDate = date.toString()
        val settings = reads.businessSettings(sessionHandle)
        val activeDeliveryCount = database.deliveryDao().activeDeliveries().count { delivery ->
            database.deliveryDao().latestStatus(delivery.deliveryId)?.status ==
                DeliveryService.STATUS_PENDING
        }
        return NativeOwnerWorkContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            shopName = settings.store?.storeName?.takeIf { it.isNotBlank() } ?: "Saaf Milk",
            activeDeliveryCount = activeDeliveryCount,
        )
    }
}
