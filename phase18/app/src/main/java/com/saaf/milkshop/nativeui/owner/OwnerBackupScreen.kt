package com.saaf.milkshop.nativeui.owner

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerBackupInspection
import com.saaf.milkshop.application.owner.NativeOwnerBackupSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel

/**
 * Backup surface — Drive vault + local export/restore under HostedScaffold for Owner,
 * or local encrypted export only when [localExportOnly] (Admin). Passphrase CharArrays
 * are wiped by the ViewModel / facade after submit.
 */
@Composable
fun OwnerBackupScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    snapshot: NativeOwnerBackupSnapshot?,
    loading: Boolean,
    error: String?,
    exportOpen: Boolean,
    restoreOpen: Boolean,
    connectOpen: Boolean,
    inspection: NativeOwnerBackupInspection?,
    busy: Boolean,
    reauthOpen: Boolean,
    reauthBusy: Boolean,
    disconnectConfirmOpen: Boolean,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenExport: () -> Unit,
    onBackFromExport: () -> Unit,
    onExport: (Uri, CharArray) -> Unit,
    onOpenRestore: () -> Unit,
    onBackFromRestore: () -> Unit,
    onInspectRestore: (Uri, CharArray) -> Unit,
    onConfirmRestore: (Boolean) -> Unit,
    onOpenConnect: () -> Unit,
    onBackFromConnect: () -> Unit,
    onConnectDrive: (CharArray, Boolean) -> Unit,
    onSetDriveEnabled: (Boolean) -> Unit,
    onRunDriveNow: () -> Unit,
    onRequestDisconnect: () -> Unit,
    onConfirmDisconnect: () -> Unit,
    onCancelDisconnect: () -> Unit,
    onSubmitReauth: (CharArray) -> Unit,
    onCancelReauth: () -> Unit,
    hostedScaffold: HostedScaffold,
    backToHubLabel: String = "Back to More",
    localExportOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val androidContext = LocalContext.current
    val exportFileName = remember(androidContext) {
        BackupManager.displayName(androidContext)
    }
    when {
        reauthOpen && !localExportOnly -> OwnerBackupReauthScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            busy = reauthBusy,
            error = error,
            onConfirm = onSubmitReauth,
            onCancel = onCancelReauth,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        exportOpen -> OwnerBackupExportScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            busy = busy,
            error = error,
            suggestedFileName = exportFileName,
            onBack = onBackFromExport,
            onExport = onExport,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        restoreOpen && !localExportOnly -> OwnerBackupRestoreScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            busy = busy,
            error = error,
            inspection = inspection,
            onBack = onBackFromRestore,
            onInspect = onInspectRestore,
            onConfirmRestore = onConfirmRestore,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        connectOpen && !localExportOnly -> OwnerBackupConnectScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            busy = busy,
            error = error,
            onBack = onBackFromConnect,
            onConnect = onConnectDrive,
            hostedScaffold = hostedScaffold,
            modifier = modifier,
        )
        else -> OwnerBackupStatusScreen(
            snackbarHostState = snackbarHostState,
            identity = identity,
            context = context,
            snapshot = snapshot,
            loading = loading,
            error = error,
            busy = busy,
            disconnectConfirmOpen = disconnectConfirmOpen && !localExportOnly,
            onBackToMenu = onBackToMenu,
            onRetry = onRetry,
            onOpenExport = onOpenExport,
            onOpenRestore = onOpenRestore,
            onOpenConnect = onOpenConnect,
            onSetDriveEnabled = onSetDriveEnabled,
            onRunDriveNow = onRunDriveNow,
            onRequestDisconnect = onRequestDisconnect,
            onConfirmDisconnect = onConfirmDisconnect,
            onCancelDisconnect = onCancelDisconnect,
            hostedScaffold = hostedScaffold,
            backToHubLabel = backToHubLabel,
            localExportOnly = localExportOnly,
            modifier = modifier,
        )
    }
}

