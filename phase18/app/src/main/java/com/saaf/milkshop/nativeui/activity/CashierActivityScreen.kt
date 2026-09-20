package com.saaf.milkshop.nativeui.activity

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.saaf.milkshop.nativeui.sale.CashierNavigation
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    when (state) {
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

@Composable
private fun ActivityLoadingScreen(
    snackbarHostState: SnackbarHostState,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(NativeUiTags.ActivityRoot),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { CashierNavigation(selected = CashierSection.Activity, onNavigate = onNavigate) },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(Modifier.testTag(NativeUiTags.ActivityLoading))
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
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(NativeUiTags.ActivityRoot),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { CashierNavigation(selected = CashierSection.Activity, onNavigate = onNavigate) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag(NativeUiTags.ActivityRetryLoad),
            ) { Text("Try again") }
        }
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
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(NativeUiTags.ActivityRoot),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ActivityHeader(identity, state.day.context) },
        bottomBar = { CashierNavigation(selected = CashierSection.Activity, onNavigate = onNavigate) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loadingDetail -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
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
                            .testTag(NativeUiTags.ActivityList),
                    ) {
                        items(state.day.records, key = { it.eventId }) { record ->
                            ActivityRow(record = record, onClick = { onOpenRecord(record.eventId) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

@Composable
private fun ActivityDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeActivityContext,
    detail: NativeActivityRecordDetail,
    onBack: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(NativeUiTags.ActivityDetail),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ActivityHeader(identity, context) },
        bottomBar = { CashierNavigation(selected = CashierSection.Activity, onNavigate = onNavigate) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.testTag(NativeUiTags.ActivityDetailBack),
            ) { Text("Back to today") }
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

@Composable
private fun ActivityHeader(
    identity: NativeSignedInIdentity,
    context: NativeActivityContext,
) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Today’s activity", style = MaterialTheme.typography.headlineMedium)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(identity.displayName, style = MaterialTheme.typography.labelLarge)
                    Text(
                        identity.role.wireValue.replaceFirstChar { it.titlecase(Locale.US) } +
                            " · " + formatBusinessDate(context.businessDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    )
                }
                Surface(
                    color = if (context.postingOpen) SaafYellow else MaterialTheme.colorScheme.error,
                    contentColor = if (context.postingOpen) SaafInk else MaterialTheme.colorScheme.onError,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        if (context.postingOpen) "Open" else "Closed",
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
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
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(NativeUiTags.activityRecord(record.eventId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.taskLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatMoney(record.amountPaisa), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${record.timeLabel} · #${record.eventId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                record.stateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatMoney(paisa: Long): String {
    val amount = BigDecimalPaisa(paisa)
    return "PKR " + "%,.2f".format(Locale.US, amount)
}

private fun BigDecimalPaisa(paisa: Long): Double = paisa / 100.0

private fun formatBusinessDate(value: String): String = try {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))
} catch (_: Exception) {
    value
}
