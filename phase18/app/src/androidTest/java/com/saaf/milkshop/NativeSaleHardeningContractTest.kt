package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.sale.NativeSaleAccount
import com.saaf.milkshop.application.sale.NativeSaleContext
import com.saaf.milkshop.application.sale.NativeSaleProduct
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierSaleScreen
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSaleHardeningContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun closedBusinessDayExplainsTheBlockAndDisablesRecord() {
        setSaleContent(context(postingOpen = false))

        composeRule.onNodeWithTag(NativeUiTags.CashierPostingClosed)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.onNodeWithText(
            "is closed. Reopen the day before recording a sale.",
            substring = true,
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale)
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun missingProductAndPaymentReferencesExplainWhatTheOwnerMustFix() {
        setSaleContent(
            context(
                products = emptyList(),
                paymentAccounts = emptyList(),
                defaultPaymentAccountId = null,
            ),
        )

        composeRule.onNodeWithTag(NativeUiTags.CashierProductUnavailable).assertIsDisplayed()
        composeRule.onNodeWithText(
            "No active sale product has a retail rate. Ask an owner or manager to configure one.",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentUnavailable)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Unavailable").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale)
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun largeTextDarkThemeKeepsCoreActionsAndTalkBackStateAvailable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f),
            ) {
                SaafTheme(darkTheme = true) {
                    CashierSaleScreen(
                        snackbarHostState = remember { SnackbarHostState() },
                        identity = identity,
                        state = NativeSaleState.Ready(context()),
                        onRetry = {},
                        onClearFeedback = {},
                        onRecordSale = {},
            onSendAsDelivery = {},
            onStartNextSale = {},
                        onNavigate = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Sale quantity")
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierSecondaryDetails)
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun longCustomerListScrollsToTheLastAccountAndSelectsIt() {
        val customers = (1L..30L).map { NativeSaleAccount(it, "Customer $it") }
        setSaleContent(context(customers = customers))

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer)
            .performScrollTo()
            .performClick()
        val customerTag = NativeUiTags.cashierCustomer(30L)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(NativeUiTags.CashierCustomerList)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerList)
            .performScrollToNode(hasTestTag(customerTag))
        composeRule.onNodeWithTag(customerTag).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).assertIsDisplayed()
        composeRule.onNodeWithText("Customer 30").assertIsDisplayed()
    }

    private fun setSaleContent(saleContext: NativeSaleContext) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                CashierSaleScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity,
                    state = NativeSaleState.Ready(saleContext),
                    onRetry = {},
                    onClearFeedback = {},
                    onRecordSale = {},
            onSendAsDelivery = {},
            onStartNextSale = {},
                    onNavigate = {},
                )
            }
        }
    }

    private fun context(
        postingOpen: Boolean = true,
        products: List<NativeSaleProduct> = listOf(
            NativeSaleProduct(1L, "Milk", "liters", 24000L, "18.0"),
            NativeSaleProduct(2L, "Yogurt", "kg", 30000L, "8.0"),
        ),
        customers: List<NativeSaleAccount> = listOf(NativeSaleAccount(20L, "Ahmed Store")),
        paymentAccounts: List<NativeSaleAccount> = listOf(NativeSaleAccount(10L, "Cash")),
        defaultPaymentAccountId: Long? = paymentAccounts.firstOrNull()?.accountId,
    ) = NativeSaleContext(
        businessDate = "2026-09-19",
        postingOpen = postingOpen,
        products = products,
        customers = customers,
        paymentAccounts = paymentAccounts,
        defaultPaymentAccountId = defaultPaymentAccountId,
        defaultPaymentAccountBalancePaisa = 125000L,
        todaySalesPaisa = 50000L,
    )

    private companion object {
        val identity = NativeSignedInIdentity(
            userId = 1L,
            loginName = "hardening.owner",
            displayName = "Hardening Owner",
            role = AppRole.OWNER,
            effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
        )
    }
}
