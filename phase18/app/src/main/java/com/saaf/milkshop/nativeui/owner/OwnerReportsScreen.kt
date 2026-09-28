package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerReportsCoverageKind
import com.saaf.milkshop.application.owner.NativeOwnerReportsPreset
import com.saaf.milkshop.application.owner.NativeOwnerReportsProductRow
import com.saaf.milkshop.application.owner.NativeOwnerReportsSnapshot
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow

/**
 * Owner Month reports — nested Overview destination under OwnerScaffold.
 * Read-only profitability panel; never mounts foreign role chrome.
 */
@Composable
fun OwnerReportsScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOwnerReportsState,
    onBackToOverview: () -> Unit,
    onRetry: () -> Unit,
    onSelectPreset: (NativeOwnerReportsPreset) -> Unit,
    onCustomRangePreview: () -> Unit,
    onSelectCustomRange: (String, String) -> Unit = { _, _ -> },
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var customRangeOpen by remember { mutableStateOf(false) }
    var customStart by remember { mutableStateOf("") }
    var customEnd by remember { mutableStateOf("") }
    BackHandler(onBack = onBackToOverview)
    OwnerScaffold(snackbarHostState, OwnerSection.Overview, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerReportsRoot),
        ) {
            when (state) {
                NativeOwnerReportsState.Idle,
                NativeOwnerReportsState.Loading,
                -> {
                    CashierScreenHeader(
                        title = "Month reports",
                        displayName = identity.displayName,
                        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                        businessDate = "",
                        postingOpen = true,
                        showDayChip = false,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerReportsLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is NativeOwnerReportsState.LoadFailure -> {
                    CashierScreenHeader(
                        title = "Month reports",
                        displayName = identity.displayName,
                        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                        businessDate = "",
                        postingOpen = true,
                        showDayChip = false,
                    )
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsRetry),
                        ) { Text("Try again") }
                        TextButton(
                            onClick = onBackToOverview,
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsBack),
                        ) { Text("Back to Overview") }
                    }
                }
                is NativeOwnerReportsState.Ready -> {
                    val snapshot = state.snapshot
                    CashierScreenHeader(
                        title = "Month reports",
                        displayName = snapshot.shopName,
                        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                        businessDate = snapshot.businessDate,
                        postingOpen = snapshot.postingOpen,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onBackToOverview,
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsBack),
                        ) { Text("Back to Overview") }
                        TextButton(
                            onClick = {
                                customStart = snapshot.startInclusive
                                customEnd = runCatching {
                                    java.time.LocalDate.parse(snapshot.endExclusive).minusDays(1).toString()
                                }.getOrDefault(snapshot.endExclusive)
                                customRangeOpen = true
                                onCustomRangePreview()
                            },
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsCustomPreview),
                        ) { Text("Custom range") }
                    }
                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .testTag(NativeUiTags.OwnerReportsError),
                        )
                    }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .nativeRefreshOverlay(state.refreshing)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag(NativeUiTags.OwnerReportsContent),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OwnerReportsPresets),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = snapshot.preset == NativeOwnerReportsPreset.ThisMonth,
                                onClick = { onSelectPreset(NativeOwnerReportsPreset.ThisMonth) },
                                label = { Text("This month") },
                                modifier = Modifier.testTag(NativeUiTags.OwnerReportsPresetThisMonth),
                            )
                            FilterChip(
                                selected = snapshot.preset == NativeOwnerReportsPreset.LastMonth,
                                onClick = { onSelectPreset(NativeOwnerReportsPreset.LastMonth) },
                                label = { Text("Last month") },
                                modifier = Modifier.testTag(NativeUiTags.OwnerReportsPresetLastMonth),
                            )
                            if (snapshot.preset == NativeOwnerReportsPreset.Custom) {
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        customStart = snapshot.startInclusive
                                        customEnd = java.time.LocalDate.parse(snapshot.endExclusive)
                                            .minusDays(1)
                                            .toString()
                                        customRangeOpen = true
                                    },
                                    label = { Text("Custom") },
                                )
                            }
                        }
                        Text(
                            snapshot.periodLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsPeriod),
                        )

                        CoverageBanner(snapshot)

                        Text(
                            "Period totals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsSummary),
                        )
                        OwnerReportsFact("Delivery surcharge", snapshot.deliverySurchargeLabel)
                        OwnerReportsFact("Operating expenses", snapshot.operatingExpensesLabel)
                        OwnerReportsFact("Salary payments", snapshot.salaryPaymentsLabel)
                        OwnerReportsFact(
                            "Estimated result",
                            snapshot.estimatedResultLabel ?: "Not available yet",
                        )
                        Text(
                            "The estimated result uses saved product costs. It is not bank balance " +
                                "or drawer cash.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Text(
                            "Result by product",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag(NativeUiTags.OwnerReportsProducts),
                        )
                        if (snapshot.products.isEmpty()) {
                            Text(
                                "No product sales were recorded in this period.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(NativeUiTags.OwnerReportsProductsEmpty),
                            )
                        } else {
                            snapshot.products.forEach { row ->
                                ProductRow(row)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    if (customRangeOpen) {
        AlertDialog(
            onDismissRequest = { customRangeOpen = false },
            title = { Text("Custom report range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Use calendar dates in YYYY-MM-DD format.")
                    OutlinedTextField(
                        value = customStart,
                        onValueChange = { customStart = it },
                        label = { Text("Start date") },
                        singleLine = true,
                        modifier = Modifier.testTag(NativeUiTags.OwnerReportsCustomStart),
                    )
                    OutlinedTextField(
                        value = customEnd,
                        onValueChange = { customEnd = it },
                        label = { Text("End date") },
                        singleLine = true,
                        modifier = Modifier.testTag(NativeUiTags.OwnerReportsCustomEnd),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customRangeOpen = false
                        onSelectCustomRange(customStart, customEnd)
                    },
                    modifier = Modifier.testTag(NativeUiTags.OwnerReportsCustomApply),
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { customRangeOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CoverageBanner(snapshot: NativeOwnerReportsSnapshot) {
    val complete = snapshot.coverageKind == NativeOwnerReportsCoverageKind.Complete
    Surface(
        color = if (complete) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            SaafYellow.copy(alpha = 0.35f)
        },
        contentColor = SaafInk,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OwnerReportsCoverage),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(snapshot.coverageLabel, style = MaterialTheme.typography.titleMedium)
            Text(snapshot.coverageDetail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProductRow(row: NativeOwnerReportsProductRow) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.ownerReportsProduct(row.productId)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                row.productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                row.coverageLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OwnerReportsFact("Sales", row.salesRevenueLabel)
        row.costLabel?.let { OwnerReportsFact("Cost of products sold", it) }
        row.profitLabel?.let { OwnerReportsFact("Sales minus product cost", it) }
    }
}

@Composable
private fun OwnerReportsFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
