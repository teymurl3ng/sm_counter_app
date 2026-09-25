package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.application.owner.NativeOwnerBackupFailure
import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.application.owner.NativeOwnerEmployeesFacade
import com.saaf.milkshop.application.owner.NativeOwnerMoreFacade
import com.saaf.milkshop.application.owner.NativeOwnerProductsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSecurityHistoryFacade
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
import com.saaf.milkshop.application.owner.NativeOwnerUsersRolesFacade
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductionLossAssessmentInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.YieldPolicyInput
import android.net.Uri
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NativeOwnerMoreViewModel internal constructor(
    private val dependencies: OwnerMoreDependencies,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOwnerMoreState>(NativeOwnerMoreState.Idle)
    private val sessions = OwnerSessionRuntime()
    private var sessionHandle: String?
        get() = sessions.handle
        set(value) = sessions.replace(value)
    private val runtime = OwnerMoreRuntime(
        scope = scope,
        state = _state,
        sessions = sessions,
        expireSession = ::expireSession,
    )
    private val personalSecurityController = OwnerPersonalSecurityController(dependencies.more, runtime)
    private val productsController = OwnerProductsController(dependencies.products, runtime)
    private val customersController = OwnerCustomersController(dependencies.customers, runtime)
    private val suppliersController = OwnerSuppliersController(dependencies.suppliers, runtime)
    private val employeesController = OwnerEmployeesController(dependencies.employees, runtime)
    private val costingController = OwnerCostingController(dependencies.costing, runtime)
    private val shopSettingsController = OwnerShopSettingsController(dependencies.shopSettings, runtime)
    private val usersRolesController = OwnerUsersRolesController(dependencies.usersRoles, runtime)
    private val securityHistoryController =
        OwnerSecurityHistoryController(dependencies.securityHistory, runtime)
    private val backupController = OwnerBackupController(dependencies.backup, runtime)
    val state: StateFlow<NativeOwnerMoreState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            (_state.value is NativeOwnerMoreState.Ready ||
                _state.value is NativeOwnerMoreState.Loading)
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        sessionHandle = null
        personalSecurityController.resetTransient()
        productsController.resetTransient()
        costingController.resetPending()
        shopSettingsController.resetPending()
        usersRolesController.resetPending()
        backupController.leave()
        _state.value = NativeOwnerMoreState.Idle
    }

    fun refresh() {
        val token = runtime.restartSession() ?: return
        val previous = _state.value as? NativeOwnerMoreState.Ready
        _state.value = previous?.copy(refreshing = true) ?: NativeOwnerMoreState.Loading
        scope.launch {
            try {
                val (context, security) = coroutineScope {
                    val contextRequest = async(Dispatchers.IO) {
                        dependencies.more.load(token.handle)
                    }
                    val securityRequest = async(Dispatchers.IO) {
                        dependencies.more.loadSecurity(token.handle)
                    }
                    contextRequest.await() to securityRequest.await()
                }
                if (!runtime.isCurrent(token)) return@launch
                _state.value = NativeOwnerMoreState.Ready(
                    context = context,
                    security = security,
                    refreshing = false,
                )
            } catch (error: Throwable) {
                if (previous != null && error !is SecurityFailure.SessionExpired) {
                    if (!runtime.isCurrent(token)) return@launch
                    _state.value = previous.copy(
                        refreshing = false,
                        error = publicMessage(error),
                    )
                } else {
                    handleLoadFailure(token, error)
                }
            }
        }
    }

    fun openPersonalSecurity() = personalSecurityController.openPersonalSecurity()

    fun openProducts() = productsController.open()

    fun refreshProducts() = productsController.refresh()

    fun setProductsShowArchived(showArchived: Boolean) =
        productsController.setShowArchived(showArchived)

    fun openProductDetail(productId: Long) = productsController.openDetail(productId)

    fun clearProductDetail() = productsController.clearDetail()

    fun openProductCreate() = productsController.openCreate()

    fun clearProductCreate() = productsController.clearCreate()

    fun createProduct(input: ProductCatalogInput) = productsController.create(input)

    fun openProductEdit() = productsController.openEdit()

    fun clearProductEdit() = productsController.clearEdit()

    fun updateProduct(input: ProductCatalogInput) = productsController.update(input)

    fun archiveProduct() = productsController.archive()

    fun unarchiveProduct() = productsController.unarchive()

    fun openCustomers() = customersController.openCustomers()

    fun refreshCustomers() = customersController.refreshCustomers()

    fun openCustomerDetail(accountId: Long) = customersController.openCustomerDetail(accountId)

    fun clearCustomerDetail() = customersController.clearCustomerDetail()

    fun openCustomerCreate() = customersController.openCustomerCreate()

    fun clearCustomerCreate() = customersController.clearCustomerCreate()

    fun createCustomer(name: String) = customersController.createCustomer(name)

    fun openCustomerEdit() = customersController.openCustomerEdit()

    fun clearCustomerEdit() = customersController.clearCustomerEdit()

    fun updateCustomer(name: String, profile: CustomerProfileInput) =
        customersController.updateCustomer(name, profile)

    fun openSuppliers() = suppliersController.openSuppliers()

    fun refreshSuppliers() = suppliersController.refreshSuppliers()

    fun openSupplierDetail(accountId: Long) = suppliersController.openSupplierDetail(accountId)

    fun clearSupplierDetail() = suppliersController.clearSupplierDetail()

    fun openSupplierCreate() = suppliersController.openSupplierCreate()

    fun clearSupplierCreate() = suppliersController.clearSupplierCreate()

    fun createSupplier(name: String) = suppliersController.createSupplier(name)

    fun openSupplierEdit() = suppliersController.openSupplierEdit()

    fun clearSupplierEdit() = suppliersController.clearSupplierEdit()

    fun updateSupplier(name: String, profile: VendorProfileInput) =
        suppliersController.updateSupplier(name, profile)

    fun openEmployees() = employeesController.openEmployees()

    fun refreshEmployees() = employeesController.refreshEmployees()

    fun openEmployeeDetail(accountId: Long) = employeesController.openEmployeeDetail(accountId)

    fun clearEmployeeDetail() = employeesController.clearEmployeeDetail()

    fun openEmployeeCreate() = employeesController.openEmployeeCreate()

    fun clearEmployeeCreate() = employeesController.clearEmployeeCreate()

    fun createEmployee(name: String) = employeesController.createEmployee(name)

    fun openEmployeeEdit() = employeesController.openEmployeeEdit()

    fun clearEmployeeEdit() = employeesController.clearEmployeeEdit()

    fun updateEmployee(name: String, profile: EmployeeProfileInput) =
        employeesController.updateEmployee(name, profile)

    fun openCostingPlans() = costingController.openCostingPlans()

    fun refreshCostingPlans() = costingController.refreshCostingPlans()

    fun setCostingSection(section: NativeOwnerCostingSection) =
        costingController.setCostingSection(section)

    fun openCostingPolicyDetail(policyId: Long) = costingController.openCostingPolicyDetail(policyId)

    fun clearCostingPolicyDetail() = costingController.clearCostingPolicyDetail()

    fun openCostingApprove() = costingController.openCostingApprove()

    fun openCostingApproveFromDetail() = costingController.openCostingApproveFromDetail()

    fun clearCostingApprove() = costingController.clearCostingApprove()

    fun approveCostingPolicy(input: CostingPolicyInput) = costingController.approveCostingPolicy(input)

    fun submitCostingReauth(passphrase: CharArray) = costingController.submitCostingReauth(passphrase)

    fun cancelCostingReauth() = costingController.cancelCostingReauth()

    fun openCostingBudgetEditor() = costingController.openCostingBudgetEditor()

    fun clearCostingBudgetEditor() = costingController.clearCostingBudgetEditor()

    fun upsertCostingBudget(input: BudgetPlanInput) = costingController.upsertCostingBudget(input)

    fun copyPreviousCostingBudget(category: String, periodStart: LocalDate, periodEndExclusive: LocalDate) =
        costingController.copyPreviousCostingBudget(category, periodStart, periodEndExclusive)

    fun openCostingCollectionsEditor() = costingController.openCostingCollectionsEditor()

    fun clearCostingCollectionsEditor() = costingController.clearCostingCollectionsEditor()

    fun upsertCostingCollectionsTarget(weekStart: LocalDate, amount: Double) =
        costingController.upsertCostingCollectionsTarget(weekStart, amount)

    fun openCostingOpeningEditor(productId: Long) =
        costingController.openCostingOpeningEditor(productId)

    fun clearCostingOpeningEditor() = costingController.clearCostingOpeningEditor()

    fun saveCostingOpeningCost(input: OpeningCostBasisInput) =
        costingController.saveCostingOpeningCost(input)

    fun openCostingYieldApprove() = costingController.openCostingYieldApprove()

    fun clearCostingYieldApprove() = costingController.clearCostingYieldApprove()

    fun approveCostingYieldPolicy(input: YieldPolicyInput) =
        costingController.approveCostingYieldPolicy(input)

    fun recordProductionLossAssessment(input: ProductionLossAssessmentInput) =
        costingController.recordProductionLossAssessment(input)

    fun openShopSettings() = shopSettingsController.openShopSettings()

    fun refreshShopSettings() = shopSettingsController.refreshShopSettings()

    fun openShopIdentityEdit() = shopSettingsController.openShopIdentityEdit()

    fun clearShopIdentityEdit() = shopSettingsController.clearShopIdentityEdit()

    fun saveShopIdentity(input: StoreIdentityInput) = shopSettingsController.saveShopIdentity(input)

    fun openShopRatesEdit() = shopSettingsController.openShopRatesEdit()

    fun clearShopRatesEdit() = shopSettingsController.clearShopRatesEdit()

    fun updateShopRates(settings: BusinessSettings) = shopSettingsController.updateShopRates(settings)

    fun openShopCategoryCreate() = shopSettingsController.openShopCategoryCreate()

    fun clearShopCategoryCreate() = shopSettingsController.clearShopCategoryCreate()

    fun createShopExpenseCategory(name: String) =
        shopSettingsController.createShopExpenseCategory(name)

    fun openShopCategoryDetail(accountId: Long) =
        shopSettingsController.openShopCategoryDetail(accountId)

    fun clearShopCategoryDetail() = shopSettingsController.clearShopCategoryDetail()

    fun setShopExpenseCategoryArchived(archived: Boolean) =
        shopSettingsController.setShopExpenseCategoryArchived(archived)

    fun openOpeningEdit(correction: Boolean) = shopSettingsController.openOpeningEdit(correction)

    fun backFromOpeningEdit() = shopSettingsController.backFromOpeningEdit()

    fun saveOpening(input: OpeningStateInput) = shopSettingsController.saveOpening(input)

    fun submitOpeningReauth(passphrase: CharArray) =
        shopSettingsController.submitOpeningReauth(passphrase)

    fun cancelOpeningReauth() = shopSettingsController.cancelOpeningReauth()

    fun openUsersRoles() = usersRolesController.openUsersRoles()

    fun refreshUsersRoles() = usersRolesController.refreshUsersRoles()

    fun openUserDetail(userId: Long) = usersRolesController.openUserDetail(userId)

    fun clearUserDetail() = usersRolesController.clearUserDetail()

    fun openUserEnroll() = usersRolesController.openUserEnroll()

    fun clearUserEnroll() = usersRolesController.clearUserEnroll()

    fun beginUserEnroll(loginName: String, displayName: String, roleWire: String) =
        usersRolesController.beginUserEnroll(loginName, displayName, roleWire)

    fun completeUserEnroll(passphrase: CharArray, confirmation: CharArray) =
        usersRolesController.completeUserEnroll(passphrase, confirmation)

    fun openUserRoleChange() = usersRolesController.openUserRoleChange()

    fun clearUserRoleChange() = usersRolesController.clearUserRoleChange()

    fun changeUserRole(roleWire: String) = usersRolesController.changeUserRole(roleWire)

    fun requestSetUserEnabled(enabled: Boolean) = usersRolesController.requestSetUserEnabled(enabled)

    fun cancelSetUserEnabled() = usersRolesController.cancelSetUserEnabled()

    fun confirmSetUserEnabled() = usersRolesController.confirmSetUserEnabled()

    fun beginCredentialReset() = usersRolesController.beginCredentialReset()

    fun submitUsersRolesReauth(passphrase: CharArray) =
        usersRolesController.submitUsersRolesReauth(passphrase)

    fun cancelUsersRolesReauth() = usersRolesController.cancelUsersRolesReauth()

    fun openSecurityHistory() = securityHistoryController.openSecurityHistory()

    fun refreshSecurityHistory() = securityHistoryController.refreshSecurityHistory()

    fun openSecurityAuditDetail(auditId: Long) =
        securityHistoryController.openSecurityAuditDetail(auditId)

    fun clearSecurityAuditDetail() = securityHistoryController.clearSecurityAuditDetail()

    fun openBackup() = backupController.openBackup()

    fun refreshBackup() = backupController.refreshBackup()

    fun openBackupExport() = backupController.openBackupExport()

    fun clearBackupExport() = backupController.clearBackupExport()

    fun exportBackup(uri: Uri, passphrase: CharArray) = backupController.exportBackup(uri, passphrase)

    fun openBackupRestore() = backupController.openBackupRestore()

    fun clearBackupRestore() = backupController.clearBackupRestore()

    fun inspectBackupRestore(uri: Uri, passphrase: CharArray) =
        backupController.inspectBackupRestore(uri, passphrase)

    fun confirmBackupRestore(confirm: Boolean) = backupController.confirmBackupRestore(confirm)

    fun openBackupConnect() = backupController.openBackupConnect()

    fun clearBackupConnect() = backupController.clearBackupConnect()

    fun connectDriveBackup(passphrase: CharArray, allowMetered: Boolean) =
        backupController.connectDriveBackup(passphrase, allowMetered)

    fun setDriveBackupEnabled(enabled: Boolean) = backupController.setDriveBackupEnabled(enabled)

    fun runDriveBackupNow() = backupController.runDriveBackupNow()

    fun requestBackupDisconnect() = backupController.requestBackupDisconnect()

    fun cancelBackupDisconnect() = backupController.cancelBackupDisconnect()

    fun confirmBackupDisconnect() = backupController.confirmBackupDisconnect()

    fun openBackupCloud() = backupController.openBackupCloud()

    fun backFromBackupCloud() = backupController.backFromBackupCloud()

    fun refreshCloudBackups() = backupController.refreshCloudBackups()

    fun inspectCloudBackup(remoteFileId: String, recoveryPassphrase: CharArray?) =
        backupController.inspectCloudBackup(remoteFileId, recoveryPassphrase)

    fun confirmCloudBackupRestore(confirm: Boolean) =
        backupController.confirmCloudBackupRestore(confirm)

    fun submitBackupReauth(passphrase: CharArray) = backupController.submitBackupReauth(passphrase)

    fun cancelBackupReauth() = backupController.cancelBackupReauth()

    fun backToMenu() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        usersRolesController.resetPending()
        costingController.resetPending()
        shopSettingsController.resetPending()
        personalSecurityController.resetTransient()
        backupController.leave()
        _state.value = current.resetForMenu()
    }

    fun requestRemoveQuickUnlock() = personalSecurityController.requestRemoveQuickUnlock()

    fun cancelRemoveQuickUnlock() = personalSecurityController.cancelRemoveQuickUnlock()

    fun removeQuickUnlock(passphrase: CharArray) =
        personalSecurityController.removeQuickUnlock(passphrase)

    fun enrollQuickPin(passphrase: CharArray, pin: CharArray) =
        personalSecurityController.enrollQuickPin(passphrase, pin)

    fun enrollPhoneSecurity(passphrase: CharArray, pin: CharArray?) =
        personalSecurityController.enrollPhoneSecurity(passphrase, pin)

    override fun onCleared() {
        backupController.leave()
        sessionHandle = null
        scope.cancel()
        super.onCleared()
    }

    private fun expireSession() {
        if (sessionHandle == null) return
        sessionHandle = null
        backupController.leave()
        _state.value = NativeOwnerMoreState.Idle
        onSessionExpired()
    }

    private fun handleLoadFailure(token: OwnerSessionToken, error: Throwable) {
        if (!runtime.isCurrent(token)) return
        if (error is SecurityFailure.SessionExpired) {
            expireSession()
            return
        }
        _state.value = NativeOwnerMoreState.LoadFailure(publicMessage(error))
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is NativeOwnerBackupFailure -> error.message ?: "Backup could not be completed."
        is SecurityFailure -> error.message ?: "More is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "That action could not be completed. Check it and try again."
        else -> "That action could not be completed. Check it and try again."
    }

    class Factory(
        private val facade: NativeOwnerMoreFacade,
        private val productsFacade: NativeOwnerProductsFacade,
        private val customersFacade: NativeOwnerCustomersFacade,
        private val suppliersFacade: NativeOwnerSuppliersFacade,
        private val employeesFacade: NativeOwnerEmployeesFacade,
        private val costingPlansFacade: NativeOwnerCostingPlansFacade,
        private val shopSettingsFacade: NativeOwnerShopSettingsFacade,
        private val usersRolesFacade: NativeOwnerUsersRolesFacade,
        private val securityHistoryFacade: NativeOwnerSecurityHistoryFacade,
        private val backupFacade: NativeOwnerBackupFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOwnerMoreViewModel::class.java))
            return NativeOwnerMoreViewModel(
                dependencies = OwnerMoreDependencies(
                    more = facade,
                    products = productsFacade,
                    customers = customersFacade,
                    suppliers = suppliersFacade,
                    employees = employeesFacade,
                    costing = costingPlansFacade,
                    shopSettings = shopSettingsFacade,
                    usersRoles = usersRolesFacade,
                    securityHistory = securityHistoryFacade,
                    backup = backupFacade,
                ),
                onSessionExpired = onSessionExpired,
            ) as T
        }
    }
}
