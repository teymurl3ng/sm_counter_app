package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeMakeYogurtReceipt
import com.saaf.milkshop.application.operations.NativeMakeYogurtRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafYellow
import java.util.Locale

@Composable
fun MakeYogurtScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeMakeYogurtState,
    onRetry: () -> Unit,
    onRecord: (NativeMakeYogurtRequest) -> Unit,
    onMakeAnother: () -> Unit,
    onBackToHome: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeMakeYogurtState.Idle,
        NativeMakeYogurtState.Loading,
        -> MakeYogurtLoading(snackbarHostState, onNavigate, modifier)

        is NativeMakeYogurtState.LoadFailure -> MakeYogurtLoadFailure(
            snackbarHostState = snackbarHostState,
            message = state.message,
            onRetry = onRetry,
            onNavigate = onNavigate,
            onBackToHome = onBackToHome,
            modifier = modifier,
        )

        is NativeMakeYogurtState.Ready -> MakeYogurtReady(
            snackbarHostState = snackbarHostState,
            identity = identity,
            state = state,
            onRecord = onRecord,
            onMakeAnother = onMakeAnother,
            onBackToHome = onBackToHome,
            onNavigate = onNavigate,
            modifier = modifier,
        )
    }
}

@Composable
private fun MakeYogurtLoading(
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
                .testTag(NativeUiTags.OpsMakeYogurtLoading),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading make yogurt…")
        }
    }
}

@Composable
private fun MakeYogurtLoadFailure(
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
                modifier = Modifier.testTag(NativeUiTags.OpsMakeYogurtRetryLoad),
            ) { Text("Try again") }
            TextButton(
                onClick = onBackToHome,
                modifier = Modifier.testTag(NativeUiTags.OpsWorkBack),
            ) { Text("Back to Work") }
        }
    }
}

