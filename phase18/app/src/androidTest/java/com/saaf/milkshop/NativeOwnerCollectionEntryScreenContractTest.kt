package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.owner.OwnerCollectionEntryScreen
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerCollectionEntryScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyUsesOwnerChromeAndExcludesCashierNavigation() {
        setScreen(NativeCollectionState.Ready(context = collectionContext()))

        composeRule.onNodeWithTag(NativeUiTags.OwnerCollectionEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector)
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
        val customer = NativeCollectionCustomer(7L, "Customer: Bilal", null)
        setScreen(
            state = NativeCollectionState.Ready(
                context = collectionContext(),
                selectedCustomer = NativeCollectionCustomerBalance(
                    customer = customer,
                    balancePaisa = 150_000L,
                ),
            ),
            onNavigate = { navigated = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput)
            .performScrollTo()
            .performTextReplacement("250.00")
        // Compact navigation is intentionally hidden behind the IME; dismiss it before leaving.
        if (composeRule.onAllNodesWithTag(NativeUiTags.OwnerNavOverview).fetchSemanticsNodes().isEmpty()) {
            androidx.test.uiautomator.UiDevice.getInstance(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
            ).pressBack()
        }
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).fetchSemanticsNode() }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()

        composeRule.onNodeWithText("Discard this payment?").assertIsDisplayed()
        assertEquals(null, navigated)

        composeRule.onNodeWithTag(NativeUiTags.CollectDiscardDraft).performClick()
        assertEquals(OwnerSection.Overview, navigated)
    }

    @Test
    fun loadFailureRetainsOwnerShellAndRetries() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeCollectionState by remember {
                    mutableStateOf(NativeCollectionState.LoadFailure("Owner collection unavailable."))
                }
                OwnerCollectionEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeCollectionState.Loading
                    },
                    onSelectCustomer = {},
                    onRecordCollection = {},
                    onCollectAnother = {},
                    onViewRecord = {},
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
        composeRule.onNodeWithText("Owner collection unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectRetryLoad).performClick()
        composeRule.onNodeWithText("Loading customer payments…").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertTrue(retried)
    }

    private fun setScreen(
        state: NativeCollectionState,
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        var activeGuard: com.saaf.milkshop.nativeui.common.EntryLeaveGuard? = null
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                val registerGuard = com.saaf.milkshop.nativeui.common.rememberPaneGuardRegistration(
                    active = { activeGuard }, publish = { activeGuard = it },
                )
                OwnerCollectionEntryScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = ownerIdentity(),
                    state = state,
                    onRetry = {},
                    onSelectCustomer = {},
                    onRecordCollection = {},
                    onCollectAnother = {},
                    onViewRecord = {},
                    onBackToWork = {},
                    onRegisterGuard = registerGuard,
                    ownScaffold = { s, m, content ->
                        OwnerScaffold(
                            snackbarHostState = s,
                            section = OwnerSection.Work,
                            onNavigate = { section ->
                                if (!((state as? NativeCollectionState.Ready)?.saving ?: false)) {
                                    activeGuard?.requestLeave { onNavigate(section) } ?: onNavigate(section)
                                }
                            },
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
        loginName = "owner.collection.contract",
        displayName = "Contract Owner",
        role = AppRole.OWNER,
        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
    )

    private fun collectionContext() = NativeCollectionContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        customers = listOf(NativeCollectionCustomer(7L, "Customer: Bilal", null)),
        paymentAccounts = listOf(NativeCollectionPaymentAccount(4L, "Cash")),
        defaultPaymentAccountId = 4L,
    )
}
