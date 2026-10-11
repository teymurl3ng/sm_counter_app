package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditDetail
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditRow
import com.saaf.milkshop.application.AppTimeFormats
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Security history content — audit list + read detail under HostedScaffold
 * (Owner More or Admin Records). Never embeds role bottom nav.
 */
@Composable
fun OwnerSecurityHistoryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    audits: List<NativeOwnerSecurityAuditRow>,
    auditsLoading: Boolean,
    auditDetail: NativeOwnerSecurityAuditDetail?,
    auditsError: String?,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenAudit: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (auditDetail != null) {
        OwnerSecurityHistoryDetailScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            detail = auditDetail,
            onBack = onBackFromDetail,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBackToMenu)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerSecurityHistoryRoot),
        ) {
            CashierScreenHeader(
                title = "Security history",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                auditsLoading && audits.isEmpty() && auditsError == null -> {
                    NativeColdLoadingPane(
                        label = "Loading security history…",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerSecurityHistoryLoading),
                    )
                }
                auditsError != null && audits.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            auditsError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerSecurityHistoryError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = hapticAction(onRetry),
                            modifier = Modifier.testTag(NativeUiTags.OwnerSecurityHistoryRetry),
                        ) { Text("Try again") }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        auditsError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerSecurityHistoryError),
                            )
                        }
                        if (audits.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .testTag(NativeUiTags.OwnerSecurityHistoryEmpty),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No security audit entries yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerSecurityHistoryList),
                            ) {
                                items(audits, key = { it.auditId }) { audit ->
                                    OwnerSecurityAuditRow(
                                        audit = audit,
                                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenAudit(audit.auditId) },
                                    )
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
private fun OwnerSecurityHistoryDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerSecurityAuditDetail,
    onBack: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    val row = detail.row
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerSecurityHistoryDetail),
        ) {
            CashierScreenHeader(
                title = "Security history",
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
                Text(
                    row.actionCode,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(NativeUiTags.OwnerSecurityHistoryDetailAction),
                )
                OwnerSecurityAuditFact("Occurred at", AppTimeFormats.dateTime(row.occurredAt))
                OwnerSecurityAuditFact("Result", row.resultCode)
                OwnerSecurityAuditFact("Actor", row.actorLabel)
                detail.reasonCode?.takeIf { it.isNotBlank() }?.let {
                    OwnerSecurityAuditFact("Reason", it)
                }
                detail.targetUserId?.let {
                    OwnerSecurityAuditFact("Target user", it.toString())
                }
                detail.details?.takeIf { it.isNotBlank() }?.let {
                    OwnerSecurityAuditFact("Details", it)
                }
                Text(
                    "Read only audit trail. Filtering and export come later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(NativeUiTags.OwnerSecurityHistoryDetailReadOnly),
                )
            }
        }
    }
}

@Composable
private fun OwnerSecurityAuditRow(
    audit: NativeOwnerSecurityAuditRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Record)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Record)
            .testTag(NativeUiTags.ownerSecurityAudit(audit.auditId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                audit.actionCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(audit.resultCode)
                    append(" · ")
                    append(audit.actorLabel)
                    append(" · ")
                    append(audit.occurredAt)
                    audit.detailsPreview?.let {
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
private fun OwnerSecurityAuditFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
