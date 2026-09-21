package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.cashclose.NativeCashCloseContext
import com.saaf.milkshop.application.cashclose.NativeCashCloseReopenSubmission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
import com.saaf.milkshop.nativeui.owner.OwnerCashCloseEntryScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerCashCloseScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyUsesOwnerChromeAndExcludesCashierNavigation() {
        setScreen(NativeCashCloseState.Ready(context = openContext()))

        composeRule.onNodeWithTag(NativeUiTags.OwnerCashCloseEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashCloseAmountInput)
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
            state = NativeCashCloseState.Ready(context = openContext()),
            onNavigate = { navigated = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.CashCloseAmountInput)
            .performScrollTo()
            .performTextReplacement("250.00")
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()

        composeRule.onNodeWithText("Discard this count?").assertIsDisplayed()
        assertEquals(null, navigated)

        composeRule.onNodeWithTag(NativeUiTags.CashCloseDiscardDraft).performClick()
        assertEquals(OwnerSection.Overview, navigated)
    }

    @Test
    fun closedDateShowsReopenFormAndSubmits() {
        var reopenedReason: String? = null
        var reopenedPassphrase: CharArray? = null
        setScreen(
            state = NativeCashCloseState.Ready(context = closedContext()),
            onReopenDay = { reason, passphrase ->
                reopenedReason = reason
                reopenedPassphrase = passphrase
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.CashClosePostingClosed)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashCloseReopenDay)
            .performScrollTo()
            .assertIsNotEnabled()

        composeRule.onNodeWithTag(NativeUiTags.CashCloseReopenReason)
            .performScrollTo()
            .performTextReplacement("Correct a verified posting")
        composeRule.onNodeWithTag(NativeUiTags.CashCloseReopenPassphrase)
            .performScrollTo()
            .performTextReplacement("owner-full-passphrase")
        composeRule.onNodeWithTag(NativeUiTags.CashCloseReopenDay)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Correct a verified posting", reopenedReason)
        assertTrue(reopenedPassphrase.contentEquals("owner-full-passphrase".toCharArray()))
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertDoesNotExist()
    }

    @Test
    fun reopenSuccessShowsOpenFeedbackUnderOwnerChrome() {
        val success = NativeCashCloseReopenSubmission(
            businessDate = "2026-09-21",
            actionId = 42L,
            refreshedContext = openContext(),
        )
        setScreen(
            NativeCashCloseState.Ready(
                context = openContext(),
                reopenSuccess = success,
            ),
        )

        composeRule.onNodeWithTag(NativeUiTags.CashCloseReopenSuccess)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashClosePostingOpen).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashCloseReopenDay).assertDoesNotExist()
    }

    @Test
    fun loadFailureRetainsOwnerShellAndRetries() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeCashCloseState by remember {
                    mutableStateOf(NativeCashCloseState.LoadFailure("Owner cash close unavailable."))
                }
                OwnerCashCloseEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeCashCloseState.Loading
                    },
                    onRecordCount = {},
                    onCloseDay = {},
                    onReopenDay = { _, _ -> },
                    onBackToWork = {},
                    onNavigate = {},
                )
            }
        }

        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithText("Owner cash close unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashCloseRetryLoad).performClick()
        composeRule.onNodeWithText("Loading cash count and close…").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertTrue(retried)
    }

    private fun setScreen(
        state: NativeCashCloseState,
        onNavigate: (OwnerSection) -> Unit = {},
        onReopenDay: (String, CharArray) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerCashCloseEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {},
                    onRecordCount = {},
                    onCloseDay = {},
                    onReopenDay = onReopenDay,
                    onBackToWork = {},
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun ownerIdentity() = NativeSignedInIdentity(
        userId = 1L,
        loginName = "owner.cash.close.contract",
        displayName = "Contract Owner",
        role = AppRole.OWNER,
        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
    )

    private fun openContext() = NativeCashCloseContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        cashAccountId = 4L,
        cashAccountName = "Cash",
        expectedCashPaisa = 12_500L,
        latestCount = null,
        reopenDeferredNotice = null,
    )

    private fun closedContext() = NativeCashCloseContext(
        businessDate = "2026-09-21",
        postingOpen = false,
        cashAccountId = 4L,
        cashAccountName = "Cash",
        expectedCashPaisa = 12_500L,
        latestCount = null,
        reopenDeferredNotice = null,
    )
}
