package com.saaf.milkshop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Rule
import org.junit.Test

class AdaptiveNavigationScaffoldContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactWidthUsesBottomNavigation() {
        render(widthDp = 599)

        composeRule.onNodeWithTag(COMPACT_NAV).assertExists()
        composeRule.onNodeWithTag(EXPANDED_NAV).assertDoesNotExist()
    }

    @Test
    fun mediumWidthUsesNavigationRail() {
        render(widthDp = 600)

        composeRule.onNodeWithTag(EXPANDED_NAV).assertExists()
        composeRule.onNodeWithTag(COMPACT_NAV).assertDoesNotExist()
    }

    private fun render(widthDp: Int) {
        composeRule.setContent {
            SaafTheme {
                Box(Modifier.requiredWidth(widthDp.dp).height(800.dp)) {
                    AdaptiveNavigationScaffold(
                        snackbarHostState = SnackbarHostState(),
                        compactNavigation = { Box(Modifier.testTag(COMPACT_NAV)) },
                        expandedNavigation = { Box(Modifier.testTag(EXPANDED_NAV)) },
                    ) {
                        Box(Modifier.fillMaxSize().testTag(CONTENT))
                    }
                }
            }
        }
    }

    private companion object {
        const val COMPACT_NAV = "adaptive_compact_navigation"
        const val EXPANDED_NAV = "adaptive_expanded_navigation"
        const val CONTENT = "adaptive_content"
    }
}
