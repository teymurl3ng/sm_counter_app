package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeDeliveryItem
import com.saaf.milkshop.application.operations.NativeDeliveryReference
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.hapticConfirm
import com.saaf.milkshop.nativeui.common.hapticCommit
import com.saaf.milkshop.nativeui.common.imePaddingExcludingNavBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue

/** Draft editing callbacks for the create-delivery form, bundled for host wiring. */
data class NativeDeliveriesDraftActions(
    val selectCustomer: (Long?) -> Unit,
    val selectProduct: (Long?) -> Unit,
    val setQuantity: (String) -> Unit,
    val setRoute: (String) -> Unit,
    val setNotes: (String) -> Unit,
    val create: () -> Unit,
)

@Composable
fun DeliveriesScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeDeliveriesState,
    onRetry: () -> Unit,
    onMarkDelivered: (Long) -> Unit,
    onLinkSale: (Long, String) -> Unit,
    draftActions: NativeDeliveriesDraftActions,
    onArchive: (Long) -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onLeaveConfirmed)
    WorkScaffold(
        snackbarHostState = snackbarHostState,
        section = OperationsSection.Deliveries,
        onNavigate = onNavigate,
        modifier = modifier,
    ) { padding ->
        NativeSectionTransition(
        targetState = state,
        contentKey = { it::class },
    ) { state -> when (state) {
            NativeDeliveriesState.Idle,
            NativeDeliveriesState.Loading,
            -> DeliveriesLoadingBody(
                modifier = Modifier.padding(padding),
            )

            is NativeDeliveriesState.LoadFailure -> DeliveriesLoadFailureBody(
                message = state.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )

            is NativeDeliveriesState.Ready -> DeliveriesReadyContent(
                identity = identity,
                state = state,
                onMarkDelivered = onMarkDelivered,
                onLinkSale = onLinkSale,
                draftActions = draftActions,
                onArchive = onArchive,
                contentPadding = padding,
            )
        }
        }
    }
}

@Composable
internal fun DeliveriesLoadingBody(
    modifier: Modifier = Modifier,
) {
    NativeColdLoadingPane(
        label = "Loading deliveries…",
        modifier = modifier.testTag(NativeUiTags.OpsDeliveriesLoading),
    )
}

@Composable
internal fun DeliveriesLoadFailureBody(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NativeLoadFailurePane(message = message, onRetry = onRetry, modifier = modifier)
}

