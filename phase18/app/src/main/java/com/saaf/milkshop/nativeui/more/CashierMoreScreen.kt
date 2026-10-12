package com.saaf.milkshop.nativeui.more

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
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
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.more.NativeBlindCashCountReceipt
import com.saaf.milkshop.application.more.NativeMoreContext
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.hapticCommit
import com.saaf.milkshop.nativeui.common.hapticConfirm
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.sale.CashierNavigation
import com.saaf.milkshop.nativeui.sale.CashierNavigationRail
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState
import com.saaf.milkshop.nativeui.operations.DeliveriesLoadingBody
import com.saaf.milkshop.nativeui.operations.DeliveriesLoadFailureBody
import com.saaf.milkshop.nativeui.operations.DeliveriesReadyContent
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesDraftActions
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
fun CashierMoreScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeMoreState,
    onRetry: () -> Unit,
    onOpenCountCash: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenCloseDay: () -> Unit = {},
    onOpenDeliveries: () -> Unit = {},
    onOpenPersonalSecurity: () -> Unit,
    onBackToMenu: () -> Unit,
    onRecordCashCount: (Long, String?) -> Unit,
    onRequestRemoveQuickUnlock: () -> Unit,
    onCancelRemoveQuickUnlock: () -> Unit,
    onRemoveQuickUnlock: (CharArray) -> Unit,
    onEnrollQuickPin: (CharArray, CharArray) -> Unit,
    onEnrollPhoneSecurity: (CharArray, CharArray?) -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeMoreState.Idle || it is NativeMoreState.Loading) NativeMoreState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeMoreState.Idle,
        NativeMoreState.Loading,
        -> MoreLoadingScreen(snackbarHostState, onNavigate, modifier)

        is NativeMoreState.LoadFailure -> MoreLoadFailureScreen(
            snackbarHostState = snackbarHostState,
            message = state.message,
            onRetry = onRetry,
            onNavigate = onNavigate,
            modifier = modifier,
        )

        is NativeMoreState.Ready -> {
            val destination = state.destination
            when (destination) {
                NativeMoreDestination.Menu -> MoreMenuScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = state,
                    onOpenCountCash = onOpenCountCash,
                    onOpenCloseDay = onOpenCloseDay,
                    onOpenExpense = onOpenExpense,
                    onOpenDeliveries = onOpenDeliveries,
                    onOpenPersonalSecurity = onOpenPersonalSecurity,
                    onSwitchAccount = onSwitchAccount,
                    onSignOut = onSignOut,
                    onNavigate = onNavigate,
                    modifier = modifier,
                )
                NativeMoreDestination.CountCash -> MoreCountCashScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = state,
                    onBack = onBackToMenu,
                    onRecord = onRecordCashCount,
                    onNavigate = onNavigate,
                    modifier = modifier,
                )
                NativeMoreDestination.CashClose,
                NativeMoreDestination.Expense -> {
                    // Hosted by CashierPreviewHost via CashierExpenseEntryScreen.
                    MoreLoadingScreen(snackbarHostState, onNavigate, modifier)
                }
                NativeMoreDestination.Deliveries -> {
                    // Hosted by CashierPreviewHost with the shared typed deliveries state.
                    MoreLoadingScreen(snackbarHostState, onNavigate, modifier)
                }
                NativeMoreDestination.PersonalSecurity -> MorePersonalSecurityScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = state,
                    onBack = onBackToMenu,
                    onRequestRemove = onRequestRemoveQuickUnlock,
                    onCancelRemove = onCancelRemoveQuickUnlock,
                    onRemove = onRemoveQuickUnlock,
                    onEnrollPin = onEnrollQuickPin,
                    onEnrollPhone = onEnrollPhoneSecurity,
                    onNavigate = onNavigate,
                    modifier = modifier,
                )
            }
        }
    }
}
    }

