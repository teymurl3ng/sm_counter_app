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
import com.saaf.milkshop.application.operations.NativeRecordSpoilageContext
import com.saaf.milkshop.application.operations.NativeRecordSpoilageReceipt
import com.saaf.milkshop.application.operations.NativeRecordSpoilageRequest
import com.saaf.milkshop.application.operations.NativeSpoilageProduct
import com.saaf.milkshop.application.operations.NativeSpoilageReason
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageState
import com.saaf.milkshop.nativeui.operations.RecordSpoilageScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeRecordSpoilageScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyFormShowsPhysicalReviewWithoutMoney() {
        setScreen(NativeRecordSpoilageState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageReview).assertIsDisplayed()
        composeRule.onNodeWithText("No money", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageRecord).assertIsNotEnabled()
    }

    @Test
    fun selectingProductQuantityAndReasonEnablesRecord() {
        var recorded: NativeRecordSpoilageRequest? = null
        setScreen(
            NativeRecordSpoilageState.Ready(
                context = openContext(),
                selectedProduct = milkProduct(),
                selectedReason = NativeSpoilageReason.SPOILED,
            ),
            onRecord = { recorded = it },
        )
        enterMeasurement(NativeUiTags.OpsRecordSpoilageQuantity, "1.5")
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageRecord).performScrollTo()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageRecord)
            .assertIsEnabled()
            .performClick()
        composeRule.waitUntil(5_000) { recorded != null }
        val request = checkNotNull(recorded)
        assertEquals(11L, request.productId)
        assertEquals(1.5, request.quantity, 0.0001)
        assertEquals(NativeSpoilageReason.SPOILED.code, request.reasonCode)
    }

    @Test
    fun successShowsRecordAnother() {
        var again = false
        setScreen(
            NativeRecordSpoilageState.Ready(
                context = openContext(),
                receipt = NativeRecordSpoilageReceipt(
                    eventId = 44L,
                    product = milkProduct(),
                    quantity = 1.5,
                    reasonCode = NativeSpoilageReason.SPOILED.code,
                    reasonLabel = NativeSpoilageReason.SPOILED.label,
                    remainingLabel = "10.5 L",
                ),
            ),
            onRecordAnother = { again = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageSuccess).assertIsDisplayed()
        composeRule.onNodeWithText("Record #44").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageAnother).performClick()
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
        state: NativeRecordSpoilageState,
        onRecord: (NativeRecordSpoilageRequest) -> Unit = {},
        onRecordAnother: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                RecordSpoilageScreen(
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
                    onSelectProduct = {},
                    onOpenProductPicker = {},
                    onCloseProductPicker = {},
                    onSelectReason = {},
                    onOpenReasonPicker = {},
                    onCloseReasonPicker = {},
                    onRecord = onRecord,
                    onRecordAnother = onRecordAnother,
                    onBackToHome = {},
                    onNavigate = {},
                )
            }
        }
    }

    private fun milkProduct() = NativeSpoilageProduct(
        productId = 11L,
        name = "Milk",
        unitLabel = "L",
        onHand = 12.0,
        onHandLabel = "12 L",
    )

    private fun openContext() = NativeRecordSpoilageContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        products = listOf(milkProduct()),
        reasons = NativeSpoilageReason.entries.toList(),
    )
}
