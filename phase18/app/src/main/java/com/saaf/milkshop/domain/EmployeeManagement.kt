package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.EmployeeProfileEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant

/**
 * Master-data operations for employee salary accounts.
 *
 * Employee identity is the existing account_id. Renaming an employee changes
 * only the current account name, so historical salary events remain attached
 * to the same account and are therefore not rewritten.
 */
class EmployeeManagementService(
    private val database: MilkShopDatabase,
) {
    data class EmployeeInfo(
        val accountId: Long,
        val name: String,
        val balance: Double,
    )

    suspend fun list(): List<EmployeeInfo> {
        val accounts = database.accountDao().findByCategory("Salary")
        val balances = database.readModelDao().accountBalances()
            .associateBy { it.accountId }

        return accounts.map { account ->
            EmployeeInfo(
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
                    type = "Expense",
                    category = "Salary",
                )
            )
            database.employeeProfileDao().save(
                EmployeeProfileEntity(accountId = accountId, createdAt = Instant.now().toString())
            )
            accountId
        }
    }

    suspend fun rename(accountId: Long, name: String) {
        val cleanName = validateName(name)
        database.withWriteTransaction {
            val account = database.accountDao().findById(accountId)
                ?: error("Employee account $accountId does not exist")
            require(account.category == "Salary") { "Account $accountId is not an employee salary account" }
            check(database.accountDao().updateName(accountId, accountName(cleanName)) == 1) {
                "Employee account $accountId could not be renamed"
            }
        }
    }

    private fun validateName(name: String): String {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Enter an employee name." }
        require(!clean.contains("\n")) { "Employee name cannot contain a new line." }
        return clean.removePrefix("Employee: ").trim()
    }

    private fun accountName(name: String): String = "Employee: $name"

    private fun displayName(accountName: String): String =
        accountName.removePrefix("Employee: ").trim()
}
