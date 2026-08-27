package com.saaf.milkshop.presentation.records

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import android.text.method.LinkMovementMethod
import com.saaf.milkshop.R
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionService

class HistoryCorrectionsUi(
    private val activity: android.app.Activity,
    private val onHistoryEventClicked: (EventEntity) -> Unit,
    private val onCorrectionRequested: (EventEntity?) -> Unit,
    private val onSaveCorrectionRequested: () -> Unit,
    private val onCancelCorrectionRequested: () -> Unit,
    private val onCorrectionSaleInputModeChanged: () -> Unit,
    private val createSpinnerAdapter: (List<String>) -> SpinnerAdapter,
) {
    private lateinit var transactionHistoryText: TextView
    private lateinit var transactionDetailTitleText: TextView
    private lateinit var transactionDetailText: TextView
    private lateinit var correctTransactionButton: Button
    private lateinit var correctionEditorContainer: LinearLayout
    private lateinit var correctionTypeText: TextView
    private lateinit var correctionNotesInput: EditText
    private lateinit var correctionPurchaseGroup: LinearLayout
    private lateinit var correctionPurchaseRawLitersInput: EditText
    private lateinit var correctionPurchaseFatInput: EditText
    private lateinit var correctionPurchaseLrInput: EditText
    private lateinit var correctionProductionGroup: LinearLayout
    private lateinit var correctionProductionMilkInput: EditText
    private lateinit var correctionProductionYogurtInput: EditText
    private lateinit var correctionSaleGroup: LinearLayout
    private lateinit var correctionSaleInputModeRadioGroup: RadioGroup
    private lateinit var correctionSaleQuantityRadioButton: RadioButton
    private lateinit var correctionSaleValueRadioButton: RadioButton
    private lateinit var correctionSaleQuantityInput: EditText
    private lateinit var correctionSaleValueInput: EditText
    private lateinit var correctionSaleRateInput: EditText
    private lateinit var correctionSaleSurchargeInput: EditText
    private lateinit var correctionSaleChannelSpinner: Spinner
    private lateinit var correctionMonetaryGroup: LinearLayout
    private lateinit var correctionMonetaryAmountInput: EditText
    private lateinit var saveCorrectionButton: Button
    private lateinit var cancelCorrectionButton: Button
    private lateinit var correctionErrorText: TextView
    private lateinit var correctionStatusText: TextView

    fun renderState(state: HistoryCorrectionsViewModel.State) {
        if (state.detailLoading) {
            transactionDetailText.text = activity.getString(R.string.history_detail_loading)
        } else {
            transactionDetailText.text = state.detailText
        }
    }

    fun setup() {
        transactionHistoryText = activity.findViewById(R.id.transactionHistoryText)
        transactionDetailTitleText = activity.findViewById(R.id.transactionDetailTitleText)
        transactionDetailText = activity.findViewById(R.id.transactionDetailText)
        correctTransactionButton = activity.findViewById(R.id.correctTransactionButton)
        correctionEditorContainer = activity.findViewById(R.id.correctionEditorContainer)
        correctionTypeText = activity.findViewById(R.id.correctionTypeText)
        correctionNotesInput = activity.findViewById(R.id.correctionNotesInput)
        correctionPurchaseGroup = activity.findViewById(R.id.correctionPurchaseGroup)
        correctionPurchaseRawLitersInput = activity.findViewById(R.id.correctionPurchaseRawLitersInput)
        correctionPurchaseFatInput = activity.findViewById(R.id.correctionPurchaseFatInput)
        correctionPurchaseLrInput = activity.findViewById(R.id.correctionPurchaseLrInput)
        correctionProductionGroup = activity.findViewById(R.id.correctionProductionGroup)
        correctionProductionMilkInput = activity.findViewById(R.id.correctionProductionMilkInput)
        correctionProductionYogurtInput = activity.findViewById(R.id.correctionProductionYogurtInput)
        correctionSaleGroup = activity.findViewById(R.id.correctionSaleGroup)
        correctionSaleInputModeRadioGroup = activity.findViewById(R.id.correctionSaleInputModeRadioGroup)
        correctionSaleQuantityRadioButton = activity.findViewById(R.id.correctionSaleQuantityRadioButton)
        correctionSaleValueRadioButton = activity.findViewById(R.id.correctionSaleValueRadioButton)
        correctionSaleQuantityInput = activity.findViewById(R.id.correctionSaleQuantityInput)
        correctionSaleValueInput = activity.findViewById(R.id.correctionSaleValueInput)
        correctionSaleRateInput = activity.findViewById(R.id.correctionSaleRateInput)
        correctionSaleSurchargeInput = activity.findViewById(R.id.correctionSaleSurchargeInput)
        correctionSaleChannelSpinner = activity.findViewById(R.id.correctionSaleChannelSpinner)
        correctionSaleChannelSpinner.adapter = createSpinnerAdapter(
            listOf("retail", "wholesale", "credit-retail", "credit-wholesale")
        )
        correctionMonetaryGroup = activity.findViewById(R.id.correctionMonetaryGroup)
        correctionMonetaryAmountInput = activity.findViewById(R.id.correctionMonetaryAmountInput)
        saveCorrectionButton = activity.findViewById(R.id.saveCorrectionButton)
        cancelCorrectionButton = activity.findViewById(R.id.cancelCorrectionButton)
        correctionErrorText = activity.findViewById(R.id.correctionErrorText)
        correctionStatusText = activity.findViewById(R.id.correctionStatusText)
        transactionHistoryText.movementMethod = LinkMovementMethod.getInstance()

        correctTransactionButton.setOnClickListener {
            onCorrectionRequested(getCurrentCorrectionTarget())
        }
        saveCorrectionButton.setOnClickListener { onSaveCorrectionRequested() }
        cancelCorrectionButton.setOnClickListener { onCancelCorrectionRequested() }
        correctionSaleInputModeRadioGroup.setOnCheckedChangeListener { _, _ ->
            onCorrectionSaleInputModeChanged()
        }
    }

    private fun getCurrentCorrectionTarget(): EventEntity? = currentCorrectionTarget

    private var currentCorrectionTarget: EventEntity? = null

    fun renderHistory(events: List<EventEntity>, formatter: (EventEntity) -> String) {
        val text = if (events.isEmpty()) {
            SpannableStringBuilder(activity.getString(R.string.history_empty))
        } else {
            SpannableStringBuilder().apply {
                events.forEachIndexed { index, event ->
                    val start = length
                    append(formatter(event))
                    val end = length
                    setSpan(
                        object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                currentCorrectionTarget = event
                                onHistoryEventClicked(event)
                            }
                        },
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    if (index < events.lastIndex) append("\n\n")
                }
            }
        }
        transactionHistoryText.text = text
    }

    fun showTransactionDetailLoading(event: EventEntity) {
        currentCorrectionTarget = event
        correctTransactionButton.visibility = View.VISIBLE
        correctionEditorContainer.visibility = View.GONE
        correctionErrorText.visibility = View.GONE
        correctionStatusText.text = ""
        transactionDetailTitleText.text = activity.getString(R.string.history_detail_title, event.eventId)
        transactionDetailText.text = activity.getString(R.string.history_detail_loading)
    }

    fun setTransactionDetailText(detail: String) {
        transactionDetailText.text = detail
    }

    fun showCorrectionEditor(event: EventEntity) {
        currentCorrectionTarget = event
        correctionEditorContainer.visibility = View.VISIBLE
        correctionErrorText.visibility = View.GONE
        correctionStatusText.text = ""
        correctionTypeText.text = activity.getString(R.string.correction_type_format, event.eventType, event.eventId)
        correctionNotesInput.setText(event.notes.orEmpty())

        correctionPurchaseGroup.visibility = View.GONE
        correctionProductionGroup.visibility = View.GONE
        correctionSaleGroup.visibility = View.GONE
        correctionMonetaryGroup.visibility = View.GONE

        when (event.eventType) {
            VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> {
                correctionPurchaseGroup.visibility = View.VISIBLE
                correctionPurchaseRawLitersInput.setText(event.rawLiters?.toString().orEmpty())
                correctionPurchaseFatInput.setText(event.fatPercentage?.toString().orEmpty())
                correctionPurchaseLrInput.setText(event.lrReading?.toString().orEmpty())
            }
            YogurtProductionService.EVENT_TYPE_PRODUCTION -> {
                correctionProductionGroup.visibility = View.VISIBLE
                correctionProductionMilkInput.setText(event.milkLitersUsed?.toString().orEmpty())
                correctionProductionYogurtInput.setText(event.yogurtKgProduced?.toString().orEmpty())
            }
            SaleService.EVENT_TYPE_SALE -> {
                correctionSaleGroup.visibility = View.VISIBLE
                correctionSaleQuantityRadioButton.isChecked = true
                correctionSaleQuantityInput.setText(event.saleQuantity?.toString().orEmpty())
                correctionSaleValueInput.setText(event.saleValue?.toString().orEmpty())
                correctionSaleRateInput.setText(event.unitRate?.toString().orEmpty())
                correctionSaleSurchargeInput.setText(event.surchargeApplied.toString())
                val index = event.channel?.let { channel ->
                    (0 until correctionSaleChannelSpinner.adapter.count).firstOrNull { position ->
                        correctionSaleChannelSpinner.adapter.getItem(position)?.toString() == channel
                    } ?: -1
                } ?: -1
                if (index >= 0) correctionSaleChannelSpinner.setSelection(index)
                updateCorrectionSaleInputMode()
            }
            CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION,
            ExpenseService.EVENT_TYPE_EXPENSE,
            SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT,
            VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT -> {
                correctionMonetaryGroup.visibility = View.VISIBLE
                correctionMonetaryAmountInput.setText(event.amount?.toString().orEmpty())
            }
            else -> {
                showCorrectionError(activity.getString(R.string.correction_error_unsupported))
                correctionEditorContainer.visibility = View.GONE
            }
        }
    }

    fun hideCorrectionEditor() {
        correctionEditorContainer.visibility = View.GONE
        correctionErrorText.visibility = View.GONE
        correctionStatusText.text = ""
    }

    fun updateCorrectionSaleInputMode() {
        val quantityMode = correctionSaleQuantityRadioButton.isChecked
        correctionSaleQuantityInput.isEnabled = quantityMode
        correctionSaleValueInput.isEnabled = !quantityMode
    }

    fun showCorrectionError(message: String) {
        correctionErrorText.text = message
        correctionErrorText.visibility = View.VISIBLE
    }

    fun clearCorrectionMessage() {
        correctionErrorText.visibility = View.GONE
    }

    fun setCorrectionStatus(message: String) {
        correctionStatusText.text = message
    }

    fun hideCorrectionEditorAndShowStatus(message: String) {
        correctionErrorText.visibility = View.GONE
        correctionStatusText.text = message
        correctionEditorContainer.visibility = View.GONE
    }

    fun correctionInput(): CorrectionUiInput = CorrectionUiInput(
        notes = correctionNotesInput.text.toString().trim().ifEmpty { null },
        purchaseRawLiters = correctionPurchaseRawLitersInput.text.toString().trim(),
        purchaseFat = correctionPurchaseFatInput.text.toString().trim(),
        purchaseLr = correctionPurchaseLrInput.text.toString().trim(),
        productionMilk = correctionProductionMilkInput.text.toString().trim(),
        productionYogurt = correctionProductionYogurtInput.text.toString().trim(),
        saleRate = correctionSaleRateInput.text.toString().trim(),
        saleSurcharge = correctionSaleSurchargeInput.text.toString().trim(),
        saleQuantityMode = correctionSaleQuantityRadioButton.isChecked,
        saleQuantity = correctionSaleQuantityInput.text.toString().trim(),
        saleValue = correctionSaleValueInput.text.toString().trim(),
        saleChannel = correctionSaleChannelSpinner.selectedItem?.toString()?.trim()?.ifEmpty { null },
        monetaryAmount = correctionMonetaryAmountInput.text.toString().trim(),
    )

    fun currentTarget(): EventEntity? = currentCorrectionTarget
}

data class CorrectionUiInput(
    val notes: String?,
    val purchaseRawLiters: String,
    val purchaseFat: String,
    val purchaseLr: String,
    val productionMilk: String,
    val productionYogurt: String,
    val saleRate: String,
    val saleSurcharge: String,
    val saleQuantityMode: Boolean,
    val saleQuantity: String,
    val saleValue: String,
    val saleChannel: String?,
    val monetaryAmount: String,
)
