package com.saaf.milkshop

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerSyncFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncRebaselineHoldEntity
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.sync.NativeSyncHold
import com.saaf.milkshop.sync.NativeSyncStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class OwnerSyncAttentionUiTest {
    @get:Rule val rule = createComposeRule()
    private val identity = NativeSignedInIdentity(1L, "review.owner", "Review Owner", AppRole.OWNER,
        RolePermissions.permissions(AppRole.OWNER))

    @Test fun badgeLightDefaultKeepsChromeStable() = geometry(1f, false)
    @Test fun badgeDarkDoubleTextKeepsChromeStable() = geometry(2f, true)

    private fun geometry(scale: Float, dark: Boolean) {
        var count by mutableStateOf(0)
        var selected by mutableStateOf(OwnerSection.Overview)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                SaafTheme(darkTheme = dark) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.testTag("review_header")) {
                            CashierScreenHeader(title = selected.name, displayName = "Synthetic shop",
                                roleLabel = "Owner", businessDate = "2026-10-15", postingOpen = true)
                        }
                        Spacer(Modifier.weight(1f))
                        OwnerNavigation(selected, syncAttention = OwnerSyncAttention(count), onNavigate = { selected = it })
                    }
                }
            }
        }
        val header = rule.onNodeWithTag("review_header").fetchSemanticsNode().boundsInRoot
        val nav = rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        for (next in listOf(0, 1, 7, 120, 0)) {
            rule.runOnIdle { count = next }
            for (section in OwnerSection.entries) {
                rule.runOnIdle { selected = section }
                assertEquals(header, rule.onNodeWithTag("review_header").fetchSemanticsNode().boundsInRoot)
                assertEquals(nav, rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
                rule.onNodeWithTag(NativeUiTags.OwnerNavMore).assert(SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription, OwnerSyncAttention(next).description))
                if (next == 0) rule.onNodeWithTag(NativeUiTags.OwnerSyncReviewBadge, useUnmergedTree = true).assertDoesNotExist()
                else {
                    val badge = rule.onNodeWithTag(NativeUiTags.OwnerSyncReviewBadge, useUnmergedTree = true)
                    // Navigation merges badge descendants; inspect the real overlay node separately.
                    badge.assertIsDisplayed()
                    rule.onNodeWithText(if (next > 99) "99+" else "$next", useUnmergedTree = true).assertIsDisplayed()
                    val bounds = badge.getUnclippedBoundsInRoot()
                    val item = rule.onNodeWithTag(NativeUiTags.OwnerNavMore).getUnclippedBoundsInRoot()
                    assertTrue("Badge has layout area", bounds.right > bounds.left && bounds.bottom > bounds.top)
                    assertTrue("Badge fits navigation item", bounds.left >= item.left && bounds.right <= item.right &&
                        bounds.top >= item.top && bounds.bottom <= item.bottom)
                }
            }
        }
        rule.runOnIdle { count = 120 }
        capture("badge-$scale-$dark")
    }

    @Test fun disconnectedHoldsStayVisibleAndCannotResolve() {
        var resolutions = 0
        val hold = NativeSyncHold("products", "synthetic-held-product", "other-device", "2026-10-15", "Full cream milk")
        rule.setContent {
            SaafTheme {
                OwnerSyncScreen(snackbarHostState = remember { SnackbarHostState() }, identity = identity,
                    context = NativeOwnerMoreContext("2026-10-15", true, "Synthetic shop"), status = null,
                    shopChoices = emptyList(), ownerChoices = emptyList(), busy = false, error = null, notice = null,
                    onRefresh = {}, onEnroll = { _, _, _ -> }, onEnrollInShop = { _, _, _, _, _ -> },
                    onEnrollAs = { _, _, _, _ -> }, onSyncNow = {}, onUnenroll = {}, holds = listOf(hold),
                    onResolveHold = { _, _, _ -> resolutions++ },
                    hostedScaffold = { _, mod, _, content -> Box(mod) { content(PaddingValues()) } })
            }
        }
        rule.onNodeWithTag(NativeUiTags.OwnerSyncReconnectReview).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldKeep).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldTake).performScrollTo().assertIsNotEnabled()
        assertEquals(0, resolutions)
        capture("disconnected-review")
    }

    @Test fun sessionReplacementCancelsOldObservationAndFailureCanRetry() {
        val counts = MutableStateFlow(3)
        val nextCounts = MutableStateFlow(0)
        var session by mutableStateOf("owner-one")
        var retry by mutableStateOf(0)
        var failed = true
        var cancelled = 0
        val firstFlow = counts.onCompletion { cancelled++ }
        val secondFlow = flow { if (failed) error("Read unavailable") else emitAll(nextCounts) }
        val holds = flowOf(emptyList<NativeSyncHold>())
        rule.setContent {
            val flow = if (session == "owner-one") firstFlow else secondFlow
            val attention = rememberOwnerSyncAttention(session, flow, holds, false, retry)
            Text(attention.description, Modifier.testTag("attention_description"))
        }
        rule.waitUntil(5000) { rule.onAllNodesWithText("3 entries need review").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { session = "owner-two" }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Sync review unavailable. Open Sync to retry.").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { counts.value = 9 }
        rule.onNodeWithTag("attention_description").assertTextEquals("Sync review unavailable. Open Sync to retry.")
        assertTrue(cancelled > 0)
        rule.runOnIdle { failed = false; retry++ }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Upload entries to your shop's online record").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun localHoldUpdatesReachOpenReviewAndResolveWithoutReload() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = MilkShopDatabase.createInMemory()
        val security = UserSecurityService(db)
        val session = runBlocking {
            security.bootstrapOwner("review.live", "Review Owner", "review-live-passphrase".toCharArray()).session.sessionId
        }
        val facade = NativeOwnerSyncFacade(context, db)
        val counts = facade.observeReviewCount(session, security)
        val holds = facade.observeReviewHolds(session, security)
        var visible by mutableStateOf(true)
        var reviewOpen by mutableStateOf(true)
        rule.setContent {
            if (visible) {
                val attention = rememberOwnerSyncAttention(session, counts, holds, reviewOpen, 0)
                val scope = rememberCoroutineScope()
                SaafTheme {
                    Column(Modifier.fillMaxSize()) {
                        Text(attention.description, Modifier.testTag("live_attention"))
                        if (reviewOpen) Box(Modifier.weight(1f)) {
                            OwnerSyncScreen(snackbarHostState = remember { SnackbarHostState() }, identity = identity,
                                context = NativeOwnerMoreContext("2026-10-15", true, "Synthetic shop"),
                                status = NativeSyncStatus("prod", "Synthetic shop", "owner", true, true, 1, 0),
                                shopChoices = emptyList(), ownerChoices = emptyList(), busy = false,
                                error = null, notice = null, onRefresh = {}, onEnroll = { _, _, _ -> },
                                onEnrollInShop = { _, _, _, _, _ -> }, onEnrollAs = { _, _, _, _ -> },
                                onSyncNow = {}, onUnenroll = {}, holds = attention.holds.orEmpty(),
                                onResolveHold = { table, uid, keep -> scope.launch { facade.resolveHold(table, uid, keep) } },
                                hostedScaffold = { _, mod, _, content -> Box(mod) { content(PaddingValues()) } })
                        } else Spacer(Modifier.weight(1f))
                        OwnerNavigation(OwnerSection.More, syncAttention = attention, onNavigate = {})
                    }
                }
            }
        }
        try {
            rule.waitUntil(5000) { rule.onAllNodesWithText("Upload entries to your shop's online record").fetchSemanticsNodes().isNotEmpty() }
            val product = runBlocking { db.productDao().findByName("Milk")!! }
            val before = runBlocking { db.syncOutboxDao().count() }
            runBlocking {
                db.syncOutboxDao().setHeld("products", product.uid, true)
                db.syncRebaselineHoldDao().upsert(SyncRebaselineHoldEntity("products", product.uid,
                    "{}", "other", 1, "2026-10-15"))
            }
            rule.waitUntil(5000) { rule.onAllNodesWithText("1 entry needs review").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldKeep).performScrollTo().assertIsEnabled()
            capture("live-held-entry")
            rule.runOnIdle { reviewOpen = false }
            rule.onNodeWithTag(NativeUiTags.OwnerNavMore).assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, "1 entry needs review"))
            val overlay = rule.onNodeWithTag(NativeUiTags.OwnerSyncReviewBadge, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue(overlay.right > overlay.left && overlay.bottom > overlay.top)
            capture("live-held-away-from-review")
            rule.runOnIdle { reviewOpen = true }
            rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldKeep).performScrollTo().performClick()
            rule.waitUntil(5000) { rule.onAllNodesWithTag(NativeUiTags.OwnerSyncReviewBadge, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            assertEquals(0, runBlocking { db.syncRebaselineHoldDao().count() })
            assertEquals(before, runBlocking { db.syncOutboxDao().count() })
            capture("live-review-resolved")
        } finally { rule.runOnIdle { visible = false }; rule.waitForIdle(); db.close() }
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "sync-attention-$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
