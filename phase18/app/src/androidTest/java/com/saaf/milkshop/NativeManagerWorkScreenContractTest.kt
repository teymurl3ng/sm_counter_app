package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.manager.NativeManagerWorkContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.manager.ManagerWorkScreen
import com.saaf.milkshop.nativeui.manager.NativeManagerWorkDestination
import com.saaf.milkshop.nativeui.manager.NativeManagerWorkState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeManagerWorkScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsManagerShortcutsAndExcludesOwnerOnlyRows() {
        setScreen(NativeManagerWorkState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkHome).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkPrimaryActions).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordSale).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordPayment).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkReceiveMilk).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordExpense).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkMakeYogurt)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordSpoilage)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkDeliveries)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkPaySupplier)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkCashClose)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerNavWork).assertIsSelected()
        composeRule.onNodeWithText("Pay salary").assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkPaySalary).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordSale).assertDoesNotExist()
    }

    @Test
    fun attentionShowsWhenCashCountMissing() {
        setScreen(
            NativeManagerWorkState.Ready(
                context = openContext().copy(cashCountMissing = true),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkAttention).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkAllClear).assertDoesNotExist()
    }

    @Test
    fun allClearWhenNoAttention() {
        setScreen(
            NativeManagerWorkState.Ready(
                context = openContext().copy(
                    postingOpen = true,
                    cashCountMissing = false,
                    activeDeliveryCount = 0,
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkAllClear).assertIsDisplayed()
    }

    @Test
    fun saleShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordSale).performClick()
        assertEquals(NativeManagerWorkDestination.RecordSale, opened)
    }

    @Test
    fun paymentShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordPayment).performClick()
        assertEquals(NativeManagerWorkDestination.RecordPayment, opened)
    }

    @Test
    fun receiveMilkShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkReceiveMilk).performClick()
        assertEquals(NativeManagerWorkDestination.ReceiveMilk, opened)
    }

    @Test
    fun makeYogurtShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkMakeYogurt)
            .performScrollTo()
            .performClick()
        assertEquals(NativeManagerWorkDestination.MakeYogurt, opened)
    }

    @Test
    fun spoilageShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordSpoilage)
            .performScrollTo()
            .performClick()
        assertEquals(NativeManagerWorkDestination.RecordSpoilage, opened)
    }

    @Test
    fun deliveriesShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkDeliveries)
            .performScrollTo()
            .performClick()
        assertEquals(NativeManagerWorkDestination.Deliveries, opened)
    }

    @Test
    fun expenseShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRecordExpense).performClick()
        assertEquals(NativeManagerWorkDestination.RecordExpense, opened)
    }

    @Test
    fun paySupplierShortcutOpensDestination() {
        var opened: NativeManagerWorkDestination? = null
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkPaySupplier)
            .performScrollTo()
            .performClick()
        assertEquals(NativeManagerWorkDestination.PaySupplier, opened)
    }

    @Test
    fun cashCloseShortcutOpensCloseTab() {
        var closeOpened = false
        setScreen(
            NativeManagerWorkState.Ready(context = openContext()),
            onOpenClose = { closeOpened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkCashClose)
            .performScrollTo()
            .performClick()
        assertEquals(true, closeOpened)
    }

    @Test
    fun retryLoadIsReachableOnFailure() {
        var retried = false
        setScreen(
            NativeManagerWorkState.LoadFailure("Work could not open."),
            onRetry = { retried = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRetryLoad).performClick()
        assertEquals(true, retried)
    }

    private fun setScreen(
        state: NativeManagerWorkState,
        onRetry: () -> Unit = {},
        onOpenDestination: (NativeManagerWorkDestination) -> Unit = {},
        onOpenClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                ManagerWorkScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    state = state,
                    onRetry = onRetry,
                    onOpenDestination = onOpenDestination,
                    onOpenClose = onOpenClose,
                    onBackToHome = {},
                    onLeaveConfirmed = {},
                    onNavigate = {},
                )
            }
        }
    }

    private fun openContext() = NativeManagerWorkContext(
        businessDate = "2026-09-22",
        postingOpen = true,
        shopName = "Contract Shop",
        activeDeliveryCount = 0,
        cashCountMissing = false,
    )
}