@Composable
internal fun DeliveriesReadyContent(
    identity: NativeSignedInIdentity,
    state: NativeDeliveriesState.Ready,
    onMarkDelivered: (Long) -> Unit,
    onLinkSale: (Long, String) -> Unit,
    draftActions: NativeDeliveriesDraftActions,
    onArchive: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize().nativeRefreshOverlay(state.refreshing)
            .padding(contentPadding)
            .imePaddingExcludingNavBar()
            .testTag(NativeUiTags.OpsDeliveriesRoot),
    ) {
        val hapticContext = LocalContext.current
        LaunchedEffect(state.lastDeliveredId, state.lastLinkedId, state.lastCreatedId) {
            if (state.lastDeliveredId != null || state.lastLinkedId != null || state.lastCreatedId != null) {
                hapticContext.hapticConfirm()
            }
        }
        CashierScreenHeader(
            title = "Active deliveries",
            displayName = identity.displayName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = state.context.businessDate,
            postingOpen = state.context.postingOpen,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(NativeUiTags.OpsDeliveriesScroll),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.context.postingOpen) {
                Text(
                    "This business day is closed. Ask Owner to reopen it before marking delivered.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(NativeUiTags.OpsDeliveriesClosed),
                )
            }
            if (state.lastDeliveredId != null) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OpsDeliveriesSuccess),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Marked delivered (#${state.lastDeliveredId})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            if (state.lastLinkedId != null) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OpsDeliveriesSuccess),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sale linked (#${state.lastLinkedId})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            if (state.lastCreatedId != null) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OpsDeliveriesSuccess),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Delivery recorded (#${state.lastCreatedId})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            if (state.error != null) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(NativeUiTags.OpsDeliveriesError),
                )
            }
            NewDeliveryCard(
                state = state,
                draftActions = draftActions,
            )
            if (state.context.isEmpty) {
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OpsDeliveriesEmpty),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null)
                        Text(
                            "No active deliveries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "When pending deliveries exist, they appear here to mark delivered.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                if (state.context.today.isNotEmpty()) {
                    SectionLabel("Today")
                    state.context.today.forEach { item ->
                        DeliveryCard(
                            item = item,
                            marking = state.markingDeliveryId == item.deliveryId,
                            canMark = state.context.postingOpen && state.markingDeliveryId == null,
                            onMarkDelivered = onMarkDelivered,
                        )
                    }
                }
                if (state.context.earlier.isNotEmpty()) {
                    SectionLabel("Earlier")
                    state.context.earlier.forEach { item ->
                        DeliveryCard(
                            item = item,
                            marking = state.markingDeliveryId == item.deliveryId,
                            canMark = state.context.postingOpen && state.markingDeliveryId == null,
                            onMarkDelivered = onMarkDelivered,
                        )
                    }
                }
            if (state.context.linkable.isNotEmpty()) {
                SectionLabel("Link a recorded sale")
                state.context.linkable.forEach { item ->
                    LinkSaleCard(
                        item = item,
                        linking = state.linkingDeliveryId == item.deliveryId,
                        busy = state.linkingDeliveryId != null || state.markingDeliveryId != null,
                        onLinkSale = onLinkSale,
                        onArchive = onArchive,
                        archiving = state.archivingDeliveryId == item.deliveryId,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun NewDeliveryCard(
    state: NativeDeliveriesState.Ready,
    draftActions: NativeDeliveriesDraftActions,
) {
    val hapticContext = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pickingCustomer by rememberSaveable { mutableStateOf(false) }
    var pickingProduct by rememberSaveable { mutableStateOf(false) }
    val selectedCustomer = state.context.customers.firstOrNull { it.id == state.draftCustomerId }
    val selectedProduct = state.context.products.firstOrNull { it.id == state.draftProductId }
    val quantityValid = state.draftQuantity.toDoubleOrNull()?.let { it > 0 } == true
    val canCreate = selectedCustomer != null && selectedProduct != null && quantityValid && !state.creating

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsDeliveriesNewSection),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!expanded) {
                val press2 = rememberPressSource()
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .pressScale(press2)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(NativeUiTags.OpsDeliveryExpand),
                    interactionSource = press2,
                ) {
                    Text("＋ New delivery", fontWeight = FontWeight.SemiBold)
                }
            } else {
            Text("New delivery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = selectedCustomer?.label ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Customer") },
                placeholder = { Text("Choose a customer") },
                singleLine = true,
                enabled = !state.creating,
                trailingIcon = {
                    TextButton(
                        onClick = { pickingCustomer = true },
                        enabled = !state.creating,
                        modifier = Modifier.testTag(NativeUiTags.OpsDeliveryCustomerPick),
                    ) { Text("Pick") }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = selectedProduct?.label ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Product") },
                placeholder = { Text("Choose a product") },
                singleLine = true,
                enabled = !state.creating,
                trailingIcon = {
                    TextButton(
                        onClick = { pickingProduct = true },
                        enabled = !state.creating,
                        modifier = Modifier.testTag(NativeUiTags.OpsDeliveryProductPick),
                    ) { Text("Pick") }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.draftQuantity,
                onValueChange = draftActions.setQuantity,
                label = { Text("Quantity") },
                placeholder = { Text("e.g. 5") },
                singleLine = true,
                enabled = !state.creating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OpsDeliveryQuantity),
            )
            OutlinedTextField(
                value = state.draftRoute,
                onValueChange = draftActions.setRoute,
                label = { Text("Route (optional)") },
                singleLine = true,
                enabled = !state.creating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OpsDeliveryRoute),
            )
            OutlinedTextField(
                value = state.draftNotes,
                onValueChange = draftActions.setNotes,
                label = { Text("Notes (optional)") },
                enabled = !state.creating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OpsDeliveryNotes),
            )
            Button(
                onClick = { hapticContext.hapticCommit(); draftActions.create() },
                enabled = canCreate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(NativeUiTags.OpsDeliveryCreate),
            ) {
                Text(
                    if (state.creating) "Recording…" else "Record delivery",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            }
        }
    }

    if (pickingCustomer) {
        NativeDeliveryPickerDialog(
            title = "Choose customer",
            options = state.context.customers,
            selectedId = state.draftCustomerId,
            onSelect = {
                draftActions.selectCustomer(it)
                pickingCustomer = false
            },
            onDismiss = { pickingCustomer = false },
        )
    }
    if (pickingProduct) {
        NativeDeliveryPickerDialog(
            title = "Choose product",
            options = state.context.products,
            selectedId = state.draftProductId,
            onSelect = {
                draftActions.selectProduct(it)
                pickingProduct = false
            },
            onDismiss = { pickingProduct = false },
        )
    }
}

