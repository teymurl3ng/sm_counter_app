package com.saaf.milkshop.presentation.transactions

import androidx.lifecycle.ViewModel
import com.saaf.milkshop.domain.VendorPurchaseCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presentation-state holder for the Purchase screen pilot.
 *
 * This does not own persistence or business semantics. The existing domain
 * calculator remains authoritative for the purchase formula; this ViewModel
 * only stores the current draft inputs/settings and exposes the presentation
 * state as an observable contract.
 */
class PurchaseViewModel : ViewModel() {
    data class Preview(
        val snfPercentage: Double,
        val tsPercentage: Double,
        val adjustedLiters: Double,
        val amountOwed: Double,
    )

    data class VendorState(val accountId: Long, val name: String)

    data class State(
        val vendors: List<VendorState> = emptyList(),
        val selectedVendorIndex: Int = 0,
        val rawLitersText: String = "",
        val fatText: String = "",
        val lrText: String = "",
        val milkRate: Double? = null,
        val deliveryFee: Double? = null,
        val tsStandard: Double? = null,
    )

    private val _state = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _state.asStateFlow()
    val state: State get() = _state.value

    fun setVendors(vendors: List<VendorState>) {
        _state.value = state.copy(
            vendors = vendors,
            selectedVendorIndex = state.selectedVendorIndex.coerceIn(0, (vendors.size - 1).coerceAtLeast(0)),
        )
    }

    fun selectVendor(index: Int) {
        _state.value = state.copy(selectedVendorIndex = index.coerceAtLeast(0))
    }

    fun selectedVendorAccountId(): Long? =
        state.vendors.getOrNull(state.selectedVendorIndex)?.accountId

    fun updateInputs(rawLitersText: String, fatText: String, lrText: String) {
        _state.value = state.copy(
            rawLitersText = rawLitersText,
            fatText = fatText,
            lrText = lrText,
        )
    }

    fun updateSettings(milkRate: Double?, deliveryFee: Double?, tsStandard: Double?) {
        _state.value = state.copy(
            milkRate = milkRate,
            deliveryFee = deliveryFee,
            tsStandard = tsStandard,
        )
    }

    fun preview(): Preview? {
        val current = state
        val raw = current.rawLitersText.toDoubleOrNull() ?: return null
        val fat = current.fatText.toDoubleOrNull() ?: return null
        val lr = current.lrText.toDoubleOrNull() ?: return null
        val rate = current.milkRate ?: return null
        val delivery = current.deliveryFee ?: return null
        val ts = current.tsStandard ?: return null

        return try {
            val result = VendorPurchaseCalculator.calculate(
                rawLiters = raw,
                fatPercentage = fat,
                lrReading = lr,
                milkRate = rate,
                deliveryFee = delivery,
                tsStandard = ts,
            )
            Preview(
                snfPercentage = result.snfPercentage,
                tsPercentage = result.tsPercentage,
                adjustedLiters = result.adjustedLiters,
                amountOwed = result.amountOwed,
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun clearDraft() {
        _state.value = state.copy(
            rawLitersText = "",
            fatText = "",
            lrText = "",
        )
    }
}
