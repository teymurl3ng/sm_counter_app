package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.CustomerProfileEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant

/**
 * Master-data operations for customer accounts.
 *
 * Customer identity is the existing account_id. Renaming a customer changes
 * only the current account name, so historical events remain attached to the
 * same account and are therefore not rewritten.
 */
class CustomerManagementService(
    private val database: MilkShopDatabase,
) {
    data class CustomerInfo(
        val accountId: Long,
        val name: String,
        val balance: Double,
    )

    suspend fun list(): List<CustomerInfo> {
        val accounts = database.accountDao().findByCategory("Customer")
        val balances = database.readModelDao().accountBalances()
            .associateBy { it.accountId }

        return accounts.map { account ->
            CustomerInfo(
                accountId = account.accountId,
                name = displayName(account.name),
                balance = balances[account.accountId]?.balance ?: 0.0,
            )
        }
    }

    suspend fun create(name: String): Long {
        val cleanName = validateName(name)
        return database.withWriteTransaction {
            val accountId = database.accountDao().insert(
                AccountEntity(
                    name = accountName(cleanName),
                    type = "Asset",
                    category = "Customer",
                )
            )
            database.customerProfileDao().save(
                CustomerProfileEntity(accountId = accountId, createdAt = Instant.now().toString())
            )
            accountId
        }
    }

    suspend fun rename(accountId: Long, name: String) {
        val cleanName = validateName(name)
        database.withWriteTransaction {
            val account = database.accountDao().findById(accountId)
                ?: error("Customer account $accountId does not exist")
            require(account.category == "Customer") { "Account $accountId is not a customer" }
            check(database.accountDao().updateName(accountId, accountName(cleanName)) == 1) {
                "Customer account $accountId could not be renamed"
            }
        }
    }

    private fun validateName(name: String): String {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Enter a customer name." }
        require(!clean.contains("\n")) { "Customer name cannot contain a new line." }
        return clean.removePrefix("Customer: ").trim()
    }

    private fun accountName(name: String): String = "Customer: $name"

    private fun displayName(accountName: String): String =
        accountName.removePrefix("Customer: ").trim()
}
