package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.NativeOwnerWorkContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader

@Composable
fun OwnerWorkScreen(
    snackbarHostState: SnackbarHostState,
    state: NativeOwnerWorkState,
    onRetry: () -> Unit,
    onOpenDestination: (NativeOwnerWorkDestination) -> Unit,
    onBackToHome: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeOwnerWorkState.Idle,
        NativeOwnerWorkState.Loading,
        -> OwnerScaffold(snackbarHostState, OwnerSection.Work, onNavigate, modifier) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(24.dp)
                    .testTag(NativeUiTags.OwnerWorkLoading),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading work…")
            }
        }
        is NativeOwnerWorkState.LoadFailure -> OwnerScaffold(
            snackbarHostState, OwnerSection.Work, onNavigate, modifier,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag(NativeUiTags.OwnerWorkRetryLoad),
                ) { Text("Try again") }
            }
        }
        is NativeOwnerWorkState.Ready -> ReadyWork(
            snackbarHostState = snackbarHostState,
            state = state,
            onOpenDestination = onOpenDestination,
            onBackToHome = onBackToHome,
            onLeaveConfirmed = onLeaveConfirmed,
            onNavigate = onNavigate,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReadyWork(
    snackbarHostState: SnackbarHostState,
    state: NativeOwnerWorkState.Ready,
    onOpenDestination: (NativeOwnerWorkDestination) -> Unit,
    onBackToHome: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    when (state.destination) {
        NativeOwnerWorkDestination.Home -> {
            BackHandler(onBack = onLeaveConfirmed)
            OwnerScaffold(snackbarHostState, OwnerSection.Work, onNavigate, modifier) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    CashierScreenHeader(
                        title = "Work",
                        displayName = state.context.shopName,
                        roleLabel = "Owner",
                        businessDate = state.context.businessDate,
                        postingOpen = state.context.postingOpen,
                    )
                    WorkHomeBody(
                        context = state.context,
                        onOpenDestination = onOpenDestination,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(NativeUiTags.OwnerWorkHome),
                    )
                }
            }
        }
        NativeOwnerWorkDestination.RecordSale,
        NativeOwnerWorkDestination.ReceiveMilk,
        NativeOwnerWorkDestination.RecordPayment,
        NativeOwnerWorkDestination.RecordExpense,
        NativeOwnerWorkDestination.PaySupplier,
        NativeOwnerWorkDestination.PaySalary,
        NativeOwnerWorkDestination.CashCountAndClose,
        NativeOwnerWorkDestination.MakeYogurt,
        NativeOwnerWorkDestination.RecordSpoilage,
        NativeOwnerWorkDestination.Deliveries,
        -> {
            // Hosted by NativePreviewActivity via existing entry screens.
            BackHandler(onBack = onBackToHome)
            OwnerScaffold(snackbarHostState, OwnerSection.Work, onNavigate, modifier) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(openingLabel(state.destination))
                }
            }
        }
    }
}

