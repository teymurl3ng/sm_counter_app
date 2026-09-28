package com.saaf.milkshop.application.operations

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.saaf.milkshop.sync.BusinessClock

data class NativeOperationsWorkContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val milkOnHandLabel: String,
    val milkProductPresent: Boolean,
    val latestReceiptLabel: String?,
    val latestProductionLabel: String?,
    val activeDeliveryCount: Int,
    val attendance: NativeOperationsAttendanceStatus,
)

sealed interface NativeOperationsAttendanceStatus {
    data object Unlinked : NativeOperationsAttendanceStatus
    data object NotMarked : NativeOperationsAttendanceStatus
    data class Marked(val statusLabel: String) : NativeOperationsAttendanceStatus
}

/**
 * Operations Work-home boundary: physical context only for choosing the next job.
 * Never returns rates, payables, margins, or financial loss values.
 */
class NativeOperationsWorkFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeOperationsWorkContext {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val businessDate = date.toString()
        val milk = database.productDao().findByName("Milk")
        val milkOnHand = if (milk == null) {
            null
        } else {
            database.readModelDao().currentStock()
                .firstOrNull { it.productId == milk.productId }
                ?.quantityOnHand
        }
        val todayEvents = database.eventDao().findAllDescending()
            .filter { it.occurredAt == businessDate }
        val supersededIds = todayEvents.mapNotNull { it.correctsEventId }.toSet()
        val effectiveToday = todayEvents.filter { it.eventId !in supersededIds }
        val latestReceipt = effectiveToday.firstOrNull {
            it.eventType == VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT
        }
        val latestProduction = effectiveToday.firstOrNull {
            it.eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION
        }
        val activeDeliveryCount = database.deliveryDao().activeDeliveries().count { delivery ->
            val status = database.deliveryDao().latestStatus(delivery.deliveryId)?.status
            status == DeliveryService.STATUS_PENDING
        }
        val attendance = when (val employeeId = actor.employeeAccountId) {
            null -> NativeOperationsAttendanceStatus.Unlinked
            else -> {
                val record = database.attendanceDao().current(employeeId, businessDate)
                if (record == null) {
                    NativeOperationsAttendanceStatus.NotMarked
                } else {
                    NativeOperationsAttendanceStatus.Marked(statusLabel = formatAttendanceStatus(record.status))
                }
            }
        }
        return NativeOperationsWorkContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            milkOnHandLabel = milkOnHand?.let { formatQuantity(it) + " L milk" } ?: "Milk stock unavailable",
            milkProductPresent = milk != null,
            latestReceiptLabel = latestReceipt?.createdAt?.let(::formatClock),
            latestProductionLabel = latestProduction?.createdAt?.let(::formatClock),
            activeDeliveryCount = activeDeliveryCount,
            attendance = attendance,
        )
    }

    private fun formatQuantity(value: Double): String =
        BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun formatClock(iso: String): String =
        com.saaf.milkshop.application.AppTimeFormats.clock(iso)

    private fun formatAttendanceStatus(raw: String): String =
        raw.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.US) }
}
