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
import com.saaf.milkshop.nativeui.more.CashierExpenseEntryScreen
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeCashierExpenseScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyUsesCashierChromeAndExcludesOwnerNavigation() {
        setScreen(NativeExpenseState.Ready(context = expenseContext()))

        composeRule.onNodeWithTag(NativeUiTags.CashierExpenseEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ExpenseCategorySelector)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavRecords).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
    }

    @Test
    fun dirtyCashierNavigationRequiresDiscardConfirmation() {
        var navigated: CashierSection? = null
        setScreen(
            state = NativeExpenseState.Ready(context = expenseContext()),
            onNavigate = { navigated = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.ExpenseAmountInput)
            .performScrollTo()
            .performTextReplacement("40.00")
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).performClick()

        composeRule.onNodeWithText("Discard this expense?").assertIsDisplayed()
        assertEquals(null, navigated)

        composeRule.onNodeWithTag(NativeUiTags.ExpenseDiscardDraft).performClick()
        assertEquals(CashierSection.Sell, navigated)
    }

    @Test
    fun loadFailureRetainsCashierShellAndRetries() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeExpenseState by remember {
                    mutableStateOf(NativeExpenseState.LoadFailure("Cashier expense unavailable."))
                }
                CashierExpenseEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity(),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeExpenseState.Loading
                    },
                    onRecordExpense = {},
                    onRecordAnother = {},
                    onBackToMore = {},
                    onNavigate = {},
                )
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierExpenseEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.ExpenseRetryLoad).performClick()
        assertTrue(retried)
    }

    private fun setScreen(
        state: NativeExpenseState,
        onNavigate: (CashierSection) -> Unit = {},
        onBackToMore: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                CashierExpenseEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity(),
                    state = state,
                    onRetry = {},
                    onRecordExpense = {},
                    onRecordAnother = {},
                    onBackToMore = onBackToMore,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun expenseContext() = NativeExpenseContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        categories = listOf(NativeExpenseCategory(11L, "Electricity")),
        paymentAccounts = listOf(NativeExpensePaymentAccount(4L, "Cash")),
        defaultPaymentAccountId = 4L,
    )

    private fun identity() = NativeSignedInIdentity(
        userId = 2L,
        loginName = "cashier",
        displayName = "Counter Cashier",
        role = AppRole.CASHIER,
        effectivePermissions = RolePermissions.permissions(AppRole.CASHIER),
    )
}