@Composable
private fun WorkHomeBody(
    context: NativeOwnerWorkContext,
    onOpenDestination: (NativeOwnerWorkDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "All daily commands remain reachable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!context.postingOpen) {
            Text(
                text = "This business date is closed. Reopen is not available in this Owner Work slice yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(NativeUiTags.OwnerWorkClosed),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.OwnerWorkPrimaryActions),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction(
                    label = "Record sale",
                    icon = Icons.Outlined.PointOfSale,
                    tag = NativeUiTags.OwnerWorkRecordSale,
                    onClick = { onOpenDestination(NativeOwnerWorkDestination.RecordSale) },
                )
                PrimaryAction(
                    label = "Record payment",
                    icon = Icons.Outlined.Payments,
                    tag = NativeUiTags.OwnerWorkRecordPayment,
                    onClick = { onOpenDestination(NativeOwnerWorkDestination.RecordPayment) },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction(
                    label = "Receive milk",
                    icon = Icons.Outlined.WaterDrop,
                    tag = NativeUiTags.OwnerWorkReceiveMilk,
                    onClick = { onOpenDestination(NativeOwnerWorkDestination.ReceiveMilk) },
                )
                PrimaryAction(
                    label = "Record expense",
                    icon = Icons.Outlined.ReceiptLong,
                    tag = NativeUiTags.OwnerWorkRecordExpense,
                    onClick = { onOpenDestination(NativeOwnerWorkDestination.RecordExpense) },
                )
            }
        }

        SectionTitle("Make and move stock")
        Column(
            Modifier.testTag(NativeUiTags.OwnerWorkStockSection),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            CommandRow(
                label = "Make yogurt",
                detail = "Record Milk used and Yogurt produced",
                icon = Icons.Outlined.Science,
                tag = NativeUiTags.OwnerWorkMakeYogurt,
                onClick = { onOpenDestination(NativeOwnerWorkDestination.MakeYogurt) },
            )
            CommandRow(
                label = "Record spoilage",
                detail = "Physical loss and reason",
                icon = Icons.Outlined.Assignment,
                tag = NativeUiTags.OwnerWorkRecordSpoilage,
                onClick = { onOpenDestination(NativeOwnerWorkDestination.RecordSpoilage) },
            )
            CommandRow(
                label = "Deliveries",
                detail = if (context.activeDeliveryCount > 0) {
                    "${context.activeDeliveryCount} active deliveries"
                } else {
                    "No active deliveries"
                },
                icon = Icons.Outlined.LocalShipping,
                tag = NativeUiTags.OwnerWorkDeliveries,
                badge = context.activeDeliveryCount.takeIf { it > 0 }?.toString(),
                onClick = { onOpenDestination(NativeOwnerWorkDestination.Deliveries) },
            )
        }

        SectionTitle("Money and close")
        Column(
            Modifier.testTag(NativeUiTags.OwnerWorkMoneySection),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            CommandRow(
                label = "Pay supplier",
                detail = "Supplier balance and payment",
                icon = Icons.Outlined.AccountBalance,
                tag = NativeUiTags.OwnerWorkPaySupplier,
                onClick = { onOpenDestination(NativeOwnerWorkDestination.PaySupplier) },
            )
            CommandRow(
                label = "Pay salary",
                detail = "Employee salary settlement",
                icon = Icons.Outlined.AccountBalanceWallet,
                tag = NativeUiTags.OwnerWorkPaySalary,
                onClick = { onOpenDestination(NativeOwnerWorkDestination.PaySalary) },
            )
            CommandRow(
                label = "Cash count and close",
                detail = "Review expected cash, count, and variance",
                icon = Icons.Outlined.Payments,
                tag = NativeUiTags.OwnerWorkCashClose,
                badge = if (context.postingOpen) "Open" else "Closed",
                onClick = { onOpenDestination(NativeOwnerWorkDestination.CashCountAndClose) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun PrimaryAction(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag(tag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(label)
        }
    }
}

@Composable
private fun CommandRow(
    label: String,
    detail: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    badge: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (badge != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag("${tag}_badge"),
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

private fun openingLabel(destination: NativeOwnerWorkDestination): String = when (destination) {
    NativeOwnerWorkDestination.RecordSale -> "Opening record sale…"
    NativeOwnerWorkDestination.RecordPayment -> "Opening record payment…"
    NativeOwnerWorkDestination.RecordExpense -> "Opening record expense…"
    NativeOwnerWorkDestination.PaySupplier -> "Opening pay supplier…"
    NativeOwnerWorkDestination.PaySalary -> "Opening pay salary…"
    NativeOwnerWorkDestination.CashCountAndClose -> "Opening cash count and close…"
    NativeOwnerWorkDestination.ReceiveMilk -> "Opening receive milk…"
    NativeOwnerWorkDestination.MakeYogurt -> "Opening make yogurt…"
    NativeOwnerWorkDestination.RecordSpoilage -> "Opening record spoilage…"
    NativeOwnerWorkDestination.Deliveries -> "Opening deliveries…"
    else -> "Opening…"
}
