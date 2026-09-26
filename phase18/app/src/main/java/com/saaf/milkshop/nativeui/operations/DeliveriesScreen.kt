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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeDeliveryItem
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun DeliveriesScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeDeliveriesState,
    onRetry: () -> Unit,
    onMarkDelivered: (Long) -> Unit,
    onLinkSale: (Long, String) -> Unit,
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
        when (state) {
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
                contentPadding = padding,
            )
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
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag(NativeUiTags.OpsDeliveriesRoot),
    ) {
        if (state.refreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
            if (state.error != null) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(NativeUiTags.OpsDeliveriesError),
                )
            }
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkSaleCard(
    item: NativeDeliveryItem,
    linking: Boolean,
    busy: Boolean,
    onLinkSale: (Long, String) -> Unit,
) {
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
                onValueChange = { saleNumber = it.filter(Char::isDigit).dropWhile { it == '0' }.take(15) },
                label = { Text("Sale record number") },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.opsDeliveryLinkInput(item.deliveryId)),
            )
            Button(
                onClick = { onLinkSale(item.deliveryId, saleNumber) },
                enabled = !busy && saleNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(NativeUiTags.opsDeliveryLink(item.deliveryId)),
            ) {
                Text(
                    if (linking) "Linking…" else "Link",
                    fontWeight = FontWeight.SemiBold,
                )
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
            Button(
                onClick = { onMarkDelivered(item.deliveryId) },
                enabled = canMark,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaafYellow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(NativeUiTags.opsDeliveryMark(item.deliveryId)),
            ) {
                Text(
                    if (marking) "Marking…" else "Delivered",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
