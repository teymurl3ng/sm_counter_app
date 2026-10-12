package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.NativeCorrectionKind
import com.saaf.milkshop.application.owner.NativeOwnerFundsContext
import com.saaf.milkshop.application.owner.NativeOwnerRecordsContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeInformationFact
import com.saaf.milkshop.nativeui.common.NativeInformationSection
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.hapticSelectionAction
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.sync.BusinessClock
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal fun ownerFundsMoney(value: Double): String = BigDecimal.valueOf(value)
    .setScale(2, RoundingMode.HALF_UP).toPlainString()

@Composable
internal fun OwnerFundsCorrectionFields(
    editor: NativeCorrectionEditorState,
    enabled: Boolean,
    onUpdate: ((NativeCorrectionEditorState) -> NativeCorrectionEditorState) -> Unit,
) {
    val funds = editor.prefill.ownerFunds ?: return
    val isReceipt = editor.prefill.kind == NativeCorrectionKind.FundsReceipt
    Text("Correct a recording mistake or cancel a record that never happened.",
        style = MaterialTheme.typography.bodyMedium)
    if (!isReceipt) Text("Money actually received is recorded separately from the withdrawal.",
        style = MaterialTheme.typography.bodyMedium)
    NativeInformationSection {
        NativeInformationFact("Before", "PKR ${editor.prefill.monetaryAmount} · ${funds.currentAccountName}")
        NativeInformationFact("Withdrawal now", "PKR ${ownerFundsMoney(funds.currentWithdrawal)}")
        NativeInformationFact("Already received", "PKR ${ownerFundsMoney(funds.receivedTotal)}")
    }
    FilterChip(selected = editor.cancelDrawing,
        enabled = enabled && editor.prefill.monetaryAmount.toDoubleOrNull() != 0.0,
        onClick = hapticSelectionAction {
            onUpdate { it.copy(cancelDrawing = !it.cancelDrawing,
                ownerFundsAccountId = if (!it.cancelDrawing) funds.currentAccountId else it.ownerFundsAccountId,
                acknowledgeExcess = false) }
        }, label = { Text(if (isReceipt) "Cancel receipt" else "Cancel withdrawal") },
        modifier = Modifier.testTag(NativeUiTags.OwnerFundsCancel))
    OutlinedTextField(value = if (editor.cancelDrawing) "0.00" else editor.monetaryAmount,
        onValueChange = { value -> onUpdate { it.copy(monetaryAmount = value, acknowledgeExcess = false) } },
        enabled = enabled && !editor.cancelDrawing,
        label = { Text(if (isReceipt) "Corrected received amount (PKR)" else "Corrected withdrawal (PKR)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerRecordsCorrectionMonetaryAmount))
    Text("Account", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        funds.accounts.forEach { account ->
            FilterChip(selected = (editor.ownerFundsAccountId ?: funds.currentAccountId) == account.accountId,
                enabled = enabled && !editor.cancelDrawing,
                onClick = hapticSelectionAction { onUpdate { it.copy(ownerFundsAccountId = account.accountId) } },
                label = { Text(account.name) },
                modifier = Modifier.testTag(NativeUiTags.ownerFundsAccount(account.accountId)))
        }
    }
    val after = if (editor.cancelDrawing) 0.0 else editor.monetaryAmount.toDoubleOrNull()
    if (after != null && after.isFinite()) {
        val selected = funds.accounts.firstOrNull { it.accountId == (editor.ownerFundsAccountId ?: funds.currentAccountId) }
        NativeInformationSection {
            NativeInformationFact("After", "PKR ${ownerFundsMoney(after)} · ${if (editor.cancelDrawing) funds.currentAccountName else selected?.name.orEmpty()}")
        }
        val proposedReceived = if (isReceipt) BigDecimal.valueOf(funds.receivedTotal)
            .subtract(BigDecimal.valueOf(editor.prefill.monetaryAmount.toDoubleOrNull() ?: 0.0))
            .add(BigDecimal.valueOf(after)) else BigDecimal.valueOf(funds.receivedTotal)
        val proposedWithdrawal = BigDecimal.valueOf(if (isReceipt) funds.currentWithdrawal else after)
        if (proposedReceived > proposedWithdrawal) {
            OwnerFundsExcessReview(proposedWithdrawal.toDouble(), proposedReceived.toDouble(), editor.acknowledgeExcess, enabled) {
                onUpdate { state -> state.copy(acknowledgeExcess = it) }
            }
        }
    }
}

@Composable
private fun OwnerFundsExcessReview(
    withdrawal: Double,
    received: Double,
    acknowledged: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    NativeInformationSection {
        Text("Money received exceeds the corrected withdrawal", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error)
        NativeInformationFact("Withdrawal", "PKR ${ownerFundsMoney(withdrawal)}")
        NativeInformationFact("Received after saving", "PKR ${ownerFundsMoney(received)}")
        Text("This can be an owner contribution. Check the amounts before saving.",
            style = MaterialTheme.typography.bodyMedium)
        FilterChip(selected = acknowledged, enabled = enabled,
            onClick = hapticSelectionAction { onChange(!acknowledged) },
            label = { Text("I reviewed these amounts") },
            modifier = Modifier.testTag(NativeUiTags.OwnerFundsAcknowledgeExcess))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OwnerFundsReceiptBody(
    snackbarHostState: SnackbarHostState,
    context: NativeOwnerRecordsContext,
    editor: NativeOwnerFundsReceiptEditorState,
    onBack: () -> Unit,
    onUpdate: ((NativeOwnerFundsReceiptEditorState) -> NativeOwnerFundsReceiptEditorState) -> Unit,
    onSave: () -> Unit,
    onRefreshFacts: () -> Unit,
    hostedScaffold: HostedScaffold,
    roleLabel: String,
    showHeader: Boolean,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canEdit = !editor.saving && !editor.refreshingFacts && editor.savedEventId == null
    fun requestLeave(action: () -> Unit) {
        if (editor.saving || editor.refreshingFacts) return
        if (editor.dirty) leaveGuard.markDirty() else leaveGuard.clear()
        leaveGuard.requestLeave(action)
    }
    ActivePaneBackHandler { requestLeave(onBack) }
    var datePickerOpen by remember { mutableStateOf(false) }
    hostedScaffold(snackbarHostState, modifier, { action -> requestLeave(action) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag(NativeUiTags.OwnerFundsReceiptRoot)) {
            if (showHeader) CashierScreenHeader("Records", context.shopName, roleLabel,
                context.businessDate, context.postingOpen)
            Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editor.savedEventId != null) {
                    Text("Money received saved", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag(NativeUiTags.OwnerFundsSaved))
                    NativeInformationSection {
                        NativeInformationFact("Receipt", "#${editor.savedEventId}")
                        NativeInformationFact("Amount", "PKR ${ownerFundsMoney(editor.amount.toDoubleOrNull() ?: 0.0)}")
                        NativeInformationFact("Receiving date", editor.date)
                    }
                    Button(onClick = hapticAction(onBack), modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerFundsDone)) {
                        Text("Done")
                    }
                } else {
                    Text("Record money returned", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("Money actually received is a new transaction. Correct a mistaken withdrawal in its own record.",
                        style = MaterialTheme.typography.bodyMedium)
                    NativeInformationSection {
                        NativeInformationFact("Linked withdrawal", "#${editor.funds.rootEventId} · ${editor.funds.rootDate}")
                        NativeInformationFact("Withdrawal now", "PKR ${ownerFundsMoney(editor.funds.currentWithdrawal)}")
                        NativeInformationFact("Already received", "PKR ${ownerFundsMoney(editor.funds.receivedTotal)}")
                    }
                    OutlinedTextField(editor.amount,
                        onValueChange = { value -> onUpdate { it.copy(amount = value, acknowledgeExcess = false) } },
                        label = { Text("Money received (PKR)") }, enabled = canEdit, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerFundsReceiptAmount))
                    Text("Receiving account", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        editor.funds.accounts.forEach { account ->
                            FilterChip(selected = editor.accountId == account.accountId, enabled = canEdit,
                                onClick = hapticSelectionAction { onUpdate { it.copy(accountId = account.accountId) } },
                                label = { Text(account.name) },
                                modifier = Modifier.testTag(NativeUiTags.ownerFundsAccount(account.accountId)))
                        }
                    }
                    Text("Actual receiving date", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = hapticAction { datePickerOpen = true }, enabled = canEdit,
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerFundsReceiptDate)) {
                        Text(editor.date)
                    }
                    Text("Choose a date from the withdrawal day through today. A closed receiving day must be reopened first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(editor.note,
                        onValueChange = { value -> onUpdate { it.copy(note = value) } },
                        label = { Text("Note (optional)") }, enabled = canEdit, minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerFundsReceiptNote))
                    editor.amount.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }?.let { amount ->
                        val total = BigDecimal.valueOf(editor.funds.receivedTotal).add(BigDecimal.valueOf(amount))
                        if (total > BigDecimal.valueOf(editor.funds.currentWithdrawal)) {
                            OwnerFundsExcessReview(editor.funds.currentWithdrawal, total.toDouble(),
                                editor.acknowledgeExcess, canEdit) { acknowledged ->
                                onUpdate { it.copy(acknowledgeExcess = acknowledged) }
                            }
                        }
                    }
                    editor.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerFundsReceiptError))
                        OutlinedButton(onClick = hapticAction(onRefreshFacts), enabled = canEdit,
                            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerFundsRefreshFacts)) {
                            Text("Refresh current amounts")
                        }
                    }
                    Button(onClick = hapticAction(onSave), enabled = canEdit,
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerFundsReceiptSave)) {
                        Text(if (editor.saving) "Saving…" else if (editor.refreshingFacts) "Refreshing…"
                            else "Save money received")
                    }
                }
            }
        }
    }
    if (datePickerOpen) {
        val earliest = LocalDate.parse(editor.funds.rootDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val latest = BusinessClock.today().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(editor.date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrDefault(latest),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in earliest..latest
            },
        )
        DatePickerDialog(onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(onClick = hapticAction {
                    picker.selectedDateMillis?.let { selected ->
                        val date = Instant.ofEpochMilli(selected).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        onUpdate { it.copy(date = date) }
                    }
                    datePickerOpen = false
                }, enabled = picker.selectedDateMillis != null,
                    modifier = Modifier.testTag(NativeUiTags.OwnerFundsDateConfirm)) { Text("Use date") }
            }, dismissButton = {
                TextButton(onClick = hapticAction { datePickerOpen = false }) { Text("Cancel") }
            }, modifier = Modifier.testTag(NativeUiTags.OwnerFundsDatePicker)) {
            DatePicker(state = picker)
        }
    }
    if (leaveGuard.discardConfirmationOpen) OwnerFundsDiscardDialog(leaveGuard)
}

@Composable
private fun OwnerFundsDiscardDialog(leaveGuard: EntryLeaveGuard) {
    androidx.compose.material3.AlertDialog(onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard money received draft?") },
        text = { Text("The unsaved amount, date and note will be lost.") },
        confirmButton = { TextButton(onClick = hapticAction(leaveGuard::confirmDiscard)) { Text("Discard") } },
        dismissButton = { TextButton(onClick = hapticAction(leaveGuard::keepEditing)) { Text("Keep editing") } },
        modifier = Modifier.testTag(NativeUiTags.OwnerFundsDiscardDialog))
}
