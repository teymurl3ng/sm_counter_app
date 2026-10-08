package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.domain.ServerAccountPolicy
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.sync.NativeSyncHold
import com.saaf.milkshop.sync.NativeSyncShopChoice
import com.saaf.milkshop.sync.NativeSyncOwnerChoice
import com.saaf.milkshop.sync.NativeSyncStatus
import com.saaf.milkshop.sync.SyncServerConfig

/**
 * Owner-facing sync enrollment and status (14.8.6 cutover). Shows the
 * enrollment state of this phone, enrolls it against the Account Server
 * (release builds are PROD-only; debug builds may pick DEV), and stops
 * syncing on request. Daily shop entry never depends on any of this.
 * Credentials live only in local Compose state and are wiped after use.
 */
@Composable
fun OwnerSyncScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    status: NativeSyncStatus?,
    shopChoices: List<NativeSyncShopChoice>,
    ownerChoices: List<NativeSyncOwnerChoice>,
    busy: Boolean,
    error: String?,
    notice: String?,
    onRefresh: () -> Unit,
    onEnroll: (username: String, credential: CharArray, environment: String) -> Unit,
    onEnrollInShop: (shopId: String, username: String, credential: CharArray, environment: String, localUserId: Long?) -> Unit,
    onEnrollAs: (localUserId: Long, username: String, credential: CharArray, environment: String) -> Unit,
    onSyncNow: () -> Unit,
    onUnenroll: () -> Unit,
    allowDevEnvironment: Boolean = false,
    // go 8 sync-review: restored entries held for an owner decision. Defaults
    // keep the admin-role composition silent (holds are owner decisions).
    holds: List<NativeSyncHold> = emptyList(),
    onResolveHold: (String, String, Boolean) -> Unit = { _, _, _ -> },
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
) {
    var unenrollConfirmOpen by remember { mutableStateOf(false) }
    // Holds the credential between a first (possibly ambiguous) attempt
    // and the shop pick, so the owner does not retype it. Wiped on use.
    var lastCredential by remember { mutableStateOf<CharArray?>(null) }
    var pickedOwnerId by remember { mutableStateOf<Long?>(null) }

    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        var username by rememberSaveable { mutableStateOf("") }
        var credential by rememberSaveable { mutableStateOf("") }
        var environment by rememberSaveable { mutableStateOf(SyncServerConfig.ENVIRONMENT_PROD) }
        var environmentMenuOpen by remember { mutableStateOf(false) }

        fun takeCredential(): CharArray {
            lastCredential?.let { return it }
            return credential.toCharArray()
        }

        // Scaffold stacks content children in a Box — separate siblings draw
        // on top of each other (the sync "ghosting"). One column holds the
        // pinned header and the scrollable body below it, inset by the
        // shell's padding.
        Column(Modifier.fillMaxSize().padding(padding)) {
        // Header sits flush at the screen top — outside the horizontally
        // padded, scrolling content column (white bars otherwise).
        CashierScreenHeader(
            title = "Sync",
            displayName = context.shopName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = context.businessDate,
            postingOpen = context.postingOpen,
        )

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .testTag(NativeUiTags.OwnerSyncRoot)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            when {
                busy && status == null -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                            .testTag(NativeUiTags.OwnerSyncLoading),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                status != null -> OwnerSyncEnrolledBody(
                    status = status,
                    busy = busy,
                    holds = holds,
                    onResolveHold = onResolveHold,
                    onRefresh = onRefresh,
                    onSyncNow = onSyncNow,
                    onUnenroll = { unenrollConfirmOpen = true },
                )

                else -> {
                    OwnerSyncEnrollBody(
                    username = username,
                    onUsernameChange = { username = it },
                    credential = credential,
                    onCredentialChange = { credential = it },
                    environment = environment,
                    onEnvironmentChange = { environment = it },
                    environmentMenuOpen = environmentMenuOpen,
                    onEnvironmentMenuOpen = { environmentMenuOpen = true },
                    onEnvironmentMenuDismiss = { environmentMenuOpen = false },
                    allowDevEnvironment = allowDevEnvironment,
                    busy = busy,
                    onEnroll = {
                        val chars = takeCredential()
                        lastCredential = chars
                        onEnroll(username, chars, environment)
                    },
                )
                    // M26: a cold-load failure used to render only as a small
                    // red line under the enroll form with no way back. The
                    // existing refresh path IS the retry — surface it.
                    if (error != null) {
                        NativeLoadFailurePane(
                            message = error,
                            onRetry = onRefresh,
                            retryTestTag = NativeUiTags.OwnerSyncRetryLoad,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            if (ownerChoices.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Several local owners use this app. Pick who this phone syncs as:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag(NativeUiTags.OwnerSyncOwnerPicker),
                )
                ownerChoices.forEach { owner ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable {
                                pickedOwnerId = owner.userId
                                val chars = takeCredential()
                                lastCredential = chars
                                onEnrollAs(owner.userId, username, chars, environment)
                            }
                            .testTag(NativeUiTags.OwnerSyncOwnerPick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(owner.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("Choose", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (shopChoices.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "This account works at several shops. Pick the one this phone records for:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag(NativeUiTags.OwnerSyncShopPicker),
                )
                shopChoices.forEach { choice ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(choice.shopName ?: choice.shopId, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                choice.shopId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = {
                                val chars = takeCredential()
                                onEnrollInShop(choice.shopId, username, chars, environment, pickedOwnerId)
                                chars.fill(' ')
                                lastCredential = null
                            },
                            enabled = !busy,
                            modifier = Modifier.testTag(NativeUiTags.OwnerSyncShopPick),
                        ) { Text("Choose") }
                    }
                }
            }

            notice?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(NativeUiTags.OwnerSyncNotice),
                )
            }
            if (status != null) error?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(NativeUiTags.OwnerSyncError),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        }
    }

    if (unenrollConfirmOpen) {
        AlertDialog(
            onDismissRequest = { unenrollConfirmOpen = false },
            title = { Text("Stop syncing this phone?") },
            text = {
                Text(
                    "New entries stay on this phone and stop uploading. " +
                        "Existing data on this phone is not deleted.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        unenrollConfirmOpen = false
                        onUnenroll()
                    },
                    modifier = Modifier.testTag(NativeUiTags.OwnerSyncUnenrollConfirm),
                ) { Text("Stop syncing") }
            },
            dismissButton = {
                TextButton(onClick = { unenrollConfirmOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun OwnerSyncEnrolledBody(
    status: NativeSyncStatus,
    busy: Boolean,
    holds: List<NativeSyncHold>,
    onResolveHold: (String, String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSyncNow: () -> Unit,
    onUnenroll: () -> Unit,
) {
    Column(Modifier.testTag(NativeUiTags.OwnerSyncStatus)) {
        OwnerSyncStatusRow("Server", status.environment.uppercase())
        OwnerSyncStatusRow("Shop", status.shopName ?: "—")
        OwnerSyncStatusRow("Signed in as", status.role)
        val identityLabel = when (status.identityStatus) {
            ServerAccountPolicy.LinkStatus.VERIFIED -> "verified"
            ServerAccountPolicy.LinkStatus.SUSPENDED -> "suspended on the server"
            ServerAccountPolicy.LinkStatus.REVOKED -> "revoked on the server"
            "active_unlinked" -> "connected - owner link pending"
            "counter_device" -> "counter device (cashier sync)"
            else -> null
        }
        if (identityLabel != null) {
            OwnerSyncStatusRow("Identity", identityLabel)
        }
        status.identityServerUsername?.let {
            OwnerSyncStatusRow("Server account", it)
        }
        status.identityOwnerName?.let {
            OwnerSyncStatusRow("Linked owner", it)
        }
        status.graceDaysLeft?.let { daysLeft ->
            OwnerSyncStatusRow(
                "Offline operation",
                if (status.graceWarningLevel >= 3) "URGENT - $daysLeft day(s) left before limited mode" else "$daysLeft day(s) of full operation left",
            )
        }
        OwnerSyncStatusRow("Session", if (status.sessionPresent) "active" else "needs sign-in again")
        OwnerSyncStatusRow("Trusted time", if (status.trustedTimeAnchored) "server-anchored" else "phone clock only")
        OwnerSyncStatusRow("Waiting to upload", status.outboxCount.toString())
        OwnerSyncStatusRow("Last pulled record", status.pullCursor.toString())
    }
    if (holds.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Needs review — entries restored from a backup that the server " +
                "holds newer versions of. Nothing uploads until you decide.",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.testTag(NativeUiTags.OwnerSyncHolds),
        )
        holds.forEach { hold ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    hold.label.ifBlank { "${hold.tableName} · ${hold.rowUid}" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (hold.label.isNotBlank()) {
                    Text(
                        hold.rowUid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Server version from ${hold.originDevice ?: "an unknown device"}" +
                        " · held ${hold.heldAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onResolveHold(hold.tableName, hold.rowUid, true) },
                        enabled = !busy,
                        modifier = Modifier.testTag(NativeUiTags.OwnerSyncHoldKeep),
                    ) { Text("Keep mine") }
                    OutlinedButton(
                        onClick = { onResolveHold(hold.tableName, hold.rowUid, false) },
                        enabled = !busy,
                        modifier = Modifier.testTag(NativeUiTags.OwnerSyncHoldTake),
                    ) { Text("Take server") }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onSyncNow,
            enabled = !busy,
            modifier = Modifier.testTag(NativeUiTags.OwnerSyncNow),
        ) { Text(if (busy) "Syncing…" else "Sync now") }
        OutlinedButton(
            onClick = onRefresh,
            enabled = !busy,
            modifier = Modifier.testTag(NativeUiTags.OwnerSyncRefresh),
        ) { Text("Refresh") }
    }
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = onUnenroll,
        enabled = !busy,
        modifier = Modifier.testTag(NativeUiTags.OwnerSyncUnenroll),
    ) { Text("Stop syncing this phone") }
}

@Composable
private fun OwnerSyncStatusRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun OwnerSyncEnrollBody(
    username: String,
    onUsernameChange: (String) -> Unit,
    credential: String,
    onCredentialChange: (String) -> Unit,
    environment: String,
    onEnvironmentChange: (String) -> Unit,
    environmentMenuOpen: Boolean,
    onEnvironmentMenuOpen: () -> Unit,
    onEnvironmentMenuDismiss: () -> Unit,
    allowDevEnvironment: Boolean,
    busy: Boolean,
    onEnroll: () -> Unit,
) {
    Column {
        Text("Business sync connection", style = MaterialTheme.typography.titleMedium)
        Text(
            "Once connected, entries recorded on this phone upload to your shop's " +
                "online record automatically whenever internet is available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Server username") },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.OwnerSyncUsername),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = credential,
            onValueChange = onCredentialChange,
            label = { Text("Server credential (15 digits)") },
            singleLine = true,
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(NativeUiTags.OwnerSyncCredential),
        )
        if (allowDevEnvironment) {
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = onEnvironmentMenuOpen,
                    enabled = !busy,
                    modifier = Modifier.testTag(NativeUiTags.OwnerSyncEnvironment),
                ) { Text("Server: ${environment.uppercase()}") }
                DropdownMenu(
                    expanded = environmentMenuOpen,
                    onDismissRequest = onEnvironmentMenuDismiss,
                ) {
                    DropdownMenuItem(
                        text = { Text("PROD (live shop)") },
                        onClick = {
                            onEnvironmentChange(SyncServerConfig.ENVIRONMENT_PROD)
                            onEnvironmentMenuDismiss()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("DEV (testing)") },
                        onClick = {
                            onEnvironmentChange(SyncServerConfig.ENVIRONMENT_DEV)
                            onEnvironmentMenuDismiss()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onEnroll,
            enabled = !busy && username.isNotBlank() && credential.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag(NativeUiTags.OwnerSyncEnroll),
        ) { Text(if (busy) "Connecting…" else "Connect this phone") }
    }
}
