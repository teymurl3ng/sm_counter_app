package com.saaf.milkshop.nativeui.expense

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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.saaf.milkshop.application.expense.NativeExpenseCategory
import com.saaf.milkshop.application.expense.NativeExpensePaymentAccount
import com.saaf.milkshop.application.expense.NativeExpenseReceipt
import com.saaf.milkshop.application.expense.NativeExpenseRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
internal fun ExpenseLoadingBody(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading expense entry…")
        }
    }
}

@Composable
internal fun ExpenseLoadFailureBody(
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
        Button(onClick = onRetry, modifier = Modifier.testTag(NativeUiTags.ExpenseRetryLoad)) {
            Text("Try again")
        }
    }
}

@Composable
internal fun ExpenseDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard this expense?") },
        text = { Text("The category, amount, and note you entered will be cleared.") },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(NativeUiTags.ExpenseDiscardDraft),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(NativeUiTags.ExpenseKeepDraft),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
internal fun ExpenseEntryContent(
    identity: NativeSignedInIdentity,
    state: NativeExpenseState.Ready,
    onRecordExpense: (NativeExpenseRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBack: () -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }
    var paymentPickerOpen by rememberSaveable { mutableStateOf(false) }
    var categoryAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var paymentAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val amountPaisa = parsePaisa(amountText)
    val selectedCategory = state.context.categories.singleOrNull { it.accountId == categoryAccountId }
    val selectedPaymentAccount = state.context.paymentAccounts.singleOrNull {
        it.accountId == paymentAccountId
    }
    val validRequest = selectedCategory != null &&
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
    LaunchedEffect(state.context.categories) {
        if (categoryAccountId != null &&
            state.context.categories.none { it.accountId == categoryAccountId }
        ) {
            categoryAccountId = null
        }
    }
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    if (categoryPickerOpen) {
        CategoryPickerDialog(
            categories = state.context.categories,
            selectedId = categoryAccountId,
            onSelect = { category ->
                categoryAccountId = category.accountId
                categoryPickerOpen = false
                draftTouched = true
            },
            onDismiss = { categoryPickerOpen = false },
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
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
    ) {
        if (state.refreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        CashierScreenHeader(
            title = "Record expense",
            displayName = identity.displayName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = state.context.businessDate,
            postingOpen = state.context.postingOpen,
        )
        if (state.receipt != null) {
            ExpenseSuccess(
                receipt = state.receipt,
                onRecordAnother = {
                    amountText = ""
                    notes = ""
                    notesExpanded = false
                    categoryAccountId = null
                    draftTouched = false
                    onRecordAnother()
                },
                onBack = onBack,
                modifier = Modifier.weight(1f),
            )
        } else {
            ExpenseForm(
                state = state,
                amountText = amountText,
                amountPaisa = amountPaisa,
                selectedCategory = selectedCategory,
                selectedPaymentAccount = selectedPaymentAccount,
                notesExpanded = notesExpanded,
                notes = notes,
                validRequest = validRequest,
                onOpenCategoryPicker = { categoryPickerOpen = true },
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
                    val category = selectedCategory ?: return@ExpenseForm
                    val amount = amountPaisa ?: return@ExpenseForm
                    onRecordExpense(
                        NativeExpenseRequest(
                            expenseCategoryAccountId = category.accountId,
                            paymentAccountId = requireNotNull(paymentAccountId),
                            amountPaisa = amount,
                            notes = notes,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExpenseForm(
    state: NativeExpenseState.Ready,
    amountText: String,
    amountPaisa: Long?,
    selectedCategory: NativeExpenseCategory?,
    selectedPaymentAccount: NativeExpensePaymentAccount?,
    notesExpanded: Boolean,
    notes: String,
    validRequest: Boolean,
    onOpenCategoryPicker: () -> Unit,
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
            .testTag(NativeUiTags.ExpenseForm),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (!state.context.postingOpen) {
            StatusPanel(
                title = "Today is closed",
                message = "Reopen the day before recording an expense.",
                warning = true,
                tag = NativeUiTags.ExpensePostingClosed,
            )
        }

        FieldGroup("Category") {
            SelectorSurface(
                title = when {
                    selectedCategory != null -> selectedCategory.name
                    state.context.categories.isEmpty() -> "No active categories"
                    else -> "Choose category"
                },
                supporting = when {
                    selectedCategory != null -> "Operating expense"
                    state.context.categories.isEmpty() -> "Add an active expense category before recording."
                    else -> "Rent, electricity, fuel, and more"
                },
                enabled = state.context.categories.isNotEmpty() && !state.saving,
                icon = Icons.Outlined.Category,
                onClick = onOpenCategoryPicker,
                tag = NativeUiTags.ExpenseCategorySelector,
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
                    .semantics { contentDescription = "Expense amount" }
                    .testTag(NativeUiTags.ExpenseAmountInput),
            )
        }

        FieldGroup("Paid from") {
            SelectorSurface(
                title = selectedPaymentAccount?.name ?: "Choose Cash/Bank account",
                supporting = if (state.context.paymentAccounts.isEmpty()) {
                    "No active Cash/Bank account is available."
                } else {
                    "The expense amount will be taken from here."
                },
                enabled = state.context.paymentAccounts.isNotEmpty() && !state.saving,
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onOpenPaymentPicker,
                tag = NativeUiTags.ExpensePaymentSelector,
            )
        }

        TextButton(
            onClick = onToggleNotes,
            enabled = !state.saving,
            modifier = Modifier.heightIn(min = 48.dp).testTag(NativeUiTags.ExpenseNoteToggle),
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
                modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.ExpenseNoteInput),
            )
        }

        ReviewPanel(
            category = selectedCategory,
            paymentAccount = selectedPaymentAccount,
            amountPaisa = amountPaisa,
        )

        state.error?.let {
            StatusPanel(
                title = "Expense not recorded",
                message = it,
                warning = true,
                tag = NativeUiTags.ExpenseError,
            )
        }

        Button(
            onClick = onSubmit,
            enabled = validRequest,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(NativeUiTags.ExpenseRecord),
        ) {
            Text(
                if (state.saving) "Recording…" else "Save expense",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ReviewPanel(
    category: NativeExpenseCategory?,
    paymentAccount: NativeExpensePaymentAccount?,
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
                    category == null -> "Choose a category to review the expense."
                    amountPaisa == null || amountPaisa <= 0L -> "Enter the amount paid."
                    paymentAccount == null -> "Choose where the expense was paid from."
                    else ->
                        "${category.name} · PKR ${formatMoney(amountPaisa)} from ${paymentAccount.name}"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExpenseSuccess(
    receipt: NativeExpenseReceipt,
    onRecordAnother: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(NativeUiTags.ExpenseSuccess),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text("Expense recorded", style = MaterialTheme.typography.headlineMedium)
        Text(
            "PKR ${formatMoney(receipt.amountPaisa)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(receipt.category.name, style = MaterialTheme.typography.titleMedium)
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
            onClick = onRecordAnother,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(NativeUiTags.ExpenseAnother),
        ) { Text("Record another") }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(NativeUiTags.ExpenseBack),
        ) { Text("Back") }
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
private fun CategoryPickerDialog(
    categories: List<NativeExpenseCategory>,
    selectedId: Long?,
    onSelect: (NativeExpenseCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(NativeUiTags.ExpenseCategoryDialog),
        title = { Text("Choose category") },
        text = {
            Column {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onSelect(category) }
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = if (category.accountId == selectedId) {
                                    "${category.name}, selected"
                                } else category.name
                            }
                            .testTag(NativeUiTags.expenseCategory(category.accountId))
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(category.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (category.accountId == selectedId) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(NativeUiTags.ExpenseCategoryCancel),
            ) { Text("Cancel") }
        },
    )
}

@Composable
private fun PaymentPickerDialog(
    accounts: List<NativeExpensePaymentAccount>,
    selectedId: Long?,
    onSelect: (NativeExpensePaymentAccount) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paid from") },
        text = {
            Column {
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onSelect(account) }
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = if (account.accountId == selectedId) {
                                    "${account.name}, selected"
                                } else account.name
                            }
                            .testTag(NativeUiTags.expensePaymentAccount(account.accountId))
                            .padding(vertical = 12.dp),
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
                onClick = onDismiss,
                modifier = Modifier.testTag(NativeUiTags.ExpensePaymentCancel),
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
            .clickable(enabled = enabled, onClick = onClick)
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

private fun formatMoney(paisa: Long): String = String.format(Locale.US, "%,.2f", paisa / 100.0)