@Composable
private fun MakeYogurtReady(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeMakeYogurtState.Ready,
    onRecord: (NativeMakeYogurtRequest) -> Unit,
    onMakeAnother: () -> Unit,
    onBackToHome: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier,
) {
    var milkText by rememberSaveable { mutableStateOf("") }
    var yogurtText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    var discardOpen by remember { mutableStateOf(false) }

    val dirty = milkText.isNotBlank() || yogurtText.isNotBlank() || noteText.isNotBlank()
    val milk = milkText.toFiniteDoubleOrNull()
    val yogurt = yogurtText.toFiniteDoubleOrNull()
    val setupReady = state.context.milkProductPresent && state.context.yogurtProductPresent
    val canRecord = state.context.postingOpen &&
        !state.saving &&
        state.receipt == null &&
        setupReady &&
        milk != null && milk > 0.0 &&
        yogurt != null && yogurt > 0.0 &&
        milk <= state.context.milkOnHandLiters + 1e-9

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
                .testTag(NativeUiTags.OpsMakeYogurtForm),
        ) {
            CashierScreenHeader(
                title = "Make yogurt",
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
                        modifier = Modifier.testTag(NativeUiTags.OpsMakeYogurtClosed),
                    )
                }
                if (!setupReady) {
                    Text(
                        "Ask Owner to set up Milk and Yogurt products before making yogurt.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OpsMakeYogurtMissingSetup),
                    )
                }

                if (state.receipt != null) {
                    SuccessCard(state.receipt, onMakeAnother)
                } else {
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OpsMakeYogurtStock),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("On hand now", style = MaterialTheme.typography.titleSmall)
                            Text(state.context.milkOnHandLabel)
                            Text(state.context.yogurtOnHandLabel)
                        }
                    }
                    MeasurementField(
                        label = "Milk used (L)",
                        value = milkText,
                        enabled = !state.saving,
                        tag = NativeUiTags.OpsMakeYogurtMilk,
                        onValueChange = { milkText = it.filterDecimalInput() },
                    )
                    MeasurementField(
                        label = "Yogurt produced (kg)",
                        value = yogurtText,
                        enabled = !state.saving,
                        tag = NativeUiTags.OpsMakeYogurtYogurt,
                        onValueChange = { yogurtText = it.filterDecimalInput() },
                    )
                    TextButton(
                        onClick = { noteOpen = !noteOpen },
                        enabled = !state.saving,
                        modifier = Modifier.testTag(NativeUiTags.OpsMakeYogurtNoteToggle),
                    ) {
                        Text(if (noteOpen) "Hide note" else "Add a note")
                    }
                    if (noteOpen) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OpsMakeYogurtNote),
                            enabled = !state.saving,
                            label = { Text("Note") },
                            minLines = 2,
                        )
                    }
                    ReviewDock(
                        milk = milk,
                        yogurt = yogurt,
                        milkOnHand = state.context.milkOnHandLiters,
                        milkOnHandLabel = state.context.milkOnHandLabel,
                    )
                    if (state.error != null) {
                        Text(
                            state.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OpsMakeYogurtError),
                        )
                    }
                    val actionLabel = if (state.saving) {
                        "Recording…"
                    } else if (milk != null && yogurt != null && milk > 0.0 && yogurt > 0.0) {
                        "Record ${formatQty(milk)} L → ${formatQty(yogurt)} kg"
                    } else {
                        "Record batch"
                    }
                    Button(
                        onClick = {
                            val milkValue = milk ?: return@Button
                            val yogurtValue = yogurt ?: return@Button
                            onRecord(
                                NativeMakeYogurtRequest(
                                    milkLitersUsed = milkValue,
                                    yogurtKgProduced = yogurtValue,
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
                            .testTag(NativeUiTags.OpsMakeYogurtRecord),
                    ) {
                        Text(actionLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (discardOpen) {
        AlertDialog(
            onDismissRequest = { discardOpen = false },
            title = { Text("Discard this batch?") },
            text = { Text("Your measurements stay if you keep editing.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        discardOpen = false
                        onBackToHome()
                    },
                    modifier = Modifier.testTag(NativeUiTags.OpsMakeYogurtDiscardConfirm),
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(
                    onClick = { discardOpen = false },
                    modifier = Modifier.testTag(NativeUiTags.OpsMakeYogurtDiscardCancel),
                ) { Text("Keep editing") }
            },
        )
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
private fun ReviewDock(
    milk: Double?,
    yogurt: Double?,
    milkOnHand: Double,
    milkOnHandLabel: String,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsMakeYogurtReview),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Physical review", style = MaterialTheme.typography.titleSmall)
            Text("Milk on hand now: $milkOnHandLabel", style = MaterialTheme.typography.bodyMedium)
            if (milk == null || yogurt == null || milk <= 0.0 || yogurt <= 0.0) {
                Text(
                    "Enter milk used and yogurt produced to review the conversion.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("${formatQty(milk)} L milk → ${formatQty(yogurt)} kg yogurt")
                val remaining = milkOnHand - milk
                if (remaining < -1e-9) {
                    Text(
                        "Not enough milk for this batch.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Milk left after batch: ${formatQty(remaining)} L")
                }
            }
            Text(
                "No money on this screen — cost and yield review stay with Owner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuccessCard(
    receipt: NativeMakeYogurtReceipt,
    onMakeAnother: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsMakeYogurtSuccess),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Yogurt made", style = MaterialTheme.typography.titleMedium)
            }
            Text("Record #${receipt.eventId}")
            Text(
                "${formatQty(receipt.milkLitersUsed)} L milk → " +
                    "${formatQty(receipt.yogurtKgProduced)} kg yogurt",
            )
            Text("Milk on hand: ${receipt.milkOnHandLabel}")
            Text("Yogurt on hand: ${receipt.yogurtOnHandLabel}")
            Button(
                onClick = onMakeAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OpsMakeYogurtAnother),
            ) { Text("Make another") }
        }
    }
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
