package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeOperationsAttendanceStatus
import com.saaf.milkshop.application.operations.NativeOperationsWorkContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

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
    onLeaveConfirmed: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (section) {
        OperationsSection.Work -> when (state) {
            NativeOperationsWorkState.Idle,
            NativeOperationsWorkState.Loading,
            -> WorkScaffold(snackbarHostState, section, onNavigate, modifier) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(it)
                        .padding(24.dp)
                        .testTag(NativeUiTags.OpsWorkLoading),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading work…")
                }
            }
            is NativeOperationsWorkState.LoadFailure -> WorkScaffold(
                snackbarHostState, section, onNavigate, modifier,
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
                onLeaveConfirmed = onLeaveConfirmed,
                onNavigate = onNavigate,
                modifier = modifier,
            )
        }
        OperationsSection.Deliveries -> {
            // Hosted by NativePreviewActivity via DeliveriesScreen.
            WorkScaffold(snackbarHostState, section, onNavigate, modifier) { padding ->
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
                    Text("Opening deliveries…")
                }
            }
        }
        OperationsSection.Attendance -> {
            // Hosted by NativePreviewActivity via AttendanceScreen.
            WorkScaffold(snackbarHostState, section, onNavigate, modifier) { padding ->
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
                    Text("Opening attendance…")
                }
            }
        }
        OperationsSection.More -> {
            // Hosted by NativePreviewActivity via OperationsMoreScreen.
            WorkScaffold(snackbarHostState, section, onNavigate, modifier) { padding ->
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
                    Text("Opening More…")
                }
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
    onLeaveConfirmed: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.destination) {
        NativeOperationsWorkDestination.Home -> {
            BackHandler(onBack = onLeaveConfirmed)
            WorkScaffold(snackbarHostState, OperationsSection.Work, onNavigate, modifier) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    CashierScreenHeader(
                        title = "Work",
                        displayName = identity.displayName,
                        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                        businessDate = state.context.businessDate,
                        postingOpen = state.context.postingOpen,
                    )
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
            WorkScaffold(snackbarHostState, OperationsSection.Work, onNavigate, modifier) { padding ->
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
                    Text(
                        when (state.destination) {
                            NativeOperationsWorkDestination.MakeYogurt -> "Opening make yogurt…"
                            NativeOperationsWorkDestination.RecordSpoilage -> "Opening record spoilage…"
                            else -> "Opening receive milk…"
                        },
                    )
                }
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
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(tag),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun TaskPreview(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOperationsWorkContext,
    title: String,
    message: String,
    tag: String,
    onBack: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    WorkScaffold(snackbarHostState, OperationsSection.Work, onNavigate, modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CashierScreenHeader(
                title = title,
                displayName = identity.displayName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag(tag),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(NativeUiTags.OpsWorkBack),
                ) { Text("Back to Work") }
            }
        }
    }
}

@Composable
private fun PreviewDestination(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    section: OperationsSection,
    title: String,
    message: String,
    tag: String,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        if (section == OperationsSection.Work) onLeaveConfirmed()
        else onNavigate(OperationsSection.Work)
    }
    WorkScaffold(snackbarHostState, section, onNavigate, modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CashierScreenHeader(
                title = title,
                displayName = identity.displayName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = java.time.LocalDate.now().toString(),
                postingOpen = true,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag(tag),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
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
    Scaffold(
        modifier = modifier.testTag(NativeUiTags.OpsWorkRoot),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { OperationsNavigation(selected = section, onNavigate = onNavigate) },
        content = content,
    )
}

@Composable
internal fun OperationsNavigation(
    selected: OperationsSection,
    onNavigate: (OperationsSection) -> Unit,
) {
    NavigationBar {
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
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

private fun attendanceLabel(status: NativeOperationsAttendanceStatus): String = when (status) {
    NativeOperationsAttendanceStatus.Unlinked -> "Ask Owner to link this account to an employee"
    NativeOperationsAttendanceStatus.NotMarked -> "Not marked today"
    is NativeOperationsAttendanceStatus.Marked -> status.statusLabel
}
