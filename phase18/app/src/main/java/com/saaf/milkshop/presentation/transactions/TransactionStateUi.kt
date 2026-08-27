package com.saaf.milkshop.presentation.transactions

import android.app.Activity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import com.saaf.milkshop.R
import com.saaf.milkshop.data.TransactionReferenceDataRepository
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.presentation.management.PeopleAccountsUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Presentation-layer state, read-side loading, and derived UI state for the
 * transaction screens. Existing domain/data behavior is intentionally reused
 * unchanged; this class only moves transaction presentation orchestration out
 * of MainActivity.
 */
class TransactionStateUi(
    private val activity: Activity,
    private val referenceDataRepository: TransactionReferenceDataRepository,
    private val purchaseViewModel: PurchaseViewModel,
    private val saleViewModel: SaleViewModel,
    private val simpleTransactionsViewModel: SimpleTransactionsViewModel,
    private val peopleAccountsUi: PeopleAccountsUi,
    private val createSpinnerAdapter: (List<String>) -> ArrayAdapter<String>,
    private val clearPurchaseMessageIfEditing: () -> Unit,
) {
    private val vendorSpinner: Spinner by lazy { activity.findViewById(R.id.vendorSpinner) }
    private val rawLitersInput: EditText by lazy { activity.findViewById(R.id.rawLitersInput) }
    private val fatInput: EditText by lazy { activity.findViewById(R.id.fatInput) }
    private val lrInput: EditText by lazy { activity.findViewById(R.id.lrInput) }
    private val snfText: TextView by lazy { activity.findViewById(R.id.snfText) }
    private val tsText: TextView by lazy { activity.findViewById(R.id.tsText) }
    private val adjustedLitersText: TextView by lazy { activity.findViewById(R.id.adjustedLitersText) }
    private val amountOwedText: TextView by lazy { activity.findViewById(R.id.amountOwedText) }
    private val savePurchaseButton: Button by lazy { activity.findViewById(R.id.savePurchaseButton) }

    private val vendorPaymentSpinner: Spinner by lazy { activity.findViewById(R.id.vendorPaymentSpinner) }
    private val paymentAccountSpinner: Spinner by lazy { activity.findViewById(R.id.paymentAccountSpinner) }
    private val saveVendorPaymentButton: Button by lazy { activity.findViewById(R.id.saveVendorPaymentButton) }

    private val saleProductSpinner: Spinner by lazy { activity.findViewById(R.id.saleProductSpinner) }
    private val saleQuantityRadioButton: RadioButton by lazy { activity.findViewById(R.id.saleQuantityRadioButton) }
    private val saleQuantityInput: EditText by lazy { activity.findViewById(R.id.saleQuantityInput) }
    private val saleValueInput: EditText by lazy { activity.findViewById(R.id.saleValueInput) }
    private val saleRateInput: EditText by lazy { activity.findViewById(R.id.saleRateInput) }
    private val salePaymentModeRadioGroup: RadioGroup by lazy { activity.findViewById(R.id.salePaymentModeRadioGroup) }
    private val saleAccountSpinner: Spinner by lazy { activity.findViewById(R.id.saleAccountSpinner) }
    private val saleCustomerSpinner: Spinner by lazy { activity.findViewById(R.id.saleCustomerSpinner) }
    private val addCustomerButton: Button by lazy { activity.findViewById(R.id.addCustomerButton) }
    private val saleSurchargeCheckbox: CheckBox by lazy { activity.findViewById(R.id.saleSurchargeCheckbox) }
    private val saleSurchargeInput: EditText by lazy { activity.findViewById(R.id.saleSurchargeInput) }
    private val saleCalculationText: TextView by lazy { activity.findViewById(R.id.saleCalculationText) }
    private val saveSaleButton: Button by lazy { activity.findViewById(R.id.saveSaleButton) }

    private val creditCollectionCustomerSpinner: Spinner by lazy { activity.findViewById(R.id.creditCollectionCustomerSpinner) }
    private val creditCollectionAccountSpinner: Spinner by lazy { activity.findViewById(R.id.creditCollectionAccountSpinner) }
    private val expenseAccountSpinner: Spinner by lazy { activity.findViewById(R.id.expenseAccountSpinner) }
    private val saveCreditCollectionButton: Button by lazy { activity.findViewById(R.id.saveCreditCollectionButton) }
    private val saveExpenseButton: Button by lazy { activity.findViewById(R.id.saveExpenseButton) }

    private val salaryEmployeeSpinner: Spinner by lazy { activity.findViewById(R.id.salaryEmployeeSpinner) }
    private val salaryPaymentAccountSpinner: Spinner by lazy { activity.findViewById(R.id.salaryPaymentAccountSpinner) }
    private val saveSalaryButton: Button by lazy { activity.findViewById(R.id.saveSalaryButton) }
    private val addEmployeeButton: Button by lazy { activity.findViewById(R.id.addEmployeeButton) }
    private val expenseCategorySpinner: Spinner by lazy { activity.findViewById(R.id.expenseCategorySpinner) }

    private val vendorDependentSpinners: List<Spinner> by lazy {
        listOf(vendorSpinner, vendorPaymentSpinner)
    }
    private val customerDependentSpinners: List<Spinner> by lazy {
        listOf(saleCustomerSpinner, creditCollectionCustomerSpinner)
    }

    private var vendors: List<VendorOption> = emptyList()
    private var paymentAccounts: List<PaymentAccountOption> = emptyList()
    private var products: List<ProductOption> = emptyList()
    private var customers: List<CustomerOption> = emptyList()
    private var salaryEmployees: List<EmployeeOption> = emptyList()
    private var saleSettings = SaleSettings(null, null, null)

    suspend fun loadAll() {
        loadPurchaseSettings()
        loadVendors()
        loadPaymentAccounts()
        loadExpenseData()
        loadSalaryData()
        loadSaleData()
    }

    suspend fun reloadVendors() = loadVendors()

    suspend fun reloadSalaryData() = loadSalaryData()

    suspend fun reloadSaleData() = loadSaleData()

    suspend fun reloadAfterBusinessSettingsSaved() {
        loadPurchaseSettings()
        loadSaleData()
        updatePurchasePreview()
        updateSelectedProductRate()
        updateSalePreview()
    }

    suspend fun loadPurchaseSettings() {
        val settings = referenceDataRepository.loadPurchaseSettings()
        purchaseViewModel.updateSettings(settings.milkRate, settings.deliveryFee, settings.tsStandard)
    }

    private suspend fun loadVendors() {
        val data = referenceDataRepository.loadVendors()
        vendors = data.accounts.map { VendorOption(it.accountId, it.name) }
        val managedVendors = data.managedVendors

        purchaseViewModel.setVendors(vendors.map { PurchaseViewModel.VendorState(it.accountId, it.name) })
        simpleTransactionsViewModel.setReferenceData(
            vendors = vendors.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            customers = customers.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            paymentAccounts = paymentAccounts.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            employees = salaryEmployees.map { SimpleTransactionsViewModel.EmployeeState(it.accountId, it.name) },
            expenseCategories = ExpenseService.EXPENSE_CATEGORIES,
        )

        withContext(Dispatchers.Main) {
            vendorDependentSpinners.forEach { spinner ->
                spinner.adapter = createSpinnerAdapter(vendors.map { it.name })
                spinner.isEnabled = vendors.isNotEmpty()
            }
            peopleAccountsUi.renderVendors(managedVendors)
            savePurchaseButton.isEnabled = vendors.isNotEmpty()
            saveVendorPaymentButton.isEnabled = vendors.isNotEmpty() && paymentAccounts.isNotEmpty()
        }
    }

    private suspend fun loadPaymentAccounts() {
        paymentAccounts = referenceDataRepository.loadPaymentAccounts()
            .map { PaymentAccountOption(it.accountId, it.name) }

        simpleTransactionsViewModel.setReferenceData(
            vendors = vendors.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            customers = customers.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            paymentAccounts = paymentAccounts.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            employees = salaryEmployees.map { SimpleTransactionsViewModel.EmployeeState(it.accountId, it.name) },
            expenseCategories = ExpenseService.EXPENSE_CATEGORIES,
        )

        withContext(Dispatchers.Main) {
            paymentAccountSpinner.adapter = createSpinnerAdapter(paymentAccounts.map { it.name })
            paymentAccountSpinner.isEnabled = paymentAccounts.isNotEmpty()
            creditCollectionAccountSpinner.adapter = createSpinnerAdapter(paymentAccounts.map { it.name })
            creditCollectionAccountSpinner.isEnabled = paymentAccounts.isNotEmpty()
            expenseAccountSpinner.adapter = createSpinnerAdapter(paymentAccounts.map { it.name })
            expenseAccountSpinner.isEnabled = paymentAccounts.isNotEmpty()
            salaryPaymentAccountSpinner.adapter = createSpinnerAdapter(paymentAccounts.map { it.name })
            salaryPaymentAccountSpinner.isEnabled = paymentAccounts.isNotEmpty()
            saveVendorPaymentButton.isEnabled = vendors.isNotEmpty() && paymentAccounts.isNotEmpty()
            updateCreditCollectionActionState()
            updateExpenseActionState()
        }
    }

    private suspend fun loadExpenseData() {
        withContext(Dispatchers.Main) {
            expenseCategorySpinner.adapter = createSpinnerAdapter(ExpenseService.EXPENSE_CATEGORIES)
            expenseCategorySpinner.isEnabled = ExpenseService.EXPENSE_CATEGORIES.isNotEmpty()
            updateExpenseActionState()
            updateSalaryActionState()
        }
    }

    private suspend fun loadSalaryData() {
        val data = referenceDataRepository.loadEmployees()
        salaryEmployees = data.accounts.map { EmployeeOption(it.accountId, it.name) }
        simpleTransactionsViewModel.setReferenceData(
            vendors = vendors.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            customers = customers.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            paymentAccounts = paymentAccounts.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            employees = salaryEmployees.map { SimpleTransactionsViewModel.EmployeeState(it.accountId, it.name) },
            expenseCategories = ExpenseService.EXPENSE_CATEGORIES,
        )
        val managedEmployees = data.managedEmployees

        withContext(Dispatchers.Main) {
            salaryEmployeeSpinner.adapter = createSpinnerAdapter(salaryEmployees.map { it.name })
            salaryEmployeeSpinner.isEnabled = salaryEmployees.isNotEmpty()
            addEmployeeButton.isEnabled = true
            peopleAccountsUi.renderEmployees(managedEmployees)
            updateSalaryActionState()
        }
    }

    private suspend fun loadSaleData() {
        val data = referenceDataRepository.loadSaleReferenceData()
        products = data.products.map { ProductOption(it.productId, it.name, it.unit) }
        customers = data.customers.map { CustomerOption(it.accountId, it.name) }
        val milkRate = data.settings.milkRate
        val yogurtRate = data.settings.yogurtRate
        val defaultSurcharge = data.settings.defaultSurcharge
        saleSettings = SaleSettings(milkRate, yogurtRate, defaultSurcharge)

        val managedCustomers = data.managedCustomers
        simpleTransactionsViewModel.setReferenceData(
            vendors = vendors.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            customers = customers.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            paymentAccounts = paymentAccounts.map { SimpleTransactionsViewModel.AccountState(it.accountId, it.name) },
            employees = salaryEmployees.map { SimpleTransactionsViewModel.EmployeeState(it.accountId, it.name) },
            expenseCategories = ExpenseService.EXPENSE_CATEGORIES,
        )
        saleViewModel.setReferenceData(
            products = products.map { SaleViewModel.ProductState(it.productId, it.name, it.unit) },
            customers = customers.map { SaleViewModel.CustomerState(it.accountId, it.name) },
            paymentAccounts = paymentAccounts.map { SaleViewModel.PaymentAccountState(it.accountId, it.name) },
            settings = SaleViewModel.Settings(milkRate, yogurtRate, defaultSurcharge),
        )

        withContext(Dispatchers.Main) {
            peopleAccountsUi.renderCustomers(managedCustomers)
            saleProductSpinner.adapter = createSpinnerAdapter(products.map { it.name })
            activity.findViewById<Spinner>(R.id.saleChannelSpinner).adapter = createSpinnerAdapter(
                listOf("retail", "wholesale", "credit-retail", "credit-wholesale")
            )
            saleAccountSpinner.adapter = createSpinnerAdapter(paymentAccounts.map { it.name })
            val customerAdapter = createSpinnerAdapter(customers.map { it.name })
            customerDependentSpinners.forEach { spinner ->
                spinner.adapter = customerAdapter
                spinner.isEnabled = customers.isNotEmpty()
            }
            saleViewModel.setSelectionsFromUi(
                saleProductSpinner.selectedItemPosition,
                saleCustomerSpinner.selectedItemPosition,
                saleAccountSpinner.selectedItemPosition,
            )
            updateCreditCollectionActionState()
            saleSurchargeInput.setText(defaultSurcharge?.let { String.format(Locale.US, "%.2f", it) } ?: "")
            updateSelectedProductRate()
            updateSaleInputMode()
            updateSalePaymentMode()
            updateSaleActionState()
            updateSalePreview()
        }
    }

    fun selectedVendorAccountId(): Long? {
        // Synchronize the ViewModel from the current UI selection at the transaction boundary.
        // This preserves the ViewModel as the owner of the selected value while ensuring
        // programmatic Spinner changes are reflected before a save operation reads it.
        purchaseViewModel.selectVendor(vendorSpinner.selectedItemPosition)
        return purchaseViewModel.selectedVendorAccountId()
    }

    fun selectedVendorPaymentVendorAccountId(): Long? {
        simpleTransactionsViewModel.setSelections(
            vendorIndex = vendorPaymentSpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedVendorAccountId()
    }

    fun selectedCustomerAccountId(): Long? {
        simpleTransactionsViewModel.setSelections(
            customerIndex = creditCollectionCustomerSpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedCustomerAccountId()
    }

    fun selectedSaleCustomerAccountId(): Long? {
        saleViewModel.setSelections(
            productIndex = saleProductSpinner.selectedItemPosition,
            customerIndex = saleCustomerSpinner.selectedItemPosition,
            paymentAccountIndex = saleAccountSpinner.selectedItemPosition,
        )
        return saleViewModel.selectedCustomerAccountId()
    }

    fun selectedSalePaymentAccountId(): Long? {
        saleViewModel.setSelections(
            productIndex = saleProductSpinner.selectedItemPosition,
            customerIndex = saleCustomerSpinner.selectedItemPosition,
            paymentAccountIndex = saleAccountSpinner.selectedItemPosition,
        )
        return saleViewModel.selectedPaymentAccountId()
    }

    fun selectedPaymentAccountForVendorPayment(): Long? {
        simpleTransactionsViewModel.setSelections(
            vendorPaymentAccountIndex = paymentAccountSpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedVendorPaymentAccountId()
    }

    fun selectedPaymentAccountForCreditCollection(): Long? {
        simpleTransactionsViewModel.setSelections(
            creditCollectionPaymentAccountIndex = creditCollectionAccountSpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedCreditCollectionPaymentAccountId()
    }

    fun selectedPaymentAccountForExpense(): Long? {
        simpleTransactionsViewModel.setSelections(
            expensePaymentAccountIndex = expenseAccountSpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedExpensePaymentAccountId()
    }

    fun selectedPaymentAccountForSalary(): Long? {
        simpleTransactionsViewModel.setSelections(
            salaryPaymentAccountIndex = salaryPaymentAccountSpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedSalaryPaymentAccountId()
    }

    fun selectedEmployeeAccountId(): Long? {
        simpleTransactionsViewModel.setSelections(
            employeeIndex = salaryEmployeeSpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedEmployeeAccountId()
    }

    fun selectedExpenseCategory(): String? {
        simpleTransactionsViewModel.setSelections(
            expenseCategoryIndex = expenseCategorySpinner.selectedItemPosition,
        )
        return simpleTransactionsViewModel.selectedExpenseCategory()
    }

    fun selectedProductId(): Long? {
        saleViewModel.setSelections(
            productIndex = saleProductSpinner.selectedItemPosition,
            customerIndex = saleCustomerSpinner.selectedItemPosition,
            paymentAccountIndex = saleAccountSpinner.selectedItemPosition,
        )
        return saleViewModel.selectedProductId()
    }

    fun vendorsExist(): Boolean = vendors.isNotEmpty()
    fun customersExist(): Boolean = customers.isNotEmpty()
    fun paymentAccountsExist(): Boolean = paymentAccounts.isNotEmpty()
    fun salaryEmployeesExist(): Boolean = salaryEmployees.isNotEmpty()

    fun updatePurchasePreviewAndClearMessage() {
        updatePurchasePreview()
        clearPurchaseMessageIfEditing()
    }

    fun clearProductionError() {
        activity.findViewById<TextView>(R.id.productionErrorText).visibility = View.GONE
    }

    fun clearVendorPaymentError() {
        activity.findViewById<TextView>(R.id.vendorPaymentErrorText).visibility = View.GONE
    }

    fun clearCreditCollectionError() {
        activity.findViewById<TextView>(R.id.creditCollectionErrorText).visibility = View.GONE
    }

    fun clearExpenseError() {
        activity.findViewById<TextView>(R.id.expenseErrorText).visibility = View.GONE
    }

    fun clearSalaryError() {
        activity.findViewById<TextView>(R.id.salaryErrorText).visibility = View.GONE
    }

    fun updateSalePreviewAndClearError() {
        activity.findViewById<TextView>(R.id.saleErrorText).visibility = View.GONE
        updateSalePreview()
    }

    fun onPurchaseVendorSelected(position: Int) {
        purchaseViewModel.selectVendor(position)
    }

    fun onSaleSelectionChanged(productIndex: Int, customerIndex: Int, paymentAccountIndex: Int) {
        saleViewModel.setSelectionsFromUi(productIndex, customerIndex, paymentAccountIndex)
    }

    fun onSaleProductSelected() {
        saleViewModel.setSelections(
            productIndex = saleProductSpinner.selectedItemPosition,
            customerIndex = saleCustomerSpinner.selectedItemPosition,
            paymentAccountIndex = saleAccountSpinner.selectedItemPosition,
        )
        updateSelectedProductRate()
        updateSalePreview()
    }

    fun onSaleInputModeChanged() {
        saleViewModel.setModes(quantityMode = saleQuantityRadioButton.isChecked)
        updateSaleInputMode()
        updateSalePreview()
    }

    fun onSalePaymentModeChanged() {
        saleViewModel.setModes(creditMode = salePaymentModeRadioGroup.checkedRadioButtonId == R.id.saleCreditRadioButton)
        saleViewModel.setSelections(
            productIndex = saleProductSpinner.selectedItemPosition,
            customerIndex = saleCustomerSpinner.selectedItemPosition,
            paymentAccountIndex = saleAccountSpinner.selectedItemPosition,
        )
        updateSalePaymentMode()
        updateSaleActionState()
    }

    fun onSaleSurchargeChanged(enabled: Boolean) {
        saleViewModel.setModes(surchargeEnabled = enabled)
        saleSurchargeInput.isEnabled = enabled
        updateSalePreview()
    }

    fun updateSelectedProductRate() {
        val product = products.getOrNull(saleProductSpinner.selectedItemPosition) ?: return
        val rate = when (product.name) {
            "Milk" -> saleSettings.milkRate
            "Yogurt" -> saleSettings.yogurtRate
            else -> null
        }
        if (rate != null) {
            saleRateInput.setText(String.format(Locale.US, "%.2f", rate))
        }
    }

    fun updateSaleInputMode() {
        val quantityMode = saleQuantityRadioButton.isChecked
        saleQuantityInput.isEnabled = quantityMode
        saleValueInput.isEnabled = !quantityMode
        if (quantityMode) {
            saleValueInput.text.clear()
        } else {
            saleQuantityInput.text.clear()
        }
    }

    fun updateSalePaymentMode() {
        val credit = salePaymentModeRadioGroup.checkedRadioButtonId == R.id.saleCreditRadioButton
        saleAccountSpinner.isEnabled = !credit
        saleCustomerSpinner.isEnabled = credit && customers.isNotEmpty()
        addCustomerButton.isEnabled = credit
    }

    fun updateSaleActionState() {
        val hasProduct = products.isNotEmpty()
        val credit = salePaymentModeRadioGroup.checkedRadioButtonId == R.id.saleCreditRadioButton
        val hasTarget = if (credit) customers.isNotEmpty() else paymentAccounts.isNotEmpty()
        saveSaleButton.isEnabled = hasProduct && hasTarget
    }

    fun updateSalePreview() {
        saleViewModel.setInputs(
            quantity = saleQuantityInput.text.toString(),
            value = saleValueInput.text.toString(),
            rate = saleRateInput.text.toString(),
            surcharge = saleSurchargeInput.text.toString(),
        )
        val rate = saleRateInput.text.toString().toDoubleOrNull()
        if (rate == null || rate <= 0.0) {
            saleCalculationText.text = activity.getString(R.string.sale_calculation_placeholder)
            return
        }

        val preview = if (saleQuantityRadioButton.isChecked) {
            saleQuantityInput.text.toString().toDoubleOrNull()?.takeIf { it > 0.0 }?.let {
                "Value: ${formatMoney(it * rate)}"
            }
        } else {
            saleValueInput.text.toString().toDoubleOrNull()?.takeIf { it > 0.0 }?.let {
                val unit = products.getOrNull(saleProductSpinner.selectedItemPosition)?.unit.orEmpty()
                "Quantity: ${formatQuantity(it / rate, unit)}"
            }
        }

        val surcharge = if (saleSurchargeCheckbox.isChecked) {
            saleSurchargeInput.text.toString().toDoubleOrNull()
        } else 0.0

        val display = buildString {
            append(preview ?: activity.getString(R.string.sale_calculation_placeholder))
            if (surcharge != null && surcharge >= 0.0 && saleSurchargeCheckbox.isChecked) {
                append("\nSurcharge: ${formatMoney(surcharge)}")
                val baseValue = if (saleQuantityRadioButton.isChecked) {
                    saleQuantityInput.text.toString().toDoubleOrNull()?.takeIf { it > 0.0 }?.times(rate)
                } else {
                    saleValueInput.text.toString().toDoubleOrNull()?.takeIf { it > 0.0 }
                }
                if (baseValue != null) append("\nTotal: ${formatMoney(baseValue + surcharge)}")
            }
        }
        saleViewModel.setPreview(display)
        saleCalculationText.text = display
    }

    fun updateCreditCollectionActionState() {
        saveCreditCollectionButton.isEnabled = customers.isNotEmpty() && paymentAccounts.isNotEmpty()
    }

    fun updateExpenseActionState() {
        saveExpenseButton.isEnabled = ExpenseService.EXPENSE_CATEGORIES.isNotEmpty() && paymentAccounts.isNotEmpty()
    }

    fun updateSalaryActionState() {
        saveSalaryButton.isEnabled = salaryEmployees.isNotEmpty() && paymentAccounts.isNotEmpty()
    }

    fun updatePurchasePreview() {
        purchaseViewModel.updateInputs(
            rawLitersText = rawLitersInput.text.toString(),
            fatText = fatInput.text.toString(),
            lrText = lrInput.text.toString(),
        )
        val result = purchaseViewModel.preview()
        if (result == null) {
            clearPurchasePreview()
            return
        }

        snfText.text = activity.getString(R.string.purchase_snf_format, result.snfPercentage)
        tsText.text = activity.getString(R.string.purchase_ts_format, result.tsPercentage)
        adjustedLitersText.text = activity.getString(
            R.string.purchase_adjusted_format,
            result.adjustedLiters,
        )
        amountOwedText.text = activity.getString(R.string.purchase_amount_format, result.amountOwed)
    }

    fun clearPurchaseInputs() {
        rawLitersInput.text.clear()
        fatInput.text.clear()
        lrInput.text.clear()
        purchaseViewModel.clearDraft()
        clearPurchasePreview()
    }

    private fun clearPurchasePreview() {
        snfText.text = activity.getString(R.string.purchase_snf_placeholder)
        tsText.text = activity.getString(R.string.purchase_ts_placeholder)
        adjustedLitersText.text = activity.getString(R.string.purchase_adjusted_placeholder)
        amountOwedText.text = activity.getString(R.string.purchase_amount_placeholder)
    }

    private fun formatQuantity(value: Double, unit: String): String =
        String.format(Locale.US, "%.2f %s", value, unit)

    private fun formatMoney(value: Double): String =
        String.format(Locale.US, "₨ %.2f", value)

    private data class VendorOption(val accountId: Long, val name: String)
    private data class PaymentAccountOption(val accountId: Long, val name: String)
    private data class ProductOption(val productId: Long, val name: String, val unit: String)
    private data class CustomerOption(val accountId: Long, val name: String)
    private data class EmployeeOption(val accountId: Long, val name: String)
    private data class SaleSettings(val milkRate: Double?, val yogurtRate: Double?, val defaultSurcharge: Double?)
}
