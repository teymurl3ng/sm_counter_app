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
import com.saaf.milkshop.application.owner.NativeOwnerRecordsContext
import com.saaf.milkshop.application.owner.NativeOwnerRecordsDetail
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.application.owner.NativeOwnerRecordsSnapshot
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.OwnerRecordsScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerRecordsScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyHistoryExplainsAndKeepsRecordsNavSelected() {
        setScreen(NativeOwnerRecordsState.Ready(snapshot = emptySnapshot()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No records yet").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavRecords).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).assertDoesNotExist()
    }

    @Test
    fun readyListOpensDetailAndBackClearsIt() {
        var state by mutableStateOf<NativeOwnerRecordsState>(
            NativeOwnerRecordsState.Ready(snapshot = snapshotWith(row(41L))),
        )
        var openedId: Long? = null
        setScreen(
            stateProvider = { state },
            onOpenRecord = { eventId ->
                openedId = eventId
                state = (state as NativeOwnerRecordsState.Ready).copy(
                    detail = detail(row(eventId)),
                )
            },
            onBackFromDetail = {
                state = (state as NativeOwnerRecordsState.Ready).copy(detail = null)
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerRecordsRecord(41L)).performClick()
        assertEquals(41L, openedId)
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsDetailEventId).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsDetailReadOnly).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsDetailBack).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsList).assertIsDisplayed()
    }

    @Test
    fun searchFilterShowsHonestEmptyWhenNoMatches() {
        setScreen(
            NativeOwnerRecordsState.Ready(
                snapshot = snapshotWith(row(7L)),
                searchQuery = "zzz",
                visibleRecords = emptyList(),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsFilterEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No matches").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsSearch).assertIsDisplayed()
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            NativeOwnerRecordsState.LoadFailure("Records could not be loaded."),
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsRetryLoad).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun deferredGroupChipSurfacesNotice() {
        var notice: String? = null
        setScreen(
            NativeOwnerRecordsState.Ready(snapshot = emptySnapshot()),
            onDeferredGroup = { notice = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsGroupBalances).performClick()
        assertEquals("Balances & ledgers", notice)
    }

    @Test
    fun overviewNavFromRecordsRequestsNavigation() {
        var navigated: OwnerSection? = null
        setScreen(
            NativeOwnerRecordsState.Ready(snapshot = emptySnapshot()),
            onNavigate = { navigated = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()
        assertEquals(OwnerSection.Overview, navigated)
    }

    private fun setScreen(
        state: NativeOwnerRecordsState,
        onRetry: () -> Unit = {},
        onSearchQueryChange: (String) -> Unit = {},
        onOpenRecord: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onDeferredGroup: (String) -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        setScreen(
            stateProvider = { state },
            onRetry = onRetry,
            onSearchQueryChange = onSearchQueryChange,
            onOpenRecord = onOpenRecord,
            onBackFromDetail = onBackFromDetail,
            onDeferredGroup = onDeferredGroup,
            onNavigate = onNavigate,
        )
    }

    private fun setScreen(
        stateProvider: () -> NativeOwnerRecordsState,
        onRetry: () -> Unit = {},
        onSearchQueryChange: (String) -> Unit = {},
        onOpenRecord: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onDeferredGroup: (String) -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerRecordsScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    state = stateProvider(),
                    onRetry = onRetry,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenRecord = onOpenRecord,
                    onBackFromDetail = onBackFromDetail,
                    onLeaveConfirmed = {},
                    onNavigate = onNavigate,
                    onDeferredGroup = onDeferredGroup,
                )
            }
        }
    }

    private fun emptySnapshot() = NativeOwnerRecordsSnapshot(
        context = NativeOwnerRecordsContext(
            businessDate = "2026-09-21",
            postingOpen = true,
            shopName = "Saaf Milk",
        ),
        records = emptyList(),
    )

    private fun snapshotWith(vararg rows: NativeOwnerRecordsRow) = NativeOwnerRecordsSnapshot(
        context = NativeOwnerRecordsContext(
            businessDate = "2026-09-21",
            postingOpen = true,
            shopName = "Saaf Milk",
        ),
        records = rows.toList(),
    )

    private fun row(eventId: Long) = NativeOwnerRecordsRow(
        eventId = eventId,
        occurredAt = "2026-09-21",
        createdAt = "2026-09-21T10:15:00Z",
        eventTypeCode = "CREDIT_COLLECTION",
        taskLabel = "Collection",
        identityLabel = "#C-$eventId",
        actorLabel = "Records Cashier",
        timeLabel = "2026-09-21 · 10:15 AM",
        trailingLabel = "PKR 250.00",
        stateLabel = "Recorded",
        notes = "Counter",
        isCorrection = false,
        isSuperseded = false,
        correctsEventId = null,
    )

    private fun detail(row: NativeOwnerRecordsRow) = NativeOwnerRecordsDetail(
        row = row,
        inventoryMovements = emptyList(),
        financialMovements = emptyList(),
        originalValueLines = emptyList(),
        notes = row.notes,
    )
}
