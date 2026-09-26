package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeDeliveriesContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState
import com.saaf.milkshop.nativeui.owner.OwnerDeliveriesEntryScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerDeliveriesEntryScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyUsesOwnerChromeAndExcludesOpsNavigation() {
        setScreen(NativeDeliveriesState.Ready(context = deliveriesContext()))

        composeRule.onNodeWithTag(NativeUiTags.OwnerDeliveriesEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertDoesNotExist()
    }

    @Test
    fun loadFailureRetainsOwnerShellAndRetries() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeDeliveriesState by remember {
                    mutableStateOf(NativeDeliveriesState.LoadFailure("Owner deliveries unavailable."))
                }
                OwnerDeliveriesEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeDeliveriesState.Loading
                    },
                    onMarkDelivered = {},
            onLinkSale = { _, _ -> },
                    onBackToWork = {},
                    onNavigate = {},
                )
            }
        }

        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithText("Owner deliveries unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesRetryLoad).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesLoading).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertTrue(retried)
    }

    private fun setScreen(state: NativeDeliveriesState) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerDeliveriesEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {},
                    onMarkDelivered = {},
            onLinkSale = { _, _ -> },
                    onBackToWork = {},
                    onNavigate = {},
                )
            }
        }
    }

    private fun ownerIdentity() = NativeSignedInIdentity(
        userId = 1L,
        loginName = "owner.deliveries.contract",
        displayName = "Contract Owner",
        role = AppRole.OWNER,
        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
    )

    private fun deliveriesContext() = NativeDeliveriesContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        today = emptyList(),
        earlier = emptyList(),
    )
}
