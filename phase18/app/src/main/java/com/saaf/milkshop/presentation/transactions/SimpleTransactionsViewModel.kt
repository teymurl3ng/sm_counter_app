package com.saaf.milkshop.presentation.transactions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presentation selection state for the simple transaction forms.
 *
 * Business validation, calculations, and persistence remain in the existing
 * transaction services. This ViewModel only owns the currently selected
 * presentation values and their reference lists.
 */
class SimpleTransactionsViewModel : ViewModel() {
    data class AccountState(val accountId: Long, val name: String)
    data class EmployeeState(val accountId: Long, val name: String)

    data class State(
        val vendors: List<AccountState> = emptyList(),
        val customers: List<AccountState> = emptyList(),
        val paymentAccounts: List<AccountState> = emptyList(),
        val employees: List<EmployeeState> = emptyList(),
        val expenseCategories: List<String> = emptyList(),
        val selectedVendorIndex: Int = 0,
        val selectedCustomerIndex: Int = 0,
        val selectedVendorPaymentAccountIndex: Int = 0,
        val selectedCreditCollectionPaymentAccountIndex: Int = 0,
        val selectedExpensePaymentAccountIndex: Int = 0,
        val selectedSalaryPaymentAccountIndex: Int = 0,
        val selectedEmployeeIndex: Int = 0,
        val selectedExpenseCategoryIndex: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _state.asStateFlow()
    val state: State get() = _state.value

    fun setReferenceData(
        vendors: List<AccountState>,
        customers: List<AccountState>,
        paymentAccounts: List<AccountState>,
        employees: List<EmployeeState>,
        expenseCategories: List<String>,
    ) {
        _state.value = state.copy(
            vendors = vendors,
            customers = customers,
            paymentAccounts = paymentAccounts,
            employees = employees,
            expenseCategories = expenseCategories,
            selectedVendorIndex = state.selectedVendorIndex.coerceIn(0, (vendors.size - 1).coerceAtLeast(0)),
            selectedCustomerIndex = state.selectedCustomerIndex.coerceIn(0, (customers.size - 1).coerceAtLeast(0)),
            selectedVendorPaymentAccountIndex = state.selectedVendorPaymentAccountIndex.coerceIn(0, (paymentAccounts.size - 1).coerceAtLeast(0)),
            selectedCreditCollectionPaymentAccountIndex = state.selectedCreditCollectionPaymentAccountIndex.coerceIn(0, (paymentAccounts.size - 1).coerceAtLeast(0)),
            selectedExpensePaymentAccountIndex = state.selectedExpensePaymentAccountIndex.coerceIn(0, (paymentAccounts.size - 1).coerceAtLeast(0)),
            selectedSalaryPaymentAccountIndex = state.selectedSalaryPaymentAccountIndex.coerceIn(0, (paymentAccounts.size - 1).coerceAtLeast(0)),
            selectedEmployeeIndex = state.selectedEmployeeIndex.coerceIn(0, (employees.size - 1).coerceAtLeast(0)),
            selectedExpenseCategoryIndex = state.selectedExpenseCategoryIndex.coerceIn(0, (expenseCategories.size - 1).coerceAtLeast(0)),
        )
    }

    fun setSelections(
        vendorIndex: Int? = null,
        customerIndex: Int? = null,
        vendorPaymentAccountIndex: Int? = null,
        creditCollectionPaymentAccountIndex: Int? = null,
        expensePaymentAccountIndex: Int? = null,
        salaryPaymentAccountIndex: Int? = null,
        employeeIndex: Int? = null,
        expenseCategoryIndex: Int? = null,
    ) {
        val current = state
        _state.value = current.copy(
            selectedVendorIndex = vendorIndex?.coerceAtLeast(0) ?: current.selectedVendorIndex,
            selectedCustomerIndex = customerIndex?.coerceAtLeast(0) ?: current.selectedCustomerIndex,
            selectedVendorPaymentAccountIndex = vendorPaymentAccountIndex?.coerceAtLeast(0) ?: current.selectedVendorPaymentAccountIndex,
            selectedCreditCollectionPaymentAccountIndex = creditCollectionPaymentAccountIndex?.coerceAtLeast(0) ?: current.selectedCreditCollectionPaymentAccountIndex,
            selectedExpensePaymentAccountIndex = expensePaymentAccountIndex?.coerceAtLeast(0) ?: current.selectedExpensePaymentAccountIndex,
            selectedSalaryPaymentAccountIndex = salaryPaymentAccountIndex?.coerceAtLeast(0) ?: current.selectedSalaryPaymentAccountIndex,
            selectedEmployeeIndex = employeeIndex?.coerceAtLeast(0) ?: current.selectedEmployeeIndex,
            selectedExpenseCategoryIndex = expenseCategoryIndex?.coerceAtLeast(0) ?: current.selectedExpenseCategoryIndex,
        )
    }

    fun selectedVendorAccountId(): Long? = state.vendors.getOrNull(state.selectedVendorIndex)?.accountId
    fun selectedCustomerAccountId(): Long? = state.customers.getOrNull(state.selectedCustomerIndex)?.accountId
    fun selectedVendorPaymentAccountId(): Long? = state.paymentAccounts.getOrNull(state.selectedVendorPaymentAccountIndex)?.accountId
    fun selectedCreditCollectionPaymentAccountId(): Long? = state.paymentAccounts.getOrNull(state.selectedCreditCollectionPaymentAccountIndex)?.accountId
    fun selectedExpensePaymentAccountId(): Long? = state.paymentAccounts.getOrNull(state.selectedExpensePaymentAccountIndex)?.accountId
    fun selectedSalaryPaymentAccountId(): Long? = state.paymentAccounts.getOrNull(state.selectedSalaryPaymentAccountIndex)?.accountId
    fun selectedEmployeeAccountId(): Long? = state.employees.getOrNull(state.selectedEmployeeIndex)?.accountId
    fun selectedExpenseCategory(): String? = state.expenseCategories.getOrNull(state.selectedExpenseCategoryIndex)
}
