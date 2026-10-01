package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.NativeCorrectionKind
import com.saaf.milkshop.application.owner.NativeCorrectionPrefill
import com.saaf.milkshop.application.owner.NativeOwnerRecordsContext
import com.saaf.milkshop.application.owner.NativeOwnerRecordsDetail
import com.saaf.milkshop.application.owner.NativeOwnerAccountLedgerDetail
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.application.owner.NativeRecordsGroup
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.formatRecordTimestamp
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import java.util.Locale

private val SALE_CHANNELS = listOf("retail", "wholesale", "credit-retail", "credit-wholesale")

/** Which pane the Ready records screen is showing; drives the section transition. */
private enum class RecordsPane { List, Detail, Ledger, Correction }

@Composable
fun OwnerRecordsScreen(
    snackbarHostState: SnackbarHostState,
    state: NativeOwnerRecordsState,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenLedger: (Long) -> Unit = {},
    onBackFromLedger: () -> Unit = {},
    onLeaveConfirmed: () -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String = "Owner",
    rootTag: String = NativeUiTags.OwnerRecordsRoot,
    loadingTag: String = NativeUiTags.OwnerRecordsLoading,
    onDeferredGroup: (String) -> Unit = {},
    onSelectGroup: (NativeRecordsGroup) -> Unit = {},
    onOpenCorrection: (NativeCorrectionPrefill) -> Unit = {},
    onBackFromCorrection: () -> Unit = {},
    onUpdateCorrection: ((NativeCorrectionEditorState) -> NativeCorrectionEditorState) -> Unit = {},
    onSaveCorrection: () -> Unit = {},
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeOwnerRecordsState.Idle || it is NativeOwnerRecordsState.Loading) NativeOwnerRecordsState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeOwnerRecordsState.Idle,
        NativeOwnerRecordsState.Loading,
        -> hostedScaffold(snackbarHostState, modifier, { it() }) {
            NativeColdLoadingPane(
                label = "Loading records…",
                modifier = Modifier
                    .padding(it)
                    .testTag(loadingTag),
            )
        }

        is NativeOwnerRecordsState.LoadFailure -> hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .testTag(rootTag),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag(NativeUiTags.OwnerRecordsRetryLoad),
                ) { Text("Try again") }
            }
        }

        is NativeOwnerRecordsState.Ready -> {
            val recordsPane = when {
                state.correction != null -> RecordsPane.Correction
                state.detail != null -> RecordsPane.Detail
                state.ledger != null -> RecordsPane.Ledger
                else -> RecordsPane.List
            }
            NativeSectionTransition(
                targetState = recordsPane,
                contentKey = { it },
            ) { pane -> when (pane) {
                RecordsPane.Correction -> {
                    val editor = state.correction ?: return@NativeSectionTransition
                    RecordsCorrectionBody(
                        snackbarHostState = snackbarHostState,
                        context = state.snapshot.context,
                        editor = editor,
                        onBack = onBackFromCorrection,
                        onUpdate = onUpdateCorrection,
                        onSave = onSaveCorrection,
                        hostedScaffold = hostedScaffold,
                        roleLabel = roleLabel,
                        showHeader = showHeader,
                        modifier = modifier,
                    )
                }
                RecordsPane.Detail -> {
                    val detail = state.detail ?: return@NativeSectionTransition
                    RecordsDetailBody(
                        snackbarHostState = snackbarHostState,
                        context = state.snapshot.context,
                        detail = detail,
                        onBack = onBackFromDetail,
                        onOpenCorrection = onOpenCorrection,
                        hostedScaffold = hostedScaffold,
                        roleLabel = roleLabel,
                        showHeader = showHeader,
                        modifier = modifier,
                    )
                }
                RecordsPane.Ledger -> {
                    val ledger = state.ledger ?: return@NativeSectionTransition
                    RecordsLedgerBody(
                        snackbarHostState = snackbarHostState,
                        context = state.snapshot.context,
                        ledger = ledger,
                        onBack = onBackFromLedger,
                        hostedScaffold = hostedScaffold,
                        roleLabel = roleLabel,
                        showHeader = showHeader,
                        modifier = modifier,
                    )
                }
                RecordsPane.List -> RecordsListBody(
                    snackbarHostState = snackbarHostState,
                    state = state,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenRecord = onOpenRecord,
                    onOpenLedger = onOpenLedger,
                    onLeaveConfirmed = onLeaveConfirmed,
                    hostedScaffold = hostedScaffold,
                    roleLabel = roleLabel,
                    showHeader = showHeader,
                    rootTag = rootTag,
                    onDeferredGroup = onDeferredGroup,
                    onSelectGroup = onSelectGroup,
                    modifier = modifier,
                )
            }
            }
        }
    }
    }
}

