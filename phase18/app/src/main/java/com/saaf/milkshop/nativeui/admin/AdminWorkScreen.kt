package com.saaf.milkshop.nativeui.admin

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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.saaf.milkshop.application.admin.NativeAdminWorkContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader

/**
 * Admin Work desk — setup / administration health first; daily Expense as secondary launcher.
 * Never mounts Owner Overview.
 */
@Composable
fun AdminWorkScreen(
    snackbarHostState: SnackbarHostState,
    state: NativeAdminWorkState,
    onRetry: () -> Unit,
    onOpenDestination: (NativeAdminWorkDestination) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenEnrollment: () -> Unit,
    onOpenProductRates: () -> Unit,
    onOpenEmployeeLinks: () -> Unit,
    onBackToHome: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (AdminSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeAdminWorkState.Idle || it is NativeAdminWorkState.Loading) NativeAdminWorkState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeAdminWorkState.Idle,
        NativeAdminWorkState.Loading,
        -> AdminScaffold(snackbarHostState, AdminSection.Work, onNavigate, modifier) {
            NativeColdLoadingPane(
                label = "Loading setup health…",
                modifier = Modifier
                    .padding(it)
                    .testTag(NativeUiTags.AdminWorkLoading),
            )
        }
        is NativeAdminWorkState.LoadFailure -> AdminScaffold(
            snackbarHostState, AdminSection.Work, onNavigate, modifier,
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
                    modifier = Modifier.testTag(NativeUiTags.AdminWorkRetryLoad),
                ) { Text("Try again") }
            }
        }
        is NativeAdminWorkState.Ready -> ReadyWork(
            snackbarHostState = snackbarHostState,
            state = state,
            onOpenDestination = onOpenDestination,
            onOpenPeople = onOpenPeople,
            onOpenEnrollment = onOpenEnrollment,
            onOpenProductRates = onOpenProductRates,
            onOpenEmployeeLinks = onOpenEmployeeLinks,
            onBackToHome = onBackToHome,
            onLeaveConfirmed = onLeaveConfirmed,
            onNavigate = onNavigate,
            modifier = modifier,
        )
    }
    }
}

@Composable
private fun ReadyWork(
    snackbarHostState: SnackbarHostState,
    state: NativeAdminWorkState.Ready,
    onOpenDestination: (NativeAdminWorkDestination) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenEnrollment: () -> Unit,
    onOpenProductRates: () -> Unit,
    onOpenEmployeeLinks: () -> Unit,
    onBackToHome: () -> Unit,
    onLeaveConfirmed: () -> Unit,
    onNavigate: (AdminSection) -> Unit,
    modifier: Modifier,
) {
    when (state.destination) {
        NativeAdminWorkDestination.Home -> {
            BackHandler(onBack = onLeaveConfirmed)
            AdminScaffold(snackbarHostState, AdminSection.Work, onNavigate, modifier) { padding ->
                Column(
                    Modifier
                        .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                        .padding(padding)
                        .testTag(NativeUiTags.AdminWorkRoot),
                ) {
                    CashierScreenHeader(
                        title = "Work desk",
                        displayName = state.context.shopName,
                        roleLabel = "Admin",
                        businessDate = state.context.businessDate,
                        postingOpen = state.context.postingOpen,
                    )
                    WorkHomeBody(
                        context = state.context,
                        onOpenDestination = onOpenDestination,
                        onOpenPeople = onOpenPeople,
                        onOpenEnrollment = onOpenEnrollment,
                        onOpenProductRates = onOpenProductRates,
                        onOpenEmployeeLinks = onOpenEmployeeLinks,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(NativeUiTags.AdminWorkHome),
                    )
                }
            }
        }
        NativeAdminWorkDestination.RecordExpense -> {
            BackHandler(onBack = onBackToHome)
            AdminScaffold(snackbarHostState, AdminSection.Work, onNavigate, modifier) { padding ->
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
                    Text("Opening record expense…")
                }
            }
        }
    }
}

