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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerProductDetail
import com.saaf.milkshop.application.owner.NativeOwnerProductRow
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.util.Locale

/**
 * Owner Products — active/archived catalog list + detail + create/edit/archive/unarchive under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome.
 */
private data class OwnerProductsScreenSnapshot(
    val identity: NativeSignedInIdentity,
    val context: NativeOwnerMoreContext,
    val products: List<NativeOwnerProductRow>,
    val productsLoading: Boolean,
    val productsShowArchived: Boolean,
    val productDetail: NativeOwnerProductDetail?,
    val productCreateOpen: Boolean,
    val productCreating: Boolean,
    val productEditOpen: Boolean,
    val productUpdating: Boolean,
    val productArchiving: Boolean,
    val productUnarchiving: Boolean,
    val productsError: String?,
    val showHeader: Boolean,
)

@Composable
fun OwnerProductsScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    products: List<NativeOwnerProductRow>,
    productsLoading: Boolean,
    productsShowArchived: Boolean,
    productDetail: NativeOwnerProductDetail?,
    productCreateOpen: Boolean,
    productCreating: Boolean,
    productEditOpen: Boolean,
    productUpdating: Boolean,
    productArchiving: Boolean,
    productUnarchiving: Boolean,
    productsError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onShowArchivedChange: (Boolean) -> Unit,
    onOpenProduct: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateProduct: (ProductCatalogInput) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateProduct: (ProductCatalogInput) -> Unit,
    onArchiveProduct: () -> Unit,
    onUnarchiveProduct: () -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val snapshot = OwnerProductsScreenSnapshot(
        identity = identity,
        context = context,
        products = products,
        productsLoading = productsLoading,
        productsShowArchived = productsShowArchived,
        productDetail = productDetail,
        productCreateOpen = productCreateOpen,
        productCreating = productCreating,
        productEditOpen = productEditOpen,
        productUpdating = productUpdating,
        productArchiving = productArchiving,
        productUnarchiving = productUnarchiving,
        productsError = productsError,
        showHeader = showHeader,
    )
    com.saaf.milkshop.nativeui.common.NativeSectionTransition(
        targetState = snapshot,
        contentKey = { pane -> when {
            pane.productCreateOpen -> "create"
            pane.productEditOpen -> "edit"
            pane.productDetail != null -> "detail:${pane.productDetail.row.productId}"
            else -> "list"
        } },
        modifier = modifier,
    ) { pane ->
        OwnerProductsScreenPane(
            snackbarHostState = snackbarHostState,
            identity = pane.identity,
            context = pane.context,
            products = pane.products,
            productsLoading = pane.productsLoading,
            productsShowArchived = pane.productsShowArchived,
            productDetail = pane.productDetail,
            productCreateOpen = pane.productCreateOpen,
            productCreating = pane.productCreating,
            productEditOpen = pane.productEditOpen,
            productUpdating = pane.productUpdating,
            productArchiving = pane.productArchiving,
            productUnarchiving = pane.productUnarchiving,
            productsError = pane.productsError,
            onBackToMenu = onBackToMenu,
            onRetry = onRetry,
            onShowArchivedChange = onShowArchivedChange,
            onOpenProduct = onOpenProduct,
            onBackFromDetail = onBackFromDetail,
            onOpenCreate = onOpenCreate,
            onBackFromCreate = onBackFromCreate,
            onCreateProduct = onCreateProduct,
            onOpenEdit = onOpenEdit,
            onBackFromEdit = onBackFromEdit,
            onUpdateProduct = onUpdateProduct,
            onArchiveProduct = onArchiveProduct,
            onUnarchiveProduct = onUnarchiveProduct,
            hostedScaffold = hostedScaffold,
            showHeader = pane.showHeader,
            modifier = modifier,
        )
    }
}

