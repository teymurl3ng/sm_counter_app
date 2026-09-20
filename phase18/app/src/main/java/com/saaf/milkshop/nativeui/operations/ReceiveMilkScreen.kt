package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeReceiveMilkPreview
import com.saaf.milkshop.application.operations.NativeReceiveMilkReceipt
import com.saaf.milkshop.application.operations.NativeReceiveMilkRequest
import com.saaf.milkshop.application.operations.NativeReceiveMilkVendor
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafYellow
import java.util.Locale

@Composable
fun ReceiveMilkScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeReceiveMilkState,
    onRetry: () -> Unit,
    onSelectVendor: (Long) -> Unit,
    onOpenVendorPicker: () -> Unit,
    onCloseVendorPicker: () -> Unit,
    onPreview: (Double, Double, Double) -> NativeReceiveMilkPreview?,
    onRecord: (NativeReceiveMilkRequest) -> Unit,
    onReceiveAnother: () -> Unit,
    onBackToHome: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeReceiveMilkState.Idle,
        NativeReceiveMilkState.Loading,
        -> ReceiveMilkLoading(snackbarHostState, onNavigate, modifier)

        is NativeReceiveMilkState.LoadFailure -> ReceiveMilkLoadFailure(
            snackbarHostState = snackbarHostState,
            message = state.message,
            onRetry = onRetry,
            onNavigate = onNavigate,
            onBackToHome = onBackToHome,
            modifier = modifier,
        )

        is NativeReceiveMilkState.Ready -> ReceiveMilkReady(
            snackbarHostState = snackbarHostState,
            identity = identity,
            state = state,
            onSelectVendor = onSelectVendor,
            onOpenVendorPicker = onOpenVendorPicker,
            onCloseVendorPicker = onCloseVendorPicker,
            onPreview = onPreview,
            onRecord = onRecord,
            onReceiveAnother = onReceiveAnother,
            onBackToHome = onBackToHome,
            onNavigate = onNavigate,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReceiveMilkLoading(
    snackbarHostState: SnackbarHostState,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier,
) {
    WorkScaffold(snackbarHostState, OperationsSection.Work, onNavigate, modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .padding(24.dp)
                .testTag(NativeUiTags.OpsReceiveMilkLoading),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading receive milk…")
        }
    }
}

@Composable
private fun ReceiveMilkLoadFailure(
    snackbarHostState: SnackbarHostState,
    message: String,
    onRetry: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBackToHome)
    WorkScaffold(snackbarHostState, OperationsSection.Work, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkRetryLoad),
            ) { Text("Try again") }
            TextButton(
                onClick = onBackToHome,
                modifier = Modifier.testTag(NativeUiTags.OpsWorkBack),
            ) { Text("Back to Work") }
        }
    }
}

