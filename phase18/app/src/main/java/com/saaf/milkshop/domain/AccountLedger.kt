package com.saaf.milkshop.domain

import com.saaf.milkshop.data.MilkShopDatabase

data class AccountLedgerEntry(
    val logId: Long,
    val eventId: Long,
    val occurredAt: String,
    val createdAt: String,
    val eventType: String,
    val channel: String?,
    val movementAmount: Double,
    val balanceAfter: Double,
    val correctsEventId: Long?,
    val isCorrection: Boolean,
    val isSuperseded: Boolean,
    val notes: String?,
)

data class AccountLedger(
    val accountId: Long,
    val accountName: String,
    val displayName: String,
    val category: String,
    val currentBalance: Double,
    val entries: List<AccountLedgerEntry>,
)

/**
 * Native account-specific ledger derived only from canonical financial_log
 * movements and event correction relationships. No accounting formula is
 * recreated here. Positive/negative movement signs are exactly the movements
 * already written by the authoritative transaction/correction services.
 */
class AccountLedgerService(
    private val database: MilkShopDatabase,
) {
    suspend fun get(accountId: Long): AccountLedger {
        val account = database.accountDao().findById(accountId)
            ?: error("Account $accountId does not exist")

        require(account.category in SUPPORTED_CATEGORIES) {
            "Account ledger is available only for Customer or Vendor accounts"
        }

        var runningBalance = 0.0
        val entries = database.financialLogDao().findAccountLedger(accountId).map { row ->
            runningBalance += row.movementAmount
            AccountLedgerEntry(
                logId = row.logId,
                eventId = row.eventId,
                occurredAt = row.occurredAt,
                createdAt = row.createdAt,
                eventType = row.eventType,
                channel = row.channel,
                movementAmount = row.movementAmount,
                balanceAfter = runningBalance,
                correctsEventId = row.correctsEventId,
                isCorrection = row.correctsEventId != null,
                isSuperseded = row.superseded,
                notes = row.notes,
            )
        }

        val canonicalBalance = database.readModelDao().accountBalances()
            .firstOrNull { it.accountId == accountId }
            ?.balance ?: 0.0

        return AccountLedger(
            accountId = account.accountId,
            accountName = account.name,
            displayName = displayName(account.name, account.category),
            category = account.category,
            currentBalance = canonicalBalance,
            entries = entries,
        )
    }

    private fun displayName(accountName: String, category: String): String = when (category) {
        "Customer" -> accountName.removePrefix("Customer: ").trim()
        "Vendor" -> accountName.removePrefix("Vendor: ").trim()
        else -> accountName
    }

    private companion object {
        val SUPPORTED_CATEGORIES = setOf("Customer", "Vendor")
    }
}
