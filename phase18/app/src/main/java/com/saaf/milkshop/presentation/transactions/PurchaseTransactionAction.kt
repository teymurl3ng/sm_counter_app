package com.saaf.milkshop.presentation.transactions

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.saaf.milkshop.R
import com.saaf.milkshop.application.transactions.TransactionOperationRepository
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.AppPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Presentation/application boundary for the existing purchase-save operation.
 *
 * The operation semantics are intentionally unchanged. This class removes the
 * purchase handler from MainActivity while keeping the same validation,
 * service call, backup, refresh, and error behavior.
 */
class PurchaseTransactionAction(
    private val activity: Activity,
    private val transactionOperations: TransactionOperationRepository,
    private val uiScope: CoroutineScope,
    private val selectedVendorAccountId: () -> Long?,
    private val vendorsExist: () -> Boolean,
    private val clearPurchaseMessageIfEditing: () -> Unit,
    private val clearPurchaseInputs: () -> Unit,
    private val showPurchaseError: (String) -> Unit,
    private val userMessageFor: (Exception) -> String,
    private val queueAutomaticLocalBackup: () -> Unit,
    private val refreshDashboard: suspend () -> Unit,
    private val refreshTransactionHistory: suspend () -> Unit,
    private val authorize: suspend (AppPermission) -> Unit,
) {
    private val rawLitersInput: EditText by lazy { activity.findViewById(R.id.rawLitersInput) }
    private val fatInput: EditText by lazy { activity.findViewById(R.id.fatInput) }
    private val lrInput: EditText by lazy { activity.findViewById(R.id.lrInput) }
    private val purchaseStatusText: TextView by lazy { activity.findViewById(R.id.purchaseStatusText) }
    private val purchaseErrorText: TextView by lazy { activity.findViewById(R.id.purchaseErrorText) }
    private val savePurchaseButton: Button by lazy { activity.findViewById(R.id.savePurchaseButton) }

    fun execute() {
        clearPurchaseMessageIfEditing()

        val vendorAccountId = selectedVendorAccountId()
        val rawLiters = rawLitersInput.text.toString().toDoubleOrNull()
        val fat = fatInput.text.toString().toDoubleOrNull()
        val lr = lrInput.text.toString().toDoubleOrNull()

        when {
            vendorAccountId == null -> {
                showPurchaseError(activity.getString(R.string.purchase_error_no_vendor))
                return
            }
            rawLiters == null -> {
                showPurchaseError(activity.getString(R.string.purchase_error_raw_liters))
                return
            }
            rawLiters <= 0.0 -> {
                showPurchaseError(activity.getString(R.string.purchase_error_raw_liters_positive))
                return
            }
            fat == null -> {
                showPurchaseError(activity.getString(R.string.purchase_error_fat))
                return
            }
            fat < 0.0 -> {
                showPurchaseError(activity.getString(R.string.purchase_error_fat_negative))
                return
            }
            lr == null -> {
                showPurchaseError(activity.getString(R.string.purchase_error_lr))
                return
            }
            lr <= 0.0 -> {
                showPurchaseError(activity.getString(R.string.purchase_error_lr_positive))
                return
            }
        }

        savePurchaseButton.isEnabled = false
        purchaseStatusText.text = activity.getString(R.string.purchase_saving)

        uiScope.launch {
            try {
                authorize(AppPermission.PURCHASE)
                val result = transactionOperations.recordPurchase(
                    VendorPurchaseInput(
                        vendorAccountId = vendorAccountId,
                        occurredAt = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                        rawLiters = rawLiters,
                        fatPercentage = fat,
                        lrReading = lr,
                    )
                )

                queueAutomaticLocalBackup()
                refreshDashboard()
                refreshTransactionHistory()
                withContext(Dispatchers.Main) {
                    purchaseStatusText.text = activity.getString(
                        R.string.purchase_saved,
                        result.eventId,
                    )
                    purchaseErrorText.visibility = View.GONE
                    savePurchaseButton.isEnabled = vendorsExist()
                    clearPurchaseInputs()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    purchaseStatusText.text = ""
                    showPurchaseError(userMessageFor(e))
                    savePurchaseButton.isEnabled = vendorsExist()
                }
            }
        }
    }
}