@Composable
fun CashierDeliveriesEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeDeliveriesState,
    onRetry: () -> Unit,
    onMarkDelivered: (Long) -> Unit,
    onLinkSale: (Long, String) -> Unit,
    draftActions: NativeDeliveriesDraftActions,
    onArchive: (Long) -> Unit,
    onBackToMore: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier = Modifier,
    onRegisterGuard: (com.saaf.milkshop.nativeui.common.EntryLeaveGuard?) -> Unit = {},
) {
    val leaveGuard = com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard()
    val ready = state as? NativeDeliveriesState.Ready
    val draftDirty = ready != null && (ready.draftCustomerId != null || ready.draftProductId != null ||
        ready.draftQuantity.isNotBlank() || ready.draftRoute.isNotBlank() || ready.draftNotes.isNotBlank())
    LaunchedEffect(draftDirty) { if (draftDirty) leaveGuard.markDirty() else leaveGuard.clear() }
    DisposableEffect(leaveGuard, draftActions) {
        leaveGuard.onDiscard = {
            draftActions.selectCustomer(null)
            draftActions.selectProduct(null)
            draftActions.setQuantity("")
            draftActions.setRoute("")
            draftActions.setNotes("")
        }
        onRegisterGuard(leaveGuard)
        onDispose { leaveGuard.onDiscard = null; onRegisterGuard(null) }
    }
    BackHandler { if (ready?.creating != true) leaveGuard.requestLeave(onBackToMore) }
    MoreScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.MoreRoot) { padding ->
        NativeSectionTransition(
            targetState = state,
            contentKey = { if (it is NativeDeliveriesState.Idle || it is NativeDeliveriesState.Loading) NativeDeliveriesState.Loading::class else it::class },
        ) { state -> when (state) {
            NativeDeliveriesState.Idle,
            NativeDeliveriesState.Loading,
            -> DeliveriesLoadingBody(Modifier.padding(padding))
            is NativeDeliveriesState.LoadFailure -> DeliveriesLoadFailureBody(
                message = state.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )
            is NativeDeliveriesState.Ready -> DeliveriesReadyContent(
                identity = identity,
                state = state,
                onMarkDelivered = onMarkDelivered,
                onLinkSale = onLinkSale,
                draftActions = draftActions,
                onArchive = onArchive,
                contentPadding = padding,
                showHeader = false,
            )
        }
        }
    }
    com.saaf.milkshop.nativeui.expense.ExpenseDiscardDialog(leaveGuard)
}

@Composable
private fun MoreScaffold(
    snackbarHostState: SnackbarHostState,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
    rootTag: String,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        compactNavigation = { CashierNavigation(CashierSection.More, onNavigate) },
        expandedNavigation = { CashierNavigationRail(CashierSection.More, onNavigate) },
        modifier = modifier.fillMaxSize().testTag(rootTag),
        content = content,
    )
}

@Composable
private fun MoreLoadingScreen(
    snackbarHostState: SnackbarHostState,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    MoreScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.MoreRoot) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            NativeColdLoadingPane(label = "Loading More…", modifier = Modifier.testTag(NativeUiTags.MoreLoading))
        }
    }
}

@Composable
private fun MoreLoadFailureScreen(
    snackbarHostState: SnackbarHostState,
    message: String,
    onRetry: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    MoreScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.MoreRoot) { padding ->
        NativeLoadFailurePane(
            message = message,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            retryTestTag = NativeUiTags.MoreRetryLoad,
        )
    }
}

@Composable
private fun MoreMenuScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeMoreState.Ready,
    onOpenCountCash: () -> Unit,
    onOpenCloseDay: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenDeliveries: () -> Unit,
    onOpenPersonalSecurity: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    MoreScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.MoreRoot) { padding ->
        Column(Modifier.fillMaxSize().nativeRefreshOverlay(state.refreshing).padding(padding)) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.MoreMenu),
            ) {
                MoreMenuRow(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "Expense",
                    subtitle = if (state.context.postingOpen) {
                        "Record shop spending from Cash or Bank"
                    } else {
                        "Day is closed — posting disabled"
                    },
                    enabled = state.context.postingOpen,
                    tag = NativeUiTags.MoreExpense,
                    onClick = onOpenExpense,
                )
                MoreMenuRow(
                    icon = Icons.Outlined.Payments,
                    title = "Count cash",
                    subtitle = if (state.context.postingOpen) {
                        "Opening or handover count for ${state.context.cashAccountName}. New dates open automatically."
                    } else {
                        "Day is closed — counting disabled"
                    },
                    enabled = state.context.postingOpen,
                    tag = NativeUiTags.MoreCountCash,
                    onClick = onOpenCountCash,
                )
                MoreMenuRow(
                    icon = Icons.Outlined.Payments,
                    title = "Count and close day",
                    subtitle = if (state.context.postingOpen) "Finish today after checking cash and deliveries" else "Review the closed day; ask Owner/Admin to reopen",
                    tag = NativeUiTags.MoreCloseDay,
                    onClick = onOpenCloseDay,
                )
                MoreMenuRow(
                    icon = Icons.Outlined.LocalShipping,
                    title = "Deliveries",
                    subtitle = "Review active deliveries and mark completed drop-offs",
                    tag = NativeUiTags.MoreDeliveries,
                    onClick = onOpenDeliveries,
                )
                MoreMenuRow(
                    icon = Icons.Outlined.Lock,
                    title = "Personal security",
                    subtitle = "Quick unlock for this account on this phone",
                    tag = NativeUiTags.MorePersonalSecurity,
                    onClick = onOpenPersonalSecurity,
                )
                MoreMenuRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch account",
                    subtitle = "Choose another person on this phone",
                    tag = NativeUiTags.MoreSwitchAccount,
                    onClick = onSwitchAccount,
                )
                MoreMenuRow(
                    icon = Icons.Outlined.Logout,
                    title = "Sign out",
                    subtitle = "Return to Welcome",
                    tag = NativeUiTags.MoreSignOut,
                    onClick = onSignOut,
                )
            }
        }
    }
}

