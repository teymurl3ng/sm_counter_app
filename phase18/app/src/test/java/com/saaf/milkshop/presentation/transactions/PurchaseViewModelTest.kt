package com.saaf.milkshop.presentation.transactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PurchaseViewModelTest {
    @Test
    fun preview_uses_existing_purchase_calculator_contract() {
        val viewModel = PurchaseViewModel()
        viewModel.updateSettings(milkRate = 180.0, deliveryFee = 10.0, tsStandard = 12.5)
        viewModel.updateInputs("20", "4", "28")

        val preview = viewModel.preview()

        requireNotNull(preview)
        assertEquals(8.6, preview.snfPercentage, 0.0000001)
        assertEquals(12.6, preview.tsPercentage, 0.0000001)
        assertEquals(20.16, preview.adjustedLiters, 0.0000001)
        assertEquals(3828.8, preview.amountOwed, 0.0000001)
        assertEquals(viewModel.state, viewModel.stateFlow.value)
    }

    @Test
    fun clearDraft_removes_preview_input_state_but_keeps_settings() {
        val viewModel = PurchaseViewModel()
        viewModel.updateSettings(milkRate = 180.0, deliveryFee = 10.0, tsStandard = 12.5)
        viewModel.updateInputs("20", "4", "28")
        requireNotNull(viewModel.preview())

        viewModel.clearDraft()

        assertNull(viewModel.preview())
    }
}
