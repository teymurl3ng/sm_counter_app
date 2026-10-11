package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.saaf.milkshop.application.authentication.NativeAccountSummary
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.admin.*
import com.saaf.milkshop.nativeui.auth.*
import com.saaf.milkshop.nativeui.common.LocalShopName
import com.saaf.milkshop.nativeui.manager.*
import com.saaf.milkshop.nativeui.operations.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.qa.NativeEvidenceCapture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real shared bars: destination wording, identity, status and selected tabs cannot move the body. */
class NativeChromeGeometryTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun workHeaderDefault() = headers(false, 1f)
    @Test fun workHeaderLarge() = headers(false, 1.5f)
    @Test fun workHeaderExtraLarge() = headers(false, 2f)
    @Test fun workHeaderDarkLarge() = headers(true, 1.5f)
    @Test fun allRoleFootersDefault() = footers(1f)
    @Test fun allRoleFootersLarge() = footers(1.5f)
    @Test fun allRoleFootersExtraLarge() = footers(2f)
    @Test fun accessStepsDefault() = access(1f)
    @Test fun accessStepsLarge() = access(1.5f)

    private fun headers(dark: Boolean, fontScale: Float) {
        rule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        var title by mutableStateOf("More")
        var name by mutableStateOf("Cashier")
        var chip by mutableStateOf(true)
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale),
                LocalShopName provides "A shop with a very long registered business name") {
                SaafTheme(darkTheme = dark) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        Box(Modifier.testTag("test-header")) {
                            CashierScreenHeader(title, name, "Administrator", "2026-10-11", true, showDayChip = chip)
                        }
                        Box(Modifier.weight(1f).testTag("test-body"))
                        OwnerNavigation(OwnerSection.More) { }
                    }
                }
            }
        }
        rule.waitForIdle()
        val baseline = rule.onNodeWithTag("test-body").fetchSemanticsNode().boundsInRoot
        val headerHeight = rule.onNodeWithTag("test-header").fetchSemanticsNode().boundsInRoot.height
        listOf("Collect payment", "Cash count and close", "Backup & restore", "An exceptionally long screen title that must not grow the band").forEachIndexed { i, label ->
            rule.runOnIdle { title = label; name = "A person with an exceptionally long account display name"; chip = i % 2 == 0 }
            rule.waitForIdle()
            assertEquals("Header height must not depend on title/account/status", headerHeight,
                rule.onNodeWithTag("test-header").fetchSemanticsNode().boundsInRoot.height, 1f)
            assertEquals("Changing header text must not move content", baseline,
                rule.onNodeWithTag("test-body").fetchSemanticsNode().boundsInRoot)
            assertTextHeight(name)
            assertTextHeight("Administrator · 11 Oct 2026")
        }
        capture("header-${if (dark) "dark" else "light"}-$fontScale", dark, fontScale)
    }

    private fun footers(fontScale: Float) {
        var role by mutableIntStateOf(0)
        var selected by mutableIntStateOf(0)
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                SaafTheme(darkTheme = false) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).testTag("test-body"))
                        Box(Modifier.testTag("test-footer")) {
                            when (role) {
                                0 -> OwnerNavigation(OwnerSection.entries[selected]) { selected = it.ordinal }
                                1 -> AdminNavigation(AdminSection.entries[selected]) { selected = it.ordinal }
                                2 -> ManagerNavigation(ManagerSection.entries[selected]) { selected = it.ordinal }
                                3 -> CashierNavigation(CashierSection.entries[selected]) { selected = it.ordinal }
                                else -> OperationsNavigation(OperationsSection.entries[selected]) { selected = it.ordinal }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        val baseline = rule.onNodeWithTag("test-body").fetchSemanticsNode().boundsInRoot
        val height = rule.onNodeWithTag("test-footer").fetchSemanticsNode().boundsInRoot.height
        repeat(5) { nextRole -> repeat(4) { nextTab ->
            rule.runOnIdle { role = nextRole; selected = nextTab }
            rule.waitForIdle()
            assertEquals("Role/tab changes cannot resize the footer", height,
                rule.onNodeWithTag("test-footer").fetchSemanticsNode().boundsInRoot.height, 1f)
            assertEquals("Role/tab changes cannot move body bounds", baseline,
                rule.onNodeWithTag("test-body").fetchSemanticsNode().boundsInRoot)
            listOf(
                listOf("Overview", "Work", "Records", "More"),
                listOf("Work", "People", "Records", "More"),
                listOf("Work", "Close", "Records", "More"),
                listOf("Sell", "Collect", "Activity", "More"),
                listOf("Work", "Deliveries", "Attendance", "More"),
            )[nextRole].forEach(::assertTextHeight)
        } }
        capture("all-role-footer-$fontScale", false, fontScale)
    }

    private fun access(fontScale: Float) {
        val account = NativeAccountSummary(9L, "demo.owner", "An unusually long account display name", AppRole.OWNER)
        var state by mutableStateOf<NativeAuthenticationState>(NativeAuthenticationState.AccountPicker(listOf(account)))
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                SaafTheme(darkTheme = false) {
                    NativeAuthenticationHost(
                        state = state, onRetry = {}, onBackToWelcome = {}, onSelectAccount = {},
                        onChooseAnotherAccount = {}, onBackFromPassphrase = {}, onUseQuickPin = {},
                        onUsePhoneSecurity = {}, onUseFullPassphrase = {}, onBackFromQuickPin = {},
                        onQuickPinSignIn = { it.fill('\u0000') }, onSignIn = { it.fill('\u0000') },
                        onBootstrapOwner = { _, _, a, b -> a.fill('\u0000'); b.fill('\u0000') },
                        onAcknowledgeRecoveryCode = {},
                    )
                }
            }
        }
        rule.waitForIdle()
        val top = rule.onNodeWithText("Choose account").fetchSemanticsNode().boundsInRoot.top
        val identity = NativeSignedInIdentity(9L, "demo.owner", account.displayName, AppRole.OWNER, RolePermissions.permissions(AppRole.OWNER))
        listOf(
            NativeAuthenticationState.Passphrase(account) to "Sign in",
            NativeAuthenticationState.MethodPicker(account) to "Sign-in method",
            NativeAuthenticationState.QuickPin(account) to "Quick PIN",
            NativeAuthenticationState.OwnerSetup() to "Create Owner",
            NativeAuthenticationState.RecoveryCode(identity, "DEMO-CODE-FOR-LAYOUT-ONLY") to "Recovery code",
            NativeAuthenticationState.StartupFailure("A long startup failure message that belongs in the body rather than the page heading.") to "Accounts unavailable",
        ).forEach { (step, label) ->
            rule.runOnIdle { state = step }
            rule.waitForIdle()
            // Select the topmost matching text: inputs/buttons may repeat the concise page name.
            val actual = rule.onAllNodesWithText(label).fetchSemanticsNodes().minOf { it.boundsInRoot.top }
            assertEquals("Access steps keep the title in the same slot", top, actual, 1f)
        }
        capture("access-fixed-band-$fontScale", false, fontScale)
    }

    private fun capture(scenario: String, dark: Boolean, fontScale: Float) {
        NativeEvidenceCapture.captureWholeWindow(rule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "ux", scenario = "chrome-$scenario", workflowId = "NativeChromeGeometryTest",
            theme = if (dark) "dark" else "light", note = "Shared chrome fixture. Local text scale=$fontScale; Owner review pending."))
    }

    private fun assertTextHeight(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("$text must expose its actual text layout", layouts.isNotEmpty())
        // Horizontal ellipsis is allowed in chrome; losing a line's height is not.
        assertTrue("$text must not be vertically clipped: paragraph=${layouts.single().multiParagraph.height}, box=${layouts.single().size.height}",
            layouts.single().multiParagraph.height <= layouts.single().size.height + 1f)
    }
}
