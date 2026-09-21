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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerCostingComponentRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyDetail
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerCostingPlansScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
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
    fun detailShowsFactsAndBack() {
        var back = false
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
                        sourceAmountLabel = "PKR 10.00",
                        basisQuantityKg = "1",
                    ),
                ),
            ),
            onBackFromDetail = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailMonth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailReadOnly)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailBack).performClick()
        assertTrue(back)
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
    fun approvePreviewSurfacesNotice() {
        var preview = false
        setScreen(
            policies = listOf(samplePolicy()),
            onApprovePreview = { preview = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansApprovePreview).performClick()
        assertTrue(preview)
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

    private fun setScreen(
        policies: List<NativeOwnerCostingPolicyRow>,
        policiesLoading: Boolean = false,
        policyDetail: NativeOwnerCostingPolicyDetail? = null,
        policiesError: String? = null,
        currentMonthSummary: String? = "Current 2026-09: Active · 1000 paisa/kg",
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenPolicy: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onApprovePreview: () -> Unit = {},
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
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenPolicy = onOpenPolicy,
                    onBackFromDetail = onBackFromDetail,
                    onApprovePreview = onApprovePreview,
                    onNavigate = onNavigate,
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
