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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsSnapshot
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Owner Shop settings — single scroll read panel under OwnerScaffold.
 * Nested from Owner More; never mounts foreign role chrome or edit write UI.
 */
@Composable
fun OwnerShopSettingsScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    snapshot: NativeOwnerShopSettingsSnapshot?,
    loading: Boolean,
    error: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onEditPreview: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackToMenu)
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerShopSettingsRoot),
        ) {
            CashierScreenHeader(
                title = "Shop settings",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                loading && snapshot == null && error == null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerShopSettingsLoading),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null && snapshot == null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsRetry),
                        ) { Text("Try again") }
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsBack),
                        ) { Text("Back to More") }
                    }
                }
                snapshot != null -> {
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
                                modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsBack),
                            ) { Text("Back to More") }
                            TextButton(
                                onClick = onEditPreview,
                                modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsEditPreview),
                            ) { Text("Edit settings") }
                        }
                        error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerShopSettingsError),
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag(NativeUiTags.OwnerShopSettingsContent),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                snapshot.shopName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OwnerShopSettingsFact("Phone", snapshot.phone ?: "Not set")
                            OwnerShopSettingsFact("Address", snapshot.address ?: "Not set")
                            OwnerShopSettingsFact("Currency", snapshot.currencyCode)

                            Text(
                                "Rates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsRatesSection),
                            )
                            OwnerShopSettingsFact("Vendor milk rate", snapshot.vendorMilkRateLabel)
                            OwnerShopSettingsFact("Vendor delivery fee", snapshot.vendorDeliveryFeeLabel)
                            OwnerShopSettingsFact("TS standard", snapshot.tsStandardLabel)
                            OwnerShopSettingsFact("Retail milk rate", snapshot.retailMilkRateLabel)
                            OwnerShopSettingsFact("Retail yogurt rate", snapshot.retailYogurtRateLabel)
                            OwnerShopSettingsFact("Default delivery surcharge", snapshot.defaultSurchargeLabel)

                            Text(
                                "Opening state",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsOpeningSection),
                            )
                            OwnerShopSettingsFact("Opening date", snapshot.openingDateLabel)
                            snapshot.openingNotes?.takeIf { it.isNotBlank() }?.let {
                                OwnerShopSettingsFact("Opening notes", it)
                            }

                            Text(
                                "Expense categories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag(NativeUiTags.OwnerShopSettingsCategoriesSection),
                            )
                            if (snapshot.expenseCategories.isEmpty()) {
                                Text(
                                    "No expense categories yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                snapshot.expenseCategories.forEach { category ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            category.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "#${category.accountId} · ${category.statusLabel}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }

                            Text(
                                "Read only in this preview. Editing shop settings comes next.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerShopSettingsFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
