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
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.operations.NativeOperationsMoreContext
import com.saaf.milkshop.application.operations.NativeOperationsWorkRecord
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.NativeOperationsMoreDestination
import com.saaf.milkshop.nativeui.operations.NativeOperationsMoreState
import com.saaf.milkshop.nativeui.operations.OpsMoreScaffold
import com.saaf.milkshop.nativeui.operations.OperationsMoreScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOperationsMoreScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun menuShowsOperatorActionsWithoutCashCount() {
        setScreen(NativeOperationsMoreState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreTodayWork).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMorePersonalSecurity).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreSwitchAccount).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreSignOut).assertIsDisplayed()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertIsSelected()
    }

    @Test
    fun todayWorkShowsPhysicalRows() {
        setScreen(
            NativeOperationsMoreState.Ready(
                context = openContext(),
                destination = NativeOperationsMoreDestination.TodayWork,
                todayRecords = listOf(
                    NativeOperationsWorkRecord(
                        eventId = 44L,
                        taskLabel = "Receive milk",
                        detailLabel = "12 L received · Ops More Dairy",
                        timeLabel = "9:15 AM",
                    ),
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreTodayWorkScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.opsMoreWorkRecord(44L)).assertIsDisplayed()
        composeRule.onNodeWithText("Receive milk").assertIsDisplayed()
        composeRule.onNodeWithText("12 L received · Ops More Dairy").assertIsDisplayed()
    }

    @Test
    fun personalSecurityOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOperationsMoreState.Ready(context = openContext()),
            onOpenPersonalSecurity = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsMorePersonalSecurity).performClick()
        assertTrue(opened)
    }

    @Test
    fun personalSecurityShowsGroupedStatusAndPinEntry() {
        setScreen(NativeOperationsMoreState.Ready(
            context = openContext(),
            destination = NativeOperationsMoreDestination.PersonalSecurity,
            security = NativeMoreSecurityStatus(false, false, true, "Phone security"),
        ))
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreSecurityScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreSecurityStatus).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreSecurityPin).assertIsDisplayed()
    }

    private fun setScreen(
        state: NativeOperationsMoreState,
        onOpenTodayWork: () -> Unit = {},
        onOpenPersonalSecurity: () -> Unit = {},
        onSwitchAccount: () -> Unit = {},
        onSignOut: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                OperationsMoreScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = NativeSignedInIdentity(
                        userId = 2L,
                        loginName = "ops",
                        displayName = "Ops",
                        role = AppRole.OPERATOR,
                        effectivePermissions = RolePermissions.permissions(AppRole.OPERATOR),
                    ),
                    state = state,
                    onRetry = {},
                    onOpenTodayWork = onOpenTodayWork,
                    onOpenPersonalSecurity = onOpenPersonalSecurity,
                    onBackToMenu = {},
                    onRequestRemoveQuickUnlock = {},
                    onCancelRemoveQuickUnlock = {},
                    onRemoveQuickUnlock = {},
                    onEnrollQuickPin = { _, _ -> },
                    onEnrollPhoneSecurity = { _, _ -> },
                    onSwitchAccount = onSwitchAccount,
                    onSignOut = onSignOut,
                    moreScaffold = { s, m, content ->
                        OpsMoreScaffold(
                            snackbarHostState = s,
                            onNavigate = {},
                            modifier = m,
                            rootTag = NativeUiTags.OpsMoreRoot,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun openContext() = NativeOperationsMoreContext(
        businessDate = "2026-09-20",
        postingOpen = true,
    )
}