@Composable
private fun OwnerProductsScreenPane(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    products: List<NativeOwnerProductRow>,
    productsLoading: Boolean,
    productsShowArchived: Boolean,
    productDetail: NativeOwnerProductDetail?,
    productCreateOpen: Boolean,
    productCreating: Boolean,
    productEditOpen: Boolean,
    productUpdating: Boolean,
    productArchiving: Boolean,
    productUnarchiving: Boolean,
    productsError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onShowArchivedChange: (Boolean) -> Unit,
    onOpenProduct: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateProduct: (ProductCatalogInput) -> Unit,
    onOpenEdit: () -> Unit,
    onBackFromEdit: () -> Unit,
    onUpdateProduct: (ProductCatalogInput) -> Unit,
    onArchiveProduct: () -> Unit,
    onUnarchiveProduct: () -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (productCreateOpen) {
        OwnerProductsFormScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            mode = OwnerProductsFormMode.Create,
            initial = null,
            saving = productCreating,
            error = productsError,
            onBack = onBackFromCreate,
            onSave = onCreateProduct,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (productEditOpen && productDetail != null) {
        OwnerProductsFormScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            mode = OwnerProductsFormMode.Edit,
            initial = productDetail.row,
            saving = productUpdating,
            error = productsError,
            onBack = onBackFromEdit,
            onSave = onUpdateProduct,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    if (productDetail != null) {
        OwnerProductsDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = productDetail,
            archiving = productArchiving,
            unarchiving = productUnarchiving,
            error = productsError,
            onBack = onBackFromDetail,
            onOpenEdit = onOpenEdit,
            onArchive = onArchiveProduct,
            onUnarchive = onUnarchiveProduct,
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
                .testTag(NativeUiTags.OwnerProductsRoot),
        ) {
            if (showHeader) {
                CashierScreenHeader(
                    title = "Products",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
            }
            when {
                productsLoading && products.isEmpty() && productsError == null -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .testTag(NativeUiTags.OwnerProductsLoading),
                            contentAlignment = Alignment.Center,
                        ) {
                            NativeColdLoadingPane(label = "Loading products…")
                        }
                }
                productsError != null && products.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            productsError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerProductsError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = hapticAction(onRetry),
                            modifier = Modifier.testTag(NativeUiTags.OwnerProductsRetry),
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
                            if (!productsShowArchived) {
                                TextButton(
                                    onClick = hapticAction(onOpenCreate),
                                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateOpen),
                                ) { Text("Add product") }
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = !productsShowArchived,
                                onClick = com.saaf.milkshop.nativeui.common.hapticDestructiveAction { onShowArchivedChange(false) },
                                enabled = !productsLoading,
                                label = { Text("Active") },
                                modifier = Modifier.testTag(NativeUiTags.OwnerProductsFilterActive),
                            )
                            FilterChip(
                                selected = productsShowArchived,
                                onClick = com.saaf.milkshop.nativeui.common.hapticDestructiveAction { onShowArchivedChange(true) },
                                enabled = !productsLoading,
                                label = { Text("Archived") },
                                modifier = Modifier.testTag(NativeUiTags.OwnerProductsFilterArchived),
                            )
                        }
                        productsError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerProductsError),
                            )
                        }
                        if (products.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .testTag(NativeUiTags.OwnerProductsEmpty),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (productsShowArchived) {
                                        "No archived products."
                                    } else {
                                        "No active products yet."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerProductsList),
                            ) {
                                items(products, key = { it.productId }) { product ->
                                    OwnerProductRow(
                                        product = product,
                                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenProduct(product.productId) },
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

private enum class OwnerProductsFormMode { Create, Edit }

private data class OwnerProductsFormTags(
    val form: String,
    val name: String,
    val unitLiters: String,
    val unitKg: String,
    val rate: String,
    val tracking: String,
    val notes: String,
    val save: String,
    val cancel: String,
    val discard: String,
    val keep: String,
)

private fun OwnerProductsFormMode.tags(): OwnerProductsFormTags = when (this) {
    OwnerProductsFormMode.Create -> OwnerProductsFormTags(
        form = NativeUiTags.OwnerProductsCreateForm,
        name = NativeUiTags.OwnerProductsCreateName,
        unitLiters = NativeUiTags.OwnerProductsCreateUnitLiters,
        unitKg = NativeUiTags.OwnerProductsCreateUnitKg,
        rate = NativeUiTags.OwnerProductsCreateRate,
        tracking = NativeUiTags.OwnerProductsCreateTracking,
        notes = NativeUiTags.OwnerProductsCreateNotes,
        save = NativeUiTags.OwnerProductsCreateSave,
        cancel = NativeUiTags.OwnerProductsCreateCancel,
        discard = NativeUiTags.OwnerProductsCreateDiscard,
        keep = NativeUiTags.OwnerProductsCreateKeep,
    )
    OwnerProductsFormMode.Edit -> OwnerProductsFormTags(
        form = NativeUiTags.OwnerProductsEditForm,
        name = NativeUiTags.OwnerProductsEditName,
        unitLiters = NativeUiTags.OwnerProductsEditUnitLiters,
        unitKg = NativeUiTags.OwnerProductsEditUnitKg,
        rate = NativeUiTags.OwnerProductsEditRate,
        tracking = NativeUiTags.OwnerProductsEditTracking,
        notes = NativeUiTags.OwnerProductsEditNotes,
        save = NativeUiTags.OwnerProductsEditSave,
        cancel = NativeUiTags.OwnerProductsEditCancel,
        discard = NativeUiTags.OwnerProductsEditDiscard,
        keep = NativeUiTags.OwnerProductsEditKeep,
    )
}

@Composable
private fun OwnerProductsFormScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    mode: OwnerProductsFormMode,
    initial: NativeOwnerProductRow?,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (ProductCatalogInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val tags = mode.tags()
    val leaveGuard = rememberEntryLeaveGuard()
    val formKey = when (mode) {
        OwnerProductsFormMode.Create -> "create"
        OwnerProductsFormMode.Edit -> "edit-${initial?.productId ?: 0L}"
    }
    var name by rememberSaveable(formKey) { mutableStateOf(initial?.name.orEmpty()) }
    var unit by rememberSaveable(formKey) { mutableStateOf(initial?.unit ?: "Liters") }
    var rateText by rememberSaveable(formKey) {
        mutableStateOf(formatRateForEdit(initial?.defaultSaleRate))
    }
    var inventoryTracking by rememberSaveable(formKey) {
        mutableStateOf(initial?.inventoryTracking ?: true)
    }
    var notes by rememberSaveable(formKey) { mutableStateOf(initial?.notes.orEmpty()) }
    var draftTouched by rememberSaveable(formKey) { mutableStateOf(false) }

    val rateTrimmed = rateText.trim()
    val rateValue = rateTrimmed.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val rateValid = rateTrimmed.isEmpty() || (rateValue != null && rateValue > 0.0)
    val nameValid = name.trim().isNotEmpty() && !name.contains('\n')
    val canSave = nameValid && rateValid && unit in setOf("Liters", "Kg") && !saving
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
                    .testTag(tags.form),
            ) {
                CashierScreenHeader(
                    title = "Products",
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
                        when (mode) {
                            OwnerProductsFormMode.Create -> "Add product"
                            OwnerProductsFormMode.Edit -> "Edit product"
                        },
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
                            .testTag(tags.name),
                    )
                    Text(
                        "Unit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = unit == "Liters",
                            onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                                unit = "Liters"
                                draftTouched = true
                            },
                            enabled = !saving,
                            label = { Text("Liters") },
                            modifier = Modifier.testTag(tags.unitLiters),
                        )
                        FilterChip(
                            selected = unit == "Kg",
                            onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                                unit = "Kg"
                                draftTouched = true
                            },
                            enabled = !saving,
                            label = { Text("Kg") },
                            modifier = Modifier.testTag(tags.unitKg),
                        )
                    }
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = {
                            rateText = it
                            draftTouched = true
                        },
                        label = { Text("Default sale rate (optional)") },
                        singleLine = true,
                        enabled = !saving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !rateValid,
                        supportingText = if (!rateValid) {
                            { Text("Rate must be greater than zero when set") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tags.rate),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Track inventory",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Switch(
                            checked = inventoryTracking,
                            onCheckedChange = {
                                inventoryTracking = it
                                draftTouched = true
                            },
                            enabled = !saving,
                            modifier = Modifier.testTag(tags.tracking),
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            draftTouched = true
                        },
                        label = { Text("Notes (optional)") },
                        enabled = !saving,
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tags.notes),
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerProductsError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                            if (!canSave) return@hapticCommitAction
                            onSave(
                                ProductCatalogInput(
                                    name = name,
                                    unit = unit,
                                    defaultSaleRate = rateValue,
                                    inventoryTracking = inventoryTracking,
                                    notes = notes,
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tags.save),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                when (mode) {
                                    OwnerProductsFormMode.Create -> "Save product"
                                    OwnerProductsFormMode.Edit -> "Save changes"
                                },
                            )
                        }
                    }
                    TextButton(
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { requestLeave(onBack) },
                        enabled = !saving,
                        modifier = Modifier.testTag(tags.cancel),
                    ) { Text("Cancel") }
                }
            }
        }
        OwnerProductsFormDiscardDialog(
            leaveGuard = leaveGuard,
            mode = mode,
            discardTag = tags.discard,
            keepTag = tags.keep,
        )
    }
}

