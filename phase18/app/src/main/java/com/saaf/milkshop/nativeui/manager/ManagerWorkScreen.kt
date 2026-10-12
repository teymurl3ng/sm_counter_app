package com.saaf.milkshop.nativeui.manager

import com.saaf.milkshop.nativeui.theme.nativeGroupLabelTextStyle
import com.saaf.milkshop.nativeui.theme.nativeVisualColors
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeNavigationTitleTextStyle
import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.nativeListItem

import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.manager.NativeManagerWorkContext
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource

/** Injected shell for Manager panes; mirrors common/HostedScaffold minus the nav-action wrap. */
typealias MgrScaffold = @Composable (
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) -> Unit

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
    mgrScaffold: MgrScaffold,
    modifier: Modifier = Modifier,
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeManagerWorkState.Idle || it is NativeManagerWorkState.Loading) NativeManagerWorkState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeManagerWorkState.Idle,
        NativeManagerWorkState.Loading,
        -> mgrScaffold(snackbarHostState, modifier) {
            NativeColdLoadingPane(
                label = "Loading today's work…",
                modifier = Modifier
                    .padding(it)
                    .testTag(NativeUiTags.ManagerWorkLoading),
            )
        }
        is NativeManagerWorkState.LoadFailure -> mgrScaffold(
            snackbarHostState, modifier,
        ) { padding ->
            NativeLoadFailurePane(
                    message = state.message,
                    onRetry = onRetry,
                    retryTestTag = NativeUiTags.ManagerWorkRetryLoad,
                    modifier = Modifier.padding(padding),
                    )
        }
        is NativeManagerWorkState.Ready -> ReadyWork(
            snackbarHostState = snackbarHostState,
            state = state,
            onOpenDestination = onOpenDestination,
            onOpenClose = onOpenClose,
            onBackToHome = onBackToHome,
            onLeaveConfirmed = onLeaveConfirmed,
            mgrScaffold = mgrScaffold,
            modifier = modifier,
        )
    }
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
    mgrScaffold: MgrScaffold,
    modifier: Modifier,
) {
    when (state.destination) {
        NativeManagerWorkDestination.Home -> {
            BackHandler(onBack = onLeaveConfirmed)
            mgrScaffold(snackbarHostState, modifier) { padding ->
                Column(
                    Modifier
                        .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                        .padding(padding)
                        .testTag(NativeUiTags.ManagerWorkRoot),
                ) {
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
            mgrScaffold(snackbarHostState, modifier) { padding ->
                NativeColdLoadingPane(
                    label = openingLabel(state.destination),
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
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
            style = nativeGroupLabelTextStyle(),
            color = nativeVisualColors.supporting,
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
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.RecordSale) },
                )
                PrimaryAction(
                    label = "Record payment",
                    icon = Icons.Outlined.Payments,
                    tag = NativeUiTags.ManagerWorkRecordPayment,
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.RecordPayment) },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction(
                    label = "Receive milk",
                    icon = Icons.Outlined.WaterDrop,
                    tag = NativeUiTags.ManagerWorkReceiveMilk,
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.ReceiveMilk) },
                )
                PrimaryAction(
                    label = "Expense",
                    icon = Icons.Outlined.ReceiptLong,
                    tag = NativeUiTags.ManagerWorkRecordExpense,
                    onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.RecordExpense) },
                )
            }
        }

        Text(
            text = "Make and move stock",
            style = nativeGroupLabelTextStyle(),
            color = nativeVisualColors.supporting,
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
                onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.MakeYogurt) },
            )
            CommandRow(
                label = "Record spoilage",
                detail = "Physical loss and reason",
                icon = Icons.Outlined.Assignment,
                tag = NativeUiTags.ManagerWorkRecordSpoilage,
                onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.RecordSpoilage) },
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
                onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.Deliveries) },
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
                onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenDestination(NativeManagerWorkDestination.PaySupplier) },
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
                    .clickable(onClick = com.saaf.milkshop.nativeui.common.hapticAction(onOpenClose)),
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
    val context = LocalContext.current
    val press = rememberPressSource()
    OutlinedButton(
        onClick = { context.hapticTap(); onClick() },
        modifier = Modifier
            .pressScale(press)
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag(tag),
        interactionSource = press,
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
internal fun CommandRow(
    label: String,
    detail: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    badge: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Navigation)
            .testTag(tag)
            .clickable(onClick = com.saaf.milkshop.nativeui.common.hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Navigation),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = nativeNavigationTitleTextStyle())
            Text(
                detail,
                style = nativeSupportingTextStyle(),
                color = nativeVisualColors.supporting,
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
