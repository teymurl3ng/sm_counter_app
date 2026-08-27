package com.saaf.milkshop.presentation.transactions

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import com.saaf.milkshop.R

/**
 * Presentation-layer wiring for the transaction screens.
 *
 * This class owns only transaction UI event wiring and forwards actual save/add
 * operations to the existing MainActivity handlers. No business or persistence
 * behavior lives here.
 */
class TransactionsUi(
    private val activity: Activity,
    private val onSavePurchase: () -> Unit,
    private val onSaveProduction: () -> Unit,
    private val onSaveVendorPayment: () -> Unit,
    private val onSaveSale: () -> Unit,
    private val onAddCustomer: () -> Unit,
    private val onSaveCreditCollection: () -> Unit,
    private val onSaveExpense: () -> Unit,
    private val onAddEmployee: () -> Unit,
    private val onSaveSalary: () -> Unit,
    private val onPurchaseEdited: () -> Unit,
    private val onProductionEdited: () -> Unit,
    private val onVendorPaymentEdited: () -> Unit,
    private val onCreditCollectionEdited: () -> Unit,
    private val onExpenseEdited: () -> Unit,
    private val onSalaryEdited: () -> Unit,
    private val onSaleEdited: () -> Unit,
    private val onPurchaseVendorSelected: (Int) -> Unit,
    private val onSaleSelectionChanged: (Int, Int, Int) -> Unit,
    private val onSaleProductSelected: () -> Unit,
    private val onSaleInputModeChanged: () -> Unit,
    private val onSalePaymentModeChanged: () -> Unit,
    private val onSaleSurchargeChanged: (Boolean) -> Unit,
) {
    private val purchaseSaveButton: Button by lazy { activity.findViewById(R.id.savePurchaseButton) }
    private val productionSaveButton: Button by lazy { activity.findViewById(R.id.saveProductionButton) }
    private val vendorPaymentSaveButton: Button by lazy { activity.findViewById(R.id.saveVendorPaymentButton) }
    private val saleSaveButton: Button by lazy { activity.findViewById(R.id.saveSaleButton) }
    private val addCustomerButton: Button by lazy { activity.findViewById(R.id.addCustomerButton) }
    private val creditCollectionSaveButton: Button by lazy { activity.findViewById(R.id.saveCreditCollectionButton) }
    private val expenseSaveButton: Button by lazy { activity.findViewById(R.id.saveExpenseButton) }
    private val addEmployeeButton: Button by lazy { activity.findViewById(R.id.addEmployeeButton) }
    private val salarySaveButton: Button by lazy { activity.findViewById(R.id.saveSalaryButton) }

    private val rawLitersInput: EditText by lazy { activity.findViewById(R.id.rawLitersInput) }
    private val fatInput: EditText by lazy { activity.findViewById(R.id.fatInput) }
    private val lrInput: EditText by lazy { activity.findViewById(R.id.lrInput) }
    private val milkUsedInput: EditText by lazy { activity.findViewById(R.id.milkUsedInput) }
    private val yogurtProducedInput: EditText by lazy { activity.findViewById(R.id.yogurtProducedInput) }
    private val paymentAmountInput: EditText by lazy { activity.findViewById(R.id.paymentAmountInput) }
    private val creditCollectionAmountInput: EditText by lazy { activity.findViewById(R.id.creditCollectionAmountInput) }
    private val expenseAmountInput: EditText by lazy { activity.findViewById(R.id.expenseAmountInput) }
    private val salaryAmountInput: EditText by lazy { activity.findViewById(R.id.salaryAmountInput) }
    private val saleQuantityInput: EditText by lazy { activity.findViewById(R.id.saleQuantityInput) }
    private val saleValueInput: EditText by lazy { activity.findViewById(R.id.saleValueInput) }
    private val saleRateInput: EditText by lazy { activity.findViewById(R.id.saleRateInput) }
    private val saleSurchargeInput: EditText by lazy { activity.findViewById(R.id.saleSurchargeInput) }
    private val saleProductSpinner: Spinner by lazy { activity.findViewById(R.id.saleProductSpinner) }
    private val saleInputModeRadioGroup: RadioGroup by lazy { activity.findViewById(R.id.saleInputModeRadioGroup) }
    private val salePaymentModeRadioGroup: RadioGroup by lazy { activity.findViewById(R.id.salePaymentModeRadioGroup) }
    private val saleSurchargeCheckbox: CheckBox by lazy { activity.findViewById(R.id.saleSurchargeCheckbox) }
    private val vendorSpinner: Spinner by lazy { activity.findViewById(R.id.vendorSpinner) }
    private val saleAccountSpinner: Spinner by lazy { activity.findViewById(R.id.saleAccountSpinner) }
    private val saleCustomerSpinner: Spinner by lazy { activity.findViewById(R.id.saleCustomerSpinner) }

    fun setup() {
        purchaseSaveButton.setOnClickListener { onSavePurchase() }
        productionSaveButton.setOnClickListener { onSaveProduction() }
        vendorPaymentSaveButton.setOnClickListener { onSaveVendorPayment() }
        saleSaveButton.setOnClickListener { onSaveSale() }
        addCustomerButton.setOnClickListener { onAddCustomer() }
        creditCollectionSaveButton.setOnClickListener { onSaveCreditCollection() }
        expenseSaveButton.setOnClickListener { onSaveExpense() }
        addEmployeeButton.setOnClickListener { onAddEmployee() }
        salarySaveButton.setOnClickListener { onSaveSalary() }

        vendorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onPurchaseVendorSelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }


        rawLitersInput.addTextChangedListener(editWatcher { onPurchaseEdited() })
        fatInput.addTextChangedListener(editWatcher { onPurchaseEdited() })
        lrInput.addTextChangedListener(editWatcher { onPurchaseEdited() })

        milkUsedInput.addTextChangedListener(editWatcher { onProductionEdited() })
        yogurtProducedInput.addTextChangedListener(editWatcher { onProductionEdited() })
        paymentAmountInput.addTextChangedListener(editWatcher { onVendorPaymentEdited() })
        creditCollectionAmountInput.addTextChangedListener(editWatcher { onCreditCollectionEdited() })
        expenseAmountInput.addTextChangedListener(editWatcher { onExpenseEdited() })
        salaryAmountInput.addTextChangedListener(editWatcher { onSalaryEdited() })

        saleQuantityInput.addTextChangedListener(editWatcher { onSaleEdited() })
        saleValueInput.addTextChangedListener(editWatcher { onSaleEdited() })
        saleRateInput.addTextChangedListener(editWatcher { onSaleEdited() })
        saleSurchargeInput.addTextChangedListener(editWatcher { onSaleEdited() })

        saleAccountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSaleSelectionChanged(
                    saleProductSpinner.selectedItemPosition,
                    saleCustomerSpinner.selectedItemPosition,
                    position,
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        saleCustomerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSaleSelectionChanged(
                    saleProductSpinner.selectedItemPosition,
                    position,
                    saleAccountSpinner.selectedItemPosition,
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        saleProductSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSaleProductSelected()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        saleInputModeRadioGroup.setOnCheckedChangeListener { _, _ -> onSaleInputModeChanged() }
        salePaymentModeRadioGroup.setOnCheckedChangeListener { _, _ -> onSalePaymentModeChanged() }
        saleSurchargeCheckbox.setOnCheckedChangeListener { _, enabled -> onSaleSurchargeChanged(enabled) }
    }

    private fun editWatcher(onChanged: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: Editable?) = Unit
    }
}
