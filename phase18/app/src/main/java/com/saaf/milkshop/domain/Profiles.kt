package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.CustomerProfileEntity
import com.saaf.milkshop.data.EmployeeProfileEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.VendorProfileEntity
import java.time.Instant
import java.time.LocalDate

data class CustomerProfileInput(
    val phone: String? = null,
    val areaRoute: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val creditLimit: Double? = null,
    val dailyQuota: Double? = null,
    val active: Boolean = true,
)

class CustomerProfileService(private val database: MilkShopDatabase) {
    suspend fun get(accountId: Long): CustomerProfileEntity? =
        database.customerProfileDao().findByAccountId(accountId)

    suspend fun save(accountId: Long, input: CustomerProfileInput) = database.withWriteTransaction {
        requireAccountCategory(database, accountId, "Customer")
        require(input.creditLimit == null || input.creditLimit >= 0.0) { "Credit limit cannot be negative" }
        require(input.dailyQuota == null || input.dailyQuota >= 0.0) { "Daily quota cannot be negative" }
        val existing = database.customerProfileDao().findByAccountId(accountId)
        database.customerProfileDao().save(
            CustomerProfileEntity(
                accountId = accountId,
                phone = input.phone.cleanOptional(),
                areaRoute = input.areaRoute.cleanOptional(),
                address = input.address.cleanOptional(),
                notes = input.notes.cleanOptional(),
                creditLimit = input.creditLimit,
                dailyQuota = input.dailyQuota,
                active = input.active,
                createdAt = existing?.createdAt ?: Instant.now().toString(),
            )
        )
    }
}

data class VendorProfileInput(
    val phone: String? = null,
    val location: String? = null,
    val notes: String? = null,
    val active: Boolean = true,
)

class VendorProfileService(private val database: MilkShopDatabase) {
    suspend fun get(accountId: Long): VendorProfileEntity? =
        database.vendorProfileDao().findByAccountId(accountId)

    suspend fun save(accountId: Long, input: VendorProfileInput) = database.withWriteTransaction {
        requireAccountCategory(database, accountId, "Vendor")
        val existing = database.vendorProfileDao().findByAccountId(accountId)
        database.vendorProfileDao().save(
            VendorProfileEntity(
                accountId = accountId,
                phone = input.phone.cleanOptional(),
                location = input.location.cleanOptional(),
                notes = input.notes.cleanOptional(),
                active = input.active,
                createdAt = existing?.createdAt ?: Instant.now().toString(),
            )
        )
    }
}

data class EmployeeProfileInput(
    val role: String? = null,
    val phone: String? = null,
    val referenceSalary: Double? = null,
    val salaryPeriod: String? = null,
    val joinDate: String? = null,
    val active: Boolean = true,
)

class EmployeeProfileService(private val database: MilkShopDatabase) {
    suspend fun get(accountId: Long): EmployeeProfileEntity? =
        database.employeeProfileDao().findByAccountId(accountId)

    suspend fun save(accountId: Long, input: EmployeeProfileInput) = database.withWriteTransaction {
        requireAccountCategory(database, accountId, "Salary")
        require(input.referenceSalary == null || input.referenceSalary >= 0.0) {
            "Reference salary cannot be negative"
        }
        input.joinDate.cleanOptional()?.let(LocalDate::parse)
        val existing = database.employeeProfileDao().findByAccountId(accountId)
        database.employeeProfileDao().save(
            EmployeeProfileEntity(
                accountId = accountId,
                role = input.role.cleanOptional(),
                phone = input.phone.cleanOptional(),
                referenceSalary = input.referenceSalary,
                salaryPeriod = input.salaryPeriod.cleanOptional(),
                joinDate = input.joinDate.cleanOptional(),
                active = input.active,
                createdAt = existing?.createdAt ?: Instant.now().toString(),
            )
        )
    }
}

internal suspend fun requireAccountCategory(
    database: MilkShopDatabase,
    accountId: Long,
    category: String,
) = database.accountDao().findById(accountId)?.also {
    require(it.category == category) { "Account $accountId is not in category $category" }
} ?: error("Account $accountId does not exist")

/**
 * Legacy databases and tests may contain categorized accounts created before
 * profile tables existed. Those remain active unless a profile explicitly
 * marks them inactive.
 */
internal suspend fun requireActiveProfile(
    database: MilkShopDatabase,
    accountId: Long,
    category: String,
) = requireAccountCategory(database, accountId, category).also {
    val active = when (category) {
        "Customer" -> database.customerProfileDao().findByAccountId(accountId)?.active
        "Vendor" -> database.vendorProfileDao().findByAccountId(accountId)?.active
        "Salary" -> database.employeeProfileDao().findByAccountId(accountId)?.active
        else -> null
    }
    require(active != false) { "$category account $accountId is inactive" }
}
