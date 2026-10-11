package com.saaf.milkshop.nativeui.activity

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.activity.NativeActivityContext
import com.saaf.milkshop.application.activity.NativeActivityRecordDetail
import com.saaf.milkshop.application.activity.NativeActivityRecordSummary
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.sale.CashierNavigation
import com.saaf.milkshop.nativeui.sale.CashierNavigationRail
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import java.util.Locale

@Composable
fun CashierActivityScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeActivityState,
    onRetry: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeActivityState.Idle || it is NativeActivityState.Loading) NativeActivityState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeActivityState.Idle,
        NativeActivityState.Loading,
        -> ActivityLoadingScreen(snackbarHostState, onNavigate, modifier)

        is NativeActivityState.LoadFailure -> ActivityLoadFailureScreen(
            snackbarHostState = snackbarHostState,
            message = state.message,
            onRetry = onRetry,
            onNavigate = onNavigate,
            modifier = modifier,
        )

        is NativeActivityState.Ready -> {
            val detail = state.detail
            if (detail != null) {
                ActivityDetailScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    context = state.day.context,
                    detail = detail,
                    refreshing = state.refreshing,
                    onBack = onBackFromDetail,
                    onNavigate = onNavigate,
                    modifier = modifier,
                )
            } else {
                ActivityListScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = state,
                    onOpenRecord = onOpenRecord,
                    onLeaveConfirmed = onLeaveConfirmed,
                    onNavigate = onNavigate,
                    modifier = modifier,
                )
            }
        }
    }
    }
}

@Composable
private fun ActivityScaffold(
    snackbarHostState: SnackbarHostState,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
    rootTag: String,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    // The role host owns chrome; standalone previews retain primary navigation.
    com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        compactNavigation = { CashierNavigation(CashierSection.Activity, onNavigate) },
        expandedNavigation = { CashierNavigationRail(CashierSection.Activity, onNavigate) },
        modifier = modifier.fillMaxSize().testTag(rootTag),
        content = content,
    )
}

@Composable
private fun ActivityLoadingScreen(
    snackbarHostState: SnackbarHostState,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    ActivityScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.ActivityRoot) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            NativeColdLoadingPane(label = "Loading activity…", modifier = Modifier.testTag(NativeUiTags.ActivityLoading))
        }
    }
}

@Composable
private fun ActivityLoadFailureScreen(
    snackbarHostState: SnackbarHostState,
    message: String,
    onRetry: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    ActivityScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.ActivityRoot) { padding ->
        NativeLoadFailurePane(
            message = message,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize().padding(padding),
            retryTestTag = NativeUiTags.ActivityRetryLoad,
        )
    }
}

@Composable
private fun ActivityListScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeActivityState.Ready,
    onOpenRecord: (Long) -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onLeaveConfirmed)
    ActivityScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.ActivityRoot) { padding ->
        Column(
            Modifier
                .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                .padding(padding),
        ) {
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loadingDetail -> {
                        NativeColdLoadingPane(label = "Loading record…", modifier = Modifier.fillMaxSize())
                    }
                    state.day.records.isEmpty() -> {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .testTag(NativeUiTags.ActivityEmpty),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "No records yet today",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Sales and collections you record today will show up here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.ActivityList),
                        ) {
                            items(state.day.records, key = { it.eventId }) { record ->
                                ActivityRow(record = record, onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenRecord(record.eventId) })
                            }
                        }
                    }
                }
                state.error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .testTag(NativeUiTags.ActivityError),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeActivityContext,
    detail: NativeActivityRecordDetail,
    refreshing: Boolean,
    onBack: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    ActivityScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.ActivityDetail) { padding ->
        Column(
            Modifier
                .fillMaxSize().nativeRefreshOverlay(refreshing)
                .padding(padding),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    detail.summary.taskLabel,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "#${detail.summary.eventId}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(NativeUiTags.ActivityDetailEventId),
                )
                Text(
                    formatMoney(detail.summary.amountPaisa),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(detail.summary.timeLabel, style = MaterialTheme.typography.bodyLarge)
                Text(detail.summary.stateLabel, style = MaterialTheme.typography.bodyLarge)
                detail.counterpartLabel?.let {
                    LabeledValue("Customer", it)
                }
                detail.destinationLabel?.let {
                    LabeledValue("Received in", it)
                }
                detail.quantityLabel?.let {
                    LabeledValue("Quantity", it)
                }
                detail.notes?.let {
                    LabeledValue("Note", it)
                }
                Text(
                    "Corrections need a supervisor. This screen is inspect-only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(
    record: NativeActivityRecordSummary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Record)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Record)
            .testTag(NativeUiTags.activityRecord(record.eventId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.taskLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(record.timeLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("#${record.eventId} · ${record.stateLabel}", style = MaterialTheme.typography.bodyMedium)
            Text(formatMoney(record.amountPaisa), style = MaterialTheme.typography.titleMedium)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(Locale.US), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatMoney(paisa: Long): String =
    String.format(Locale.US, "PKR %,.2f", paisa / 100.0)
