package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.application.owner.NativeOwnerOverviewSnapshot
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import java.util.Locale

@Composable
fun OwnerOverviewScreen(
    snackbarHostState: SnackbarHostState,
    state: NativeOwnerOverviewState,
    section: OwnerSection,
    onRetry: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onAttentionReview: () -> Unit = {},
    onOpenCashCount: () -> Unit = {},
    onOpenMonthReports: () -> Unit = {},
    ownScaffold: OwnScaffold,
    modifier: Modifier = Modifier,
) {
    when (section) {
        OwnerSection.Overview -> NativeSectionTransition(
            targetState = state,
            contentKey = { if (it is NativeOwnerOverviewState.Idle || it is NativeOwnerOverviewState.Loading) NativeOwnerOverviewState.Loading::class else it::class },
        ) { state -> when (state) {
            NativeOwnerOverviewState.Idle,
            NativeOwnerOverviewState.Loading,
            -> ownScaffold(snackbarHostState, modifier) {
                NativeColdLoadingPane(
                    label = "Loading overview…",
                    modifier = Modifier
                        .padding(it)
                        .testTag(NativeUiTags.OwnerOverviewLoading),
                )
            }
            is NativeOwnerOverviewState.LoadFailure -> ownScaffold(
                snackbarHostState,
                modifier,
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
                        modifier = Modifier.testTag(NativeUiTags.OwnerOverviewRetryLoad),
                    ) { Text("Try again") }
                }
            }
            is NativeOwnerOverviewState.Ready -> {
                BackHandler(onBack = onLeaveConfirmed)
                ownScaffold(snackbarHostState, modifier) { padding ->
                    Column(
                        Modifier
                            .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                            .padding(padding),
                    ) {
                        CashierScreenHeader(
                            title = "Overview",
                            displayName = state.snapshot.shopName,
                            roleLabel = "Owner",
                            businessDate = state.snapshot.businessDate,
                            postingOpen = state.snapshot.postingOpen,
                            onDayStatusClick = onOpenCashCount,
                        )
                        OverviewBody(
                            snapshot = state.snapshot,
                            onAttentionReview = onAttentionReview,
                            onOpenMonthReports = onOpenMonthReports,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(NativeUiTags.OwnerOverviewRoot),
                        )
                    }
                }
            }
        }
        }
        OwnerSection.Work -> ownScaffold(snackbarHostState, modifier) {
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
                Text("Opening work…")
            }
        }
        OwnerSection.More -> ownScaffold(snackbarHostState, modifier) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(24.dp)
                    .testTag(NativeUiTags.OwnerMoreLoading),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Opening more…")
            }
        }
        OwnerSection.Records -> ownScaffold(snackbarHostState, modifier) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(24.dp)
                    .testTag(NativeUiTags.OwnerRecordsLoading),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Opening records…")
            }
        }
    }
}

@Composable
private fun OverviewBody(
    snapshot: NativeOwnerOverviewSnapshot,
    onAttentionReview: () -> Unit,
    onOpenMonthReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Button(
            onClick = onOpenMonthReports,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.OwnerOverviewMonthReports),
        ) {
            Text("Month reports")
        }

        if (snapshot.attention.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OwnerOverviewAttention),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                snapshot.attention.forEach { item ->
                    Surface(
                        color = SaafYellow.copy(alpha = 0.35f),
                        contentColor = SaafInk,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(item.detail, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (item.actionLabel != null) {
                                TextButton(
                                    onClick = onAttentionReview,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerOverviewAttentionReview),
                                ) { Text(item.actionLabel) }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "No urgent issues",
                modifier = Modifier.testTag(NativeUiTags.OwnerOverviewClear),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Cash, balances, and backup checks that Overview tracks are clear.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.OwnerOverviewSales),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Today’s sales", style = MaterialTheme.typography.labelLarge)
            Text(
                "PKR ${formatMoney(snapshot.todaySalesPaisa)}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp,
                ),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (snapshot.coverageComplete) {
                        "Estimated gross result\nfor covered records"
                    } else {
                        "Estimated gross result\n(covered records only)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    snapshot.estimatedGrossPaisa?.let { "PKR ${formatMoney(it)}" } ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag(NativeUiTags.OwnerOverviewGross),
                )
            }
            if (snapshot.coverageWarning != null) {
                Text(
                    snapshot.coverageWarning,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(NativeUiTags.OwnerOverviewCoverageWarning),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Balances & outstanding", style = MaterialTheme.typography.labelLarge)
            BalanceRow("Expected cash", snapshot.expectedCashPaisa, NativeUiTags.OwnerOverviewExpectedCash)
            BalanceRow("Customers owe", snapshot.customersDuePaisa, NativeUiTags.OwnerOverviewCustomersDue)
            BalanceRow("Owed to suppliers", snapshot.suppliersDuePaisa, NativeUiTags.OwnerOverviewSuppliersDue)
        }

        Column(
            Modifier.testTag(NativeUiTags.OwnerOverviewStock),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Stock on hand", style = MaterialTheme.typography.labelLarge)
            if (snapshot.stock.isEmpty()) {
                Text("No stock positions yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    snapshot.stock.forEach { line ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(line.productName, style = MaterialTheme.typography.bodyMedium)
                                Text(line.quantityLabel, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }

        Column(
            Modifier.testTag(NativeUiTags.OwnerOverviewLatestActivity),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Latest activity", style = MaterialTheme.typography.labelLarge)
            val activity = snapshot.latestActivity
            if (activity == null) {
                Text(
                    "No records yet for this business.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(NativeUiTags.OwnerOverviewLatestEmpty),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(activity.title, style = MaterialTheme.typography.titleMedium)
                        Text(activity.subtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (activity.trailingLabel != null) {
                            Text(activity.trailingLabel, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(activity.stateLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(label: String, paisa: Long, tag: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("PKR ${formatMoney(paisa)}", style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatMoney(paisa: Long): String =
    String.format(Locale.US, "%,.2f", paisa / 100.0)
