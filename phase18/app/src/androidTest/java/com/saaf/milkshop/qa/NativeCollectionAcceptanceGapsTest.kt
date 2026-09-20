package com.saaf.milkshop.qa

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeCollectionAcceptanceGapsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun resetOrientation() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.waitForIdle()
    }

    @Test
    fun collectCancelPathsAndCustomerSwitchResetAmount() {
        var state by mutableStateOf<NativeCollectionState>(NativeCollectionState.Ready(context()))
        setScreen(
            stateProvider = { state },
            onSelectCustomer = { accountId ->
                state = NativeCollectionState.Ready(
                    context(),
                    selectedCustomer = NativeCollectionCustomerBalance(
                        customers.single { it.accountId == accountId },
                        balancePaisa = if (accountId == 1L) 150_000L else 80_000L,
                    ),
                )
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerCancel).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerDialog).assertDoesNotExist()
        composeRule.onNodeWithText("Choose customer").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.collectCustomer(1L)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("250")
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("250")

        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentSelector).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentCancel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentSelector)
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasText("Cash", substring = true))

        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectCustomerDialog).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.collectCustomer(2L)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Bilal Dairy Point").assertIsDisplayed()
        composeRule.waitForIdle()
        val amountAfterSwitch = composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text
        assertEquals("Customer switch must clear the previous amount", "", amountAfterSwitch)
    }

    @Test
    fun collectInvalidAmountAndPayFullDisabled() {
        setScreen(
            stateProvider = {
                NativeCollectionState.Ready(
                    context(),
                    selectedCustomer = NativeCollectionCustomerBalance(customers.first(), balancePaisa = 0L),
                )
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectPayFull).assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("0")
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("")
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("12.345")
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("12.34")
    }

    @Test
    fun collectDestinationRetentionOnCollectAnother() {
        var state by mutableStateOf<NativeCollectionState>(
            NativeCollectionState.Ready(
                context(),
                selectedCustomer = NativeCollectionCustomerBalance(customers.first(), 250_000L),
            ),
        )
        setScreen(
            stateProvider = { state },
            onRecord = {
                state = NativeCollectionState.Ready(
                    context = context(),
                    selectedCustomer = NativeCollectionCustomerBalance(customers.first(), 150_000L),
                    receipt = NativeCollectionReceipt(
                        eventId = 7L,
                        customer = customers.first(),
                        paymentAccount = accounts[1],
                        amountPaisa = 100_000L,
                        customerBalanceAfterPaisa = 150_000L,
                    ),
                )
            },
            onCollectAnother = {
                state = NativeCollectionState.Ready(context())
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentSelector).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.collectPaymentAccount(11L)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentSelector)
            .assert(hasText("Bank", substring = true))
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("1000")
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectSuccess).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectAnother).performClick()
        composeRule.onNodeWithText("Choose customer").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectPaymentSelector)
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasText("Bank", substring = true))
    }

    @Test
    fun collectNoMatchSearchAndImeDoneClearFocus() {
        setScreen(
            stateProvider = {
                NativeCollectionState.Ready(
                    context(),
                    selectedCustomer = NativeCollectionCustomerBalance(customers.first(), 150_000L),
                )
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSearch).performTextReplacement("zzz-no-match")
        composeRule.onNodeWithText("No customers match that search.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerCancel).performClick()

        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("100")
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performImeAction()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertIsNotFocused()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("100")
    }

    @Test
    fun collectDirtyBackKeepAndDiscard() {
        var left = false
        setScreen(
            stateProvider = {
                NativeCollectionState.Ready(
                    context(),
                    selectedCustomer = NativeCollectionCustomerBalance(customers.first(), 150_000L),
                )
            },
            onLeaveConfirmed = { left = true },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("40")
        composeRule.waitForIdle()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Discard this payment?").fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.CollectKeepDraft).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).assertTextEquals("40")
        assertEquals(false, left)

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectDiscardDraft).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.CollectDiscardDraft).performClick()
        assertTrue(left)
    }

    @Test
    fun collectLargeTextLandscapeSemanticsRemainAvailable() {
        try {
            composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            composeRule.waitForIdle()
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                    SaafTheme(darkTheme = false) {
                        CashierCollectionScreen(
                            snackbarHostState = remember { SnackbarHostState() },
                            identity = identity,
                            state = NativeCollectionState.Ready(
                                context(),
                                selectedCustomer = NativeCollectionCustomerBalance(customers.first(), 150_000L),
                            ),
                            onRetry = {},
                            onSelectCustomer = {},
                            onRecordCollection = {},
                            onCollectAnother = {},
                            onViewRecord = {},
                            onLeaveConfirmed = {},
                            onNavigate = {},
                        )
                    }
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput)
                .performScrollTo()
                .assertIsDisplayed()
                .assertContentDescriptionEquals("Collection amount")
            composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).assertIsDisplayed()
        } finally {
            composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            composeRule.waitForIdle()
        }
    }

    private fun setScreen(
        stateProvider: () -> NativeCollectionState,
        onSelectCustomer: (Long) -> Unit = {},
        onRecord: (NativeCollectionRequest) -> Unit = {},
        onCollectAnother: () -> Unit = {},
        onLeaveConfirmed: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                CashierCollectionScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity,
                    state = stateProvider(),
                    onRetry = {},
                    onSelectCustomer = onSelectCustomer,
                    onRecordCollection = onRecord,
                    onCollectAnother = onCollectAnother,
                    onViewRecord = {},
                    onLeaveConfirmed = onLeaveConfirmed,
                    onNavigate = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

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
