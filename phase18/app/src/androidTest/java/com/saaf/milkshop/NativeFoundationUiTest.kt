package com.saaf.milkshop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.nativeui.NativeUiTags
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeFoundationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<NativePreviewActivity>()

    @Test
    fun welcomeAccountPickerAndFullPassphraseOpenCashierSale() {
        openCashier()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertIsSelected()
    }

    @Test
    fun accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts() {
        openAccountPicker()

        composeRule.onNodeWithTag(NativeUiTags.AuthBackToWelcome).performClick()
        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
        openAccountPicker()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
        openAccountPicker()

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthPhoneSecurityMethod).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthMethodBack).performClick()
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthChooseAnotherAccount).performClick()
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()
        composeRule.onNodeWithText("Enter Quick PIN").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinBack).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()
        composeRule.onNodeWithText("Enter Quick PIN").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinUsePassphrase).performClick()
        composeRule.onNodeWithText("Welcome back, Instrumentation Owner").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseBack).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthChooseAnotherAccount).performClick()
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithText("Welcome back, Instrumentation Owner").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement("incorrect full passphrase")
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()

        waitForText("The login name or passphrase is incorrect, or this account is unavailable.")
        composeRule.onNodeWithText("Welcome back, Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithText(
            "The login name or passphrase is incorrect, or this account is unavailable.",
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        waitForText("New sale")
    }

    @Test
    fun quickPinUnlocksTheSelectedAccount() {
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PIN)
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).performClick()
        waitForText("New sale")
    }

    @Test
    fun quickPinRejectsAnotherAccountsPinAndUnlocksTheSelectedAccount() {
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_CASHIER_PIN)
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).performClick()
        waitForText(
            "That Quick PIN did not unlock Instrumentation Owner. " +
                "Check the selected account or use the full passphrase.",
        )
        composeRule.onNodeWithText("Enter Quick PIN").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PIN)
        composeRule.onNodeWithText(
            "That Quick PIN did not unlock Instrumentation Owner. " +
                "Check the selected account or use the full passphrase.",
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).performClick()
        try {
            waitForText("New sale")
        } catch (timeout: Throwable) {
            val visibleError = runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthError).printToString()
            }.getOrElse { "No tagged authentication error was visible." }
            throw AssertionError(
                "Correct selected-account PIN did not open Cashier. $visibleError",
                timeout,
            )
        }
    }

    @Test
    fun cashierSystemBackEndsSessionBeforeReturningToAccountPicker() {
        openCashier()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            assertNull((activity.application as MilkShopApplication).currentSessionId)
        }

        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Who is using Saaf?")
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
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        waitForText("New sale")
    }

    private fun openAccountPicker() {
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Who is using Saaf?")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
