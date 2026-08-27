package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.PhysicalCashCountEntity
import java.time.Instant
import java.time.LocalDate

data class PhysicalCashCountInput(
    val accountId: Long,
    val businessDate: String,
    val countedAmount: Double,
    val notes: String? = null,
)

data class CashReconciliation(
    val count: PhysicalCashCountEntity,
    val expectedBalance: Double,
    val variance: Double,
)

class PhysicalCashCountService(private val database: MilkShopDatabase) {
    suspend fun record(input: PhysicalCashCountInput): CashReconciliation = database.withWriteTransaction {
        LocalDate.parse(input.businessDate)
        require(input.countedAmount >= 0.0) { "Counted cash cannot be negative" }
        val account = database.accountDao().findById(input.accountId)
            ?: error("Cash account ${input.accountId} does not exist")
        require(account.category == "Cash/Bank" && account.name == "Cash") {
            "Physical cash counts must use the Cash account"
        }
        val count = PhysicalCashCountEntity(
            accountId = input.accountId,
            businessDate = input.businessDate,
            countedAmount = input.countedAmount,
            countedAt = Instant.now().toString(),
            notes = input.notes.cleanOptional(),
        )
        val id = database.physicalCashCountDao().insert(count)
        val saved = count.copy(countId = id)
        val expected = expectedBalance(input.accountId, saved.countedAt)
        CashReconciliation(saved, expected, saved.countedAmount - expected)
    }

    suspend fun latest(accountId: Long, businessDate: String): CashReconciliation? {
        val count = database.physicalCashCountDao().latest(accountId, businessDate) ?: return null
        val expected = expectedBalance(accountId, count.countedAt)
        return CashReconciliation(count, expected, count.countedAmount - expected)
    }

    private suspend fun expectedBalance(accountId: Long, countedAt: String): Double =
        database.physicalCashCountDao().canonicalBalanceAt(accountId, countedAt)
}