@Composable
private fun OwnerBackupStatusScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    snapshot: NativeOwnerBackupSnapshot?,
    loading: Boolean,
    error: String?,
    busy: Boolean,
    disconnectConfirmOpen: Boolean,
    onBackToMenu: () -> Unit,
    onRetry: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenRestore: () -> Unit,
    onOpenConnect: () -> Unit,
    onSetDriveEnabled: (Boolean) -> Unit,
    onRunDriveNow: () -> Unit,
    onRequestDisconnect: () -> Unit,
    onConfirmDisconnect: () -> Unit,
    onCancelDisconnect: () -> Unit,
    hostedScaffold: HostedScaffold,
    backToHubLabel: String = "Back to More",
    localExportOnly: Boolean = false,
    modifier: Modifier,
) {
    BackHandler(onBack = onBackToMenu)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerBackupRoot),
        ) {
            CashierScreenHeader(
                title = if (localExportOnly) "Local export" else "Backup & restore",
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
                        ) { Text(backToHubLabel) }
                    }
                }
                snapshot != null -> {
                    Column(Modifier.fillMaxSize()) {
                        TextButton(
                            onClick = onBackToMenu,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag(NativeUiTags.OwnerBackupBack),
                        ) { Text(backToHubLabel) }
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
                            if (localExportOnly) {
                                Text(
                                    "Encrypted local export",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag(NativeUiTags.OwnerBackupStatusHeadline),
                                )
                                Text(
                                    "Create an encrypted backup file on this phone. Drive connect, restore, and recovery stay Owner-only.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Button(
                                    onClick = onOpenExport,
                                    enabled = !busy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(NativeUiTags.OwnerBackupExport),
                                ) { Text("Export backup") }
                            } else {
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
                                Text(
                                    "Local encrypted backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Button(
                                    onClick = onOpenExport,
                                    enabled = !busy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(NativeUiTags.OwnerBackupExport),
                                ) { Text("Export backup") }
                                Button(
                                    onClick = onOpenRestore,
                                    enabled = !busy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(NativeUiTags.OwnerBackupRestore),
                                ) { Text("Restore") }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Google Drive vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (!snapshot.configured) {
                                    Button(
                                        onClick = onOpenConnect,
                                        enabled = !busy && snapshot.cloudActionsAvailable,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(NativeUiTags.OwnerBackupConnect),
                                    ) { Text("Connect Drive") }
                                    if (!snapshot.cloudActionsAvailable) {
                                        Text(
                                            "Drive connect needs Google authorization on this device.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    if (snapshot.enabled) {
                                        TextButton(
                                            onClick = { onSetDriveEnabled(false) },
                                            enabled = !busy && snapshot.cloudActionsAvailable,
                                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupPause),
                                        ) { Text("Pause Drive vault") }
                                        Button(
                                            onClick = onRunDriveNow,
                                            enabled = !busy && snapshot.cloudActionsAvailable,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag(NativeUiTags.OwnerBackupRunNow),
                                        ) { Text("Run backup now") }
                                    } else {
                                        Button(
                                            onClick = { onSetDriveEnabled(true) },
                                            enabled = !busy && snapshot.cloudActionsAvailable,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag(NativeUiTags.OwnerBackupEnable),
                                        ) { Text("Enable Drive vault") }
                                    }
                                    TextButton(
                                        onClick = onRequestDisconnect,
                                        enabled = !busy && snapshot.cloudActionsAvailable,
                                        modifier = Modifier.testTag(NativeUiTags.OwnerBackupDisconnect),
                                    ) { Text("Disconnect Drive") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (disconnectConfirmOpen) {
        AlertDialog(
            onDismissRequest = onCancelDisconnect,
            title = { Text("Disconnect Drive?") },
            text = {
                Text(
                    "This revokes Google authorization on this phone. Remote Drive backups stay in the account until you delete them there.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDisconnect,
                    modifier = Modifier.testTag(NativeUiTags.OwnerBackupDisconnectConfirm),
                ) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancelDisconnect,
                    modifier = Modifier.testTag(NativeUiTags.OwnerBackupDisconnectCancel),
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun OwnerBackupExportScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    busy: Boolean,
    error: String?,
    suggestedFileName: String,
    onBack: () -> Unit,
    onExport: (Uri, CharArray) -> Unit,
    hostedScaffold: HostedScaffold,
    backToHubLabel: String = "Back to More",
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val dirty = draftTouched && !busy
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }
    fun requestLeave(action: () -> Unit) {
        if (busy) return
        leaveGuard.requestLeave(action)
    }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val chars = passphrase.toCharArray()
        passphrase = ""
        confirm = ""
        onExport(uri, chars)
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
                    .testTag(NativeUiTags.OwnerBackupExportForm),
            ) {
                CashierScreenHeader(
                    title = "Export backup",
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
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        modifier = Modifier.testTag(NativeUiTags.OwnerBackupExportBack),
                    ) { Text("Back") }
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupError),
                        )
                    }
                    Text(
                        "Creates an encrypted portable backup. Choose a strong passphrase you can recover later — it cannot be reset from the file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            draftTouched = true
                        },
                        label = { Text("Backup passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerBackupExportPassphrase),
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = {
                            confirm = it
                            draftTouched = true
                        },
                        label = { Text("Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerBackupExportConfirm),
                    )
                    Button(
                        onClick = {
                            if (passphrase != confirm) return@Button
                            createDocument.launch(suggestedFileName)
                        },
                        enabled = !busy &&
                            passphrase.length >= 15 &&
                            passphrase == confirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerBackupExportSubmit),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Choose destination and export")
                        }
                    }
                }
            }
        }
        OwnerBackupDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard export draft?",
            body = "The passphrase fields will be cleared.",
            discardTag = NativeUiTags.OwnerBackupExportDiscard,
            keepTag = NativeUiTags.OwnerBackupExportKeep,
        )
    }
}

