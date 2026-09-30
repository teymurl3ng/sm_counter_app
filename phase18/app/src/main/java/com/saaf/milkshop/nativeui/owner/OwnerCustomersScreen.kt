package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.util.Locale

/**
 * Owner Customers — customer list + detail + create/edit under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome.
 */
@Composable
fun OwnerCustomersScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    customers: List<NativeOwnerCustomerRow>,
    customersLoading: Boolean,
    customerDetail: NativeOwnerCustomerDetail?,
    customerCreateOpen: Boolean,
    customerCreating: Boolean,
    customerEditOpen: Boolean,
    customerUpdating: Boolean,
    customersError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateCustomer: (String) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateCustomer: (String, CustomerProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
) {
    if (customerCreateOpen) {
        OwnerCustomersCreateScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            saving = customerCreating,
            error = customersError,
            onBack = onBackFromCreate,
            onSave = onCreateCustomer,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (customerEditOpen && customerDetail != null) {
        OwnerCustomersEditScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = customerDetail,
            saving = customerUpdating,
            error = customersError,
            onBack = onBackFromEdit,
            onSave = onUpdateCustomer,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (customerDetail != null) {
        OwnerCustomersDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = customerDetail,
            error = customersError,
            onBack = onBackFromDetail,
            onOpenEdit = onOpenEdit,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBackToMenu)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCustomersRoot),
        ) {
            CashierScreenHeader(
                title = "Customers",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                customersLoading && customers.isEmpty() && customersError == null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerCustomersLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                customersError != null && customers.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            customersError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersRetry),
                        ) { Text("Try again") }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = onOpenCreate,
                                modifier = Modifier.testTag(NativeUiTags.OwnerCustomersCreateOpen),
                            ) { Text("Add customer") }
                        }
                        customersError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerCustomersError),
                            )
                        }
                        if (customers.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .testTag(NativeUiTags.OwnerCustomersEmpty),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No customers yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .testTag(NativeUiTags.OwnerCustomersList),
                            ) {
                                items(customers, key = { it.accountId }) { customer ->
                                    OwnerCustomerRow(
                                        customer = customer,
                                        onClick = { onOpenCustomer(customer.accountId) },
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerCustomersCreateScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var name by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val nameValid = name.trim().isNotEmpty() && !name.contains('\n')
    val canSave = nameValid && !saving
    val dirty = draftTouched && !saving

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        hostedScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
            { action -> requestLeave(action) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerCustomersCreateForm),
            ) {
                CashierScreenHeader(
                    title = "Customers",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Add customer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            draftTouched = true
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersCreateName),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                        )
                    }
                    Button(
                        onClick = {
                            if (!canSave) return@Button
                            onSave(name)
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersCreateSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save customer")
                        }
                    }
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCustomersCreateCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerCustomersDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this customer?",
            body = "The name you entered will be cleared.",
            discardTag = NativeUiTags.OwnerCustomersCreateDiscard,
            keepTag = NativeUiTags.OwnerCustomersCreateKeep,
        )
    }
}

