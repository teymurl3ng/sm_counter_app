package com.saaf.milkshop.nativeui.vendorpayment

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.nativeListItem

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notes
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
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentAccount
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentReceipt
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentRequest
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentVendor
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import androidx.compose.ui.platform.LocalContext
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.hapticCommit
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

@Composable
internal fun VendorPaymentLoadingBody(
    modifier: Modifier = Modifier,
) {
    NativeColdLoadingPane(label = "Loading supplier payment…", modifier = modifier)
}

@Composable
internal fun VendorPaymentLoadFailureBody(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NativeLoadFailurePane(
        message = message,
        onRetry = onRetry,
        modifier = modifier,
        retryTestTag = NativeUiTags.VendorPaymentRetryLoad,
    )
}

@Composable
internal fun VendorPaymentDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard this payment?") },
        text = { Text("The supplier, amount, and note you entered will be cleared.") },
        confirmButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticDestructiveAction(leaveGuard::confirmDiscard),
                modifier = Modifier.testTag(NativeUiTags.VendorPaymentDiscardDraft),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticAction(leaveGuard::keepEditing),
                modifier = Modifier.testTag(NativeUiTags.VendorPaymentKeepDraft),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
internal fun VendorPaymentEntryContent(
    identity: NativeSignedInIdentity,
    state: NativeVendorPaymentState.Ready,
    onRecordPayment: (NativeVendorPaymentRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBack: () -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var vendorPickerOpen by rememberSaveable { mutableStateOf(false) }
    var paymentPickerOpen by rememberSaveable { mutableStateOf(false) }
    var vendorAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var paymentAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val amountPaisa = parsePaisa(amountText)
    val selectedVendor = state.context.vendors.singleOrNull { it.accountId == vendorAccountId }
    val selectedPaymentAccount = state.context.paymentAccounts.singleOrNull {
        it.accountId == paymentAccountId
    }
    val validRequest = selectedVendor != null &&
        selectedPaymentAccount != null &&
        amountPaisa != null && amountPaisa > 0L &&
        state.context.postingOpen && !state.saving
    val dirty = draftTouched && state.receipt == null

    LaunchedEffect(state.context.defaultPaymentAccountId, state.context.paymentAccounts) {
        if (state.context.paymentAccounts.none { it.accountId == paymentAccountId }) {
            paymentAccountId = state.context.defaultPaymentAccountId
                ?: state.context.paymentAccounts.firstOrNull()?.accountId
        }
    }
    LaunchedEffect(state.context.vendors) {
        if (vendorAccountId != null &&
            state.context.vendors.none { it.accountId == vendorAccountId }
        ) {
            vendorAccountId = null
        }
    }
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    if (vendorPickerOpen) {
        VendorPickerDialog(
            vendors = state.context.vendors,
            selectedId = vendorAccountId,
            onSelect = { vendor ->
                vendorAccountId = vendor.accountId
                vendorPickerOpen = false
                draftTouched = true
            },
            onDismiss = { vendorPickerOpen = false },
        )
    }

    if (paymentPickerOpen) {
        PaymentPickerDialog(
            accounts = state.context.paymentAccounts,
            selectedId = paymentAccountId,
            onSelect = { account ->
                paymentAccountId = account.accountId
                paymentPickerOpen = false
                draftTouched = true
            },
            onDismiss = { paymentPickerOpen = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize().nativeRefreshOverlay(state.refreshing)
            .padding(contentPadding)
            .imePadding(),
    ) {
        CashierScreenHeader(
            title = "Pay supplier",
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
            VendorPaymentSuccess(
                receipt = state.receipt,
                onRecordAnother = {
                    amountText = ""
                    notes = ""
                    notesExpanded = false
                    vendorAccountId = null
                    draftTouched = false
                    onRecordAnother()
                },
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            VendorPaymentForm(
                state = state,
                amountText = amountText,
                amountPaisa = amountPaisa,
                selectedVendor = selectedVendor,
                selectedPaymentAccount = selectedPaymentAccount,
                notesExpanded = notesExpanded,
                notes = notes,
                validRequest = validRequest,
                onOpenVendorPicker = { vendorPickerOpen = true },
                onAmountChange = {
                    amountText = sanitizeAmount(it)
                    draftTouched = true
                },
                onOpenPaymentPicker = { paymentPickerOpen = true },
                onToggleNotes = {
                    notesExpanded = !notesExpanded
                    draftTouched = true
                },
                onNotesChange = {
                    notes = it.take(240)
                    draftTouched = true
                },
                onSubmit = {
                    val vendor = selectedVendor ?: return@VendorPaymentForm
                    val amount = amountPaisa ?: return@VendorPaymentForm
                    onRecordPayment(
                        NativeVendorPaymentRequest(
                            vendorAccountId = vendor.accountId,
                            paymentAccountId = requireNotNull(paymentAccountId),
                            amountPaisa = amount,
                            notes = notes,
                        ),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        }
    }
}

@Composable
private fun VendorPaymentForm(
    state: NativeVendorPaymentState.Ready,
    amountText: String,
    amountPaisa: Long?,
    selectedVendor: NativeVendorPaymentVendor?,
    selectedPaymentAccount: NativeVendorPaymentAccount?,
    notesExpanded: Boolean,
    notes: String,
    validRequest: Boolean,
    onOpenVendorPicker: () -> Unit,
    onAmountChange: (String) -> Unit,
    onOpenPaymentPicker: () -> Unit,
    onToggleNotes: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .testTag(NativeUiTags.VendorPaymentForm),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (!state.context.postingOpen) {
            StatusPanel(
                title = "Today is closed",
                message = "Reopen the day before paying a supplier.",
                warning = true,
                tag = NativeUiTags.VendorPaymentPostingClosed,
            )
        }

        FieldGroup("Supplier") {
            SelectorSurface(
                title = when {
                    selectedVendor != null -> selectedVendor.name
                    state.context.vendors.isEmpty() -> "No active suppliers"
                    else -> "Choose supplier"
                },
                supporting = when {
                    selectedVendor != null -> "Vendor payable"
                    state.context.vendors.isEmpty() -> "Add an active supplier before paying."
                    else -> "Who received this payment"
                },
                enabled = state.context.vendors.isNotEmpty() && !state.saving,
                icon = Icons.Outlined.LocalShipping,
                onClick = onOpenVendorPicker,
                tag = NativeUiTags.VendorPaymentVendorSelector,
            )
        }

        FieldGroup("Amount") {
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChange,
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
                    .semantics { contentDescription = "Supplier payment amount" }
                    .testTag(NativeUiTags.VendorPaymentAmountInput),
            )
        }

        FieldGroup("Paid from") {
            SelectorSurface(
                title = selectedPaymentAccount?.name ?: "Choose Cash/Bank account",
                supporting = if (state.context.paymentAccounts.isEmpty()) {
                    "No active Cash/Bank account is available."
                } else {
                    "The payment amount will be taken from here."
                },
                enabled = state.context.paymentAccounts.isNotEmpty() && !state.saving,
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onOpenPaymentPicker,
                tag = NativeUiTags.VendorPaymentPaymentSelector,
            )
        }

        TextButton(
            onClick = com.saaf.milkshop.nativeui.common.hapticAction(onToggleNotes),
            enabled = !state.saving,
            modifier = Modifier.heightIn(min = 48.dp).testTag(NativeUiTags.VendorPaymentNoteToggle),
        ) {
            Icon(if (notesExpanded) Icons.Outlined.Close else Icons.Outlined.Notes, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (notesExpanded) "Remove note" else "Add note")
        }
        if (notesExpanded) {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                enabled = !state.saving,
                label = { Text("Note") },
                supportingText = { Text("Optional · ${notes.length}/240") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.VendorPaymentNoteInput),
            )
        }

        ReviewPanel(
            vendor = selectedVendor,
            paymentAccount = selectedPaymentAccount,
            amountPaisa = amountPaisa,
        )

        state.error?.let {
            StatusPanel(
                title = "Payment not recorded",
                message = it,
                warning = true,
                tag = NativeUiTags.VendorPaymentError,
            )
        }

        val hapticContext = LocalContext.current
        val press = rememberPressSource()
        Button(
            onClick = { hapticContext.hapticCommit(); onSubmit() },
            enabled = validRequest,
            modifier = Modifier
                .pressScale(press)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(NativeUiTags.VendorPaymentRecord),
        colors = ButtonDefaults.buttonColors(
            containerColor = SaafYellow,
            contentColor = SaafInk,
        ),
            interactionSource = press,
        ) {
            Text(
                if (state.saving) "Recording…" else "Save payment",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ReviewPanel(
    vendor: NativeVendorPaymentVendor?,
    paymentAccount: NativeVendorPaymentAccount?,
    amountPaisa: Long?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("After recording", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    vendor == null -> "Choose a supplier to review the payment."
                    amountPaisa == null || amountPaisa <= 0L -> "Enter the amount paid."
                    paymentAccount == null -> "Choose where the payment was paid from."
                    else ->
                        "${vendor.name} · PKR ${formatMoney(amountPaisa)} from ${paymentAccount.name}"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun VendorPaymentSuccess(
    receipt: NativeVendorPaymentReceipt,
    onRecordAnother: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.saaf.milkshop.nativeui.common.NativeReceiptFeedback("supplier-payment", receipt.eventId)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(NativeUiTags.VendorPaymentSuccess),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text("Supplier paid", style = MaterialTheme.typography.headlineMedium)
        Text(
            "PKR ${formatMoney(receipt.amountPaisa)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(receipt.vendor.name, style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReceiptRow("Paid from", receipt.paymentAccount.name)
                HorizontalDivider()
                ReceiptRow("Record", "#${receipt.eventId}")
            }
        }
        Button(
            onClick = com.saaf.milkshop.nativeui.common.hapticAction(onRecordAnother),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(NativeUiTags.VendorPaymentAnother),
        ) { Text("Pay another") }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VendorPickerDialog(
    vendors: List<NativeVendorPaymentVendor>,
    selectedId: Long?,
    onSelect: (NativeVendorPaymentVendor) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(NativeUiTags.VendorPaymentVendorDialog),
        title = { Text("Choose supplier") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                vendors.forEach { vendor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Selection, selected = vendor.accountId == selectedId)
                            .clickable(onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelect(vendor) })
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = if (vendor.accountId == selectedId) {
                                    "${vendor.name}, selected"
                                } else vendor.name
                            }
                            .testTag(NativeUiTags.vendorPaymentVendor(vendor.accountId))
                            .nativeListItemContent(NativeListStyle.Selection),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(vendor.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (vendor.accountId == selectedId) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticAction(onDismiss),
                modifier = Modifier.testTag(NativeUiTags.VendorPaymentVendorCancel),
            ) { Text("Cancel") }
        },
    )
}

@Composable
private fun PaymentPickerDialog(
    accounts: List<NativeVendorPaymentAccount>,
    selectedId: Long?,
    onSelect: (NativeVendorPaymentAccount) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paid from") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Selection, selected = account.accountId == selectedId)
                            .clickable(onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelect(account) })
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = if (account.accountId == selectedId) {
                                    "${account.name}, selected"
                                } else account.name
                            }
                            .testTag(NativeUiTags.vendorPaymentPaymentAccount(account.accountId))
                            .nativeListItemContent(NativeListStyle.Selection),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(account.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (account.accountId == selectedId) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticAction(onDismiss),
                modifier = Modifier.testTag(NativeUiTags.VendorPaymentPaymentCancel),
            ) { Text("Cancel") }
        },
    )
}

@Composable
private fun FieldGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun SelectorSurface(
    title: String,
    supporting: String,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(enabled = enabled, onClick = com.saaf.milkshop.nativeui.common.hapticAction(onClick))
            .semantics { role = Role.Button }
            .testTag(tag),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
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
    val contentColor = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
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

private fun formatMoney(paisa: Long): String = String.format(Locale.US, "%,.2f", paisa / 100.0)
