package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.sale.NativeSaleContext
import com.saaf.milkshop.application.sale.NativeSaleReceipt
import com.saaf.milkshop.application.sale.NativeSaleProduct
import com.saaf.milkshop.application.sale.NativeSaleAccount
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierSaleScreen
import com.saaf.milkshop.nativeui.sale.NativeSaleDeliveryFollowUp
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSaleScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saleLoadFailureRetryChangesToVisibleLoadingState() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeSaleState by remember {
                    mutableStateOf(NativeSaleState.LoadFailure("Today’s sale details are unavailable."))
                }
                CashierSaleScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = testIdentity(),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeSaleState.Loading
                    },
                    onClearFeedback = {},
                    onRecordSale = {},
                    onSendAsDelivery = {},
                    onStartNextSale = {},
                    onNavigate = {},
                )
            }
        }

        composeRule.onNodeWithText("Today’s sale details are unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierRetryLoad).performClick()
        composeRule.onNodeWithText("Loading today’s sale details…").assertIsDisplayed()
        assertTrue(retried)
    }

    @Test
    fun saleFollowUpOffersDeliveryForCreditSale() {
        var sent = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                CashierSaleScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = testIdentity(),
                    state = NativeSaleState.Ready(
                        context = creditContext(),
                        receipt = NativeSaleReceipt(eventId = 41, quantity = 5.0, totalPaisa = 60_000L),
                        deliveryFollowUp = followUp(41),
                    ),
                    onRetry = {},
                    onClearFeedback = {},
                    onRecordSale = {},
                    onSendAsDelivery = { sent = true },
                    onStartNextSale = {},
                    onNavigate = {},
                )
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleScroll)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliveryCard)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleScroll)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliverySend))
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleScroll).performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, -200f))
                up()
            }
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleDeliverySend).performClick()
        assertTrue(sent)
    }

    @Test
    fun saleFollowUpCreatesBornLinkedDelivery() {
        var sentCustomer: Long? = null
        var sentRoute: String? = null
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                CashierSaleScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = testIdentity(),
                    state = NativeSaleState.Ready(
                        context = creditContext(),
                        receipt = NativeSaleReceipt(eventId = 42, quantity = 3.0, totalPaisa = 36_000L),
                        deliveryFollowUp = followUp(42),
                    ),
                    onRetry = {},
                    onClearFeedback = {},
                    onRecordSale = {},
                    onSendAsDelivery = { sentRoute = it },
                    onStartNextSale = {},
                    onNavigate = {},
                )
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleScroll)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliveryRoute))
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleDeliveryRoute).performTextInput("Morning route")
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleScroll)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliverySend))
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleDeliverySend).performClick()
        assertEquals("Morning route", sentRoute)
    }

    @Test
    fun saleFollowUpClearsAfterSent() {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                CashierSaleScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = testIdentity(),
                    state = NativeSaleState.Ready(
                        context = creditContext(),
                        receipt = NativeSaleReceipt(eventId = 42, quantity = 3.0, totalPaisa = 36_000L),
                        deliveryFollowUp = followUp(42).copy(sending = false, sent = true),
                    ),
                    onRetry = {},
                    onClearFeedback = {},
                    onRecordSale = {},
                    onSendAsDelivery = {},
                    onStartNextSale = {},
                    onNavigate = {},
                )
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleScroll)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliveryLinked)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleDeliveryCard).assertDoesNotExist()
    }

    private fun testIdentity() = NativeSignedInIdentity(
        userId = 1L,
        loginName = "contract.owner",
        displayName = "Contract Owner",
        role = AppRole.OWNER,
        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
    )

    private fun creditContext() = NativeSaleContext(
        businessDate = "2026-09-27",
        postingOpen = true,
        products = listOf(
            NativeSaleProduct(productId = 3L, name = "Milk", unit = "Liters", ratePaisaPerUnit = 12_000L, quantityOnHand = "12 L"),
        ),
        customers = listOf(NativeSaleAccount(accountId = 7L, name = "Noor Store")),
        paymentAccounts = emptyList(),
        defaultPaymentAccountId = null,
        defaultPaymentAccountBalancePaisa = null,
        todaySalesPaisa = 0L,
    )

    private fun followUp(eventId: Long) = NativeSaleDeliveryFollowUp(
        customerId = 7L,
        productId = 3L,
        customerName = "Noor Store",
        productName = "Milk",
        quantity = "5.0",
        saleEventId = eventId,
    )
}
