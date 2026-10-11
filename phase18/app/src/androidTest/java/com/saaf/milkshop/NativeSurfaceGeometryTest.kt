package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItem
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.qa.NativeEvidenceCapture
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Geometry assertions protect readable insets; screenshots remain Owner-unreviewed. */
@RunWith(AndroidJUnit4::class)
class NativeSurfaceGeometryTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun lightDefaultRows() = checkRows(false, 1f)
    @Test fun darkDefaultRows() = checkRows(true, 1f)
    @Test fun lightLargeTextRows() = checkRows(false, 1.5f)
    @Test fun darkLargeTextRows() = checkRows(true, 1.5f)

    private fun checkRows(dark: Boolean, fontScale: Float) {
        var density = 1f
        rule.setContent {
            val deviceDensity = LocalDensity.current.density
            density = deviceDensity
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale)) {
                SaafTheme(darkTheme = dark) {
                    Column(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(NativeListStyle.Navigation, NativeListStyle.Entity, NativeListStyle.Selection, NativeListStyle.Record).forEach { style ->
                            Column(
                                Modifier.nativeListItem(style, selected = style == NativeListStyle.Selection)
                                    .clickable { }.testTag("surface-${style.name}").nativeListItemContent(style),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("Long customer and product name", Modifier.testTag("title-${style.name}"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("PKR 123,456,789.00", Modifier.testTag("amount-${style.name}"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        listOf(NativeListStyle.Navigation, NativeListStyle.Entity, NativeListStyle.Selection, NativeListStyle.Record).forEach { style ->
            val surface = rule.onNodeWithTag("surface-${style.name}").fetchSemanticsNode().boundsInRoot
            val title = rule.onNodeWithTag("title-${style.name}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val amount = rule.onNodeWithTag("amount-${style.name}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("${style.name} must inset text from the left edge", title.left - surface.left >= 16f * density - 1f)
            assertTrue("${style.name} must inset money from the right edge", amount.right <= surface.right - 16f * density + 1f)
            assertTrue("${style.name} title and money must not overlap", title.bottom <= amount.top)
        }
        NativeEvidenceCapture.captureWholeWindow(rule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "ux", scenario = "surface-${if (dark) "dark" else "light"}-${if (fontScale > 1f) "large" else "default"}",
            workflowId = "NativeSurfaceGeometryTest", theme = if (dark) "dark" else "light", note = "Shared variants, long content and font scaling. Presentation fixture; Owner visual review pending.",
        ))
    }
}
