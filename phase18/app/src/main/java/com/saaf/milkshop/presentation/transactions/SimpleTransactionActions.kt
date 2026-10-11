package com.saaf.milkshop.presentation.transactions

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.saaf.milkshop.R
import com.saaf.milkshop.application.transactions.TransactionOperationRepository
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.SalaryPaymentInput
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.AppPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Presentation/application boundary for the five simpler transaction-save operations.
 *
 * Operation semantics are intentionally unchanged. This class only moves the
 * existing handlers out of MainActivity so the transaction presentation layer
 * can become independently replaceable later.
 */
class SimpleTransactionActions(
    private val activity: Activity,
    private val transactionOperations: TransactionOperationRepository,
    private val uiScope: CoroutineScope,
    private val selectedVendorAccountId: () -> Long?,
    private val vendorsExist: () -> Boolean,
    private val selectedCustomerAccountId: () -> Long?,
    private val customersExist: () -> Boolean,
    private val selectedPaymentAccountForVendorPayment: () -> Long?,
    private val selectedPaymentAccountForCreditCollection: () -> Long?,
    private val selectedPaymentAccountForExpense: () -> Long?,
    private val selectedPaymentAccountForSalary: () -> Long?,
    private val selectedExpenseCategory: () -> String?,
    private val paymentAccountsExist: () -> Boolean,
    private val selectedEmployeeAccountId: () -> Long?,
    private val salaryEmployeesExist: () -> Boolean,
    private val showCreditCollectionError: (String) -> Unit,
    private val userMessageForCreditCollection: (Exception) -> String,
    private val updateCreditCollectionActionState: () -> Unit,
    private val showExpenseError: (String) -> Unit,
    private val userMessageForExpense: (Exception) -> String,
    private val showSalaryError: (String) -> Unit,
    private val userMessageForSalary: (Exception) -> String,
    private val updateSalaryActionState: () -> Unit,
    private val showVendorPaymentError: (String) -> Unit,
    private val userMessageForVendorPayment: (Exception) -> String,
    private val showProductionError: (String) -> Unit,
    private val userMessageForProduction: (Exception) -> String,
    private val queueAutomaticLocalBackup: () -> Unit,
    private val refreshDashboard: suspend () -> Unit,
    private val refreshTransactionHistory: suspend () -> Unit,
    private val authorize: suspend (AppPermission) -> Unit,
) {
    private val productionErrorText: TextView by lazy { activity.findViewById(R.id.productionErrorText) }
    private val productionStatusText: TextView by lazy { activity.findViewById(R.id.productionStatusText) }
    private val milkUsedInput: EditText by lazy { activity.findViewById(R.id.milkUsedInput) }
    private val yogurtProducedInput: EditText by lazy { activity.findViewById(R.id.yogurtProducedInput) }
    private val saveProductionButton: Button by lazy { activity.findViewById(R.id.saveProductionButton) }

    private val vendorPaymentSpinner: Spinner by lazy { activity.findViewById(R.id.vendorPaymentSpinner) }
    private val paymentAccountSpinner: Spinner by lazy { activity.findViewById(R.id.paymentAccountSpinner) }
    private val paymentAmountInput: EditText by lazy { activity.findViewById(R.id.paymentAmountInput) }
    private val vendorPaymentErrorText: TextView by lazy { activity.findViewById(R.id.vendorPaymentErrorText) }
    private val vendorPaymentStatusText: TextView by lazy { activity.findViewById(R.id.vendorPaymentStatusText) }
    private val saveVendorPaymentButton: Button by lazy { activity.findViewById(R.id.saveVendorPaymentButton) }

    private val creditCollectionCustomerSpinner: Spinner by lazy { activity.findViewById(R.id.creditCollectionCustomerSpinner) }
    private val creditCollectionAccountSpinner: Spinner by lazy { activity.findViewById(R.id.creditCollectionAccountSpinner) }
    private val creditCollectionAmountInput: EditText by lazy { activity.findViewById(R.id.creditCollectionAmountInput) }
    private val creditCollectionErrorText: TextView by lazy { activity.findViewById(R.id.creditCollectionErrorText) }
    private val creditCollectionStatusText: TextView by lazy { activity.findViewById(R.id.creditCollectionStatusText) }
    private val saveCreditCollectionButton: Button by lazy { activity.findViewById(R.id.saveCreditCollectionButton) }

    private val expenseCategorySpinner: Spinner by lazy { activity.findViewById(R.id.expenseCategorySpinner) }
    private val expenseAccountSpinner: Spinner by lazy { activity.findViewById(R.id.expenseAccountSpinner) }
    private val expenseAmountInput: EditText by lazy { activity.findViewById(R.id.expenseAmountInput) }
    private val expenseErrorText: TextView by lazy { activity.findViewById(R.id.expenseErrorText) }
    private val expenseStatusText: TextView by lazy { activity.findViewById(R.id.expenseStatusText) }
    private val saveExpenseButton: Button by lazy { activity.findViewById(R.id.saveExpenseButton) }

    private val salaryEmployeeSpinner: Spinner by lazy { activity.findViewById(R.id.salaryEmployeeSpinner) }
    private val salaryPaymentAccountSpinner: Spinner by lazy { activity.findViewById(R.id.salaryPaymentAccountSpinner) }
    private val salaryAmountInput: EditText by lazy { activity.findViewById(R.id.salaryAmountInput) }
    private val salaryErrorText: TextView by lazy { activity.findViewById(R.id.salaryErrorText) }
    private val salaryStatusText: TextView by lazy { activity.findViewById(R.id.salaryStatusText) }
    private val saveSalaryButton: Button by lazy { activity.findViewById(R.id.saveSalaryButton) }

    fun saveProduction() {
        productionErrorText.visibility = View.GONE

        val milkUsed = milkUsedInput.text.toString().toDoubleOrNull()
        val yogurtProduced = yogurtProducedInput.text.toString().toDoubleOrNull()

        when {
            milkUsed == null -> {
                showProductionError(activity.getString(R.string.production_error_milk_used))
                return
            }
            milkUsed <= 0.0 -> {
                showProductionError(activity.getString(R.string.production_error_milk_used_positive))
                return
            }
            yogurtProduced == null -> {
                showProductionError(activity.getString(R.string.production_error_yogurt_produced))
                return
            }
            yogurtProduced <= 0.0 -> {
                showProductionError(activity.getString(R.string.production_error_yogurt_produced_positive))
                return
            }
        }

        saveProductionButton.isEnabled = false
        productionStatusText.text = activity.getString(R.string.production_saving)

        uiScope.launch {
            try {
                authorize(AppPermission.PRODUCTION_AND_SPOILAGE)
                val result = transactionOperations.recordProduction(
                    YogurtProductionInput(
                        occurredAt = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                        milkLitersUsed = milkUsed!!,
                        yogurtKgProduced = yogurtProduced!!,
                    )
                )

                queueAutomaticLocalBackup()
                refreshDashboard()
                refreshTransactionHistory()
                withContext(Dispatchers.Main) {
                    productionStatusText.text = activity.getString(
                        R.string.production_saved,
                        result.eventId,
                    )
                    productionErrorText.visibility = View.GONE
                    saveProductionButton.isEnabled = true
                    milkUsedInput.text.clear()
                    yogurtProducedInput.text.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    productionStatusText.text = ""
                    showProductionError(userMessageForProduction(e))
                    saveProductionButton.isEnabled = true
                }
            }
        }
    }

    fun saveVendorPayment() {
        vendorPaymentErrorText.visibility = View.GONE

        val vendorAccountId = selectedVendorAccountId()
        val paymentAccountId = selectedPaymentAccountForVendorPayment()
        val amount = paymentAmountInput.text.toString().toDoubleOrNull()

        when {
            vendorAccountId == null -> {
                showVendorPaymentError(activity.getString(R.string.vendor_payment_error_no_vendor))
                return
            }
            paymentAccountId == null -> {
                showVendorPaymentError(activity.getString(R.string.vendor_payment_error_no_payment_account))
                return
            }
            amount == null -> {
                showVendorPaymentError(activity.getString(R.string.vendor_payment_error_amount))
                return
            }
            amount <= 0.0 -> {
                showVendorPaymentError(activity.getString(R.string.vendor_payment_error_amount_positive))
                return
            }
        }

        saveVendorPaymentButton.isEnabled = false
        vendorPaymentStatusText.text = activity.getString(R.string.vendor_payment_saving)

        uiScope.launch {
            try {
                authorize(AppPermission.SUPPLIER_PAYMENT)
                val result = transactionOperations.recordVendorPayment(
                    VendorPaymentInput(
                        vendorAccountId = vendorAccountId,
                        paymentAccountId = paymentAccountId,
                        amount = amount!!,
                        occurredAt = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                    )
                )

                queueAutomaticLocalBackup()
                refreshDashboard()
                refreshTransactionHistory()
                withContext(Dispatchers.Main) {
                    vendorPaymentStatusText.text = activity.getString(
                        R.string.vendor_payment_saved,
                        result.eventId,
                    )
                    vendorPaymentErrorText.visibility = View.GONE
                    saveVendorPaymentButton.isEnabled = vendorsExist() && paymentAccountsExist()
                    paymentAmountInput.text.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    vendorPaymentStatusText.text = ""
                    showVendorPaymentError(userMessageForVendorPayment(e))
                    saveVendorPaymentButton.isEnabled = vendorsExist() && paymentAccountsExist()
                }
            }
        }
    }

    fun saveCreditCollection() {
        creditCollectionErrorText.visibility = View.GONE

        val customerAccountId = selectedCustomerAccountId()
        val paymentAccountId = selectedPaymentAccountForCreditCollection()
        val amount = creditCollectionAmountInput.text.toString().toDoubleOrNull()

        when {
            customerAccountId == null -> {
                showCreditCollectionError(activity.getString(R.string.credit_collection_error_no_customer))
                return
            }
            paymentAccountId == null -> {
                showCreditCollectionError(activity.getString(R.string.credit_collection_error_no_payment_account))
                return
            }
            amount == null -> {
                showCreditCollectionError(activity.getString(R.string.credit_collection_error_amount))
                return
            }
            amount <= 0.0 -> {
                showCreditCollectionError(activity.getString(R.string.credit_collection_error_amount_positive))
                return
            }
        }

        saveCreditCollectionButton.isEnabled = false
        creditCollectionStatusText.text = activity.getString(R.string.credit_collection_saving)

        uiScope.launch {
            try {
                authorize(AppPermission.SALE_AND_COLLECTION)
                val result = transactionOperations.recordCreditCollection(
                    CreditCollectionInput(
                        customerAccountId = customerAccountId,
                        paymentAccountId = paymentAccountId,
                        amount = amount!!,
                        occurredAt = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                    )
                )

                queueAutomaticLocalBackup()
                refreshDashboard()
                refreshTransactionHistory()
                withContext(Dispatchers.Main) {
                    creditCollectionStatusText.text = activity.getString(
                        R.string.credit_collection_saved,
                        result.eventId,
                    )
                    creditCollectionErrorText.visibility = View.GONE
                    updateCreditCollectionActionState()
                    creditCollectionAmountInput.text.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    creditCollectionStatusText.text = ""
                    showCreditCollectionError(userMessageForCreditCollection(e))
                    updateCreditCollectionActionState()
                }
            }
        }
    }

    fun saveExpense() {
        expenseErrorText.visibility = View.GONE

        val category = selectedExpenseCategory()
        val paymentAccountId = selectedPaymentAccountForExpense()
        val amount = expenseAmountInput.text.toString().toDoubleOrNull()

        when {
            category == null -> {
                showExpenseError(activity.getString(R.string.expense_error_no_category))
                return
            }
            paymentAccountId == null -> {
                showExpenseError(activity.getString(R.string.expense_error_no_payment_account))
                return
            }
            amount == null -> {
                showExpenseError(activity.getString(R.string.expense_error_amount))
                return
            }
            amount <= 0.0 -> {
                showExpenseError(activity.getString(R.string.expense_error_amount_positive))
                return
            }
        }

        saveExpenseButton.isEnabled = false
        expenseStatusText.text = activity.getString(R.string.expense_saving)

        uiScope.launch {
            try {
                authorize(AppPermission.EXPENSE)
                val result = transactionOperations.recordExpense(
                    ExpenseInput(
                        category = category!!,
                        paymentAccountId = paymentAccountId,
                        amount = amount!!,
                        occurredAt = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                    )
                )

                queueAutomaticLocalBackup()
                refreshDashboard()
                refreshTransactionHistory()
                withContext(Dispatchers.Main) {
                    expenseStatusText.text = activity.getString(
                        R.string.expense_saved,
                        result.eventId,
                    )
                    expenseErrorText.visibility = View.GONE
                    saveExpenseButton.isEnabled = true
                    expenseAmountInput.text.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    expenseStatusText.text = ""
                    showExpenseError(userMessageForExpense(e))
                    saveExpenseButton.isEnabled = paymentAccountsExist()
                }
            }
        }
    }

    fun saveSalaryPayment() {
        salaryErrorText.visibility = View.GONE

        val employeeAccountId = selectedEmployeeAccountId()
        val paymentAccountId = selectedPaymentAccountForSalary()
        val amount = salaryAmountInput.text.toString().toDoubleOrNull()

        when {
            employeeAccountId == null -> {
                showSalaryError(activity.getString(R.string.salary_error_no_employee))
                return
            }
            paymentAccountId == null -> {
                showSalaryError(activity.getString(R.string.salary_error_no_payment_account))
                return
            }
            amount == null -> {
                showSalaryError(activity.getString(R.string.salary_error_amount))
                return
            }
            amount <= 0.0 -> {
                showSalaryError(activity.getString(R.string.salary_error_amount_positive))
                return
            }
        }

        saveSalaryButton.isEnabled = false
        salaryStatusText.text = activity.getString(R.string.salary_saving)

        uiScope.launch {
            try {
                authorize(AppPermission.SALARY_PAYMENT)
                val result = transactionOperations.recordSalaryPayment(
                    SalaryPaymentInput(
                        employeeAccountId = employeeAccountId,
                        paymentAccountId = paymentAccountId,
                        amount = amount!!,
                        occurredAt = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                    )
                )

                queueAutomaticLocalBackup()
                refreshDashboard()
                refreshTransactionHistory()
                withContext(Dispatchers.Main) {
                    salaryStatusText.text = activity.getString(
                        R.string.salary_saved,
                        result.eventId,
                    )
                    salaryErrorText.visibility = View.GONE
                    updateSalaryActionState()
                    salaryAmountInput.text.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    salaryStatusText.text = ""
                    showSalaryError(userMessageForSalary(e))
                    updateSalaryActionState()
                }
            }
        }
    }
}
