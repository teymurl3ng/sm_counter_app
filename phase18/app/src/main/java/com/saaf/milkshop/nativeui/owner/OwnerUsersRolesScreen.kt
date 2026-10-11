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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.hapticAction
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerPendingEnrollment
import com.saaf.milkshop.application.owner.NativeOwnerUserDetail
import com.saaf.milkshop.application.owner.NativeOwnerUserRow
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.application.AppTimeFormats
import java.util.Locale

/**
 * Owner Users & roles — staff login list + detail + enroll / enable / role change
 * under HostedScaffold (Owner More or Admin People). distinct from Employees salary accounts.
 */
private data class OwnerUsersRolesScreenSnapshot(
    val identity: NativeSignedInIdentity,
    val context: NativeOwnerMoreContext,
    val users: List<NativeOwnerUserRow>,
    val usersLoading: Boolean,
    val userDetail: NativeOwnerUserDetail?,
    val userEnrollOpen: Boolean,
    val userEnrollPending: NativeOwnerPendingEnrollment?,
    val userEnrollBusy: Boolean,
    val userRoleChangeOpen: Boolean,
    val userRoleChanging: Boolean,
    val userEnableConfirmOpen: Boolean,
    val userEnableBusy: Boolean,
    val userReauthOpen: Boolean,
    val userReauthBusy: Boolean,
    val usersError: String?,
    val enrollableRoles: List<AppRole>,
    val showHeader: Boolean,
)

@Composable
fun OwnerUsersRolesScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    users: List<NativeOwnerUserRow>,
    usersLoading: Boolean,
    userDetail: NativeOwnerUserDetail?,
    userEnrollOpen: Boolean,
    userEnrollPending: NativeOwnerPendingEnrollment?,
    userEnrollBusy: Boolean,
    userRoleChangeOpen: Boolean,
    userRoleChanging: Boolean,
    userEnableConfirmOpen: Boolean,
    userEnableBusy: Boolean,
    userReauthOpen: Boolean,
    userReauthBusy: Boolean,
    usersError: String?,
    enrollableRoles: List<AppRole>,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenEnroll: () -> Unit,
    onBackFromEnroll: () -> Unit,
    onBeginEnroll: (loginName: String, displayName: String, roleWire: String) -> Unit,
    onCompleteEnroll: (passphrase: CharArray, confirmation: CharArray) -> Unit,
    onOpenRoleChange: () -> Unit,
    onBackFromRoleChange: () -> Unit,
    onChangeRole: (String) -> Unit,
    onRequestSetEnabled: (Boolean) -> Unit,
    onConfirmSetEnabled: () -> Unit,
    onCancelSetEnabled: () -> Unit,
    onBeginCredentialReset: () -> Unit = {},
    onResumeCredentialSetup: () -> Unit = {},
    onSubmitReauth: (CharArray) -> Unit,
    onCancelReauth: () -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val paneSnapshot = OwnerUsersRolesScreenSnapshot(
        identity = identity,
        context = context,
        users = users,
        usersLoading = usersLoading,
        userDetail = userDetail,
        userEnrollOpen = userEnrollOpen,
        userEnrollPending = userEnrollPending,
        userEnrollBusy = userEnrollBusy,
        userRoleChangeOpen = userRoleChangeOpen,
        userRoleChanging = userRoleChanging,
        userEnableConfirmOpen = userEnableConfirmOpen,
        userEnableBusy = userEnableBusy,
        userReauthOpen = userReauthOpen,
        userReauthBusy = userReauthBusy,
        usersError = usersError,
        enrollableRoles = enrollableRoles,
        showHeader = showHeader,
    )
    com.saaf.milkshop.nativeui.common.NativeSectionTransition(
        targetState = paneSnapshot,
        contentKey = { pane -> when {
            pane.userEnrollOpen && pane.userEnrollPending != null -> "credentials"
            pane.userEnrollOpen -> "enroll"
            pane.userRoleChangeOpen -> "role"
            pane.userDetail != null -> "user-detail"
            else -> "home"
        } },
        modifier = modifier,
    ) { pane ->
        OwnerUsersRolesScreenPane(
            snackbarHostState = snackbarHostState,
            identity = pane.identity,
            context = pane.context,
            users = pane.users,
            usersLoading = pane.usersLoading,
            userDetail = pane.userDetail,
            userEnrollOpen = pane.userEnrollOpen,
            userEnrollPending = pane.userEnrollPending,
            userEnrollBusy = pane.userEnrollBusy,
            userRoleChangeOpen = pane.userRoleChangeOpen,
            userRoleChanging = pane.userRoleChanging,
            userEnableConfirmOpen = pane.userEnableConfirmOpen,
            userEnableBusy = pane.userEnableBusy,
            userReauthOpen = pane.userReauthOpen,
            userReauthBusy = pane.userReauthBusy,
            usersError = pane.usersError,
            enrollableRoles = pane.enrollableRoles,
            onBackToMenu = onBackToMenu,
            onRetry = onRetry,
            onOpenUser = onOpenUser,
            onBackFromDetail = onBackFromDetail,
            onOpenEnroll = onOpenEnroll,
            onBackFromEnroll = onBackFromEnroll,
            onBeginEnroll = onBeginEnroll,
            onCompleteEnroll = onCompleteEnroll,
            onOpenRoleChange = onOpenRoleChange,
            onBackFromRoleChange = onBackFromRoleChange,
            onChangeRole = onChangeRole,
            onRequestSetEnabled = onRequestSetEnabled,
            onConfirmSetEnabled = onConfirmSetEnabled,
            onCancelSetEnabled = onCancelSetEnabled,
            onBeginCredentialReset = onBeginCredentialReset,
            onResumeCredentialSetup = onResumeCredentialSetup,
            onSubmitReauth = onSubmitReauth,
            onCancelReauth = onCancelReauth,
            hostedScaffold = hostedScaffold,
            showHeader = pane.showHeader,
            modifier = modifier,
        )
    }
}

