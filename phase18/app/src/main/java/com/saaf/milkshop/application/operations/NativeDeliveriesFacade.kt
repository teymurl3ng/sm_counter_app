package com.saaf.milkshop.application.operations

import com.saaf.milkshop.application.bridge.DeliveryBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale

/**
 * Typed native boundary for Operations Active deliveries.
 *
 * Physical logistics only — no sale amounts, balances, or surcharge money.
 */
class NativeDeliveriesFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val commands = DeliveryBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeDeliveriesContext {
        security.requirePermission(sessionHandle, AppPermission.DELIVERY)
        val businessDate = date.toString()
        val pending = pendingDeliveries(businessDate)
        return NativeDeliveriesContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            today = pending.filter { it.businessDate == businessDate },
            earlier = pending.filter { it.businessDate != businessDate },
        )
    }

    suspend fun markDelivered(
        sessionHandle: String,
        deliveryId: Long,
        date: LocalDate = LocalDate.now(),
    ): NativeDeliveriesContext {
        val before = load(sessionHandle, date)
        if (!before.postingOpen) {
            throw IllegalStateException(
                "This business day is closed. Ask Owner to reopen it before marking delivered.",
            )
        }
        val target = (before.today + before.earlier).singleOrNull { it.deliveryId == deliveryId }
            ?: throw IllegalArgumentException("That delivery is no longer active.")
        if (target.businessDate != before.businessDate &&
            database.dailyCloseDao().isPostingClosed(target.businessDate)
        ) {
            throw IllegalStateException(
                "The delivery date is closed. Ask Owner to reopen it before marking delivered.",
            )
        }
        commands.transition(
            sessionHandle = sessionHandle,
            deliveryId = deliveryId,
            status = DeliveryService.STATUS_DELIVERED,
            notes = null,
        )
        return load(sessionHandle, date)
    }

    private suspend fun pendingDeliveries(today: String): List<NativeDeliveryItem> {
        val items = mutableListOf<NativeDeliveryItem>()
        for (delivery in database.deliveryDao().activeDeliveries()) {
            val status = database.deliveryDao().latestStatus(delivery.deliveryId)?.status
            if (status != DeliveryService.STATUS_PENDING) continue
            val customer = database.accountDao().findById(delivery.customerAccountId)
            val product = database.productDao().findById(delivery.productId)
            val customerName = customer?.name
                ?.removePrefix("Customer: ")
                ?.trim()
                .orEmpty()
                .ifBlank { "Customer #${delivery.customerAccountId}" }
            val productName = product?.name ?: "Product #${delivery.productId}"
            val unit = product?.unit?.trim().orEmpty().ifBlank { "units" }
            items += NativeDeliveryItem(
                deliveryId = delivery.deliveryId,
                customerName = customerName,
                productName = productName,
                quantityLabel = "${formatQty(delivery.quantity)} $unit",
                route = delivery.route?.trim()?.takeIf { it.isNotEmpty() },
                businessDate = delivery.businessDate,
                isToday = delivery.businessDate == today,
            )
        }
        return items.sortedWith(
            compareByDescending<NativeDeliveryItem> { it.isToday }
                .thenByDescending { it.businessDate }
                .thenBy { it.customerName.lowercase(Locale.US) },
        )
    }

    private fun formatQty(value: Double): String =
        BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
}

data class NativeDeliveryItem(
    val deliveryId: Long,
    val customerName: String,
    val productName: String,
    val quantityLabel: String,
    val route: String?,
    val businessDate: String,
    val isToday: Boolean,
)

data class NativeDeliveriesContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val today: List<NativeDeliveryItem>,
    val earlier: List<NativeDeliveryItem>,
) {
    val isEmpty: Boolean get() = today.isEmpty() && earlier.isEmpty()
}
