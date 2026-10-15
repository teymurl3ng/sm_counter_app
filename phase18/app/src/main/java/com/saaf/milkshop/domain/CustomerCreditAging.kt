package com.saaf.milkshop.domain

import com.saaf.milkshop.data.MilkShopDatabase
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

data class CreditAge(
    val available: Boolean,
    val oldestKnownDays: Long? = null,
    val unknownAgePaisa: Long = 0,
) {
    val description: String get() = when {
        !available -> "Age unavailable for these records."
        unknownAgePaisa > 0 -> "Carried-over debt has unknown age." +
            (oldestKnownDays?.let { " Oldest dated unpaid sale: $it ${if (it == 1L) "day" else "days"}." } ?: "")
        oldestKnownDays != null -> "Oldest unpaid sale: $oldestKnownDays ${if (oldestKnownDays == 1L) "day" else "days"}."
        else -> "No unpaid debt."
    }
}

/** One event's movement for this customer; zero-movement ancestors remain in the graph. */
data class CreditAgeFact(
    val eventId: Long,
    val eventUid: String,
    val correctsEventId: Long?,
    val eventType: String,
    val businessDate: String,
    val amount: BigDecimal,
)

/** Reconciled visible-paisa obligations, in the same FIFO order used for age. */
data class UnpaidCreditLot(val rootSaleUid: String, val saleDate: LocalDate?, val remainingPaisa: Long)

data class CreditAllocation(
    val balancePaisa: Long,
    val age: CreditAge,
    val unpaidLots: List<UnpaidCreditLot>,
)

object CustomerCreditAging {
    fun calculate(facts: List<CreditAgeFact>, canonicalBalancePaisa: Long, asOf: LocalDate): CreditAge =
        allocate(facts, canonicalBalancePaisa, asOf).age

    fun allocate(facts: List<CreditAgeFact>, canonicalBalancePaisa: Long, asOf: LocalDate): CreditAllocation {
        return try {
            val byId = facts.associateBy { it.eventId }
            require(byId.size == facts.size)
            val families = linkedMapOf<Long, MutableList<CreditAgeFact>>()
            for (fact in facts) {
                val date = LocalDate.parse(fact.businessDate)
                require(!date.isAfter(asOf))
                val visited = hashSetOf<Long>()
                var root = fact
                while (true) {
                    require(visited.add(root.eventId))
                    val parentId = root.correctsEventId ?: break
                    val parent = requireNotNull(byId[parentId])
                    require(parent.eventType == fact.eventType)
                    if (fact.eventType != "OPENING_STATE") require(parent.businessDate == fact.businessDate)
                    root = parent
                }
                require(root.eventUid.isNotBlank())
                families.getOrPut(root.eventId) { mutableListOf() }.add(fact)
            }
            data class Lot(val uid: String, val date: LocalDate?, val amount: BigDecimal)
            val lots = mutableListOf<Lot>()
            var credit = BigDecimal.ZERO
            val rootUids = hashSetOf<String>()
            for ((id, entries) in families) {
                val root = byId.getValue(id)
                require(rootUids.add(root.eventUid))
                val amount = entries.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount }
                when (root.eventType) {
                    "SALE" -> { require(amount.signum() >= 0); lots += Lot(root.eventUid, LocalDate.parse(root.businessDate), amount) }
                    "CREDIT_COLLECTION" -> { require(amount.signum() <= 0); credit -= amount }
                    "OPENING_STATE" -> if (amount.signum() > 0) lots += Lot(root.eventUid, null, amount) else credit -= amount
                    else -> error("Unsupported customer movement")
                }
            }
            fun paisa(value: BigDecimal) = value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
            val net = facts.fold(BigDecimal.ZERO) { sum, fact -> sum + fact.amount }
            require(paisa(net) == canonicalBalancePaisa)
            var cumulative = BigDecimal.ZERO
            var rounded = 0L
            var unknown = 0L
            var oldest: Long? = null
            val unpaid = mutableListOf<UnpaidCreditLot>()
            for (lot in lots.sortedWith(compareBy<Lot>({ it.date ?: LocalDate.MIN }, { it.uid }))) {
                val paid = credit.min(lot.amount)
                credit -= paid
                cumulative += lot.amount - paid
                val nextRounded = paisa(cumulative)
                val remaining = nextRounded - rounded
                rounded = nextRounded
                if (remaining > 0) {
                    unpaid += UnpaidCreditLot(lot.uid, lot.date, remaining)
                    if (lot.date == null) unknown += remaining
                    else if (oldest == null) oldest = java.time.temporal.ChronoUnit.DAYS.between(lot.date, asOf)
                }
            }
            require(rounded == canonicalBalancePaisa.coerceAtLeast(0))
            CreditAllocation(canonicalBalancePaisa, CreditAge(true, oldest, unknown), unpaid.toList())
        } catch (_: Exception) { CreditAllocation(canonicalBalancePaisa, CreditAge(false), emptyList()) }
    }
}

/** Selected-customer read only. Never rewrites transactions or invents a balancing movement. */
class CustomerCreditAgeService(private val database: MilkShopDatabase) {
    suspend fun load(accountId: Long, balancePaisa: Long, asOf: LocalDate): CreditAge = try {
        val rows = database.financialLogDao().findAccountLedger(accountId)
        val amounts = rows.groupBy { it.eventId }.mapValues { (_, movements) ->
            movements.fold(BigDecimal.ZERO) { sum, row -> sum + BigDecimal.valueOf(row.movementAmount) }
        }
        val facts = rows.associate { row -> row.eventId to CreditAgeFact(row.eventId, row.eventUid,
            row.correctsEventId, row.eventType, row.occurredAt, amounts.getValue(row.eventId)) }.toMutableMap()
        suspend fun include(id: Long) {
            var next: Long? = id
            val visited = hashSetOf<Long>()
            while (next != null && visited.add(next)) {
                val fact = facts[next] ?: requireNotNull(database.eventDao().findById(next)).let { event ->
                    CreditAgeFact(event.eventId, event.eventUid, event.correctsEventId, event.eventType,
                        event.occurredAt, amounts[event.eventId] ?: BigDecimal.ZERO).also { facts[next!!] = it }
                }
                next = fact.correctsEventId
            }
        }
        for (id in amounts.keys) include(id)
        for (event in facts.values) {
            if (event.correctsEventId == null && event.eventType != "OPENING_STATE") {
                require(event.eventId in amounts) { "The original customer movement is unavailable" }
            }
        }
        CustomerCreditAging.calculate(facts.values.toList(), balancePaisa, asOf)
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { CreditAge(false) }
}
