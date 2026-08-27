package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.VendorProfileEntity
import java.time.Instant

/**
 * Master-data operations for vendor accounts.
 *
 * Vendor identity is the existing account_id. Renaming a vendor changes only
 * the current account name, so historical events remain attached to the same
 * account and are therefore not rewritten.
 */
class VendorManagementService(
    private val database: MilkShopDatabase,
) {
    data class VendorInfo(
        val accountId: Long,
        val name: String,
        val balance: Double,
    )

    suspend fun list(): List<VendorInfo> {
        val accounts = database.accountDao().findByCategory("Vendor")
        val balances = database.readModelDao().accountBalances()
            .associateBy { it.accountId }

        return accounts.map { account ->
            VendorInfo(
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
                    type = "Liability",
                    category = "Vendor",
                )
            )
            database.vendorProfileDao().save(
                VendorProfileEntity(accountId = accountId, createdAt = Instant.now().toString())
            )
            accountId
        }
    }

    suspend fun rename(accountId: Long, name: String) {
        val cleanName = validateName(name)
        database.withWriteTransaction {
            val account = database.accountDao().findById(accountId)
                ?: error("Vendor account $accountId does not exist")
            require(account.category == "Vendor") { "Account $accountId is not a vendor" }
            check(database.accountDao().updateName(accountId, accountName(cleanName)) == 1) {
                "Vendor account $accountId could not be renamed"
            }
        }
    }

    private fun validateName(name: String): String {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Enter a vendor name." }
        require(!clean.contains("\n")) { "Vendor name cannot contain a new line." }
        return clean.removePrefix("Vendor: ").trim()
    }

    private fun accountName(name: String): String = "Vendor: $name"

    private fun displayName(accountName: String): String =
        accountName.removePrefix("Vendor: ").trim()
}
