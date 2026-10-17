package com.saaf.milkshop.application.owner

import androidx.room3.withReadTransaction
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.application.readmodels.decimal
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.BusinessClock
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

data class CustomerStatementPeriod(val start: LocalDate, val endInclusive: LocalDate) {
    fun validate(today: LocalDate) {
        require(start <= endInclusive && endInclusive <= today) { "Choose a valid period ending no later than today." }
    }
    val endExclusive: String get() = endInclusive.plusDays(1).toString()
    val label: String get() = "$start – $endInclusive"
    companion object {
        fun lastWeek(today: LocalDate) = CustomerStatementPeriod(today.minusDays(6), today)
    }
}

data class CustomerStatementLine(
    val date: String, val reference: String, val description: String, val productText: String?,
    val movementPaisa: Long, val balancePaisa: Long, val roundingAdjusted: Boolean,
)

data class CustomerStatement(
    val customerId: Long, val customerUid: String, val customerName: String,
    val shopName: String, val shopPhone: String?, val shopAddress: String?, val currency: String,
    val period: CustomerStatementPeriod, val generatedAt: String, val openingPaisa: Long,
    val closingPaisa: Long, val lines: List<CustomerStatementLine>,
) {
    val closingLabel: String get() = when {
        closingPaisa < 0 -> "Customer advance"
        closingPaisa == 0L -> "Settled"
        else -> "Amount outstanding"
    }
}

internal fun statementMoney(paisa: Long): String = "PKR " + String.format(Locale.US, "%,.2f", BigDecimal.valueOf(paisa, 2))

/** A read-only statement snapshot. No balances, status, agreements or export files are written. */
internal class CustomerStatementReader(private val db: MilkShopDatabase, private val security: UserSecurityService) {
    suspend fun load(session: String, accountId: Long, period: CustomerStatementPeriod,
        today: LocalDate = BusinessClock.today()): CustomerStatement = db.withReadTransaction {
        period.validate(today)
        val actor = security.requirePermission(session, AppPermission.VIEW_FULL_FINANCIAL)
        require(AppRole.parse(actor.role) in setOf(AppRole.OWNER, AppRole.MANAGER, AppRole.ADMIN)) { "Sign in as Owner, Admin or Manager to prepare statements." }
        val account = requireNotNull(db.accountDao().findById(accountId)) { "Customer no longer available." }
        require(account.category == "Customer") { "Choose a customer account." }
        val identity = requireNotNull(db.storeProfileDao().get()) { "Set the shop name in Shop settings before preparing a statement." }
        require(identity.storeName.isNotBlank()) { "Set the shop name in Shop settings before preparing a statement." }
        val dao = db.financialLogDao()
        val opening = money(dao.statementBalanceBefore(accountId, period.start.toString()))
        val closing = money(dao.statementBalanceBefore(accountId, period.endExclusive))
        val rows = dao.statementMovements(accountId, period.start.toString(), period.endExclusive)
        require(rows.size <= 10000) { "There are too many entries for one statement. Choose a shorter period." }
        val products = dao.statementProducts(accountId, period.start.toString(), period.endExclusive).groupBy { it.eventId }
        var previous = opening
        val lines = rows.map { row ->
            val balance = money(row.balanceAfter)
            val movement = Math.subtractExact(balance, previous)
            previous = balance
            CustomerStatementLine(row.occurredAt, row.eventUid.takeIf { it.isNotBlank() } ?: "Record ${row.eventId}",
                when {
                    row.correctsEventId != null -> "Correction / adjustment"
                    row.eventType == "SALE" -> "Sale"
                    row.eventType == "CREDIT_COLLECTION" -> "Payment received"
                    row.eventType.startsWith("OPENING") -> "Opening entry"
                    else -> "Account adjustment"
                }, products[row.eventId]?.joinToString(" · ") { "${it.productName} ${decimal(-it.quantity)} ${it.unit}" },
                movement, balance, movement != money(row.movementAmount))
        }
        check(previous == closing) { "The statement entries do not add up. Refresh or choose a shorter period. No statement was prepared." }
        if (period.endInclusive == today) {
            val current = db.readModelDao().accountBalances().singleOrNull { it.accountId == accountId }?.balance ?: 0.0
            check(money(current) == closing) { "Current balance includes entries outside this statement. Review account history before preparing it." }
        }
        CustomerStatement(accountId, account.uid, account.name.removePrefix("Customer: ").trim(),
            identity.storeName, identity.phone, identity.address, identity.currencyCode, period, Instant.now().toString(), opening, closing, lines)
    }
}