@Composable
private fun OwnerUsersRolesScreenPane(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    users: List<NativeOwnerUserRow>,
    usersLoading: Boolean,
    userDetail: NativeOwnerUserDetail?,
    userEnrollOpen: Boolean,
    userEnrollPending: NativeOwnerPendingEnrollment?,
    userEnrollBusy: Boolean,
    userRoleChangeOpen: Boolean,
    userRoleChanging: Boolean,
    userEnableConfirmOpen: Boolean,
    userEnableBusy: Boolean,
    userReauthOpen: Boolean,
    userReauthBusy: Boolean,
    usersError: String?,
    enrollableRoles: List<AppRole>,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onBackFromDetail: () -> Unit,
    onOpenEnroll: () -> Unit,
    onBackFromEnroll: () -> Unit,
    onBeginEnroll: (loginName: String, displayName: String, roleWire: String) -> Unit,
    onCompleteEnroll: (passphrase: CharArray, confirmation: CharArray) -> Unit,
    onOpenRoleChange: () -> Unit,
    onBackFromRoleChange: () -> Unit,
    onChangeRole: (String) -> Unit,
    onRequestSetEnabled: (Boolean) -> Unit,
    onConfirmSetEnabled: () -> Unit,
    onCancelSetEnabled: () -> Unit,
    onBeginCredentialReset: () -> Unit = {},
    onResumeCredentialSetup: () -> Unit = {},
    onSubmitReauth: (CharArray) -> Unit,
    onCancelReauth: () -> Unit,
    hostedScaffold: HostedScaffold,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    when {
        userReauthOpen -> {
            OwnerUsersRolesReauthDialog(
                busy = userReauthBusy,
                error = usersError,
                onConfirm = onSubmitReauth,
                onCancel = onCancelReauth,
            )
        }
    }

    when {
        userEnrollOpen && userEnrollPending != null -> {
            OwnerUsersRolesEnrollPassphraseScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                pending = userEnrollPending,
                saving = userEnrollBusy,
                error = usersError,
                onBack = onBackFromEnroll,
                onSave = onCompleteEnroll,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            return
        }
        userEnrollOpen -> {
            OwnerUsersRolesEnrollIdentityScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                roles = enrollableRoles,
                saving = userEnrollBusy,
                error = usersError,
                onBack = onBackFromEnroll,
                onSave = onBeginEnroll,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            return
        }
        userRoleChangeOpen && userDetail != null -> {
            OwnerUsersRolesRoleChangeScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                detail = userDetail,
                roles = enrollableRoles,
                saving = userRoleChanging,
                error = usersError,
                onBack = onBackFromRoleChange,
                onSave = onChangeRole,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            return
        }
        userDetail != null -> {
            OwnerUsersRolesDetailScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                context = context,
                detail = userDetail,
                enableConfirmOpen = userEnableConfirmOpen,
                enableBusy = userEnableBusy,
                error = usersError,
                onBack = onBackFromDetail,
                onOpenRoleChange = onOpenRoleChange,
                onRequestSetEnabled = onRequestSetEnabled,
                onConfirmSetEnabled = onConfirmSetEnabled,
                onCancelSetEnabled = onCancelSetEnabled,
                onBeginCredentialReset = onBeginCredentialReset,
                onResumeCredentialSetup = onResumeCredentialSetup,
                hostedScaffold = hostedScaffold,
                modifier = modifier,
            )
            return
        }
    }

    BackHandler(onBack = onBackToMenu)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerUsersRolesRoot),
        ) {
            CashierScreenHeader(
                title = "Users & roles",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            when {
                usersLoading && users.isEmpty() && usersError == null -> {
                    NativeColdLoadingPane(
                        label = "Loading users & roles…",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(NativeUiTags.OwnerUsersRolesLoading),
                    )
                }
                usersError != null && users.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            usersError,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesError),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = hapticAction(onRetry),
                            modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesRetry),
                        ) { Text("Try again") }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = hapticAction(onOpenEnroll),
                                modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesEnroll),
                            ) { Text("Enroll user") }
                        }
                        usersError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .testTag(NativeUiTags.OwnerUsersRolesError),
                            )
                        }
                        if (users.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .testTag(NativeUiTags.OwnerUsersRolesEmpty),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No staff users yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.OwnerUsersRolesList),
                            ) {
                                items(users, key = { it.userId }) { user ->
                                    OwnerUserRow(
                                        user = user,
                                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { onOpenUser(user.userId) },
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
private fun OwnerUsersRolesDetailScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerUserDetail,
    enableConfirmOpen: Boolean,
    enableBusy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOpenRoleChange: () -> Unit,
    onRequestSetEnabled: (Boolean) -> Unit,
    onConfirmSetEnabled: () -> Unit,
    onCancelSetEnabled: () -> Unit,
    onBeginCredentialReset: () -> Unit = {},
    onResumeCredentialSetup: () -> Unit = {},
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    BackHandler(enabled = !enableBusy, onBack = onBack)
    val row = detail.row
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerUsersRolesDetail),
        ) {
            CashierScreenHeader(
                title = "Users & roles",
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
                    row.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesDetailName),
                )
                OwnerUserFact("Login", row.loginName)
                OwnerUserFact("Role", row.roleLabel)
                OwnerUserFact("Status", row.statusLabel)
                OwnerUserFact("Setup required", if (row.setupRequired) "Yes" else "No")
                OwnerUserFact("Created at", AppTimeFormats.dateTime(detail.createdAt))
                OwnerUserFact("Updated at", detail.updatedAt)
                detail.disabledAt?.takeIf { it.isNotBlank() }?.let {
                    OwnerUserFact("Disabled at", it)
                }
                OwnerUserFact("Employee account", detail.employeeAccountIdLabel)
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesError),
                    )
                }
                Button(
                    onClick = hapticAction(onOpenRoleChange),
                    enabled = !enableBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerUsersRolesDetailChangeRole),
                ) { Text("Change role") }
                if (row.enabled) {
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { onRequestSetEnabled(false) },
                        enabled = !enableBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesDetailDisable),
                    ) { Text(if (enableBusy) "Working…" else "Disable user") }
                } else {
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticAction { onRequestSetEnabled(true) },
                        enabled = !enableBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesDetailEnable),
                    ) { Text(if (enableBusy) "Working…" else "Enable user") }
                }
                Button(
                    onClick = hapticAction(onBeginCredentialReset),
                    enabled = !enableBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerUsersRolesCredentialReset),
                ) { Text("Reset credentials") }
                if (row.setupRequired) {
                    Button(
                        onClick = hapticAction(onResumeCredentialSetup),
                        enabled = !enableBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesResumeSetup),
                    ) { Text("Resume setup") }
                }
                Text(
                    "Creates a one-time setup token so this staff login can choose a new passphrase. " +
                        "This is staff login access, not Employees salary accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (enableConfirmOpen) {
        val enabling = !row.enabled
        AlertDialog(
            onDismissRequest = { if (!enableBusy) onCancelSetEnabled() },
            title = { Text(if (enabling) "Enable this user?" else "Disable this user?") },
            text = {
                Text(
                    if (enabling) {
                        "They will be able to sign in again with their existing passphrase."
                    } else {
                        "They will not be able to sign in until you enable the account again."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = hapticAction(onConfirmSetEnabled),
                    enabled = !enableBusy,
                    modifier = Modifier.testTag(
                        if (enabling) {
                            NativeUiTags.OwnerUsersRolesEnableConfirm
                        } else {
                            NativeUiTags.OwnerUsersRolesDisableConfirm
                        },
                    ),
                ) { Text(if (enabling) "Enable" else "Disable") }
            },
            dismissButton = {
                TextButton(
                    onClick = hapticAction(onCancelSetEnabled),
                    enabled = !enableBusy,
                    modifier = Modifier.testTag(
                        if (enabling) {
                            NativeUiTags.OwnerUsersRolesEnableCancel
                        } else {
                            NativeUiTags.OwnerUsersRolesDisableCancel
                        },
                    ),
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun OwnerUsersRolesEnrollIdentityScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    roles: List<AppRole>,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (loginName: String, displayName: String, roleWire: String) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var loginName by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var roleWire by rememberSaveable { mutableStateOf(AppRole.CASHIER.wireValue) }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val canSave = loginName.trim().isNotEmpty() && displayName.trim().isNotEmpty() && !saving
    val dirty = draftTouched && !saving

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        hostedScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
            { action -> requestLeave(action) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerUsersRolesEnrollForm),
            ) {
                CashierScreenHeader(
                    title = "Users & roles",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Enroll user",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Create the identity, then hand the device to this person so they can privately choose their passphrase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = loginName,
                        onValueChange = {
                            loginName = it
                            draftTouched = true
                        },
                        label = { Text("Login name") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesEnrollLogin),
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            draftTouched = true
                        },
                        label = { Text("Display name") },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesEnrollDisplayName),
                    )
                    Text(
                        "Role",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OwnerRoleChipRow(
                        roles = roles,
                        selectedWire = roleWire,
                        enabled = !saving,
                        tagForRole = NativeUiTags::ownerUsersRolesEnrollRole,
                        onSelect = {
                            roleWire = it
                            draftTouched = true
                        },
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction { onSave(loginName, displayName, roleWire) },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesEnrollSave),
                    ) { Text(if (saving) "Saving…" else "Continue") }
                }
            }
        }
        OwnerUsersRolesDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard this enrollment?",
            body = "The draft login details will be lost.",
            discardTag = NativeUiTags.OwnerUsersRolesEnrollDiscard,
            keepTag = NativeUiTags.OwnerUsersRolesEnrollKeep,
        )
    }
}

