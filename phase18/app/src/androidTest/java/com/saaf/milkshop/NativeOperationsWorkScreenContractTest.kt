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
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeOperationsAttendanceStatus
import com.saaf.milkshop.application.operations.NativeOperationsWorkContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.NativeOperationsWorkState
import com.saaf.milkshop.nativeui.operations.OperationsSection
import com.saaf.milkshop.nativeui.operations.OperationsWorkScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOperationsWorkScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsPhysicalContextAndKeepsWorkNavSelected() {
        setScreen(NativeOperationsWorkState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkContext).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilk).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurt).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilage).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertIsSelected()
        composeRule.onNodeWithText("no money", substring = true).assertIsDisplayed()
    }

    @Test
    fun receiveMilkOpensConnectedEntry() {
        var opened = false
        setScreen(
            NativeOperationsWorkState.Ready(context = openContext()),
            onOpenReceiveMilk = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilk).performClick()
        assertTrue(opened)
    }

    @Test
    fun makeYogurtOpensConnectedEntry() {
        var opened = false
        setScreen(
            NativeOperationsWorkState.Ready(context = openContext()),
            onOpenMakeYogurt = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurt).performClick()
        assertTrue(opened)
    }

    @Test
    fun recordSpoilageOpensConnectedEntry() {
        var opened = false
        setScreen(
            NativeOperationsWorkState.Ready(context = openContext()),
            onOpenRecordSpoilage = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilage).performClick()
        assertTrue(opened)
    }

    @Test
    fun deliveriesNavOpensConnectedEntry() {
        var navigated: OperationsSection? = null
        setScreen(
            NativeOperationsWorkState.Ready(context = openContext()),
            onNavigate = { navigated = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).performClick()
        assertEquals(OperationsSection.Deliveries, navigated)
    }

    @Test
    fun attendanceNavOpensConnectedEntry() {
        var navigated: OperationsSection? = null
        setScreen(
            NativeOperationsWorkState.Ready(context = openContext()),
            onNavigate = { navigated = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).performClick()
        assertEquals(OperationsSection.Attendance, navigated)
    }

    @Test
    fun moreNavShowsExplicitPreviewNotice() {
        setScreen(
            NativeOperationsWorkState.Ready(context = openContext()),
            section = OperationsSection.More,
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsMorePreview).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Operations More is not connected in this native preview yet.",
        ).assertIsDisplayed()
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            NativeOperationsWorkState.LoadFailure("Work could not open."),
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkRetryLoad).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun closedBusinessDateShowsClosedBanner() {
        setScreen(
            NativeOperationsWorkState.Ready(
                context = openContext().copy(postingOpen = false),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkClosed).assertIsDisplayed()
    }

    private fun setScreen(
        state: NativeOperationsWorkState,
        section: OperationsSection = OperationsSection.Work,
        onRetry: () -> Unit = {},
        onOpenReceiveMilk: () -> Unit = {},
        onOpenMakeYogurt: () -> Unit = {},
        onOpenRecordSpoilage: () -> Unit = {},
        onNavigate: (OperationsSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                OperationsWorkScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity(),
                    state = state,
                    section = section,
                    onRetry = onRetry,
                    onOpenReceiveMilk = onOpenReceiveMilk,
                    onOpenMakeYogurt = onOpenMakeYogurt,
                    onOpenRecordSpoilage = onOpenRecordSpoilage,
                    onBackToHome = {},
                    onLeaveConfirmed = {},
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun openContext() = NativeOperationsWorkContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        milkOnHandLabel = "12 L milk",
        milkProductPresent = true,
        latestReceiptLabel = null,
        latestProductionLabel = null,
        activeDeliveryCount = 0,
        attendance = NativeOperationsAttendanceStatus.Unlinked,
    )

    private fun identity() = NativeSignedInIdentity(
        userId = 2L,
        loginName = "operator",
        displayName = "Ops Operator",
        role = AppRole.OPERATOR,
        effectivePermissions = RolePermissions.permissions(AppRole.OPERATOR),
    )
}
