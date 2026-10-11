package com.saaf.milkshop.presentation.transactions

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.CheckBox
import android.widget.TextView
import com.saaf.milkshop.R
import com.saaf.milkshop.application.transactions.TransactionOperationRepository
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.AppPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Presentation/application boundary for the existing sale-save operation.
 *
 * The operation semantics are intentionally unchanged. This class removes the
 * sale handler from MainActivity while keeping the same validation, service
 * call, backup, refresh, and error behavior.
 */
class SaleTransactionAction(
    private val activity: Activity,
    private val transactionOperations: TransactionOperationRepository,
    private val uiScope: CoroutineScope,
    private val selectedProductId: () -> Long?,
    private val selectedCustomerAccountId: () -> Long?,
    private val selectedPaymentAccountId: () -> Long?,
    private val userMessageFor: (Exception) -> String,
    private val showError: (String) -> Unit,
    private val queueAutomaticLocalBackup: () -> Unit,
    private val refreshDashboard: suspend () -> Unit,
    private val refreshTransactionHistory: suspend () -> Unit,
    private val formatMoney: (Double) -> String,
    private val updateActionState: () -> Unit,
    private val clearInputs: () -> Unit,
    private val authorize: suspend (AppPermission) -> Unit,
) {
    private val rateInput: EditText by lazy { activity.findViewById(R.id.saleRateInput) }
    private val quantityRadioButton: android.widget.RadioButton by lazy { activity.findViewById(R.id.saleQuantityRadioButton) }
    private val quantityInput: EditText by lazy { activity.findViewById(R.id.saleQuantityInput) }
    private val valueInput: EditText by lazy { activity.findViewById(R.id.saleValueInput) }
    private val channelSpinner: Spinner by lazy { activity.findViewById(R.id.saleChannelSpinner) }
    private val paymentModeRadioGroup: RadioGroup by lazy { activity.findViewById(R.id.salePaymentModeRadioGroup) }
    private val accountSpinner: Spinner by lazy { activity.findViewById(R.id.saleAccountSpinner) }
    private val customerSpinner: Spinner by lazy { activity.findViewById(R.id.saleCustomerSpinner) }
    private val surchargeCheckbox: CheckBox by lazy { activity.findViewById(R.id.saleSurchargeCheckbox) }
    private val surchargeInput: EditText by lazy { activity.findViewById(R.id.saleSurchargeInput) }
    private val noteInput: EditText by lazy { activity.findViewById(R.id.saleNoteInput) }
    private val saveButton: Button by lazy { activity.findViewById(R.id.saveSaleButton) }
    private val statusText: TextView by lazy { activity.findViewById(R.id.saleStatusText) }
    private val errorText: TextView by lazy { activity.findViewById(R.id.saleErrorText) }

    fun execute() {
        errorText.visibility = View.GONE

        val productId = selectedProductId()
        val rate = rateInput.text.toString().toDoubleOrNull()
        val quantityMode = quantityRadioButton.isChecked
        val quantity = quantityInput.text.toString().toDoubleOrNull()
        val value = valueInput.text.toString().toDoubleOrNull()
        val channel = channelSpinner.selectedItem?.toString()
        val credit = paymentModeRadioGroup.checkedRadioButtonId == R.id.saleCreditRadioButton
        val accountId = selectedPaymentAccountId()
        val customerAccountId = selectedCustomerAccountId()
        val surchargeText = surchargeInput.text.toString().toDoubleOrNull()

        when {
            productId == null -> { showError(activity.getString(R.string.sale_error_no_product)); return }
            rate == null -> { showError(activity.getString(R.string.sale_error_rate)); return }
            rate <= 0.0 -> { showError(activity.getString(R.string.sale_error_rate_positive)); return }
            quantityMode && quantity == null -> { showError(activity.getString(R.string.sale_error_quantity)); return }
            quantityMode && quantity!! <= 0.0 -> { showError(activity.getString(R.string.sale_error_quantity_positive)); return }
            !quantityMode && value == null -> { showError(activity.getString(R.string.sale_error_value)); return }
            !quantityMode && value!! <= 0.0 -> { showError(activity.getString(R.string.sale_error_value_positive)); return }
            surchargeCheckbox.isChecked && surchargeText == null -> { showError(activity.getString(R.string.sale_error_surcharge)); return }
            surchargeCheckbox.isChecked && surchargeText!! < 0.0 -> { showError(activity.getString(R.string.sale_error_surcharge_negative)); return }
            credit && customerAccountId == null -> { showError(activity.getString(R.string.sale_error_no_customer)); return }
            !credit && accountId == null -> { showError(activity.getString(R.string.sale_error_no_payment_account)); return }
        }

        saveButton.isEnabled = false
        statusText.text = activity.getString(R.string.sale_saving)

        uiScope.launch {
            try {
                authorize(AppPermission.SALE_AND_COLLECTION)
                val result = transactionOperations.recordSale(
                    SaleInput(
                        productId = productId!!,
                        occurredAt = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                        channel = channel,
                        unitRate = rate!!,
                        quantity = if (quantityMode) quantity else null,
                        value = if (quantityMode) null else value,
                        payment = if (credit) SalePayment.OnCredit(customerAccountId!!)
                                  else SalePayment.Account(accountId!!),
                        surchargeEnabled = surchargeCheckbox.isChecked,
                        surchargeAmount = if (surchargeCheckbox.isChecked) surchargeText else null,
                        notes = noteInput.text.toString().trim().ifEmpty { null },
                    )
                )
                queueAutomaticLocalBackup()
                refreshDashboard()
                refreshTransactionHistory()
                withContext(Dispatchers.Main) {
                    statusText.text = activity.getString(R.string.sale_saved, result.eventId, formatMoney(result.total))
                    errorText.visibility = View.GONE
                    updateActionState()
                    clearInputs()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = ""
                    showError(userMessageFor(e))
                    updateActionState()
                }
            }
        }
    }
}
