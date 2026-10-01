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
import com.saaf.milkshop.application.owner.NativeOwnerWorkContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerWorkDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerWorkState
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerWorkScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerWorkScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsCommandGroupsAndKeepsWorkNavSelected() {
        setScreen(NativeOwnerWorkState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkPrimaryActions).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkStockSection).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkMoneySection).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordSale).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkReceiveMilk).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkDeliveries).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithText("All daily commands remain reachable.").assertIsDisplayed()
    }

    @Test
    fun deliveriesBadgeShowsActiveCount() {
        setScreen(
            NativeOwnerWorkState.Ready(
                context = openContext().copy(activeDeliveryCount = 3),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkDeliveries).assertIsDisplayed()
        composeRule.onNodeWithTag(
            "${NativeUiTags.OwnerWorkDeliveries}_badge",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText("3 active deliveries").assertIsDisplayed()
    }

    @Test
    fun recordSaleOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordSale).performClick()
        assertEquals(NativeOwnerWorkDestination.RecordSale, opened)
    }

    @Test
    fun recordPaymentOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordPayment).performClick()
        assertEquals(NativeOwnerWorkDestination.RecordPayment, opened)
    }

    @Test
    fun receiveMilkOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkReceiveMilk).performClick()
        assertEquals(NativeOwnerWorkDestination.ReceiveMilk, opened)
    }

    @Test
    fun makeYogurtOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkMakeYogurt).performClick()
        assertEquals(NativeOwnerWorkDestination.MakeYogurt, opened)
    }

    @Test
    fun recordSpoilageOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordSpoilage).performClick()
        assertEquals(NativeOwnerWorkDestination.RecordSpoilage, opened)
    }

    @Test
    fun deliveriesOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkDeliveries).performClick()
        assertEquals(NativeOwnerWorkDestination.Deliveries, opened)
    }

    @Test
    fun recordExpenseOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordExpense).performClick()
        assertEquals(NativeOwnerWorkDestination.RecordExpense, opened)
    }

    @Test
    fun paySupplierOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkPaySupplier).performClick()
        assertEquals(NativeOwnerWorkDestination.PaySupplier, opened)
    }

    @Test
    fun paySalaryOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkCashClose).performScrollTo()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkPaySalary)
            .assertIsDisplayed()
            .performClick()
        assertEquals(NativeOwnerWorkDestination.PaySalary, opened)
    }

    @Test
    fun cashCloseOpensConnectedDestination() {
        var opened: NativeOwnerWorkDestination? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkCashClose)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(NativeOwnerWorkDestination.CashCountAndClose, opened)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            NativeOwnerWorkState.LoadFailure("Work could not open."),
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRetryLoad).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun overviewNavFromWorkRequestsNavigation() {
        var navigated: OwnerSection? = null
        setScreen(
            NativeOwnerWorkState.Ready(context = openContext()),
            onNavigate = { navigated = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()
        assertEquals(OwnerSection.Overview, navigated)
    }

    private fun setScreen(
        state: NativeOwnerWorkState,
        onRetry: () -> Unit = {},
        onOpenDestination: (NativeOwnerWorkDestination) -> Unit = {},
        onBackToHome: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                OwnerWorkScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    state = state,
                    onRetry = onRetry,
                    onOpenDestination = onOpenDestination,
                    onBackToHome = onBackToHome,
                    onLeaveConfirmed = {},
                    ownScaffold = { s, m, content ->
                        OwnerScaffold(
                            snackbarHostState = s,
                            section = OwnerSection.Work,
                            onNavigate = onNavigate,
                            modifier = m,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun openContext() = NativeOwnerWorkContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        shopName = "Saaf Milk",
        activeDeliveryCount = 0,
    )
}
