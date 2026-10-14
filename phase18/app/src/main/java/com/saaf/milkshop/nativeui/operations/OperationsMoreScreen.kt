package com.saaf.milkshop.nativeui.operations

import com.saaf.milkshop.nativeui.common.NativePersonalSecurityPanel
import com.saaf.milkshop.nativeui.common.NativePersonalSecurityTags
import androidx.compose.foundation.layout.size
import com.saaf.milkshop.nativeui.theme.nativeNavigationTitleTextStyle
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeValueTextStyle
import com.saaf.milkshop.nativeui.theme.nativeVisualColors

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.operations.NativeOperationsMoreContext
import com.saaf.milkshop.application.operations.NativeOperationsWorkRecord
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane

/**
 * Operations More — same shell layout as Cashier More (green header, menu rows,
 * personal security form), with Ops bottom nav and Today's work instead of Count cash.
 */
@Composable
fun OperationsMoreScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOperationsMoreState,
    onRetry: () -> Unit,
    onOpenTodayWork: () -> Unit,
    onOpenPersonalSecurity: () -> Unit,
    onBackToMenu: () -> Unit,
    onRequestRemoveQuickUnlock: () -> Unit,
    onCancelRemoveQuickUnlock: () -> Unit,
    onRemoveQuickUnlock: (CharArray) -> Unit,
    onEnrollQuickPin: (CharArray, CharArray) -> Unit,
    onEnrollPhoneSecurity: (CharArray, CharArray?) -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    moreScaffold: OpsScaffold,
    modifier: Modifier = Modifier,
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeOperationsMoreState.Idle || it is NativeOperationsMoreState.Loading) NativeOperationsMoreState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeOperationsMoreState.Idle,
        NativeOperationsMoreState.Loading,
        -> OpsMoreLoadingScreen(snackbarHostState, moreScaffold, modifier)

        is NativeOperationsMoreState.LoadFailure -> OpsMoreLoadFailureScreen(
            snackbarHostState = snackbarHostState,
            message = state.message,
            onRetry = onRetry,
            moreScaffold = moreScaffold,
            modifier = modifier,
        )

        is NativeOperationsMoreState.Ready -> NativeSectionTransition(
            targetState = state.destination,
            contentKey = { it },
            modifier = modifier,
        ) { destination ->
            Column(Modifier.nativeRefreshOverlay(state.refreshing)) {
                when (destination) {
                    NativeOperationsMoreDestination.Menu -> OpsMoreMenuScreen(
                        snackbarHostState = snackbarHostState,
                        identity = identity,
                        state = state,
                        onOpenTodayWork = onOpenTodayWork,
                        onOpenPersonalSecurity = onOpenPersonalSecurity,
                        onSwitchAccount = onSwitchAccount,
                        onSignOut = onSignOut,
                        moreScaffold = moreScaffold,
                        modifier = modifier,
                    )
                    NativeOperationsMoreDestination.TodayWork -> OpsMoreTodayWorkScreen(
                        snackbarHostState = snackbarHostState,
                        identity = identity,
                        state = state,
                        onBack = onBackToMenu,
                        moreScaffold = moreScaffold,
                        modifier = modifier,
                    )
                    NativeOperationsMoreDestination.PersonalSecurity -> OpsMorePersonalSecurityScreen(
                        snackbarHostState = snackbarHostState,
                        identity = identity,
                        state = state,
                        onBack = onBackToMenu,
                        onRequestRemove = onRequestRemoveQuickUnlock,
                        onCancelRemove = onCancelRemoveQuickUnlock,
                        onRemove = onRemoveQuickUnlock,
                        onEnrollPin = onEnrollQuickPin,
                        onEnrollPhone = onEnrollPhoneSecurity,
                        moreScaffold = moreScaffold,
                        modifier = modifier,
                    )
                }
            }
        }
    }
    }
}

@Composable
internal fun OpsMoreScaffold(
    snackbarHostState: SnackbarHostState,
    onNavigate: (OperationsSection) -> Unit,
    modifier: Modifier,
    rootTag: String,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        modifier = modifier.fillMaxSize().testTag(rootTag),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        compactNavigation = { OperationsNavigation(selected = OperationsSection.More, onNavigate = onNavigate) },
        expandedNavigation = { OperationsNavigationRail(selected = OperationsSection.More, onNavigate = onNavigate) },
        content = content,
    )
}

@Composable
private fun OpsMoreLoadingScreen(
    snackbarHostState: SnackbarHostState,
    moreScaffold: OpsScaffold,
    modifier: Modifier,
) {
    moreScaffold(snackbarHostState, modifier) { padding ->
        NativeColdLoadingPane(
            label = "Loading…",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OpsMoreLoading),
        )
    }
}

