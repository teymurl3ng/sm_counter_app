package com.saaf.milkshop.nativeui.drawing

import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.drawing.NativeDrawingRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.hapticCommit
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.expense.ExpenseDiscardDialog
import com.saaf.milkshop.nativeui.owner.OwnScaffold
import com.saaf.milkshop.nativeui.expense.ExpenseLoadFailureBody
import com.saaf.milkshop.nativeui.expense.ExpenseLoadingBody
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Owner-owned entry surface for OWNER_DRAWING (M18): cash taken out of the
 * drawer for non-shop purposes. The drawer is fixed (the seeded "Cash"
 * account) — the form is amount + optional note, deliberately smaller than
 * the expense form because there is no category and no counter-account.
 */
@Composable
fun OwnerDrawingEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeDrawingState,
    onRetry: () -> Unit,
    onRecordDrawing: (NativeDrawingRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToWork: () -> Unit,
    onRegisterGuard: (EntryLeaveGuard?) -> Unit = {},
    ownScaffold: OwnScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    DisposableEffect(leaveGuard) {
        onRegisterGuard(leaveGuard)
        onDispose { onRegisterGuard(null) }
    }
    val canLeave = (state as? NativeDrawingState.Ready)?.saving != true

    LaunchedEffect(state is NativeDrawingState.Ready) {
        if (state !is NativeDrawingState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.OwnerDrawingEntryRoot),
    ) {
        ownScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            NativeSectionTransition(
                targetState = state,
                contentKey = { if (it is NativeDrawingState.Idle || it is NativeDrawingState.Loading) NativeDrawingState.Loading::class else it::class },
            ) { state -> when (state) {
                NativeDrawingState.Idle,
                NativeDrawingState.Loading,
                -> ExpenseLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeDrawingState.LoadFailure -> ExpenseLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding),
                )
                is NativeDrawingState.Ready -> DrawingEntryContent(
                    identity = identity,
                    state = state,
                    onRecordDrawing = onRecordDrawing,
                    onRecordAnother = onRecordAnother,
                    onBack = onBackToWork,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
            }
        }
        ExpenseDiscardDialog(leaveGuard)
    }
}

@Composable
private fun DrawingEntryContent(
    identity: NativeSignedInIdentity,
    state: NativeDrawingState.Ready,
    onRecordDrawing: (NativeDrawingRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBack: () -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier = Modifier,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val amountPaisa = parsePaisa(amountText)
    val validRequest = amountPaisa != null && amountPaisa > 0L &&
        state.context.postingOpen && !state.saving
    val dirty = draftTouched && state.receipt == null

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    Column(
        modifier = modifier
            .fillMaxSize().nativeRefreshOverlay(state.refreshing)
            .padding(contentPadding)
            .imePadding(),
    ) {
        CashierScreenHeader(
            title = "Owner drawing",
            displayName = identity.displayName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = state.context.businessDate,
            postingOpen = state.context.postingOpen,
        )
        NativeSectionTransition(
            targetState = state,
            contentKey = { it.receipt != null },
            modifier = Modifier.weight(1f),
        ) { state ->
        if (state.receipt != null) {
            DrawingSuccess(
                receipt = state.receipt,
                onRecordAnother = {
                    amountText = ""
                    notes = ""
                    notesExpanded = false
                    draftTouched = false
                    onRecordAnother()
                },
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .testTag(NativeUiTags.DrawingForm),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (!state.context.postingOpen) {
                    StatusPanel(
                        title = "Today is closed",
                        message = "Reopen the day before recording a drawing.",
                        warning = true,
                        tag = NativeUiTags.DrawingPostingClosed,
                    )
                }

                FieldGroup("From") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                state.context.paymentAccount.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "The shop drawer — drawings are not expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                FieldGroup("Amount") {
                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = sanitizeAmount(it)
                            draftTouched = true
                        },
                        enabled = !state.saving,
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
                            .semantics { contentDescription = "Drawing amount" }
                            .testTag(NativeUiTags.DrawingAmountInput),
                    )
                }

                TextButton(
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                        notesExpanded = !notesExpanded
                        draftTouched = true
                    },
                    enabled = !state.saving,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(NativeUiTags.DrawingNoteToggle),
                ) {
                    Icon(if (notesExpanded) Icons.Outlined.Close else Icons.Outlined.Notes, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (notesExpanded) "Remove note" else "Add note")
                }
                if (notesExpanded) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it.take(240)
                            draftTouched = true
                        },
                        enabled = !state.saving,
                        label = { Text("Note") },
                        supportingText = { Text("Optional · ${notes.length}/240") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.DrawingNoteInput),
                    )
                }

                ReviewPanel(
                    paymentAccountName = state.context.paymentAccount.name,
                    amountPaisa = amountPaisa,
                )

                state.error?.let {
                    StatusPanel(
                        title = "Drawing not recorded",
                        message = it,
                        warning = true,
                        tag = NativeUiTags.DrawingError,
                    )
                }

                val hapticContext = LocalContext.current
                val press = rememberPressSource()
                Button(
                    onClick = { hapticContext.hapticCommit(); onRecordDrawing(
                        NativeDrawingRequest(amountPaisa = requireNotNull(amountPaisa), notes = notes)
                    ) },
                    enabled = validRequest,
                    modifier = Modifier
                        .pressScale(press)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag(NativeUiTags.DrawingRecord),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaafYellow,
                        contentColor = SaafInk,
                    ),
                    interactionSource = press,
                ) {
                    Text(
                        if (state.saving) "Recording…" else "Save drawing",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        }
    }
}

@Composable
private fun ReviewPanel(
    paymentAccountName: String,
    amountPaisa: Long?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("After recording", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    amountPaisa == null || amountPaisa <= 0L -> "Enter the amount taken."
                    else -> "PKR ${formatMoney(amountPaisa)} from $paymentAccountName"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DrawingSuccess(
    receipt: com.saaf.milkshop.application.drawing.NativeDrawingReceipt,
    onRecordAnother: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.saaf.milkshop.nativeui.common.NativeReceiptFeedback("drawing", receipt.eventId)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(NativeUiTags.DrawingSuccess),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            "Drawing recorded · #${receipt.eventId}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "PKR ${formatMoney(receipt.amountPaisa)} from ${receipt.paymentAccount.name}",
            style = MaterialTheme.typography.titleMedium,
            textDecoration = TextDecoration.None,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = com.saaf.milkshop.nativeui.common.hapticAction(onRecordAnother),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(NativeUiTags.DrawingAnother),
            colors = ButtonDefaults.buttonColors(
                containerColor = SaafYellow,
                contentColor = SaafInk,
            ),
        ) {
            Text("Record another", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = com.saaf.milkshop.nativeui.common.hapticAction(onBack), modifier = Modifier.testTag(NativeUiTags.DrawingBackToWork)) {
            Text("Back to Work")
        }
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(tag),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
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
    NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(paisa / 100.0)
