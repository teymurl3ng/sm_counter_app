package com.saaf.milkshop.application.drawing

import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

data class NativeDrawingPaymentAccount(
    val accountId: Long,
    val name: String,
)

data class NativeDrawingContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val paymentAccount: NativeDrawingPaymentAccount,
)

data class NativeDrawingRequest(
    val amountPaisa: Long,
    val notes: String?,
)

data class NativeDrawingReceipt(
    val eventId: Long,
    val amountPaisa: Long,
    val paymentAccount: NativeDrawingPaymentAccount,
)

data class NativeDrawingSubmission(
    val receipt: NativeDrawingReceipt,
    val refreshedContext: NativeDrawingContext,
)

/**
 * Typed native boundary for Owner drawing (M18): cash the owner takes out of
 * the drawer for non-shop purposes. The drawer is always the seeded "Cash"
 * account — drawings are an equity withdrawal, not an expense, so no category
 * and no counter-leg account exist; the event itself is the record. Posting
 * goes through [OperationsBridgeCommandService.recordOwnerDrawing] (owner/admin
 * permission, open-day guard, audit, sync push).
 */
class NativeDrawingFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    private val commands = OperationsBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeDrawingContext {
        security.requirePermission(sessionHandle, AppPermission.OWNER_DRAWING)
        val drawer = database.accountDao().findByCategory("Cash/Bank")
            .singleOrNull { it.name == "Cash" }
            ?: throw IllegalStateException("The Cash drawer account is unavailable.")
        val businessDate = date.toString()
        return NativeDrawingContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            paymentAccount = NativeDrawingPaymentAccount(drawer.accountId, drawer.name),
        )
    }

    suspend fun record(
        sessionHandle: String,
        request: NativeDrawingRequest,
        date: LocalDate = BusinessClock.today(),
    ): NativeDrawingSubmission {
        require(request.amountPaisa > 0L) { "Enter an amount greater than zero." }
        val context = load(sessionHandle, date)
        val result = commands.recordOwnerDrawing(
            sessionHandle = sessionHandle,
            amount = BigDecimal(request.amountPaisa).movePointLeft(2).toDouble(),
            paymentAccountId = null,
            occurredAt = context.businessDate,
            notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },
        )
        val refreshedContext = load(sessionHandle, date)
        return NativeDrawingSubmission(
            receipt = NativeDrawingReceipt(
                eventId = result.eventId,
                amountPaisa = request.amountPaisa,
                paymentAccount = context.paymentAccount,
            ),
            refreshedContext = refreshedContext,
        )
    }
}
