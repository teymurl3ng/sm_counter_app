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
import com.saaf.milkshop.application.owner.NativeOwnerBackupSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Owner Backup — Drive vault status panel under OwnerScaffold.
 * Nested from Owner More; export / restore / connect are explicit preview only.
 */
@Composable
fun OwnerBackupScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    snapshot: NativeOwnerBackupSnapshot?,
    loading: Boolean,
    error: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onExportPreview: () -> Unit,
    onRestorePreview: () -> Unit,
    onConnectPreview: () -> Unit,
    onNavigate: (OwnerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackToMenu)
    OwnerScaffold(snackbarHostState, OwnerSection.More, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerBackupRoot),
        ) {
            CashierScreenHeader(
                title = "Backup & restore",
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
                            .testTag(NativeUiTags.OwnerBackupLoading),
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
                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupRetry),
                        ) { Text("Try again") }
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupBack),
                        ) { Text("Back to More") }
                    }
                }
                snapshot != null -> {
                    Column(Modifier.fillMaxSize()) {
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag(NativeUiTags.OwnerBackupBack),
                        ) { Text("Back to More") }
                        error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerBackupError),
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag(NativeUiTags.OwnerBackupContent),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                snapshot.statusHeadline,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag(NativeUiTags.OwnerBackupStatusHeadline),
                            )
                            OwnerBackupFact("Schedule", snapshot.scheduleDescription)
                            OwnerBackupFact(
                                "Configured",
                                if (snapshot.configured) "Yes" else "No",
                            )
                            OwnerBackupFact(
                                "Enabled",
                                if (snapshot.enabled) "Yes" else "No",
                            )
                            OwnerBackupFact(
                                "Account",
                                snapshot.accountDisplay ?: "Not linked",
                            )
                            OwnerBackupFact(
                                "Last success",
                                snapshot.lastSuccessAt ?: "Never",
                            )
                            OwnerBackupFact(
                                "Last attempt",
                                snapshot.lastAttemptAt ?: "Never",
                            )
                            snapshot.lastErrorMessage?.takeIf { it.isNotBlank() }?.let {
                                OwnerBackupFact("Last error", it)
                            }
                            OwnerBackupFact(
                                "Stale",
                                if (snapshot.stale) "Yes" else "No",
                            )
                            OwnerBackupFact(
                                "Pending upload",
                                if (snapshot.pending) "Yes" else "No",
                            )

                            Spacer(Modifier.height(4.dp))
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TextButton(
                                    onClick = onExportPreview,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerBackupExportPreview),
                                ) { Text("Export backup") }
                                TextButton(
                                    onClick = onRestorePreview,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerBackupRestorePreview),
                                ) { Text("Restore") }
                                TextButton(
                                    onClick = onConnectPreview,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerBackupConnectPreview),
                                ) { Text("Connect Drive") }
                            }
                            Text(
                                "Read only in this preview. Export, restore, and Drive connect come next.",
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
private fun OwnerBackupFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
