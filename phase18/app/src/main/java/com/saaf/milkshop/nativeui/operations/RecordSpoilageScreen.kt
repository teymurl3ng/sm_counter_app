package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.saaf.milkshop.application.operations.NativeRecordSpoilageReceipt
import com.saaf.milkshop.application.operations.NativeRecordSpoilageRequest
import com.saaf.milkshop.application.operations.NativeSpoilageProduct
import com.saaf.milkshop.application.operations.NativeSpoilageReason
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import java.util.Locale

@Composable
fun RecordSpoilageScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeRecordSpoilageState,
    onRetry: () -> Unit,
    onSelectProduct: (Long) -> Unit,
    onOpenProductPicker: () -> Unit,
    onCloseProductPicker: () -> Unit,
    onSelectReason: (String) -> Unit,
    onOpenReasonPicker: () -> Unit,
    onCloseReasonPicker: () -> Unit,
    onRecord: (NativeRecordSpoilageRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToHome: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeRecordSpoilageState.Ready)?.saving != true
    LaunchedEffect(state is NativeRecordSpoilageState.Ready) {
        if (state !is NativeRecordSpoilageState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToHome)
    }
    WorkScaffold(
        snackbarHostState = snackbarHostState,
        section = OperationsSection.Work,
        onNavigate = { section ->
            if (canLeave) leaveGuard.requestLeave { onNavigate(section) }
        },
        modifier = modifier,
    ) { padding ->
        when (state) {
            NativeRecordSpoilageState.Idle,
            NativeRecordSpoilageState.Loading,
            -> RecordSpoilageLoadingBody(
                modifier = Modifier.padding(padding),
            )

            is NativeRecordSpoilageState.LoadFailure -> RecordSpoilageLoadFailureBody(
                message = state.message,
                onRetry = onRetry,
                onBackToHome = onBackToHome,
                modifier = Modifier.padding(padding),
            )

            is NativeRecordSpoilageState.Ready -> RecordSpoilageReadyContent(
                identity = identity,
                state = state,
                onSelectProduct = onSelectProduct,
                onOpenProductPicker = onOpenProductPicker,
                onCloseProductPicker = onCloseProductPicker,
                onSelectReason = onSelectReason,
                onOpenReasonPicker = onOpenReasonPicker,
                onCloseReasonPicker = onCloseReasonPicker,
                onRecord = onRecord,
                onRecordAnother = onRecordAnother,
                onBackToHome = onBackToHome,
                leaveGuard = leaveGuard,
                contentPadding = padding,
            )
        }
    }
    RecordSpoilageDiscardDialog(leaveGuard)
}

@Composable
internal fun RecordSpoilageLoadingBody(
    modifier: Modifier = Modifier,
) {
    NativeColdLoadingPane(
        label = "Loading record spoilage…",
        modifier = modifier.testTag(NativeUiTags.OpsRecordSpoilageLoading),
    )
}

@Composable
internal fun RecordSpoilageLoadFailureBody(
    message: String,
    onRetry: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NativeLoadFailurePane(message = message, onRetry = onRetry, modifier = modifier)
}

