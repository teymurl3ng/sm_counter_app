package com.saaf.milkshop.application.operations

import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.SpoilageService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionService
import com.saaf.milkshop.quickunlock.QuickUnlockService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Typed native boundary for Operations More.
 *
 * Own physical work for today + personal Quick unlock.
 * Never exposes cash count, expected/variance, rates, or balances.
 */
class NativeOperationsMoreFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val quickUnlock: QuickUnlockService? = null,
) {
    suspend fun load(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeOperationsMoreContext {
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val businessDate = date.toString()
        return NativeOperationsMoreContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
        )
    }

    suspend fun loadTodayWork(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeOperationsMoreToday {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val context = load(sessionHandle, date)
        val allowed = setOf(
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT,
            YogurtProductionService.EVENT_TYPE_PRODUCTION,
            SpoilageService.EVENT_TYPE_SPOILAGE,
        )
        val records = ArrayList<NativeOperationsWorkRecord>()
        for (event in database.eventDao().findAllDescending()) {
            if (event.occurredAt != context.businessDate) continue
            if (event.eventType !in allowed) continue
            val link = database.eventActorLinkDao().find(event.eventId) ?: continue
            if (link.actorUserId != actor.userId) continue
            records += toRecord(event.eventId, event.eventType, event.createdAt, event.rawLiters)
        }
        return NativeOperationsMoreToday(context = context, records = records)
    }

    suspend fun loadSecurity(sessionHandle: String): NativeMoreSecurityStatus {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val service = quickUnlock
            ?: return NativeMoreSecurityStatus(
                quickPinEnabled = false,
                phoneSecurityEnabled = false,
                phoneSecurityAvailable = false,
                phoneSecurityLabel = "Phone security",
            )
        val status = service.status()
        val enrollment = status.accounts.singleOrNull { it.loginName == actor.loginName }
        return NativeMoreSecurityStatus(
            quickPinEnabled = enrollment?.pinEnabled == true,
            phoneSecurityEnabled = enrollment?.deviceAuthenticationEnabled == true,
            phoneSecurityAvailable = status.deviceAuthenticationAvailable,
            phoneSecurityLabel = status.deviceAuthenticationLabel,
        )
    }

    suspend fun enrollQuickUnlock(
        sessionHandle: String,
        currentPassphrase: CharArray,
        enablePhoneSecurity: Boolean,
        pin: CharArray?,
    ): NativeMoreSecurityStatus {
        val service = quickUnlock
            ?: throw IllegalStateException("Quick unlock is unavailable in this preview.")
        try {
            service.enroll(
                sessionId = sessionHandle,
                currentPassphrase = currentPassphrase,
                enableDeviceAuthentication = enablePhoneSecurity,
                pin = pin,
            )
        } finally {
            currentPassphrase.fill('\u0000')
            pin?.fill('\u0000')
        }
        return loadSecurity(sessionHandle)
    }

    suspend fun removeQuickUnlock(
        sessionHandle: String,
        currentPassphrase: CharArray,
    ): NativeMoreSecurityStatus {
        val service = quickUnlock
            ?: throw IllegalStateException("Quick unlock is unavailable in this preview.")
        try {
            service.remove(sessionHandle, currentPassphrase)
        } finally {
            currentPassphrase.fill('\u0000')
        }
        return loadSecurity(sessionHandle)
    }

    private suspend fun toRecord(
        eventId: Long,
        eventType: String,
        createdAt: String,
        rawLiters: Double?,
    ): NativeOperationsWorkRecord {
        val taskLabel = when (eventType) {
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> "Receive milk"
            YogurtProductionService.EVENT_TYPE_PRODUCTION -> "Make yogurt"
            SpoilageService.EVENT_TYPE_SPOILAGE -> "Spoilage"
            else -> eventType
        }
        val detail = buildPhysicalDetail(eventId, eventType, rawLiters)
        return NativeOperationsWorkRecord(
            eventId = eventId,
            taskLabel = taskLabel,
            detailLabel = detail,
            timeLabel = formatClock(createdAt),
        )
    }

    private suspend fun buildPhysicalDetail(
        eventId: Long,
        eventType: String,
        rawLiters: Double?,
    ): String {
        val parts = ArrayList<String>()
        when (eventType) {
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> {
                rawLiters?.let { parts += "${formatQuantity(it)} L received" }
                vendorName(eventId)?.let { parts += it }
            }
            else -> {
                val logs = database.inventoryLogDao().findByEventId(eventId)
                for (log in logs) {
                    val product = database.productDao().findById(log.productId) ?: continue
                    val qty = formatQuantity(kotlin.math.abs(log.quantity))
                    val unit = product.unit.trim()
                    val verb = when {
                        log.quantity < 0 -> "removed"
                        eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION -> "produced"
                        else -> "moved"
                    }
                    parts += listOf(qty, unit, product.name, verb)
                        .filter { it.isNotEmpty() }
                        .joinToString(" ")
                }
            }
        }
        return parts.joinToString(" · ").ifBlank { "Recorded" }
    }

    private suspend fun vendorName(eventId: Long): String? {
        for (movement in database.financialLogDao().findByEventId(eventId)) {
            val account = database.accountDao().findById(movement.accountId) ?: continue
            if (account.category == "Vendor") {
                return account.name.removePrefix("Vendor: ").trim()
            }
        }
        return null
    }

    private fun formatQuantity(value: Double): String {
        val normalized = BigDecimal.valueOf(value).stripTrailingZeros()
        return if (normalized.scale() <= 0) normalized.toPlainString() else "%.2f".format(Locale.US, value)
    }

    private fun formatClock(createdAt: String): String =
        com.saaf.milkshop.application.AppTimeFormats.clock(createdAt)
}

data class NativeOperationsMoreContext(
    val businessDate: String,
    val postingOpen: Boolean,
)

data class NativeOperationsWorkRecord(
    val eventId: Long,
    val taskLabel: String,
    val detailLabel: String,
    val timeLabel: String,
)

data class NativeOperationsMoreToday(
    val context: NativeOperationsMoreContext,
    val records: List<NativeOperationsWorkRecord>,
)
