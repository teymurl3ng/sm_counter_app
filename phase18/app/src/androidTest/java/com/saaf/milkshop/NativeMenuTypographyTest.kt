package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.owner.OwnerMoreMenuRow
import com.saaf.milkshop.nativeui.owner.OwnerMoreSectionLabel
import com.saaf.milkshop.nativeui.owner.CommandRow as OwnerCommandRow
import com.saaf.milkshop.nativeui.owner.SectionTitle as OwnerSectionTitle
import com.saaf.milkshop.nativeui.admin.AdminMoreMenuRow
import com.saaf.milkshop.nativeui.admin.AdminMoreSectionLabel
import com.saaf.milkshop.nativeui.admin.AdminPeopleMenuRow
import com.saaf.milkshop.nativeui.admin.AdminPeopleSectionLabel
import com.saaf.milkshop.nativeui.admin.AdminRecordsMenuRow
import com.saaf.milkshop.nativeui.admin.AdminRecordsSectionLabel
import com.saaf.milkshop.nativeui.manager.ManagerMoreMenuRow
import com.saaf.milkshop.nativeui.manager.ManagerMoreSectionLabel
import com.saaf.milkshop.nativeui.manager.CommandRow as ManagerCommandRow
import com.saaf.milkshop.nativeui.more.MoreMenuRow
import com.saaf.milkshop.nativeui.operations.OpsMoreMenuRow
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.qa.NativeEvidenceCapture
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Exercise the actual nine row helpers, not a duplicate test-only menu layout. */
class NativeMenuTypographyTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun defaultLight() = anatomy(1f, false)
    @Test fun defaultDark() = anatomy(1f, true)
    @Test fun largeLight() = anatomy(1.5f, false)
    @Test fun extraLargeDark() = anatomy(2f, true)

    private fun anatomy(scale: Float, dark: Boolean) {
        val families = listOf("Owner More", "Admin More", "Manager More", "Cashier More", "Operator More",
            "Admin People", "Admin Records", "Owner Work", "Manager Work")
        val clicks = IntArray(families.size)
        var density = 1f
        rule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        rule.setContent {
            density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                SaafTheme(darkTheme = dark) {
                    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onSurface) {
                        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            families.forEachIndexed { index, family ->
                                val description = "Manage shop details, access and daily records for $family without losing any information."
                                val click = { clicks[index]++; Unit }
                                val icon = Icons.Outlined.Settings
                                val tag = "menu-$index"
                                Box(Modifier.fillMaxWidth().testTag("frame-$index")) {
                                    when (index) {
                                        0 -> OwnerMoreMenuRow(icon, family, description, tag, click)
                                        1 -> AdminMoreMenuRow(icon, family, description, tag, click)
                                        2 -> ManagerMoreMenuRow(icon, family, description, tag, click)
                                        3 -> MoreMenuRow(icon, family, description, tag, click)
                                        4 -> OpsMoreMenuRow(icon, family, description, tag, click)
                                        5 -> AdminPeopleMenuRow(icon, family, description, tag, click)
                                        6 -> AdminRecordsMenuRow(icon, family, description, tag, click)
                                        7 -> OwnerCommandRow(family, description, icon, tag, click, badge = "12")
                                        8 -> ManagerCommandRow(family, description, icon, tag, click, badge = "3")
                                    }
                                }
                            }
                            OwnerMoreSectionLabel("Owner group")
                            AdminMoreSectionLabel("Admin group")
                            ManagerMoreSectionLabel("Manager group")
                            AdminPeopleSectionLabel("People group")
                            AdminRecordsSectionLabel("Records group")
                            OwnerSectionTitle("Work group")
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        families.forEachIndexed { index, family ->
            val description = "Manage shop details, access and daily records for $family without losing any information."
            // Bring the complete row into view: scrolling only its title can leave
            // the description outside the viewport, where clipped bounds are empty.
            rule.onNodeWithTag("frame-$index").performScrollTo()
            rule.onNodeWithText(family, useUnmergedTree = true).assertIsDisplayed()
            rule.onNodeWithText(description, useUnmergedTree = true).assertIsDisplayed()
            val frame = rule.onNodeWithTag("frame-$index").fetchSemanticsNode().boundsInRoot
            val title = rule.onNodeWithText(family, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val supporting = rule.onNodeWithText(description, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals("Shared inset/icon/gap: $family", frame.left + 54f * density, title.left, 1.5f)
            assertEquals("Description aligns with title: $family", title.left, supporting.left, 1f)
            assertEquals("Text gap: $family", 2f * density, supporting.top - title.bottom, 1.5f)
            assertEquals("Title role: $family", FontWeight.SemiBold, layout(family).layoutInput.style.fontWeight)
            assertEquals("Description role: $family", FontWeight.Medium, layout(description).layoutInput.style.fontWeight)
            assertEquals(16f, layout(family).layoutInput.style.fontSize.value, .01f)
            assertEquals(14f, layout(description).layoutInput.style.fontSize.value, .01f)
            assertNotEquals(layout(family).layoutInput.style.color, layout(description).layoutInput.style.color)
            for (text in listOf(family, description)) {
                val result = layout(text)
                assertTrue("Complete text: $family", (0 until result.lineCount).all { line ->
                    !result.isLineEllipsized(line) && result.getLineLeft(line) >= -1f &&
                        result.getLineRight(line) <= result.size.width + 1f
                } && result.multiParagraph.height <= result.size.height + 1f)
            }
            if (index >= 7) {
                val badge = rule.onNodeWithTag("menu-${index}_badge", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertTrue("Badge does not overlap text: $family", supporting.right <= badge.left + 1f)
            }
            rule.onNodeWithTag("menu-$index").performClick()
            assertEquals("One callback per tap: $family", 1, clicks[index])
        }
        for (group in listOf("Owner group", "Admin group", "Manager group", "People group", "Records group", "Work group")) {
            rule.onNodeWithText(group, useUnmergedTree = true).performScrollTo()
            assertEquals(14f, layout(group).layoutInput.style.fontSize.value, .01f)
            assertEquals(FontWeight.SemiBold, layout(group).layoutInput.style.fontWeight)
        }
        rule.onNodeWithText("Owner More", useUnmergedTree = true).performScrollTo()
        NativeEvidenceCapture.captureWholeWindow(rule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "ux", scenario = "menu-anatomy-$scale-${if (dark) "dark" else "light"}",
            workflowId = "NativeMenuTypographyTest", fontScale = scale.toString(), theme = if (dark) "dark" else "light",
            note = "Actual nine navigation helpers, long descriptions and Work badges. Fixture appearance; user review pending."))
    }

    private fun layout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }
}
