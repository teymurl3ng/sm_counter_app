package com.saaf.milkshop.presentation.transactions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation state for Sale. Business calculations remain outside this ViewModel. */
class SaleViewModel : ViewModel() {
    data class ProductState(val productId: Long, val name: String, val unit: String)
    data class CustomerState(val accountId: Long, val name: String)
    data class PaymentAccountState(val accountId: Long, val name: String)
    data class Settings(val milkRate: Double?, val yogurtRate: Double?, val defaultSurcharge: Double?)

    data class State(
        val products: List<ProductState> = emptyList(),
        val customers: List<CustomerState> = emptyList(),
        val paymentAccounts: List<PaymentAccountState> = emptyList(),
        val settings: Settings = Settings(null, null, null),
        val selectedProductIndex: Int = 0,
        val selectedCustomerIndex: Int = 0,
        val selectedPaymentAccountIndex: Int = 0,
        val quantityMode: Boolean = true,
        val creditMode: Boolean = false,
        val surchargeEnabled: Boolean = false,
        val rateText: String = "",
        val quantityText: String = "",
        val valueText: String = "",
        val surchargeText: String = "",
        val previewText: String = "",
        val saveEnabled: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _state.asStateFlow()
    val state: State get() = _state.value

    fun setReferenceData(
        products: List<ProductState>,
        customers: List<CustomerState>,
        paymentAccounts: List<PaymentAccountState>,
        settings: Settings,
    ) {
        _state.value = state.copy(
            products = products,
            customers = customers,
            paymentAccounts = paymentAccounts,
            settings = settings,
        )
    }

    fun setSelections(productIndex: Int, customerIndex: Int, paymentAccountIndex: Int) {
        _state.value = state.copy(
            selectedProductIndex = productIndex.coerceAtLeast(0),
            selectedCustomerIndex = customerIndex.coerceAtLeast(0),
            selectedPaymentAccountIndex = paymentAccountIndex.coerceAtLeast(0),
        )
    }

    fun setModes(quantityMode: Boolean? = null, creditMode: Boolean? = null, surchargeEnabled: Boolean? = null) {
        _state.value = state.copy(
            quantityMode = quantityMode ?: state.quantityMode,
            creditMode = creditMode ?: state.creditMode,
            surchargeEnabled = surchargeEnabled ?: state.surchargeEnabled,
        )
    }

    fun setInputs(quantity: String, value: String, rate: String, surcharge: String) {
        _state.value = state.copy(
            quantityText = quantity,
            valueText = value,
            rateText = rate,
            surchargeText = surcharge,
        )
    }

    fun setRate(rateText: String) {
        _state.value = state.copy(rateText = rateText)
    }

    fun setPreview(previewText: String) {
        _state.value = state.copy(previewText = previewText)
    }

    fun setSaveEnabled(enabled: Boolean) {
        _state.value = state.copy(saveEnabled = enabled)
    }

    fun setSelectionsFromUi(productIndex: Int, customerIndex: Int, paymentAccountIndex: Int) =
        setSelections(productIndex, customerIndex, paymentAccountIndex)

    fun selectedProductId(): Long? = state.products.getOrNull(state.selectedProductIndex)?.productId
    fun selectedCustomerAccountId(): Long? = state.customers.getOrNull(state.selectedCustomerIndex)?.accountId
    fun selectedPaymentAccountId(): Long? = state.paymentAccounts.getOrNull(state.selectedPaymentAccountIndex)?.accountId
}
