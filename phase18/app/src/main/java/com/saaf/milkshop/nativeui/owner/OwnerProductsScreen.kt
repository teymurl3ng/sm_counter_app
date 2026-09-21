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
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Owner Products — active catalog list + read detail + create under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome.
 */
@Composable
fun OwnerProductsScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    products: List<NativeOwnerProductRow>,
    productsLoading: Boolean,
    productDetail: NativeOwnerProductDetail?,
    productCreateOpen: Boolean,
    productCreating: Boolean,
    productsError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenCreate: () -> Unit,
    onBackFromCreate: () -> Unit,
    onCreateProduct: (ProductCatalogInput) -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (productCreateOpen) {
        OwnerProductsCreateScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            creating = productCreating,
            error = productsError,
            onBack = onBackFromCreate,
            onCreate = onCreateProduct,
            onNavigate = onNavigate,
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
            onBack = onBackFromDetail,
            onNavigate = onNavigate,
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBackToMenu)
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerProductsRoot),
        ) {
            CashierScreenHeader(
                title = "Products",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                productsLoading && products.isEmpty() && productsError == null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerProductsLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
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
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerProductsRetry),
                        ) { Text("Try again") }
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.testTag(NativeUiTags.OwnerProductsBack),
                        ) { Text("Back to More") }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = onBackToMenu,
                                modifier = Modifier.testTag(NativeUiTags.OwnerProductsBack),
                            ) { Text("Back to More") }
                            TextButton(
                                onClick = onOpenCreate,
                                modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateOpen),
                            ) { Text("Add product") }
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
                                    "No active products yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .testTag(NativeUiTags.OwnerProductsList),
                            ) {
                                items(products, key = { it.productId }) { product ->
                                    OwnerProductRow(
                                        product = product,
                                        onClick = { onOpenProduct(product.productId) },
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
private fun OwnerProductsCreateScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    creating: Boolean,
    error: String?,
    onBack: () -> Unit,
    onCreate: (ProductCatalogInput) -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("Liters") }
    var rateText by rememberSaveable { mutableStateOf("") }
    var inventoryTracking by rememberSaveable { mutableStateOf(true) }
    var notes by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val rateTrimmed = rateText.trim()
    val rateValue = rateTrimmed.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val rateValid = rateTrimmed.isEmpty() || (rateValue != null && rateValue > 0.0)
    val nameValid = name.trim().isNotEmpty() && !name.contains('\n')
    val canSave = nameValid && rateValid && unit in setOf("Liters", "Kg") && !creating
    val dirty = draftTouched && !creating

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (creating) return
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        OwnerScaffold(
            snackbarHostState = snackbarHostState,
            section = OwnerSection.More,
            onNavigate = { section -> requestLeave { onNavigate(section) } },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerProductsCreateForm),
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
                TextButton(
                    onClick = { requestLeave(onBack) },
                    enabled = !creating,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateBack),
                ) { Text("Back to products") }
                Text(
                    "Add product",
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
                    enabled = !creating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerProductsCreateName),
                )
                Text(
                    "Unit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = unit == "Liters",
                        onClick = {
                            unit = "Liters"
                            draftTouched = true
                        },
                        enabled = !creating,
                        label = { Text("Liters") },
                        modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateUnitLiters),
                    )
                    FilterChip(
                        selected = unit == "Kg",
                        onClick = {
                            unit = "Kg"
                            draftTouched = true
                        },
                        enabled = !creating,
                        label = { Text("Kg") },
                        modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateUnitKg),
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
                    enabled = !creating,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !rateValid,
                    supportingText = if (!rateValid) {
                        { Text("Rate must be greater than zero when set") }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerProductsCreateRate),
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
                        enabled = !creating,
                        modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateTracking),
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        draftTouched = true
                    },
                    label = { Text("Notes (optional)") },
                    enabled = !creating,
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerProductsCreateNotes),
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerProductsError),
                    )
                }
                Button(
                    onClick = {
                        if (!canSave) return@Button
                        onCreate(
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
                        .testTag(NativeUiTags.OwnerProductsCreateSave),
                ) {
                    if (creating) {
                        CircularProgressIndicator(
                            Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Save product")
                    }
                }
                TextButton(
                    onClick = { requestLeave(onBack) },
                    enabled = !creating,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateCancel),
                ) { Text("Cancel") }
                }
            }
        }
        OwnerProductsCreateDiscardDialog(leaveGuard)
    }
}

@Composable
private fun OwnerProductsCreateDiscardDialog(leaveGuard: EntryLeaveGuard) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text("Discard this product?") },
        text = { Text("The name, unit, rate, and notes you entered will be cleared.") },
        confirmButton = {
            TextButton(
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateDiscard),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(NativeUiTags.OwnerProductsCreateKeep),
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
    onBack: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val row = detail.row
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
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
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsDetailBack),
                ) { Text("Back to products") }
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
                Text(
                    "Read only in this preview. Editing comes next.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(NativeUiTags.OwnerProductsDetailReadOnly),
                )
            }
        }
    }
}

@Composable
private fun OwnerProductRow(
    product: NativeOwnerProductRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
