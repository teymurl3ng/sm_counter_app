package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.application.owner.NativeOwnerBackupSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyDetail
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
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
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsFacade
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerSupplierDetail
import com.saaf.milkshop.application.owner.NativeOwnerSupplierRow
import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
import com.saaf.milkshop.application.owner.NativeOwnerUserDetail
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.application.owner.NativeOwnerUserRow
import com.saaf.milkshop.application.owner.NativeOwnerUsersRolesFacade
import com.saaf.milkshop.domain.SecurityFailure
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

sealed interface NativeOwnerMoreState {
    data object Idle : NativeOwnerMoreState
    data object Loading : NativeOwnerMoreState
    data class Ready(
        val context: NativeOwnerMoreContext,
        val destination: NativeOwnerMoreDestination = NativeOwnerMoreDestination.Menu,
        val security: NativeMoreSecurityStatus? = null,
        val products: List<NativeOwnerProductRow> = emptyList(),
        val productsLoading: Boolean = false,
        val productDetail: NativeOwnerProductDetail? = null,
        val productCreateOpen: Boolean = false,
        val productCreating: Boolean = false,
        val productEditOpen: Boolean = false,
        val productUpdating: Boolean = false,
        val productArchiving: Boolean = false,
        val productsError: String? = null,
        val customers: List<NativeOwnerCustomerRow> = emptyList(),
        val customersLoading: Boolean = false,
        val customerDetail: NativeOwnerCustomerDetail? = null,
        val customersError: String? = null,
        val suppliers: List<NativeOwnerSupplierRow> = emptyList(),
        val suppliersLoading: Boolean = false,
        val supplierDetail: NativeOwnerSupplierDetail? = null,
        val suppliersError: String? = null,
        val employees: List<NativeOwnerEmployeeRow> = emptyList(),
        val employeesLoading: Boolean = false,
        val employeeDetail: NativeOwnerEmployeeDetail? = null,
        val employeesError: String? = null,
        val costingPolicies: List<NativeOwnerCostingPolicyRow> = emptyList(),
        val costingPoliciesLoading: Boolean = false,
        val costingPolicyDetail: NativeOwnerCostingPolicyDetail? = null,
        val costingPoliciesError: String? = null,
        val costingCurrentMonthSummary: String? = null,
        val shopSettings: NativeOwnerShopSettingsSnapshot? = null,
        val shopSettingsLoading: Boolean = false,
        val shopSettingsError: String? = null,
        val users: List<NativeOwnerUserRow> = emptyList(),
        val usersLoading: Boolean = false,
        val userDetail: NativeOwnerUserDetail? = null,
        val usersError: String? = null,
        val securityAudits: List<NativeOwnerSecurityAuditRow> = emptyList(),
        val securityHistoryLoading: Boolean = false,
        val securityAuditDetail: NativeOwnerSecurityAuditDetail? = null,
        val securityHistoryError: String? = null,
        val backup: NativeOwnerBackupSnapshot? = null,
        val backupLoading: Boolean = false,
        val backupError: String? = null,
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
    val state: StateFlow<NativeOwnerMoreState> = _state.asStateFlow()

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
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            customersError = null,
            customerDetail = null,
            suppliersError = null,
            supplierDetail = null,
            employeesError = null,
            employeeDetail = null,
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
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeesError = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { productsFacade.loadProducts(session) }
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
        _state.value = current.copy(
            productsLoading = true,
            productsError = null,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { productsFacade.loadProducts(session) }
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
            productsError = null,
            productsLoading = false,
        )
    }

