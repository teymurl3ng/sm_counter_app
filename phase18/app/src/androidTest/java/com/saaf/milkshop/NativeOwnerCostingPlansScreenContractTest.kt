package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerCostingComponentRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyDetail
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerCostingPlansScreen
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerCostingPlansScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsPoliciesWithoutCashierChrome() {
        setScreen(policies = listOf(samplePolicy()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingPolicy(7L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(
            policies = emptyList(),
            currentMonthSummary = "No costing policy for 2026-09 yet.",
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No costing policies yet.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansSummary).assertIsDisplayed()
    }

    @Test
    fun policyRowOpensDetail() {
        var opened: Long? = null
        setScreen(
            policies = listOf(samplePolicy()),
            onOpenPolicy = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingPolicy(7L)).performClick()
        assertEquals(7L, opened)
    }

    @Test
    fun detailShowsFactsAndSupersedeForActive() {
        var supersede = false
        setScreen(
            policies = listOf(samplePolicy()),
            policyDetail = NativeOwnerCostingPolicyDetail(
                row = samplePolicy(),
                normalCapacityKg = "100",
                notes = "Seeded month",
                approvedAt = "2026-09-01T10:00:00Z",
                components = listOf(
                    NativeOwnerCostingComponentRow(
                        componentCode = "culture",
                        sourceAmountPaisa = 1_000L,
                        sourceAmountLabel = "PKR 10.00",
                        basisQuantityKg = "1",
                    ),
                ),
            ),
            onOpenApproveFromDetail = { supersede = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailMonth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailSupersede)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(supersede)
    }

    @Test
    fun supersededDetailShowsDeferredEditNotice() {
        setScreen(
            policies = listOf(samplePolicy().copy(statusLabel = "Superseded")),
            policyDetail = NativeOwnerCostingPolicyDetail(
                row = samplePolicy().copy(statusLabel = "Superseded"),
                normalCapacityKg = "100",
                notes = null,
                approvedAt = "2026-09-01T10:00:00Z",
                components = emptyList(),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailDeferredEdit)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailSupersede).assertDoesNotExist()
    }

    @Test
    fun backToMenuFromList() {
        var back = false
        setScreen(
            policies = listOf(samplePolicy()),
            onBackToMenu = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansBack).performClick()
        assertTrue(back)
    }

    @Test
    fun approveOpensFormAndSavesTypedInput() {
        var saved: CostingPolicyInput? = null
        setScreen(
            policies = emptyList(),
            approveOpen = true,
            reviewReasons = CostingPolicyService.REVIEW_REASONS.sorted(),
            componentCodes = listOf(
                "culture",
                "packaging",
                "variable_energy",
                "direct_labor",
                CostingPolicyService.COMPONENT_FIXED_OVERHEAD,
            ),
            onApprovePolicy = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansApproveForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansApproveMonth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingComponentAmount("culture"))
            .performScrollTo()
            .performTextInput("10")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansApproveSave)
            .performScrollTo()
            .performClick()
        assertNotNull(saved)
        assertEquals("2026-09", saved!!.calendarMonth)
        assertEquals("monthly", saved!!.reviewReason)
        assertEquals(5, saved!!.components.size)
        assertEquals(1_000L, saved!!.components.first { it.code == "culture" }.sourceAmountPaisa)
        assertEquals(null, saved!!.supersedesPolicyId)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            policies = emptyList(),
            policiesError = "Costing policies could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansRetry).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun reauthDialogIsShownWhenRequired() {
        var cancelled = false
        setScreen(
            policies = emptyList(),
            approveOpen = true,
            reauthOpen = true,
            onCancelReauth = { cancelled = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansReauth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansReauthCancel).performClick()
        assertTrue(cancelled)
    }

    private fun setScreen(
        policies: List<NativeOwnerCostingPolicyRow>,
        policiesLoading: Boolean = false,
        policyDetail: NativeOwnerCostingPolicyDetail? = null,
        policiesError: String? = null,
        currentMonthSummary: String? = "Current 2026-09: Active · 1000 paisa/kg",
        approveOpen: Boolean = false,
        approveSeed: NativeOwnerCostingPolicyDetail? = null,
        approving: Boolean = false,
        reauthOpen: Boolean = false,
        reauthBusy: Boolean = false,
        reviewReasons: List<String> = emptyList(),
        componentCodes: List<String> = emptyList(),
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenPolicy: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenApprove: () -> Unit = {},
        onOpenApproveFromDetail: () -> Unit = {},
        onBackFromApprove: () -> Unit = {},
        onApprovePolicy: (CostingPolicyInput) -> Unit = {},
        onSubmitReauth: (CharArray) -> Unit = { it.fill('\u0000') },
        onCancelReauth: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerCostingPlansScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = NativeSignedInIdentity(
                        userId = 1L,
                        loginName = "owner",
                        displayName = "Owner",
                        role = AppRole.OWNER,
                        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
                    ),
                    context = NativeOwnerMoreContext(
                        businessDate = "2026-09-21",
                        postingOpen = true,
                        shopName = "Saaf Milk",
                    ),
                    policies = policies,
                    policiesLoading = policiesLoading,
                    policyDetail = policyDetail,
                    policiesError = policiesError,
                    currentMonthSummary = currentMonthSummary,
                    approveOpen = approveOpen,
                    approveSeed = approveSeed,
                    approving = approving,
                    reauthOpen = reauthOpen,
                    reauthBusy = reauthBusy,
                    reviewReasons = reviewReasons,
                    componentCodes = componentCodes,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenPolicy = onOpenPolicy,
                    onBackFromDetail = onBackFromDetail,
                    onOpenApprove = onOpenApprove,
                    onOpenApproveFromDetail = onOpenApproveFromDetail,
                    onBackFromApprove = onBackFromApprove,
                    onApprovePolicy = onApprovePolicy,
                    onSubmitReauth = onSubmitReauth,
                    onCancelReauth = onCancelReauth,
                    hostedScaffold = { snackbar, mod, wrapNav, content ->
                        OwnerScaffold(
                            snackbarHostState = snackbar,
                            section = OwnerSection.More,
                            onNavigate = { section -> wrapNav { } },
                            modifier = mod,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun samplePolicy() = NativeOwnerCostingPolicyRow(
        policyId = 7L,
        calendarMonth = "2026-09",
        statusLabel = "Active",
        conversionCostLabel = "1000 paisa/kg",
        reviewReason = "monthly",
    )
}