@Composable
private fun NativeDeliveryPickerDialog(
    title: String,
    options: List<NativeDeliveryReference>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                Modifier
                    .heightIn(max = 360.dp)
                    .testTag(NativeUiTags.OpsDeliveryPickerList),
            ) {
                if (options.isEmpty()) {
                    item { Text("Nothing available yet.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(options, key = { it.id }) { option ->
                    TextButton(
                        onClick = { onSelect(option.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.opsDeliveryPickerOption(option.id)),
                    ) {
                        Text(
                            option.label + if (option.id == selectedId) " ✓" else "",
                            fontWeight = if (option.id == selectedId) FontWeight.SemiBold else null,
                        )
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
private fun LinkSaleCard(
    item: NativeDeliveryItem,
    linking: Boolean,
    busy: Boolean,
    onLinkSale: (Long, String) -> Unit,
    onArchive: (Long) -> Unit,
    archiving: Boolean,
) {
    val hapticContext = LocalContext.current
    var saleNumber by rememberSaveable(item.deliveryId) { mutableStateOf("") }
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.opsDeliveryLinkSection(item.deliveryId)),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${item.productName} · ${item.quantityLabel}")
            Text(
                "Delivery #${item.deliveryId} · ${item.businessDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = saleNumber,
                onValueChange = { saleNumber = it.filter(Char::isDigit).dropWhile { zero -> zero == '0' }.take(15) },
                label = { Text("Sale record number") },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.opsDeliveryLinkInput(item.deliveryId)),
            )
            val press1 = rememberPressSource()
            Button(
                onClick = { hapticContext.hapticCommit(); onLinkSale(item.deliveryId, saleNumber) },
                enabled = !busy && saleNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
                modifier = Modifier
                    .pressScale(press1)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(NativeUiTags.opsDeliveryLink(item.deliveryId)),
                interactionSource = press1,
            ) {
                Text(
                    if (linking) "Linking…" else "Link",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // An escape hatch for any unlinked delivery — pending or already
            // delivered — so nothing is stuck with only "Link" as an option.
            OutlinedButton(
                onClick = { onArchive(item.deliveryId) },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(NativeUiTags.opsDeliveryArchive(item.deliveryId)),
            ) {
                Text(if (archiving) "Removing…" else "Remove from active list")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun DeliveryCard(
    item: NativeDeliveryItem,
    marking: Boolean,
    canMark: Boolean,
    onMarkDelivered: (Long) -> Unit,
) {
    val hapticContext = LocalContext.current
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.opsDelivery(item.deliveryId)),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${item.productName} · ${item.quantityLabel}")
            if (item.saleEventId != null) {
                Text(
                    "From sale #S-${item.saleEventId} — no linking needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.route != null) {
                Text(
                    "Route: ${item.route}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!item.isToday) {
                Text(
                    "Date: ${item.businessDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Pending",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            val press = rememberPressSource()
            Button(
                onClick = { hapticContext.hapticCommit(); onMarkDelivered(item.deliveryId) },
                enabled = canMark,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = SaafInk,
                ),
                modifier = Modifier
                    .pressScale(press)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(NativeUiTags.opsDeliveryMark(item.deliveryId)),
                interactionSource = press,
            ) {
                Text(
                    if (marking) "Marking…" else "Delivered",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