    fun openProductCreate() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        _state.value = current.copy(
            productCreateOpen = true,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
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
                val snapshot = withContext(Dispatchers.IO) { productsFacade.loadProducts(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productCreateOpen = false,
                    productCreating = false,
                    productEditOpen = false,
                    productUpdating = false,
                    productArchiving = false,
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
        if (current.productDetail == null) return
        _state.value = current.copy(
            productEditOpen = true,
            productUpdating = false,
            productCreateOpen = false,
            productCreating = false,
            productArchiving = false,
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
        _state.value = current.copy(productUpdating = true, productsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    productsFacade.updateProduct(session, productId, input)
                }
                val detail = withContext(Dispatchers.IO) {
                    productsFacade.loadDetail(session, productId)
                }
                val snapshot = withContext(Dispatchers.IO) { productsFacade.loadProducts(session) }
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
        if (current.productArchiving || current.productEditOpen || current.productCreateOpen) return
        _state.value = current.copy(productArchiving = true, productsError = null)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    productsFacade.archiveProduct(session, productId)
                }
                val snapshot = withContext(Dispatchers.IO) { productsFacade.loadProducts(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Products) return@launch
                _state.value = latest.copy(
                    products = snapshot.products,
                    productDetail = null,
                    productEditOpen = false,
                    productUpdating = false,
                    productArchiving = false,
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
            customersError = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
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
        _state.value = current.copy(customersLoading = true, customersError = null, customerDetail = null)
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
        _state.value = current.copy(customersLoading = true, customersError = null)
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
        _state.value = current.copy(customerDetail = null, customersError = null, customersLoading = false)
    }

    fun openSuppliers() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Suppliers,
            suppliersLoading = true,
            supplierDetail = null,
            suppliersError = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            employeesLoading = false,
            employeeDetail = null,
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
        _state.value = current.copy(suppliersLoading = true, suppliersError = null, supplierDetail = null)
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
        _state.value = current.copy(suppliersLoading = true, suppliersError = null)
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
        _state.value = current.copy(supplierDetail = null, suppliersError = null, suppliersLoading = false)
    }

    fun openEmployees() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Employees,
            employeesLoading = true,
            employeeDetail = null,
            employeesError = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            shopSettingsLoading = false,
            shopSettings = null,
            shopSettingsError = null,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
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
        _state.value = current.copy(employeesLoading = true, employeesError = null, employeeDetail = null)
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
        _state.value = current.copy(employeesLoading = true, employeesError = null)
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
        _state.value = current.copy(employeeDetail = null, employeesError = null, employeesLoading = false)
    }

    fun openCostingPlans() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.CostingPlans,
            costingPoliciesLoading = true,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeesError = null,
            shopSettingsLoading = false,
            shopSettings = null,
            shopSettingsError = null,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadPolicies(session, current.context.businessDate)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
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
                val snapshot = withContext(Dispatchers.IO) {
                    costingPlansFacade.loadPolicies(session, current.context.businessDate)
                }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.CostingPlans) return@launch
                _state.value = latest.copy(
                    costingPolicies = snapshot.policies,
                    costingPoliciesLoading = false,
                    costingPoliciesError = null,
                    costingCurrentMonthSummary = snapshot.currentMonthSummary,
                )
            } catch (error: Throwable) {
                handleCostingPlansFailure(error)
            }
        }
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
        _state.value = current.copy(
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingPoliciesLoading = false,
        )
    }

    fun openShopSettings() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.ShopSettings,
            shopSettingsLoading = true,
            shopSettings = null,
            shopSettingsError = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeesError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
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

    fun openUsersRoles() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.UsersRoles,
            usersLoading = true,
            userDetail = null,
            usersError = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeesError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            shopSettingsLoading = false,
            shopSettings = null,
            shopSettingsError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { usersRolesFacade.loadUsers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                _state.value = latest.copy(
                    users = snapshot.users,
                    usersLoading = false,
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
        _state.value = current.copy(usersLoading = true, usersError = null, userDetail = null)
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { usersRolesFacade.loadUsers(session) }
                val latest = _state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                _state.value = latest.copy(
                    users = snapshot.users,
                    usersLoading = false,
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
        _state.value = current.copy(usersLoading = true, usersError = null)
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
        _state.value = current.copy(userDetail = null, usersError = null, usersLoading = false)
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
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeesError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            shopSettingsLoading = false,
            shopSettings = null,
            shopSettingsError = null,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
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
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Backup,
            backupLoading = true,
            backup = null,
            backupError = null,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeesError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            shopSettingsLoading = false,
            shopSettings = null,
            shopSettingsError = null,
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

    fun backToMenu() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Menu,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeesError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            shopSettingsLoading = false,
            shopSettings = null,
            shopSettingsError = null,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
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
            employeesError = publicMessage(error),
        )
    }

    private fun handleCostingPlansFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            costingPoliciesLoading = false,
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
            shopSettingsError = publicMessage(error),
        )
    }

    private fun handleUsersRolesFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            usersLoading = false,
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
            _state.value = NativeOwnerMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            backupLoading = false,
            backupError = publicMessage(error),
        )
    }

    private fun publicMessage(error: Throwable): String = when (error) {
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