@Composable
private fun MoreCountCashScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeMoreState.Ready,
    onBack: () -> Unit,
    onRecord: (Long, String?) -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    var amountText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val amountPaisa = parsePaisa(amountText)
    val canRecord = state.context.postingOpen &&
        !state.saving &&
        state.countReceipt == null &&
        amountPaisa != null &&
        amountPaisa >= 0L

    MoreScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.MoreCountCashScreen) { padding ->
        Column(
            Modifier
                .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Count cash", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Enter what is in the ${state.context.cashAccountName} drawer. This count stays blind — no expected drawer total is shown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NativeSectionTransition(targetState = state, contentKey = { it.countReceipt != null }) { pane ->
                if (pane.countReceipt != null) {
                    com.saaf.milkshop.nativeui.common.NativeReceiptFeedback("blind-count", pane.countReceipt.countId)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CountSuccess(pane.countReceipt)
                        Button(onClick = com.saaf.milkshop.nativeui.common.hapticAction(onBack),
                            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.MoreCountDone)) { Text("Done") }
                    }
                } else {
                    Column(
                        Modifier.nativeListItem(NativeListStyle.Section).nativeListItemContent(NativeListStyle.Section),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = sanitizeAmount(it) },
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.MoreCountAmount),
                        label = { Text("Counted amount (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !pane.saving && pane.context.postingOpen,
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(240) },
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.MoreCountNotes),
                        label = { Text("Note (optional)") },
                        enabled = !pane.saving && pane.context.postingOpen,
                    )
                    if (!pane.context.postingOpen) {
                        Text(
                            "This business day is closed. Reopen it before counting cash.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    pane.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(NativeUiTags.MoreError))
                    }
                    val hapticContext = LocalContext.current
                    Button(
                        onClick = {
                            val amount = amountPaisa ?: return@Button
                            hapticContext.hapticCommit()
                            onRecord(amount, notes)
                        },
                        enabled = canRecord,
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.MoreCountRecord),
                    ) {
                        Text(if (pane.saving) "Recording…" else "Record count")
                    }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun MorePersonalSecurityScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeMoreState.Ready,
    onBack: () -> Unit,
    onRequestRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onRemove: (CharArray) -> Unit,
    onEnrollPin: (CharArray, CharArray) -> Unit,
    onEnrollPhone: (CharArray, CharArray?) -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onBack)
    MoreScaffold(snackbarHostState, onNavigate, modifier, NativeUiTags.MoreSecurityScreen) { padding ->
        Column(
            Modifier.fillMaxSize().nativeRefreshOverlay(state.refreshing).padding(padding).imePadding().testTag(NativeUiTags.MoreSecurityScreen),
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
                        status = NativeUiTags.MoreSecurityStatus,
                        passphrase = NativeUiTags.MoreSecurityPassphrase,
                        pin = NativeUiTags.MoreSecurityPin,
                        savePin = NativeUiTags.MoreSecurityEnrollPin,
                        enablePhone = NativeUiTags.MoreSecurityEnrollPhone,
                        remove = NativeUiTags.MoreSecurityRemove,
                        confirmRemove = NativeUiTags.MoreSecurityRemoveConfirm,
                        error = NativeUiTags.MoreError,
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
internal fun MoreMenuRow(
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
            .clickable(enabled = enabled, onClick = com.saaf.milkshop.nativeui.common.hapticAction(onClick))
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

@Composable
private fun CountSuccess(receipt: NativeBlindCashCountReceipt) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.MoreCountSuccess),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Cash count saved", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Counted PKR ${formatMoney(receipt.countedCashPaisa)}", style = MaterialTheme.typography.bodyLarge)
        Text("Record #${receipt.countId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "This blind count does not reveal the shop expected drawer total.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun sanitizeAmount(raw: String): String {
    val filtered = buildString {
        var dotSeen = false
        raw.forEach { ch ->
            when {
                ch.isDigit() -> append(ch)
                ch == '.' && !dotSeen -> {
                    append(ch)
                    dotSeen = true
                }
            }
        }
    }
    val parts = filtered.split('.', limit = 2)
    return if (parts.size == 1) parts[0].take(9)
    else parts[0].take(9) + "." + parts[1].take(2)
}

private fun parsePaisa(text: String): Long? = runCatching {
    if (text.isBlank()) return null
    BigDecimal(text).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
}.getOrNull()

private fun formatMoney(paisa: Long): String =
    String.format(Locale.US, "%,.2f", paisa / 100.0)
