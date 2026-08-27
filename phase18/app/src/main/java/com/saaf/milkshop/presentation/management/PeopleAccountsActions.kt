package com.saaf.milkshop.presentation.management

import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.R
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.data.ManagementDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.saaf.milkshop.domain.AppPermission

/**
 * Application-facing orchestration for People & Accounts management actions.
 * Existing service calls and refresh ordering are preserved exactly.
 */
class PeopleAccountsActions(
    private val context: android.content.Context,
    private val repository: ManagementDataRepository,
    private val uiScope: CoroutineScope,
    private val reloadVendors: suspend () -> Unit,
    private val reloadSalaryData: suspend () -> Unit,
    private val reloadSaleData: suspend () -> Unit,
    private val refreshDashboard: suspend () -> Unit,
    private val showVendorError: (String) -> Unit,
    private val clearVendorError: () -> Unit,
    private val showCustomerError: (String) -> Unit,
    private val clearCustomerError: () -> Unit,
    private val showEmployeeError: (String) -> Unit,
    private val clearEmployeeError: () -> Unit,
    private val authorize: suspend (AppPermission) -> Unit,
) {
    fun createVendor(name: String) {
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_MASTER_DATA)
                repository.createVendor(name)
                clearVendorError()
                reloadVendors()
            } catch (_: Exception) {
                showVendorError(context.getString(R.string.vendor_create_failed))
            }
        }
    }

    fun renameVendor(vendor: VendorManagementService.VendorInfo, name: String) {
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_MASTER_DATA)
                repository.renameVendor(vendor.accountId, name)
                clearVendorError()
                reloadVendors()
                refreshDashboard()
            } catch (_: Exception) {
                showVendorError(context.getString(R.string.vendor_rename_failed))
            }
        }
    }

    fun createCustomer(name: String) {
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_MASTER_DATA)
                repository.createCustomer(name)
                clearCustomerError()
                reloadSaleData()
                refreshDashboard()
            } catch (_: Exception) {
                showCustomerError(context.getString(R.string.customer_management_save_failed))
            }
        }
    }

    fun renameCustomer(customer: CustomerManagementService.CustomerInfo, name: String) {
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_MASTER_DATA)
                repository.renameCustomer(customer.accountId, name)
                clearCustomerError()
                reloadSaleData()
                refreshDashboard()
            } catch (_: Exception) {
                showCustomerError(context.getString(R.string.customer_management_save_failed))
            }
        }
    }

    fun createEmployee(name: String) {
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_MASTER_DATA)
                repository.createEmployee(name)
                clearEmployeeError()
                reloadSalaryData()
                refreshDashboard()
            } catch (_: Exception) {
                showEmployeeError(context.getString(R.string.employee_management_save_failed))
            }
        }
    }

    fun renameEmployee(employee: EmployeeManagementService.EmployeeInfo, name: String) {
        uiScope.launch {
            try {
                authorize(AppPermission.MANAGE_MASTER_DATA)
                repository.renameEmployee(employee.accountId, name)
                clearEmployeeError()
                reloadSalaryData()
                refreshDashboard()
            } catch (_: Exception) {
                showEmployeeError(context.getString(R.string.employee_management_save_failed))
            }
        }
    }
}
