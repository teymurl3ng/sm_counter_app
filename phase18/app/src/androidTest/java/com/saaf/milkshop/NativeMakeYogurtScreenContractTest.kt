package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeMakeYogurtContext
import com.saaf.milkshop.application.operations.NativeMakeYogurtReceipt
import com.saaf.milkshop.application.operations.NativeMakeYogurtRequest
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.MakeYogurtScreen
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtState
import com.saaf.milkshop.nativeui.operations.OperationsSection
import com.saaf.milkshop.nativeui.operations.WorkScaffold
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeMakeYogurtScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyFormShowsPhysicalReviewWithoutMoney() {
        setScreen(NativeMakeYogurtState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtReview).assertIsDisplayed()
        composeRule.onNodeWithText("No money", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtRecord).assertIsNotEnabled()
    }

    @Test
    fun enteringConversionEnablesRecord() {
        var recorded: NativeMakeYogurtRequest? = null
        setScreen(
            NativeMakeYogurtState.Ready(context = openContext()),
            onRecord = { recorded = it },
        )
        enterMeasurement(NativeUiTags.OpsMakeYogurtMilk, "20")
        enterMeasurement(NativeUiTags.OpsMakeYogurtYogurt, "15")
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtRecord).performScrollTo()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtRecord)
            .assertIsEnabled()
            .performClick()
        composeRule.waitUntil(5_000) { recorded != null }
        val request = checkNotNull(recorded)
        assertEquals(20.0, request.milkLitersUsed, 0.0001)
        assertEquals(15.0, request.yogurtKgProduced, 0.0001)
    }

    @Test
    fun successShowsMakeAnother() {
        var again = false
        setScreen(
            NativeMakeYogurtState.Ready(
                context = openContext(),
                receipt = NativeMakeYogurtReceipt(
                    eventId = 88L,
                    milkLitersUsed = 20.0,
                    yogurtKgProduced = 15.0,
                    milkOnHandLabel = "30 L milk",
                    yogurtOnHandLabel = "15 kg yogurt",
                ),
            ),
            onMakeAnother = { again = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtSuccess).assertIsDisplayed()
        composeRule.onNodeWithText("Record #88").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtAnother).performClick()
        assertTrue(again)
    }

    private fun enterMeasurement(tag: String, value: String) {
        val node = composeRule.onNode(
            hasTestTag(tag) and hasSetTextAction(),
            useUnmergedTree = true,
        )
        node.performScrollTo()
        node.performTextClearance()
        node.performTextInput(value)
    }

    private fun setScreen(
        state: NativeMakeYogurtState,
        onRecord: (NativeMakeYogurtRequest) -> Unit = {},
        onMakeAnother: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                MakeYogurtScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = NativeSignedInIdentity(
                        userId = 2L,
                        loginName = "ops",
                        displayName = "Ops",
                        role = AppRole.OPERATOR,
                        effectivePermissions = RolePermissions.permissions(AppRole.OPERATOR),
                    ),
                    state = state,
                    onRetry = {},
                    onRecord = onRecord,
                    onMakeAnother = onMakeAnother,
                    onBackToHome = {},
                    workScaffold = { s, m, content ->
                        WorkScaffold(
                            snackbarHostState = s,
                            section = OperationsSection.Work,
                            onNavigate = {},
                            modifier = m,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun openContext() = NativeMakeYogurtContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        milkProductPresent = true,
        yogurtProductPresent = true,
        milkOnHandLiters = 50.0,
        milkOnHandLabel = "50 L milk",
        yogurtOnHandLabel = "0 kg yogurt",
    )
}
