package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
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
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleCheque)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliveryCard)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleCheque)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliverySend))
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleCheque).performTouchInput {
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
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleCheque)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliveryRoute))
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleDeliveryRoute).performTextInput("Morning route")
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleCheque)
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
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleCheque)
            .performScrollToNode(hasTestTag(NativeUiTags.CashierSaleDeliveryLinked)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleDeliveryCard).assertDoesNotExist()
    }

    @Test
    fun salePaymentPickerCancelsAndChangesDestination() {
        composeRule.setContent {
            SaafTheme {
                CashierSaleScreen(remember { SnackbarHostState() }, testIdentity(),
                    NativeSaleState.Ready(creditContext().copy(paymentAccounts = listOf(NativeSaleAccount(10, "Cash"), NativeSaleAccount(11, "Bank")), defaultPaymentAccountId = 10)),
                    {}, {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentAccountSelector).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCancel).performClick()
        composeRule.onNodeWithTag(NativeUiTags.cashierPaymentAccount(11)).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentAccountSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.cashierPaymentAccount(11)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentAccountSelector).assertTextContains("Bank")
    }

    @Test
    fun saleWholesaleRateAndNoteStayEditable() {
        composeRule.setContent {
            SaafTheme {
                CashierSaleScreen(remember { SnackbarHostState() }, testIdentity(), NativeSaleState.Ready(creditContext()),
                    {}, {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierSecondaryDetails).performScrollTo().performClick()
        composeRule.onNodeWithText("Wholesale").performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierWholesaleRate).performScrollTo().performTextReplacement("100.25")
        composeRule.onNodeWithTag(NativeUiTags.CashierWholesaleRate)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("100.25")))
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleNote).performScrollTo().performTextReplacement("Delivery note")
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleNote)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Delivery note")))
    }

    @Test
    fun saleChequeNextReturnsToForm() {
        var next = 0
        composeRule.setContent {
            SaafTheme {
                var state by remember { mutableStateOf<NativeSaleState>(NativeSaleState.Ready(creditContext(), receipt = NativeSaleReceipt(42, 1.0, 12000))) }
                CashierSaleScreen(remember { SnackbarHostState() }, testIdentity(), state,
                    {}, {}, {}, {}, { next++; state = NativeSaleState.Ready(creditContext()) }, {})
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleCheque).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleNext).performScrollTo().performClick()
        assertEquals(1, next)
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleScroll).assertIsDisplayed()
    }

    @Test
    fun catalogPickerHandlesZeroOneTwoThreeTwelveAndFiftyProducts() {
        var state by mutableStateOf<NativeSaleState>(NativeSaleState.Ready(creditContext().copy(products = emptyList())))
        var submitted: com.saaf.milkshop.application.sale.NativeSaleRequest? = null
        composeRule.setContent {
            SaafTheme {
                CashierSaleScreen(remember { SnackbarHostState() }, testIdentity(), state,
                    {}, {}, { submitted = it }, {}, {}, {})
            }
        }
        composeRule.onNodeWithText("No active products", substring = true).assertIsDisplayed()
        for (count in listOf(1, 2, 3, 12, 50)) {
            val products = (1..count).map { index -> NativeSaleProduct(
                index.toLong(), "Product $index", "Kg", index * 10_000L, "12 kg",
            ) }
            state = NativeSaleState.Ready(creditContext().copy(products = products,
                paymentAccounts = listOf(NativeSaleAccount(10L, "Cash", 100_000L)),
                defaultPaymentAccountId = 10L, defaultPaymentAccountBalancePaisa = 100_000L))
            if (count > 6) {
                val previous = submitted!!.productId
                composeRule.onNodeWithTag(NativeUiTags.CashierProductPickerOpen).performScrollTo().performClick()
                composeRule.onNodeWithTag(NativeUiTags.CashierProductPickerCancel).performClick()
                composeRule.onNodeWithTag(NativeUiTags.CashierProductPickerOpen).assertTextContains("Product $previous")
                composeRule.onNodeWithTag(NativeUiTags.CashierProductPickerOpen).performClick()
                composeRule.onNodeWithTag(NativeUiTags.CashierProductSearch).performTextReplacement("Product $count")
                composeRule.onNodeWithTag(NativeUiTags.CashierProductSearch).performImeAction()
                composeRule.onNodeWithTag(NativeUiTags.cashierProductPick(count.toLong())).performClick()
                composeRule.onNodeWithTag(NativeUiTags.CashierProductPickerOpen).assertTextContains("Product $count")
            } else {
                composeRule.onNodeWithTag(NativeUiTags.cashierProduct("Product $count"))
                    .performScrollTo().performClick().assertIsSelected()
            }
            composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performScrollTo().performTextReplacement("1")
            composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performImeAction()
            composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().performClick()
            assertEquals(count.toLong(), submitted!!.productId)
        }
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
        defaultSurchargeRs = 50.0,
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
