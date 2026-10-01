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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.expense.NativeExpenseCategory
import com.saaf.milkshop.application.expense.NativeExpenseContext
import com.saaf.milkshop.application.expense.NativeExpensePaymentAccount
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.expense.NativeExpenseState
import com.saaf.milkshop.nativeui.owner.OwnerExpenseEntryScreen
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerExpenseScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyUsesOwnerChromeAndExcludesCashierNavigation() {
        setScreen(NativeExpenseState.Ready(context = expenseContext()))

        composeRule.onNodeWithTag(NativeUiTags.OwnerExpenseEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ExpenseCategorySelector)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
    }

    @Test
    fun dirtyOwnerNavigationRequiresDiscardConfirmation() {
        var navigated: OwnerSection? = null
        setScreen(
            state = NativeExpenseState.Ready(context = expenseContext()),
            onNavigate = { navigated = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.ExpenseAmountInput)
            .performScrollTo()
            .performTextReplacement("125.50")
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()

        composeRule.onNodeWithText("Discard this expense?").assertIsDisplayed()
        assertEquals(null, navigated)

        composeRule.onNodeWithTag(NativeUiTags.ExpenseDiscardDraft).performClick()
        assertEquals(OwnerSection.Overview, navigated)
    }

    @Test
    fun loadFailureRetainsOwnerShellAndRetries() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeExpenseState by remember {
                    mutableStateOf(NativeExpenseState.LoadFailure("Owner expense unavailable."))
                }
                OwnerExpenseEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeExpenseState.Loading
                    },
                    onRecordExpense = {},
                    onRecordAnother = {},
                    onBackToWork = {},
                    ownScaffold = { s, m, content ->
                        OwnerScaffold(
                            snackbarHostState = s,
                            section = OwnerSection.Work,
                            onNavigate = {},
                            modifier = m,
                            content = content,
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithText("Owner expense unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ExpenseRetryLoad).performClick()
        composeRule.onNodeWithText("Loading expense entry…").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertTrue(retried)
    }

    private fun setScreen(
        state: NativeExpenseState,
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerExpenseEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {},
                    onRecordExpense = {},
                    onRecordAnother = {},
                    onBackToWork = {},
                    ownScaffold = { s, m, content ->
                        OwnerScaffold(
                            snackbarHostState = s,
                            section = OwnerSection.Work,
                            onNavigate = {},
                            modifier = m,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun ownerIdentity() = NativeSignedInIdentity(
        userId = 1L,
        loginName = "owner.expense.contract",
        displayName = "Contract Owner",
        role = AppRole.OWNER,
        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
    )

    private fun expenseContext() = NativeExpenseContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        categories = listOf(NativeExpenseCategory(11L, "Electricity")),
        paymentAccounts = listOf(NativeExpensePaymentAccount(4L, "Cash")),
        defaultPaymentAccountId = 4L,
    )
}
