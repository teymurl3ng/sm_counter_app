package com.saaf.milkshop.data

import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.VendorManagementService

/**
 * Application-facing boundary for People & Accounts management operations.
 * Existing management services remain authoritative; this class owns their
 * database-backed construction so presentation actions do not touch the data layer.
 */
class ManagementDataRepository(
    database: MilkShopDatabase,
) {
    private val vendorService = VendorManagementService(database)
    private val customerService = CustomerManagementService(database)
    private val employeeService = EmployeeManagementService(database)

    suspend fun createVendor(name: String): Long = vendorService.create(name)
    suspend fun renameVendor(accountId: Long, name: String) = vendorService.rename(accountId, name)

    suspend fun createCustomer(name: String): Long = customerService.create(name)
    suspend fun renameCustomer(accountId: Long, name: String) = customerService.rename(accountId, name)

    suspend fun createEmployee(name: String): Long = employeeService.create(name)
    suspend fun renameEmployee(accountId: Long, name: String) = employeeService.rename(accountId, name)
}
