package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerMoreFacade
import com.saaf.milkshop.application.owner.NativeOwnerProductDetail
import com.saaf.milkshop.application.owner.NativeOwnerProductRow
import com.saaf.milkshop.application.owner.NativeOwnerProductsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSupplierDetail
import com.saaf.milkshop.application.owner.NativeOwnerSupplierRow
import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
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
        val productsError: String? = null,
        val customers: List<NativeOwnerCustomerRow> = emptyList(),
        val customersLoading: Boolean = false,
        val customerDetail: NativeOwnerCustomerDetail? = null,
        val customersError: String? = null,
        val suppliers: List<NativeOwnerSupplierRow> = emptyList(),
        val suppliersLoading: Boolean = false,
        val supplierDetail: NativeOwnerSupplierDetail? = null,
        val suppliersError: String? = null,
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
            customersError = null,
            customerDetail = null,
            suppliersError = null,
            supplierDetail = null,
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
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
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
        _state.value = current.copy(productsLoading = true, productsError = null, productDetail = null)
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
        _state.value = current.copy(productsLoading = true, productsError = null)
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
        _state.value = current.copy(productDetail = null, productsError = null, productsLoading = false)
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
            productsError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
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
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
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

    fun backToMenu() {
        val current = _state.value as? NativeOwnerMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOwnerMoreDestination.Menu,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
            productsLoading = false,
            productDetail = null,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            suppliersError = null,
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
                onSessionExpired,
            ) as T
        }
    }
}