@Composable
private fun OwnerCustomersEditScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerCustomerDetail,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String, CustomerProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val formKey = "edit-${detail.row.accountId}"
    var name by rememberSaveable(formKey) { mutableStateOf(detail.row.name) }
    var phone by rememberSaveable(formKey) { mutableStateOf(detail.row.phone.orEmpty()) }
    var address by rememberSaveable(formKey) { mutableStateOf(detail.address.orEmpty()) }
    var areaRoute by rememberSaveable(formKey) { mutableStateOf(detail.row.areaRoute.orEmpty()) }
    var notes by rememberSaveable(formKey) { mutableStateOf(detail.notes.orEmpty()) }
    var creditLimitText by rememberSaveable(formKey) {
        mutableStateOf(formatOptionalDoubleForEdit(detail.creditLimit))
    }
    var dailyQuotaText by rememberSaveable(formKey) {
        mutableStateOf(formatOptionalDoubleForEdit(detail.dailyQuota))
    }
    var active by rememberSaveable(formKey) { mutableStateOf(detail.active) }
    var draftTouched by rememberSaveable(formKey) { mutableStateOf(false) }

    val creditTrimmed = creditLimitText.trim()
    val creditValue = creditTrimmed.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val creditValid = creditTrimmed.isEmpty() || (creditValue != null && creditValue >= 0.0)
    val quotaTrimmed = dailyQuotaText.trim()
    val quotaValue = quotaTrimmed.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val quotaValid = quotaTrimmed.isEmpty() || (quotaValue != null && quotaValue >= 0.0)
    val nameValid = name.trim().isNotEmpty() && !name.contains('\n')
    val canSave = nameValid && creditValid && quotaValid && !saving
    val dirty = draftTouched && !saving

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        hostedScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
            { action -> requestLeave(action) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerCustomersEditForm),
            ) {
                CashierScreenHeader(
                    title = "Customers",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Edit customer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            draftTouched = true
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditName),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            draftTouched = true
                        },
                        label = { Text("Phone (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditPhone),
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            draftTouched = true
                        },
                        label = { Text("Address (optional)") },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditAddress),
                    )
                    OutlinedTextField(
                        value = areaRoute,
                        onValueChange = {
                            areaRoute = it
                            draftTouched = true
                        },
                        label = { Text("Area / route (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditAreaRoute),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            draftTouched = true
                        },
                        label = { Text("Notes (optional)") },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditNotes),
                    )
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = {
                            creditLimitText = it
                            draftTouched = true
                        },
                        label = { Text("Credit limit (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        isError = !creditValid,
                        supportingText = if (!creditValid) {
                            { Text("Enter a number ≥ 0, or leave blank") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditCreditLimit),
                    )
                    OutlinedTextField(
                        value = dailyQuotaText,
                        onValueChange = {
                            dailyQuotaText = it
                            draftTouched = true
                        },
                        label = { Text("Daily quota liters (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        isError = !quotaValid,
                        supportingText = if (!quotaValid) {
                            { Text("Enter a number ≥ 0, or leave blank") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditDailyQuota),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Active", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = active,
                            onCheckedChange = {
                                active = it
                                draftTouched = true
                            },
                            enabled = !saving,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersEditActive),
                        )
                    }
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                        )
                    }
                    Button(
                        onClick = {
                            if (!canSave) return@Button
                            onSave(
                                name,
                                CustomerProfileInput(
                                    phone = phone.trim().takeIf { it.isNotEmpty() },
                                    areaRoute = areaRoute.trim().takeIf { it.isNotEmpty() },
                                    address = address.trim().takeIf { it.isNotEmpty() },
                                    notes = notes.trim().takeIf { it.isNotEmpty() },
                                    creditLimit = creditValue,
                                    dailyQuota = quotaValue,
                                    active = active,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCustomersEditSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save changes")
                        }
                    }
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCustomersEditCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerCustomersDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard changes?",
            body = "Unsaved customer edits will be lost.",
            discardTag = NativeUiTags.OwnerCustomersEditDiscard,
            keepTag = NativeUiTags.OwnerCustomersEditKeep,
        )
    }
}

@Composable
private fun OwnerCustomersDiscardDialog(
    leaveGuard: EntryLeaveGuard,
    title: String,
    body: String,
    discardTag: String,
    keepTag: String,
) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(discardTag),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(keepTag),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
private fun OwnerCustomersDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerCustomerDetail,
    error: String?,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val row = detail.row
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCustomersDetail),
        ) {
            CashierScreenHeader(
                title = "Customers",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCustomersDetailName),
                )
                OwnerCustomerFact("Status", row.statusLabel)
                OwnerCustomerFact("Balance", row.balanceLabel)
                row.phone?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Phone", it) }
                row.areaRoute?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Area / route", it) }
                detail.address?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Address", it) }
                detail.creditLimitLabel?.let { OwnerCustomerFact("Credit limit", it) }
                detail.dailyQuotaLabel?.let { OwnerCustomerFact("Daily quota", it) }
                detail.notes?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Notes", it) }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                    )
                }
                Button(
                    onClick = onOpenEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCustomersDetailEdit),
                ) { Text("Edit customer") }
            }
        }
    }
}

@Composable
private fun OwnerCustomerRow(
    customer: NativeOwnerCustomerRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(NativeUiTags.ownerCustomer(customer.accountId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(customer.statusLabel)
                    append(" · ")
                    append(customer.balanceLabel)
                    customer.areaRoute?.takeIf { it.isNotBlank() }?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun OwnerCustomerFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatOptionalDoubleForEdit(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%s", value)
    }
}
