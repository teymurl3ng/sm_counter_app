package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
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
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
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
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerSupplierDetail
import com.saaf.milkshop.application.owner.NativeOwnerSupplierRow
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Owner Suppliers — vendor list + detail + create/edit under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome.
 */
private data class OwnerSuppliersScreenSnapshot(
    val identity: NativeSignedInIdentity,
    val context: NativeOwnerMoreContext,
    val suppliers: List<NativeOwnerSupplierRow>,
    val suppliersLoading: Boolean,
    val supplierDetail: NativeOwnerSupplierDetail?,
    val supplierCreateOpen: Boolean,
    val supplierCreating: Boolean,
    val supplierEditOpen: Boolean,
    val supplierUpdating: Boolean,
    val suppliersError: String?,
    val showHeader: Boolean,
)

@Composable
fun OwnerSuppliersScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    suppliers: List<NativeOwnerSupplierRow>,
    suppliersLoading: Boolean,
    supplierDetail: NativeOwnerSupplierDetail?,
    supplierCreateOpen: Boolean,
    supplierCreating: Boolean,
    supplierEditOpen: Boolean,
    supplierUpdating: Boolean,
    suppliersError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenSupplier: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateSupplier: (String) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateSupplier: (String, VendorProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val snapshot = OwnerSuppliersScreenSnapshot(
        identity = identity,
        context = context,
        suppliers = suppliers,
        suppliersLoading = suppliersLoading,
        supplierDetail = supplierDetail,
        supplierCreateOpen = supplierCreateOpen,
        supplierCreating = supplierCreating,
        supplierEditOpen = supplierEditOpen,
        supplierUpdating = supplierUpdating,
        suppliersError = suppliersError,
        showHeader = showHeader,
    )
    com.saaf.milkshop.nativeui.common.NativeSectionTransition(
        targetState = snapshot,
        contentKey = { pane -> when {
            pane.supplierCreateOpen -> "create"
            pane.supplierEditOpen -> "edit"
            pane.supplierDetail != null -> "detail:${pane.supplierDetail.row.accountId}"
            else -> "list"
        } },
        modifier = modifier,
    ) { pane ->
        OwnerSuppliersScreenPane(
            snackbarHostState = snackbarHostState,
            identity = pane.identity,
            context = pane.context,
            suppliers = pane.suppliers,
            suppliersLoading = pane.suppliersLoading,
            supplierDetail = pane.supplierDetail,
            supplierCreateOpen = pane.supplierCreateOpen,
            supplierCreating = pane.supplierCreating,
            supplierEditOpen = pane.supplierEditOpen,
            supplierUpdating = pane.supplierUpdating,
            suppliersError = pane.suppliersError,
            onBackToMenu = onBackToMenu,
            onRetry = onRetry,
            onOpenSupplier = onOpenSupplier,
            onBackFromDetail = onBackFromDetail,
            onOpenCreate = onOpenCreate,
            onBackFromCreate = onBackFromCreate,
            onCreateSupplier = onCreateSupplier,
            onOpenEdit = onOpenEdit,
            onBackFromEdit = onBackFromEdit,
            onUpdateSupplier = onUpdateSupplier,
            hostedScaffold = hostedScaffold,
            showHeader = pane.showHeader,
            modifier = modifier,
        )
    }
}

@Composable
private fun OwnerSuppliersScreenPane(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    suppliers: List<NativeOwnerSupplierRow>,
    suppliersLoading: Boolean,
    supplierDetail: NativeOwnerSupplierDetail?,
    supplierCreateOpen: Boolean,
    supplierCreating: Boolean,
    supplierEditOpen: Boolean,
    supplierUpdating: Boolean,
    suppliersError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenSupplier: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateSupplier: (String) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateSupplier: (String, VendorProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (supplierCreateOpen) {
        OwnerSuppliersCreateScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            saving = supplierCreating,
            error = suppliersError,
            onBack = onBackFromCreate,
            onSave = onCreateSupplier,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (supplierEditOpen && supplierDetail != null) {
        OwnerSuppliersEditScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = supplierDetail,
            saving = supplierUpdating,
            error = suppliersError,
            onBack = onBackFromEdit,
            onSave = onUpdateSupplier,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (supplierDetail != null) {
        OwnerSuppliersDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = supplierDetail,
            error = suppliersError,
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
                .testTag(NativeUiTags.OwnerSuppliersRoot),
        ) {
            if (showHeader) {
                CashierScreenHeader(
                    title = "Suppliers",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
            }
            when {
                suppliersLoading && suppliers.isEmpty() && suppliersError == null -> {
                        NativeColdLoadingPane(
                            label = "Loading suppliers…",
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(NativeUiTags.OwnerSuppliersLoading),
                        )
                }
                suppliersError != null && suppliers.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            suppliersError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = hapticAction(onRetry),
                            modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersRetry),
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
                                onClick = hapticAction(onOpenCreate),
                                modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersCreateOpen),
                            ) { Text("Add supplier") }
                        }
                        suppliersError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerSuppliersError),
                            )
                        }
                        if (suppliers.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .testTag(NativeUiTags.OwnerSuppliersEmpty),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No suppliers yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerSuppliersList),
                            ) {
                                items(suppliers, key = { it.accountId }) { supplier ->
                                    OwnerSupplierRow(
                                        supplier = supplier,
                                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenSupplier(supplier.accountId) },
                                    )
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
private fun OwnerSuppliersCreateScreen(
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
                    .testTag(NativeUiTags.OwnerSuppliersCreateForm),
            ) {
                CashierScreenHeader(
                    title = "Suppliers",
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
                        "Add supplier",
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
                            .testTag(NativeUiTags.OwnerSuppliersCreateName),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                            if (!canSave) return@hapticCommitAction
                            onSave(name)
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerSuppliersCreateSave),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Save supplier")
                        }
                    }
                    TextButton(
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersCreateCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerSuppliersDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this supplier?",
            body = "The name you entered will be cleared.",
            discardTag = NativeUiTags.OwnerSuppliersCreateDiscard,
            keepTag = NativeUiTags.OwnerSuppliersCreateKeep,
        )
    }
}

