package com.saaf.milkshop

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.owner.NativeCorrectionKind
import com.saaf.milkshop.application.owner.NativeCorrectionPrefill
import com.saaf.milkshop.application.owner.NativeOwnerRecordsContext
import com.saaf.milkshop.application.owner.NativeOwnerRecordsDetail
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.application.owner.NativeOwnerRecordsSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerFundsAccount
import com.saaf.milkshop.application.owner.NativeOwnerFundsContext
import com.saaf.milkshop.application.owner.NativeOwnerFundsLinkedReceipt
import com.saaf.milkshop.application.owner.NativeRecordsGroup
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeCorrectionEditorState
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.NativeOwnerFundsReceiptEditorState
import com.saaf.milkshop.nativeui.owner.OwnerRecordsScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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

    @Test
    fun correctionDiscardCanKeepOrConfirmNavigation() {
        val record = row(55L)
        var navigated = 0
        setScreen(NativeOwnerRecordsState.Ready(snapshot = snapshotWith(record),
            correction = NativeCorrectionEditorState(prefill = monetaryPrefill(record), draftTouched = true)),
            onNavigate = { navigated++ })
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionDiscardDialog).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionDiscardKeep).performClick()
        assertEquals(0, navigated)
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionDiscardConfirm).performClick()
        assertEquals(1, navigated)
    }

    @Test
    fun drawingCancellationUsesExplicitChoiceAndRequiredReason() {
        val record = row(88L).copy(eventTypeCode = "OWNER_DRAWING", taskLabel = "Owner drawing")
        val prefill = monetaryPrefill(record).copy(kind = NativeCorrectionKind.Drawing,
            drawingPaymentAccountName = "Cash", notes = "", ownerFunds = funds())
        var state by mutableStateOf<NativeOwnerRecordsState>(NativeOwnerRecordsState.Ready(
            snapshot = snapshotWith(record), correction = NativeCorrectionEditorState(prefill)))
        var saved: NativeCorrectionEditorState? = null
        setScreen(stateProvider = { state }, onUpdateCorrection = { transform ->
            val ready = state as NativeOwnerRecordsState.Ready
            state = ready.copy(correction = transform(ready.correction!!))
        }, onSaveCorrection = { saved = (state as NativeOwnerRecordsState.Ready).correction })
        composeRule.onNodeWithText("PKR 250 · Cash").assertExists()
        composeRule.onNodeWithText("Reason (required)").assertExists()
        composeRule.onNodeWithText("Cancel withdrawal").performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionMonetaryAmount).assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionNotes).performScrollTo().performTextReplacement("Withdrawal never happened")
        composeRule.onNodeWithText("Save cancellation").performScrollTo().performClick()
        assertTrue(saved!!.cancelDrawing)
        assertEquals("Withdrawal never happened", saved!!.notes)
    }

    @Test
    fun withdrawalDetailShowsLinkedReceiptsAndOwnerAction() {
        val record = row(88L).copy(eventTypeCode = "OWNER_DRAWING", taskLabel = "Owner drawing")
        val ownerFunds = funds().copy(linkedReceipts = listOf(
            NativeOwnerFundsLinkedReceipt(91L, "2026-09-21", 30.0, "Superseded"),
            NativeOwnerFundsLinkedReceipt(92L, "2026-09-21", 25.0, "Corrected")))
        var opened: NativeOwnerFundsContext? = null
        setScreen(NativeOwnerRecordsState.Ready(snapshot = snapshotWith(record),
            detail = detail(record).copy(ownerFunds = ownerFunds)),
            onOpenReceipt = { opened = it })
        composeRule.onNodeWithText("Money received against this withdrawal").assertExists()
        composeRule.onNodeWithTag(NativeUiTags.ownerFundsLinkedReceipt(91L)).assertExists()
        composeRule.onNodeWithTag(NativeUiTags.ownerFundsLinkedReceipt(92L)).assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsRecordReceipt).performScrollTo().performClick()
        assertEquals(88L, opened?.rootEventId)
    }

    @Test
    fun managerWithdrawalDetailHasNoReceiptAction() {
        val record = row(88L).copy(eventTypeCode = "OWNER_DRAWING", taskLabel = "Owner drawing")
        setScreen(NativeOwnerRecordsState.Ready(snapshot = snapshotWith(record),
            detail = detail(record).copy(ownerFunds = funds().copy(canRecordReceipt = false))))
        composeRule.onNodeWithText("Money received against this withdrawal").assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsRecordReceipt).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsDetailCorrect).assertDoesNotExist()
    }

    @Test
    fun receiptDraftUsesMaterialDateDialog() {
        var state by mutableStateOf<NativeOwnerRecordsState>(NativeOwnerRecordsState.Ready(
            snapshot = emptySnapshot(), receipt = NativeOwnerFundsReceiptEditorState(funds())))
        setScreen(stateProvider = { state }, onUpdateReceipt = { transform ->
            val ready = state as NativeOwnerRecordsState.Ready
            state = ready.copy(receipt = transform(ready.receipt!!))
        })
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptRoot).assertExists()
        saveQaScreenshot("receipt-light")
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptAmount).performTextReplacement("40")
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptDate).performScrollTo().performClick()
        composeRule.onNodeWithText("Use date").assertExists()
        composeRule.onNodeWithText("Use date").performClick()
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Use date").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptDate).assertExists()
    }

    @Test
    fun receiptSaveShowsPersistentConfirmationAndCannotRepeat() {
        var state by mutableStateOf<NativeOwnerRecordsState>(NativeOwnerRecordsState.Ready(
            snapshot = emptySnapshot(), receipt = NativeOwnerFundsReceiptEditorState(funds(), amount = "40")))
        var saves = 0
        setScreen(stateProvider = { state }, onSaveReceipt = {
            saves++
            val ready = state as NativeOwnerRecordsState.Ready
            state = ready.copy(receipt = ready.receipt!!.copy(savedEventId = 93L))
        })
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptSave).performScrollTo().performClick()
        assertEquals(1, saves)
        composeRule.onNodeWithText("Money received saved").assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsDone).assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptSave).assertDoesNotExist()
    }

    @Test
    fun receiptFailureKeepsDraftAndOffersCurrentFactsRefresh() {
        var refreshes = 0
        setScreen(NativeOwnerRecordsState.Ready(snapshot = emptySnapshot(),
            receipt = NativeOwnerFundsReceiptEditorState(funds(), amount = "275", note = "Actually returned",
                error = "Owner funds changed; review again", draftTouched = true)),
            onRefreshReceiptFacts = { refreshes++ })
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptError).performScrollTo().assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsRefreshFacts).performScrollTo().performClick()
        assertEquals(1, refreshes)
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptAmount).assertExists()
    }

    @Test
    fun ownerFundsCorrectionSavedStateRequiresDone() {
        val record = row(88L).copy(eventTypeCode = "OWNER_DRAWING", taskLabel = "Owner drawing")
        val prefill = monetaryPrefill(record).copy(kind = NativeCorrectionKind.Drawing,
            drawingPaymentAccountName = "Cash", notes = "", ownerFunds = funds())
        setScreen(NativeOwnerRecordsState.Ready(snapshot = snapshotWith(record),
            correction = NativeCorrectionEditorState(prefill, savedEventId = 94L)))
        composeRule.onNodeWithText("Saved as #94").performScrollTo().assertExists()
        composeRule.onNodeWithText("Done").performScrollTo().assertExists()
        composeRule.onNodeWithText("Save correction").assertDoesNotExist()
    }

    @Test
    fun correctionFailureKeepsDraftAndOffersCurrentFactsRefresh() {
        val record = row(88L).copy(eventTypeCode = "OWNER_DRAWING", taskLabel = "Owner drawing")
        val prefill = monetaryPrefill(record).copy(kind = NativeCorrectionKind.Drawing,
            drawingPaymentAccountName = "Cash", notes = "", ownerFunds = funds())
        var refreshes = 0
        setScreen(NativeOwnerRecordsState.Ready(snapshot = snapshotWith(record),
            correction = NativeCorrectionEditorState(prefill, monetaryAmount = "200", notes = "Wrong account",
                error = "Owner funds changed; review again", draftTouched = true)),
            onRefreshCorrectionFacts = { refreshes++ })
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsCorrectionRefreshFacts).performScrollTo().performClick()
        assertEquals(1, refreshes)
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionMonetaryAmount).assertExists()
    }

    @Test
    fun receiptFormControlsRemainReachableInDarkLargeText() {
        setScreen(NativeOwnerRecordsState.Ready(snapshot = emptySnapshot(),
            receipt = NativeOwnerFundsReceiptEditorState(funds())), darkTheme = true, fontScale = 1.5f)
        saveQaScreenshot("receipt-dark-large")
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptAmount).assertExists()
        composeRule.onNodeWithTag(NativeUiTags.ownerFundsAccount(2L)).performScrollTo().assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptDate).performScrollTo().assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerFundsReceiptSave).performScrollTo().assertExists()
    }

    @Test
    fun correctionFormControlsRemainReachableInDarkLargeText() {
        val record = row(88L).copy(eventTypeCode = "OWNER_DRAWING", taskLabel = "Owner drawing")
        val prefill = monetaryPrefill(record).copy(kind = NativeCorrectionKind.Drawing,
            drawingPaymentAccountName = "Cash", notes = "", ownerFunds = funds())
        setScreen(NativeOwnerRecordsState.Ready(snapshot = snapshotWith(record),
            correction = NativeCorrectionEditorState(prefill)), darkTheme = true, fontScale = 1.5f)
        saveQaScreenshot("correction-dark-large")
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionMonetaryAmount).performScrollTo().assertExists()
        composeRule.onNodeWithTag(NativeUiTags.ownerFundsAccount(2L)).performScrollTo().assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionNotes).performScrollTo().assertExists()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsCorrectionSave).performScrollTo().assertExists()
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
        onRefreshCorrectionFacts: () -> Unit = {},
        onOpenReceipt: (NativeOwnerFundsContext) -> Unit = {},
        onUpdateReceipt: ((NativeOwnerFundsReceiptEditorState) -> NativeOwnerFundsReceiptEditorState) -> Unit = {},
        onSaveReceipt: () -> Unit = {},
        onRefreshReceiptFacts: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
        darkTheme: Boolean = false,
        fontScale: Float? = null,
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
            onRefreshCorrectionFacts = onRefreshCorrectionFacts,
            onOpenReceipt = onOpenReceipt,
            onUpdateReceipt = onUpdateReceipt,
            onSaveReceipt = onSaveReceipt,
            onRefreshReceiptFacts = onRefreshReceiptFacts,
            onNavigate = onNavigate,
            darkTheme = darkTheme,
            fontScale = fontScale,
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
        onRefreshCorrectionFacts: () -> Unit = {},
        onOpenReceipt: (NativeOwnerFundsContext) -> Unit = {},
        onUpdateReceipt: ((NativeOwnerFundsReceiptEditorState) -> NativeOwnerFundsReceiptEditorState) -> Unit = {},
        onSaveReceipt: () -> Unit = {},
        onRefreshReceiptFacts: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
        darkTheme: Boolean = false,
        fontScale: Float? = null,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale ?: density.fontScale)) {
            SaafTheme(darkTheme = darkTheme) {
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
                    onRefreshCorrectionFacts = onRefreshCorrectionFacts,
                    onOpenReceipt = onOpenReceipt,
                    onUpdateReceipt = onUpdateReceipt,
                    onSaveReceipt = onSaveReceipt,
                    onRefreshReceiptFacts = onRefreshReceiptFacts,
                )
            }
            }
        }
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onRoot().fetchSemanticsNode() }.isSuccess
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

    private fun funds() = NativeOwnerFundsContext(
        rootEventId = 88L, rootDate = "2026-09-21", currentWithdrawal = 250.0,
        receivedTotal = 25.0, currentAccountId = 1L, currentAccountName = "Cash",
        accounts = listOf(NativeOwnerFundsAccount(1L, "Cash"), NativeOwnerFundsAccount(2L, "Bank")),
        linkedReceipts = emptyList(), canRecordReceipt = true, isReceipt = false)

    private fun saveQaScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "b3d-$name.png")
        output.outputStream().use { stream ->
            composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

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
