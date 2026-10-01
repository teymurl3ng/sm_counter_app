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
import com.saaf.milkshop.application.operations.NativeAttendanceContext
import com.saaf.milkshop.application.operations.NativeAttendanceLink
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.AttendanceScreen
import com.saaf.milkshop.nativeui.operations.NativeAttendanceState
import com.saaf.milkshop.nativeui.operations.OperationsSection
import com.saaf.milkshop.nativeui.operations.WorkScaffold
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeAttendanceScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unlinkedKeepsAttendanceNavSelected() {
        setScreen(
            NativeAttendanceState.Ready(
                context = NativeAttendanceContext(
                    businessDate = "2026-09-20",
                    postingOpen = true,
                    link = NativeAttendanceLink.Unlinked,
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceUnlinked).assertIsDisplayed()
        composeRule.onNodeWithText("Account not linked").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).assertIsSelected()
    }

    @Test
    fun linkedNotMarkedOffersMarkPresent() {
        var marked = false
        setScreen(
            NativeAttendanceState.Ready(context = linkedContext(todayStatus = null)),
            onMarkPresent = { marked = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceNotMarked).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceEmployee).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarkPresent).performClick()
        assertTrue(marked)
    }

    @Test
    fun markedTodayShowsStatusWithoutPrimaryWrite() {
        setScreen(
            NativeAttendanceState.Ready(context = linkedContext(todayStatus = "Present")),
        )
        composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarked).assertIsDisplayed()
        composeRule.onNodeWithText("Today: Present").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarkPresent).assertDoesNotExist()
    }

    private fun setScreen(
        state: NativeAttendanceState,
        onMarkPresent: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme {
                AttendanceScreen(
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
                    onMarkPresent = onMarkPresent,
                    attendanceScaffold = { s, m, content ->
                        WorkScaffold(
                            snackbarHostState = s,
                            section = OperationsSection.Attendance,
                            onNavigate = {},
                            modifier = m,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun linkedContext(todayStatus: String?) = NativeAttendanceContext(
        businessDate = "2026-09-20",
        postingOpen = true,
        link = NativeAttendanceLink.Linked(
            employeeAccountId = 9L,
            employeeName = "Preview Operator Staff",
            todayStatus = todayStatus,
            attendanceId = if (todayStatus != null) 3L else null,
        ),
    )
}
