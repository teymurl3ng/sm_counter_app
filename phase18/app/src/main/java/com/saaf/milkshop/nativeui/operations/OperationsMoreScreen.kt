package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.CircularProgressIndicator
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
    onLeaveConfirmed: () -> Unit,
    moreScaffold: OpsScaffold,
    modifier: Modifier = Modifier,
) {
    when (state) {
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
            OpsMoreHeader(identity, state.context)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag(NativeUiTags.OpsMoreMenu),
            ) {
                OpsMoreMenuRow(
                    icon = Icons.Outlined.History,
                    title = "Today's work",
                    subtitle = "Your physical receive, yogurt, and spoilage records",
                    tag = NativeUiTags.OpsMoreTodayWork,
                    onClick = onOpenTodayWork,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OpsMoreMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = "Personal security",
                    subtitle = "Quick unlock for this account on this phone",
                    tag = NativeUiTags.OpsMorePersonalSecurity,
                    onClick = onOpenPersonalSecurity,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OpsMoreMenuRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch account",
                    subtitle = "Choose another person on this phone",
                    tag = NativeUiTags.OpsMoreSwitchAccount,
                    onClick = onSwitchAccount,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OpsMoreMenuRow(
                    icon = Icons.Outlined.Logout,
                    title = "Sign out",
                    subtitle = "Return to Welcome",
                    tag = NativeUiTags.OpsMoreSignOut,
                    onClick = onSignOut,
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
            OpsMoreHeader(identity, state.context)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Today's work", style = MaterialTheme.typography.headlineMedium)
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
    val security = state.security
    var passphrase by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    if (state.confirmRemoveUnlock) {
        AlertDialog(
            onDismissRequest = onCancelRemove,
            title = { Text("Remove Quick unlock?") },
            text = { Text("This clears Quick PIN and phone security for ${identity.displayName} on this phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val secret = passphrase.toCharArray()
                        passphrase = ""
                        pin = ""
                        onRemove(secret)
                    },
                    modifier = Modifier.testTag(NativeUiTags.OpsMoreSecurityRemoveConfirm),
                ) { Text("Discard and leave") }
            },
            dismissButton = {
                TextButton(onClick = onCancelRemove) { Text("Keep editing") }
            },
        )
    }

    moreScaffold(snackbarHostState, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag(NativeUiTags.OpsMoreSecurityScreen),
        ) {
            OpsMoreHeader(identity, state.context)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Personal security", style = MaterialTheme.typography.headlineMedium)
                if (security == null) {
                    CircularProgressIndicator()
                } else {
                    OpsMoreSecurityStatus(security)
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OpsMoreSecurityPassphrase),
                        label = { Text("Current full passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4) },
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OpsMoreSecurityPin),
                        label = { Text("New Quick PIN (4 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(NativeUiTags.OpsMoreError))
                    }
                    Button(
                        onClick = {
                            if (passphrase.isBlank() || pin.length != 4) return@Button
                            val secret = passphrase.toCharArray()
                            val quickPin = pin.toCharArray()
                            passphrase = ""
                            pin = ""
                            onEnrollPin(secret, quickPin)
                        },
                        enabled = !state.saving && passphrase.isNotBlank() && pin.length == 4,
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OpsMoreSecurityEnrollPin),
                    ) { Text(if (state.saving) "Saving…" else "Save Quick PIN") }
                    if (security.phoneSecurityAvailable) {
                        Button(
                            onClick = {
                                if (passphrase.isBlank()) return@Button
                                val secret = passphrase.toCharArray()
                                val quickPin = pin.takeIf { it.length == 4 }?.toCharArray()
                                passphrase = ""
                                pin = ""
                                onEnrollPhone(secret, quickPin)
                            },
                            enabled = !state.saving && passphrase.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OpsMoreSecurityEnrollPhone),
                        ) { Text("Enable ${security.phoneSecurityLabel}") }
                    }
                    if (security.quickPinEnabled || security.phoneSecurityEnabled) {
                        TextButton(
                            onClick = onRequestRemove,
                            enabled = !state.saving && passphrase.isNotBlank(),
                            modifier = Modifier.testTag(NativeUiTags.OpsMoreSecurityRemove),
                        ) { Text("Remove Quick unlock") }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpsMoreHeader(identity: NativeSignedInIdentity, context: NativeOperationsMoreContext) {
    CashierScreenHeader(
        title = "More",
        displayName = identity.displayName,
        roleLabel = formatCashierRoleLabel(identity.role.wireValue),
        businessDate = context.businessDate,
        postingOpen = context.postingOpen,
    )
}

@Composable
private fun OpsMoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun OpsMoreSecurityStatus(security: NativeMoreSecurityStatus) {
    Column(
        Modifier.fillMaxWidth().testTag(NativeUiTags.OpsMoreSecurityStatus),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Quick PIN: ${if (security.quickPinEnabled) "On" else "Off"}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "${security.phoneSecurityLabel}: ${
                when {
                    !security.phoneSecurityAvailable -> "Unavailable on this phone"
                    security.phoneSecurityEnabled -> "On"
                    else -> "Off"
                }
            }",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
