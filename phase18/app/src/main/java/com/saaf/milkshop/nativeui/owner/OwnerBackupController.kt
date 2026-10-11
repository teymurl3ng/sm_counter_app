package com.saaf.milkshop.nativeui.owner

import android.net.Uri
import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.domain.SecurityFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns local/cloud backup, restore tickets, recovery secrets, and backup re-auth. */
internal class OwnerBackupController(
    private val facade: NativeOwnerBackupFacade,
    private val runtime: OwnerMoreRuntime,
) {
    private val pendingBackupActions = OwnerPendingBackupActionStore()

    fun openBackup() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        current.backupInspection?.restoreTicket?.let { ticket ->
            facade.cancelRestoreTicket(ticket)
            facade.cancelCloudRestoreTicket(ticket)
        }
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.Backup,
            backupLoading = NativeOwnerMoreDestination.Backup !in current.loadedDestinations,
            backupError = null,
            backupExportOpen = false,
            backupRestoreOpen = false,
            backupConnectOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            shopSettingsLoading = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupLoading = false,
                    backupError = null,
                    loadedDestinations = latest.loadedDestinations + NativeOwnerMoreDestination.Backup,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshBackup() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        runtime.state.value = current.copy(backupLoading = true, backupError = null)
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupLoading = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openBackupExport() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        clearPendingBackupSecrets()
        runtime.state.value = current.copy(
            backupExportOpen = true,
            backupRestoreOpen = false,
            backupConnectOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            backupError = null,
        )
    }

    fun clearBackupExport() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        runtime.state.value = current.copy(
            backupExportOpen = false,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupError = null,
        )
    }

    fun exportBackup(uri: Uri, passphrase: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupExportOpen ||
            current.backupBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        val retryPassphrase = passphrase.copyOf()
        var retryRetained = false
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            var saved = false
            try {
                withContext(Dispatchers.IO) {
                    facade.exportEncrypted(session, uri, passphrase)
                }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Encrypted backup exported")
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                clearPendingBackupSecrets()
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupExportOpen = false,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.Export(uri, retryPassphrase))
                    retryRetained = true
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, com.saaf.milkshop.nativeui.common.savedReadbackFailure(saved, error))
                }
            } finally {
                passphrase.fill('\u0000')
                if (!retryRetained) retryPassphrase.fill('\u0000')
            }
        }
    }

    fun openBackupRestore() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        clearPendingBackupSecrets()
        current.backupInspection?.restoreTicket?.let { ticket ->
            facade.cancelRestoreTicket(ticket)
            facade.cancelCloudRestoreTicket(ticket)
        }
        runtime.state.value = current.copy(
            backupRestoreOpen = true,
            backupExportOpen = false,
            backupConnectOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            backupError = null,
        )
    }

    fun clearBackupRestore() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        current.backupInspection?.restoreTicket?.let { ticket ->
            facade.cancelRestoreTicket(ticket)
            facade.cancelCloudRestoreTicket(ticket)
        }
        runtime.state.value = current.copy(
            backupRestoreOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupError = null,
        )
    }

    fun inspectBackupRestore(uri: Uri, passphrase: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupRestoreOpen ||
            current.backupBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        val retryPassphrase = passphrase.copyOf()
        var retryRetained = false
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            try {
                val inspection = withContext(Dispatchers.IO) {
                    facade.inspectEncrypted(session, uri, passphrase)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backupInspection = inspection,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.Inspect(uri, retryPassphrase))
                    retryRetained = true
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            } finally {
                if (!retryRetained) retryPassphrase.fill('\u0000')
            }
        }
    }

    fun confirmBackupRestore(confirm: Boolean) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val ticket = current.backupInspection?.restoreTicket ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupRestoreOpen ||
            current.backupBusy
        ) {
            return
        }
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            var saved = false
            try {
                withContext(Dispatchers.IO) {
                    facade.confirmRestore(session, ticket, confirm)
                }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Backup restored on this phone")
                clearPendingBackupSecrets()
                runtime.endSession()
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.ConfirmRestore(confirm))
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, com.saaf.milkshop.nativeui.common.savedReadbackFailure(saved, error))
                }
            }
        }
    }

    fun openBackupConnect() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        clearPendingBackupSecrets()
        runtime.state.value = current.copy(
            backupConnectOpen = true,
            backupExportOpen = false,
            backupRestoreOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            backupError = null,
        )
    }

    fun clearBackupConnect() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        runtime.state.value = current.copy(
            backupConnectOpen = false,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupError = null,
        )
    }

    fun connectDriveBackup(passphrase: CharArray, allowMetered: Boolean) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupConnectOpen ||
            current.backupBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        val retryPassphrase = passphrase.copyOf()
        var retryRetained = false
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            var saved = false
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.connectDrive(session, passphrase, allowMetered)
                }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Drive backup connected")
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                clearPendingBackupSecrets()
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupConnectOpen = false,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.Connect(retryPassphrase, allowMetered))
                    retryRetained = true
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, com.saaf.milkshop.nativeui.common.savedReadbackFailure(saved, error))
                }
            } finally {
                passphrase.fill('\u0000')
                if (!retryRetained) retryPassphrase.fill('\u0000')
            }
        }
    }

    fun setDriveBackupEnabled(enabled: Boolean) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        val allowMetered = current.backup?.allowMetered ?: false
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            var saved = false
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.setDrivePolicy(session, enabled, allowMetered)
                }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Backup settings saved")
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.SetPolicy(enabled))
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, com.saaf.milkshop.nativeui.common.savedReadbackFailure(saved, error))
                }
            }
        }
    }

    fun runDriveBackupNow() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            var saved = false
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.runDriveBackupNow(session)
                }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Backup requested. Check backup status for completion.")
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, com.saaf.milkshop.nativeui.common.savedReadbackFailure(saved, error))
            }
        }
    }

    fun requestBackupDisconnect() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        runtime.state.value = current.copy(backupDisconnectConfirmOpen = true, backupError = null)
    }

    fun cancelBackupDisconnect() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(backupDisconnectConfirmOpen = false)
    }

    fun confirmBackupDisconnect() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        runtime.state.value = current.copy(
            backupBusy = true,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            backupError = null,
        )
        runtime.scope.launch {
            var saved = false
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.disconnectDrive(session, confirmRevoke = true)
                }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Drive backup disconnected")
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.Disconnect)
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, com.saaf.milkshop.nativeui.common.savedReadbackFailure(saved, error))
                }
            }
        }
    }


    fun openBackupCloud() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        current.backupInspection?.restoreTicket?.let(facade::cancelCloudRestoreTicket)
        runtime.state.value = current.copy(
            backupCloudOpen = true,
            backupExportOpen = false,
            backupRestoreOpen = false,
            backupConnectOpen = false,
            backupInspection = null,
            backupError = null,
        )
    }

    fun backFromBackupCloud() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        current.backupInspection?.restoreTicket?.let(facade::cancelCloudRestoreTicket)
        runtime.state.value = current.copy(
            backupCloudOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupError = null,
        )
    }

    fun refreshCloudBackups() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.refreshCloudSnapshots(session)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun inspectCloudBackup(remoteFileId: String, recoveryPassphrase: CharArray?) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            recoveryPassphrase?.fill('\u0000')
            return
        }
        val session = token.handle
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) {
            recoveryPassphrase?.fill('\u0000')
            return
        }
        val retryPassphrase = recoveryPassphrase?.copyOf()
        var retryRetained = false
        current.backupInspection?.restoreTicket?.let(facade::cancelCloudRestoreTicket)
        runtime.state.value = current.copy(backupBusy = true, backupError = null, backupInspection = null)
        runtime.scope.launch {
            try {
                val inspection = withContext(Dispatchers.IO) {
                    facade.inspectCloudBackup(session, remoteFileId, recoveryPassphrase)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                runtime.state.value = latest.copy(
                    backupInspection = inspection,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.InspectCloud(
                        remoteFileId = remoteFileId,
                        recoveryPassphrase = retryPassphrase,
                    ))
                    retryRetained = true
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            } finally {
                if (!retryRetained) retryPassphrase?.fill('\u0000')
            }
        }
    }

    fun confirmCloudBackupRestore(confirm: Boolean) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val ticket = current.backupInspection?.restoreTicket ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        runtime.state.value = current.copy(backupBusy = true, backupError = null)
        runtime.scope.launch {
            var saved = false
            try {
                withContext(Dispatchers.IO) {
                    facade.confirmCloudRestore(session, ticket, confirm)
                }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Cloud backup restored on this phone")
                runtime.endSession()
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupActions.replace(PendingBackupAction.ConfirmCloudRestore(confirm))
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleFailure(token, com.saaf.milkshop.nativeui.common.savedReadbackFailure(saved, error))
                }
            }
        }
    }

    fun submitBackupReauth(passphrase: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        if (!current.backupReauthOpen || current.backupReauthBusy) {
            passphrase.fill('\u0000')
            return
        }
        val pending = pendingBackupActions.current
        if (pending == null) {
            passphrase.fill('\u0000')
            runtime.state.value = current.copy(backupReauthOpen = false, backupReauthBusy = false)
            return
        }
        val retryAction = pending
        runtime.state.value = current.copy(backupReauthBusy = true, backupError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.reauthenticate(session, passphrase)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                runtime.state.value = latest.copy(
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
                pendingBackupActions.take()
                when (retryAction) {
                    is PendingBackupAction.Export -> {
                        exportBackup(retryAction.uri, retryAction.passphrase)
                    }
                    is PendingBackupAction.Inspect -> {
                        inspectBackupRestore(retryAction.uri, retryAction.passphrase)
                    }
                    is PendingBackupAction.ConfirmRestore -> {
                        confirmBackupRestore(retryAction.confirm)
                    }
                    is PendingBackupAction.Connect -> {
                        connectDriveBackup(retryAction.passphrase, retryAction.allowMetered)
                    }
                    is PendingBackupAction.SetPolicy -> {
                        setDriveBackupEnabled(retryAction.enabled)
                    }
                    PendingBackupAction.Disconnect -> {
                        confirmBackupDisconnect()
                    }
                    is PendingBackupAction.InspectCloud -> {
                        inspectCloudBackup(retryAction.remoteFileId, retryAction.recoveryPassphrase)
                    }
                    is PendingBackupAction.ConfirmCloudRestore -> {
                        confirmCloudBackupRestore(retryAction.confirm)
                    }
                    PendingBackupAction.RefreshCloud -> {
                        refreshCloudBackups()
                    }
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun cancelBackupReauth() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        runtime.state.value = current.copy(
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupBusy = false,
            backupError = null,
        )
    }

    private fun clearPendingBackupSecrets() {
        pendingBackupActions.clear()
    }


    fun leave() {
        clearPendingBackupSecrets()
        val current = runtime.ready() ?: return
        current.backupInspection?.restoreTicket?.let { ticket ->
            facade.cancelRestoreTicket(ticket)
            facade.cancelCloudRestoreTicket(ticket)
        }
    }

    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            clearPendingBackupSecrets()
        }
        runtime.handleFailure(token, NativeOwnerMoreDestination.Backup, error) { current, message ->
            current.copy(
                backupLoading = false,
                backupBusy = false,
                backupReauthBusy = false,
                backupError = message,
            )
        }
    }
}
