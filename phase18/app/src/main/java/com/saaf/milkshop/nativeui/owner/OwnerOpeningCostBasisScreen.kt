package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.nativeListItem
import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerOpeningCostRow
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
internal fun OwnerOpeningCostBasisBody(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    rows: List<NativeOwnerOpeningCostRow>,
    summary: String?,
    error: String?,
    onOpenEditor: (Long) -> Unit,
    onOpenShopSettings: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            header()
            summary?.takeIf { rows.isNotEmpty() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(NativeUiTags.OwnerCostingOpeningSummary),
                )
            }
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(NativeUiTags.OwnerCostingPlansError),
                )
            }
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.OwnerCostingOpeningList),
            ) {
                if (rows.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .testTag(NativeUiTags.OwnerCostingOpeningEmpty),
                        ) {
                            Text(
                                summary
                                    ?: "No supported opening stock needs a cost basis.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(NativeUiTags.OwnerCostingOpeningSummary),
                            )
                            Button(
                                onClick = hapticAction(onOpenShopSettings),
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .testTag(NativeUiTags.OwnerCostingOpeningOpenShopSettings),
                            ) {
                                Text("Open Shop settings")
                            }
                        }
                    }
                } else {
                    items(rows, key = { it.productId }) { row ->
                        OwnerOpeningCostRow(
                            row = row,
                            onOpenEditor = { onOpenEditor(row.productId) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerOpeningCostRow(
    row: NativeOwnerOpeningCostRow,
    onOpenEditor: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Record)
            .nativeListItemContent(NativeListStyle.Record)
            .testTag(NativeUiTags.ownerCostingOpeningRow(row.productId)),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    row.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Opening quantity · ${row.quantity} ${row.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = hapticAction(onOpenEditor),
                modifier = Modifier.testTag(NativeUiTags.ownerCostingOpeningEdit(row.productId)),
            ) {
                Text(if (row.basisId == null) "Add cost" else "Correct")
            }
        }
        if (row.basisId == null) {
            Text(
                "Cost basis missing — inventory valuation is incomplete.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                "${row.totalCostLabel} · ${row.basisKindLabel}",
                style = MaterialTheme.typography.bodyMedium,
            )
            row.evidenceNotes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun OwnerOpeningCostBasisEditorScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    row: NativeOwnerOpeningCostRow,
    basisKinds: List<String>,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (OpeningCostBasisInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var totalCostText by rememberSaveable(row.productId, row.basisId) {
        mutableStateOf(row.totalCostInput)
    }
    var basisKind by rememberSaveable(row.productId, row.basisId) {
        mutableStateOf(row.basisKindCode)
    }
    var evidenceNotes by rememberSaveable(row.productId, row.basisId) {
        mutableStateOf(row.evidenceNotes.orEmpty())
    }
    val availableKinds = basisKinds.ifEmpty {
        listOf("documented_historical", "owner_estimate")
    }
    val totalCostPaisa = parseOpeningRupeesToPaisaOrNull(totalCostText)
    val evidenceRequired = basisKind == "owner_estimate"
    val totalCostError = when {
        totalCostText.isBlank() -> null
        totalCostPaisa == null -> "Enter a valid total amount."
        totalCostPaisa <= 0L -> "Total cost must be greater than zero."
        else -> null
    }
    val evidenceError = evidenceRequired && evidenceNotes.isBlank()
    val canSave = !saving && totalCostPaisa != null && totalCostPaisa > 0L &&
        basisKind in availableKinds && (!evidenceRequired || evidenceNotes.isNotBlank())
    val requestBack = { leaveGuard.requestLeave(onBack) }

    BackHandler(onBack = requestBack)
    hostedScaffold(snackbarHostState, modifier, { leave -> leaveGuard.requestLeave(leave) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCostingOpeningForm),
        ) {
            CashierScreenHeader(
                title = "Opening cost basis",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    row.productName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Opening quantity is locked to the current opening state.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = "${row.quantity} ${row.unit}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Opening quantity") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingOpeningQuantity),
                )
                OutlinedTextField(
                    value = totalCostText,
                    onValueChange = {
                        totalCostText = it
                        leaveGuard.markDirty()
                    },
                    label = { Text("Total opening cost (PKR)") },
                    supportingText = {
                        Text(
                            totalCostError ?: "Enter the total cost for this opening quantity.",
                            modifier = if (totalCostError != null) {
                                Modifier.testTag(NativeUiTags.OwnerCostingOpeningTotalError)
                            } else {
                                Modifier
                            },
                        )
                    },
                    isError = totalCostError != null,
                    singleLine = true,
                    enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingOpeningTotal),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Basis source", style = MaterialTheme.typography.labelLarge)
                    availableKinds.forEach { kind ->
                        FilterChip(
                            selected = basisKind == kind,
                            onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                                basisKind = kind
                                leaveGuard.markDirty()
                            },
                            enabled = !saving,
                            label = { Text(openingBasisKindLabel(kind)) },
                            modifier = Modifier.testTag(NativeUiTags.ownerCostingOpeningKind(kind)),
                        )
                    }
                }
                OutlinedTextField(
                    value = evidenceNotes,
                    onValueChange = {
                        evidenceNotes = it
                        leaveGuard.markDirty()
                    },
                    label = {
                        Text(if (evidenceRequired) "Estimate evidence" else "Evidence or notes (optional)")
                    },
                    supportingText = {
                        if (evidenceRequired) {
                            Text(
                                "Explain how this estimate was obtained.",
                                modifier = if (evidenceError) {
                                    Modifier.testTag(NativeUiTags.OwnerCostingOpeningEvidenceError)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    },
                    isError = evidenceError,
                    minLines = 3,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingOpeningEvidence),
                )
                if (row.basisId != null) {
                    Text(
                        "Saving creates a correction and preserves the previous cost basis in history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCostingOpeningSupersedeHint),
                    )
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansError),
                    )
                }
                Button(
                    onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                        if (!canSave) return@hapticCommitAction
                        onSave(
                            OpeningCostBasisInput(
                                openingEventId = row.openingEventId,
                                productId = row.productId,
                                quantity = BigDecimal(row.quantity),
                                totalCostPaisa = totalCostPaisa!!,
                                basisKind = basisKind,
                                evidenceNotes = evidenceNotes.trim().ifBlank { null },
                                supersedesBasisId = row.basisId,
                            ),
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingOpeningSave),
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (row.basisId == null) "Save opening cost" else "Save correction")
                    }
                }
            }
        }
        OwnerCostingPlansDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this opening cost?",
            body = "The values you entered will be cleared.",
            discardTag = NativeUiTags.OwnerCostingOpeningDiscard,
            keepTag = NativeUiTags.OwnerCostingOpeningKeep,
        )
    }
}

private fun openingBasisKindLabel(code: String): String = when (code) {
    "documented_historical" -> "Documented bill or historical record"
    "owner_estimate" -> "Owner estimate"
    else -> code.replace('_', ' ')
}

private fun parseOpeningRupeesToPaisaOrNull(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return runCatching {
        BigDecimal(trimmed)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()?.takeIf { it >= 0L }
}
