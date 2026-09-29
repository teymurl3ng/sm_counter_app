package com.saaf.milkshop.nativeui.collection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.PersonSearch
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
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
import com.saaf.milkshop.application.collection.NativeCollectionCustomer
import com.saaf.milkshop.application.collection.NativeCollectionCustomerBalance
import com.saaf.milkshop.application.collection.NativeCollectionPaymentAccount
import com.saaf.milkshop.application.collection.NativeCollectionReceipt
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.hapticConfirm
import com.saaf.milkshop.nativeui.common.imePaddingExcludingNavBar
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierNavigation
import com.saaf.milkshop.nativeui.sale.CashierNavigationRail
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
fun CashierCollectionScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeCollectionState,
    onRetry: () -> Unit,
    onSelectCustomer: (Long) -> Unit,
    onRecordCollection: (NativeCollectionRequest) -> Unit,
    onCollectAnother: () -> Unit,
    onViewRecord: (Long) -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeCollectionState.Ready)?.saving != true
    LaunchedEffect(state is NativeCollectionState.Ready) {
        if (state !is NativeCollectionState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onLeaveConfirmed)
    }
    CollectionScaffold(
        snackbarHostState = snackbarHostState,
        onNavigate = { section ->
            if (canLeave) {
                leaveGuard.requestLeave { onNavigate(section) }
            }
        },
        modifier = modifier,
    ) { padding ->
        NativeSectionTransition(
        targetState = state,
        contentKey = { it::class },
    ) { state -> when (state) {
            NativeCollectionState.Idle,
            NativeCollectionState.Loading,
            -> CollectionLoadingBody(
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding(),
            )

            is NativeCollectionState.LoadFailure -> CollectionLoadFailureBody(
                message = state.message,
                onRetry = onRetry,
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding(),
            )

            is NativeCollectionState.Ready -> CollectionEntryContent(
                identity = identity,
                state = state,
                onSelectCustomer = onSelectCustomer,
                onRecordCollection = onRecordCollection,
                onCollectAnother = onCollectAnother,
                onViewRecord = onViewRecord,
                leaveGuard = leaveGuard,
                contentPadding = padding,
            )
        }
        }
    }
    CollectionDiscardDialog(leaveGuard)
}

@Composable
internal fun CollectionLoadingBody(
    modifier: Modifier = Modifier,
) {
    NativeColdLoadingPane(label = "Loading customer payments…", modifier = modifier)
}

@Composable
internal fun CollectionLoadFailureBody(
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
        Button(onClick = onRetry, modifier = Modifier.testTag(NativeUiTags.CollectRetryLoad)) {
            Text("Try again")
        }
    }
}

@Composable
private fun CollectionScaffold(
    snackbarHostState: SnackbarHostState,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        compactNavigation = {
            CashierNavigation(selected = CashierSection.Collect, onNavigate = onNavigate)
        },
        expandedNavigation = {
            CashierNavigationRail(selected = CashierSection.Collect, onNavigate = onNavigate)
        },
        content = content,
    )
}

@Composable
internal fun CollectionDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard this payment?") },
        text = { Text("The customer, amount, and note you entered will be cleared.") },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(NativeUiTags.CollectDiscardDraft),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(NativeUiTags.CollectKeepDraft),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
