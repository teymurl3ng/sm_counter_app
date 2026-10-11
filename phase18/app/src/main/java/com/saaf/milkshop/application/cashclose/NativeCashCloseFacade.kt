package com.saaf.milkshop.application.cashclose

import com.saaf.milkshop.application.commands.ReconciliationBridgeCommandService
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.data.MilkShopDatabase
import androidx.room3.withWriteTransaction
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import com.saaf.milkshop.domain.VerifiedDailyReopenInput
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs
import com.saaf.milkshop.sync.BusinessClock

data class NativeCashCloseCountSnapshot(
    val countId: Long,
    val countedCashPaisa: Long,
    val expectedCashPaisa: Long,
    val variancePaisa: Long,
    val countedAt: String,
)

data class NativeCashClosePendingDelivery(
    val deliveryId: Long,
    val label: String,
)

data class NativeCashCloseAbsorbedDay(
    val businessDate: String,
    val eventCount: Int,
)

data class NativeCashCloseContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val cashAccountId: Long,
    val cashAccountName: String,
    /** Current drawer expectation from the Cash account balance (same source as Owner Overview). */
    val expectedCashPaisa: Long,
    val latestCount: NativeCashCloseCountSnapshot?,
    /**
     * Always null for Owner Cash close — reopen write UI is connected.
     * Retained for binary compatibility with earlier contract fixtures.
     */
    val reopenDeferredNotice: String? = null,
    /** H8: open days earlier than the close date that today's close will absorb. */
    val absorbedDays: List<NativeCashCloseAbsorbedDay> = emptyList(),
    /** H9: today's pending deliveries, listed in the dock with one-tap resolve. */
    val pendingDeliveries: List<NativeCashClosePendingDelivery> = emptyList(),
    /** H8: one-line multi-day coverage note for the variance screen (null = single day). */
    val coverageNote: String? = null,
    /** H8: "Last synced N min ago" signal for the pre-close decision. */
    val lastSyncedLabel: String = "Unknown",
    /** H8: reverse consequence for the reopen confirmation (null = nothing absorbed). */
    val reopenNote: String? = null,
    /** H8: refused pushes in the last 24 h (closed-day refusals arrive as halts). */
    val refusalCount: Int = 0,
)

data class NativeCashCloseCountRequest(
    val countedCashPaisa: Long,
    val notes: String?,
)

data class NativeCashCloseDayRequest(
    val physicalCashCountId: Long,
    val varianceReason: String?,
)

data class NativeCashCloseReceipt(
    val businessDate: String,
    val actionId: Long,
    val countedCashPaisa: Long,
    val expectedCashPaisa: Long,
    val variancePaisa: Long,
)

data class NativeCashCloseCountSubmission(
    val count: NativeCashCloseCountSnapshot,
    val refreshedContext: NativeCashCloseContext,
)

data class NativeCashCloseDaySubmission(
    val receipt: NativeCashCloseReceipt,
    val refreshedContext: NativeCashCloseContext,
)

data class NativeCashCloseReopenSubmission(
    val businessDate: String,
    val actionId: Long,
    val refreshedContext: NativeCashCloseContext,
)

/**
 * Typed native boundary for Owner Cash count and close (including reopen depth).
 *
 * Count, close, and reopen go through [ReconciliationBridgeCommandService].
 * Expected cash comes from the Cash account balance / latest physical count —
 * never invents formulas. Reopen requires full passphrase re-auth (not Quick PIN).
 */
class NativeCashCloseFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val reconciliation = ReconciliationBridgeCommandService(database, security)
    private val physicalCounts = PhysicalCashCountService(database)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeCashCloseContext {
        security.requirePermission(sessionHandle, AppPermission.CASH_COUNT_AND_CLOSE)
        val cash = database.accountDao().findByCategory("Cash/Bank")
            .singleOrNull { it.name == "Cash" }
            ?: throw IllegalStateException("The Cash drawer account is unavailable.")
        val businessDate = date.toString()
        val postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate)
        val expectedCashPaisa = money(
            database.readModelDao().accountBalance(cash.accountId)?.balance ?: 0.0,
        )
        val latest = physicalCounts.latest(cash.accountId, businessDate)?.let { snapshot ->
            NativeCashCloseCountSnapshot(
                countId = snapshot.count.countId,
                countedCashPaisa = money(snapshot.count.countedAmount),
                expectedCashPaisa = money(snapshot.expectedBalance),
                variancePaisa = money(snapshot.variance),
                countedAt = snapshot.count.countedAt,
            )
        }
        val pendingDeliveries = if (postingOpen) {
            database.deliveryDao().activeDeliveries()
                .filter { it.businessDate <= businessDate && !database.dailyCloseDao().isPostingClosed(it.businessDate) }
                .mapNotNull { delivery ->
                    val status = database.deliveryDao().latestStatus(delivery.deliveryId)?.status
                    if (status != "pending") return@mapNotNull null
                    val customer = database.accountDao().findById(delivery.customerAccountId)?.name
                        ?: "Customer #${delivery.customerAccountId}"
                    val product = database.productDao().findById(delivery.productId)?.name
                        ?: "Product #${delivery.productId}"
                    NativeCashClosePendingDelivery(
                        deliveryId = delivery.deliveryId,
                        label = "${delivery.businessDate}: $product for $customer (${delivery.quantity})",
                    )
                }
        } else {
            emptyList()
        }
        val absorbedDays = if (postingOpen) {
            database.dailyCloseDao().absorbedOpenDays(businessDate).map {
                NativeCashCloseAbsorbedDay(it.businessDate, it.eventCount)
            }
        } else {
            emptyList()
        }
        return NativeCashCloseContext(
            businessDate = businessDate,
            postingOpen = postingOpen,
            cashAccountId = cash.accountId,
            cashAccountName = cash.name,
            expectedCashPaisa = expectedCashPaisa,
            latestCount = latest,
            reopenDeferredNotice = null,
            absorbedDays = absorbedDays,
            pendingDeliveries = pendingDeliveries,
            coverageNote = CloseSemanticsLabels.coverageLine(
                businessDate,
                absorbedDays.map { it.businessDate },
            ),
            lastSyncedLabel = lastSyncedLabel(),
            refusalCount = database.securityAuditDao().refusalsSince(
                java.time.Instant.now().minusSeconds(24 * 3600).toString(),
            ),
            reopenNote = if (!postingOpen) {
                CloseSemanticsLabels.reopenLine(
                    businessDate,
                    database.securityAuditDao().absorbedDaysSealedBy(businessDate),
                )
            } else {
                null
            },
        )
    }

    suspend fun recordCount(
        sessionHandle: String,
        request: NativeCashCloseCountRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeCashCloseCountSubmission {
        require(request.countedCashPaisa >= 0L) { "Counted drawer cash cannot be negative." }
        val context = load(sessionHandle, date)
        if (!context.postingOpen) {
            throw IllegalStateException("This business day is closed. Reopen it before counting cash.")
        }
        // The mutation, required evidence and receipt reads commit together.
        // A read/authorization failure cannot report failure after saving.
        return database.withWriteTransaction {
            val result = reconciliation.recordCount(
                sessionHandle,
                PhysicalCashCountInput(
                    accountId = context.cashAccountId,
                    businessDate = context.businessDate,
                    countedAmount = BigDecimal(request.countedCashPaisa).movePointLeft(2).toDouble(),
                    notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            val count = NativeCashCloseCountSnapshot(
                countId = result.count.countId,
                countedCashPaisa = money(result.count.countedAmount),
                expectedCashPaisa = money(result.expectedBalance),
                variancePaisa = money(result.variance),
                countedAt = result.count.countedAt,
            )
            NativeCashCloseCountSubmission(
                count = count,
                refreshedContext = load(sessionHandle, date),
            )
        }
    }

    suspend fun closeDay(
        sessionHandle: String,
        request: NativeCashCloseDayRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeCashCloseDaySubmission {
        return database.withWriteTransaction {
        val context = load(sessionHandle, date)
        if (!context.postingOpen) {
            throw IllegalStateException("This business date is already closed.")
        }
        val latest = context.latestCount
            ?: throw IllegalStateException("Save a physical drawer count for this date before closing it.")
        require(latest.countId == request.physicalCashCountId) {
            "Use the latest physical cash count for this date."
        }
        if (needsVarianceReason(latest.variancePaisa)) {
            val reason = request.varianceReason?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Explain the non-zero cash variance before closing.")
            require(reason.isNotEmpty())
        }
        require(context.pendingDeliveries.isEmpty()) {
            "Finish or archive pending deliveries before closing the day."
        }
        require(latest.expectedCashPaisa == context.expectedCashPaisa) {
            "Cash changed since this count. Record a fresh drawer count before closing."
        }
        // The mutation, required evidence and receipt reads commit together.
        // A read/authorization failure cannot report failure after saving.
            val result = reconciliation.closeDate(
                sessionHandle,
                VerifiedDailyCloseInput(
                    businessDate = context.businessDate,
                    physicalCashCountId = request.physicalCashCountId,
                    varianceReason = request.varianceReason?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            // H8: record HOW each absorbed day was sealed — audit rows that say
            // "absorbed by this close, with no count of its own", so reports can
            // tell closed-with-count apart from absorbed. The surrounding
            // transaction includes the close, these seal rows and its receipt;
            // a crash or audit failure cannot separate their commits.
            if (context.absorbedDays.isNotEmpty()) {
                val actor = security.requirePermission(sessionHandle, AppPermission.CASH_COUNT_AND_CLOSE)
                database.withWriteTransaction {
                    context.absorbedDays.forEach { day ->
                        database.securityAuditDao().insert(
                            com.saaf.milkshop.data.SecurityAuditEntryEntity(
                                auditId = 0,
                                occurredAt = java.time.Instant.now().toString(),
                                action = "daily_close_seal_absorbed",
                                result = "info",
                                reasonCode = day.businessDate,
                                details = "sealed_by=${context.businessDate};events=${day.eventCount};actor=${actor.userId}",
                            ),
                        )
                    }
                }
            }
            val counted = result.countedCash ?: latest.countedCashPaisa / 100.0
            val expected = result.expectedCash ?: latest.expectedCashPaisa / 100.0
            val variance = result.variance ?: (counted - expected)
            NativeCashCloseDaySubmission(
                receipt = NativeCashCloseReceipt(
                    businessDate = context.businessDate,
                    actionId = result.action.actionId,
                    countedCashPaisa = money(counted),
                    expectedCashPaisa = money(expected),
                    variancePaisa = money(variance),
                ),
                refreshedContext = load(sessionHandle, date),
            )
        }
    }

    /**
     * Explicit profile/shop verification is evidence of server contact.
     * Queue timestamps do not prove delivery; this label does not claim convergence.
     */
    private suspend fun lastSyncedLabel(): String {
        val newest = database.serverAccountDao().findBinding()?.verifiedAtEpochMs ?: return "Server contact unknown"
        val minutes = (System.currentTimeMillis() - newest) / 60_000
        return when {
            minutes < 1 -> "Server verified moments ago"
            minutes < 60 -> "Server verified $minutes min ago"
            minutes < 24 * 60 -> "Server verified ${minutes / 60} h ago"
            else -> "Server verified ${minutes / (24 * 60)} days ago"
        }
    }

    /**
     * H9: one-tap resolve from the close dock. Today is still open at this
     * moment (the dock only shows this pre-close), so the deliveries
     * facade's posting-open gate passes.
     */
    suspend fun resolvePendingDelivery(
        sessionHandle: String,
        deliveryId: Long,
        date: LocalDate = BusinessClock.today(),
    ): NativeCashCloseContext {
        val deliveries = NativeDeliveriesFacade(database, security)
        return database.withWriteTransaction {
            deliveries.markDelivered(sessionHandle, deliveryId, date)
            load(sessionHandle, date)
        }
    }

    /**
     * Reopens a closed business date. Requires [AppPermission.REOPEN_DAY] and a full
     * passphrase re-authentication (Quick PIN cannot authorize). Wipes [passphrase]
     * before returning.
     */
    suspend fun reopenDay(
        sessionHandle: String,
        reason: String,
        passphrase: CharArray,
        date: LocalDate = BusinessClock.today(),
    ): NativeCashCloseReopenSubmission {
        try {
            val cleanedReason = reason.trim()
            require(cleanedReason.isNotEmpty()) { "Enter a reason for reopening the business date." }
            security.requirePermission(sessionHandle, AppPermission.REOPEN_DAY)
            val context = load(sessionHandle, date)
            if (context.postingOpen) {
                throw IllegalStateException("This business date is already open.")
            }
            security.reauthenticate(sessionHandle, passphrase)
            // The mutation, required evidence and receipt reads commit together.
            // A read/authorization failure cannot report failure after saving.
            return database.withWriteTransaction {
                val result = reconciliation.reopenDate(
                    sessionHandle,
                    VerifiedDailyReopenInput(
                        businessDate = context.businessDate,
                        reason = cleanedReason,
                    ),
                )
                val refreshed = load(sessionHandle, date)
                require(refreshed.postingOpen) { "Reopen did not restore an open posting day." }
                NativeCashCloseReopenSubmission(
                    businessDate = context.businessDate,
                    actionId = result.action.actionId,
                    refreshedContext = refreshed,
                )
            }
        } finally {
            passphrase.fill('\u0000')
        }
    }

    companion object {
        fun needsVarianceReason(variancePaisa: Long): Boolean = abs(variancePaisa) != 0L
    }
}
