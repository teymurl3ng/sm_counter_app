package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Owner Customers — customer list + read detail under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome.
 */
@Composable
fun OwnerCustomersScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    customers: List<NativeOwnerCustomerRow>,
    customersLoading: Boolean,
    customerDetail: NativeOwnerCustomerDetail?,
    customersError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onCreatePreview: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (customerDetail != null) {
        OwnerCustomersDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = customerDetail,
            onBack = onBackFromDetail,
            onNavigate = onNavigate,
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBackToMenu)
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCustomersRoot),
        ) {
            CashierScreenHeader(
                title = "Customers",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                customersLoading && customers.isEmpty() && customersError == null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerCustomersLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                customersError != null && customers.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            customersError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersRetry),
                        ) { Text("Try again") }
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCustomersBack),
                        ) { Text("Back to More") }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = onBackToMenu,
                                modifier = Modifier.testTag(NativeUiTags.OwnerCustomersBack),
                            ) { Text("Back to More") }
                            TextButton(
                                onClick = onCreatePreview,
                                modifier = Modifier.testTag(NativeUiTags.OwnerCustomersCreatePreview),
                            ) { Text("Add customer") }
                        }
                        customersError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerCustomersError),
                            )
                        }
                        if (customers.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .testTag(NativeUiTags.OwnerCustomersEmpty),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No customers yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .testTag(NativeUiTags.OwnerCustomersList),
                            ) {
                                items(customers, key = { it.accountId }) { customer ->
                                    OwnerCustomerRow(
                                        customer = customer,
                                        onClick = { onOpenCustomer(customer.accountId) },
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerCustomersDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerCustomerDetail,
    onBack: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val row = detail.row
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerCustomersDetail),
        ) {
            CashierScreenHeader(
                title = "Customers",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCustomersDetailBack),
                ) { Text("Back to customers") }
                Text(
                    row.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCustomersDetailName),
                )
                OwnerCustomerFact("Status", row.statusLabel)
                OwnerCustomerFact("Balance", row.balanceLabel)
                row.phone?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Phone", it) }
                row.areaRoute?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Area / route", it) }
                detail.address?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Address", it) }
                detail.creditLimitLabel?.let { OwnerCustomerFact("Credit limit", it) }
                detail.dailyQuotaLabel?.let { OwnerCustomerFact("Daily quota", it) }
                detail.notes?.takeIf { it.isNotBlank() }?.let { OwnerCustomerFact("Notes", it) }
                Text(
                    "Read only in this preview. Editing comes next.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCustomersDetailReadOnly),
                )
            }
        }
    }
}

@Composable
private fun OwnerCustomerRow(
    customer: NativeOwnerCustomerRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(NativeUiTags.ownerCustomer(customer.accountId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(customer.statusLabel)
                    append(" · ")
                    append(customer.balanceLabel)
                    customer.areaRoute?.takeIf { it.isNotBlank() }?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun OwnerCustomerFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