@Composable
private fun OwnerProductsFormDiscardDialog(
    leaveGuard: EntryLeaveGuard,
    mode: OwnerProductsFormMode,
    discardTag: String,
    keepTag: String,
) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = {
            Text(
                when (mode) {
                    OwnerProductsFormMode.Create -> "Discard this product?"
                    OwnerProductsFormMode.Edit -> "Discard changes?"
                },
            )
        },
        text = {
            Text(
                when (mode) {
                    OwnerProductsFormMode.Create ->
                        "The name, unit, rate, and notes you entered will be cleared."
                    OwnerProductsFormMode.Edit ->
                        "Unsaved edits to this product will be lost."
                },
            )
        },
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
private fun OwnerProductsDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerProductDetail,
    archiving: Boolean,
    unarchiving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    var archiveConfirmOpen by rememberSaveable { mutableStateOf(false) }
    var unarchiveConfirmOpen by rememberSaveable { mutableStateOf(false) }
    val busy = archiving || unarchiving
    BackHandler(enabled = !busy) {
        when {
            archiveConfirmOpen -> archiveConfirmOpen = false
            unarchiveConfirmOpen -> unarchiveConfirmOpen = false
            else -> onBack()
        }
    }
    val row = detail.row
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerProductsDetail),
        ) {
            CashierScreenHeader(
                title = "Products",
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
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsDetailName),
                )
                OwnerProductFact("Unit", row.unit)
                OwnerProductFact("Status", row.statusLabel)
                OwnerProductFact("Tracking", row.trackingLabel)
                OwnerProductFact("Default rate", row.rateLabel ?: "Not set")
                if (!row.notes.isNullOrBlank()) {
                    OwnerProductFact("Notes", row.notes)
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerProductsError),
                    )
                }
                if (row.archived) {
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { unarchiveConfirmOpen = true },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerProductsDetailUnarchive),
                    ) {
                        if (unarchiving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Restore product")
                        }
                    }
                } else {
                    Button(
                        onClick = hapticAction(onOpenEdit),
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerProductsDetailEdit),
                    ) { Text("Edit product") }
                    TextButton(
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { archiveConfirmOpen = true },
                        enabled = !busy,
                        modifier = Modifier.testTag(NativeUiTags.OwnerProductsDetailArchive),
                    ) {
                        if (archiving) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Archive product")
                        }
                    }
                }
            }
        }
    }
    if (archiveConfirmOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) archiveConfirmOpen = false
            },
            title = { Text("Archive this product?") },
            text = {
                Text(
                    "“${row.name}” will leave the active catalog and no longer appear in the product list.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = com.saaf.milkshop.nativeui.common.hapticDestructiveAction {
                        archiveConfirmOpen = false
                        onArchive()
                    },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsArchiveConfirm),
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction { archiveConfirmOpen = false },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsArchiveCancel),
                ) { Text("Cancel") }
            },
        )
    }
    if (unarchiveConfirmOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) unarchiveConfirmOpen = false
            },
            title = { Text("Restore this product?") },
            text = {
                Text(
                    "“${row.name}” will return to the active catalog.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                        unarchiveConfirmOpen = false
                        onUnarchive()
                    },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsUnarchiveConfirm),
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction { unarchiveConfirmOpen = false },
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsUnarchiveCancel),
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun OwnerProductRow(
    product: NativeOwnerProductRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Entity)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Entity)
            .testTag(NativeUiTags.ownerProduct(product.productId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(product.unit)
                    append(" · ")
                    append(product.statusLabel)
                    product.rateLabel?.let {
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
private fun OwnerProductFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatRateForEdit(rate: Double?): String {
    if (rate == null) return ""
    return if (rate % 1.0 == 0.0) {
        rate.toLong().toString()
    } else {
        String.format(Locale.US, "%s", rate)
    }
}
