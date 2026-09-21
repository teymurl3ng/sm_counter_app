package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerOverviewAttention
import com.saaf.milkshop.application.owner.NativeOwnerOverviewAttentionKind
import com.saaf.milkshop.application.owner.NativeOwnerOverviewLatestActivity
import com.saaf.milkshop.application.owner.NativeOwnerOverviewSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerOverviewStockLine
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerOverviewState
import com.saaf.milkshop.nativeui.owner.OwnerOverviewScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerOverviewScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overviewShowsDecisionSummaryAndKeepsOverviewNavSelected() {
        setScreen(NativeOwnerOverviewState.Ready(snapshot = attentionSnapshot()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewAttention).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewSales).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewExpectedCash).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewStock).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewLatestActivity).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertIsSelected()
        composeRule.onNodeWithText("PKR 472.00").assertIsDisplayed()
    }

    @Test
    fun clearStateShowsNoUrgentIssues() {
        setScreen(NativeOwnerOverviewState.Ready(snapshot = clearSnapshot()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewClear).assertIsDisplayed()
        composeRule.onNodeWithText("No urgent issues").assertIsDisplayed()
    }

    @Test
    fun coverageWarningIsVisibleWhenIncomplete() {
        setScreen(
            NativeOwnerOverviewState.Ready(
                snapshot = clearSnapshot().copy(
                    coverageComplete = false,
                    coverageWarning = "Cost coverage is incomplete.",
                    estimatedGrossPaisa = 10000L,
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewCoverageWarning).assertIsDisplayed()
        composeRule.onNodeWithText("Cost coverage is incomplete.").assertIsDisplayed()
    }

    @Test
    fun workNavShowsPreviewNoticeDestination() {
        var navigated: OwnerSection? = null
        setScreen(
            NativeOwnerOverviewState.Ready(snapshot = attentionSnapshot()),
            onNavigate = { navigated = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).performClick()
        assertEquals(OwnerSection.Work, navigated)
    }

    @Test
    fun workSectionShowsOpeningPlaceholderWhileHosted() {
        setScreen(
            NativeOwnerOverviewState.Ready(snapshot = attentionSnapshot()),
            section = OwnerSection.Work,
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkLoading).assertIsDisplayed()
        composeRule.onNodeWithText("Opening work…").assertIsDisplayed()
    }

    @Test
    fun recordsNavRequestsRecordsDestination() {
        var navigated: OwnerSection? = null
        setScreen(
            NativeOwnerOverviewState.Ready(snapshot = attentionSnapshot()),
            onNavigate = { navigated = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.OwnerNavRecords).performClick()
        assertEquals(OwnerSection.Records, navigated)
    }

    @Test
    fun recordsSectionShowsOpeningPlaceholderWhileHosted() {
        setScreen(
            NativeOwnerOverviewState.Ready(snapshot = attentionSnapshot()),
            section = OwnerSection.Records,
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsLoading).assertIsDisplayed()
        composeRule.onNodeWithText("Opening records…").assertIsDisplayed()
    }

    @Test
    fun moreNavRequestsMoreDestination() {
        var navigated: OwnerSection? = null
        setScreen(
            NativeOwnerOverviewState.Ready(snapshot = attentionSnapshot()),
            onNavigate = { navigated = it },
        )

        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).performClick()
        assertEquals(OwnerSection.More, navigated)
    }

    @Test
    fun morePreviewShowsExplicitNotConnectedCopy() {
        setScreen(
            NativeOwnerOverviewState.Ready(snapshot = attentionSnapshot()),
            section = OwnerSection.More,
        )

        composeRule.onNodeWithTag(NativeUiTags.OwnerMorePreview).assertIsDisplayed()
        composeRule.onNodeWithText("Owner More comes next").assertIsDisplayed()
    }

    private fun setScreen(
        state: NativeOwnerOverviewState,
        section: OwnerSection = OwnerSection.Overview,
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                OwnerOverviewScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    state = state,
                    section = section,
                    onRetry = {},
                    onLeaveConfirmed = {},
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun attentionSnapshot() = NativeOwnerOverviewSnapshot(
        businessDate = "2026-09-21",
        postingOpen = true,
        shopName = "Saaf Milk",
        todaySalesPaisa = 47200L,
        estimatedGrossPaisa = 18240L,
        coverageComplete = true,
        coverageWarning = null,
        expectedCashPaisa = 2845000L,
        customersDuePaisa = 18430000L,
        suppliersDuePaisa = 9650000L,
        stock = listOf(
            NativeOwnerOverviewStockLine("Milk", "65.5 L"),
            NativeOwnerOverviewStockLine("Yogurt", "27 kg"),
        ),
        attention = listOf(
            NativeOwnerOverviewAttention(
                kind = NativeOwnerOverviewAttentionKind.CashCountWaiting,
                title = "Cash count waiting",
                detail = "No drawer count has been submitted for today.",
                actionLabel = "Review",
            ),
        ),
        latestActivity = NativeOwnerOverviewLatestActivity(
            title = "Sale · #S-1842",
            subtitle = "Areeba · Cash",
            trailingLabel = "PKR 600.00",
            stateLabel = "Completed",
        ),
    )

    private fun clearSnapshot() = attentionSnapshot().copy(attention = emptyList())
}
