package com.saaf.milkshop.application.cashclose

import com.saaf.milkshop.application.bridge.ReconciliationBridgeCommandService
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs

data class NativeCashCloseCountSnapshot(
    val countId: Long,
    val countedCashPaisa: Long,
    val expectedCashPaisa: Long,
    val variancePaisa: Long,
    val countedAt: String,
)

data class NativeCashCloseContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val cashAccountId: Long,
    val cashAccountName: String,
    /** Current drawer expectation from the Cash account balance (same source as Owner Overview). */
    val expectedCashPaisa: Long,
    val latestCount: NativeCashCloseCountSnapshot?,
    /** Present when the day is closed; reopen write UI is deferred this slice. */
    val reopenDeferredNotice: String?,
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

/**
 * Typed native boundary for Owner Cash count and close.
 *
 * Count and close go through [ReconciliationBridgeCommandService].
 * Expected cash comes from the Cash account balance / latest physical count —
 * never invents formulas. Does not expose reopen writes.
 */
class NativeCashCloseFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val reconciliation = ReconciliationBridgeCommandService(database, security)
    private val physicalCounts = PhysicalCashCountService(database)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
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
        val currentAction = database.dailyCloseDao().currentAction(businessDate)
        val reopenDeferredNotice = when {
            currentAction?.action == DailyCloseService.ACTION_CLOSE ->
                "This date is closed. Reopening is not available in this Owner Work slice yet."
            else -> null
        }
        return NativeCashCloseContext(
            businessDate = businessDate,
            postingOpen = postingOpen,
            cashAccountId = cash.accountId,
            cashAccountName = cash.name,
            expectedCashPaisa = expectedCashPaisa,
            latestCount = latest,
            reopenDeferredNotice = reopenDeferredNotice,
        )
    }

    suspend fun recordCount(
        sessionHandle: String,
        request: NativeCashCloseCountRequest,
        date: LocalDate = LocalDate.now(),
    ): NativeCashCloseCountSubmission {
        require(request.countedCashPaisa >= 0L) { "Counted drawer cash cannot be negative." }
        val context = load(sessionHandle, date)
        if (!context.postingOpen) {
            throw IllegalStateException("This business day is closed. Reopen it before counting cash.")
        }
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
        return NativeCashCloseCountSubmission(
            count = count,
            refreshedContext = load(sessionHandle, date),
        )
    }

    suspend fun closeDay(
        sessionHandle: String,
        request: NativeCashCloseDayRequest,
        date: LocalDate = LocalDate.now(),
    ): NativeCashCloseDaySubmission {
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
        val result = reconciliation.closeDate(
            sessionHandle,
            VerifiedDailyCloseInput(
                businessDate = context.businessDate,
                physicalCashCountId = request.physicalCashCountId,
                varianceReason = request.varianceReason?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        val counted = result.countedCash ?: latest.countedCashPaisa / 100.0
        val expected = result.expectedCash ?: latest.expectedCashPaisa / 100.0
        val variance = result.variance ?: (counted - expected)
        return NativeCashCloseDaySubmission(
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

    companion object {
        fun needsVarianceReason(variancePaisa: Long): Boolean = abs(variancePaisa) != 0L
    }
}
