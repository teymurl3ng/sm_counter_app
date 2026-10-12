package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.saaf.milkshop.application.authentication.NativeAccountSummary
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.auth.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.qa.NativeEvidenceCapture
import com.saaf.milkshop.qa.NativeLayoutImeEvidence
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NativeQuickPinContractTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val account = NativeAccountSummary(17, "keypad.owner", "Keypad Owner", AppRole.OWNER, quickPinEnabled = true)
    private val state = mutableStateOf<NativeAuthenticationState>(NativeAuthenticationState.QuickPin(account))
    private val submissions = mutableListOf<String>()
    private var phoneCalls = 0

    private fun mount(fontScale: Float = 1f, dark: Boolean = false, height: Int? = null) {
        rule.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
            }
        }
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                SaafTheme(darkTheme = dark) {
                    Box(if (height == null) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(height.dp)) {
                        NativeAuthenticationHost(state = state.value,
                            onRetry = {}, onBackToWelcome = {}, onSelectAccount = {}, onChooseAnotherAccount = {},
                            onBackFromPassphrase = {}, onUseQuickPin = {},
                            onUsePhoneSecurity = {
                                phoneCalls++
                                val pin = state.value as NativeAuthenticationState.QuickPin
                                state.value = pin.copy(error = "Quick unlock was cancelled")
                            },
                            onUseFullPassphrase = { state.value = NativeAuthenticationState.Passphrase(account) },
                            onBackFromQuickPin = {},
                            onQuickPinSignIn = {
                                submissions += it.concatToString(); it.fill('\u0000')
                                val pin = state.value as NativeAuthenticationState.QuickPin
                                state.value = pin.copy(busy = true, error = null)
                            },
                            onQuickPinEdit = {
                                val pin = state.value as NativeAuthenticationState.QuickPin
                                if (!pin.persistentError) state.value = pin.copy(error = null)
                            },
                            onSignIn = { it.fill('\u0000') }, onBootstrapOwner = { _, _, a, b -> a.fill('\u0000'); b.fill('\u0000') },
                            onAcknowledgeRecoveryCode = {})
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun digit(value: Int) = rule.onNodeWithTag(NativeUiTags.authQuickPinDigit(value)).performScrollTo().performClick()
    private fun count(value: Int) {
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "$value digits entered"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            .assert(SemanticsMatcher("No editable secret") { !it.config.contains(androidx.compose.ui.semantics.SemanticsActions.SetText) })
    }

    @Test fun keypadEditingHardwareAndBusyContracts() {
        mount()
        val unlock = rule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit)
        unlock.assertIsNotEnabled()
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinClear).assertIsNotEnabled()
        digit(0); digit(1); digit(2); count(3); unlock.assertIsNotEnabled()
        digit(3); count(4); unlock.assertIsEnabled()
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinDelete).performClick(); count(3)
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinClear).performClick(); count(0)
        repeat(12) { digit(it % 10) }
        count(12)
        rule.onNodeWithTag(NativeUiTags.authQuickPinDigit(1)).assertIsNotEnabled()
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinClear).performClick()
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinKeypad).performKeyInput {
            pressKey(Key.Zero); pressKey(Key.One); pressKey(Key.Two); pressKey(Key.Three)
            pressKey(Key.Backspace); pressKey(Key.Four); pressKey(Key.Enter)
        }
        assertEquals(listOf("0124"), submissions)
        count(0)
        unlock.assertIsNotEnabled()
        rule.onNodeWithTag(NativeUiTags.authQuickPinDigit(0)).assertIsNotEnabled()
        unlock.performClick()
        assertEquals(1, submissions.size)
        rule.runOnIdle { state.value = NativeAuthenticationState.QuickPin(account, error = "Incorrect PIN") }
        digit(0)
        rule.onNodeWithText("Incorrect PIN").assertDoesNotExist()
        rule.runOnIdle { state.value = NativeAuthenticationState.QuickPin(account, error = "PIN paused", persistentError = true) }
        digit(1)
        rule.onNodeWithText("PIN paused").performScrollTo().assertIsDisplayed()
        rule.runOnIdle {
            assertFalse(NativeLayoutImeEvidence.imeInsets(rule.activity).isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()))
        }
    }

    @Test fun keypadPhoneSecurityAndRecoveryContracts() {
        mount()
        rule.onNodeWithTag(NativeUiTags.AuthPhoneSecurityMethod).assertDoesNotExist()
        rule.runOnIdle { state.value = NativeAuthenticationState.QuickPin(account.copy(phoneSecurityEnabled = true)) }
        digit(1); digit(2)
        rule.onNodeWithTag(NativeUiTags.AuthPhoneSecurityMethod).performScrollTo().performClick()
        assertEquals(1, phoneCalls); count(0)
        rule.onNodeWithText("Quick unlock was cancelled").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinUsePassphrase).performScrollTo().performClick()
        rule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).assertIsDisplayed()
    }

    @Test fun keypadGeometryDefault() = geometry(1f, false, null)
    @Test fun keypadGeometryLarge() = geometry(1.5f, false, null)
    @Test fun keypadGeometryExtraLargeDark() = geometry(2f, true, 600)

    private fun geometry(scale: Float, dark: Boolean, height: Int?) {
        val identity = account.copy(displayName = "Alexandra at the Neighbourhood Market", phoneSecurityEnabled = true)
        state.value = NativeAuthenticationState.QuickPin(identity)
        mount(scale, dark, height)
        val toolbar = rule.onNodeWithText("Your shop").fetchSemanticsNode().boundsInRoot
        val key = rule.onNodeWithTag(NativeUiTags.authQuickPinDigit(1))
        key.performScrollTo()
        val bounds = key.fetchSemanticsNode().boundsInRoot
        digit(1)
        for (next in listOf(
            NativeAuthenticationState.QuickPin(identity, busy = true),
            NativeAuthenticationState.QuickPin(identity, error = "That PIN did not unlock this account. Use your full passphrase or check the selected account."),
            NativeAuthenticationState.QuickPin(identity))) {
            rule.runOnIdle { state.value = next }; rule.waitForIdle()
            assertEquals("Toolbar stays mounted", toolbar, rule.onNodeWithText("Your shop").fetchSemanticsNode().boundsInRoot)
            assertEquals("Feedback cannot move keypad", bounds, key.fetchSemanticsNode().boundsInRoot)
        }
        if (scale == 1f) rule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).assertIsDisplayed()
        rule.onNodeWithTag(NativeUiTags.AuthQuickPinUsePassphrase).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(NativeUiTags.AuthPhoneSecurityMethod).performScrollTo().assertIsDisplayed()
        if (scale == 1f) key.performScrollTo()
        NativeEvidenceCapture.captureWholeWindow(rule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "auth", scenario = "quick-pin-keypad-$scale-${if (dark) "dark" else "light"}",
            workflowId = "NativeQuickPinContractTest", fontScale = scale.toString(),
            theme = if (dark) "dark" else "light", note = "Keypad fixture; large text may be scrolled. Owner review pending."))
        rule.runOnIdle { assertFalse(NativeLayoutImeEvidence.imeInsets(rule.activity).isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())) }
    }
}
