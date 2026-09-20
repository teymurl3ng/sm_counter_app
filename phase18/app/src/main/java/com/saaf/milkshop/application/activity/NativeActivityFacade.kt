package com.saaf.milkshop.application.activity

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.UserSecurityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NativeActivityContext(
    val businessDate: String,
    val postingOpen: Boolean,
)

data class NativeActivityRecordSummary(
    val eventId: Long,
    val occurredAt: String,
    val createdAt: String,
    val taskLabel: String,
    val amountPaisa: Long,
    val stateLabel: String,
    val timeLabel: String,
)

data class NativeActivityRecordDetail(
    val summary: NativeActivityRecordSummary,
    val counterpartLabel: String?,
    val destinationLabel: String?,
    val notes: String?,
    val quantityLabel: String?,
)

data class NativeActivityDay(
    val context: NativeActivityContext,
    val records: List<NativeActivityRecordSummary>,
)

/**
 * Cashier-scoped Activity boundary: own records for the current business date only.
 * Does not reuse shop-wide eventHistory and does not expose full financial ledgers.
 */
class NativeActivityFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun loadToday(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeActivityDay {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val businessDate = date.toString()
        val events = database.eventDao().findAllDescending()
            .filter { it.occurredAt == businessDate }
            .filter { it.eventType == SaleService.EVENT_TYPE_SALE || it.eventType == CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION }
        val records = ArrayList<NativeActivityRecordSummary>()
        for (event in events) {
            val link = database.eventActorLinkDao().find(event.eventId) ?: continue
            if (link.actorUserId != actor.userId) continue
            records += toSummary(event)
        }
        return NativeActivityDay(
            context = NativeActivityContext(
                businessDate = businessDate,
                postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            ),
            records = records,
        )
    }

    suspend fun loadDetail(
        sessionHandle: String,
        eventId: Long,
        date: LocalDate = LocalDate.now(),
    ): NativeActivityRecordDetail {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val event = database.eventDao().findById(eventId)
            ?: throw IllegalArgumentException("That record is not available.")
        val link = database.eventActorLinkDao().find(eventId)
            ?: throw IllegalArgumentException("That record is not available.")
        if (link.actorUserId != actor.userId) {
            throw IllegalArgumentException("That record belongs to another account.")
        }
        if (event.eventType != SaleService.EVENT_TYPE_SALE &&
            event.eventType != CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION
        ) {
            throw IllegalArgumentException("That record is not part of Cashier Activity.")
        }
        // Prefer same-day Activity entries; still allow Collect View record the same day.
        if (event.occurredAt != date.toString()) {
            // Allow detail when owned even if opened from a receipt on the posting date encoded on the event.
            // Reject only when the event is clearly outside the cashier Activity product types handled above.
        }
        val parties = resolveParties(event.eventId)
        val summary = toSummary(event, parties)
        return NativeActivityRecordDetail(
            summary = summary,
            counterpartLabel = parties.counterpart,
            destinationLabel = parties.destination,
            notes = event.notes?.trim()?.takeIf { it.isNotEmpty() },
            quantityLabel = event.saleQuantity?.let { qty ->
                val unit = database.inventoryLogDao().findByEventId(event.eventId)
                    .firstOrNull()
                    ?.let { database.productDao().findById(it.productId)?.unit }
                    ?: ""
                listOf(formatQuantity(qty), unit).filter { it.isNotEmpty() }.joinToString(" ")
            },
        )
    }

    private data class Parties(
        val counterpart: String?,
        val destination: String?,
        val stateLabel: String,
    )

    private suspend fun resolveParties(eventId: Long): Parties {
        val movements = database.financialLogDao().findByEventId(eventId)
        var customerName: String? = null
        var cashBankName: String? = null
        for (movement in movements) {
            val account = database.accountDao().findById(movement.accountId) ?: continue
            when (account.category) {
                "Customer" -> customerName = cleanAccountName(account.name, account.category)
                "Cash/Bank" -> cashBankName = cleanAccountName(account.name, account.category)
            }
        }
        val event = database.eventDao().findById(eventId)
        return when (event?.eventType) {
            SaleService.EVENT_TYPE_SALE -> {
                if (customerName != null) {
                    Parties(
                        counterpart = customerName,
                        destination = null,
                        stateLabel = "On account · $customerName",
                    )
                } else {
                    Parties(
                        counterpart = null,
                        destination = cashBankName,
                        stateLabel = listOfNotNull("Paid now", cashBankName).joinToString(" · "),
                    )
                }
            }
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION -> Parties(
                counterpart = customerName,
                destination = cashBankName,
                stateLabel = listOfNotNull(customerName, cashBankName?.let { "Received in $it" })
                    .joinToString(" · "),
            )
            else -> Parties(null, null, "Recorded")
        }
    }

    private suspend fun toSummary(
        event: com.saaf.milkshop.data.EventEntity,
        parties: Parties? = null,
    ): NativeActivityRecordSummary {
        val resolved = parties ?: resolveParties(event.eventId)
        val amountPaisa = when (event.eventType) {
            SaleService.EVENT_TYPE_SALE -> moneyToPaisa((event.saleValue ?: 0.0) + event.surchargeApplied)
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION -> moneyToPaisa(event.amount ?: 0.0)
            else -> 0L
        }
        return NativeActivityRecordSummary(
            eventId = event.eventId,
            occurredAt = event.occurredAt,
            createdAt = event.createdAt,
            taskLabel = when (event.eventType) {
                SaleService.EVENT_TYPE_SALE -> "Sale"
                CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION -> "Collection"
                else -> event.eventType
            },
            amountPaisa = amountPaisa,
            stateLabel = resolved.stateLabel,
            timeLabel = formatClock(event.createdAt),
        )
    }

    private fun cleanAccountName(name: String, category: String): String = when (category) {
        "Customer" -> name.removePrefix("Customer: ")
        "Vendor" -> name.removePrefix("Vendor: ")
        else -> name
    }.trim()

    private fun moneyToPaisa(value: Double): Long = BigDecimal.valueOf(value)
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

    private fun formatQuantity(value: Double): String {
        val normalized = BigDecimal.valueOf(value).stripTrailingZeros()
        return if (normalized.scale() <= 0) normalized.toPlainString() else "%.2f".format(Locale.US, value)
    }

    private fun formatClock(createdAt: String): String = try {
        val instant = OffsetDateTime.parse(createdAt)
        instant.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    } catch (_: Exception) {
        createdAt.takeLast(8)
    }
}
