package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.activity.NativeActivityContext
import com.saaf.milkshop.application.activity.NativeActivityDay
import com.saaf.milkshop.application.activity.NativeActivityRecordDetail
import com.saaf.milkshop.application.activity.NativeActivityRecordSummary
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.activity.CashierActivityScreen
import com.saaf.milkshop.nativeui.activity.NativeActivityState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeActivityScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyTodayExplainsAndKeepsActivityNavSelected() {
        setScreen(NativeActivityState.Ready(day = emptyDay()))
        composeRule.onNodeWithTag(NativeUiTags.ActivityEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No records yet today").assertIsDisplayed()
        composeRule.onNodeWithText("Sales, collections and expenses you record today will show up here.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).assertIsSelected()
    }

    @Test
    fun readyListOpensDetailAndBackClearsIt() {
        var state by mutableStateOf<NativeActivityState>(
            NativeActivityState.Ready(day = dayWith(summary(41L))),
        )
        var openedId: Long? = null
        setScreen(
            stateProvider = { state },
            onOpenRecord = { eventId ->
                openedId = eventId
                state = (state as NativeActivityState.Ready).copy(
                    detail = detail(summary(eventId)),
                )
            },
            onBackFromDetail = {
                state = (state as NativeActivityState.Ready).copy(detail = null)
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.ActivityList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.activityRecord(41L)).performClick()
        assertEquals(41L, openedId)
        composeRule.onNodeWithTag(NativeUiTags.ActivityDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ActivityDetailEventId).assertIsDisplayed()
    }

    @Test
    fun expenseRowOpensOwnDetailWithCategoryAndPaymentAccount() {
        val expense = summary(52L).copy(
            taskLabel = "Expense",
            amountPaisa = 12_550L,
            stateLabel = "Fuel · Paid from Cash",
        )
        var openedId: Long? = null
        var state by mutableStateOf<NativeActivityState>(NativeActivityState.Ready(day = dayWith(expense)))
        setScreen(stateProvider = { state }, onOpenRecord = { eventId ->
            openedId = eventId
            state = (state as NativeActivityState.Ready).copy(detail = NativeActivityRecordDetail(
                summary = expense,
                counterpartLabel = null,
                destinationLabel = null,
                notes = "Generator fuel",
                quantityLabel = null,
                expenseCategoryLabel = "Fuel",
                paymentAccountLabel = "Cash",
            ))
        })
        composeRule.onNodeWithTag(NativeUiTags.activityRecord(52L)).performClick()
        assertEquals(52L, openedId)
        composeRule.onNodeWithTag(NativeUiTags.ActivityDetail).assertIsDisplayed()
        composeRule.onNodeWithText("Category").assertIsDisplayed()
        composeRule.onNodeWithText("Fuel").assertIsDisplayed()
        composeRule.onNodeWithText("Paid from").assertIsDisplayed()
        composeRule.onNodeWithText("Cash").assertIsDisplayed()
        composeRule.onNodeWithText("PKR 125.50").assertIsDisplayed()
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            NativeActivityState.LoadFailure("Activity could not be loaded."),
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.ActivityRetryLoad).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        state: NativeActivityState,
        onRetry: () -> Unit = {},
        onOpenRecord: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
    ) {
        setScreen(
            stateProvider = { state },
            onRetry = onRetry,
            onOpenRecord = onOpenRecord,
            onBackFromDetail = onBackFromDetail,
        )
    }

    private fun setScreen(
        stateProvider: () -> NativeActivityState,
        onRetry: () -> Unit = {},
        onOpenRecord: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                CashierActivityScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = identity,
                    state = stateProvider(),
                    onRetry = onRetry,
                    onOpenRecord = onOpenRecord,
                    onBackFromDetail = onBackFromDetail,
                    onLeaveConfirmed = {},
                    onNavigate = {},
                )
            }
        }
    }

    private fun emptyDay() = NativeActivityDay(
        context = NativeActivityContext(businessDate = "2026-09-20", postingOpen = true),
        records = emptyList(),
    )

    private fun dayWith(vararg records: NativeActivityRecordSummary) = NativeActivityDay(
        context = NativeActivityContext(businessDate = "2026-09-20", postingOpen = true),
        records = records.toList(),
    )

    private fun summary(eventId: Long) = NativeActivityRecordSummary(
        eventId = eventId,
        occurredAt = "2026-09-20",
        createdAt = "2026-09-20T10:15:00Z",
        taskLabel = "Collection",
        amountPaisa = 25_000L,
        stateLabel = "Ahmed Store · Received in Cash",
        timeLabel = "10:15 AM",
    )

    private fun detail(summary: NativeActivityRecordSummary) = NativeActivityRecordDetail(
        summary = summary,
        counterpartLabel = "Ahmed Store",
        destinationLabel = "Cash",
        notes = "Counter",
        quantityLabel = null,
    )

    private companion object {
        val identity = NativeSignedInIdentity(
            userId = 2L,
            loginName = "activity.cashier",
            displayName = "Activity Cashier",
            role = AppRole.CASHIER,
            effectivePermissions = RolePermissions.permissions(AppRole.CASHIER),
        )
    }
}
