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
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeCorrectionKind
import com.saaf.milkshop.application.owner.NativeCorrectionPrefill
import com.saaf.milkshop.application.owner.NativeOwnerRecordsContext
import com.saaf.milkshop.application.owner.NativeOwnerRecordsDetail
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.application.owner.NativeOwnerRecordsSnapshot
import com.saaf.milkshop.application.owner.NativeRecordsGroup
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeCorrectionEditorState
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.OwnerRecordsScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun readyListOpensDetail() {
        var state by mutableStateOf<NativeOwnerRecordsState>(
            NativeOwnerRecordsState.Ready(snapshot = snapshotWith(row(41L))),
        )
        var openedId: Long? = null
        setScreen(
            stateProvider = { state },
            onOpenRecord = { eventId ->
                openedId = eventId
                val selected = row(eventId)
                state = (state as NativeOwnerRecordsState.Ready).copy(
                    detail = detail(
                        selected,
                        canCorrect = true,
                        prefill = monetaryPrefill(selected),
                    ),
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
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsDetailCorrect).assertIsDisplayed()
    }

    @Test
    fun correctableDetailOpensEditor() {
        val record = row(55L)
        val prefill = monetaryPrefill(record)
        var state by mutableStateOf<NativeOwnerRecordsState>(
            NativeOwnerRecordsState.Ready(
                snapshot = snapshotWith(record),
                detail = detail(record, canCorrect = true, prefill = prefill),
            ),
        )
        setScreen(
            stateProvider = { state },
            onOpenCorrection = { prefill ->
                state = (state as NativeOwnerRecordsState.Ready).copy(
                    correction = NativeCorrectionEditorState(prefill = prefill),
                )
            },
            onBackFromCorrection = {
                state = (state as NativeOwnerRecordsState.Ready).copy(correction = null)
            },
        )

        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsDetailCorrect).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionMonetaryAmount).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionSave).assertIsDisplayed()
    }

    @Test
    fun correctionsGroupSelectsWithoutDeferredNotice() {
        var selected: NativeRecordsGroup? = null
        var deferred: String? = null
        setScreen(
            NativeOwnerRecordsState.Ready(snapshot = emptySnapshot()),
            onSelectGroup = { selected = it },
            onDeferredGroup = { deferred = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsGroupCorrections).performClick()
        assertEquals(NativeRecordsGroup.Corrections, selected)
        assertNull(deferred)
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
    fun balancesAndClosingGroupsSelectRealViews() {
        val selected = mutableListOf<NativeRecordsGroup>()
        setScreen(
            NativeOwnerRecordsState.Ready(snapshot = emptySnapshot()),
            onSelectGroup = { selected += it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsGroupBalances).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsGroupClosing).performClick()
        assertEquals(listOf(NativeRecordsGroup.Balances, NativeRecordsGroup.Closing), selected)
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
        onSelectGroup: (NativeRecordsGroup) -> Unit = {},
        onOpenCorrection: (NativeCorrectionPrefill) -> Unit = {},
        onBackFromCorrection: () -> Unit = {},
        onUpdateCorrection: ((NativeCorrectionEditorState) -> NativeCorrectionEditorState) -> Unit = {},
        onSaveCorrection: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        setScreen(
            stateProvider = { state },
            onRetry = onRetry,
            onSearchQueryChange = onSearchQueryChange,
            onOpenRecord = onOpenRecord,
            onBackFromDetail = onBackFromDetail,
            onDeferredGroup = onDeferredGroup,
            onSelectGroup = onSelectGroup,
            onOpenCorrection = onOpenCorrection,
            onBackFromCorrection = onBackFromCorrection,
            onUpdateCorrection = onUpdateCorrection,
            onSaveCorrection = onSaveCorrection,
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
        onSelectGroup: (NativeRecordsGroup) -> Unit = {},
        onOpenCorrection: (NativeCorrectionPrefill) -> Unit = {},
        onBackFromCorrection: () -> Unit = {},
        onUpdateCorrection: ((NativeCorrectionEditorState) -> NativeCorrectionEditorState) -> Unit = {},
        onSaveCorrection: () -> Unit = {},
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
                    hostedScaffold = { snackbar, mod, wrapNav, content ->
                        com.saaf.milkshop.nativeui.owner.OwnerScaffold(
                            snackbarHostState = snackbar,
                            section = OwnerSection.Records,
                            onNavigate = { section -> wrapNav { onNavigate(section) } },
                            modifier = mod,
                            content = content,
                        )
                    },
                    onDeferredGroup = onDeferredGroup,
                    onSelectGroup = onSelectGroup,
                    onOpenCorrection = onOpenCorrection,
                    onBackFromCorrection = onBackFromCorrection,
                    onUpdateCorrection = onUpdateCorrection,
                    onSaveCorrection = onSaveCorrection,
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

    private fun monetaryPrefill(row: NativeOwnerRecordsRow) = NativeCorrectionPrefill(
        kind = NativeCorrectionKind.Monetary,
        originalEventId = row.eventId,
        occurredAt = row.occurredAt,
        taskLabel = row.taskLabel,
        identityLabel = row.identityLabel,
        notes = row.notes.orEmpty(),
        monetaryAmount = "250",
    )

    private fun detail(
        row: NativeOwnerRecordsRow,
        canCorrect: Boolean = false,
        prefill: NativeCorrectionPrefill? = null,
    ) = NativeOwnerRecordsDetail(
        row = row,
        inventoryMovements = emptyList(),
        financialMovements = emptyList(),
        originalValueLines = emptyList(),
        notes = row.notes,
        canCorrect = canCorrect,
        correctionPrefill = prefill,
    )
}
