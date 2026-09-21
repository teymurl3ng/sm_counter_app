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
import com.saaf.milkshop.application.owner.NativeOwnerReportsCoverageKind
import com.saaf.milkshop.application.owner.NativeOwnerReportsPreset
import com.saaf.milkshop.application.owner.NativeOwnerReportsProductRow
import com.saaf.milkshop.application.owner.NativeOwnerReportsSnapshot
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerReportsState
import com.saaf.milkshop.nativeui.owner.OwnerReportsScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerReportsScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentShowsReportWithoutCashierChrome() {
        setScreen(NativeOwnerReportsState.Ready(snapshot = sampleSnapshot()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsContent).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsSummary).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsCoverage).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun backToOverviewFromContent() {
        var back = false
        setScreen(
            NativeOwnerReportsState.Ready(snapshot = sampleSnapshot()),
            onBackToOverview = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsBack).performClick()
        assertTrue(back)
    }

    @Test
    fun presetChipRequestsLastMonth() {
        var preset: NativeOwnerReportsPreset? = null
        setScreen(
            NativeOwnerReportsState.Ready(snapshot = sampleSnapshot()),
            onSelectPreset = { preset = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsPresetLastMonth).performClick()
        assertEquals(NativeOwnerReportsPreset.LastMonth, preset)
    }

    @Test
    fun customRangePreviewSurfacesNotice() {
        var preview = false
        setScreen(
            NativeOwnerReportsState.Ready(snapshot = sampleSnapshot()),
            onCustomRangePreview = { preview = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsCustomPreview).performClick()
        assertTrue(preview)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            NativeOwnerReportsState.LoadFailure("Month reports could not open."),
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsRetry).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun emptyProductsShowsHonestCopy() {
        setScreen(
            NativeOwnerReportsState.Ready(
                snapshot = sampleSnapshot().copy(
                    coverageKind = NativeOwnerReportsCoverageKind.Empty,
                    coverageLabel = "No product sales in this period",
                    products = emptyList(),
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerReportsProductsEmpty)
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setScreen(
        state: NativeOwnerReportsState,
        onBackToOverview: () -> Unit = {},
        onRetry: () -> Unit = {},
        onSelectPreset: (NativeOwnerReportsPreset) -> Unit = {},
        onCustomRangePreview: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                OwnerReportsScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = NativeSignedInIdentity(
                        userId = 1L,
                        loginName = "owner",
                        displayName = "Reports Owner",
                        role = AppRole.OWNER,
                        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
                    ),
                    state = state,
                    onBackToOverview = onBackToOverview,
                    onRetry = onRetry,
                    onSelectPreset = onSelectPreset,
                    onCustomRangePreview = onCustomRangePreview,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleSnapshot() = NativeOwnerReportsSnapshot(
        shopName = "Saaf Milk",
        businessDate = "2026-09-21",
        postingOpen = true,
        preset = NativeOwnerReportsPreset.ThisMonth,
        periodLabel = "2026-09-01 through 2026-09-30",
        startInclusive = "2026-09-01",
        endExclusive = "2026-10-01",
        coverageKind = NativeOwnerReportsCoverageKind.Complete,
        coverageLabel = "Cost history complete",
        coverageDetail = "The app has saved cost information for the stock sold in this period.",
        deliverySurchargeLabel = "PKR 12.00",
        operatingExpensesLabel = "PKR 100.00",
        salaryPaymentsLabel = "PKR 50.00",
        estimatedResultLabel = "PKR 240.00",
        products = listOf(
            NativeOwnerReportsProductRow(
                productId = 1L,
                productName = "Milk",
                salesRevenueLabel = "PKR 400.00",
                costLabel = "PKR 200.00",
                profitLabel = "PKR 200.00",
                coverageLabel = "Cost history complete",
            ),
        ),
        customRangeDeferredNotice = "Custom date range comes next in this preview.",
    )
}
