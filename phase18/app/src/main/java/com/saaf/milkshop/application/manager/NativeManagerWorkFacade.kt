package com.saaf.milkshop.application.manager

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.UserSecurityService
import java.time.LocalDate

data class NativeManagerWorkContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val shopName: String,
    val activeDeliveryCount: Int,
    /** True when posting is open but no physical cash count exists yet for today. */
    val cashCountMissing: Boolean,
)

/**
 * Manager Work-home boundary: daily command launcher context (not Owner-only).
 * Does not call Owner-gated business-settings reads — shop name comes from store profile.
 */
class NativeManagerWorkFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun load(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeManagerWorkContext {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(actor.role)
        require(role == AppRole.MANAGER) {
            "Manager Work is only available to Manager."
        }
        val businessDate = date.toString()
        val postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate)
        val cash = database.accountDao().findByCategory("Cash/Bank")
            .singleOrNull { it.name == "Cash" }
        val latestCount = cash?.let { account ->
            database.physicalCashCountDao().latest(account.accountId, businessDate)
        }
        val activeDeliveryCount = database.deliveryDao().activeDeliveries().count { delivery ->
            database.deliveryDao().latestStatus(delivery.deliveryId)?.status ==
                DeliveryService.STATUS_PENDING
        }
        return NativeManagerWorkContext(
            businessDate = businessDate,
            postingOpen = postingOpen,
            shopName = database.storeProfileDao().get()?.storeName?.takeIf { it.isNotBlank() }
                ?: "Saaf Milk",
            activeDeliveryCount = activeDeliveryCount,
            cashCountMissing = postingOpen && latestCount == null,
        )
    }
}
