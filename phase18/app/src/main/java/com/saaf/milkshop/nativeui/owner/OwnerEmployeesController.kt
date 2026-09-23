package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerEmployeesFacade
import com.saaf.milkshop.domain.EmployeeProfileInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns employee list, detail, and editor behavior for Owner More. */
internal class OwnerEmployeesController(
    private val facade: NativeOwnerEmployeesFacade,
    private val runtime: OwnerMoreRuntime,
) {
    fun openEmployees() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.Employees,
            employeesLoading = NativeOwnerMoreDestination.Employees !in current.loadedDestinations,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
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
                val snapshot = withContext(Dispatchers.IO) { facade.loadEmployees(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                runtime.state.value = latest.copy(
                    employees = snapshot.employees,
                    employeesLoading = false,
                    employeesError = null,
                    loadedDestinations = latest.loadedDestinations + NativeOwnerMoreDestination.Employees,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshEmployees() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        runtime.state.value = current.copy(
            employeesLoading = true,
            employeesError = null,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
        )
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadEmployees(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                runtime.state.value = latest.copy(
                    employees = snapshot.employees,
                    employeesLoading = false,
                    employeesError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openEmployeeDetail(accountId: Long) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        runtime.state.value = current.copy(
            employeesLoading = true,
            employeesError = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
        )
        runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, accountId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                runtime.state.value = latest.copy(
                    employeeDetail = detail,
                    employeesLoading = false,
                    employeesError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearEmployeeDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            employeeDetail = null,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
            employeesLoading = false,
        )
    }

    fun openEmployeeCreate() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        runtime.state.value = current.copy(
            employeeCreateOpen = true,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeeDetail = null,
            employeesError = null,
            employeesLoading = false,
        )
    }

    fun clearEmployeeCreate() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
        )
    }

    fun createEmployee(name: String) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees || !current.employeeCreateOpen) return
        if (current.employeeCreating) return
        runtime.state.value = current.copy(employeeCreating = true, employeesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) { facade.createEmployee(session, name) }
                val snapshot = withContext(Dispatchers.IO) { facade.loadEmployees(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                runtime.state.value = latest.copy(
                    employees = snapshot.employees,
                    employeeCreateOpen = false,
                    employeeCreating = false,
                    employeeEditOpen = false,
                    employeeUpdating = false,
                    employeesError = null,
                    employeeDetail = null,
                    employeesLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openEmployeeEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        if (current.employeeDetail == null) return
        runtime.state.value = current.copy(
            employeeEditOpen = true,
            employeeUpdating = false,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeesError = null,
            employeesLoading = false,
        )
    }

    fun clearEmployeeEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
        )
    }

    fun updateEmployee(name: String, profile: EmployeeProfileInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.employeeDetail?.row?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees || !current.employeeEditOpen) return
        if (current.employeeUpdating) return
        runtime.state.value = current.copy(employeeUpdating = true, employeesError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.updateEmployee(session, accountId, name, profile)
                }
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { facade.loadEmployees(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                runtime.state.value = latest.copy(
                    employees = snapshot.employees,
                    employeeDetail = detail,
                    employeeEditOpen = false,
                    employeeUpdating = false,
                    employeesError = null,
                    employeesLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }


    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.Employees, error) { current, message ->
            current.copy(
                employeesLoading = false,
                employeeCreating = false,
                employeeUpdating = false,
                employeesError = message,
            )
        }
    }
}