@Composable
private fun ReceiveMilkReady(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeReceiveMilkState.Ready,
    onSelectVendor: (Long) -> Unit,
    onOpenVendorPicker: () -> Unit,
    onCloseVendorPicker: () -> Unit,
    onPreview: (Double, Double, Double) -> NativeReceiveMilkPreview?,
    onRecord: (NativeReceiveMilkRequest) -> Unit,
    onReceiveAnother: () -> Unit,
    onBackToHome: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier,
) {
    var litersText by rememberSaveable { mutableStateOf("") }
    var fatText by rememberSaveable { mutableStateOf("") }
    var lrText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    var discardOpen by remember { mutableStateOf(false) }

    val dirty = litersText.isNotBlank() || fatText.isNotBlank() || lrText.isNotBlank() ||
        noteText.isNotBlank() || state.selectedVendor != null
    val liters = litersText.toFiniteDoubleOrNull()
    val fat = fatText.toFiniteDoubleOrNull()
    val lr = lrText.toFiniteDoubleOrNull()
    val preview = if (liters != null && fat != null && lr != null) {
        onPreview(liters, fat, lr)
    } else {
        null
    }
    val canRecord = state.context.postingOpen &&
        !state.saving &&
        state.receipt == null &&
        state.selectedVendor != null &&
        preview != null &&
        state.context.milkProductPresent &&
        state.context.vendors.isNotEmpty()

    fun requestLeave() {
        if (state.saving) return
        if (state.receipt != null || !dirty) {
            onBackToHome()
        } else {
            discardOpen = true
        }
    }

    BackHandler(onBack = ::requestLeave)

    WorkScaffold(snackbarHostState, OperationsSection.Work, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag(NativeUiTags.OpsReceiveMilkForm),
        ) {
            CashierScreenHeader(
                title = "Receive milk",
                displayName = identity.displayName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = state.context.businessDate,
                postingOpen = state.context.postingOpen,
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = ::requestLeave,
                    modifier = Modifier.testTag(NativeUiTags.OpsWorkBack),
                ) { Text("Back to Work") }

                if (!state.context.postingOpen) {
                    Text(
                        "This business day is closed. Ask Owner to reopen it before recording.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkClosed),
                    )
                }
                if (state.context.vendors.isEmpty()) {
                    Text(
                        "No active supplier is set up. Ask Owner to add a supplier first.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkMissingSetup),
                    )
                }

                if (state.receipt != null) {
                    SuccessCard(state.receipt, onReceiveAnother)
                } else {
                    SupplierRow(
                        vendor = state.selectedVendor,
                        enabled = !state.saving,
                        onClick = onOpenVendorPicker,
                    )
                    MeasurementField(
                        label = "Liters",
                        value = litersText,
                        enabled = !state.saving,
                        tag = NativeUiTags.OpsReceiveMilkLiters,
                        onValueChange = { litersText = it.filterDecimalInput() },
                    )
                    MeasurementField(
                        label = "Fat %",
                        value = fatText,
                        enabled = !state.saving,
                        tag = NativeUiTags.OpsReceiveMilkFat,
                        onValueChange = { fatText = it.filterDecimalInput() },
                    )
                    MeasurementField(
                        label = "LR reading",
                        value = lrText,
                        enabled = !state.saving,
                        tag = NativeUiTags.OpsReceiveMilkLr,
                        onValueChange = { lrText = it.filterDecimalInput() },
                    )
                    TextButton(
                        onClick = { noteOpen = !noteOpen },
                        enabled = !state.saving,
                        modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkNoteToggle),
                    ) {
                        Text(if (noteOpen) "Hide note" else "Add a note")
                    }
                    if (noteOpen) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OpsReceiveMilkNote),
                            enabled = !state.saving,
                            label = { Text("Note") },
                            minLines = 2,
                        )
                    }
                    ReviewDock(preview = preview, milkOnHand = state.context.milkOnHandLabel)
                    if (state.error != null) {
                        Text(
                            state.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkError),
                        )
                    }
                    Button(
                        onClick = {
                            val vendor = state.selectedVendor ?: return@Button
                            val raw = liters ?: return@Button
                            val fatValue = fat ?: return@Button
                            val lrValue = lr ?: return@Button
                            onRecord(
                                NativeReceiveMilkRequest(
                                    vendorAccountId = vendor.accountId,
                                    rawLiters = raw,
                                    fatPercentage = fatValue,
                                    lrReading = lrValue,
                                    notes = noteText,
                                ),
                            )
                        },
                        enabled = canRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaafYellow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag(NativeUiTags.OpsReceiveMilkRecord),
                    ) {
                        Text(
                            if (state.saving) "Recording…" else "Record receipt",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (state.selectingVendor) {
        VendorPickerDialog(
            vendors = state.context.vendors,
            onSelect = onSelectVendor,
            onDismiss = onCloseVendorPicker,
        )
    }

    if (discardOpen) {
        AlertDialog(
            onDismissRequest = { discardOpen = false },
            title = { Text("Discard this receipt?") },
            text = { Text("Your supplier and measurements stay if you keep editing.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        discardOpen = false
                        onBackToHome()
                    },
                    modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkDiscardConfirm),
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(
                    onClick = { discardOpen = false },
                    modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkDiscardCancel),
                ) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun SupplierRow(
    vendor: NativeReceiveMilkVendor?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(NativeUiTags.OpsReceiveMilkSupplier)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.LocalShipping, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Supplier", style = MaterialTheme.typography.labelMedium)
                Text(
                    vendor?.name ?: "Choose supplier",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun MeasurementField(
    label: String,
    value: String,
    enabled: Boolean,
    tag: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
            .testTag(tag),
    )
}

@Composable
private fun ReviewDock(preview: NativeReceiveMilkPreview?, milkOnHand: String) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsReceiveMilkReview),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Physical review", style = MaterialTheme.typography.titleSmall)
            Text("Milk on hand now: $milkOnHand", style = MaterialTheme.typography.bodyMedium)
            if (preview == null) {
                Text(
                    "Enter liters, fat, and LR to see SNF, TS, and adjusted liters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("SNF ${formatQty(preview.snfPercentage)}%")
                Text("TS ${formatQty(preview.tsPercentage)}%")
                Text("Adjusted ${formatQty(preview.adjustedLiters)} L")
            }
            Text(
                "No money on this screen — supplier payables stay with Owner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuccessCard(
    receipt: NativeReceiveMilkReceipt,
    onReceiveAnother: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsReceiveMilkSuccess),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Milk received", style = MaterialTheme.typography.titleMedium)
            }
            Text("Record #${receipt.eventId}")
            Text(receipt.vendor.name)
            Text(
                "${formatQty(receipt.rawLiters)} L in · Fat ${formatQty(receipt.fatPercentage)}% · " +
                    "LR ${formatQty(receipt.lrReading)}",
            )
            Text("SNF ${formatQty(receipt.snfPercentage)}% · TS ${formatQty(receipt.tsPercentage)}%")
            Text("Adjusted ${formatQty(receipt.adjustedLiters)} L")
            Text("Milk on hand: ${receipt.milkOnHandLabel}")
            Button(
                onClick = onReceiveAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OpsReceiveMilkAnother),
            ) { Text("Receive another") }
        }
    }
}

@Composable
private fun VendorPickerDialog(
    vendors: List<NativeReceiveMilkVendor>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(NativeUiTags.OpsReceiveMilkSupplierPicker),
        title = { Text("Choose supplier") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (vendors.isEmpty()) {
                    Text("No active supplier is available.")
                }
                vendors.forEach { vendor ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onSelect(vendor.accountId) }
                            .padding(vertical = 12.dp)
                            .testTag(NativeUiTags.opsReceiveMilkVendor(vendor.accountId)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            vendor.name,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun String.filterDecimalInput(): String {
    val cleaned = filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    return if (firstDot < 0) {
        cleaned
    } else {
        cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")
    }
}

private fun String.toFiniteDoubleOrNull(): Double? =
    toDoubleOrNull()?.takeIf { it.isFinite() }

private fun formatQty(value: Double): String =
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