@Composable
private fun RecordsListBody(
    snackbarHostState: SnackbarHostState,
    state: NativeOwnerRecordsState.Ready,
    onSearchQueryChange: (String) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onOpenLedger: (Long) -> Unit,
    onLeaveConfirmed: () -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String,
    showHeader: Boolean,
    rootTag: String,
    onDeferredGroup: (String) -> Unit,
    onSelectGroup: (NativeRecordsGroup) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onLeaveConfirmed)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .nativeRefreshOverlay(state.refreshing)
                .padding(padding)
                .testTag(rootTag),
        ) {
            if (showHeader) {
                CashierScreenHeader(
                    title = "Records",
                    displayName = state.snapshot.context.shopName,
                    roleLabel = roleLabel,
                    businessDate = state.snapshot.context.businessDate,
                    postingOpen = state.snapshot.context.postingOpen,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerRecordsSearch),
                    singleLine = true,
                    label = {
                        Text(if (state.activeGroup == NativeRecordsGroup.Balances) "Search accounts" else "Search records")
                    },
                    placeholder = {
                        Text(if (state.activeGroup == NativeRecordsGroup.Balances) "Customer or supplier" else "Id, type, actor, notes…")
                    },
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag(NativeUiTags.OwnerRecordsGroups),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.activeGroup == NativeRecordsGroup.Recent,
                        onClick = { onSelectGroup(NativeRecordsGroup.Recent) },
                        label = { Text("Recent") },
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsGroupRecent),
                    )
                    FilterChip(
                        selected = state.activeGroup == NativeRecordsGroup.Balances,
                        onClick = { onSelectGroup(NativeRecordsGroup.Balances) },
                        label = { Text("Balances") },
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsGroupBalances),
                    )
                    FilterChip(
                        selected = state.activeGroup == NativeRecordsGroup.Closing,
                        onClick = { onSelectGroup(NativeRecordsGroup.Closing) },
                        label = { Text("Closing") },
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsGroupClosing),
                    )
                    FilterChip(
                        selected = state.activeGroup == NativeRecordsGroup.Corrections,
                        onClick = { onSelectGroup(NativeRecordsGroup.Corrections) },
                        label = { Text("Corrections") },
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsGroupCorrections),
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loadingDetail -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.activeGroup == NativeRecordsGroup.Balances -> {
                        RecordsBalancesBody(state = state, onOpenLedger = onOpenLedger)
                    }
                    state.activeGroup == NativeRecordsGroup.Closing -> {
                        RecordsClosingBody(state = state)
                    }
                    state.snapshot.records.isEmpty() && state.activeGroup == NativeRecordsGroup.Recent -> {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .testTag(NativeUiTags.OwnerRecordsEmpty),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "No records yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Business-wide activity will show up here as work is recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    state.visibleRecords.isEmpty() -> {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .testTag(NativeUiTags.OwnerRecordsFilterEmpty),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (state.activeGroup == NativeRecordsGroup.Corrections &&
                                    state.searchQuery.isBlank()
                                ) {
                                    "No corrections yet"
                                } else {
                                    "No matches"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (state.activeGroup == NativeRecordsGroup.Corrections &&
                                    state.searchQuery.isBlank()
                                ) {
                                    "Corrected and superseded records will appear here."
                                } else {
                                    "Nothing matches this search. Clear the query to see recent activity."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(NativeUiTags.OwnerRecordsList),
                        ) {
                            items(state.visibleRecords, key = { it.eventId }) { record ->
                                RecordsRow(record = record, onClick = { onOpenRecord(record.eventId) })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
                state.error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .testTag(NativeUiTags.OwnerRecordsError),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordsBalancesBody(
    state: NativeOwnerRecordsState.Ready,
    onOpenLedger: (Long) -> Unit,
) {
    val needle = state.searchQuery.trim().lowercase(Locale.US)
    val rows = remember(state.snapshot.balances, needle) {
        state.snapshot.balances.filter { row ->
            needle.isEmpty() || row.displayName.lowercase(Locale.US).contains(needle) ||
                row.categoryLabel.lowercase(Locale.US).contains(needle)
        }
    }
    if (rows.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (needle.isEmpty()) "No customer or supplier balances" else "No matching accounts")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(rows, key = { it.accountId }) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable(onClick = { onOpenLedger(row.accountId) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(row.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(row.categoryLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(row.balanceLabel, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RecordsClosingBody(state: NativeOwnerRecordsState.Ready) {
    val rows = state.snapshot.closeActions
    if (rows.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No day-closing history yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Close and reopen actions will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(rows, key = { it.actionId }) { row ->
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${row.actionLabel} · ${row.businessDate}", fontWeight = FontWeight.SemiBold)
                    Text("#${row.actionId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(row.actorLabel, style = MaterialTheme.typography.bodyMedium)
                Text(row.createdAtLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                row.reason?.let { Text("Reason: $it", style = MaterialTheme.typography.bodyMedium) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RecordsLedgerBody(
    snackbarHostState: SnackbarHostState,
    context: NativeOwnerRecordsContext,
    ledger: NativeOwnerAccountLedgerDetail,
    onBack: () -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String,
    showHeader: Boolean,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showHeader) {
                CashierScreenHeader(
                    title = ledger.displayName,
                    displayName = context.shopName,
                    roleLabel = roleLabel,
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ledger.currentBalanceLabel, fontWeight = FontWeight.Bold)
            }
            Text(
                "${ledger.categoryLabel} ledger",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ledger.entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ledger movements yet")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(ledger.entries, key = { it.logId }) { entry ->
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.taskLabel, fontWeight = FontWeight.SemiBold)
                                Text(entry.movementLabel, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Balance after: ${entry.balanceAfterLabel}")
                            Text(
                                "${formatRecordTimestamp(entry.occurredAt)} · Event #${entry.eventId} · ${entry.stateLabel}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            entry.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsDetailBody(
    snackbarHostState: SnackbarHostState,
    context: NativeOwnerRecordsContext,
    detail: NativeOwnerRecordsDetail,
    onBack: () -> Unit,
    onOpenCorrection: (NativeCorrectionPrefill) -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String,
    showHeader: Boolean,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerRecordsDetail),
        ) {
            if (showHeader) {
                CashierScreenHeader(
                    title = "Records",
                    displayName = context.shopName,
                    roleLabel = roleLabel,
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    detail.row.taskLabel,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    detail.row.identityLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(NativeUiTags.OwnerRecordsDetailEventId),
                )
                detail.row.trailingLabel?.let {
                    Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text(detail.row.timeLabel, style = MaterialTheme.typography.bodyLarge)
                detail.row.actorLabel?.let { LabeledValue("Actor", it) }
                Text(detail.row.stateLabel, style = MaterialTheme.typography.bodyLarge)
                detail.row.correctsEventId?.let {
                    LabeledValue("Corrects", "#$it")
                }
                detail.notes?.let { LabeledValue("Note", it) }
                if (detail.inventoryMovements.isNotEmpty()) {
                    Text("Inventory", style = MaterialTheme.typography.titleMedium)
                    detail.inventoryMovements.forEach { line ->
                        LabeledValue(line.productName, line.quantityLabel)
                    }
                }
                detail.financialMovements?.takeIf { it.isNotEmpty() }?.let { lines ->
                    Text("Money", style = MaterialTheme.typography.titleMedium)
                    lines.forEach { line ->
                        LabeledValue(line.accountName, line.amountLabel)
                    }
                }
                if (detail.originalValueLines.isNotEmpty()) {
                    Text("Original values", style = MaterialTheme.typography.titleMedium)
                    detail.originalValueLines.forEach { (label, value) ->
                        LabeledValue(label, value)
                    }
                }
                val correctionPrefill = detail.correctionPrefill
                if (detail.canCorrect && correctionPrefill != null) {
                    Button(
                        onClick = { onOpenCorrection(correctionPrefill) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerRecordsDetailCorrect),
                    ) { Text("Correct this record") }
                } else if (detail.row.isSuperseded) {
                    Text(
                        "This record was already replaced. Open its latest correction to correct further.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsDetailReadOnly),
                    )
                } else {
                    Text(
                        "This record type cannot be corrected here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsDetailReadOnly),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordsCorrectionBody(
    snackbarHostState: SnackbarHostState,
    context: NativeOwnerRecordsContext,
    editor: NativeCorrectionEditorState,
    onBack: () -> Unit,
    onUpdate: ((NativeCorrectionEditorState) -> NativeCorrectionEditorState) -> Unit,
    onSave: () -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String,
    showHeader: Boolean,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = !editor.saving

    fun requestLeave(action: () -> Unit) {
        if (!canLeave) return
        // Read the editor's current state at the moment the user leaves. Keeping
        // this synchronous prevents a quick edit-then-back gesture from racing a
        // composition effect and bypassing the unsaved-changes confirmation.
        if (editor.dirty) leaveGuard.markDirty() else leaveGuard.clear()
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        hostedScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
            { action -> requestLeave(action) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerRecordsCorrectionRoot),
            ) {
                if (showHeader) {
                    CashierScreenHeader(
                        title = "Records",
                        displayName = context.shopName,
                        roleLabel = roleLabel,
                        businessDate = context.businessDate,
                        postingOpen = context.postingOpen,
                    )
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Correct ${editor.prefill.taskLabel}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        editor.prefill.identityLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Business day ${editor.prefill.occurredAt}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when (editor.prefill.kind) {
                        NativeCorrectionKind.Purchase -> {
                            CorrectionField(
                                value = editor.purchaseRawLiters,
                                label = "Raw liters",
                                tag = NativeUiTags.OwnerRecordsCorrectionPurchaseRaw,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(purchaseRawLiters = value) }
                                },
                            )
                            CorrectionField(
                                value = editor.purchaseFat,
                                label = "Fat %",
                                tag = NativeUiTags.OwnerRecordsCorrectionPurchaseFat,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(purchaseFat = value) }
                                },
                            )
                            CorrectionField(
                                value = editor.purchaseLr,
                                label = "LR reading",
                                tag = NativeUiTags.OwnerRecordsCorrectionPurchaseLr,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(purchaseLr = value) }
                                },
                            )
                        }
                        NativeCorrectionKind.Production -> {
                            CorrectionField(
                                value = editor.productionMilk,
                                label = "Milk liters used",
                                tag = NativeUiTags.OwnerRecordsCorrectionProductionMilk,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(productionMilk = value) }
                                },
                            )
                            CorrectionField(
                                value = editor.productionYogurt,
                                label = "Yogurt kg produced",
                                tag = NativeUiTags.OwnerRecordsCorrectionProductionYogurt,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(productionYogurt = value) }
                                },
                            )
                        }
                        NativeCorrectionKind.Sale -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = editor.saleQuantityMode,
                                    onClick = {
                                        if (canLeave) {
                                            onUpdate { it.copy(saleQuantityMode = true) }
                                        }
                                    },
                                    label = { Text("By quantity") },
                                    modifier = Modifier.testTag(
                                        NativeUiTags.OwnerRecordsCorrectionSaleQuantityMode,
                                    ),
                                )
                                FilterChip(
                                    selected = !editor.saleQuantityMode,
                                    onClick = {
                                        if (canLeave) {
                                            onUpdate { it.copy(saleQuantityMode = false) }
                                        }
                                    },
                                    label = { Text("By value") },
                                    modifier = Modifier.testTag(
                                        NativeUiTags.OwnerRecordsCorrectionSaleValueMode,
                                    ),
                                )
                            }
                            CorrectionField(
                                value = editor.saleRate,
                                label = "Unit rate (PKR)",
                                tag = NativeUiTags.OwnerRecordsCorrectionSaleRate,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(saleRate = value) }
                                },
                            )
                            if (editor.saleQuantityMode) {
                                CorrectionField(
                                    value = editor.saleQuantity,
                                    label = "Quantity",
                                    tag = NativeUiTags.OwnerRecordsCorrectionSaleQuantity,
                                    enabled = canLeave,
                                    onChange = { value ->
                                        onUpdate { it.copy(saleQuantity = value) }
                                    },
                                )
                            } else {
                                CorrectionField(
                                    value = editor.saleValue,
                                    label = "Sale value (PKR)",
                                    tag = NativeUiTags.OwnerRecordsCorrectionSaleValue,
                                    enabled = canLeave,
                                    onChange = { value ->
                                        onUpdate { it.copy(saleValue = value) }
                                    },
                                )
                            }
                            CorrectionField(
                                value = editor.saleSurcharge,
                                label = "Surcharge (PKR)",
                                tag = NativeUiTags.OwnerRecordsCorrectionSaleSurcharge,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(saleSurcharge = value) }
                                },
                            )
                            Text(
                                "Channel",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag(NativeUiTags.OwnerRecordsCorrectionSaleChannel),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SALE_CHANNELS.forEach { channel ->
                                    FilterChip(
                                        selected = editor.saleChannel == channel,
                                        onClick = {
                                            if (canLeave) {
                                                onUpdate { it.copy(saleChannel = channel) }
                                            }
                                        },
                                        label = { Text(channel) },
                                    )
                                }
                            }
                        }
                        NativeCorrectionKind.Monetary -> {
                            CorrectionField(
                                value = editor.monetaryAmount,
                                label = "Amount (PKR)",
                                tag = NativeUiTags.OwnerRecordsCorrectionMonetaryAmount,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(monetaryAmount = value) }
                                },
                            )
                        }
                        NativeCorrectionKind.Spoilage -> {
                            CorrectionField(
                                value = editor.spoilageQuantity,
                                label = "Quantity",
                                tag = NativeUiTags.OwnerRecordsCorrectionSpoilageQuantity,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(spoilageQuantity = value) }
                                },
                            )
                            CorrectionField(
                                value = editor.spoilageReason,
                                label = "Reason",
                                tag = NativeUiTags.OwnerRecordsCorrectionSpoilageReason,
                                enabled = canLeave,
                                onChange = { value ->
                                    onUpdate { it.copy(spoilageReason = value) }
                                },
                            )
                        }
                    }
                    CorrectionField(
                        value = editor.notes,
                        label = "Notes (optional)",
                        tag = NativeUiTags.OwnerRecordsCorrectionNotes,
                        enabled = canLeave,
                        singleLine = false,
                        onChange = { value ->
                            onUpdate { it.copy(notes = value) }
                        },
                    )
                    editor.error?.let { message ->
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerRecordsCorrectionError),
                        )
                    }
                    Button(
                        onClick = onSave,
                        enabled = canLeave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerRecordsCorrectionSave),
                    ) {
                        Text(if (editor.saving) "Saving…" else "Save correction")
                    }
                }
            }
        }
        RecordsCorrectionDiscardDialog(leaveGuard)
    }
}

@Composable
private fun CorrectionField(
    value: String,
    label: String,
    tag: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label) },
    )
}

@Composable
private fun RecordsCorrectionDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsCorrectionDiscardDialog),
        title = { Text("Discard correction?") },
        text = { Text("Your unsaved correction draft will be lost.") },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(NativeUiTags.OwnerRecordsCorrectionDiscardConfirm),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(NativeUiTags.OwnerRecordsCorrectionDiscardKeep),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
private fun RecordsRow(
    record: NativeOwnerRecordsRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(NativeUiTags.ownerRecordsRecord(record.eventId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    record.taskLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    record.timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                listOfNotNull(record.identityLabel, record.actorLabel, record.stateLabel)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            record.trailingLabel?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(Locale.US),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
