package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Two synthetic setup inputs; the callback is a spy, not an enrollment service. */
class NativePersonalSecurityFieldsTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    @Test fun secureSetupPreservesValidationAndClearsSubmittedSecrets() {
        var submitted=0
        rule.setContent {
            SaafTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    NativePersonalSecurityPanel(93,"Security Fixture",NativeMoreSecurityStatus(false,false,false,"Phone security"),
                        false,null,false,NativePersonalSecurityTags("status","passphrase","pin","save","phone","remove","confirm","error"),
                        {},{}, { it.fill('\u0000');error("No removal") },
                        { passphrase,pin ->
                            assertEquals("fixture-only",passphrase.concatToString())
                            assertEquals("123456789012",pin.concatToString())
                            submitted++;passphrase.fill('\u0000');pin.fill('\u0000')
                        },{ a,b -> a.fill('\u0000');b?.fill('\u0000');error("No biometric enrollment") })
                }
            }
        }
        val passphrase=rule.onNodeWithTag("passphrase")
        val pin=rule.onNodeWithTag("pin")
        val save=rule.onNodeWithTag("save")
        passphrase.performScrollTo().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).performTextInput("fixture-only")
        pin.performScrollTo().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).performTextInput("123")
        save.performScrollTo().assertIsNotEnabled()
        pin.performScrollTo().performTextInput("4")
        save.performScrollTo().assertIsEnabled()
        pin.performScrollTo().performTextInput("x")
        pin.performTextInput("56789012")
        pin.performTextInput("3")
        save.performScrollTo().performClick()
        assertEquals(1,submitted)
        save.assertIsNotEnabled()
    }
}
