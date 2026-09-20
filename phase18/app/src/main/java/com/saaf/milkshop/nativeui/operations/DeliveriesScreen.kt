package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun DeliveriesScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeDeliveriesState,
    onRetry: () -> Unit,
    onMarkDelivered: (Long) -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeDeliveriesState.Idle,
        NativeDeliveriesState.Loading,
        -> DeliveriesLoading(snackbarHostState, onNavigate, onLeaveConfirmed, modifier)

        is NativeDeliveriesState.LoadFailure -> DeliveriesLoadFailure(
            snackbarHostState = snackbarHostState,
            message = state.message,
            onRetry = onRetry,
            onNavigate = onNavigate,
            onLeaveConfirmed = onLeaveConfirmed,
            modifier = modifier,
        )

        is NativeDeliveriesState.Ready -> DeliveriesReady(
            snackbarHostState = snackbarHostState,
            identity = identity,
            state = state,
            onMarkDelivered = onMarkDelivered,
            onNavigate = onNavigate,
            onLeaveConfirmed = onLeaveConfirmed,
            modifier = modifier,
        )
    }
}

@Composable
private fun DeliveriesLoading(
    snackbarHostState: SnackbarHostState,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onLeaveConfirmed)
    WorkScaffold(snackbarHostState, OperationsSection.Deliveries, onNavigate, modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .padding(24.dp)
                .testTag(NativeUiTags.OpsDeliveriesLoading),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading deliveries…")
        }
    }
}

@Composable
private fun DeliveriesLoadFailure(
    snackbarHostState: SnackbarHostState,
    message: String,
    onRetry: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onLeaveConfirmed)
    WorkScaffold(snackbarHostState, OperationsSection.Deliveries, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag(NativeUiTags.OpsDeliveriesRetryLoad),
            ) { Text("Try again") }
        }
    }
}

@Composable
private fun DeliveriesReady(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeDeliveriesState.Ready,
    onMarkDelivered: (Long) -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onLeaveConfirmed)
    WorkScaffold(snackbarHostState, OperationsSection.Deliveries, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OpsDeliveriesRoot),
        ) {
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
                }
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
