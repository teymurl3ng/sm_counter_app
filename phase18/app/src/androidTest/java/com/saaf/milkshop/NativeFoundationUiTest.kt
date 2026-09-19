package com.saaf.milkshop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.nativeui.NativeUiTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeFoundationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<NativePreviewActivity>()

    @Test
    fun welcomeContinueOpensCashierSale() {
        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()

        composeRule.onNodeWithText("New sale").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertIsSelected()
    }

    @Test
    fun productButtonsSwitchProductRateUnitAndCalculatedEffects() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierProductMilk).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierProductYogurt).performClick().assertIsSelected()
        composeRule.onNodeWithText("PKR 400 / kg").assertIsDisplayed()
        composeRule.onNodeWithText("Cash +PKR 800").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierProductMilk).performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithText("PKR 300 / L").assertIsDisplayed()
        composeRule.onNodeWithText("Cash +PKR 600").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun entryModesInputAndSteppersConvertAndRecalculate() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierModeAmount).performClick().assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("600")
        composeRule.onNodeWithText("PKR").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement("900")
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("900")
        composeRule.onNodeWithTag(NativeUiTags.CashierIncrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("950")
        composeRule.onNodeWithTag(NativeUiTags.CashierDecrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("900")

        composeRule.onNodeWithTag(NativeUiTags.CashierModeQuantity).performClick().assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("3.0")
        composeRule.onNodeWithTag(NativeUiTags.CashierDecrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("2.5")
        composeRule.onNodeWithTag(NativeUiTags.CashierIncrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("3.0")
    }

    @Test
    fun customerPaymentOpensPickerAndPaidNowRestoresCashEffect() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerAhmed).performClick()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).assertIsDisplayed()
        composeRule.onNodeWithText("Customer due +PKR 600").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentPaidNow).performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithText("Cash +PKR 600").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).assertDoesNotExist()
    }

    @Test
    fun customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerCancel).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentPaidNow).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).assertIsNotSelected()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerBilal).performClick()
        composeRule.onNodeWithText("Bilal Dairy Point").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerAhmed).performClick()
        composeRule.onNodeWithText("Ahmed Store").assertIsDisplayed()
    }

    @Test
    fun secondaryDetailsToggleShowsAndHidesItsDefinedContent() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierSecondaryDetails).performScrollTo().performClick()
        composeRule.onNodeWithText("Counter pickup").assertIsDisplayed()
        composeRule.onNodeWithText("None").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierSecondaryDetails).performClick()
        composeRule.onNodeWithText("Counter pickup").assertDoesNotExist()
        composeRule.onNodeWithText("None").assertDoesNotExist()
    }

    @Test
    fun everyBottomDestinationProvidesItsDefinedPreviewResponse() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).performClick()
        composeRule.onNodeWithText("Sell is already open.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).performClick()
        composeRule.onNodeWithText("Collect is not connected in this native preview yet.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).performClick()
        composeRule.onNodeWithText("Activity is not connected in this native preview yet.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).performClick()
        composeRule.onNodeWithText("More is not connected in this native preview yet.").assertIsDisplayed()
    }

    @Test
    fun recordSaleShowsSafeNonWritingResult() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().performClick()

        composeRule.onNodeWithText("Native preview only — no sale was recorded.").assertIsDisplayed()
    }

    private fun openCashier() {
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        composeRule.onNodeWithText("New sale").assertIsDisplayed()
    }
}
