package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeDeliveriesContext
import com.saaf.milkshop.application.operations.NativeDeliveryItem
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.DeliveriesScreen
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeDeliveriesScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateKeepsDeliveriesNavSelected() {
        setScreen(NativeDeliveriesState.Ready(context = emptyContext()))
        composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No active deliveries").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).assertIsSelected()
    }

    @Test
    fun readyListMarksDelivered() {
        var marked: Long? = null
        setScreen(
            NativeDeliveriesState.Ready(context = contextWith(item(77L))),
            onMarkDelivered = { marked = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.opsDelivery(77L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.opsDeliveryMark(77L)).performClick()
        assertEquals(77L, marked)
    }

    @Test
    fun successBannerShowsAfterMark() {
        setScreen(
            NativeDeliveriesState.Ready(
                context = emptyContext(),
                lastDeliveredId = 12L,
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesSuccess).assertIsDisplayed()
        composeRule.onNodeWithText("Marked delivered (#12)").assertIsDisplayed()
        assertTrue(true)
    }

    private fun setScreen(
        state: NativeDeliveriesState,
        onMarkDelivered: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                DeliveriesScreen(
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
                    onMarkDelivered = onMarkDelivered,
                    onNavigate = {},
                    onLeaveConfirmed = {},
                )
            }
        }
    }

    private fun item(id: Long) = NativeDeliveryItem(
        deliveryId = id,
        customerName = "Noor Store",
        productName = "Milk",
        quantityLabel = "5 L",
        route = "Morning route",
        businessDate = "2026-09-20",
        isToday = true,
    )

    private fun contextWith(vararg items: NativeDeliveryItem) = NativeDeliveriesContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        today = items.toList(),
        earlier = emptyList(),
    )

    private fun emptyContext() = NativeDeliveriesContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        today = emptyList(),
        earlier = emptyList(),
    )
}