@Composable
private fun OwnerBackupRestoreScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    busy: Boolean,
    error: String?,
    inspection: NativeOwnerBackupInspection?,
    onBack: () -> Unit,
    onInspect: (Uri, CharArray) -> Unit,
    onConfirmRestore: (Boolean) -> Unit,
    hostedScaffold: HostedScaffold,
    backToHubLabel: String = "Back to More",
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val dirty = draftTouched && !busy
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }
    fun requestLeave(action: () -> Unit) {
        if (busy) return
        leaveGuard.requestLeave(action)
    }
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val chars = passphrase.toCharArray()
        passphrase = ""
        onInspect(uri, chars)
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
                    .testTag(NativeUiTags.OwnerBackupRestoreForm),
            ) {
                CashierScreenHeader(
                    title = "Restore backup",
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
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        modifier = Modifier.testTag(NativeUiTags.OwnerBackupRestoreBack),
                    ) { Text("Back") }
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupError),
                        )
                    }
                    Text(
                        "Restoring replaces this installation’s business and security data. You will need to sign in again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (inspection == null) {
                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = {
                                passphrase = it
                                draftTouched = true
                            },
                            label = { Text("Backup passphrase") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OwnerBackupRestorePassphrase),
                        )
                        Button(
                            onClick = {
                                openDocument.launch(
                                    arrayOf("application/octet-stream", "application/json", "*/*"),
                                )
                            },
                            enabled = !busy && passphrase.length >= 15,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OwnerBackupRestorePick),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                            } else {
                                Text("Choose backup file")
                            }
                        }
                    } else {
                        Column(
                            Modifier.testTag(NativeUiTags.OwnerBackupRestoreInspection),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OwnerBackupFact("File", inspection.fileName)
                            OwnerBackupFact("Created", inspection.createdAt)
                            OwnerBackupFact("Format", inspection.formatVersion.toString())
                            OwnerBackupFact(
                                "Contains users",
                                if (inspection.containsSecurityState) "Yes" else "No",
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = confirmed,
                                onCheckedChange = {
                                    confirmed = it
                                    draftTouched = true
                                },
                                modifier = Modifier.testTag(NativeUiTags.OwnerBackupRestoreConfirmCheck),
                            )
                            Text(
                                BackupManager.CONFIRMATION_PHRASE,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Button(
                            onClick = { onConfirmRestore(confirmed) },
                            enabled = !busy && confirmed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(NativeUiTags.OwnerBackupRestoreSubmit),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                            } else {
                                Text("Restore and replace")
                            }
                        }
                    }
                }
            }
        }
        OwnerBackupDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Leave restore?",
            body = "Any inspected backup ticket on this screen will be cancelled.",
            discardTag = NativeUiTags.OwnerBackupRestoreDiscard,
            keepTag = NativeUiTags.OwnerBackupRestoreKeep,
        )
    }
}