@Composable
private fun WorkHomeBody(
    context: NativeAdminWorkContext,
    onOpenDestination: (NativeAdminWorkDestination) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenEnrollment: () -> Unit,
    onOpenProductRates: () -> Unit,
    onOpenEmployeeLinks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Keep the system healthy — setup gaps and blocked work first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AttentionStrip(context = context, onOpenPeople = onOpenPeople)

        Text(
            text = "Setup health",
            style = MaterialTheme.typography.labelLarge,
        )
        Column(
            Modifier.testTag(NativeUiTags.AdminWorkHealthSection),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            HealthRow(
                label = "Private enrollment",
                detail = if (context.setupRequiredCount > 0) {
                    "${context.setupRequiredCount} account(s) waiting for passphrase setup"
                } else {
                    "No accounts waiting for private enrollment"
                },
                icon = Icons.Outlined.PersonOff,
                tag = NativeUiTags.AdminWorkSetupRequired,
                badge = context.setupRequiredCount.takeIf { it > 0 }?.toString(),
                onClick = onOpenEnrollment,
            )
            HealthRow(
                label = "Products and rates",
                detail = when {
                    context.catalogEmpty -> "No active products — sales cannot price lines"
                    context.productsMissingRateCount > 0 ->
                        "${context.productsMissingRateCount} active product(s) missing a sale rate"
                    else -> "Active products have sale rates"
                },
                icon = Icons.Outlined.WarningAmber,
                tag = NativeUiTags.AdminWorkProductsRates,
                badge = when {
                    context.catalogEmpty -> "Empty"
                    context.productsMissingRateCount > 0 -> context.productsMissingRateCount.toString()
                    else -> null
                },
                onClick = onOpenProductRates,
                enabled = true,
            )
            HealthRow(
                label = "Staff employee links",
                detail = if (context.unlinkedStaffCount > 0) {
                    "${context.unlinkedStaffCount} staff account(s) not linked to an employee"
                } else {
                    "Staff accounts are linked to employees"
                },
                icon = Icons.Outlined.LinkOff,
                tag = NativeUiTags.AdminWorkEmployeeLinks,
                badge = context.unlinkedStaffCount.takeIf { it > 0 }?.toString(),
                onClick = onOpenEmployeeLinks,
            )
        }

        Text(
            text = "Daily shortcuts",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.AdminWorkPrimaryActions),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val press = rememberPressSource()
            OutlinedButton(
                onClick = { onOpenDestination(NativeAdminWorkDestination.RecordExpense) },
                modifier = Modifier
                    .pressScale(press)
                    .weight(1f)
                    .heightIn(min = 72.dp)
                    .testTag(NativeUiTags.AdminWorkRecordExpense),
                interactionSource = press,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null)
                    Text("Expense")
                }
            }
        }
    }
}

@Composable
private fun AttentionStrip(
    context: NativeAdminWorkContext,
    onOpenPeople: () -> Unit,
) {
    val items = buildList {
        if (context.setupRequiredCount > 0) {
            add("${context.setupRequiredCount} account(s) waiting for private enrollment.")
        }
        when {
            context.catalogEmpty -> add("Product catalog is empty — transactions cannot price.")
            context.productsMissingRateCount > 0 ->
                add("${context.productsMissingRateCount} product(s) missing a sale rate.")
        }
        if (context.unlinkedStaffCount > 0) {
            add("${context.unlinkedStaffCount} staff account(s) lack an employee link.")
        }
    }
    if (items.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.AdminWorkAllClear),
        ) {
            Text(
                text = "All clear for now — setup health looks good.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.AdminWorkAttention)
            .clickable(onClick = onOpenPeople),
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

@Composable
private fun HealthRow(
    label: String,
    detail: String,
    icon: ImageVector,
    tag: String,
    onClick: (() -> Unit)?,
    badge: String? = null,
    enabled: Boolean = true,
) {
    val clickable = enabled && onClick != null
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(tag)
            .then(
                if (clickable) Modifier.clickable(onClick = onClick!!)
                else Modifier,
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (clickable) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = if (clickable) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
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
        if (clickable) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}
