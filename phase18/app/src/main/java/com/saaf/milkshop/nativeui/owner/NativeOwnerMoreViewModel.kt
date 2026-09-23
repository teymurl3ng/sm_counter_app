package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.application.owner.NativeOwnerBackupFailure
import com.saaf.milkshop.application.owner.NativeOwnerBackupInspection
import com.saaf.milkshop.application.owner.NativeOwnerBackupSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerBudgetRow
import com.saaf.milkshop.application.owner.NativeOwnerCollectionsTargetRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyDetail
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
import com.saaf.milkshop.application.owner.NativeOwnerYieldBatchRow
import com.saaf.milkshop.application.owner.NativeOwnerYieldPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeDetail
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeRow
import com.saaf.milkshop.application.owner.NativeOwnerEmployeesFacade
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerMoreFacade
import com.saaf.milkshop.application.owner.NativeOwnerProductDetail
import com.saaf.milkshop.application.owner.NativeOwnerProductRow
import com.saaf.milkshop.application.owner.NativeOwnerProductsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditDetail
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditRow
import com.saaf.milkshop.application.owner.NativeOwnerSecurityHistoryFacade
import com.saaf.milkshop.application.owner.NativeOwnerExpenseCategoryRow
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsFacade
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerSupplierDetail
import com.saaf.milkshop.application.owner.NativeOwnerSupplierRow
import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
import com.saaf.milkshop.application.owner.NativeOwnerPendingEnrollment
import com.saaf.milkshop.application.owner.NativeOwnerUserDetail
import com.saaf.milkshop.application.owner.NativeOwnerUserRow
import com.saaf.milkshop.application.owner.NativeOwnerUsersRolesFacade
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.YieldPolicyInput
import java.time.LocalDate
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.SecurityFailure
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NativeOwnerMoreDestination {
    Menu,
    PersonalSecurity,
    Products,
    Customers,
    Suppliers,
    Employees,
    CostingPlans,
    ShopSettings,
    UsersRoles,
    SecurityHistory,
    Backup,
}

enum class NativeOwnerCostingSection {
    Policies,
    Plans,
    Yield,
}

sealed interface NativeOwnerMoreState {
    data object Idle : NativeOwnerMoreState
    data object Loading : NativeOwnerMoreState
    data class Ready(
        val context: NativeOwnerMoreContext,
        val destination: NativeOwnerMoreDestination = NativeOwnerMoreDestination.Menu,
        val security: NativeMoreSecurityStatus? = null,
        val products: List<NativeOwnerProductRow> = emptyList(),
        val productsLoading: Boolean = false,
        val productsShowArchived: Boolean = false,
        val productDetail: NativeOwnerProductDetail? = null,
        val productCreateOpen: Boolean = false,
        val productCreating: Boolean = false,
        val productEditOpen: Boolean = false,
        val productUpdating: Boolean = false,
        val productArchiving: Boolean = false,
        val productUnarchiving: Boolean = false,
        val productsError: String? = null,
        val customers: List<NativeOwnerCustomerRow> = emptyList(),
        val customersLoading: Boolean = false,
        val customerDetail: NativeOwnerCustomerDetail? = null,
        val customerCreateOpen: Boolean = false,
        val customerCreating: Boolean = false,
        val customerEditOpen: Boolean = false,
        val customerUpdating: Boolean = false,
        val customersError: String? = null,
        val suppliers: List<NativeOwnerSupplierRow> = emptyList(),
        val suppliersLoading: Boolean = false,
        val supplierDetail: NativeOwnerSupplierDetail? = null,
        val supplierCreateOpen: Boolean = false,
        val supplierCreating: Boolean = false,
        val supplierEditOpen: Boolean = false,
        val supplierUpdating: Boolean = false,
        val suppliersError: String? = null,
        val employees: List<NativeOwnerEmployeeRow> = emptyList(),
        val employeesLoading: Boolean = false,
        val employeeDetail: NativeOwnerEmployeeDetail? = null,
        val employeeCreateOpen: Boolean = false,
        val employeeCreating: Boolean = false,
        val employeeEditOpen: Boolean = false,
        val employeeUpdating: Boolean = false,
        val employeesError: String? = null,
        val costingPolicies: List<NativeOwnerCostingPolicyRow> = emptyList(),
        val costingPoliciesLoading: Boolean = false,
        val costingPolicyDetail: NativeOwnerCostingPolicyDetail? = null,
        val costingPoliciesError: String? = null,
        val costingCurrentMonthSummary: String? = null,
        val costingSection: NativeOwnerCostingSection = NativeOwnerCostingSection.Policies,
        val costingBudgets: List<NativeOwnerBudgetRow> = emptyList(),
        val costingTargets: List<NativeOwnerCollectionsTargetRow> = emptyList(),
        val costingBudgetCategories: List<String> = emptyList(),
        val costingDefaultMilkWeekStart: String = "",
        val costingDefaultOperatingMonthStart: String = "",
        val costingBudgetEditorOpen: Boolean = false,
        val costingBudgetSaving: Boolean = false,
        val costingCollectionsEditorOpen: Boolean = false,
        val costingCollectionsSaving: Boolean = false,
        val costingYieldPolicies: List<NativeOwnerYieldPolicyRow> = emptyList(),
        val costingYieldBatches: List<NativeOwnerYieldBatchRow> = emptyList(),
        val costingYieldActivePolicyId: Long? = null,
        val costingYieldSummary: String? = null,
        val costingYieldApproveOpen: Boolean = false,
        val costingYieldApproving: Boolean = false,
        val costingApproveOpen: Boolean = false,
        val costingApproveSeed: NativeOwnerCostingPolicyDetail? = null,
        val costingApproving: Boolean = false,
        val costingReauthOpen: Boolean = false,
        val costingReauthBusy: Boolean = false,
        val costingReviewReasons: List<String> = emptyList(),
        val costingComponentCodes: List<String> = emptyList(),
        val shopSettings: NativeOwnerShopSettingsSnapshot? = null,
        val shopSettingsLoading: Boolean = false,
        val shopSettingsIdentityEditOpen: Boolean = false,
        val shopSettingsIdentitySaving: Boolean = false,
        val shopSettingsRatesEditOpen: Boolean = false,
        val shopSettingsRatesSaving: Boolean = false,
        val shopSettingsCategoryCreateOpen: Boolean = false,
        val shopSettingsCategoryCreating: Boolean = false,
        val shopSettingsCategoryDetail: NativeOwnerExpenseCategoryRow? = null,
        val shopSettingsCategoryArchiving: Boolean = false,
        val shopSettingsError: String? = null,
        val shopSettingsOpeningEditOpen: Boolean = false,
        val shopSettingsOpeningCorrection: Boolean = false,
        val shopSettingsOpeningSaving: Boolean = false,
        val shopSettingsOpeningReauthOpen: Boolean = false,
        val shopSettingsOpeningReauthBusy: Boolean = false,
        val users: List<NativeOwnerUserRow> = emptyList(),
        val usersLoading: Boolean = false,
        val userDetail: NativeOwnerUserDetail? = null,
        val userEnrollOpen: Boolean = false,
        val userEnrollPending: NativeOwnerPendingEnrollment? = null,
        val userEnrollBusy: Boolean = false,
        val userRoleChangeOpen: Boolean = false,
        val userRoleChanging: Boolean = false,
        val userEnableConfirmOpen: Boolean = false,
        val userEnableTargetEnabled: Boolean = false,
        val userEnableBusy: Boolean = false,
        val userReauthOpen: Boolean = false,
        val userReauthBusy: Boolean = false,
        val enrollableRoles: List<AppRole> = AppRole.entries.toList(),
        val usersError: String? = null,
        val securityAudits: List<NativeOwnerSecurityAuditRow> = emptyList(),
        val securityHistoryLoading: Boolean = false,
        val securityAuditDetail: NativeOwnerSecurityAuditDetail? = null,
        val securityHistoryError: String? = null,
        val backup: NativeOwnerBackupSnapshot? = null,
        val backupLoading: Boolean = false,
        val backupError: String? = null,
        val backupExportOpen: Boolean = false,
        val backupRestoreOpen: Boolean = false,
        val backupConnectOpen: Boolean = false,
        val backupInspection: NativeOwnerBackupInspection? = null,
        val backupBusy: Boolean = false,
        val backupReauthOpen: Boolean = false,
        val backupReauthBusy: Boolean = false,
        val backupDisconnectConfirmOpen: Boolean = false,
        val backupCloudOpen: Boolean = false,
        val saving: Boolean = false,
        val confirmRemoveUnlock: Boolean = false,
        val error: String? = null,
    ) : NativeOwnerMoreState
    data class LoadFailure(val message: String) : NativeOwnerMoreState
}