@Composable
private fun OwnerUsersRolesEnrollPassphraseScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    pending: NativeOwnerPendingEnrollment,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (passphrase: CharArray, confirmation: CharArray) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val canSave = passphrase.isNotEmpty() && confirmation.isNotEmpty() && !saving
    val dirty = draftTouched && !saving

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        hostedScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
            { action -> requestLeave(action) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerUsersRolesEnrollPassphraseForm),
            ) {
                CashierScreenHeader(
                    title = "Users & roles",
                    displayName = context.shopName,
                    roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                    businessDate = context.businessDate,
                    postingOpen = context.postingOpen,
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Passphrase for ${pending.displayName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "The new user should enter this privately. The administrator must look away and must not record it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            draftTouched = true
                            localError = null
                        },
                        label = { Text("New passphrase") },
                        singleLine = true,
                        enabled = !saving,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesEnrollPassphrase),
                    )
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = {
                            confirmation = it
                            draftTouched = true
                            localError = null
                        },
                        label = { Text("Confirm passphrase") },
                        singleLine = true,
                        enabled = !saving,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesEnrollPassphraseConfirm),
                    )
                    (localError ?: error)?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                            if (passphrase != confirmation) {
                                localError = "Passphrases do not match"
                                return@hapticCommitAction
                            }
                            val secret = passphrase.toCharArray()
                            val confirm = confirmation.toCharArray()
                            passphrase = ""
                            confirmation = ""
                            onSave(secret, confirm)
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesEnrollPassphraseSave),
                    ) { Text(if (saving) "Saving…" else "Finish enrollment") }
                }
            }
        }
        OwnerUsersRolesDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Leave without finishing?",
            body = "The login was created but still needs a passphrase. You can finish setup later.",
            discardTag = NativeUiTags.OwnerUsersRolesEnrollDiscard,
            keepTag = NativeUiTags.OwnerUsersRolesEnrollKeep,
        )
    }
}

