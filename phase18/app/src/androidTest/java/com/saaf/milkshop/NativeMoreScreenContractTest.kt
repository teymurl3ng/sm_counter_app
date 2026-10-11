package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.saaf.milkshop.qa.NativeLayoutImeEvidence
import com.saaf.milkshop.qa.NativeEvidenceCapture
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.more.NativeBlindCashCountReceipt
import com.saaf.milkshop.application.more.NativeMoreContext
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.more.CashierMoreScreen
import com.saaf.milkshop.nativeui.more.NativeMoreDestination
import com.saaf.milkshop.nativeui.more.NativeMoreState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeMoreScreenContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun menuShowsCashierActionsAndKeepsMoreNavSelected() {
        setScreen(NativeMoreState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.MoreMenu).assertIsDisplayed()
        NativeEvidenceCapture.captureWholeWindow(composeRule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "ux", scenario = "cashier-more-surfaces", workflowId = "menuShowsCashierActionsAndKeepsMoreNavSelected",
            note = "Cashier navigation row surfaces; visual approval pending.",
        ))
        composeRule.onNodeWithTag(NativeUiTags.MoreExpense).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.MoreCountCash).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.MorePersonalSecurity).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.MoreSwitchAccount).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.MoreSignOut).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertIsSelected()
        composeRule.onNodeWithText("More is not connected in this native preview yet.").assertDoesNotExist()
    }

    @Test
    fun countCashRecordsBlindAmountAndShowsSuccessWithoutExpectedVariance() {
        var recorded: Pair<Long, String?>? = null
        var state by mutableStateOf(
            NativeMoreState.Ready(context = openContext(), destination = NativeMoreDestination.CountCash),
        )
        setScreen(
            stateProvider = { state },
            onRecordCashCount = { amount, notes ->
                recorded = amount to notes?.takeIf { it.isNotBlank() }
                state = (state as NativeMoreState.Ready).copy(
                    countReceipt = NativeBlindCashCountReceipt(
                        countId = 77L,
                        businessDate = "2026-09-20",
                        countedCashPaisa = amount,
                    ),
                )
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.MoreCountCashScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.MoreCountAmount).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.MoreCountRecord).assertIsDisplayed()

        val amountBounds = composeRule.onNodeWithTag(NativeUiTags.MoreCountAmount).fetchSemanticsNode().boundsInRoot
        val notesBounds = composeRule.onNodeWithTag(NativeUiTags.MoreCountNotes).fetchSemanticsNode().boundsInRoot
        val recordBounds = composeRule.onNodeWithTag(NativeUiTags.MoreCountRecord).fetchSemanticsNode().boundsInRoot
        assertTrue("Count and notes fields must not overlap", amountBounds.bottom <= notesBounds.top)
        assertTrue("Notes and Record button must not overlap", notesBounds.bottom <= recordBounds.top)
        NativeEvidenceCapture.captureWholeWindow(composeRule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "ux", scenario = "cashier-blind-count-form", workflowId = "countCashRecordsBlindAmountAndShowsSuccessWithoutExpectedVariance",
            note = "Real form geometry before input; blind count keeps expected total hidden.",
        ))
        composeRule.onNodeWithTag(NativeUiTags.MoreCountAmount).performTextReplacement("125.50")
        NativeLayoutImeEvidence.dismissImeAndWait(composeRule.activity, composeRule)
        composeRule.onNodeWithTag(NativeUiTags.MoreCountRecord).performScrollTo().performTouchInput { click() }
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithTag(NativeUiTags.MoreCountSuccess).fetchSemanticsNode() }.isSuccess
        }
        assertEquals(12_550L to null, recorded)
        composeRule.onNodeWithTag(NativeUiTags.MoreCountSuccess).assertIsDisplayed()
        composeRule.onNodeWithText("Counted PKR", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Record #77", substring = true).assertIsDisplayed()
    }

    @Test
    fun menuOpensCountCashEntry() {
        var opened = false
        setScreen(
            NativeMoreState.Ready(context = openContext()),
            onOpenCountCash = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.MoreCountCash).performClick()
        assertTrue(opened)
    }

    @Test
    fun menuOpensExpenseEntry() {
        var opened = false
        setScreen(
            NativeMoreState.Ready(context = openContext()),
            onOpenExpense = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.MoreExpense).performClick()
        assertTrue(opened)
    }

    @Test
    fun menuOpensDeliveriesFromMore() {
        var opened = false
        setScreen(
            NativeMoreState.Ready(context = openContext()),
            onOpenDeliveries = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.MoreDeliveries).performClick()
        assertTrue(opened)
    }

    @Test
    fun switchAccountAndSignOutFireFromMenu() {
        var switched = false
        var signedOut = false
        setScreen(
            NativeMoreState.Ready(context = openContext()),
            onSwitchAccount = { switched = true },
            onSignOut = { signedOut = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.MoreSwitchAccount).performClick()
        composeRule.onNodeWithTag(NativeUiTags.MoreSignOut).performClick()
        assertTrue(switched)
        assertTrue(signedOut)
    }

    @Test
    fun personalSecurityShowsStatus() {
        setScreen(
            NativeMoreState.Ready(
                context = openContext(),
                destination = NativeMoreDestination.PersonalSecurity,
                security = NativeMoreSecurityStatus(
                    quickPinEnabled = true,
                    phoneSecurityEnabled = false,
                    phoneSecurityAvailable = true,
                    phoneSecurityLabel = "Phone security",
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.MoreSecurityStatus).assertIsDisplayed()
        composeRule.onNodeWithText("Quick PIN: On").assertIsDisplayed()
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            NativeMoreState.LoadFailure("More could not be loaded."),
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.MoreRetryLoad).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun countReceiptDoneCallsBackOnceAndReturnsToMenu() {
        var state by mutableStateOf<NativeMoreState>(NativeMoreState.Ready(
            context = openContext(), destination = NativeMoreDestination.CountCash,
            countReceipt = NativeBlindCashCountReceipt(77L, "2026-09-20", 12550L),
        ))
        var left = 0
        setScreen(stateProvider = { state }, onBackToMenu = {
            left++
            state = NativeMoreState.Ready(context = openContext())
        })
        composeRule.onNodeWithTag(NativeUiTags.MoreCountDone).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.MoreMenu).assertIsDisplayed()
        assertEquals(1, left)
    }

    private fun setScreen(
        state: NativeMoreState,
        onRetry: () -> Unit = {},
        onOpenCountCash: () -> Unit = {},
        onOpenExpense: () -> Unit = {},
        onOpenDeliveries: () -> Unit = {},
        onRecordCashCount: (Long, String?) -> Unit = { _, _ -> },
        onSwitchAccount: () -> Unit = {},
        onSignOut: () -> Unit = {},
    ) {
        setScreen(
            stateProvider = { state },
            onRetry = onRetry,
            onOpenCountCash = onOpenCountCash,
            onOpenExpense = onOpenExpense,
            onOpenDeliveries = onOpenDeliveries,
            onRecordCashCount = onRecordCashCount,
            onSwitchAccount = onSwitchAccount,
            onSignOut = onSignOut,
        )
    }

    private fun setScreen(
        stateProvider: () -> NativeMoreState,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenCountCash: () -> Unit = {},
        onOpenExpense: () -> Unit = {},
        onOpenDeliveries: () -> Unit = {},
        onRecordCashCount: (Long, String?) -> Unit = { _, _ -> },
        onSwitchAccount: () -> Unit = {},
        onSignOut: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                val state = stateProvider()
                CashierMoreScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity(),
                    state = state,
                    onRetry = onRetry,
                    onOpenCountCash = onOpenCountCash,
                    onOpenExpense = onOpenExpense,
                    onOpenDeliveries = onOpenDeliveries,
                    onOpenPersonalSecurity = {},
                    onBackToMenu = onBackToMenu,
                    onRecordCashCount = onRecordCashCount,
                    onRequestRemoveQuickUnlock = {},
                    onCancelRemoveQuickUnlock = {},
                    onRemoveQuickUnlock = {},
                    onEnrollQuickPin = { _, _ -> },
                    onEnrollPhoneSecurity = { _, _ -> },
                    onSwitchAccount = onSwitchAccount,
                    onSignOut = onSignOut,
                    onNavigate = {},
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }

    private fun openContext() = NativeMoreContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        cashAccountId = 1L,
        cashAccountName = "Cash",
    )

    private fun identity() = NativeSignedInIdentity(
        userId = 1L,
        loginName = "cashier",
        displayName = "Counter Cashier",
        role = AppRole.CASHIER,
        effectivePermissions = RolePermissions.permissions(AppRole.CASHIER),
    )
}
