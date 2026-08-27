package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.ExpenseCategoryProfileEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant

class ExpenseCategoryManagementService(private val database: MilkShopDatabase) {
    suspend fun list(includeArchived: Boolean = false): List<ExpenseCategoryProfileEntity> =
        database.expenseCategoryProfileDao().all().filter { includeArchived || it.active }

    suspend fun create(name: String): Long = database.withWriteTransaction {
        val displayName = validateName(name)
        require(database.expenseCategoryProfileDao().findByDisplayName(displayName) == null) {
            "Expense category already exists"
        }
        val accountId = database.accountDao().insert(
            AccountEntity(
                name = "Expense: $displayName",
                type = "Expense",
                category = "Operating Expense",
            )
        )
        database.expenseCategoryProfileDao().insert(
            ExpenseCategoryProfileEntity(
                accountId = accountId,
                displayName = displayName,
                createdAt = Instant.now().toString(),
            )
        )
        accountId
    }

    suspend fun setArchived(accountId: Long, archived: Boolean) = database.withWriteTransaction {
        requireAccountCategory(database, accountId, "Operating Expense")
        check(database.expenseCategoryProfileDao().setActive(accountId, !archived) == 1) {
            "Expense category profile $accountId does not exist"
        }
    }

    private fun validateName(name: String): String {
        val clean = name.trim().removePrefix("Expense: ").trim()
        require(clean.isNotEmpty()) { "Enter an expense category" }
        require(!clean.contains('\n')) { "Expense category cannot contain a new line" }
        return clean
    }
}
