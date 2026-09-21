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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.NativeOwnerRecordsContext
import com.saaf.milkshop.application.owner.NativeOwnerRecordsDetail
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import java.util.Locale

@Composable
fun OwnerRecordsScreen(
    snackbarHostState: SnackbarHostState,
    state: NativeOwnerRecordsState,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    onDeferredGroup: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeOwnerRecordsState.Idle,
        NativeOwnerRecordsState.Loading,
        -> OwnerScaffold(snackbarHostState, OwnerSection.Records, onNavigate, modifier) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(24.dp)
                    .testTag(NativeUiTags.OwnerRecordsLoading),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading records…")
            }
        }

        is NativeOwnerRecordsState.LoadFailure -> OwnerScaffold(
            snackbarHostState, OwnerSection.Records, onNavigate, modifier,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .testTag(NativeUiTags.OwnerRecordsRoot),
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
            val detail = state.detail
            if (detail != null) {
                RecordsDetailBody(
                    snackbarHostState = snackbarHostState,
                    context = state.snapshot.context,
                    detail = detail,
                    onBack = onBackFromDetail,
                    onNavigate = onNavigate,
                    modifier = modifier,
                )
            } else {
                RecordsListBody(
                    snackbarHostState = snackbarHostState,
                    state = state,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenRecord = onOpenRecord,
                    onLeaveConfirmed = onLeaveConfirmed,
                    onNavigate = onNavigate,
                    onDeferredGroup = onDeferredGroup,
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
    onNavigate: (OwnerSection) -> Unit,
    onDeferredGroup: (String) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onLeaveConfirmed)
    OwnerScaffold(snackbarHostState, OwnerSection.Records, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerRecordsRoot),
        ) {
            CashierScreenHeader(
                title = "Records",
                displayName = state.snapshot.context.shopName,
                roleLabel = "Owner",
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
                        selected = true,
                        onClick = {},
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
                        selected = false,
                        onClick = { onDeferredGroup("Corrections") },
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
                    state.snapshot.records.isEmpty() -> {
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
                                "No matches",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Nothing matches this search. Clear the query to see recent activity.",
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
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    OwnerScaffold(snackbarHostState, OwnerSection.Records, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerRecordsDetail),
        ) {
            CashierScreenHeader(
                title = "Records",
                displayName = context.shopName,
                roleLabel = "Owner",
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
                Text(
                    "Corrections write UI comes next. This screen is inspect-only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(NativeUiTags.OwnerRecordsDetailReadOnly),
                )
            }
        }
    }
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
