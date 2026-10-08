package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeOperationsAttendanceStatus
import com.saaf.milkshop.application.operations.NativeOperationsWorkContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import androidx.compose.ui.platform.LocalContext
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.hapticCommit
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/** Injected shell for Operations panes; mirrors common/HostedScaffold minus the nav-action wrap. */
typealias OpsScaffold = @Composable (
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) -> Unit

@Composable
fun OperationsWorkScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOperationsWorkState,
    section: OperationsSection,
    onRetry: () -> Unit,
    onOpenReceiveMilk: () -> Unit,
    onOpenMakeYogurt: () -> Unit,
    onOpenRecordSpoilage: () -> Unit,
    onBackToHome: () -> Unit,
    workScaffold: OpsScaffold,
    modifier: Modifier = Modifier,
) {
    when (section) {
        OperationsSection.Work -> NativeSectionTransition(
            targetState = state,
            contentKey = { if (it is NativeOperationsWorkState.Idle || it is NativeOperationsWorkState.Loading) NativeOperationsWorkState.Loading::class else it::class },
        ) { state -> when (state) {
            NativeOperationsWorkState.Idle,
            NativeOperationsWorkState.Loading,
            -> workScaffold(snackbarHostState, modifier) {
                NativeColdLoadingPane(
                    label = "Loading work…",
                    modifier = Modifier
                        .padding(it)
                        .testTag(NativeUiTags.OpsWorkLoading),
                )
            }
            is NativeOperationsWorkState.LoadFailure -> workScaffold(
                snackbarHostState, modifier,
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
                        modifier = Modifier.testTag(NativeUiTags.OpsWorkRetryLoad),
                    ) { Text("Try again") }
                }
            }
            is NativeOperationsWorkState.Ready -> ReadyWork(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = state,
                onOpenReceiveMilk = onOpenReceiveMilk,
                onOpenMakeYogurt = onOpenMakeYogurt,
                onOpenRecordSpoilage = onOpenRecordSpoilage,
                onBackToHome = onBackToHome,
                workScaffold = workScaffold,
                modifier = modifier,
            )
        }
        }
        OperationsSection.Deliveries -> {
            // Hosted by NativePreviewActivity via DeliveriesScreen.
            workScaffold(snackbarHostState, modifier) { padding ->
                NativeColdLoadingPane(
                    label = "Opening deliveries…",
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
        OperationsSection.Attendance -> {
            // Hosted by NativePreviewActivity via AttendanceScreen.
            workScaffold(snackbarHostState, modifier) { padding ->
                NativeColdLoadingPane(
                    label = "Opening attendance…",
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
        OperationsSection.More -> {
            // Hosted by NativePreviewActivity via OperationsMoreScreen.
            workScaffold(snackbarHostState, modifier) { padding ->
                NativeColdLoadingPane(
                    label = "Opening More…",
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ReadyWork(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOperationsWorkState.Ready,
    onOpenReceiveMilk: () -> Unit,
    onOpenMakeYogurt: () -> Unit,
    onOpenRecordSpoilage: () -> Unit,
    onBackToHome: () -> Unit,
    workScaffold: OpsScaffold,
    modifier: Modifier = Modifier,
) {
    when (state.destination) {
        NativeOperationsWorkDestination.Home -> {
            // T17: back at the Work root is swallowed — the session stays.
            BackHandler { }
            workScaffold(snackbarHostState, modifier) { padding ->
                Column(
                    Modifier
                        .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                        .padding(padding),
                ) {
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    WorkHomeBody(
                        context = state.context,
                        onOpenReceiveMilk = onOpenReceiveMilk,
                        onOpenMakeYogurt = onOpenMakeYogurt,
                        onOpenRecordSpoilage = onOpenRecordSpoilage,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(NativeUiTags.OpsWorkHome),
                    )
                }
            }
        }
        NativeOperationsWorkDestination.ReceiveMilk,
        NativeOperationsWorkDestination.MakeYogurt,
        NativeOperationsWorkDestination.RecordSpoilage,
        -> {
            // Hosted by NativePreviewActivity via dedicated entry screens.
            BackHandler(onBack = onBackToHome)
            workScaffold(snackbarHostState, modifier) { padding ->
                NativeColdLoadingPane(
                    label = when (state.destination) {
                        NativeOperationsWorkDestination.MakeYogurt -> "Opening make yogurt…"
                        NativeOperationsWorkDestination.RecordSpoilage -> "Opening record spoilage…"
                        else -> "Opening receive milk…"
                    },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
    }
}

@Composable
private fun WorkHomeBody(
    context: NativeOperationsWorkContext,
    onOpenReceiveMilk: () -> Unit,
    onOpenMakeYogurt: () -> Unit,
    onOpenRecordSpoilage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Choose the next physical job. This screen shows stock and today’s work status only — no money.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.OpsWorkContext),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ContextRow("Milk on hand", context.milkOnHandLabel)
                ContextRow(
                    "Latest receipt today",
                    context.latestReceiptLabel ?: "None yet",
                )
                ContextRow(
                    "Latest production today",
                    context.latestProductionLabel ?: "None yet",
                )
                ContextRow(
                    "Active deliveries",
                    context.activeDeliveryCount.toString(),
                )
                ContextRow("Attendance", attendanceLabel(context.attendance))
            }
        }
        if (!context.postingOpen) {
            Text(
                text = "This business date is closed. Ask the Owner to reopen it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(NativeUiTags.OpsWorkClosed),
            )
        }
        TaskButton(
            label = "Receive milk",
            icon = Icons.Outlined.WaterDrop,
            tag = NativeUiTags.OpsReceiveMilk,
            onClick = onOpenReceiveMilk,
        )
        TaskButton(
            label = "Make yogurt",
            icon = Icons.Outlined.Science,
            tag = NativeUiTags.OpsMakeYogurt,
            onClick = onOpenMakeYogurt,
        )
        TaskButton(
            label = "Record spoilage",
            icon = Icons.Outlined.Assignment,
            tag = NativeUiTags.OpsRecordSpoilage,
            onClick = onOpenRecordSpoilage,
        )
    }
}

@Composable
private fun ContextRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TaskButton(
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
            .heightIn(min = 56.dp)
            .testTag(tag),
        interactionSource = press,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
internal fun WorkScaffold(
    snackbarHostState: SnackbarHostState,
    section: OperationsSection,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier.testTag(NativeUiTags.OpsWorkRoot),
        compactNavigation = { OperationsNavigation(selected = section, onNavigate = onNavigate) },
        expandedNavigation = { OperationsNavigationRail(selected = section, onNavigate = onNavigate) },
        content = content,
    )
}

@Composable
internal fun OperationsNavigationRail(
    selected: OperationsSection,
    onNavigate: (OperationsSection) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        OpsRailItem(selected == OperationsSection.Work, "Work", Icons.Outlined.WorkOutline, NativeUiTags.OpsNavWork) { onNavigate(OperationsSection.Work) }
        OpsRailItem(selected == OperationsSection.Deliveries, "Deliveries", Icons.Outlined.LocalShipping, NativeUiTags.OpsNavDeliveries) { onNavigate(OperationsSection.Deliveries) }
        OpsRailItem(selected == OperationsSection.Attendance, "Attendance", Icons.Outlined.EventAvailable, NativeUiTags.OpsNavAttendance) { onNavigate(OperationsSection.Attendance) }
        OpsRailItem(selected == OperationsSection.More, "More", Icons.Outlined.MoreHoriz, NativeUiTags.OpsNavMore) { onNavigate(OperationsSection.More) }
    }
}

@Composable
private fun OpsRailItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag).heightIn(min = 56.dp),
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}

@Composable
internal fun OperationsNavigation(
    selected: OperationsSection,
    onNavigate: (OperationsSection) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        OpsNavItem(
            selected = selected == OperationsSection.Work,
            label = "Work",
            icon = Icons.Outlined.WorkOutline,
            tag = NativeUiTags.OpsNavWork,
            onClick = { onNavigate(OperationsSection.Work) },
        )
        OpsNavItem(
            selected = selected == OperationsSection.Deliveries,
            label = "Deliveries",
            icon = Icons.Outlined.LocalShipping,
            tag = NativeUiTags.OpsNavDeliveries,
            onClick = { onNavigate(OperationsSection.Deliveries) },
        )
        OpsNavItem(
            selected = selected == OperationsSection.Attendance,
            label = "Attendance",
            icon = Icons.Outlined.EventAvailable,
            tag = NativeUiTags.OpsNavAttendance,
            onClick = { onNavigate(OperationsSection.Attendance) },
        )
        OpsNavItem(
            selected = selected == OperationsSection.More,
            label = "More",
            icon = Icons.Outlined.MoreHoriz,
            tag = NativeUiTags.OpsNavMore,
            onClick = { onNavigate(OperationsSection.More) },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.OpsNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    NavigationBarItem(
        selected = selected,
        onClick = { context.hapticTap(); onClick() },
        modifier = Modifier.testTag(tag).heightIn(min = 56.dp),
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

private fun attendanceLabel(status: NativeOperationsAttendanceStatus): String = when (status) {
    NativeOperationsAttendanceStatus.Unlinked -> "Ask Owner to link this account to an employee"
    NativeOperationsAttendanceStatus.NotMarked -> "Not marked today"
    is NativeOperationsAttendanceStatus.Marked -> status.statusLabel
}
