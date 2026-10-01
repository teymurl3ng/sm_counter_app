package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.admin.NativeAdminWorkContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.admin.AdminScaffold
import com.saaf.milkshop.nativeui.admin.AdminSection
import com.saaf.milkshop.nativeui.admin.AdminWorkScreen
import com.saaf.milkshop.nativeui.admin.NativeAdminWorkDestination
import com.saaf.milkshop.nativeui.admin.NativeAdminWorkState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeAdminWorkScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsSetupHealthAndExpenseShortcut() {
        setScreen(NativeAdminWorkState.Ready(context = clearContext()))
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkHome).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkHealthSection).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkSetupRequired).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkProductsRates).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkEmployeeLinks).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkPrimaryActions).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkRecordExpense).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminNavWork).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertDoesNotExist()
    }

    @Test
    fun attentionShowsWhenSetupGapsExist() {
        setScreen(
            NativeAdminWorkState.Ready(
                context = clearContext().copy(setupRequiredCount = 2),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkAttention).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkAllClear).assertDoesNotExist()
    }

    @Test
    fun allClearWhenNoSetupGaps() {
        setScreen(NativeAdminWorkState.Ready(context = clearContext()))
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkAllClear).assertIsDisplayed()
    }

    @Test
    fun expenseShortcutOpensDestination() {
        var opened: NativeAdminWorkDestination? = null
        setScreen(
            NativeAdminWorkState.Ready(context = clearContext()),
            onOpenDestination = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkRecordExpense).performClick()
        assertEquals(NativeAdminWorkDestination.RecordExpense, opened)
    }

    @Test
    fun retryLoadIsReachableOnFailure() {
        var retried = false
        setScreen(
            NativeAdminWorkState.LoadFailure("Could not load"),
            onRetry = { retried = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkRetryLoad).performClick()
        assertEquals(true, retried)
    }

    private fun setScreen(
        state: NativeAdminWorkState,
        onRetry: () -> Unit = {},
        onOpenDestination: (NativeAdminWorkDestination) -> Unit = {},
        onOpenPeople: () -> Unit = {},
        onOpenEnrollment: () -> Unit = {},
        onOpenProductRates: () -> Unit = {},
        onOpenEmployeeLinks: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                AdminWorkScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    state = state,
                    onRetry = onRetry,
                    onOpenDestination = onOpenDestination,
                    onOpenPeople = onOpenPeople,
            onOpenEnrollment = onOpenEnrollment,
            onOpenProductRates = onOpenProductRates,
            onOpenEmployeeLinks = onOpenEmployeeLinks,
                    onBackToHome = {},
                    onLeaveConfirmed = {},
                    admScaffold = { s, m, content ->
                        AdminScaffold(
                            snackbarHostState = s,
                            section = AdminSection.Work,
                            onNavigate = {},
                            modifier = m,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun clearContext() = NativeAdminWorkContext(
        businessDate = "2026-09-22",
        postingOpen = true,
        shopName = "Saaf Milk",
        setupRequiredCount = 0,
                firstSetupRequiredUserId = null,
        productsMissingRateCount = 0,
        catalogEmpty = false,
        unlinkedStaffCount = 0,
    )
}
