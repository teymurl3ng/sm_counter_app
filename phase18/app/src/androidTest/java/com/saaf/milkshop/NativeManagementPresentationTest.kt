package com.saaf.milkshop

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.owner.OwnerSyncScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.sync.NativeSyncHold
import com.saaf.milkshop.sync.NativeSyncStatus
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NativeManagementPresentationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun syncLightDefault() = syncGeometry(1f, false)
    @Test fun syncLightLarge() = syncGeometry(1.5f, false)
    @Test fun syncDarkExtraLarge() = syncGeometry(2f, true)

    private fun syncGeometry(scale: Float, dark: Boolean) {
        val shop = "Northern community market and full cream dairy counter"
        val account = "counter.delivery.north.district"
        val holdTitle = "Extra thick full cream yoghurt for the northern market route"
        val busy = mutableStateOf(false)
        val decisions = mutableListOf<Boolean>()
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                SaafTheme(darkTheme = dark) {
                    OwnerSyncScreen(
                        snackbarHostState = remember { SnackbarHostState() },
                        identity = NativeSignedInIdentity(1L, "owner", "Owner", AppRole.OWNER,
                            RolePermissions.permissions(AppRole.OWNER)),
                        context = NativeOwnerMoreContext("2026-10-13", true, "Your shop"),
                        status = NativeSyncStatus(environment = "prod", shopName = shop, role = "cashier",
                            sessionPresent = true, trustedTimeAnchored = true, outboxCount = 125,
                            pullCursor = 123456789L, identityStatus = "counter_device",
                            identityServerUsername = account),
                        shopChoices = emptyList(), ownerChoices = emptyList(), busy = busy.value,
                        error = null, notice = null, onRefresh = {}, onEnroll = { _, _, _ -> },
                        onEnrollInShop = { _, _, _, _, _ -> }, onEnrollAs = { _, _, _, _ -> },
                        onSyncNow = {}, onUnenroll = {},
                        holds = listOf(NativeSyncHold("products", "product-northern-market-123456789",
                            "counter-north", "2026-10-13T10:00:00Z", label = holdTitle)),
                        onResolveHold = { _, _, keep -> decisions += keep },
                        hostedScaffold = { snackbar, mod, wrap, content ->
                            OwnerScaffold(snackbarHostState = snackbar, section = OwnerSection.More,
                                onNavigate = { wrap {} }, modifier = mod, content = content)
                        },
                    )
                }
            }
        }
        for (value in listOf(shop, account, "125", "123456789")) complete(value)
        complete(shop)
        capture("sync-status-$scale-${if (dark) "dark" else "light"}")
        complete(holdTitle)
        rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldKeep).performScrollTo().performClick()
        rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldTake).performScrollTo().performClick()
        assertEquals(listOf(true, false), decisions)
        capture("sync-review-$scale-${if (dark) "dark" else "light"}")
        busy.value = true
        rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldKeep).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(NativeUiTags.OwnerSyncHoldTake).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(NativeUiTags.OwnerSyncNow).performScrollTo().assertIsNotEnabled()
    }

    private fun complete(value: String) {
        val node = rule.onNodeWithText(value, useUnmergedTree = true).performScrollTo()
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("Complete text: $value", results.isNotEmpty() && results.all { layout ->
            !layout.didOverflowHeight && layout.getLineEnd(layout.lineCount - 1) == value.length &&
                (0 until layout.lineCount).all { !layout.isLineEllipsized(it) &&
                    layout.getLineRight(it) <= layout.size.width + 1f }
        })
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "batch7-$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
