package com.saaf.milkshop.nativeui.manager

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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.saaf.milkshop.application.manager.NativeManagerWorkContext
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader

/**
 * Manager Work desk — Owner Work launcher density under Manager chrome.
 * Product IA excludes Pay salary, costing, shop settings, users, backup, reopen.
 * Sale / Collect / Ops stock entries are hosted via Manager*EntryScreen wrappers.
 */
@Composable
fun ManagerWorkScreen(
    snackbarHostState: SnackbarHostState,
    state: NativeManagerWorkState,
    onRetry: () -> Unit,
    onOpenDestination: (NativeManagerWorkDestination) -> Unit,
    onOpenClose: () -> Unit,
    onBackToHome: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        NativeManagerWorkState.Idle,
        NativeManagerWorkState.Loading,
        -> ManagerScaffold(snackbarHostState, ManagerSection.Work, onNavigate, modifier) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(24.dp)
                    .testTag(NativeUiTags.ManagerWorkLoading),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading today's work…")
            }
        }
        is NativeManagerWorkState.LoadFailure -> ManagerScaffold(
            snackbarHostState, ManagerSection.Work, onNavigate, modifier,
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
                    modifier = Modifier.testTag(NativeUiTags.ManagerWorkRetryLoad),
                ) { Text("Try again") }
            }
        }
        is NativeManagerWorkState.Ready -> ReadyWork(
            snackbarHostState = snackbarHostState,
            state = state,
            onOpenDestination = onOpenDestination,
            onOpenClose = onOpenClose,
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
    state: NativeManagerWorkState.Ready,
    onOpenDestination: (NativeManagerWorkDestination) -> Unit,
    onOpenClose: () -> Unit,
    onBackToHome: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier,
) {
    when (state.destination) {
        NativeManagerWorkDestination.Home -> {
            BackHandler(onBack = onLeaveConfirmed)
            ManagerScaffold(snackbarHostState, ManagerSection.Work, onNavigate, modifier) { padding ->
                Column(
                    Modifier
                        .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                        .padding(padding)
                        .testTag(NativeUiTags.ManagerWorkRoot),
                ) {
                    CashierScreenHeader(
                        title = "Today's work",
                        displayName = state.context.shopName,
                        roleLabel = "Manager",
                        businessDate = state.context.businessDate,
                        postingOpen = state.context.postingOpen,
                    )
                    WorkHomeBody(
                        context = state.context,
                        onOpenDestination = onOpenDestination,
                        onOpenClose = onOpenClose,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(NativeUiTags.ManagerWorkHome),
                    )
                }
            }
        }
        NativeManagerWorkDestination.RecordSale,
        NativeManagerWorkDestination.RecordPayment,
        NativeManagerWorkDestination.ReceiveMilk,
        NativeManagerWorkDestination.MakeYogurt,
        NativeManagerWorkDestination.RecordSpoilage,
        NativeManagerWorkDestination.Deliveries,
        NativeManagerWorkDestination.RecordExpense,
        NativeManagerWorkDestination.PaySupplier,
        -> {
            // Hosted by ManagerPreviewHost via Manager*EntryScreen wrappers.
            BackHandler(onBack = onBackToHome)
            ManagerScaffold(snackbarHostState, ManagerSection.Work, onNavigate, modifier) { padding ->
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
    context: NativeManagerWorkContext,
    onOpenDestination: (NativeManagerWorkDestination) -> Unit,
    onOpenClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Run today — exceptions and close readiness first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AttentionStrip(
            context = context,
            onOpenClose = onOpenClose,
            modifier = Modifier.testTag(NativeUiTags.ManagerWorkAttentionSection),
        )

        Text(
            text = "Daily shortcuts",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.ManagerWorkPrimaryActions),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction(
                    label = "Record sale",
                    icon = Icons.Outlined.PointOfSale,
                    tag = NativeUiTags.ManagerWorkRecordSale,
                    onClick = { onOpenDestination(NativeManagerWorkDestination.RecordSale) },
                )
                PrimaryAction(
                    label = "Record payment",
                    icon = Icons.Outlined.Payments,
                    tag = NativeUiTags.ManagerWorkRecordPayment,
                    onClick = { onOpenDestination(NativeManagerWorkDestination.RecordPayment) },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction(
                    label = "Receive milk",
                    icon = Icons.Outlined.WaterDrop,
                    tag = NativeUiTags.ManagerWorkReceiveMilk,
                    onClick = { onOpenDestination(NativeManagerWorkDestination.ReceiveMilk) },
                )
                PrimaryAction(
                    label = "Expense",
                    icon = Icons.Outlined.ReceiptLong,
                    tag = NativeUiTags.ManagerWorkRecordExpense,
                    onClick = { onOpenDestination(NativeManagerWorkDestination.RecordExpense) },
                )
            }
        }

        Text(
            text = "Make and move stock",
            style = MaterialTheme.typography.labelLarge,
        )
        Column(
            Modifier.testTag(NativeUiTags.ManagerWorkStockSection),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            CommandRow(
                label = "Make yogurt",
                detail = "Record Milk used and Yogurt produced",
                icon = Icons.Outlined.Science,
                tag = NativeUiTags.ManagerWorkMakeYogurt,
                onClick = { onOpenDestination(NativeManagerWorkDestination.MakeYogurt) },
            )
            CommandRow(
                label = "Record spoilage",
                detail = "Physical loss and reason",
                icon = Icons.Outlined.Assignment,
                tag = NativeUiTags.ManagerWorkRecordSpoilage,
                onClick = { onOpenDestination(NativeManagerWorkDestination.RecordSpoilage) },
            )
            CommandRow(
                label = "Deliveries",
                detail = if (context.activeDeliveryCount > 0) {
                    "${context.activeDeliveryCount} active — gaps need follow-up"
                } else {
                    "No active delivery gaps"
                },
                icon = Icons.Outlined.LocalShipping,
                tag = NativeUiTags.ManagerWorkDeliveries,
                badge = context.activeDeliveryCount.takeIf { it > 0 }?.toString(),
                onClick = { onOpenDestination(NativeManagerWorkDestination.Deliveries) },
            )
        }

        Text(
            text = "Money and close",
            style = MaterialTheme.typography.labelLarge,
        )
        Column(
            Modifier.testTag(NativeUiTags.ManagerWorkMoneySection),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            CommandRow(
                label = "Pay supplier",
                detail = "Supplier balance and payment",
                icon = Icons.Outlined.AccountBalance,
                tag = NativeUiTags.ManagerWorkPaySupplier,
                onClick = { onOpenDestination(NativeManagerWorkDestination.PaySupplier) },
            )
            CommandRow(
                label = "Cash count and close",
                detail = when {
                    !context.postingOpen -> "Day closed — reopen is Owner-only"
                    context.cashCountMissing -> "Count still needed before close"
                    else -> "Review expected cash, count, and variance"
                },
                icon = Icons.Outlined.Payments,
                tag = NativeUiTags.ManagerWorkCashClose,
                badge = when {
                    !context.postingOpen -> "Closed"
                    context.cashCountMissing -> "Count"
                    else -> "Open"
                },
                onClick = onOpenClose,
            )
        }
    }
}

@Composable
private fun AttentionStrip(
    context: NativeManagerWorkContext,
    onOpenClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = buildList {
        if (!context.postingOpen) {
            add("Today is closed. Reopen remains Owner-only.")
        } else if (context.cashCountMissing) {
            add("Cash count still needed before close.")
        }
        if (context.activeDeliveryCount > 0) {
            add("${context.activeDeliveryCount} active deliveries need attention.")
        }
    }
    Column(modifier) {
        if (items.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.ManagerWorkAllClear),
            ) {
                Text(
                    text = "All clear for now — use the shortcuts below for daily work.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.ManagerWorkAttention)
                    .clickable(onClick = onOpenClose),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null)
                        Text("Needs attention", style = MaterialTheme.typography.titleMedium)
                    }
                    items.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
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
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
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

private fun openingLabel(destination: NativeManagerWorkDestination): String = when (destination) {
    NativeManagerWorkDestination.RecordSale -> "Opening record sale…"
    NativeManagerWorkDestination.RecordPayment -> "Opening record payment…"
    NativeManagerWorkDestination.ReceiveMilk -> "Opening receive milk…"
    NativeManagerWorkDestination.MakeYogurt -> "Opening make yogurt…"
    NativeManagerWorkDestination.RecordSpoilage -> "Opening record spoilage…"
    NativeManagerWorkDestination.Deliveries -> "Opening deliveries…"
    NativeManagerWorkDestination.RecordExpense -> "Opening record expense…"
    NativeManagerWorkDestination.PaySupplier -> "Opening pay supplier…"
    NativeManagerWorkDestination.Home -> "Opening…"
}
