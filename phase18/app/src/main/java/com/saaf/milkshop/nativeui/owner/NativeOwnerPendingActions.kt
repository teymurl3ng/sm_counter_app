package com.saaf.milkshop.nativeui.owner

import android.net.Uri
import com.saaf.milkshop.domain.CreateUserInput

internal sealed interface PendingUsersRolesAction {
    data class BeginEnroll(val input: CreateUserInput) : PendingUsersRolesAction
    data class SetEnabled(val userId: Long, val enabled: Boolean) : PendingUsersRolesAction
    data class ChangeRole(val userId: Long, val roleWire: String) : PendingUsersRolesAction
    data class BeginCredentialReset(val userId: Long) : PendingUsersRolesAction
}

internal sealed interface PendingBackupAction {
    data class Export(val uri: Uri, val passphrase: CharArray) : PendingBackupAction
    data class Inspect(val uri: Uri, val passphrase: CharArray) : PendingBackupAction
    data class ConfirmRestore(val confirm: Boolean) : PendingBackupAction
    data class Connect(val passphrase: CharArray, val allowMetered: Boolean) : PendingBackupAction
    data class SetPolicy(val enabled: Boolean) : PendingBackupAction
    data object Disconnect : PendingBackupAction
    data object RefreshCloud : PendingBackupAction
    data class InspectCloud(
        val remoteFileId: String,
        val recoveryPassphrase: CharArray?,
    ) : PendingBackupAction
    data class ConfirmCloudRestore(val confirm: Boolean) : PendingBackupAction
}

/** Owns pending backup secrets and guarantees they are wiped when replaced or abandoned. */
internal class OwnerPendingBackupActionStore {
    var current: PendingBackupAction? = null
        private set

    fun replace(next: PendingBackupAction) {
        clear()
        current = next
    }

    /** Transfers ownership to the caller without wiping the action's secrets. */
    fun take(): PendingBackupAction? = current.also { current = null }

    fun clear() {
        when (val pending = current) {
            is PendingBackupAction.Export -> pending.passphrase.fill('\u0000')
            is PendingBackupAction.Inspect -> pending.passphrase.fill('\u0000')
            is PendingBackupAction.Connect -> pending.passphrase.fill('\u0000')
            is PendingBackupAction.InspectCloud -> pending.recoveryPassphrase?.fill('\u0000')
            else -> Unit
        }
        current = null
    }
}