@Composable
private fun OwnerUsersRolesRoleChangeScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    detail: NativeOwnerUserDetail,
    roles: List<AppRole>,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var roleWire by rememberSaveable { mutableStateOf(detail.row.roleWire) }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val dirty = draftTouched && roleWire != detail.row.roleWire && !saving
    val canSave = roleWire != detail.row.roleWire && !saving

    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }

    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }

    BackHandler { requestLeave(onBack) }

    Box(modifier.fillMaxSize()) {
        hostedScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
            { action -> requestLeave(action) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(NativeUiTags.OwnerUsersRolesRoleChangeForm),
            ) {
                CashierScreenHeader(
                    title = "Users & roles",
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
                        "Change role",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        detail.row.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OwnerRoleChipRow(
                        roles = roles,
                        selectedWire = roleWire,
                        enabled = !saving,
                        tagForRole = NativeUiTags::ownerUsersRolesChangeRole,
                        onSelect = {
                            roleWire = it
                            draftTouched = true
                        },
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesError),
                        )
                    }
                    Button(
                        onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction { onSave(roleWire) },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerUsersRolesRoleChangeSave),
                    ) { Text(if (saving) "Saving…" else "Save role") }
                }
            }
        }
        OwnerUsersRolesDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard role change?",
            body = "The selected role will not be saved.",
            discardTag = NativeUiTags.OwnerUsersRolesRoleChangeDiscard,
            keepTag = NativeUiTags.OwnerUsersRolesRoleChangeKeep,
        )
    }
}

