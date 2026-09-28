package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudBackupState
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CostCoverageCode
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.saaf.milkshop.sync.BusinessClock

enum class NativeOwnerOverviewAttentionKind {
    CashCountWaiting,
    BackupNeedsAttention,
}

data class NativeOwnerOverviewAttention(
    val kind: NativeOwnerOverviewAttentionKind,
    val title: String,
    val detail: String,
    val actionLabel: String?,
)

data class NativeOwnerOverviewStockLine(
    val productName: String,
    val quantityLabel: String,
)

data class NativeOwnerOverviewLatestActivity(
    val title: String,
    val subtitle: String,
    val trailingLabel: String?,
    val stateLabel: String,
)

/**
 * Owner Overview boundary: decision-first day summary composed from existing reads.
 * Does not invent aging, close/reopen, or new accounting formulas.
 */
data class NativeOwnerOverviewSnapshot(
    val businessDate: String,
    val postingOpen: Boolean,
    val shopName: String,
    val todaySalesPaisa: Long,
    val estimatedGrossPaisa: Long?,
    val coverageComplete: Boolean,
    val coverageWarning: String?,
    val expectedCashPaisa: Long,
    val customersDuePaisa: Long,
    val suppliersDuePaisa: Long,
    val stock: List<NativeOwnerOverviewStockLine>,
    val attention: List<NativeOwnerOverviewAttention>,
    val latestActivity: NativeOwnerOverviewLatestActivity?,
)

class NativeOwnerOverviewFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
    private val cloudBackupSettings: CloudBackupSettingsStore? = null,
) {
    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
        now: Instant = Instant.now(),
    ): NativeOwnerOverviewSnapshot {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Overview is only available to Owner."
        }
        security.requirePermission(sessionHandle, AppPermission.VIEW_FULL_FINANCIAL)

        val businessDate = date.toString()
        val endExclusive = date.plusDays(1).toString()
        val settings = reads.businessSettings(sessionHandle)
        val financial = reads.financialOverview(sessionHandle, businessDate, endExclusive)
        val profitability = reads.profitabilityReport(sessionHandle, businessDate, endExclusive)
        val dashboard = reads.dashboard(sessionHandle, businessDate, endExclusive)
        val archive = reads.reconciliationArchive(sessionHandle)

        val coverageComplete = profitability.coverageCode == CostCoverageCode.COMPLETE
        val coverageWarning = if (coverageComplete) {
            null
        } else {
            "Cost coverage is incomplete. Current stock and cash remain reliable; " +
                "estimated gross is shown only for covered records."
        }

        val cashAccount = database.accountDao().findByCategory("Cash/Bank")
            .firstOrNull { it.name.equals("Cash", ignoreCase = true) }
            ?: database.accountDao().findByCategory("Cash/Bank").firstOrNull()
        val expectedCashPaisa = cashAccount?.let { account ->
            money(
                database.readModelDao().accountBalance(account.accountId)?.balance ?: 0.0,
            )
        } ?: 0L

        val customersDuePaisa = financial.customers
            .filter { it.canonicalBalancePaisa > 0L }
            .sumOf { it.canonicalBalancePaisa }
        val suppliersDuePaisa = financial.vendors
            .filter { it.canonicalBalancePaisa > 0L }
            .sumOf { it.canonicalBalancePaisa }

        val stock = listOfNotNull(
            dashboard.stock.firstOrNull { it.name.equals("Milk", ignoreCase = true) }
                ?.let { NativeOwnerOverviewStockLine(it.name, "${it.quantityOnHand} ${it.unit}".trim()) },
            dashboard.stock.firstOrNull { it.name.equals("Yogurt", ignoreCase = true) }
                ?.let { NativeOwnerOverviewStockLine(it.name, "${it.quantityOnHand} ${it.unit}".trim()) },
        ).ifEmpty {
            dashboard.stock.take(2).map {
                NativeOwnerOverviewStockLine(it.name, "${it.quantityOnHand} ${it.unit}".trim())
            }
        }

        val hasCashCountToday = archive.cashCounts.any { it.businessDate == businessDate }
        val postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate)
        val attention = buildList {
            if (postingOpen && !hasCashCountToday) {
                add(
                    NativeOwnerOverviewAttention(
                        kind = NativeOwnerOverviewAttentionKind.CashCountWaiting,
                        title = "Cash count waiting",
                        detail = "No drawer count has been submitted for today.",
                        actionLabel = "Review",
                    ),
                )
            }
            backupAttention(now)?.let { add(it) }
        }

        return NativeOwnerOverviewSnapshot(
            businessDate = businessDate,
            postingOpen = postingOpen,
            shopName = settings.store?.storeName?.takeIf { it.isNotBlank() } ?: "Saaf Milk",
            todaySalesPaisa = financial.totals.salesValuePaisa,
            estimatedGrossPaisa = profitability.estimatedRecordedResultPaisa,
            coverageComplete = coverageComplete,
            coverageWarning = coverageWarning,
            expectedCashPaisa = expectedCashPaisa,
            customersDuePaisa = customersDuePaisa,
            suppliersDuePaisa = suppliersDuePaisa,
            stock = stock,
            attention = attention,
            latestActivity = latestActivity(),
        )
    }

    private fun backupAttention(now: Instant): NativeOwnerOverviewAttention? {
        val state = cloudBackupSettings?.read() ?: return null
        val needsAttention = state.lastErrorMessage != null ||
            state.lastErrorCode != null ||
            isStale(state, now)
        if (!needsAttention) return null
        val detail = when {
            !state.lastErrorMessage.isNullOrBlank() -> state.lastErrorMessage
            isStale(state, now) -> "Last successful recovery point is older than 48 hours."
            else -> "Cloud backup needs a check."
        }
        return NativeOwnerOverviewAttention(
            kind = NativeOwnerOverviewAttentionKind.BackupNeedsAttention,
            title = "Drive backup needs attention",
            detail = detail,
            actionLabel = "Check",
        )
    }

    private fun isStale(state: CloudBackupState, now: Instant): Boolean {
        val protectedAt = state.lastSuccessAt ?: state.configuredAt
        return runCatching {
            Duration.between(Instant.parse(protectedAt), now) > STALE_AFTER
        }.getOrDefault(false)
    }

    private suspend fun latestActivity(): NativeOwnerOverviewLatestActivity? {
        val events = database.eventDao().findAllDescending()
        val superseded = events.mapNotNull { it.correctsEventId }.toSet()
        val event = events.firstOrNull { it.eventId !in superseded } ?: return null
        val actorName = database.eventActorLinkDao().find(event.eventId)?.actorNameSnapshot
        val title = when (event.eventType) {
            SaleService.EVENT_TYPE_SALE -> "Sale · #S-${event.eventId}"
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION -> "Collection · #C-${event.eventId}"
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> "Milk receipt · #R-${event.eventId}"
            YogurtProductionService.EVENT_TYPE_PRODUCTION -> "Yogurt batch · #Y-${event.eventId}"
            else -> "${event.eventType.replace('_', ' ')} · #${event.eventId}"
        }
        val trailing = when (event.eventType) {
            SaleService.EVENT_TYPE_SALE -> {
                val total = (event.saleValue ?: 0.0) + event.surchargeApplied
                "PKR ${formatMoney(money(total))}"
            }
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION ->
                event.amount?.let { "PKR ${formatMoney(money(it))}" }
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT ->
                event.rawLiters?.let { "${formatQuantity(it)} L" }
            YogurtProductionService.EVENT_TYPE_PRODUCTION ->
                event.yogurtKgProduced?.let { "${formatQuantity(it)} kg" }
            else -> null
        }
        val subtitle = listOfNotNull(
            actorName,
            formatClock(event.createdAt),
        ).joinToString(" · ").ifBlank { "Recorded" }
        return NativeOwnerOverviewLatestActivity(
            title = title,
            subtitle = subtitle,
            trailingLabel = trailing,
            stateLabel = "Completed",
        )
    }

    private fun money(value: Double): Long = BigDecimal.valueOf(value)
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

    private fun formatMoney(paisa: Long): String =
        String.format(Locale.US, "%,.2f", paisa / 100.0)

    private fun formatQuantity(value: Double): String =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun formatClock(iso: String): String = runCatching {
        OffsetDateTime.parse(iso).toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }.getOrElse { iso }

    companion object {
        private val STALE_AFTER = Duration.ofHours(48)
    }
}
