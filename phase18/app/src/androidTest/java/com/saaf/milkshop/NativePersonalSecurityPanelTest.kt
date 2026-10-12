package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.admin.AdminMorePersonalSecurityScreen
import com.saaf.milkshop.nativeui.manager.ManagerMorePersonalSecurityScreen
import com.saaf.milkshop.nativeui.common.NativePersonalSecurityPanel
import com.saaf.milkshop.nativeui.common.NativePersonalSecurityTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NativePersonalSecurityPanelTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val tags = NativePersonalSecurityTags(
        status = "security_status", passphrase = "security_passphrase", pin = "security_pin",
        savePin = "security_save", enablePhone = "security_phone", remove = "security_remove",
        confirmRemove = "security_confirm", error = "security_error",
    )

    @Test fun twelveDigitPinKeepsLeadingZeroesAndRequiresFullPassphrase() {
        var saved: String? = null
        var confirmedBy: String? = null
        content(onEnrollPin = { passphrase, pin ->
            saved = String(pin)
            confirmedBy = String(passphrase)
        })
        rule.onNodeWithTag(tags.savePin).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(tags.passphrase).performTextInput("full passphrase")
        rule.onNodeWithTag(tags.pin).performTextInput("000456789012")
        rule.onNodeWithTag(tags.savePin).performScrollTo().assertIsEnabled().performClick()
        assertEquals("000456789012", saved)
        assertEquals("full passphrase", confirmedBy)
    }

    @Test fun phoneSecurityRequiresPinWhenPinWasAlreadyEnabledAndCancelPreservesSetup() {
        var requested = false
        var cancelled = false
        var enrolledPin: String? = null
        var confirmedBy: String? = null
        var confirm by mutableStateOf(false)
        content(confirmRemove = { confirm }, onRequestRemove = { requested = true; confirm = true },
            onCancelRemove = { cancelled = true; confirm = false },
            onEnrollPhone = { passphrase, pin ->
                enrolledPin = pin?.let(::String)
                confirmedBy = String(passphrase)
            })
        rule.onNodeWithTag(tags.passphrase).performTextInput("full passphrase")
        rule.onNodeWithTag(tags.enablePhone).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(tags.pin).performScrollTo().performTextInput("0042")
        rule.onNodeWithTag(tags.enablePhone).performScrollTo().assertIsEnabled().performClick()
        assertEquals("0042", enrolledPin)
        assertEquals("full passphrase", confirmedBy)
        rule.onNodeWithTag(tags.passphrase).performScrollTo().performTextInput("full passphrase")
        rule.onNodeWithTag(tags.remove).performScrollTo().performClick()
        assertTrue(requested)
        rule.onNodeWithText("Keep Quick unlock").performClick()
        assertTrue(cancelled)
        rule.onNodeWithTag(tags.confirmRemove).assertDoesNotExist()
    }

    @Test fun busyErrorAndLongStatusLightDefault() = busyErrorAndLongStatus(1f, false)
    @Test fun busyErrorAndLongStatusDarkLargeText() = busyErrorAndLongStatus(1.5f, true)
    @Test fun busyErrorAndLongStatusExtraLargeText() = busyErrorAndLongStatus(2f, false)

    @Test fun adminSecurityDestinationRendersGroupedControls() = delegatedSecurity(AppRole.ADMIN)
    @Test fun managerSecurityDestinationRendersGroupedControls() = delegatedSecurity(AppRole.MANAGER)

    private fun delegatedSecurity(role: AppRole) {
        val state = NativeOwnerMoreState.Ready(
            context = NativeOwnerMoreContext("2026-10-12", true, "Test Dairy"),
            security = NativeMoreSecurityStatus(false, false, true, "Phone security"),
        )
        val identity = NativeSignedInIdentity(5L, "test", "Test Employee", role,
            RolePermissions.permissions(role))
        rule.setContent {
            SaafTheme {
                val snackbar = remember { SnackbarHostState() }
                if (role == AppRole.ADMIN) {
                    AdminMorePersonalSecurityScreen(snackbar, identity, state, {}, {}, {}, {},
                        { _, _ -> }, { _, _ -> },
                        admScaffold = { _, _, content -> content(PaddingValues(0.dp)) },
                        modifier = Modifier.safeDrawingPadding())
                } else {
                    ManagerMorePersonalSecurityScreen(snackbar, identity, state, {}, {}, {}, {},
                        { _, _ -> }, { _, _ -> },
                        mgrScaffold = { _, _, content -> content(PaddingValues(0.dp)) },
                        modifier = Modifier.safeDrawingPadding())
                }
            }
        }
        val screen = if (role == AppRole.ADMIN) NativeUiTags.AdminMoreSecurityScreen else NativeUiTags.ManagerMoreSecurityScreen
        val status = if (role == AppRole.ADMIN) NativeUiTags.AdminMoreSecurityStatus else NativeUiTags.ManagerMoreSecurityStatus
        val pin = if (role == AppRole.ADMIN) NativeUiTags.AdminMoreSecurityPin else NativeUiTags.ManagerMoreSecurityPin
        rule.onNodeWithTag(screen).assertIsDisplayed()
        rule.onNodeWithTag(status).assertIsDisplayed()
        rule.onNodeWithTag(pin).performScrollTo().assertIsDisplayed()
    }

    private fun busyErrorAndLongStatus(scale: Float, dark: Boolean) {
        content(saving = true, error = "Passphrase could not be confirmed", scale = scale,
            dark = dark, name = "A long employee account name for the counter")
        rule.onNodeWithTag(tags.status).assertIsDisplayed()
        rule.onNodeWithTag(tags.error).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(tags.savePin).performScrollTo().assertIsNotEnabled()
    }

    private fun content(
        saving: Boolean = false,
        error: String? = null,
        scale: Float = 1f,
        dark: Boolean = false,
        name: String = "Counter Cashier",
        confirmRemove: () -> Boolean = { false },
        onRequestRemove: () -> Unit = {},
        onCancelRemove: () -> Unit = {},
        onEnrollPin: (CharArray, CharArray) -> Unit = { _, _ -> },
        onEnrollPhone: (CharArray, CharArray?) -> Unit = { _, _ -> },
    ) {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                SaafTheme(darkTheme = dark) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                        .padding(16.dp)) {
                        NativePersonalSecurityPanel(
                            identityKey = 5L,
                            identityName = name,
                            security = NativeMoreSecurityStatus(true, false, true, "Phone security"),
                            saving = saving,
                            error = error,
                            confirmRemove = confirmRemove(),
                            tags = tags,
                            onRequestRemove = onRequestRemove,
                            onCancelRemove = onCancelRemove,
                            onRemove = {},
                            onEnrollPin = onEnrollPin,
                            onEnrollPhone = onEnrollPhone,
                        )
                    }
                }
            }
        }
    }
}
