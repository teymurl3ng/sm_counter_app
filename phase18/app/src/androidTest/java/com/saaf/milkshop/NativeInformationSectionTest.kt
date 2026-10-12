package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.common.NativeInformationFact
import com.saaf.milkshop.nativeui.common.NativeInformationHeading
import com.saaf.milkshop.nativeui.common.NativeInformationSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.theme.nativeVisualColors
import com.saaf.milkshop.qa.NativeEvidenceCapture
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NativeInformationSectionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun lightDefault() = geometry(1f, false)
    @Test fun darkDefault() = geometry(1f, true)
    @Test fun largeText() = geometry(1.5f, false)
    @Test fun extraLargeDark() = geometry(2f, true)

    private fun geometry(scale: Float, dark: Boolean) {
        val address = "12 Market Road, beside the community centre, North District"
        val money = "PKR 123,456,789,012.99"
        var density = 1f
        var edits = 0
        val contrasts = mutableMapOf<String, Float>()
        rule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        rule.setContent {
            density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                SaafTheme(darkTheme = dark) {
                    val roles = nativeVisualColors
                    contrasts.clear()
                    listOf("supporting" to roles.supporting, "metric" to roles.metric,
                        "value" to MaterialTheme.colorScheme.onSurface, "action" to MaterialTheme.colorScheme.primary).forEach { (name, color) ->
                        val foreground = color.compositeOver(MaterialTheme.colorScheme.surface).luminance()
                        val background = MaterialTheme.colorScheme.surface.luminance()
                        contrasts[name] = (maxOf(foreground, background) + .05f) / (minOf(foreground, background) + .05f)
                    }
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NativeInformationSection(Modifier.widthIn(max = 320.dp).testTag("section")) {
                            NativeInformationHeading("Shop details", actionLabel = "Edit", actionTag = "edit", onAction = { edits++ })
                            NativeInformationFact("Currency", "PKR", Modifier.testTag("currency"))
                            NativeInformationFact("Phone", "Not set")
                            NativeInformationFact("TS standard", "13%")
                            NativeInformationFact("Stock items", "5")
                            NativeInformationFact("Account", "#12")
                            NativeInformationFact("Status", "Active")
                            NativeInformationFact("Rate", "PKR 170.00", emphasized = true)
                            NativeInformationFact("Address", address, Modifier.testTag("address"), stacked = true, prose = true)
                            NativeInformationFact("Total", money, Modifier.testTag("money"), emphasized = true)
                        }
                        NativeInformationSection(Modifier.widthIn(max = 320.dp).testTag("long-section")) {
                            NativeInformationHeading("North District community market details", Modifier.testTag("long-heading"),
                                actionLabel = "Edit details", actionTag = "long-edit", onAction = { edits++ })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertTrue("Every enabled role has readable contrast: $contrasts", contrasts.size == 4 && contrasts.values.all { it >= 4.5f })
        val surface = rule.onNodeWithTag("section").fetchSemanticsNode().boundsInRoot
        val labels = listOf("Currency", "Phone", "TS standard", "Stock items", "Account", "Status", "Rate", "Address", "Total")
        val values = listOf("PKR", "Not set", "13%", "5", "#12", "Active", "PKR 170.00", address, money)
        labels.zip(values).forEach { (label, value) ->
            rule.onNodeWithText(value, useUnmergedTree = true).performScrollTo()
            val l = rule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val v = rule.onNodeWithText(value, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Left inset: $label", l.left >= surface.left + density * 16f - 1f)
            assertTrue("Right inset: $label", v.right <= surface.right - density * 16f + 1f)
            if (label in listOf("Address", "Total") || scale >= 1.5f) assertTrue("Stacked facts: $label", l.bottom <= v.top + 1f)
            else assertBaseline(label, value)
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(value, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Complete value: $label", layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
            val labelLayouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(labelLayouts) }
            assertEquals("Fact labels use stronger supporting strokes", FontWeight.Medium, labelLayouts.single().layoutInput.style.fontWeight)
            val valueStyle = layouts.single().layoutInput.style
            assertEquals("Value weight follows its role", when (label) {
                "Total", "Rate" -> FontWeight.Bold
                "Address" -> FontWeight.Normal
                else -> FontWeight.SemiBold
            }, valueStyle.fontWeight)
            assertNotEquals("Label and value retain distinct colors", labelLayouts.single().layoutInput.style.color, valueStyle.color)
        }
        rule.onNodeWithTag("currency").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        rule.onNodeWithText("Shop details").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithTag("edit").performScrollTo()
        val heading = layout("Shop details")
        assertEquals(16f, heading.layoutInput.style.fontSize.value, .01f)
        assertEquals(FontWeight.SemiBold, heading.layoutInput.style.fontWeight)
        val editBounds = rule.onNodeWithTag("edit").fetchSemanticsNode().boundsInRoot
        assertEquals("Action target stays at the inner trailing edge", surface.right - density * 16f, editBounds.right, 1f)
        if (scale < 1.5f) assertBaseline("Shop details", "Edit")
        else assertTrue(rule.onNodeWithText("Shop details").fetchSemanticsNode().boundsInRoot.bottom <= editBounds.top + 1f)
        rule.onNodeWithTag("edit").performClick()
        assertEquals(1, edits)
        val longAction = rule.onNodeWithTag("long-edit").performScrollTo().fetchSemanticsNode().boundsInRoot
        val longTitle = rule.onNodeWithText("North District community market details", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Long heading stacks its action", longTitle.bottom <= longAction.top + 1f)
        assertTrue("Minimum action target", longAction.height >= 48f * density - 1f)
        val longSurface = rule.onNodeWithTag("long-section").fetchSemanticsNode().boundsInRoot
        assertEquals("Stacked action retains its trailing anchor", longSurface.right - density * 16f, longAction.right, 1f)
        rule.onNodeWithTag("long-edit").performClick()
        assertEquals(2, edits)
        rule.onNodeWithTag("edit").performScrollTo()
        NativeEvidenceCapture.captureWholeWindow(rule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "settings-grouping", scenario = "component-$scale-${if (dark) "dark" else "light"}",
            workflowId = "NativeInformationSectionTest", fontScale = scale.toString(), theme = if (dark) "dark" else "light",
            note = "Grouped facts at up to 320dp body width; long text/money and action reflow. Fixture, user review pending."))
    }
    private fun layout(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single()
    }

    private fun assertBaseline(label: String, value: String) {
        val labelTop = rule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        val valueTop = rule.onNodeWithText(value, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        assertEquals("Rendered first baselines: $label / $value", labelTop + layout(label).firstBaseline,
            valueTop + layout(value).firstBaseline, 1f)
    }

}