internal fun CollectionEntryContent(
    identity: NativeSignedInIdentity,
    state: NativeCollectionState.Ready,
    onSelectCustomer: (Long) -> Unit,
    onRecordCollection: (NativeCollectionRequest) -> Unit,
    onCollectAnother: () -> Unit,
    onViewRecord: (Long) -> Unit,
    leaveGuard: EntryLeaveGuard,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var customerPickerOpen by rememberSaveable { mutableStateOf(false) }
    var paymentPickerOpen by rememberSaveable { mutableStateOf(false) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var paymentAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var advanceConfirmationOpen by rememberSaveable { mutableStateOf(false) }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val selectedCustomer = state.selectedCustomer
    val amountPaisa = parsePaisa(amountText)
    val selectedPaymentAccount = state.context.paymentAccounts.singleOrNull {
        it.accountId == paymentAccountId
    }
    val resultingBalance = selectedCustomer?.let { it.balancePaisa - (amountPaisa ?: 0L) }
    val advancePaisa = resultingBalance?.takeIf { it < 0L }?.let { -it } ?: 0L
    val validRequest = selectedCustomer != null &&
        selectedPaymentAccount != null &&
        amountPaisa != null && amountPaisa > 0L &&
        state.context.postingOpen && !state.saving && !state.selectingCustomer
    val dirty = draftTouched && state.receipt == null

    LaunchedEffect(state.context.defaultPaymentAccountId, state.context.paymentAccounts) {
        if (state.context.paymentAccounts.none { it.accountId == paymentAccountId }) {
            paymentAccountId = state.context.defaultPaymentAccountId
                ?: state.context.paymentAccounts.firstOrNull()?.accountId
        }
    }
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    if (advanceConfirmationOpen && selectedCustomer != null && amountPaisa != null) {
        AdvanceConfirmationDialog(
            customer = selectedCustomer,
            amountPaisa = amountPaisa,
            advancePaisa = advancePaisa,
            saving = state.saving,
            onConfirm = {
                advanceConfirmationOpen = false
                onRecordCollection(
                    NativeCollectionRequest(
                        customerAccountId = selectedCustomer.customer.accountId,
                        paymentAccountId = requireNotNull(paymentAccountId),
                        amountPaisa = amountPaisa,
                        notes = notes,
                    ),
                )
            },
            onCancel = { advanceConfirmationOpen = false },
        )
    }

    if (customerPickerOpen) {
        CustomerPickerDialog(
            customers = state.context.customers,
            selecting = state.selectingCustomer,
            onSelect = { customer ->
                customerPickerOpen = false
                amountText = ""
                draftTouched = true
                onSelectCustomer(customer.accountId)
            },
            onDismiss = { customerPickerOpen = false },
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
            .imePaddingExcludingNavBar(),
    ) {
        CashierScreenHeader(
            title = "Collect payment",
            displayName = identity.displayName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = state.context.businessDate,
            postingOpen = state.context.postingOpen,
        )
        if (state.receipt != null) {
            CollectionSuccess(
                receipt = state.receipt,
                onCollectAnother = {
                    amountText = ""
                    notes = ""
                    notesExpanded = false
                    draftTouched = false
                    onCollectAnother()
                },
                onViewRecord = { onViewRecord(state.receipt.eventId) },
                modifier = Modifier.weight(1f),
            )
        } else {
            CollectionForm(
                state = state,
                amountText = amountText,
                amountPaisa = amountPaisa,
                selectedPaymentAccount = selectedPaymentAccount,
                resultingBalancePaisa = resultingBalance,
                advancePaisa = advancePaisa,
                notesExpanded = notesExpanded,
                notes = notes,
                validRequest = validRequest,
                onOpenCustomerPicker = { customerPickerOpen = true },
                onAmountChange = {
                    amountText = sanitizeAmount(it)
                    draftTouched = true
                },
                onQuick1000 = {
                    amountText = "1000"
                    draftTouched = true
                },
                onPayFull = {
                    val owed = selectedCustomer?.balancePaisa ?: 0L
                    if (owed > 0L) {
                        amountText = formatInputPaisa(owed)
                        draftTouched = true
                    }
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
                    val customer = selectedCustomer ?: return@CollectionForm
                    val amount = amountPaisa ?: return@CollectionForm
                    if (advancePaisa > 0L) {
                        advanceConfirmationOpen = true
                    } else {
                        onRecordCollection(
                            NativeCollectionRequest(
                                customerAccountId = customer.customer.accountId,
                                paymentAccountId = requireNotNull(paymentAccountId),
                                amountPaisa = amount,
                                notes = notes,
                            ),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CollectionForm(
    state: NativeCollectionState.Ready,
    amountText: String,
    amountPaisa: Long?,
    selectedPaymentAccount: NativeCollectionPaymentAccount?,
    resultingBalancePaisa: Long?,
    advancePaisa: Long,
    notesExpanded: Boolean,
    notes: String,
    validRequest: Boolean,
    onOpenCustomerPicker: () -> Unit,
    onAmountChange: (String) -> Unit,
    onQuick1000: () -> Unit,
    onPayFull: () -> Unit,
    onOpenPaymentPicker: () -> Unit,
    onToggleNotes: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val customer = state.selectedCustomer
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (!state.context.postingOpen) {
            StatusPanel(
                title = "Today is closed",
                message = "Reopen the day before recording a customer payment.",
                warning = true,
                tag = NativeUiTags.CollectPostingClosed,
            )
        }

        FieldGroup("Customer") {
            SelectorSurface(
                title = when {
                    state.selectingCustomer -> "Loading customer…"
                    customer != null -> customer.customer.name
                    state.context.customers.isEmpty() -> "No active customers"
                    else -> "Choose customer"
                },
                supporting = when {
                    customer != null -> balanceLabel(customer.balancePaisa)
                    state.context.customers.isEmpty() -> "Add an active customer before collecting payment."
                    else -> "Search by name or phone"
                },
                enabled = state.context.customers.isNotEmpty() && !state.saving && !state.selectingCustomer,
                icon = Icons.Outlined.PersonSearch,
                onClick = onOpenCustomerPicker,
                tag = NativeUiTags.CollectCustomerSelector,
            )
        }

        FieldGroup("Amount received") {
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChange,
                enabled = customer != null && !state.saving,
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
                    .semantics { contentDescription = "Collection amount" }
                    .testTag(NativeUiTags.CollectAmountInput),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onQuick1000,
                    enabled = customer != null && !state.saving,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(NativeUiTags.CollectQuick1000),
                ) { Text("PKR 1,000") }
                OutlinedButton(
                    onClick = onPayFull,
                    enabled = customer != null && customer.balancePaisa > 0L && !state.saving,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(NativeUiTags.CollectPayFull),
                ) { Text("Pay full") }
            }
        }

        FieldGroup("Received in") {
            SelectorSurface(
                title = selectedPaymentAccount?.name ?: "Choose Cash/Bank account",
                supporting = if (state.context.paymentAccounts.isEmpty()) {
                    "No active Cash/Bank account is available."
                } else {
                    "The received amount will be added here."
                },
                enabled = state.context.paymentAccounts.isNotEmpty() && !state.saving,
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onOpenPaymentPicker,
                tag = NativeUiTags.CollectPaymentSelector,
            )
        }

        TextButton(
            onClick = onToggleNotes,
            enabled = !state.saving,
            modifier = Modifier.heightIn(min = 48.dp).testTag(NativeUiTags.CollectNoteToggle),
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
                modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.CollectNoteInput),
            )
        }

        ReviewPanel(
            customer = customer,
            paymentAccount = selectedPaymentAccount,
            amountPaisa = amountPaisa,
            resultingBalancePaisa = resultingBalancePaisa,
        )

        if (advancePaisa > 0L) {
            StatusPanel(
                title = "This creates customer advance",
                message = "PKR ${formatMoney(advancePaisa)} will remain available on the customer account.",
                warning = true,
                tag = NativeUiTags.CollectReviewAdvance,
            )
        }

        state.error?.let {
            StatusPanel(
                title = "Payment not recorded",
                message = it,
                warning = true,
                tag = NativeUiTags.CollectError,
            )
        }

        val press = rememberPressSource()
        Button(
            onClick = onSubmit,
            enabled = validRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (advancePaisa > 0L) SaafYellow else MaterialTheme.colorScheme.primary,
                contentColor = if (advancePaisa > 0L) SaafInk else MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .pressScale(press)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(NativeUiTags.CollectRecord),
            interactionSource = press,
        ) {
            Text(
                when {
                    state.saving -> "Recording…"
                    advancePaisa > 0L -> "Review advance · PKR ${formatMoney(advancePaisa)}"
                    else -> "Record payment"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ReviewPanel(
    customer: NativeCollectionCustomerBalance?,
    paymentAccount: NativeCollectionPaymentAccount?,
    amountPaisa: Long?,
    resultingBalancePaisa: Long?,
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
                    customer == null -> "Choose a customer to review their updated balance."
                    resultingBalancePaisa == null -> "Enter the received amount to review the result."
                    resultingBalancePaisa > 0L ->
                        "${customer.customer.name} will owe PKR ${formatMoney(resultingBalancePaisa)}"
                    resultingBalancePaisa == 0L -> "${customer.customer.name} will be paid up"
                    else ->
                        "${customer.customer.name} will have PKR ${formatMoney(-resultingBalancePaisa)} advance"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            Text(
                if (paymentAccount != null && amountPaisa != null && amountPaisa > 0L) {
                    "${paymentAccount.name} increases by PKR ${formatMoney(amountPaisa)}"
                } else {
                    "Choose where the payment was received."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CollectionSuccess(
    receipt: NativeCollectionReceipt,
    onCollectAnother: () -> Unit,
    onViewRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(receipt.eventId) { context.hapticConfirm() }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(NativeUiTags.CollectSuccess),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text("Payment recorded", style = MaterialTheme.typography.headlineMedium)
        Text(
            "PKR ${formatMoney(receipt.amountPaisa)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(receipt.customer.name, style = MaterialTheme.typography.titleMedium)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReceiptRow("Customer balance", balanceLabel(receipt.customerBalanceAfterPaisa))
                HorizontalDivider()
                ReceiptRow("Received in", receipt.paymentAccount.name)
                HorizontalDivider()
                ReceiptRow("Record", "#${receipt.eventId}")
            }
        }
        Button(
            onClick = onCollectAnother,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(NativeUiTags.CollectAnother),
        ) { Text("Collect another") }
        OutlinedButton(
            onClick = onViewRecord,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(NativeUiTags.CollectViewRecord),
        ) { Text("View record") }
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
private fun CustomerPickerDialog(
    customers: List<NativeCollectionCustomer>,
    selecting: Boolean,
    onSelect: (NativeCollectionCustomer) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val matches = remember(customers, query) {
        customers.filter {
            it.name.contains(query.trim(), ignoreCase = true) ||
                (it.phone?.contains(query.trim(), ignoreCase = true) == true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(NativeUiTags.CollectCustomerDialog),
        title = { Text("Choose customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search name or phone") },
                    leadingIcon = { Icon(Icons.Outlined.PersonSearch, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag(NativeUiTags.CollectCustomerSearch),
                )
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                ) {
                    if (matches.isEmpty()) {
                        Text("No customers match that search.", Modifier.padding(vertical = 16.dp))
                    }
                    matches.forEach { customer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable(enabled = !selecting) { onSelect(customer) }
                                .testTag(NativeUiTags.collectCustomer(customer.accountId))
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.SemiBold)
                                customer.phone?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(NativeUiTags.CollectCustomerCancel),
            ) { Text("Cancel") }
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun PaymentPickerDialog(
    accounts: List<NativeCollectionPaymentAccount>,
    selectedId: Long?,
    onSelect: (NativeCollectionPaymentAccount) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Received in") },
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
                            .testTag(NativeUiTags.collectPaymentAccount(account.accountId))
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
                modifier = Modifier.testTag(NativeUiTags.CollectPaymentCancel),
            ) { Text("Cancel") }
        },
    )
}

@Composable
private fun AdvanceConfirmationDialog(
    customer: NativeCollectionCustomerBalance,
    amountPaisa: Long,
    advancePaisa: Long,
    saving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
        title = { Text("Confirm customer advance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${customer.customer.name} currently owes PKR ${formatMoney(customer.balancePaisa.coerceAtLeast(0L))}.")
                Text("You are recording PKR ${formatMoney(amountPaisa)}.")
                Text(
                    "PKR ${formatMoney(advancePaisa)} will remain as customer advance.",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            val press = rememberPressSource()
            Button(
                interactionSource = press,
                onClick = onConfirm,
                enabled = !saving,
                modifier = Modifier.pressScale(press).testTag(NativeUiTags.CollectConfirmAdvance),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
            ) { Text("Record with advance") }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.testTag(NativeUiTags.CollectCancelAdvance),
            ) { Text("Go back") }
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

private fun formatInputPaisa(paisa: Long): String = BigDecimal(paisa)
    .movePointLeft(2)
    .stripTrailingZeros()
    .toPlainString()

private fun formatMoney(paisa: Long): String = String.format(Locale.US, "%,.2f", paisa / 100.0)

private fun balanceLabel(balancePaisa: Long): String = when {
    balancePaisa > 0L -> "Owes PKR ${formatMoney(balancePaisa)}"
    balancePaisa < 0L -> "Has PKR ${formatMoney(-balancePaisa)} advance"
    else -> "Paid up"
}