@Composable
private fun OpsMoreLoadFailureScreen(
    snackbarHostState: SnackbarHostState,
    message: String,
    onRetry: () -> Unit,
    moreScaffold: OpsScaffold,
    modifier: Modifier,
) {
    moreScaffold(snackbarHostState, modifier) { padding ->
        NativeLoadFailurePane(
            message = message,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            retryTestTag = NativeUiTags.OpsMoreRetryLoad,
        )
    }
}

@Composable
private fun OpsMoreMenuScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOperationsMoreState.Ready,
    onOpenTodayWork: () -> Unit,
    onOpenPersonalSecurity: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    moreScaffold: OpsScaffold,
    modifier: Modifier,
) {
    moreScaffold(snackbarHostState, modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.OpsMoreMenu),
            ) {
                OpsMoreMenuRow(
                    icon = Icons.Outlined.History,
                    title = "Today's work",
                    subtitle = "Your physical receive, yogurt, and spoilage records",
                    tag = NativeUiTags.OpsMoreTodayWork,
                    onClick = hapticAction(onOpenTodayWork),
                )
                OpsMoreMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = "Personal security",
                    subtitle = "Quick unlock for this account on this phone",
                    tag = NativeUiTags.OpsMorePersonalSecurity,
                    onClick = hapticAction(onOpenPersonalSecurity),
                )
                OpsMoreMenuRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch account",
                    subtitle = "Choose another person on this phone",
                    tag = NativeUiTags.OpsMoreSwitchAccount,
                    onClick = hapticAction(onSwitchAccount),
                )
                OpsMoreMenuRow(
                    icon = Icons.Outlined.Logout,
                    title = com.saaf.milkshop.nativeui.common.NativeAccessCopy.LOCK_TITLE,
                    subtitle = com.saaf.milkshop.nativeui.common.NativeAccessCopy.LOCK_DETAIL,
                    tag = NativeUiTags.OpsMoreSignOut,
                    onClick = hapticAction(onSignOut),
                )
            }
        }
    }
}

@Composable
private fun OpsMoreTodayWorkScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOperationsMoreState.Ready,
    onBack: () -> Unit,
    moreScaffold: OpsScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    moreScaffold(snackbarHostState, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag(NativeUiTags.OpsMoreTodayWorkScreen),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Physical records you posted today. Amounts and balances stay out of this list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(NativeUiTags.OpsMoreError))
                }
                if (state.todayRecords.isEmpty()) {
                    Text(
                        "No physical work recorded yet today.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(NativeUiTags.OpsMoreTodayEmpty),
                    )
                } else {
                    state.todayRecords.forEach { record ->
                        OpsMoreWorkRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun OpsMoreWorkRow(record: NativeOperationsWorkRecord) {
    Column(
        Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Record)
            .nativeListItemContent(NativeListStyle.Record)
            .testTag(NativeUiTags.opsMoreWorkRecord(record.eventId)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                record.taskLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                record.timeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            record.detailLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Record #${record.eventId}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OpsMorePersonalSecurityScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeOperationsMoreState.Ready,
    onBack: () -> Unit,
    onRequestRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onRemove: (CharArray) -> Unit,
    onEnrollPin: (CharArray, CharArray) -> Unit,
    onEnrollPhone: (CharArray, CharArray?) -> Unit,
    moreScaffold: OpsScaffold,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    moreScaffold(snackbarHostState, modifier) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().testTag(NativeUiTags.OpsMoreSecurityScreen),
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NativePersonalSecurityPanel(
                    identityKey = identity.userId,
                    identityName = identity.displayName,
                    security = state.security,
                    saving = state.saving,
                    error = state.error,
                    confirmRemove = state.confirmRemoveUnlock,
                    tags = NativePersonalSecurityTags(
                        status = NativeUiTags.OpsMoreSecurityStatus,
                        passphrase = NativeUiTags.OpsMoreSecurityPassphrase,
                        pin = NativeUiTags.OpsMoreSecurityPin,
                        savePin = NativeUiTags.OpsMoreSecurityEnrollPin,
                        enablePhone = NativeUiTags.OpsMoreSecurityEnrollPhone,
                        remove = NativeUiTags.OpsMoreSecurityRemove,
                        confirmRemove = NativeUiTags.OpsMoreSecurityRemoveConfirm,
                        error = NativeUiTags.OpsMoreError,
                    ),
                    onRequestRemove = onRequestRemove,
                    onCancelRemove = onCancelRemove,
                    onRemove = onRemove,
                    onEnrollPin = onEnrollPin,
                    onEnrollPhone = onEnrollPhone,
                )
            }
        }
    }
}
@Composable
internal fun OpsMoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Navigation, enabled = enabled)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Navigation)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = nativeNavigationTitleTextStyle())
            Text(subtitle, style = nativeSupportingTextStyle(), color = nativeVisualColors.supporting)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}