@Composable
private fun OwnerUsersRolesReauthDialog(
    busy: Boolean,
    error: String?,
    onConfirm: (CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesReauth),
        title = { Text("Confirm with passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your full Owner passphrase to continue this sensitive action.")
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Current full passphrase") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerUsersRolesReauthPassphrase),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = com.saaf.milkshop.nativeui.common.hapticCommitAction {
                    if (passphrase.isBlank()) return@hapticCommitAction
                    val secret = passphrase.toCharArray()
                    passphrase = ""
                    onConfirm(secret)
                },
                enabled = !busy && passphrase.isNotBlank(),
                modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesReauthConfirm),
            ) { Text(if (busy) "Checking…" else "Continue") }
        },
        dismissButton = {
            TextButton(
                onClick = hapticAction(onCancel),
                enabled = !busy,
                modifier = Modifier.testTag(NativeUiTags.OwnerUsersRolesReauthCancel),
            ) { Text("Cancel") }
        },
    )
}

@Composable
private fun OwnerRoleChipRow(
    roles: List<AppRole>,
    selectedWire: String,
    enabled: Boolean,
    tagForRole: (String) -> String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        roles.chunked(3).forEach { rowRoles ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowRoles.forEach { role ->
                    FilterChip(
                        selected = role.wireValue == selectedWire,
                        onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelect(role.wireValue) },
                        enabled = enabled,
                        label = {
                            Text(
                                role.wireValue.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                                },
                            )
                        },
                        modifier = Modifier.testTag(tagForRole(role.wireValue)),
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerUserRow(
    user: NativeOwnerUserRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 4.dp).nativeListItem(NativeListStyle.Entity)
            .semantics { role = Role.Button }
            .clickable(onClick = hapticAction(onClick))
            .nativeListItemContent(NativeListStyle.Entity)
            .testTag(NativeUiTags.ownerUser(user.userId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                user.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                buildString {
                    append(user.roleLabel)
                    append(" · ")
                    append(user.statusLabel)
                    append(" · ")
                    append(user.loginName)
                    if (user.setupRequired) append(" · Setup required")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun OwnerUserFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun OwnerUsersRolesDiscardDialog(
    leaveGuard: EntryLeaveGuard,
    title: String,
    body: String,
    discardTag: String,
    keepTag: String,
) {
    if (!leaveGuard.discardConfirmationOpen) return
    AlertDialog(
        onDismissRequest = leaveGuard::keepEditing,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = hapticAction(leaveGuard::confirmDiscard),
                modifier = Modifier.testTag(discardTag),
            ) { Text("Discard and leave") }
        },
        dismissButton = {
            TextButton(
                onClick = hapticAction(leaveGuard::keepEditing),
                modifier = Modifier.testTag(keepTag),
            ) { Text("Keep editing") }
        },
    )
}
