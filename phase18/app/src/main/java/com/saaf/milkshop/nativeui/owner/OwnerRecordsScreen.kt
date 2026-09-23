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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.application.owner.NativeRecordsGroup
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import java.util.Locale

private val SALE_CHANNELS = listOf("retail", "wholesale", "credit-retail", "credit-wholesale")

@Composable
fun OwnerRecordsScreen(
    snackbarHostState: SnackbarHostState,
    state: NativeOwnerRecordsState,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeOwnerRecordsState.Idle,
        NativeOwnerRecordsState.Loading,
        -> hostedScaffold(snackbarHostState, modifier, { it() }) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(24.dp)
                    .testTag(loadingTag),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading records…")
            }
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
            val correction = state.correction
            when {
                correction != null -> RecordsCorrectionBody(
                    snackbarHostState = snackbarHostState,
                    context = state.snapshot.context,
                    editor = correction,
                    onBack = onBackFromCorrection,
                    onUpdate = onUpdateCorrection,
                    onSave = onSaveCorrection,
                    hostedScaffold = hostedScaffold,
                    roleLabel = roleLabel,
                    modifier = modifier,
                )
                state.detail != null -> RecordsDetailBody(
                    snackbarHostState = snackbarHostState,
                    context = state.snapshot.context,
                    detail = state.detail,
                    onBack = onBackFromDetail,
                    onOpenCorrection = onOpenCorrection,
                    hostedScaffold = hostedScaffold,
                    roleLabel = roleLabel,
                    modifier = modifier,
                )
                else -> RecordsListBody(
                    snackbarHostState = snackbarHostState,
                    state = state,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenRecord = onOpenRecord,
                    onLeaveConfirmed = onLeaveConfirmed,
                    hostedScaffold = hostedScaffold,
                    roleLabel = roleLabel,
                    rootTag = rootTag,
                    onDeferredGroup = onDeferredGroup,
                    onSelectGroup = onSelectGroup,
                    modifier = modifier,
                )
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
    onLeaveConfirmed: () -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String,
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
                .padding(padding)
                .testTag(rootTag),
        ) {
            CashierScreenHeader(
                title = "Records",
                displayName = state.snapshot.context.shopName,
                roleLabel = roleLabel,
                businessDate = state.snapshot.context.businessDate,
                postingOpen = state.snapshot.context.postingOpen,
            )
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
                    label = { Text("Search records") },
                    placeholder = { Text("Id, type, actor, notes…") },
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
                        selected = false,
                        onClick = { onDeferredGroup("Balances & ledgers") },
                        label = { Text("Balances") },
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsGroupBalances),
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onDeferredGroup("Closing history") },
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
private fun RecordsDetailBody(
    snackbarHostState: SnackbarHostState,
    context: NativeOwnerRecordsContext,
    detail: NativeOwnerRecordsDetail,
    onBack: () -> Unit,
    onOpenCorrection: (NativeCorrectionPrefill) -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String,
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
            CashierScreenHeader(
                title = "Records",
                displayName = context.shopName,
                roleLabel = roleLabel,
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(NativeUiTags.OwnerRecordsDetailBack),
                ) { Text("Back to records") }
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
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = !editor.saving

    LaunchedEffect(editor.dirty) {
        if (editor.dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (!canLeave) return
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
                CashierScreenHeader(
                    title = "Records",
                    displayName = context.shopName,
                    roleLabel = roleLabel,
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = canLeave,
                        modifier = Modifier.testTag(NativeUiTags.OwnerRecordsCorrectionBack),
                    ) { Text("Back to detail") }
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
            ) { Text("Discard") }
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
