package com.saaf.milkshop

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.cashclose.*
import com.saaf.milkshop.application.operations.*
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
import com.saaf.milkshop.nativeui.common.LocalShopName
import com.saaf.milkshop.nativeui.operations.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Three named visual tails, current phone font scale/theme, no business writes. */
class StaffVisualTailTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun overviewFactsAndLatestActivityAreReadable() {
        rule.activity.runOnUiThread { rule.activity.enableEdgeToEdge() }
        rule.setContent { SaafTheme { CompositionLocalProvider(LocalShopName provides "Shop Day QA") {
            OwnerOverviewScreen(remember { SnackbarHostState() },
                NativeOwnerOverviewState.Ready(NativeOwnerOverviewSnapshot(
                    businessDate = "2026-10-14", postingOpen = true, shopName = "Shop Day QA",
                    todaySalesPaisa = 56500, estimatedGrossPaisa = 2956,
                    coverageComplete = true, coverageWarning = null, expectedCashPaisa = 933500,
                    customersDuePaisa = 18000, suppliersDuePaisa = 1657692,
                    stock = listOf(NativeOwnerOverviewStockLine("Milk", "78.5 L"),
                        NativeOwnerOverviewStockLine("Yogurt", "16 kg")),
                    attention = listOf(NativeOwnerOverviewAttention(NativeOwnerOverviewAttentionKind.CashCountWaiting,
                        "Cash count waiting", "No drawer count has been submitted for today.", "Review")),
                    latestActivity = NativeOwnerOverviewLatestActivity("Sale · #S-1842", "Day Cashier · Cash", "PKR 285.00", "Completed"),
                )), OwnerSection.Overview, onRetry = {}, ownScaffold = { s, m, content ->
                    OwnerScaffold(s, OwnerSection.Overview, onNavigate = {}, modifier = m, content = content)
                })
        } } }
        rule.onNodeWithTag(NativeUiTags.OwnerOverviewExpectedCash).assertIsDisplayed()
        capture("overview-top")
        rule.onNodeWithTag(NativeUiTags.OwnerOverviewLatestActivity).performScrollTo().assertIsDisplayed()
        complete("Sale · #S-1842"); complete("Day Cashier · Cash"); complete("PKR 285.00")
        complete("Completed")
        capture("overview-latest")
    }

    @Test fun operatorTodayPhysicalFactsAreReadable() {
        rule.activity.runOnUiThread { rule.activity.enableEdgeToEdge() }
        rule.setContent { SaafTheme { CompositionLocalProvider(LocalShopName provides "Shop Day QA") {
            OperationsMoreScreen(remember { SnackbarHostState() }, identity(AppRole.OPERATOR),
                NativeOperationsMoreState.Ready(NativeOperationsMoreContext("2026-10-14", true),
                    destination = NativeOperationsMoreDestination.TodayWork, todayRecords = listOf(
                        NativeOperationsWorkRecord(1842, "Receive milk", "100 L · Day Supplier · Fat 4% · LR 28", "08:15"),
                        NativeOperationsWorkRecord(1843, "Make yogurt", "20 L milk → 17 kg yogurt", "09:30"),
                        NativeOperationsWorkRecord(1844, "Spoilage", "Milk · 0.5 L · Damaged container", "10:05"),
                    )), onRetry = {}, onOpenTodayWork = {}, onOpenPersonalSecurity = {}, onBackToMenu = {},
                onRequestRemoveQuickUnlock = {}, onCancelRemoveQuickUnlock = {}, onRemoveQuickUnlock = {},
                onEnrollQuickPin = { _, _ -> }, onEnrollPhoneSecurity = { _, _ -> }, onSwitchAccount = {},
                onSignOut = {}, moreScaffold = { snackbar, modifier, content ->
                    WorkScaffold(snackbar, OperationsSection.More, onNavigate = {}, modifier = modifier) { padding ->
                        androidx.compose.foundation.layout.Column(
                            androidx.compose.ui.Modifier.fillMaxSize().padding(padding)) {
                            com.saaf.milkshop.nativeui.sale.CashierScreenHeader("Today's work", "Day Operator",
                                "Operator", "2026-10-14", true)
                            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) {
                                content(androidx.compose.foundation.layout.PaddingValues(0.dp))
                            }
                        }
                    }
                })
        } } }
        rule.onNodeWithTag(NativeUiTags.OpsMoreTodayWorkScreen).assertIsDisplayed()
        complete("100 L · Day Supplier · Fat 4% · LR 28")
        complete("20 L milk → 17 kg yogurt")
        capture("operator-today")
    }

    @Test fun savedCashCloseReceiptSeparatesCountExpectedAndDifference() {
        rule.activity.runOnUiThread { rule.activity.enableEdgeToEdge() }
        rule.setContent { SaafTheme { CompositionLocalProvider(LocalShopName provides "Shop Day QA") {
            OwnerCashCloseEntryScreen(remember { SnackbarHostState() }, identity(AppRole.OWNER),
                NativeCashCloseState.Ready(NativeCashCloseContext("2026-10-14", false, 1L,
                    "Cash", 933500L, null), receipt = NativeCashCloseReceipt("2026-10-14", 3L,
                    933000L, 933500L, -500L)), onRetry = {}, onRecordCount = {}, onCloseDay = {},
                onReopenDay = { _, _ -> }, onBackToWork = {}, ownScaffold = { s, m, content ->
                    OwnerScaffold(s, OwnerSection.Work, onNavigate = {}, modifier = m, content = content)
                })
        } } }
        for (text in listOf("You counted", "App expected", "Difference", "Close record")) complete(text)
        rule.onNodeWithTag(NativeUiTags.CashCloseDone).performScrollTo().assertIsDisplayed()
        capture("count-close-receipt")
    }

    private fun identity(role: AppRole) = NativeSignedInIdentity(1L, "day.${role.wireValue}",
        "Day ${role.wireValue.replaceFirstChar { it.uppercase() }}", role, RolePermissions.permissions(role))

    private fun complete(text: String) {
        val node = rule.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        capture("check-" + text.replace(Regex("[^a-zA-Z0-9]"), "-").take(32))
        val metadata = org.json.JSONObject().put("text", text).put("layouts", org.json.JSONArray(layouts.map { layout ->
            org.json.JSONObject().put("width", layout.size.width).put("heightOverflow", layout.didOverflowHeight)
                .put("widthOverflowFlag", layout.didOverflowWidth).put("lastLineEnd", layout.getLineEnd(layout.lineCount - 1))
                .put("rightEdges", org.json.JSONArray((0 until layout.lineCount).map { layout.getLineRight(it).toDouble() }))
        }))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "staff-layouts.jsonl").appendText(metadata.toString() + "\n")
        // Pixel rounding can set didOverflowWidth for a fractional glyph edge.
        // Verify the actual last character, ellipsis and measured edges with
        // the same one-physical-pixel tolerance as existing presentation tests.
        assertTrue("Text layout check: $metadata", layouts.isNotEmpty() && layouts.all {
            !it.didOverflowHeight && it.getLineEnd(it.lineCount - 1) == text.length &&
                (0 until it.lineCount).all { line -> !it.isLineEllipsized(line) && it.getLineRight(line) <= it.size.width + 1f }
        })
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "staff-visuals").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
