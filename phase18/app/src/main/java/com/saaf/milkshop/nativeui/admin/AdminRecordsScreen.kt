package com.saaf.milkshop.nativeui.admin

import androidx.compose.foundation.layout.size
import com.saaf.milkshop.nativeui.theme.nativeVisualColors
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeNavigationTitleTextStyle
import com.saaf.milkshop.nativeui.theme.nativeGroupLabelTextStyle
import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeCorrectionPrefill
import com.saaf.milkshop.application.owner.NativeRecordsGroup
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.owner.NativeCorrectionEditorState
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.OwnerRecordsScreen
import com.saaf.milkshop.nativeui.owner.OwnerSecurityHistoryScreen
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import java.time.LocalDate

enum class AdminRecordsDestination {
    Hub,
    Business,
    SecurityHistory,
}

/**
 * Admin Records — business records + security history under AdminScaffold(Records).
 * Hub first; shared content via HostedScaffold. Never mounts Owner Records chrome.
 */
@Composable
fun AdminRecordsScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    destination: AdminRecordsDestination,
    ownerRecordsState: NativeOwnerRecordsState,
    ownerMoreState: NativeOwnerMoreState,
    onOpenBusiness: () -> Unit,
    onOpenSecurityHistory: () -> Unit,
    onBackToHub: () -> Unit,
    onRetryRecords: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onBackFromRecordDetail: () -> Unit,
    onOpenLedger: (Long) -> Unit = {},
    onBackFromLedger: () -> Unit = {},
    onRetrySecurityHistory: () -> Unit,
    onOpenSecurityAudit: (Long) -> Unit,
    onBackFromSecurityAuditDetail: () -> Unit,
    onDeferredGroup: (String) -> Unit,
    onSelectGroup: (NativeRecordsGroup) -> Unit = {},
    onOpenCorrection: (NativeCorrectionPrefill) -> Unit = {},
    onBackFromCorrection: () -> Unit = {},
    onUpdateCorrection: ((NativeCorrectionEditorState) -> NativeCorrectionEditorState) -> Unit = {},
    onSaveCorrection: () -> Unit = {},
    onRefreshCorrectionFacts: () -> Unit = {},
    onOpenReceipt: (com.saaf.milkshop.application.owner.NativeOwnerFundsContext) -> Unit = {},
    onBackFromReceipt: () -> Unit = {},
    onUpdateReceipt: ((com.saaf.milkshop.nativeui.owner.NativeOwnerFundsReceiptEditorState) -> com.saaf.milkshop.nativeui.owner.NativeOwnerFundsReceiptEditorState) -> Unit = {},
    onSaveReceipt: () -> Unit = {},
    onRefreshReceiptFacts: () -> Unit = {},
    admScaffold: AdmScaffold,
    modifier: Modifier = Modifier,
) {
    val adminRecordsShell: HostedScaffold = { snackbar, mod, _, content ->
        admScaffold(snackbar, mod, content)
    }

    NativeSectionTransition(
        targetState = destination,
        contentKey = { it },
    ) { destination -> when (destination) {
        AdminRecordsDestination.Hub -> AdminRecordsHubScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            postingOpen = (ownerMoreState as? NativeOwnerMoreState.Ready)?.context?.postingOpen ?: true,
            onOpenBusiness = onOpenBusiness,
            onOpenSecurityHistory = onOpenSecurityHistory,
            admScaffold = admScaffold,
            modifier = modifier,
        )
        AdminRecordsDestination.Business -> Box(
            Modifier
                .fillMaxSize()
                .testTag(NativeUiTags.AdminBusinessRecordsRoot),
        ) {
            val ready = ownerRecordsState as? NativeOwnerRecordsState.Ready
            BackHandler(enabled = ready?.detail == null && ready?.ledger == null && ready?.correction == null && ready?.receipt == null, onBack = onBackToHub)
            OwnerRecordsScreen(
            showHeader = false,
                snackbarHostState = snackbarHostState,
                state = ownerRecordsState,
                onRetry = onRetryRecords,
                onSearchQueryChange = onSearchQueryChange,
                onOpenRecord = onOpenRecord,
                onBackFromDetail = onBackFromRecordDetail,
                onOpenLedger = onOpenLedger,
                    onBackFromLedger = onBackFromLedger,
                    hostedScaffold = adminRecordsShell,
                roleLabel = "Admin",
                rootTag = NativeUiTags.AdminRecordsRoot,
                loadingTag = NativeUiTags.AdminRecordsLoading,
                onDeferredGroup = onDeferredGroup,
                onSelectGroup = onSelectGroup,
                onOpenCorrection = onOpenCorrection,
                onBackFromCorrection = onBackFromCorrection,
                onUpdateCorrection = onUpdateCorrection,
                onSaveCorrection = onSaveCorrection,
                onRefreshCorrectionFacts = onRefreshCorrectionFacts,
                onOpenReceipt = onOpenReceipt,
                onBackFromReceipt = onBackFromReceipt,
                onUpdateReceipt = onUpdateReceipt,
                onSaveReceipt = onSaveReceipt,
                onRefreshReceiptFacts = onRefreshReceiptFacts,
                modifier = modifier,
            )
        }
        AdminRecordsDestination.SecurityHistory -> {
            val ready = ownerMoreState as? NativeOwnerMoreState.Ready
            if (ready == null) {
                BackHandler(onBack = onBackToHub)
                admScaffold(snackbarHostState, modifier) {
                    NativeColdLoadingPane(
                        label = "Loading…",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                            .testTag(NativeUiTags.AdminRecordsLoading),
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(NativeUiTags.AdminSecurityHistoryRoot),
                ) {
                    OwnerSecurityHistoryScreen(
                        showHeader = false,
                        snackbarHostState = snackbarHostState,
                        identity = identity,
                        context = ready.context,
                        audits = ready.securityAudits,
                        auditsLoading = ready.securityHistoryLoading,
                        auditDetail = ready.securityAuditDetail,
                        auditsError = ready.securityHistoryError,
                        onBackToMenu = onBackToHub,
                        onRetry = onRetrySecurityHistory,
                        onOpenAudit = onOpenSecurityAudit,
                        onBackFromDetail = onBackFromSecurityAuditDetail,
                        hostedScaffold = adminRecordsShell,
                        modifier = modifier,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun AdminRecordsHubScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    postingOpen: Boolean,
    onOpenBusiness: () -> Unit,
    onOpenSecurityHistory: () -> Unit,
    admScaffold: AdmScaffold,
    modifier: Modifier,
) {
    admScaffold(snackbarHostState, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.AdminRecordsRoot),
        ) {
            CashierScreenHeader(
                title = "Records",
                displayName = identity.displayName,
                roleLabel = "Admin",
                businessDate = com.saaf.milkshop.sync.BusinessClock.today().toString(),
                postingOpen = postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.AdminRecordsHub),
            ) {
                AdminRecordsSectionLabel("Business and security")
                AdminRecordsMenuRow(
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    title = "Business records",
                    subtitle = "Inspect shop events and movements",
                    tag = NativeUiTags.AdminRecordsBusiness,
                    onClick = hapticAction(onOpenBusiness),
                )
                AdminRecordsMenuRow(
                    icon = Icons.Outlined.History,
                    title = "Security history",
                    subtitle = "Audit trail for access and privileged actions",
                    tag = NativeUiTags.AdminRecordsSecurityHistory,
                    onClick = hapticAction(onOpenSecurityHistory),
                )
            }
        }
    }
}

@Composable
internal fun AdminRecordsSectionLabel(label: String) {
    Text(
        label,
        style = nativeGroupLabelTextStyle(),
        color = nativeVisualColors.supporting,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag(NativeUiTags.AdminRecordsSectionLabel),
    )
}

@Composable
internal fun AdminRecordsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Navigation)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Navigation)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = nativeNavigationTitleTextStyle())
            Text(
                subtitle,
                style = nativeSupportingTextStyle(),
                color = nativeVisualColors.supporting,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}
