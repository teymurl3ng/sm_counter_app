package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerSecurityHistoryFacade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns security audit history and audit detail loading. */
internal class OwnerSecurityHistoryController(
    private val facade: NativeOwnerSecurityHistoryFacade,
    private val runtime: OwnerMoreRuntime,
) {
    fun openSecurityHistory() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.SecurityHistory,
            securityHistoryLoading = NativeOwnerMoreDestination.SecurityHistory !in current.loadedDestinations,
            securityAuditDetail = null,
            securityHistoryError = null,
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
            backupLoading = false,
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
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.loadAudits(session)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.SecurityHistory) return@launch
                runtime.state.value = latest.copy(
                    securityAudits = snapshot.audits,
                    securityHistoryLoading = false,
                    securityHistoryError = null,
                    loadedDestinations = latest.loadedDestinations + NativeOwnerMoreDestination.SecurityHistory,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshSecurityHistory() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.SecurityHistory) return
        runtime.state.value = current.copy(
            securityHistoryLoading = true,
            securityHistoryError = null,
            securityAuditDetail = null,
        )
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.loadAudits(session)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.SecurityHistory) return@launch
                runtime.state.value = latest.copy(
                    securityAudits = snapshot.audits,
                    securityHistoryLoading = false,
                    securityHistoryError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openSecurityAuditDetail(auditId: Long) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.SecurityHistory) return
        runtime.state.value = current.copy(securityHistoryLoading = true, securityHistoryError = null)
        runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, auditId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.SecurityHistory) return@launch
                runtime.state.value = latest.copy(
                    securityAuditDetail = detail,
                    securityHistoryLoading = false,
                    securityHistoryError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearSecurityAuditDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            securityAuditDetail = null,
            securityHistoryError = null,
            securityHistoryLoading = false,
        )
    }


    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.SecurityHistory, error) { current, message ->
            current.copy(
                securityHistoryLoading = false,
                securityHistoryError = message,
            )
        }
    }
}