@Composable
internal fun RecordSpoilageReadyContent(
    identity: NativeSignedInIdentity,
    state: NativeRecordSpoilageState.Ready,
    onSelectProduct: (Long) -> Unit,
    onOpenProductPicker: () -> Unit,
    onCloseProductPicker: () -> Unit,
    onSelectReason: (String) -> Unit,
    onOpenReasonPicker: () -> Unit,
    onCloseReasonPicker: () -> Unit,
    onRecord: (NativeRecordSpoilageRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToHome: () -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var quantityText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteOpen by rememberSaveable { mutableStateOf(false) }

    val draftDirty = quantityText.isNotBlank() || noteText.isNotBlank() ||
        state.selectedProduct != null || state.selectedReason != null
    LaunchedEffect(draftDirty, state.receipt) {
        when {
            state.receipt != null -> leaveGuard.clear()
            draftDirty -> leaveGuard.markDirty()
            else -> leaveGuard.clear()
        }
    }

    val quantity = quantityText.toFiniteDoubleOrNull()
    val reason = state.selectedReason
    val product = state.selectedProduct
    val noteRequired = reason?.requiresNote == true
    val canRecord = state.context.postingOpen &&
        !state.saving &&
        state.receipt == null &&
        product != null &&
        reason != null &&
        quantity != null && quantity > 0.0 &&
        quantity <= product.onHand + 1e-9 &&
        (!noteRequired || noteText.isNotBlank()) &&
        state.context.products.isNotEmpty()

    fun requestLeave() {
        if (state.saving) return
        leaveGuard.requestLeave(onBackToHome)
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding()
                .testTag(NativeUiTags.OpsRecordSpoilageForm),
        ) {
            if (state.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            CashierScreenHeader(
                title = "Record spoilage",
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
                        modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageClosed),
                    )
                }
                if (state.context.products.isEmpty()) {
                    Text(
                        "No tracked product is set up. Ask Owner to add Milk or Yogurt first.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageMissingSetup),
                    )
                }

                if (state.receipt != null) {
                    SuccessCard(state.receipt, onRecordAnother)
                } else {
                    SelectorRow(
                        label = "Product",
                        value = product?.name,
                        placeholder = "Choose product",
                        enabled = !state.saving,
                        tag = NativeUiTags.OpsRecordSpoilageProduct,
                        onClick = onOpenProductPicker,
                    )
                    if (product != null) {
                        Text(
                            "On hand now: ${product.onHandLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageStock),
                        )
                    }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filterDecimalInput() },
                        enabled = !state.saving,
                        label = {
                            Text(
                                if (product == null) "Quantity" else "Quantity (${product.unitLabel})",
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Quantity" }
                            .testTag(NativeUiTags.OpsRecordSpoilageQuantity),
                    )
                    SelectorRow(
                        label = "Reason",
                        value = reason?.label,
                        placeholder = "Choose reason",
                        enabled = !state.saving,
                        tag = NativeUiTags.OpsRecordSpoilageReason,
                        onClick = onOpenReasonPicker,
                    )
                    if (!noteRequired) {
                        TextButton(
                            onClick = { noteOpen = !noteOpen },
                            enabled = !state.saving,
                            modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageNoteToggle),
                        ) {
                            Text(if (noteOpen) "Hide note" else "Add a note")
                        }
                    }
                    if (noteRequired || noteOpen) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            enabled = !state.saving,
                            label = {
                                Text(if (noteRequired) "Note (required)" else "Note")
                            },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OpsRecordSpoilageNote),
                        )
                    }
                    ReviewDock(product = product, quantity = quantity, reason = reason)
                    if (state.error != null) {
                        Text(
                            state.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageError),
                        )
                    }
                    Button(
                        onClick = {
                            val selected = product ?: return@Button
                            val qty = quantity ?: return@Button
                            val selectedReason = reason ?: return@Button
                            onRecord(
                                NativeRecordSpoilageRequest(
                                    productId = selected.productId,
                                    quantity = qty,
                                    reasonCode = selectedReason.code,
                                    notes = noteText,
                                ),
                            )
                        },
                        enabled = canRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaafYellow,
                            contentColor = SaafInk,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag(NativeUiTags.OpsRecordSpoilageRecord),
                    ) {
                        Text(
                            if (state.saving) "Recording…" else "Record spoilage",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (state.selectingProduct) {
            ProductPickerDialog(
                products = state.context.products,
                onSelect = onSelectProduct,
                onDismiss = onCloseProductPicker,
            )
        }
        if (state.selectingReason) {
            ReasonPickerDialog(
                reasons = state.context.reasons,
                onSelect = onSelectReason,
                onDismiss = onCloseReasonPicker,
            )
        }
    }
}

@Composable
internal fun RecordSpoilageDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard this entry?") },
        text = { Text("Your product and quantity stay if you keep editing.") },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageDiscardConfirm),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageDiscardCancel),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
private fun SelectorRow(
    label: String,
    value: String?,
    placeholder: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(tag)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    value ?: placeholder,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ReviewDock(
    product: NativeSpoilageProduct?,
    quantity: Double?,
    reason: NativeSpoilageReason?,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsRecordSpoilageReview),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Physical review", style = MaterialTheme.typography.titleSmall)
            if (product == null || quantity == null || quantity <= 0.0 || reason == null) {
                Text(
                    "Choose product, quantity, and reason to review the stock effect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("Remove ${formatQty(quantity)} ${product.unitLabel} ${product.name}")
                Text("Reason: ${reason.label}")
                val remaining = product.onHand - quantity
                if (remaining < -1e-9) {
                    Text(
                        "Not enough stock for this entry.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Left after: ${formatQty(remaining)} ${product.unitLabel}")
                }
            }
            Text(
                "No money on this screen — loss value stays with Owner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuccessCard(
    receipt: NativeRecordSpoilageReceipt,
    onRecordAnother: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsRecordSpoilageSuccess),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Spoilage recorded", style = MaterialTheme.typography.headlineMedium)
            }
            Text("Record #${receipt.eventId}")
            Text(
                "${formatQty(receipt.quantity)} ${receipt.product.unitLabel} ${receipt.product.name}",
            )
            Text(receipt.reasonLabel)
            Text("Left on hand: ${receipt.remainingLabel}")
            Button(
                onClick = onRecordAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OpsRecordSpoilageAnother),
            ) { Text("Record another") }
        }
    }
}

@Composable
private fun ProductPickerDialog(
    products: List<NativeSpoilageProduct>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageProductPicker),
        title = { Text("Choose product") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                products.forEach { product ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onSelect(product.productId) }
                            .padding(vertical = 12.dp)
                            .testTag(NativeUiTags.opsRecordSpoilageProduct(product.productId)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text(product.onHandLabel, style = MaterialTheme.typography.bodyMedium)
                        }
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

@Composable
private fun ReasonPickerDialog(
    reasons: List<NativeSpoilageReason>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(NativeUiTags.OpsRecordSpoilageReasonPicker),
        title = { Text("Choose reason") },
        text = {
            Column {
                reasons.forEach { reason ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onSelect(reason.code) }
                            .padding(vertical = 12.dp)
                            .testTag(NativeUiTags.opsRecordSpoilageReason(reason.code)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            reason.label,
                            Modifier.weight(1f),
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
