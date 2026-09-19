package com.saaf.milkshop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.graphics.toPixelMap
import com.saaf.milkshop.application.authentication.NativeAccountSummary
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationHost
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeAuthenticationScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun darkSystemHostKeepsTheBrandedAccountPickerContrast() {
        val account = NativeAccountSummary(
            userId = 9L,
            loginName = "contrast.owner",
            displayName = "Contrast Owner",
            role = AppRole.OWNER,
        )
        composeRule.setContent {
            SaafTheme(darkTheme = true) {
                NativeAuthenticationHost(
                    state = NativeAuthenticationState.AccountPicker(listOf(account)),
                    onRetry = {},
                    onBackToWelcome = {},
                    onSelectAccount = {},
                    onChooseAnotherAccount = {},
                    onBackFromPassphrase = {},
                    onUseQuickPin = {},
                    onUsePhoneSecurity = {},
                    onUseFullPassphrase = {},
                    onBackFromQuickPin = {},
                    onQuickPinSignIn = { it.fill('\u0000') },
                    onSignIn = { it.fill('\u0000') },
                    onBootstrapOwner = { _, _, first, second ->
                        first.fill('\u0000')
                        second.fill('\u0000')
                    },
                    onAcknowledgeRecoveryCode = {},
                )
            }
        }

        val titlePixels = composeRule.onNodeWithText("Who is using Saaf?")
            .captureToImage()
            .toPixelMap()
        var brightestTitlePixel = 0f
        for (y in 0 until titlePixels.height) {
            for (x in 0 until titlePixels.width) {
                val color = titlePixels[x, y]
                brightestTitlePixel = maxOf(brightestTitlePixel, color.red + color.green + color.blue)
            }
        }
        assertTrue(
            "The green access header must retain its light title in a dark system host.",
            brightestTitlePixel > 2.2f,
        )

        val labelPixels = composeRule.onNodeWithText("ACCOUNTS ON THIS PHONE")
            .captureToImage()
            .toPixelMap()
        var darkestLabelPixel = 3f
        for (y in 0 until labelPixels.height) {
            for (x in 0 until labelPixels.width) {
                val color = labelPixels[x, y]
                darkestLabelPixel = minOf(darkestLabelPixel, color.red + color.green + color.blue)
            }
        }
        assertTrue(
            "The ivory access body must retain a dark readable label in a dark system host.",
            darkestLabelPixel < 1.8f,
        )
    }

    @Test
    fun startupFailureRetryChangesToTheRequestedVisibleState() {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeAuthenticationState by remember {
                    mutableStateOf(NativeAuthenticationState.StartupFailure("Accounts are unavailable."))
                }
                NativeAuthenticationHost(
                    state = state,
                    onRetry = { state = NativeAuthenticationState.AccountPicker(emptyList()) },
                    onBackToWelcome = {},
                    onSelectAccount = {},
                    onChooseAnotherAccount = {},
                    onBackFromPassphrase = {},
                    onUseQuickPin = {},
                    onUsePhoneSecurity = {},
                    onUseFullPassphrase = {},
                    onBackFromQuickPin = {},
                    onQuickPinSignIn = { it.fill('\u0000') },
                    onSignIn = { it.fill('\u0000') },
                    onBootstrapOwner = { _, _, first, second ->
                        first.fill('\u0000')
                        second.fill('\u0000')
                    },
                    onAcknowledgeRecoveryCode = {},
                )
            }
        }

        composeRule.onNodeWithText("Accounts are unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthStartupRetry).performClick()
        composeRule.onNodeWithText("Who is using Saaf?").assertIsDisplayed()
    }

    @Test
    fun methodPickerShowsOnlyEnrolledPhoneSecurityAndSurfacesCancellation() {
        val account = NativeAccountSummary(
            userId = 7L,
            loginName = "phone.owner",
            displayName = "Phone Owner",
            role = AppRole.OWNER,
            phoneSecurityEnabled = true,
            phoneSecurityLabel = "Phone security",
        )
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeAuthenticationState by remember {
                    mutableStateOf(NativeAuthenticationState.MethodPicker(account))
                }
                NativeAuthenticationHost(
                    state = state,
                    onRetry = {},
                    onBackToWelcome = {},
                    onSelectAccount = {},
                    onChooseAnotherAccount = {},
                    onBackFromPassphrase = {},
                    onUseQuickPin = {},
                    onUsePhoneSecurity = {
                        state = NativeAuthenticationState.MethodPicker(
                            account = account,
                            error = "Quick unlock was cancelled",
                        )
                    },
                    onUseFullPassphrase = {},
                    onBackFromQuickPin = {},
                    onQuickPinSignIn = { it.fill('\u0000') },
                    onSignIn = { it.fill('\u0000') },
                    onBootstrapOwner = { _, _, first, second ->
                        first.fill('\u0000')
                        second.fill('\u0000')
                    },
                    onAcknowledgeRecoveryCode = {},
                )
            }
        }

        composeRule.onNodeWithTag(NativeUiTags.AuthPhoneSecurityMethod).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).assertIsDisplayed()
        composeRule.onNodeWithText("Phone security").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthPhoneSecurityMethod).performClick()
        composeRule.onNodeWithText("Quick unlock was cancelled").assertIsDisplayed()
    }

    @Test
    fun rateLimitedQuickPinKeepsFullPassphraseRecoveryVisibleAndWorking() {
        val account = NativeAccountSummary(
            userId = 8L,
            loginName = "pin.owner",
            displayName = "PIN Owner",
            role = AppRole.OWNER,
            quickPinEnabled = true,
        )
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeAuthenticationState by remember {
                    mutableStateOf(
                        NativeAuthenticationState.QuickPin(
                            account = account,
                            error = "Quick unlock is paused. Sign in with your full passphrase.",
                        ),
                    )
                }
                NativeAuthenticationHost(
                    state = state,
                    onRetry = {},
                    onBackToWelcome = {},
                    onSelectAccount = {},
                    onChooseAnotherAccount = {},
                    onBackFromPassphrase = {},
                    onUseQuickPin = {},
                    onUsePhoneSecurity = {},
                    onUseFullPassphrase = { state = NativeAuthenticationState.Passphrase(account) },
                    onBackFromQuickPin = {},
                    onQuickPinSignIn = { it.fill('\u0000') },
                    onSignIn = { it.fill('\u0000') },
                    onBootstrapOwner = { _, _, first, second ->
                        first.fill('\u0000')
                        second.fill('\u0000')
                    },
                    onAcknowledgeRecoveryCode = {},
                )
            }
        }

        composeRule.onNodeWithText("Quick unlock is paused. Sign in with your full passphrase.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinUsePassphrase).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Welcome back, PIN Owner").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).assertIsDisplayed()
    }
}
