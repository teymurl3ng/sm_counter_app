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
import com.saaf.milkshop.application.operations.NativeReceiveMilkContext
import com.saaf.milkshop.application.operations.NativeReceiveMilkPreview
import com.saaf.milkshop.application.operations.NativeReceiveMilkReceipt
import com.saaf.milkshop.application.operations.NativeReceiveMilkRequest
import com.saaf.milkshop.application.operations.NativeReceiveMilkVendor
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkState
import com.saaf.milkshop.nativeui.operations.OperationsSection
import com.saaf.milkshop.nativeui.operations.ReceiveMilkScreen
import com.saaf.milkshop.nativeui.operations.WorkScaffold
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeReceiveMilkScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyFormShowsPhysicalReviewWithoutMoney() {
        setScreen(NativeReceiveMilkState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkReview).assertIsDisplayed()
        composeRule.onNodeWithText("No money", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).assertIsNotEnabled()
    }

    @Test
    fun selectingSupplierAndMeasurementsEnablesRecord() {
        var recorded: NativeReceiveMilkRequest? = null
        setScreen(
            NativeReceiveMilkState.Ready(
                context = openContext(),
                selectedVendor = NativeReceiveMilkVendor(11L, "Dawn Dairy"),
            ),
            onRecord = { recorded = it },
            onPreview = { raw, fat, lr ->
                if (raw > 0.0 && fat >= 0.0 && lr > 0.0) {
                    NativeReceiveMilkPreview(
                        snfPercentage = 8.1,
                        tsPercentage = 12.1,
                        adjustedLiters = 42.0,
                    )
                } else {
                    null
                }
            },
        )
        enterMeasurement(NativeUiTags.OpsReceiveMilkLiters, "40")
        enterMeasurement(NativeUiTags.OpsReceiveMilkFat, "4")
        enterMeasurement(NativeUiTags.OpsReceiveMilkLr, "28")
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).performScrollTo()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord)
            .assertIsEnabled()
            .performClick()
        composeRule.waitUntil(5_000) { recorded != null }
        val request = checkNotNull(recorded) { "Record receipt did not emit a request" }
        assertEquals(11L, request.vendorAccountId)
        assertEquals(40.0, request.rawLiters, 0.0001)
    }

    @Test
    fun successShowsReceiveAnother() {
        var again = false
        setScreen(
            NativeReceiveMilkState.Ready(
                context = openContext(),
                selectedVendor = NativeReceiveMilkVendor(11L, "Dawn Dairy"),
                receipt = NativeReceiveMilkReceipt(
                    eventId = 77L,
                    vendor = NativeReceiveMilkVendor(11L, "Dawn Dairy"),
                    rawLiters = 40.0,
                    fatPercentage = 4.0,
                    lrReading = 28.0,
                    snfPercentage = 8.1,
                    tsPercentage = 12.1,
                    adjustedLiters = 42.0,
                    milkOnHandLabel = "40 L milk",
                ),
            ),
            onReceiveAnother = { again = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSuccess).assertIsDisplayed()
        composeRule.onNodeWithText("Record #77").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkAnother).performClick()
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
        state: NativeReceiveMilkState,
        onRecord: (NativeReceiveMilkRequest) -> Unit = {},
        onReceiveAnother: () -> Unit = {},
        onPreview: (Double, Double, Double) -> NativeReceiveMilkPreview? = { _, _, _ -> null },
    ) {
        composeRule.setContent {
            SaafTheme {
                ReceiveMilkScreen(
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
                    onSelectVendor = {},
                    onOpenVendorPicker = {},
                    onCloseVendorPicker = {},
                    onPreview = onPreview,
                    onRecord = onRecord,
                    onReceiveAnother = onReceiveAnother,
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

    private fun openContext() = NativeReceiveMilkContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        vendors = listOf(NativeReceiveMilkVendor(11L, "Dawn Dairy")),
        tsStandardPercent = 12.5,
        milkProductPresent = true,
        milkOnHandLabel = "0 L milk",
    )
}
