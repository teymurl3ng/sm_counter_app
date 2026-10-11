package com.saaf.milkshop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
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
class NativeOwnerBootstrapUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<NativePreviewActivity>()

    @Test
    fun firstRunCreatesOwnerShowsRecoveryCodeAndEntersCashier() {
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Create Owner")
        composeRule.onNodeWithTag(NativeUiTags.AuthCreateOwner).assertIsNotEnabled()

        composeRule.onNodeWithTag(NativeUiTags.AuthBackToWelcome).performClick()
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Create Owner")

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Create Owner")

        composeRule.onNodeWithTag(NativeUiTags.AuthOwnerDisplayInput)
            .performTextReplacement("Instrumentation Owner")
        composeRule.onNodeWithTag(NativeUiTags.AuthOwnerLoginInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_LOGIN)
        composeRule.onNodeWithTag(NativeUiTags.AuthOwnerPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthOwnerConfirmationInput)
            .performTextReplacement("different owner passphrase 2026")
        composeRule.onNodeWithTag(NativeUiTags.AuthCreateOwner)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("The two passphrases do not match.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthOwnerPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthOwnerConfirmationInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthCreateOwner)
            .performScrollTo()
            .performClick()

        waitForText("Recovery code")
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Recovery code").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthRecoveryAcknowledge).performClick()
        waitForText("Overview")
    }

    private fun waitForText(text: String) {
        try {
            composeRule.waitUntil(timeoutMillis = 20_000) {
                composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag(NativeUiTags.AuthError).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (timeout: Throwable) {
            throw AssertionError(
                "Timed out waiting for '$text'.",
                timeout,
            )
        }
        val errors = composeRule.onAllNodesWithTag(NativeUiTags.AuthError).fetchSemanticsNodes()
        check(errors.isEmpty()) {
            "Authentication showed an error while waiting for '$text'."
        }
        // A compact title can share its wording with the form's primary action.
        composeRule.onAllNodesWithText(text)[0].assertIsDisplayed()
    }
}