class NativeOwnerMoreViewModel(
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
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOwnerMoreState>(NativeOwnerMoreState.Idle)
    private var sessionHandle: String? = null
    private var pendingUsersAction: PendingUsersRolesAction? = null
    private var pendingCostingApprove: CostingPolicyInput? = null
    private var pendingYieldApprove: YieldPolicyInput? = null
    private var pendingBackupAction: PendingBackupAction? = null
    private var pendingOpeningInput: OpeningStateInput? = null
    private var pendingOpeningCorrection: Boolean = false
    val state: StateFlow<NativeOwnerMoreState> = _state.asStateFlow()

    private sealed interface PendingUsersRolesAction {
        data class BeginEnroll(val input: CreateUserInput) : PendingUsersRolesAction
        data class SetEnabled(val userId: Long, val enabled: Boolean) : PendingUsersRolesAction
        data class ChangeRole(val userId: Long, val roleWire: String) : PendingUsersRolesAction
        data class BeginCredentialReset(val userId: Long) : PendingUsersRolesAction
    }

    private sealed interface PendingBackupAction {
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

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            _state.value is NativeOwnerMoreState.Ready
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        sessionHandle = null
        clearPendingBackupSecrets()
        _state.value = NativeOwnerMoreState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        _state.value = NativeOwnerMoreState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                val security = withContext(Dispatchers.IO) { facade.loadSecurity(session) }
                _state.value = NativeOwnerMoreState.Ready(context = context, security = security)
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun openPersonalSecurity() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.PersonalSecurity,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
            productsError = null,
            productsShowArchived = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            customersError = null,
            customerDetail = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            suppliersError = null,
            supplierDetail = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            employeesError = null,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
        )
        scope.launch {
            try {
                val security = withContext(Dispatchers.IO) { facade.loadSecurity(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                _state.value = latest.copy(security = security)
            } catch (error: Throwable) {
                handleReadyFailure(error)
            }
        }
    }

    fun openProducts() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Products,
            productsLoading = true,
            productsShowArchived = false,
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
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    productsFacade.loadProducts(session, includeArchived = false)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productsLoading = false,
                    productsError = null,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun refreshProducts() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        val showArchived = current.productsShowArchived
        _state.value = current.copy(
            productsLoading = true,
            productsError = null,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    productsFacade.loadProducts(session, includeArchived = showArchived)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productsLoading = false,
                    productsError = null,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun setProductsShowArchived(showArchived: Boolean) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        if (current.productsShowArchived == showArchived &&
            !current.productsLoading &&
            current.productDetail == null
        ) {
            return
        }
        _state.value = current.copy(
            productsShowArchived = showArchived,
            productsLoading = true,
            productsError = null,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    productsFacade.loadProducts(session, includeArchived = showArchived)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productsLoading = false,
                    productsError = null,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun openProductDetail(productId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        _state.value = current.copy(
            productsLoading = true,
            productsError = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
        )
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    productsFacade.loadDetail(session, productId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    productDetail = detail,
                    productsLoading = false,
                    productsError = null,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun clearProductDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            productDetail = null,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = null,
            productsLoading = false,
        )
    }

    fun openProductCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        if (current.productsShowArchived) return
        _state.value = current.copy(
            productCreateOpen = true,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productDetail = null,
            productsError = null,
            productsLoading = false,
        )
    }

    fun clearProductCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = null,
        )
    }

    fun createProduct(input: ProductCatalogInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Products || !current.productCreateOpen) return
        if (current.productCreating) return
        _state.value = current.copy(productCreating = true, productsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) { productsFacade.createProduct(session, input) }
                val snapshot = withContext(Dispatchers.IO) {
                    productsFacade.loadProducts(session, includeArchived = false)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productsShowArchived = false,
                    productCreateOpen = false,
                    productCreating = false,
                    productEditOpen = false,
                    productUpdating = false,
                    productArchiving = false,
                    productUnarchiving = false,
                    productsError = null,
                    productDetail = null,
                    productsLoading = false,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun openProductEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        val detail = current.productDetail ?: return
        if (detail.row.archived) return
        _state.value = current.copy(
            productEditOpen = true,
            productUpdating = false,
            productCreateOpen = false,
            productCreating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = null,
            productsLoading = false,
        )
    }

    fun clearProductEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            productEditOpen = false,
            productUpdating = false,
            productsError = null,
        )
    }

    fun updateProduct(input: ProductCatalogInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val productId = current.productDetail?.row?.productId ?: return
        if (current.destination != NativeOwnerMoreDestination.Products || !current.productEditOpen) return
        if (current.productUpdating) return
        val showArchived = current.productsShowArchived
        _state.value = current.copy(productUpdating = true, productsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    productsFacade.updateProduct(session, productId, input)
                }
                val detail = withContext(Dispatchers.IO) {
                    productsFacade.loadDetail(session, productId)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    productsFacade.loadProducts(session, includeArchived = showArchived)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productDetail = detail,
                    productEditOpen = false,
                    productUpdating = false,
                    productsError = null,
                    productsLoading = false,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun archiveProduct() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val productId = current.productDetail?.row?.productId ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        if (current.productDetail?.row?.archived == true) return
        if (current.productArchiving || current.productUnarchiving ||
            current.productEditOpen || current.productCreateOpen
        ) {
            return
        }
        val showArchived = current.productsShowArchived
        _state.value = current.copy(productArchiving = true, productsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    productsFacade.archiveProduct(session, productId)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    productsFacade.loadProducts(session, includeArchived = showArchived)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productDetail = null,
                    productEditOpen = false,
                    productUpdating = false,
                    productArchiving = false,
                    productUnarchiving = false,
                    productsError = null,
                    productsLoading = false,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun unarchiveProduct() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val productId = current.productDetail?.row?.productId ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        if (current.productDetail?.row?.archived != true) return
        if (current.productUnarchiving || current.productArchiving ||
            current.productEditOpen || current.productCreateOpen
        ) {
            return
        }
        val showArchived = current.productsShowArchived
        _state.value = current.copy(productUnarchiving = true, productsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    productsFacade.unarchiveProduct(session, productId)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    productsFacade.loadProducts(session, includeArchived = showArchived)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productDetail = null,
                    productEditOpen = false,
                    productUpdating = false,
                    productArchiving = false,
                    productUnarchiving = false,
                    productsError = null,
                    productsLoading = false,
                )
            } catch (error: Throwable) {
                handleProductsFailure(error)
            }
        }
    }

    fun openCustomers() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Customers,
            customersLoading = true,
            customerDetail = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = null,
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
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { customersFacade.loadCustomers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                _state.value = latest.copy(
                    customers = snapshot.customers,
                    customersLoading = false,
                    customersError = null,
                )
            } catch (error: Throwable) {
                handleCustomersFailure(error)
            }
        }
    }

    fun refreshCustomers() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        _state.value = current.copy(
            customersLoading = true,
            customersError = null,
            customerDetail = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { customersFacade.loadCustomers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                _state.value = latest.copy(
                    customers = snapshot.customers,
                    customersLoading = false,
                    customersError = null,
                )
            } catch (error: Throwable) {
                handleCustomersFailure(error)
            }
        }
    }

    fun openCustomerDetail(accountId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        _state.value = current.copy(
            customersLoading = true,
            customersError = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
        )
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    customersFacade.loadDetail(session, accountId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                _state.value = latest.copy(
                    customerDetail = detail,
                    customersLoading = false,
                    customersError = null,
                )
            } catch (error: Throwable) {
                handleCustomersFailure(error)
            }
        }
    }

    fun clearCustomerDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            customerDetail = null,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
            customersLoading = false,
        )
    }

    fun openCustomerCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        _state.value = current.copy(
            customerCreateOpen = true,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            customerDetail = null,
            customersError = null,
            customersLoading = false,
        )
    }

    fun clearCustomerCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
        )
    }

    fun createCustomer(name: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers || !current.customerCreateOpen) return
        if (current.customerCreating) return
        _state.value = current.copy(customerCreating = true, customersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) { customersFacade.createCustomer(session, name) }
                val snapshot = withContext(Dispatchers.IO) { customersFacade.loadCustomers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                _state.value = latest.copy(
                    customers = snapshot.customers,
                    customerCreateOpen = false,
                    customerCreating = false,
                    customerEditOpen = false,
                    customerUpdating = false,
                    customersError = null,
                    customerDetail = null,
                    customersLoading = false,
                )
            } catch (error: Throwable) {
                handleCustomersFailure(error)
            }
        }
    }

    fun openCustomerEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        if (current.customerDetail == null) return
        _state.value = current.copy(
            customerEditOpen = true,
            customerUpdating = false,
            customerCreateOpen = false,
            customerCreating = false,
            customersError = null,
            customersLoading = false,
        )
    }

    fun clearCustomerEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
        )
    }

    fun updateCustomer(name: String, profile: CustomerProfileInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.customerDetail?.row?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers || !current.customerEditOpen) return
        if (current.customerUpdating) return
        _state.value = current.copy(customerUpdating = true, customersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    customersFacade.updateCustomer(session, accountId, name, profile)
                }
                val detail = withContext(Dispatchers.IO) {
                    customersFacade.loadDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { customersFacade.loadCustomers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                _state.value = latest.copy(
                    customers = snapshot.customers,
                    customerDetail = detail,
                    customerEditOpen = false,
                    customerUpdating = false,
                    customersError = null,
                    customersLoading = false,
                )
            } catch (error: Throwable) {
                handleCustomersFailure(error)
            }
        }
    }

    fun openSuppliers() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Suppliers,
            suppliersLoading = true,
            supplierDetail = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
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
            employeesLoading = false,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { suppliersFacade.loadSuppliers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                _state.value = latest.copy(
                    suppliers = snapshot.suppliers,
                    suppliersLoading = false,
                    suppliersError = null,
                )
            } catch (error: Throwable) {
                handleSuppliersFailure(error)
            }
        }
    }

    fun refreshSuppliers() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        _state.value = current.copy(
            suppliersLoading = true,
            suppliersError = null,
            supplierDetail = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { suppliersFacade.loadSuppliers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                _state.value = latest.copy(
                    suppliers = snapshot.suppliers,
                    suppliersLoading = false,
                    suppliersError = null,
                )
            } catch (error: Throwable) {
                handleSuppliersFailure(error)
            }
        }
    }

    fun openSupplierDetail(accountId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        _state.value = current.copy(
            suppliersLoading = true,
            suppliersError = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
        )
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    suppliersFacade.loadDetail(session, accountId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                _state.value = latest.copy(
                    supplierDetail = detail,
                    suppliersLoading = false,
                    suppliersError = null,
                )
            } catch (error: Throwable) {
                handleSuppliersFailure(error)
            }
        }
    }

    fun clearSupplierDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            supplierDetail = null,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
            suppliersLoading = false,
        )
    }

    fun openSupplierCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        _state.value = current.copy(
            supplierCreateOpen = true,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            supplierDetail = null,
            suppliersError = null,
            suppliersLoading = false,
        )
    }

    fun clearSupplierCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
        )
    }

    fun createSupplier(name: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers || !current.supplierCreateOpen) return
        if (current.supplierCreating) return
        _state.value = current.copy(supplierCreating = true, suppliersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) { suppliersFacade.createSupplier(session, name) }
                val snapshot = withContext(Dispatchers.IO) { suppliersFacade.loadSuppliers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                _state.value = latest.copy(
                    suppliers = snapshot.suppliers,
                    supplierCreateOpen = false,
                    supplierCreating = false,
                    supplierEditOpen = false,
                    supplierUpdating = false,
                    suppliersError = null,
                    supplierDetail = null,
                    suppliersLoading = false,
                )
            } catch (error: Throwable) {
                handleSuppliersFailure(error)
            }
        }
    }

    fun openSupplierEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        if (current.supplierDetail == null) return
        _state.value = current.copy(
            supplierEditOpen = true,
            supplierUpdating = false,
            supplierCreateOpen = false,
            supplierCreating = false,
            suppliersError = null,
            suppliersLoading = false,
        )
    }

    fun clearSupplierEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
        )
    }

    fun updateSupplier(name: String, profile: VendorProfileInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.supplierDetail?.row?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers || !current.supplierEditOpen) return
        if (current.supplierUpdating) return
        _state.value = current.copy(supplierUpdating = true, suppliersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    suppliersFacade.updateSupplier(session, accountId, name, profile)
                }
                val detail = withContext(Dispatchers.IO) {
                    suppliersFacade.loadDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { suppliersFacade.loadSuppliers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                _state.value = latest.copy(
                    suppliers = snapshot.suppliers,
                    supplierDetail = detail,
                    supplierEditOpen = false,
                    supplierUpdating = false,
                    suppliersError = null,
                    suppliersLoading = false,
                )
            } catch (error: Throwable) {
                handleSuppliersFailure(error)
            }
        }
    }

    fun openEmployees() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Employees,
            employeesLoading = true,
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
            costingCurrentMonthSummary = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            shopSettingsLoading = false,
            shopSettings = null,
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
            backup = null,
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
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { employeesFacade.loadEmployees(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                _state.value = latest.copy(
                    employees = snapshot.employees,
                    employeesLoading = false,
                    employeesError = null,
                )
            } catch (error: Throwable) {
                handleEmployeesFailure(error)
            }
        }
    }

    fun refreshEmployees() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        _state.value = current.copy(
            employeesLoading = true,
            employeesError = null,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { employeesFacade.loadEmployees(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                _state.value = latest.copy(
                    employees = snapshot.employees,
                    employeesLoading = false,
                    employeesError = null,
                )
            } catch (error: Throwable) {
                handleEmployeesFailure(error)
            }
        }
    }

    fun openEmployeeDetail(accountId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        _state.value = current.copy(
            employeesLoading = true,
            employeesError = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
        )
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    employeesFacade.loadDetail(session, accountId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                _state.value = latest.copy(
                    employeeDetail = detail,
                    employeesLoading = false,
                    employeesError = null,
                )
            } catch (error: Throwable) {
                handleEmployeesFailure(error)
            }
        }
    }

    fun clearEmployeeDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            employeeDetail = null,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
            employeesLoading = false,
        )
    }

    fun openEmployeeCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        _state.value = current.copy(
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
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
        )
    }

    fun createEmployee(name: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees || !current.employeeCreateOpen) return
        if (current.employeeCreating) return
        _state.value = current.copy(employeeCreating = true, employeesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) { employeesFacade.createEmployee(session, name) }
                val snapshot = withContext(Dispatchers.IO) { employeesFacade.loadEmployees(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                _state.value = latest.copy(
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
                handleEmployeesFailure(error)
            }
        }
    }

    fun openEmployeeEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees) return
        if (current.employeeDetail == null) return
        _state.value = current.copy(
            employeeEditOpen = true,
            employeeUpdating = false,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeesError = null,
            employeesLoading = false,
        )
    }

    fun clearEmployeeEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
        )
    }

    fun updateEmployee(name: String, profile: EmployeeProfileInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.employeeDetail?.row?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.Employees || !current.employeeEditOpen) return
        if (current.employeeUpdating) return
        _state.value = current.copy(employeeUpdating = true, employeesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    employeesFacade.updateEmployee(session, accountId, name, profile)
                }
                val detail = withContext(Dispatchers.IO) {
                    employeesFacade.loadDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { employeesFacade.loadEmployees(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Employees) return@launch
                _state.value = latest.copy(
                    employees = snapshot.employees,
                    employeeDetail = detail,
                    employeeEditOpen = false,
                    employeeUpdating = false,
                    employeesError = null,
                    employeesLoading = false,
                )
            } catch (error: Throwable) {
                handleEmployeesFailure(error)
            }
        }
    }

    fun openCostingPlans() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingCostingApprove = null
        pendingYieldApprove = null
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.CostingPlans,
            costingPoliciesLoading = true,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            costingSection = NativeOwnerCostingSection.Policies,
            costingBudgets = emptyList(),
            costingTargets = emptyList(),
            costingBudgetEditorOpen = false,
            costingBudgetSaving = false,
            costingCollectionsEditorOpen = false,
            costingCollectionsSaving = false,
            costingYieldPolicies = emptyList(),
            costingYieldBatches = emptyList(),
            costingYieldActivePolicyId = null,
            costingYieldSummary = null,
            costingYieldApproveOpen = false,
            costingYieldApproving = false,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
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
            shopSettingsLoading = false,
            shopSettings = null,
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
            backup = null,
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
        scope.launch {
            try {
                val (snapshot, planning, yieldContext) = withContext(Dispatchers.IO) {
                    Triple(
                        costingPlansFacade.loadPolicies(session, current.context.businessDate),
                        costingPlansFacade.loadPlanning(session),
                        costingPlansFacade.loadYieldContext(session),
                    )
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingYieldPolicies = yieldContext.policies,
                    costingYieldBatches = yieldContext.batches,
                    costingYieldActivePolicyId = yieldContext.activePolicyId,
                    costingYieldSummary = yieldContext.activeSummary,
                )
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            }
        }
    }

    fun refreshCostingPlans() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        _state.value = current.copy(
            costingPoliciesLoading = true,
            costingPoliciesError = null,
            costingPolicyDetail = null,
        )
        scope.launch {
            try {
                val (snapshot, planning, yieldContext) = withContext(Dispatchers.IO) {
                    Triple(
                        costingPlansFacade.loadPolicies(session, current.context.businessDate),
                        costingPlansFacade.loadPlanning(session),
                        costingPlansFacade.loadYieldContext(session),
                    )
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingYieldPolicies = yieldContext.policies,
                    costingYieldBatches = yieldContext.batches,
                    costingYieldActivePolicyId = yieldContext.activePolicyId,
                    costingYieldSummary = yieldContext.activeSummary,
                )
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            }
        }
    }

    fun setCostingSection(section: NativeOwnerCostingSection) {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        if (current.costingApproveOpen ||
            current.costingReauthOpen ||
            current.costingBudgetEditorOpen ||
            current.costingCollectionsEditorOpen ||
            current.costingYieldApproveOpen ||
            current.costingPolicyDetail != null
        ) {
            return
        }
        _state.value = current.copy(costingSection = section, costingPoliciesError = null)
    }

    fun openCostingPolicyDetail(policyId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        _state.value = current.copy(costingPoliciesLoading = true, costingPoliciesError = null)
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadDetail(session, policyId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingPolicyDetail = detail,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            }
        }
    }

    fun clearCostingPolicyDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.costingApproveOpen || current.costingReauthOpen) return
        _state.value = current.copy(
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingPoliciesLoading = false,
        )
    }

    fun openCostingApprove() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        pendingCostingApprove = null
        _state.value = current.copy(
            costingApproveOpen = true,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingReviewReasons = costingPlansFacade.reviewReasons(),
            costingComponentCodes = costingPlansFacade.componentCodes(),
            costingPoliciesError = null,
        )
    }

    fun openCostingApproveFromDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.costingPolicyDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        if (detail.row.statusLabel != "Active") return
        pendingCostingApprove = null
        _state.value = current.copy(
            costingApproveOpen = true,
            costingApproveSeed = detail,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingReviewReasons = costingPlansFacade.reviewReasons(),
            costingComponentCodes = costingPlansFacade.componentCodes(),
            costingPoliciesError = null,
        )
    }

    fun clearCostingApprove() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingCostingApprove = null
        _state.value = current.copy(
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingPoliciesError = null,
        )
    }

    fun approveCostingPolicy(input: CostingPolicyInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingApproveOpen ||
            current.costingApproving
        ) {
            return
        }
        _state.value = current.copy(costingApproving = true, costingPoliciesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    costingPlansFacade.approvePolicy(session, input)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadPolicies(session, current.context.businessDate)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                pendingCostingApprove = null
                _state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
                    costingPolicyDetail = null,
                    costingApproveOpen = false,
                    costingApproveSeed = null,
                    costingApproving = false,
                    costingReauthOpen = false,
                    costingReauthBusy = false,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingCostingApprove = input
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        costingApproving = false,
                        costingReauthOpen = true,
                        costingReauthBusy = false,
                        costingPoliciesError = null,
                    )
                } else {
                    handleCostingPlansFailure(error)
                }
            }
        }
    }

    fun submitCostingReauth(passphrase: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingReauthOpen ||
            current.costingReauthBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        val pendingPolicy = pendingCostingApprove
        val pendingYield = pendingYieldApprove
        if (pendingPolicy == null && pendingYield == null) {
            passphrase.fill('\u0000')
            _state.value = current.copy(costingReauthOpen = false, costingPoliciesError = null)
            return
        }
        _state.value = current.copy(costingReauthBusy = true, costingPoliciesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    costingPlansFacade.reauthenticate(session, passphrase)
                }
                if (pendingPolicy != null) {
                    withContext(Dispatchers.IO) {
                        costingPlansFacade.approvePolicy(session, pendingPolicy)
                    }
                    val snapshot = withContext(Dispatchers.IO) {
                        costingPlansFacade.loadPolicies(session, current.context.businessDate)
                    }
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                    pendingCostingApprove = null
                    pendingYieldApprove = null
                    _state.value = latest.copy(
                        costingPolicies = snapshot.policies,
                        costingCurrentMonthSummary = snapshot.currentMonthSummary,
                        costingPolicyDetail = null,
                        costingApproveOpen = false,
                        costingApproveSeed = null,
                        costingApproving = false,
                        costingReauthOpen = false,
                        costingReauthBusy = false,
                        costingPoliciesLoading = false,
                        costingPoliciesError = null,
                    )
                } else if (pendingYield != null) {
                    withContext(Dispatchers.IO) {
                        costingPlansFacade.approveYieldPolicy(session, pendingYield)
                    }
                    val yieldContext = withContext(Dispatchers.IO) {
                        costingPlansFacade.loadYieldContext(session)
                    }
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                    pendingCostingApprove = null
                    pendingYieldApprove = null
                    _state.value = latest.copy(
                        costingYieldPolicies = yieldContext.policies,
                        costingYieldBatches = yieldContext.batches,
                        costingYieldActivePolicyId = yieldContext.activePolicyId,
                        costingYieldSummary = yieldContext.activeSummary,
                        costingYieldApproveOpen = false,
                        costingYieldApproving = false,
                        costingReauthOpen = false,
                        costingReauthBusy = false,
                        costingPoliciesError = null,
                    )
                }
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun cancelCostingReauth() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingCostingApprove = null
        pendingYieldApprove = null
        _state.value = current.copy(
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingApproving = false,
            costingYieldApproving = false,
        )
    }

    fun openCostingBudgetEditor() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        _state.value = current.copy(
            costingSection = NativeOwnerCostingSection.Plans,
            costingBudgetEditorOpen = true,
            costingBudgetSaving = false,
            costingPoliciesError = null,
        )
    }

    fun clearCostingBudgetEditor() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            costingBudgetEditorOpen = false,
            costingBudgetSaving = false,
            costingPoliciesError = null,
        )
    }

    fun upsertCostingBudget(input: BudgetPlanInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingBudgetEditorOpen ||
            current.costingBudgetSaving
        ) {
            return
        }
        _state.value = current.copy(costingBudgetSaving = true, costingPoliciesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    costingPlansFacade.upsertBudget(session, input)
                }
                val planning = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadPlanning(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingBudgetEditorOpen = false,
                    costingBudgetSaving = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            }
        }
    }

    fun copyPreviousCostingBudget(category: String, periodStart: LocalDate, periodEndExclusive: LocalDate) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingBudgetEditorOpen ||
            current.costingBudgetSaving
        ) {
            return
        }
        _state.value = current.copy(costingBudgetSaving = true, costingPoliciesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    costingPlansFacade.copyPreviousBudget(session, category, periodStart, periodEndExclusive)
                }
                val planning = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadPlanning(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingBudgetEditorOpen = false,
                    costingBudgetSaving = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            }
        }
    }

    fun openCostingCollectionsEditor() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        _state.value = current.copy(
            costingSection = NativeOwnerCostingSection.Plans,
            costingCollectionsEditorOpen = true,
            costingCollectionsSaving = false,
            costingPoliciesError = null,
        )
    }

    fun clearCostingCollectionsEditor() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            costingCollectionsEditorOpen = false,
            costingCollectionsSaving = false,
            costingPoliciesError = null,
        )
    }

    fun upsertCostingCollectionsTarget(weekStart: LocalDate, amount: Double) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingCollectionsEditorOpen ||
            current.costingCollectionsSaving
        ) {
            return
        }
        _state.value = current.copy(costingCollectionsSaving = true, costingPoliciesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    costingPlansFacade.createOrReviseCollectionsTarget(session, weekStart, amount)
                }
                val planning = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadPlanning(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingBudgets = planning.budgets,
                    costingTargets = planning.collectionTargets,
                    costingBudgetCategories = planning.budgetCategories,
                    costingDefaultMilkWeekStart = planning.defaultMilkWeekStart,
                    costingDefaultOperatingMonthStart = planning.defaultOperatingMonthStart,
                    costingCollectionsEditorOpen = false,
                    costingCollectionsSaving = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            }
        }
    }

    fun openCostingYieldApprove() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans) return
        pendingYieldApprove = null
        _state.value = current.copy(
            costingSection = NativeOwnerCostingSection.Yield,
            costingYieldApproveOpen = true,
            costingYieldApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingPoliciesError = null,
        )
    }

    fun clearCostingYieldApprove() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingYieldApprove = null
        _state.value = current.copy(
            costingYieldApproveOpen = false,
            costingYieldApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            costingPoliciesError = null,
        )
    }

    fun approveCostingYieldPolicy(input: YieldPolicyInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.CostingPlans ||
            !current.costingYieldApproveOpen ||
            current.costingYieldApproving
        ) {
            return
        }
        _state.value = current.copy(costingYieldApproving = true, costingPoliciesError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    costingPlansFacade.approveYieldPolicy(session, input)
                }
                val yieldContext = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadYieldContext(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                pendingYieldApprove = null
                _state.value = latest.copy(
                    costingYieldPolicies = yieldContext.policies,
                    costingYieldBatches = yieldContext.batches,
                    costingYieldActivePolicyId = yieldContext.activePolicyId,
                    costingYieldSummary = yieldContext.activeSummary,
                    costingYieldApproveOpen = false,
                    costingYieldApproving = false,
                    costingReauthOpen = false,
                    costingReauthBusy = false,
                    costingPoliciesError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingYieldApprove = input
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        costingYieldApproving = false,
                        costingReauthOpen = true,
                        costingReauthBusy = false,
                        costingPoliciesError = null,
                    )
                } else {
                    handleCostingPlansFailure(error)
                }
            }
        }
    }

    fun openShopSettings() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.ShopSettings,
            shopSettingsLoading = true,
            shopSettings = null,
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
            costingCurrentMonthSummary = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
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
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsLoading = false,
                    shopSettingsError = null,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }

    fun refreshShopSettings() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        _state.value = current.copy(shopSettingsLoading = true, shopSettingsError = null)
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsLoading = false,
                    shopSettingsError = null,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }

    fun openShopIdentityEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettings == null) return
        _state.value = current.copy(
            shopSettingsIdentityEditOpen = true,
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
            shopSettingsLoading = false,
        )
    }

    fun clearShopIdentityEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsError = null,
        )
    }

    fun saveShopIdentity(input: StoreIdentityInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings ||
            !current.shopSettingsIdentityEditOpen
        ) {
            return
        }
        if (current.shopSettingsIdentitySaving) return
        _state.value = current.copy(shopSettingsIdentitySaving = true, shopSettingsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) { shopSettingsFacade.saveIdentity(session, input) }
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsIdentityEditOpen = false,
                    shopSettingsIdentitySaving = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }

    fun openShopRatesEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettings == null) return
        _state.value = current.copy(
            shopSettingsRatesEditOpen = true,
            shopSettingsRatesSaving = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
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
            shopSettingsLoading = false,
        )
    }

    fun clearShopRatesEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsError = null,
        )
    }

    fun updateShopRates(settings: BusinessSettings) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings ||
            !current.shopSettingsRatesEditOpen
        ) {
            return
        }
        if (current.shopSettingsRatesSaving) return
        _state.value = current.copy(shopSettingsRatesSaving = true, shopSettingsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) { shopSettingsFacade.updateRates(session, settings) }
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsRatesEditOpen = false,
                    shopSettingsRatesSaving = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }

    fun openShopCategoryCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        _state.value = current.copy(
            shopSettingsCategoryCreateOpen = true,
            shopSettingsCategoryCreating = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsLoading = false,
        )
    }

    fun clearShopCategoryCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsError = null,
        )
    }

    fun createShopExpenseCategory(name: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings ||
            !current.shopSettingsCategoryCreateOpen
        ) {
            return
        }
        if (current.shopSettingsCategoryCreating) return
        _state.value = current.copy(shopSettingsCategoryCreating = true, shopSettingsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    shopSettingsFacade.createExpenseCategory(session, name)
                }
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsCategoryCreateOpen = false,
                    shopSettingsCategoryCreating = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }

    fun openShopCategoryDetail(accountId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        _state.value = current.copy(
            shopSettingsLoading = true,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsError = null,
        )
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    shopSettingsFacade.loadCategoryDetail(session, accountId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                _state.value = latest.copy(
                    shopSettingsCategoryDetail = detail,
                    shopSettingsLoading = false,
                    shopSettingsError = null,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }

    fun clearShopCategoryDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsLoading = false,
        )
    }

    fun setShopExpenseCategoryArchived(archived: Boolean) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.shopSettingsCategoryDetail?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettingsCategoryArchiving) return
        _state.value = current.copy(shopSettingsCategoryArchiving = true, shopSettingsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    shopSettingsFacade.setExpenseCategoryArchived(session, accountId, archived)
                }
                val detail = withContext(Dispatchers.IO) {
                    shopSettingsFacade.loadCategoryDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsCategoryDetail = detail,
                    shopSettingsCategoryArchiving = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }


    fun openOpeningEdit(correction: Boolean) {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettings == null) return
        pendingOpeningInput = null
        pendingOpeningCorrection = correction
        _state.value = current.copy(
            shopSettingsOpeningEditOpen = true,
            shopSettingsOpeningCorrection = correction,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryDetail = null,
            shopSettingsError = null,
        )
    }

    fun backFromOpeningEdit() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingOpeningInput = null
        pendingOpeningCorrection = false
        _state.value = current.copy(
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsError = null,
        )
    }

    fun saveOpening(input: OpeningStateInput) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (!current.shopSettingsOpeningEditOpen || current.shopSettingsOpeningSaving) return
        val correction = current.shopSettingsOpeningCorrection
        _state.value = current.copy(shopSettingsOpeningSaving = true, shopSettingsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (correction) {
                        shopSettingsFacade.correctOpening(session, input)
                    } else {
                        shopSettingsFacade.recordOpening(session, input)
                    }
                }
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                pendingOpeningInput = null
                pendingOpeningCorrection = false
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsOpeningEditOpen = false,
                    shopSettingsOpeningCorrection = false,
                    shopSettingsOpeningSaving = false,
                    shopSettingsOpeningReauthOpen = false,
                    shopSettingsOpeningReauthBusy = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingOpeningInput = input
                    pendingOpeningCorrection = correction
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        shopSettingsOpeningSaving = false,
                        shopSettingsOpeningReauthOpen = true,
                        shopSettingsOpeningReauthBusy = false,
                        shopSettingsError = null,
                    )
                } else {
                    handleShopSettingsFailure(error)
                }
            }
        }
    }

    fun submitOpeningReauth(passphrase: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (!current.shopSettingsOpeningReauthOpen || current.shopSettingsOpeningReauthBusy) {
            passphrase.fill('\u0000')
            return
        }
        val pending = pendingOpeningInput
        if (pending == null) {
            passphrase.fill('\u0000')
            _state.value = current.copy(
                shopSettingsOpeningReauthOpen = false,
                shopSettingsOpeningReauthBusy = false,
            )
            return
        }
        val correction = pendingOpeningCorrection
        _state.value = current.copy(shopSettingsOpeningReauthBusy = true, shopSettingsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    shopSettingsFacade.reauthenticate(session, passphrase)
                }
                withContext(Dispatchers.IO) {
                    if (correction) {
                        shopSettingsFacade.correctOpening(session, pending)
                    } else {
                        shopSettingsFacade.recordOpening(session, pending)
                    }
                }
                val snapshot = withContext(Dispatchers.IO) { shopSettingsFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                pendingOpeningInput = null
                pendingOpeningCorrection = false
                _state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsOpeningEditOpen = false,
                    shopSettingsOpeningCorrection = false,
                    shopSettingsOpeningSaving = false,
                    shopSettingsOpeningReauthOpen = false,
                    shopSettingsOpeningReauthBusy = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleShopSettingsFailure(error)
            }
        }
    }

    fun cancelOpeningReauth() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingOpeningInput = null
        _state.value = current.copy(
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsOpeningSaving = false,
            shopSettingsError = null,
        )
    }

    fun openUsersRoles() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.UsersRoles,
            usersLoading = true,
            userDetail = null,
            userEnrollOpen = false,
            userEnrollPending = null,
            userEnrollBusy = false,
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userEnableTargetEnabled = false,
            userEnableBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
            enrollableRoles = emptyList(),
            usersError = null,
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
            costingCurrentMonthSummary = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            shopSettingsLoading = false,
            shopSettings = null,
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
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
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
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { usersRolesFacade.loadUsers(session) }
                val roles = withContext(Dispatchers.IO) { usersRolesFacade.enrollableRoles(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                _state.value = latest.copy(
                    users = snapshot.users,
                    usersLoading = false,
                    enrollableRoles = roles,
                    usersError = null,
                )
            } catch (error: Throwable) {
                handleUsersRolesFailure(error)
            }
        }
    }

    fun refreshUsersRoles() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles) return
        _state.value = current.copy(
            usersLoading = true,
            usersError = null,
            userDetail = null,
            userEnrollOpen = false,
            userEnrollPending = null,
            userEnrollBusy = false,
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userEnableBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
        )
        pendingUsersAction = null
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { usersRolesFacade.loadUsers(session) }
                val roles = withContext(Dispatchers.IO) { usersRolesFacade.enrollableRoles(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                _state.value = latest.copy(
                    users = snapshot.users,
                    usersLoading = false,
                    enrollableRoles = roles,
                    usersError = null,
                )
            } catch (error: Throwable) {
                handleUsersRolesFailure(error)
            }
        }
    }

    fun openUserDetail(userId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles) return
        _state.value = current.copy(
            usersLoading = true,
            usersError = null,
            userEnrollOpen = false,
            userEnrollPending = null,
            userRoleChangeOpen = false,
            userEnableConfirmOpen = false,
            userReauthOpen = false,
        )
        pendingUsersAction = null
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    usersRolesFacade.loadDetail(session, userId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                _state.value = latest.copy(
                    userDetail = detail,
                    usersLoading = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                handleUsersRolesFailure(error)
            }
        }
    }

    fun clearUserDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        _state.value = current.copy(
            userDetail = null,
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userEnableBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
            usersLoading = false,
        )
    }

    fun openUserEnroll() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles) return
        pendingUsersAction = null
        _state.value = current.copy(
            userEnrollOpen = true,
            userEnrollPending = null,
            userEnrollBusy = false,
            userDetail = null,
            userRoleChangeOpen = false,
            userEnableConfirmOpen = false,
            userReauthOpen = false,
            usersError = null,
        )
    }

    fun clearUserEnroll() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        _state.value = current.copy(
            userEnrollOpen = false,
            userEnrollPending = null,
            userEnrollBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
        )
    }

    fun beginUserEnroll(loginName: String, displayName: String, roleWire: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userEnrollOpen ||
            current.userEnrollBusy
        ) {
            return
        }
        val input = CreateUserInput(
            loginName = loginName,
            displayName = displayName,
            role = roleWire,
        )
        _state.value = current.copy(userEnrollBusy = true, usersError = null)
        scope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    usersRolesFacade.beginEnroll(session, input)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                _state.value = latest.copy(
                    userEnrollPending = pending,
                    userEnrollBusy = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.BeginEnroll(input)
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        userEnrollBusy = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleUsersRolesFailure(error)
                }
            }
        }
    }

    fun completeUserEnroll(passphrase: CharArray, confirmation: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val pending = current.userEnrollPending
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            pending == null ||
            current.userEnrollBusy
        ) {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            return
        }
        if (!passphrase.contentEquals(confirmation)) {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            _state.value = current.copy(usersError = "Passphrases do not match")
            return
        }
        confirmation.fill('\u0000')
        _state.value = current.copy(userEnrollBusy = true, usersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    usersRolesFacade.completeEnroll(pending.enrollmentToken, passphrase)
                }
                val snapshot = withContext(Dispatchers.IO) { usersRolesFacade.loadUsers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                _state.value = latest.copy(
                    users = snapshot.users,
                    userEnrollOpen = false,
                    userEnrollPending = null,
                    userEnrollBusy = false,
                    usersLoading = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                handleUsersRolesFailure(error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun openUserRoleChange() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            current.userDetail == null
        ) {
            return
        }
        pendingUsersAction = null
        _state.value = current.copy(
            userRoleChangeOpen = true,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userReauthOpen = false,
            usersError = null,
        )
    }

    fun clearUserRoleChange() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        _state.value = current.copy(
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
        )
    }

    fun changeUserRole(roleWire: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.userDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userRoleChangeOpen ||
            current.userRoleChanging
        ) {
            return
        }
        _state.value = current.copy(userRoleChanging = true, usersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    usersRolesFacade.changeRole(session, detail.row.userId, roleWire)
                }
                val refreshed = withContext(Dispatchers.IO) {
                    usersRolesFacade.loadDetail(session, detail.row.userId)
                }
                val snapshot = withContext(Dispatchers.IO) { usersRolesFacade.loadUsers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                _state.value = latest.copy(
                    users = snapshot.users,
                    userDetail = refreshed,
                    userRoleChangeOpen = false,
                    userRoleChanging = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.ChangeRole(
                        userId = detail.row.userId,
                        roleWire = roleWire,
                    )
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        userRoleChanging = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleUsersRolesFailure(error)
                }
            }
        }
    }

    fun requestSetUserEnabled(enabled: Boolean) {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            current.userDetail == null
        ) {
            return
        }
        _state.value = current.copy(
            userEnableConfirmOpen = true,
            userEnableTargetEnabled = enabled,
            usersError = null,
        )
    }

    fun cancelSetUserEnabled() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            userEnableConfirmOpen = false,
            userEnableBusy = false,
        )
    }

    fun confirmSetUserEnabled() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.userDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userEnableConfirmOpen ||
            current.userEnableBusy
        ) {
            return
        }
        val enabled = current.userEnableTargetEnabled
        _state.value = current.copy(userEnableBusy = true, usersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    usersRolesFacade.setEnabled(session, detail.row.userId, enabled)
                }
                val refreshed = withContext(Dispatchers.IO) {
                    usersRolesFacade.loadDetail(session, detail.row.userId)
                }
                val snapshot = withContext(Dispatchers.IO) { usersRolesFacade.loadUsers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                _state.value = latest.copy(
                    users = snapshot.users,
                    userDetail = refreshed,
                    userEnableConfirmOpen = false,
                    userEnableBusy = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.SetEnabled(
                        userId = detail.row.userId,
                        enabled = enabled,
                    )
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        userEnableBusy = false,
                        userEnableConfirmOpen = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleUsersRolesFailure(error)
                }
            }
        }
    }


    fun beginCredentialReset() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.userDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles || current.userEnrollBusy) return
        _state.value = current.copy(userEnrollBusy = true, usersError = null)
        scope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    usersRolesFacade.beginCredentialReset(session, detail.row.userId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                _state.value = latest.copy(
                    userEnrollOpen = true,
                    userEnrollPending = pending,
                    userEnrollBusy = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.BeginCredentialReset(
                        userId = detail.row.userId,
                    )
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        userEnrollBusy = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleUsersRolesFailure(error)
                }
            }
        }
    }

    fun submitUsersRolesReauth(passphrase: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userReauthOpen ||
            current.userReauthBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        val pending = pendingUsersAction
        if (pending == null) {
            passphrase.fill('\u0000')
            _state.value = current.copy(userReauthOpen = false, usersError = null)
            return
        }
        _state.value = current.copy(userReauthBusy = true, usersError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    usersRolesFacade.reauthenticate(session, passphrase)
                }
                when (pending) {
                    is PendingUsersRolesAction.BeginEnroll -> {
                        val enrollPending = withContext(Dispatchers.IO) {
                            usersRolesFacade.beginEnroll(session, pending.input)
                        }
                        val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        pendingUsersAction = null
                        _state.value = latest.copy(
                            userEnrollPending = enrollPending,
                            userEnrollBusy = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                    is PendingUsersRolesAction.SetEnabled -> {
                        withContext(Dispatchers.IO) {
                            usersRolesFacade.setEnabled(session, pending.userId, pending.enabled)
                        }
                        val refreshed = withContext(Dispatchers.IO) {
                            usersRolesFacade.loadDetail(session, pending.userId)
                        }
                        val snapshot = withContext(Dispatchers.IO) {
                            usersRolesFacade.loadUsers(session)
                        }
                        val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        pendingUsersAction = null
                        _state.value = latest.copy(
                            users = snapshot.users,
                            userDetail = refreshed,
                            userEnableConfirmOpen = false,
                            userEnableBusy = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                    is PendingUsersRolesAction.BeginCredentialReset -> {
                        val resetPending = withContext(Dispatchers.IO) {
                            usersRolesFacade.beginCredentialReset(session, pending.userId)
                        }
                        val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        pendingUsersAction = null
                        _state.value = latest.copy(
                            userEnrollOpen = true,
                            userEnrollPending = resetPending,
                            userEnrollBusy = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                    is PendingUsersRolesAction.ChangeRole -> {
                        withContext(Dispatchers.IO) {
                            usersRolesFacade.changeRole(session, pending.userId, pending.roleWire)
                        }
                        val refreshed = withContext(Dispatchers.IO) {
                            usersRolesFacade.loadDetail(session, pending.userId)
                        }
                        val snapshot = withContext(Dispatchers.IO) {
                            usersRolesFacade.loadUsers(session)
                        }
                        val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        pendingUsersAction = null
                        _state.value = latest.copy(
                            users = snapshot.users,
                            userDetail = refreshed,
                            userRoleChangeOpen = false,
                            userRoleChanging = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                }
            } catch (error: Throwable) {
                handleUsersRolesFailure(error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun cancelUsersRolesReauth() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        _state.value = current.copy(
            userReauthOpen = false,
            userReauthBusy = false,
            userEnrollBusy = false,
            userRoleChanging = false,
            userEnableBusy = false,
            usersError = null,
        )
    }

    fun openSecurityHistory() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.SecurityHistory,
            securityHistoryLoading = true,
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
            costingCurrentMonthSummary = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            shopSettingsLoading = false,
            shopSettings = null,
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
            backup = null,
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
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    securityHistoryFacade.loadAudits(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.SecurityHistory) return@launch
                _state.value = latest.copy(
                    securityAudits = snapshot.audits,
                    securityHistoryLoading = false,
                    securityHistoryError = null,
                )
            } catch (error: Throwable) {
                handleSecurityHistoryFailure(error)
            }
        }
    }

    fun refreshSecurityHistory() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.SecurityHistory) return
        _state.value = current.copy(
            securityHistoryLoading = true,
            securityHistoryError = null,
            securityAuditDetail = null,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    securityHistoryFacade.loadAudits(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.SecurityHistory) return@launch
                _state.value = latest.copy(
                    securityAudits = snapshot.audits,
                    securityHistoryLoading = false,
                    securityHistoryError = null,
                )
            } catch (error: Throwable) {
                handleSecurityHistoryFailure(error)
            }
        }
    }

    fun openSecurityAuditDetail(auditId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.SecurityHistory) return
        _state.value = current.copy(securityHistoryLoading = true, securityHistoryError = null)
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    securityHistoryFacade.loadDetail(session, auditId)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.SecurityHistory) return@launch
                _state.value = latest.copy(
                    securityAuditDetail = detail,
                    securityHistoryLoading = false,
                    securityHistoryError = null,
                )
            } catch (error: Throwable) {
                handleSecurityHistoryFailure(error)
            }
        }
    }

    fun clearSecurityAuditDetail() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            securityAuditDetail = null,
            securityHistoryError = null,
            securityHistoryLoading = false,
        )
    }

    fun openBackup() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        current.backupInspection?.restoreTicket?.let { ticket ->
            backupFacade.cancelRestoreTicket(ticket)
            backupFacade.cancelCloudRestoreTicket(ticket)
        }
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Backup,
            backupLoading = true,
            backup = null,
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
            costingCurrentMonthSummary = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            shopSettingsLoading = false,
            shopSettings = null,
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
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { backupFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backup = snapshot,
                    backupLoading = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                handleBackupFailure(error)
            }
        }
    }

    fun refreshBackup() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        _state.value = current.copy(backupLoading = true, backupError = null)
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { backupFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backup = snapshot,
                    backupLoading = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                handleBackupFailure(error)
            }
        }
    }

    fun openBackupExport() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        clearPendingBackupSecrets()
        _state.value = current.copy(
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
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        _state.value = current.copy(
            backupExportOpen = false,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupError = null,
        )
    }

    fun exportBackup(uri: Uri, passphrase: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupExportOpen ||
            current.backupBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backupFacade.exportEncrypted(session, uri, passphrase)
                }
                val snapshot = withContext(Dispatchers.IO) { backupFacade.load(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                clearPendingBackupSecrets()
                _state.value = latest.copy(
                    backup = snapshot,
                    backupExportOpen = false,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.Export(uri, passphrase.copyOf())
                    passphrase.fill('\u0000')
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    passphrase.fill('\u0000')
                    handleBackupFailure(error)
                }
            }
        }
    }

    fun openBackupRestore() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        clearPendingBackupSecrets()
        current.backupInspection?.restoreTicket?.let { ticket ->
            backupFacade.cancelRestoreTicket(ticket)
            backupFacade.cancelCloudRestoreTicket(ticket)
        }
        _state.value = current.copy(
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
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        current.backupInspection?.restoreTicket?.let { ticket ->
            backupFacade.cancelRestoreTicket(ticket)
            backupFacade.cancelCloudRestoreTicket(ticket)
        }
        _state.value = current.copy(
            backupRestoreOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupError = null,
        )
    }

    fun inspectBackupRestore(uri: Uri, passphrase: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupRestoreOpen ||
            current.backupBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                val inspection = withContext(Dispatchers.IO) {
                    backupFacade.inspectEncrypted(session, uri, passphrase)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backupInspection = inspection,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.Inspect(uri, passphrase.copyOf())
                    passphrase.fill('\u0000')
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    passphrase.fill('\u0000')
                    handleBackupFailure(error)
                }
            }
        }
    }

    fun confirmBackupRestore(confirm: Boolean) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val ticket = current.backupInspection?.restoreTicket ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupRestoreOpen ||
            current.backupBusy
        ) {
            return
        }
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backupFacade.confirmRestore(session, ticket, confirm)
                }
                clearPendingBackupSecrets()
                sessionHandle = null
                _state.value = NativeOwnerMoreState.Idle
                onSessionExpired()
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.ConfirmRestore(confirm)
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleBackupFailure(error)
                }
            }
        }
    }

    fun openBackupConnect() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        clearPendingBackupSecrets()
        _state.value = current.copy(
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
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        _state.value = current.copy(
            backupConnectOpen = false,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupError = null,
        )
    }

    fun connectDriveBackup(passphrase: CharArray, allowMetered: Boolean) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup ||
            !current.backupConnectOpen ||
            current.backupBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    backupFacade.connectDrive(session, passphrase, allowMetered)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                clearPendingBackupSecrets()
                _state.value = latest.copy(
                    backup = snapshot,
                    backupConnectOpen = false,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.Connect(passphrase.copyOf(), allowMetered)
                    passphrase.fill('\u0000')
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    passphrase.fill('\u0000')
                    handleBackupFailure(error)
                }
            }
        }
    }

    fun setDriveBackupEnabled(enabled: Boolean) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        val allowMetered = current.backup?.allowMetered ?: false
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    backupFacade.setDrivePolicy(session, enabled, allowMetered)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.SetPolicy(enabled)
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleBackupFailure(error)
                }
            }
        }
    }

    fun runDriveBackupNow() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    backupFacade.runDriveBackupNow(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                handleBackupFailure(error)
            }
        }
    }

    fun requestBackupDisconnect() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        _state.value = current.copy(backupDisconnectConfirmOpen = true, backupError = null)
    }

    fun cancelBackupDisconnect() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(backupDisconnectConfirmOpen = false)
    }

    fun confirmBackupDisconnect() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        _state.value = current.copy(
            backupBusy = true,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            backupError = null,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    backupFacade.disconnectDrive(session, confirmRevoke = true)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.Disconnect
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleBackupFailure(error)
                }
            }
        }
    }


    fun openBackupCloud() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup) return
        current.backupInspection?.restoreTicket?.let(backupFacade::cancelCloudRestoreTicket)
        _state.value = current.copy(
            backupCloudOpen = true,
            backupExportOpen = false,
            backupRestoreOpen = false,
            backupConnectOpen = false,
            backupInspection = null,
            backupError = null,
        )
    }

    fun backFromBackupCloud() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        current.backupInspection?.restoreTicket?.let(backupFacade::cancelCloudRestoreTicket)
        _state.value = current.copy(
            backupCloudOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupError = null,
        )
    }

    fun refreshCloudBackups() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    backupFacade.refreshCloudSnapshots(session)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backup = snapshot,
                    backupBusy = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                handleBackupFailure(error)
            }
        }
    }

    fun inspectCloudBackup(remoteFileId: String, recoveryPassphrase: CharArray?) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) {
            recoveryPassphrase?.fill('\u0000')
            return
        }
        current.backupInspection?.restoreTicket?.let(backupFacade::cancelCloudRestoreTicket)
        _state.value = current.copy(backupBusy = true, backupError = null, backupInspection = null)
        scope.launch {
            try {
                val inspection = withContext(Dispatchers.IO) {
                    backupFacade.inspectCloudBackup(session, remoteFileId, recoveryPassphrase)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Backup) return@launch
                _state.value = latest.copy(
                    backupInspection = inspection,
                    backupBusy = false,
                    backupReauthOpen = false,
                    backupError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.InspectCloud(
                        remoteFileId = remoteFileId,
                        recoveryPassphrase = recoveryPassphrase,
                    )
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    recoveryPassphrase?.fill('\u0000')
                    handleBackupFailure(error)
                }
            }
        }
    }

    fun confirmCloudBackupRestore(confirm: Boolean) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        val ticket = current.backupInspection?.restoreTicket ?: return
        if (current.destination != NativeOwnerMoreDestination.Backup || current.backupBusy) return
        _state.value = current.copy(backupBusy = true, backupError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backupFacade.confirmCloudRestore(session, ticket, confirm)
                }
                onSessionExpired()
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingBackupAction = PendingBackupAction.ConfirmCloudRestore(confirm)
                    val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    _state.value = latest.copy(
                        backupBusy = false,
                        backupReauthOpen = true,
                        backupReauthBusy = false,
                        backupError = null,
                    )
                } else {
                    handleBackupFailure(error)
                }
            }
        }
    }

    fun submitBackupReauth(passphrase: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (!current.backupReauthOpen || current.backupReauthBusy) {
            passphrase.fill('\u0000')
            return
        }
        val pending = pendingBackupAction
        if (pending == null) {
            passphrase.fill('\u0000')
            _state.value = current.copy(backupReauthOpen = false, backupReauthBusy = false)
            return
        }
        val retryAction = pending
        _state.value = current.copy(backupReauthBusy = true, backupError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backupFacade.reauthenticate(session, passphrase)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                _state.value = latest.copy(
                    backupReauthOpen = false,
                    backupReauthBusy = false,
                    backupError = null,
                )
                when (retryAction) {
                    is PendingBackupAction.Export -> {
                        pendingBackupAction = null
                        exportBackup(retryAction.uri, retryAction.passphrase)
                    }
                    is PendingBackupAction.Inspect -> {
                        pendingBackupAction = null
                        inspectBackupRestore(retryAction.uri, retryAction.passphrase)
                    }
                    is PendingBackupAction.ConfirmRestore -> {
                        pendingBackupAction = null
                        confirmBackupRestore(retryAction.confirm)
                    }
                    is PendingBackupAction.Connect -> {
                        pendingBackupAction = null
                        connectDriveBackup(retryAction.passphrase, retryAction.allowMetered)
                    }
                    is PendingBackupAction.SetPolicy -> {
                        pendingBackupAction = null
                        setDriveBackupEnabled(retryAction.enabled)
                    }
                    PendingBackupAction.Disconnect -> {
                        pendingBackupAction = null
                        confirmBackupDisconnect()
                    }
                    is PendingBackupAction.InspectCloud -> {
                        pendingBackupAction = null
                        inspectCloudBackup(retryAction.remoteFileId, retryAction.recoveryPassphrase)
                    }
                    is PendingBackupAction.ConfirmCloudRestore -> {
                        pendingBackupAction = null
                        confirmCloudBackupRestore(retryAction.confirm)
                    }
                    PendingBackupAction.RefreshCloud -> {
                        pendingBackupAction = null
                        refreshCloudBackups()
                    }
                }
            } catch (error: Throwable) {
                passphrase.fill('\u0000')
                handleBackupFailure(error)
            }
        }
    }

    fun cancelBackupReauth() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        clearPendingBackupSecrets()
        _state.value = current.copy(
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupBusy = false,
            backupError = null,
        )
    }

    private fun clearPendingBackupSecrets() {
        when (val pending = pendingBackupAction) {
            is PendingBackupAction.Export -> pending.passphrase.fill('\u0000')
            is PendingBackupAction.Inspect -> pending.passphrase.fill('\u0000')
            is PendingBackupAction.Connect -> pending.passphrase.fill('\u0000')
            is PendingBackupAction.InspectCloud -> pending.recoveryPassphrase?.fill('\u0000')
            else -> Unit
        }
        pendingBackupAction = null
    }

    fun backToMenu() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        clearPendingBackupSecrets()
        current.backupInspection?.restoreTicket?.let { ticket ->
            backupFacade.cancelRestoreTicket(ticket)
            backupFacade.cancelCloudRestoreTicket(ticket)
        }
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Menu,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
            productsLoading = false,
            productsShowArchived = false,
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
            costingCurrentMonthSummary = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            shopSettingsLoading = false,
            shopSettings = null,
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
            userEnrollOpen = false,
            userEnrollPending = null,
            userEnrollBusy = false,
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userEnableBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
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
        )
    }

    fun requestRemoveQuickUnlock() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(confirmRemoveUnlock = true, error = null)
    }

    fun cancelRemoveQuickUnlock() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(confirmRemoveUnlock = false)
    }

    fun removeQuickUnlock(passphrase: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                val security = withContext(Dispatchers.IO) {
                    facade.removeQuickUnlock(session, passphrase)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                _state.value = latest.copy(
                    security = security,
                    saving = false,
                    confirmRemoveUnlock = false,
                    error = null,
                )
            } catch (error: Throwable) {
                handleReadyFailure(error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun enrollQuickPin(passphrase: CharArray, pin: CharArray) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                val security = withContext(Dispatchers.IO) {
                    facade.enrollQuickUnlock(
                        sessionHandle = session,
                        currentPassphrase = passphrase,
                        enablePhoneSecurity = current.security?.phoneSecurityEnabled == true,
                        pin = pin,
                    )
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                _state.value = latest.copy(security = security, saving = false, error = null)
            } catch (error: Throwable) {
                handleReadyFailure(error)
            } finally {
                passphrase.fill('\u0000')
                pin.fill('\u0000')
            }
        }
    }

    fun enrollPhoneSecurity(passphrase: CharArray, pin: CharArray?) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.saving) return
        if (current.security?.quickPinEnabled == true && (pin == null || pin.size != 4)) {
            _state.value = current.copy(
                error = "Enter your 4-digit Quick PIN to keep it while enabling phone security.",
            )
            passphrase.fill('\u0000')
            pin?.fill('\u0000')
            return
        }
        _state.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                val security = withContext(Dispatchers.IO) {
                    facade.enrollQuickUnlock(
                        sessionHandle = session,
                        currentPassphrase = passphrase,
                        enablePhoneSecurity = true,
                        pin = pin,
                    )
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                _state.value = latest.copy(security = security, saving = false, error = null)
            } catch (error: Throwable) {
                handleReadyFailure(error)
            } finally {
                passphrase.fill('\u0000')
                pin?.fill('\u0000')
            }
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        _state.value = NativeOwnerMoreState.LoadFailure(publicMessage(error))
    }

    private fun handleReadyFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(saving = false, error = publicMessage(error))
    }

    private fun handleProductsFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            productsLoading = false,
            productCreating = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = publicMessage(error),
        )
    }

    private fun handleCustomersFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            customersLoading = false,
            customerCreating = false,
            customerUpdating = false,
            customersError = publicMessage(error),
        )
    }

    private fun handleSuppliersFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            suppliersLoading = false,
            supplierCreating = false,
            supplierUpdating = false,
            suppliersError = publicMessage(error),
        )
    }

    private fun handleEmployeesFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            employeesLoading = false,
            employeeCreating = false,
            employeeUpdating = false,
            employeesError = publicMessage(error),
        )
    }

    private fun handleCostingPlansFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            pendingCostingApprove = null
            pendingYieldApprove = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            costingPoliciesLoading = false,
            costingApproving = false,
            costingReauthBusy = false,
            costingBudgetSaving = false,
            costingCollectionsSaving = false,
            costingYieldApproving = false,
            costingPoliciesError = publicMessage(error),
        )
    }

    private fun handleShopSettingsFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            shopSettingsLoading = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesSaving = false,
            shopSettingsCategoryCreating = false,
            shopSettingsCategoryArchiving = false,
                    shopSettingsOpeningSaving = false,
                    shopSettingsOpeningReauthBusy = false,
            shopSettingsError = publicMessage(error),
        )
    }

    private fun handleUsersRolesFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            pendingUsersAction = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            usersLoading = false,
            userEnrollBusy = false,
            userRoleChanging = false,
            userEnableBusy = false,
            userEnableConfirmOpen = false,
            userReauthBusy = false,
            usersError = publicMessage(error),
        )
    }

    private fun handleSecurityHistoryFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            securityHistoryLoading = false,
            securityHistoryError = publicMessage(error),
        )
    }

    private fun handleBackupFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            clearPendingBackupSecrets()
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            backupLoading = false,
            backupBusy = false,
            backupReauthBusy = false,
            backupError = publicMessage(error),
        )
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
                facade,
                productsFacade,
                customersFacade,
                suppliersFacade,
                employeesFacade,
                costingPlansFacade,
                shopSettingsFacade,
                usersRolesFacade,
                securityHistoryFacade,
                backupFacade,
                onSessionExpired,
            ) as T
        }
    }
}
