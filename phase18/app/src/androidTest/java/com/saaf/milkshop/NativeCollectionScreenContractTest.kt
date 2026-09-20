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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.collection.NativeCollectionContext
import com.saaf.milkshop.application.collection.NativeCollectionCustomer
import com.saaf.milkshop.application.collection.NativeCollectionCustomerBalance
import com.saaf.milkshop.application.collection.NativeCollectionPaymentAccount
import com.saaf.milkshop.application.collection.NativeCollectionReceipt
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.collection.CashierCollectionScreen
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeCollectionScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun customerSearchSelectionAmountShortcutsAccountAndNoteControlsChangeVisibleState() {
        var state by mutableStateOf<NativeCollectionState>(NativeCollectionState.Ready(context()))
        var selectedCustomerId: Long? = null
        setScreen(
            stateProvider = { state },
            onSelectCustomer = { accountId ->
                selectedCustomerId = accountId
                state = NativeCollectionState.Ready(
                    context(),
                    selectedCustomer = NativeCollectionCustomerBalance(
                        customer = customers.single { it.accountId == accountId },
                        balancePaisa = 150_000L,
                    ),
                )
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSearch).performTextReplacement("bilal")
        composeRule.onNodeWithTag(NativeUiTags.collectCustomer(1L)).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.collectCustomer(2L)).performClick()
        assertEquals(2L, selectedCustomerId)
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Owes PKR 1,500.00").fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Owes PKR 1,500.00").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CollectQuick1000).performClick()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("1000")
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("1000")
        composeRule.onNodeWithText("Bilal Dairy Point will owe PKR 500.00").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectPayFull).performClick()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("1500")
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("1500")
        composeRule.onNodeWithText("Bilal Dairy Point will be paid up").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentSelector).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.collectPaymentAccount(11L)).performClick()
        composeRule.onNodeWithText("Bank").assertIsDisplayed()

        expandCollectNote()
        composeRule.onNodeWithTag(NativeUiTags.CollectNoteInput).performTextReplacement("Counter receipt 41")
        composeRule.onNodeWithText("Optional · 18/240").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).assertIsSelected()
    }

    @Test
    fun normalPaymentSubmitsExactRequestAndSavingStateBlocksDuplicateTap() {
        var state by mutableStateOf<NativeCollectionState>(ready(balancePaisa = 250_000L))
        val requests = mutableListOf<NativeCollectionRequest>()
        setScreen(
            stateProvider = { state },
            onRecord = { request ->
                requests += request
                state = (state as NativeCollectionState.Ready).copy(saving = true)
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("1000.25")
        expandCollectNote()
        composeRule.onNodeWithTag(NativeUiTags.CollectNoteInput).performTextReplacement("Paid at counter")
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, requests.size)
        assertEquals(
            NativeCollectionRequest(
                customerAccountId = 1L,
                paymentAccountId = 10L,
                amountPaisa = 100_025L,
                notes = "Paid at counter",
            ),
            requests.single(),
        )
        composeRule.onNodeWithText("Recording…").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).assertIsNotEnabled().performClick()
        assertEquals(1, requests.size)
    }

    @Test
    fun overpaymentRequiresExplicitAdvanceConfirmationWithExactConsequences() {
        var request: NativeCollectionRequest? = null
        setScreen(
            stateProvider = { ready(balancePaisa = 50_000L) },
            onRecord = { request = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("650")
        composeRule.onNodeWithText("Ahmed Store will have PKR 150.00 advance")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().performClick()

        assertNull(request)
        waitForAdvanceDialog()
        composeRule.onNodeWithText("Ahmed Store currently owes PKR 500.00.").assertIsDisplayed()
        composeRule.onNodeWithText("You are recording PKR 650.00.").assertIsDisplayed()
        composeRule.onNodeWithText("PKR 150.00 will remain as customer advance.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectCancelAdvance).performClick()
        assertNull(request)
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectConfirmAdvance).fetchSemanticsNode()
            }.isFailure
        }

        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().performClick()
        waitForAdvanceDialog()
        composeRule.onNodeWithTag(NativeUiTags.CollectConfirmAdvance).performClick()
        assertNotNull(request)
        assertEquals(65_000L, request!!.amountPaisa)
    }

    @Test
    fun successShowsAuthoritativeResultAndBothActionsRespond() {
        var state by mutableStateOf<NativeCollectionState>(
            NativeCollectionState.Ready(
                context = context(),
                selectedCustomer = NativeCollectionCustomerBalance(customers.first(), -12_500L),
                receipt = NativeCollectionReceipt(
                    eventId = 42L,
                    customer = customers.first(),
                    paymentAccount = accounts.first(),
                    amountPaisa = 62_500L,
                    customerBalanceAfterPaisa = -12_500L,
                ),
            ),
        )
        var viewRecordId: Long? = null
        var collectAnotherCount = 0
        setScreen(
            stateProvider = { state },
            onCollectAnother = {
                collectAnotherCount += 1
                state = NativeCollectionState.Ready(context())
            },
            onViewRecord = { viewRecordId = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectSuccess).assertIsDisplayed()
        composeRule.onNodeWithText("PKR 625.00").assertIsDisplayed()
        composeRule.onNodeWithText("Has PKR 125.00 advance").assertIsDisplayed()
        composeRule.onNodeWithText("#42").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectViewRecord).performClick()
        assertEquals(42L, viewRecordId)
        composeRule.onNodeWithTag(NativeUiTags.CollectAnother).performClick()
        assertEquals(1, collectAnotherCount)
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).assertIsDisplayed()
    }

    @Test
    fun unavailableAndFailureStatesExplainRecoveryAndPreserveEnteredAmount() {
        val unavailable = NativeCollectionContext(
            businessDate = "2026-09-20",
            postingOpen = false,
            customers = emptyList(),
            paymentAccounts = emptyList(),
            defaultPaymentAccountId = null,
        )
        setScreen(stateProvider = { NativeCollectionState.Ready(unavailable) })

        composeRule.onNodeWithTag(NativeUiTags.CollectPostingClosed).assertIsDisplayed()
        composeRule.onNodeWithText("No active customers").assertIsDisplayed()
        composeRule.onNodeWithText("No active Cash/Bank account is available.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentSelector).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun loadRetryAndSaveFailureHaveVisibleRecoverableWorkflows() {
        var state by mutableStateOf<NativeCollectionState>(
            NativeCollectionState.LoadFailure("Customer payments are unavailable."),
        )
        setScreen(
            stateProvider = { state },
            onRetry = { state = NativeCollectionState.Loading },
        )
        composeRule.onNodeWithText("Customer payments are unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectRetryLoad).performClick()
        composeRule.onNodeWithText("Loading customer payments…").assertIsDisplayed()

        state = ready(balancePaisa = 250_000L)
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("725")
        state = (state as NativeCollectionState.Ready).copy(
            error = "The payment was not recorded. Your entries are still here—check them and try again.",
        )
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectError).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.CollectError).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("725")
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().assertIsEnabled()
    }

    private fun waitForAdvanceDialog() {
        composeRule.waitForIdle()
        composeRule.waitUntil(8_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectConfirmAdvance).assertIsDisplayed()
            }.isSuccess
        }
    }

    private fun expandCollectNote() {
        composeRule.waitForIdle()
        repeat(3) { attempt ->
            val visible = runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectNoteInput).fetchSemanticsNode()
            }.isSuccess
            if (visible) return
            composeRule.onNodeWithTag(NativeUiTags.CollectNoteToggle)
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()
            val opened = runCatching {
                composeRule.waitUntil(3_000) {
                    runCatching {
                        composeRule.onNodeWithTag(NativeUiTags.CollectNoteInput).fetchSemanticsNode()
                    }.isSuccess
                }
                true
            }.getOrDefault(false)
            if (opened) return
            check(attempt < 2) { "Collect note input did not open after toggle retries" }
        }
    }

    private fun setScreen(
        stateProvider: () -> NativeCollectionState,
        onRetry: () -> Unit = {},
        onSelectCustomer: (Long) -> Unit = {},
        onRecord: (NativeCollectionRequest) -> Unit = {},
        onCollectAnother: () -> Unit = {},
        onViewRecord: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                val state = stateProvider()
                CashierCollectionScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity,
                    state = state,
                    onRetry = onRetry,
                    onSelectCustomer = onSelectCustomer,
                    onRecordCollection = onRecord,
                    onCollectAnother = onCollectAnother,
                    onViewRecord = onViewRecord,
                    onLeaveConfirmed = {},
                    onNavigate = {},
                )
            }
        }
    }

    private fun ready(balancePaisa: Long): NativeCollectionState.Ready =
        NativeCollectionState.Ready(
            context = context(),
            selectedCustomer = NativeCollectionCustomerBalance(customers.first(), balancePaisa),
        )

    private fun context() = NativeCollectionContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        customers = customers,
        paymentAccounts = accounts,
        defaultPaymentAccountId = 10L,
    )

    private companion object {
        val customers = listOf(
            NativeCollectionCustomer(1L, "Ahmed Store", "0300 1111111"),
            NativeCollectionCustomer(2L, "Bilal Dairy Point", "0300 2222222"),
        )
        val accounts = listOf(
            NativeCollectionPaymentAccount(10L, "Cash"),
            NativeCollectionPaymentAccount(11L, "Bank"),
        )
        val identity = NativeSignedInIdentity(
            userId = 1L,
            loginName = "contract.cashier",
            displayName = "Contract Cashier",
            role = AppRole.CASHIER,
            effectivePermissions = RolePermissions.permissions(AppRole.CASHIER),
        )
    }
}
