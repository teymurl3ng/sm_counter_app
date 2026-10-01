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
import com.saaf.milkshop.application.sale.NativeSaleAccount
import com.saaf.milkshop.application.sale.NativeSaleContext
import com.saaf.milkshop.application.sale.NativeSaleProduct
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerSaleEntryScreen
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerSaleEntryScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyUsesOwnerChromeAndExcludesCashierNavigation() {
        setScreen(NativeSaleState.Ready(context = saleContext()))

        composeRule.onNodeWithTag(NativeUiTags.OwnerSaleEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput)
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
            state = NativeSaleState.Ready(context = saleContext()),
            onNavigate = { navigated = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput)
            .performScrollTo()
            .performTextReplacement("3.0")
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()

        composeRule.onNodeWithText("Discard this sale draft?").assertIsDisplayed()
        assertEquals(null, navigated)

        composeRule.onNodeWithTag(NativeUiTags.CashierDiscardDraft).performClick()
        assertEquals(OwnerSection.Overview, navigated)
    }

    @Test
    fun loadFailureRetainsOwnerShellAndRetries() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeSaleState by remember {
                    mutableStateOf(NativeSaleState.LoadFailure("Owner sale unavailable."))
                }
                OwnerSaleEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeSaleState.Loading
                    },
                    onClearFeedback = {},
                    onRecordSale = {},
            onSendAsDelivery = {},
            onStartNextSale = {},
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
        composeRule.onNodeWithText("Owner sale unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierRetryLoad).performClick()
        composeRule.onNodeWithText("Loading today’s sale details…").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertTrue(retried)
    }

    private fun setScreen(
        state: NativeSaleState,
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerSaleEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {},
                    onClearFeedback = {},
                    onRecordSale = {},
            onSendAsDelivery = {},
            onStartNextSale = {},
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
        loginName = "owner.sale.contract",
        displayName = "Contract Owner",
        role = AppRole.OWNER,
        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
    )

    private fun saleContext() = NativeSaleContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        products = listOf(
            NativeSaleProduct(
                productId = 1L,
                name = "Milk",
                unit = "liters",
                ratePaisaPerUnit = 25000L,
                quantityOnHand = "48.0",
            ),
        ),
        customers = listOf(NativeSaleAccount(7L, "Customer: Bilal")),
        paymentAccounts = listOf(NativeSaleAccount(4L, "Cash")),
        defaultPaymentAccountId = 4L,
        defaultPaymentAccountBalancePaisa = 125000L,
        todaySalesPaisa = 0L,
    )
}
