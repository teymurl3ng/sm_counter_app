package com.saaf.milkshop.qa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.SaafMilkTestRunner
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.nativeui.NativeUiTags
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Activity-hosted Collect IME occlusion + whole-window evidence captures.
 */
@RunWith(AndroidJUnit4::class)
class NativeCollectionDeviceEvidenceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<NativePreviewActivity>()

    @Test
    fun collectAmountImeDoesNotPermanentlyOccludeFocusedFieldOrRecordAction() = runBlocking {
        val app = composeRule.activity.application as MilkShopApplication
        val customerName = "IME Collect ${System.nanoTime()}"
        val customerId = CustomerManagementService(app.database).create(customerName)
        openCollect()

        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSearch).performTextReplacement(customerName)
        composeRule.onNodeWithTag(NativeUiTags.collectCustomer(customerId)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("150")
        composeRule.waitForIdle()

        val report = NativeLayoutImeEvidence.assertFocusedFieldNotOccluded(
            activity = composeRule.activity,
            focused = composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput),
            critical = composeRule.onNodeWithTag(NativeUiTags.CollectRecord),
        )
        NativeEvidenceCapture.captureWholeWindow(
            composeRule.activity,
            NativeEvidenceCapture.CaptureSpec(
                screen = "collect",
                scenario = if (report.imeVisible) "keyboard-risk-amount" else "ready-amount-focused",
                workflowId = "collect_amount",
                note = "imeVisible=${report.imeVisible};imeTop=${report.imeTop}",
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().assertIsDisplayed()
        Unit
    }

    @Test
    fun collectCheckpointEvidenceCapturesReadyDefault() {
        openCollect()
        NativeEvidenceCapture.captureWholeWindow(
            composeRule.activity,
            NativeEvidenceCapture.CaptureSpec(
                screen = "collect",
                scenario = "ready-default",
                workflowId = "collect_customer_selector",
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).assertIsDisplayed()
    }

    private fun openCollect() {
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_CASHIER_LOGIN)).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_CASHIER_LOGIN)).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).fetchSemanticsNode()
            }.isSuccess ||
                runCatching {
                    composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).fetchSemanticsNode()
                }.isSuccess
        }
        if (runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).fetchSemanticsNode()
            }.isSuccess
        ) {
            composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        }
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_CASHIER_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onNodeWithText("New sale").fetchSemanticsNode() }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).performClick()
        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onNodeWithText("Collect payment").fetchSemanticsNode() }.isSuccess
        }
    }
}
