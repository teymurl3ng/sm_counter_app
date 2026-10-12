package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.saaf.milkshop.application.authentication.NativeAccountSummary
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.admin.*
import com.saaf.milkshop.nativeui.auth.*
import com.saaf.milkshop.nativeui.common.LocalShopName
import com.saaf.milkshop.nativeui.manager.*
import com.saaf.milkshop.nativeui.operations.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.theme.nativeVisualColors
import com.saaf.milkshop.nativeui.welcome.WelcomeScreen
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
    @Test fun workHeaderDarkDefault() = headers(true, 1f)
    @Test fun workHeaderDarkExtraLarge() = headers(true, 2f)
    @Test fun allRoleFootersDefault() = footers(1f)
    @Test fun allRoleFootersLarge() = footers(1.5f)
    @Test fun allRoleFootersExtraLarge() = footers(2f)
    @Test fun accessStepsDefault() = access(1f)
    @Test fun accessStepsLarge() = access(1.5f)
    @Test fun accessStepsExtraLarge() = access(2f)
    @Test fun welcomeLight() = welcome(false, 1f, 640)
    @Test fun welcomeDark() = welcome(true, 1f, 640)
    @Test fun welcomeLargeText() = welcome(false, 1.5f, 540)
    @Test fun welcomeExtraLargeSmallViewport() = welcome(false, 2f, 420)
    @Test fun accountListDefault() = picker(false, 1f)
    @Test fun accountListLargeText() = picker(false, 1.5f)
    @Test fun accountListExtraLargeDark() = picker(true, 2f)

    private fun headers(dark: Boolean, fontScale: Float) {
        prepareWindow(dark, workHeader = true)
        var title by mutableStateOf("More")
        var name by mutableStateOf("Cashier")
        var chip by mutableStateOf(true)
        var open by mutableStateOf(true)
        var headerColor = Color.Unspecified
        var actionColor = Color.Unspecified
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale),
                LocalShopName provides "A shop with a very long registered business name") {
                SaafTheme(darkTheme = dark) {
                    headerColor = nativeVisualColors.header
                    actionColor = MaterialTheme.colorScheme.primary
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        Box(Modifier.testTag("test-header")) {
                            CashierScreenHeader(title, name, "Administrator", "2026-10-11", open,
                                onDayStatusClick = {}, showDayChip = chip)
                        }
                        Box(Modifier.weight(1f).testTag("test-body"))
                        OwnerNavigation(OwnerSection.More) { }
                    }
                }
            }
        }
        rule.waitForIdle()
        if (dark) assertTrue("Night chrome is a dark surface, preserving a brighter action accent", headerColor.luminance() < .1f && actionColor.luminance() > headerColor.luminance())
        listOf("More", "Cashier", "Administrator · 11 Oct 2026").forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNode(hasText(text) and hasAnyAncestor(hasTestTag("test-header")), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val foreground = layouts.single().layoutInput.style.color.compositeOver(headerColor).luminance()
            val background = headerColor.luminance()
            assertTrue("Readable header role: $text", (maxOf(foreground, background) + .05f) / (minOf(foreground, background) + .05f) >= 4.5f)
        }
        val baseline = rule.onNodeWithTag("test-body").fetchSemanticsNode().boundsInRoot
        val headerHeight = rule.onNodeWithTag("test-header").fetchSemanticsNode().boundsInRoot.height
        val statusBounds = rule.onNodeWithContentDescription("Day status: Open").fetchSemanticsNode().boundsInRoot
        for (nextOpen in listOf(true, false, true)) {
            rule.runOnIdle { open = nextOpen }
            rule.waitForIdle()
            val label = if (nextOpen) "Open" else "Closed"
            val target = rule.onNodeWithContentDescription("Day status: $label").fetchSemanticsNode().boundsInRoot
            val text = rule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals("Day status cannot resize or move its target", statusBounds, target)
            assertEquals("Status text is horizontally centered", target.center.x, text.center.x, 1f)
            assertEquals("Status text is vertically centered", target.center.y, text.center.y, 1f)
            assertTrue("Day status retains a 48dp touch target", target.height >= 48f * rule.activity.resources.displayMetrics.density - 1f)
            assertTextHeight(label)
            val statusLayout = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(statusLayout) }
            val measured = statusLayout.single()
            assertTrue("$label must fit horizontally: box=${measured.size}, paragraph=${measured.multiParagraph.width}x${measured.multiParagraph.height}",
                !measured.didOverflowWidth)
            assertEquals("$label must retain its last character", label.length,
                statusLayout.single().getLineEnd(0, visibleEnd = true))
            assertEquals(baseline, rule.onNodeWithTag("test-body").fetchSemanticsNode().boundsInRoot)
            if (!nextOpen) capture("status-closed-${if (dark) "dark" else "light"}-$fontScale", dark, fontScale)
        }
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
        prepareWindow(false)
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
        val shop = rule.onNodeWithText("Your shop").fetchSemanticsNode().boundsInRoot
        val identity = NativeSignedInIdentity(9L, "demo.owner", account.displayName, AppRole.OWNER, RolePermissions.permissions(AppRole.OWNER))
        listOf(
            NativeAuthenticationState.Loading to "Loading accounts",
            NativeAuthenticationState.AccountPicker(listOf(account), cancelReturnsToSession = true) to "Switch account",
            NativeAuthenticationState.Passphrase(account) to "Sign in",
            NativeAuthenticationState.MethodPicker(account) to "Sign-in method",
            NativeAuthenticationState.QuickPin(account) to "Quick PIN",
            NativeAuthenticationState.OwnerSetup() to "Create Owner",
            NativeAuthenticationState.RecoveryCode(identity, "DEMO-CODE-FOR-LAYOUT-ONLY") to "Recovery code",
            NativeAuthenticationState.StartupFailure("A long startup failure message that belongs in the body rather than the page heading.") to "Accounts unavailable",
        ).forEach { (step, label) ->
            rule.runOnIdle { state = step }
            rule.waitForIdle()
            // A repeated button label can have empty clipped bounds at large text sizes.
            val pageTitle = rule.onNode(hasText(label) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            pageTitle.assertIsDisplayed()
            assertEquals("$label keeps the page heading in the same slot", top,
                pageTitle.fetchSemanticsNode().boundsInRoot.top, 1f)
            assertEquals("Back availability and state cannot move the shop toolbar", shop,
                rule.onNodeWithText("Your shop").fetchSemanticsNode().boundsInRoot)
        }
        capture("access-fixed-band-$fontScale", false, fontScale)
    }

    private fun welcome(dark: Boolean, fontScale: Float, viewportDp: Int) {
        prepareWindow(dark)
        val shop = if (fontScale == 1f) "Willow Market" else "Willow Market and Neighbourhood Store"
        var continued = 0
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale), LocalShopName provides shop) {
                SaafTheme(darkTheme = dark) {
                    Box(Modifier.fillMaxWidth().height(viewportDp.dp)) {
                        WelcomeScreen(onContinue = { continued++ })
                    }
                }
            }
        }
        rule.waitForIdle()
        val action = rule.onNodeWithTag(NativeUiTags.WelcomeContinue)
        action.assertIsDisplayed().assertIsEnabled()
        val footer = action.fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText(shop).performScrollTo()
        assertEquals("Short and long shop names must stay centered above Continue", footer.center.x,
            rule.onNodeWithText(shop).fetchSemanticsNode().boundsInRoot.center.x, 1f)
        assertTextHeight(shop)
        assertTextHeight("Continue")
        assertEquals("Scrolling the shop identity cannot move Continue", footer, action.fetchSemanticsNode().boundsInRoot)
        rule.onNodeWithText("Welcome").assertDoesNotExist()
        capture("welcome-${if (dark) "dark" else "light"}-$fontScale-$viewportDp", dark, fontScale)
        action.performClick()
        assertEquals("Welcome retains its Continue callback", 1, continued)
    }

    private fun picker(dark: Boolean, fontScale: Float) {
        prepareWindow(dark)
        val shop = "Willow Market and Neighbourhood Store"
        val accounts = List(20) { index -> NativeAccountSummary(index + 1L, "layout.$index",
            "Willow Williams $index", AppRole.entries[index % AppRole.entries.size],
            quickPinEnabled = true, phoneSecurityEnabled = true) }
        var state by mutableStateOf<NativeAuthenticationState>(NativeAuthenticationState.AccountPicker(emptyList()))
        var selected: NativeAccountSummary? = null
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale), LocalShopName provides shop) {
                SaafTheme(darkTheme = dark) {
                    NativeAuthenticationHost(state, onRetry = {}, onBackToWelcome = {},
                        onSelectAccount = { selected = it; state = NativeAuthenticationState.Passphrase(it) },
                        onChooseAnotherAccount = { state = NativeAuthenticationState.AccountPicker(accounts) },
                        onBackFromPassphrase = {}, onUseQuickPin = {}, onUsePhoneSecurity = {},
                        onUseFullPassphrase = {}, onBackFromQuickPin = {}, onQuickPinSignIn = { it.fill('\u0000') },
                        onSignIn = { it.fill('\u0000') }, onBootstrapOwner = { _, _, a, b -> a.fill('\u0000'); b.fill('\u0000') },
                        onAcknowledgeRecoveryCode = {})
                }
            }
        }
        rule.onNodeWithText("No active account is ready to sign in.").assertIsDisplayed()
        val toolbar = rule.onNodeWithText(shop).fetchSemanticsNode().boundsInRoot
        for (count in listOf(1, 6, 20)) {
            rule.runOnIdle { state = NativeAuthenticationState.AccountPicker(accounts.take(count)) }
            rule.waitForIdle()
            assertEquals("Account count cannot move the toolbar", toolbar, rule.onNodeWithText(shop).fetchSemanticsNode().boundsInRoot)
            val last = rule.onNodeWithTag(NativeUiTags.authAccount(accounts[count - 1].loginName))
            last.performScrollTo().assertIsDisplayed()
            assertTextHeight(accounts[count - 1].displayName)
            val row = last.fetchSemanticsNode().boundsInRoot
            val text = rule.onNodeWithText(accounts[count - 1].displayName, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Account text needs breathing room inside the row", text.left > row.left + 24f)
            if (count == 6) capture("picker-six-${if (dark) "dark" else "light"}-$fontScale", dark, fontScale)
        }
        rule.onNodeWithTag(NativeUiTags.authAccount(accounts.last().loginName)).performClick()
        assertEquals("Last account remains selectable after scrolling", accounts.last(), selected)
        rule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(NativeUiTags.AuthSignIn).performScrollTo().assertIsDisplayed()
        assertEquals("Sign-in keeps the same toolbar", toolbar, rule.onNodeWithText(shop).fetchSemanticsNode().boundsInRoot)
        capture("sign-in-${if (dark) "dark" else "light"}-$fontScale", dark, fontScale)
    }

    private fun prepareWindow(dark: Boolean, workHeader: Boolean = false) {
        rule.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                isAppearanceLightStatusBars = !workHeader && !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    private fun capture(scenario: String, dark: Boolean, fontScale: Float) {
        NativeEvidenceCapture.captureWholeWindow(rule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "ux", scenario = "chrome-$scenario", workflowId = "NativeChromeGeometryTest",
            fontScale = fontScale.toString(),
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