@Composable
private fun OwnerSuppliersEditScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerSupplierDetail,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String, VendorProfileInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val formKey = "edit-${detail.row.accountId}"
    var name by rememberSaveable(formKey) { mutableStateOf(detail.row.name) }
    var phone by rememberSaveable(formKey) { mutableStateOf(detail.row.phone.orEmpty()) }
    var location by rememberSaveable(formKey) { mutableStateOf(detail.row.location.orEmpty()) }
    var notes by rememberSaveable(formKey) { mutableStateOf(detail.notes.orEmpty()) }
    var active by rememberSaveable(formKey) { mutableStateOf(detail.active) }
    var draftTouched by rememberSaveable(formKey) { mutableStateOf(false) }

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
                    .testTag(NativeUiTags.OwnerSuppliersEditForm),
            ) {
                CashierScreenHeader(
                    title = "Suppliers",
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
                        "Edit supplier",
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
                            .testTag(NativeUiTags.OwnerSuppliersEditName),
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
                            .testTag(NativeUiTags.OwnerSuppliersEditPhone),
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            draftTouched = true
                        },
                        label = { Text("Location (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerSuppliersEditLocation),
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
                            .testTag(NativeUiTags.OwnerSuppliersEditNotes),
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
                            modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersEditActive),
                        )
                    }
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                            if (!canSave) return@hapticCommitAction
                            onSave(
                                name,
                                VendorProfileInput(
                                    phone = phone.trim().takeIf { it.isNotEmpty() },
                                    location = location.trim().takeIf { it.isNotEmpty() },
                                    notes = notes.trim().takeIf { it.isNotEmpty() },
                                    active = active,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerSuppliersEditSave),
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
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersEditCancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerSuppliersDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard changes?",
            body = "Unsaved supplier edits will be lost.",
            discardTag = NativeUiTags.OwnerSuppliersEditDiscard,
            keepTag = NativeUiTags.OwnerSuppliersEditKeep,
        )
    }
}

@Composable
private fun OwnerSuppliersDiscardDialog(
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
                onClick = hapticAction(leaveGuard::confirmDiscard),
                modifier = Modifier.testTag(discardTag),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = hapticAction(leaveGuard::keepEditing),
                modifier = Modifier.testTag(keepTag),
            ) { Text("Keep editing") }
        },
    )
}

@Composable
private fun OwnerSuppliersDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerSupplierDetail,
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
                .testTag(NativeUiTags.OwnerSuppliersDetail),
        ) {
            CashierScreenHeader(
                title = "Suppliers",
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
                    modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersDetailName),
                )
                OwnerSupplierFact("Status", row.statusLabel)
                OwnerSupplierFact("Balance", row.balanceLabel)
                row.phone?.takeIf { it.isNotBlank() }?.let { OwnerSupplierFact("Phone", it) }
                row.location?.takeIf { it.isNotBlank() }?.let { OwnerSupplierFact("Location", it) }
                detail.notes?.takeIf { it.isNotBlank() }?.let { OwnerSupplierFact("Notes", it) }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerSuppliersError),
                    )
                }
                Button(
                    onClick = hapticAction(onOpenEdit),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerSuppliersDetailEdit),
                ) { Text("Edit supplier") }
            }
        }
    }
}

@Composable
private fun OwnerSupplierRow(
    supplier: NativeOwnerSupplierRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Entity)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Entity)
            .testTag(NativeUiTags.ownerSupplier(supplier.accountId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                supplier.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(supplier.statusLabel)
                    append(" · ")
                    append(supplier.balanceLabel)
                    supplier.location?.takeIf { it.isNotBlank() }?.let {
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
private fun OwnerSupplierFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