@Composable
private fun OwnerBackupConnectScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConnect: (CharArray, Boolean) -> Unit,
    hostedScaffold: HostedScaffold,
    backToHubLabel: String = "Back to More",
    modifier: Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var allowMetered by rememberSaveable { mutableStateOf(false) }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val dirty = draftTouched && !busy
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }
    fun requestLeave(action: () -> Unit) {
        if (busy) return
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
                    .testTag(NativeUiTags.OwnerBackupConnectForm),
            ) {
                CashierScreenHeader(
                    title = "Connect Drive",
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
                    TextButton(
                        onClick = { requestLeave(onBack) },
                        modifier = Modifier.testTag(NativeUiTags.OwnerBackupConnectBack),
                    ) { Text("Back") }
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupError),
                        )
                    }
                    Text(
                        "Choose a recovery passphrase for the Drive vault, then pick the Google account. Live Google account selection requires Play services on the device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            draftTouched = true
                        },
                        label = { Text("Recovery passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerBackupConnectPassphrase),
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = {
                            confirm = it
                            draftTouched = true
                        },
                        label = { Text("Confirm recovery passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerBackupConnectPassphraseConfirm),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allowMetered,
                            onCheckedChange = {
                                allowMetered = it
                                draftTouched = true
                            },
                            modifier = Modifier.testTag(NativeUiTags.OwnerBackupConnectAllowMetered),
                        )
                        Text("Allow backup on metered networks")
                    }
                    Button(
                        onClick = {
                            if (passphrase != confirm) return@Button
                            val chars = passphrase.toCharArray()
                            passphrase = ""
                            confirm = ""
                            onConnect(chars, allowMetered)
                        },
                        enabled = !busy &&
                            passphrase.length >= 15 &&
                            passphrase == confirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerBackupConnectSubmit),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Connect Google Drive")
                        }
                    }
                }
            }
        }
        OwnerBackupDiscardDialog(
            leaveGuard = leaveGuard,
            title = "Discard Drive connect?",
            body = "The recovery passphrase fields will be cleared.",
            discardTag = NativeUiTags.OwnerBackupConnectDiscard,
            keepTag = NativeUiTags.OwnerBackupConnectKeep,
        )
    }
}

@Composable
private fun OwnerBackupReauthScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    busy: Boolean,
    error: String?,
    onConfirm: (CharArray) -> Unit,
    onCancel: () -> Unit,
    hostedScaffold: HostedScaffold,
    backToHubLabel: String = "Back to More",
    modifier: Modifier,
) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    BackHandler(onBack = onCancel)
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(NativeUiTags.OwnerBackupReauth),
        ) {
            CashierScreenHeader(
                title = "Confirm it is you",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag(NativeUiTags.OwnerBackupReauthCancel),
                ) { Text("Cancel") }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerBackupError),
                    )
                }
                Text(
                    "Enter your Owner passphrase to continue this sensitive backup action.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Owner passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerBackupReauthPassphrase),
                )
                Button(
                    onClick = {
                        val chars = passphrase.toCharArray()
                        passphrase = ""
                        onConfirm(chars)
                    },
                    enabled = !busy && passphrase.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerBackupReauthConfirm),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                                Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                    } else {
                        Text("Continue")
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

@Composable
private fun OwnerBackupDiscardDialog(
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
                onClick = leaveGuard::confirmDiscard,
                modifier = Modifier.testTag(discardTag),
            ) { Text("Discard") }
        },
        dismissButton = {
            TextButton(
                onClick = leaveGuard::keepEditing,
                modifier = Modifier.testTag(keepTag),
            ) { Text("Keep editing") }
        },
    )
}
