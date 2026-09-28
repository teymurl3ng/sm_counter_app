package com.saaf.milkshop.nativeui.cashclose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountSnapshot
import com.saaf.milkshop.application.cashclose.NativeCashCloseDayRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseFacade
import com.saaf.milkshop.application.cashclose.NativeCashCloseReceipt
import com.saaf.milkshop.application.cashclose.NativeCashCloseReopenSubmission
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun CashCloseLoadingBody(
    modifier: Modifier = Modifier,
) {
    NativeColdLoadingPane(label = "Loading cash count and close…", modifier = modifier)
}

@Composable
internal fun CashCloseLoadFailureBody(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, modifier = Modifier.testTag(NativeUiTags.CashCloseRetryLoad)) {
            Text("Try again")
        }
    }
}

@Composable
internal fun CashCloseDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard this count?") },
        text = { Text("The counted amount and reopen details you entered will be cleared.") },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(NativeUiTags.CashCloseDiscardDraft),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(NativeUiTags.CashCloseKeepDraft),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
internal fun CashCloseEntryContent(
    identity: NativeSignedInIdentity,
    state: NativeCashCloseState.Ready,
    onRecordCount: (NativeCashCloseCountRequest) -> Unit,
    onCloseDay: (NativeCashCloseDayRequest) -> Unit,
    onReopenDay: (String, CharArray) -> Unit,
    onBack: () -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    allowReopen: Boolean = true,
) {
    var countedText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var varianceReason by rememberSaveable { mutableStateOf("") }
    var reopenReason by rememberSaveable { mutableStateOf("") }
    var reopenPassphrase by remember { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val countedPaisa = parsePaisa(countedText)
    val activeCount = state.lastRecordedCount ?: state.context.latestCount
    val needsReason = activeCount != null &&
        NativeCashCloseFacade.needsVarianceReason(activeCount.variancePaisa)
    val dirty = draftTouched && state.receipt == null && state.reopenSuccess == null

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    LaunchedEffect(state.reopenSuccess?.actionId) {
        if (state.reopenSuccess != null) {
            reopenReason = ""
            reopenPassphrase = ""
            draftTouched = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize().nativeRefreshOverlay(state.refreshing)
            .padding(contentPadding)
            .imePadding(),
    ) {
        CashierScreenHeader(
            title = "Cash count and close",
            displayName = identity.displayName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = state.context.businessDate,
            postingOpen = state.context.postingOpen,
        )
        if (state.receipt != null) {
            CashCloseSuccess(
                receipt = state.receipt,
                onBack = onBack,
                modifier = Modifier.weight(1f),
            )
        } else {
            CashCloseForm(
                state = state,
                countedText = countedText,
                countedPaisa = countedPaisa,
                notes = notes,
                varianceReason = varianceReason,
                reopenReason = reopenReason,
                reopenPassphrase = reopenPassphrase,
                activeCount = activeCount,
                needsReason = needsReason,
                onCountedChange = {
                    countedText = sanitizeAmount(it)
                    draftTouched = true
                },
                onNotesChange = {
                    notes = it.take(240)
                    draftTouched = true
                },
                onVarianceReasonChange = {
                    varianceReason = it.take(240)
                    draftTouched = true
                },
                onReopenReasonChange = {
                    reopenReason = it.take(240)
                    draftTouched = true
                },
                onReopenPassphraseChange = {
                    reopenPassphrase = it
                    draftTouched = true
                },
                onRecordCount = {
                    val amount = countedPaisa ?: return@CashCloseForm
                    onRecordCount(
                        NativeCashCloseCountRequest(
                            countedCashPaisa = amount,
                            notes = notes,
                        ),
                    )
                    draftTouched = false
                },
                onCloseDay = {
                    val count = activeCount ?: return@CashCloseForm
                    onCloseDay(
                        NativeCashCloseDayRequest(
                            physicalCashCountId = count.countId,
                            varianceReason = varianceReason,
                        ),
                    )
                    draftTouched = false
                },
                onReopenDay = {
                    if (reopenReason.trim().isEmpty() || reopenPassphrase.isBlank()) return@CashCloseForm
                    val secret = reopenPassphrase.toCharArray()
                    reopenPassphrase = ""
                    onReopenDay(reopenReason.trim(), secret)
                    draftTouched = false
                },
                allowReopen = allowReopen,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CashCloseForm(
    state: NativeCashCloseState.Ready,
    countedText: String,
    countedPaisa: Long?,
    notes: String,
    varianceReason: String,
    reopenReason: String,
    reopenPassphrase: String,
    activeCount: NativeCashCloseCountSnapshot?,
    needsReason: Boolean,
    onCountedChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onVarianceReasonChange: (String) -> Unit,
    onReopenReasonChange: (String) -> Unit,
    onReopenPassphraseChange: (String) -> Unit,
    onRecordCount: () -> Unit,
    onCloseDay: () -> Unit,
    onReopenDay: () -> Unit,
    allowReopen: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val canCount = state.context.postingOpen &&
        countedPaisa != null &&
        countedPaisa >= 0L &&
        !state.saving
    val reasonOk = !needsReason || varianceReason.trim().isNotEmpty()
    val canClose = state.context.postingOpen &&
        activeCount != null &&
        reasonOk &&
        !state.saving
    val canReopen = allowReopen &&
        !state.context.postingOpen &&
        reopenReason.trim().isNotEmpty() &&
        reopenPassphrase.isNotBlank() &&
        !state.saving

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .testTag(NativeUiTags.CashCloseForm),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (!state.context.postingOpen) {
            if (allowReopen) {
                StatusPanel(
                    title = "Today is closed",
                    message = "Transactions and cash counts for this date are locked. Reopen with a reason and your full passphrase.",
                    warning = true,
                    tag = NativeUiTags.CashClosePostingClosed,
                )
                ReopenPanel(
                    reason = reopenReason,
                    passphrase = reopenPassphrase,
                    saving = state.saving,
                    canReopen = canReopen,
                    onReasonChange = onReopenReasonChange,
                    onPassphraseChange = onReopenPassphraseChange,
                    onReopen = onReopenDay,
                )
            } else {
                StatusPanel(
                    title = "Today is closed",
                    message = "Transactions and cash counts for this date are locked. Reopening a closed day is Owner-only.",
                    warning = true,
                    tag = NativeUiTags.CashClosePostingClosed,
                )
                Text(
                    text = "Ask an Owner to reopen if this date must accept more transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(NativeUiTags.CashCloseReopenOwnerOnlyNotice),
                )
            }
        } else {
            StatusPanel(
                title = "Open business date",
                message = "Count the Cash drawer, compare to what the app expects, then close the day.",
                warning = false,
                tag = NativeUiTags.CashClosePostingOpen,
            )
            state.reopenSuccess?.let { success ->
                ReopenSuccessPanel(success)
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.CashCloseExpectedPanel),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App expected", style = MaterialTheme.typography.titleMedium)
                Text(
                    "PKR ${formatMoney(state.context.expectedCashPaisa)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "${state.context.cashAccountName} drawer · ${state.context.businessDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FieldGroup("Physical drawer cash (PKR)") {
            OutlinedTextField(
                value = countedText,
                onValueChange = onCountedChange,
                enabled = state.context.postingOpen && !state.saving,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                placeholder = { Text("0") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Counted drawer cash amount" }
                    .testTag(NativeUiTags.CashCloseAmountInput),
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            enabled = state.context.postingOpen && !state.saving,
            label = { Text("Count notes (optional)") },
            supportingText = { Text("${notes.length}/240") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.CashCloseNotesInput),
        )

        Button(
            onClick = onRecordCount,
            enabled = canCount,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(NativeUiTags.CashCloseRecordCount),
        colors = ButtonDefaults.buttonColors(
            containerColor = SaafYellow,
            contentColor = SaafInk,
        ),
        ) {
            Text(
                if (state.saving && state.context.postingOpen) "Saving count…" else "Record count",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (activeCount != null) {
            VariancePanel(count = activeCount)
            if (needsReason && state.context.postingOpen) {
                OutlinedTextField(
                    value = varianceReason,
                    onValueChange = onVarianceReasonChange,
                    enabled = !state.saving,
                    label = { Text("Explain the cash difference") },
                    supportingText = { Text("Required before closing · ${varianceReason.length}/240") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.CashCloseVarianceReason),
                )
            }
            if (state.context.postingOpen) {
                Button(
                    onClick = onCloseDay,
                    enabled = canClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaafYellow,
                        contentColor = SaafInk,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag(NativeUiTags.CashCloseCloseDay),
                ) {
                    Text(
                        if (state.saving) "Closing…" else "Close ${state.context.businessDate}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        } else if (state.context.postingOpen) {
            Text(
                "Save a physical drawer count for this date before closing it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(NativeUiTags.CashCloseNeedCount),
            )
        }

        state.error?.let {
            StatusPanel(
                title = "Could not finish",
                message = it,
                warning = true,
                tag = NativeUiTags.CashCloseError,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ReopenPanel(
    reason: String,
    passphrase: String,
    saving: Boolean,
    canReopen: Boolean,
    onReasonChange: (String) -> Unit,
    onPassphraseChange: (String) -> Unit,
    onReopen: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Reopen this date", style = MaterialTheme.typography.titleMedium)
            Text(
                "Quick PIN cannot authorize reopen. Enter your full Saaf passphrase.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                enabled = !saving,
                label = { Text("Reason for reopening") },
                supportingText = { Text("Required · ${reason.length}/240") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.CashCloseReopenReason),
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = onPassphraseChange,
                enabled = !saving,
                label = { Text("Current full passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Current full passphrase for reopen" }
                    .testTag(NativeUiTags.CashCloseReopenPassphrase),
            )
            Button(
                onClick = onReopen,
                enabled = canReopen,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(NativeUiTags.CashCloseReopenDay),
            colors = ButtonDefaults.buttonColors(
                containerColor = SaafYellow,
                contentColor = SaafInk,
            ),
            ) {
                Text(
                    if (saving) "Reopening…" else "Confirm Reopen",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun ReopenSuccessPanel(success: NativeCashCloseReopenSubmission) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(NativeUiTags.CashCloseReopenSuccess),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Date reopened", fontWeight = FontWeight.Bold)
                Text(
                    "${success.businessDate} is open again. Count the drawer before closing.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun VariancePanel(count: NativeCashCloseCountSnapshot) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.CashCloseVariancePanel),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("After counting", style = MaterialTheme.typography.titleMedium)
            SummaryRow("You counted", "PKR ${formatMoney(count.countedCashPaisa)}")
            HorizontalDivider()
            SummaryRow("App expected", "PKR ${formatMoney(count.expectedCashPaisa)}")
            HorizontalDivider()
            SummaryRow("Difference", formatVariance(count.variancePaisa))
        }
    }
}

@Composable
private fun CashCloseSuccess(
    receipt: NativeCashCloseReceipt,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(NativeUiTags.CashCloseSuccess),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text("Day closed", style = MaterialTheme.typography.headlineMedium)
        Text(
            receipt.businessDate,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryRow("You counted", "PKR ${formatMoney(receipt.countedCashPaisa)}")
                HorizontalDivider()
                SummaryRow("App expected", "PKR ${formatMoney(receipt.expectedCashPaisa)}")
                HorizontalDivider()
                SummaryRow("Difference", formatVariance(receipt.variancePaisa))
                HorizontalDivider()
                SummaryRow("Close record", "#${receipt.actionId}")
            }
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(NativeUiTags.CashCloseBack),
        ) { Text("Back to Work") }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FieldGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun StatusPanel(
    title: String,
    message: String,
    warning: Boolean,
    tag: String,
) {
    val color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (warning) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(tag),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun sanitizeAmount(raw: String): String {
    val normalized = raw.replace(',', '.')
    if (normalized.isEmpty()) return ""
    if (!normalized.matches(Regex("\\d{0,9}(\\.\\d{0,2})?"))) return normalized.dropLast(1)
    return normalized
}

private fun parsePaisa(value: String): Long? = runCatching {
    if (value.isBlank() || value == ".") return null
    BigDecimal(value).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
}.getOrNull()

private fun formatMoney(paisa: Long): String =
    String.format(Locale.US, "%,.2f", paisa / 100.0)

private fun formatVariance(paisa: Long): String {
    val absValue = formatMoney(abs(paisa))
    return when {
        paisa > 0L -> "Over by PKR $absValue"
        paisa < 0L -> "Short by PKR $absValue"
        else -> "Even"
    }
}
