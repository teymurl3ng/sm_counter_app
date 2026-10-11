package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
import com.saaf.milkshop.domain.VendorProfileInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns supplier list, detail, and editor behavior for Owner More. */
internal class OwnerSuppliersController(
    private val facade: NativeOwnerSuppliersFacade,
    private val runtime: OwnerMoreRuntime,
) {
    fun openSuppliers() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.Suppliers,
            suppliersLoading = NativeOwnerMoreDestination.Suppliers !in current.loadedDestinations,
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
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadSuppliers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                runtime.state.value = latest.copy(
                    suppliers = snapshot.suppliers,
                    suppliersLoading = false,
                    suppliersError = null,
                    loadedDestinations = latest.loadedDestinations + NativeOwnerMoreDestination.Suppliers,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshSuppliers() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        runtime.state.value = current.copy(
            suppliersLoading = true,
            suppliersError = null,
            supplierDetail = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
        )
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadSuppliers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                runtime.state.value = latest.copy(
                    suppliers = snapshot.suppliers,
                    suppliersLoading = false,
                    suppliersError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openSupplierDetail(accountId: Long) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        runtime.state.value = current.copy(
            suppliersLoading = true,
            suppliersError = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
        )
        runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, accountId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                runtime.state.value = latest.copy(
                    supplierDetail = detail,
                    suppliersLoading = false,
                    suppliersError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearSupplierDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            supplierDetail = null,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
            suppliersLoading = false,
        )
    }

    fun openSupplierCreate() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        runtime.state.value = current.copy(
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
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
        )
    }

    fun createSupplier(name: String) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers || !current.supplierCreateOpen) return
        if (current.supplierCreating) return
        runtime.state.value = current.copy(supplierCreating = true, suppliersError = null)
        runtime.scope.launch {
            var saved = false
            try {
                withContext(Dispatchers.IO) { facade.createSupplier(session, name) }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Suppliers) { it.copy(supplierCreateOpen = false, supplierCreating = false) }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Supplier added")
                com.saaf.milkshop.nativeui.DataChangeBus.notifyDataChanged()
                val snapshot = withContext(Dispatchers.IO) { facade.loadSuppliers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                runtime.state.value = latest.copy(
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
                handleFailure(token, if (saved && error !is com.saaf.milkshop.domain.SecurityFailure) IllegalStateException("Saved on this phone. The latest list could not refresh; try Refresh.", error) else error)
            }
        }
    }

    fun openSupplierEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers) return
        if (current.supplierDetail == null) return
        runtime.state.value = current.copy(
            supplierEditOpen = true,
            supplierUpdating = false,
            supplierCreateOpen = false,
            supplierCreating = false,
            suppliersError = null,
            suppliersLoading = false,
        )
    }

    fun clearSupplierEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
        )
    }

    fun updateSupplier(name: String, profile: VendorProfileInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.supplierDetail?.row?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.Suppliers || !current.supplierEditOpen) return
        if (current.supplierUpdating) return
        runtime.state.value = current.copy(supplierUpdating = true, suppliersError = null)
        runtime.scope.launch {
            var saved = false
            try {
                withContext(Dispatchers.IO) {
                    facade.updateSupplier(session, accountId, name, profile)
                }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Suppliers) { it.copy(supplierEditOpen = false, supplierUpdating = false) }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Supplier saved")
                com.saaf.milkshop.nativeui.DataChangeBus.notifyDataChanged()
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { facade.loadSuppliers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Suppliers) return@launch
                runtime.state.value = latest.copy(
                    suppliers = snapshot.suppliers,
                    supplierDetail = detail,
                    supplierEditOpen = false,
                    supplierUpdating = false,
                    suppliersError = null,
                    suppliersLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, if (saved && error !is com.saaf.milkshop.domain.SecurityFailure) IllegalStateException("Saved on this phone. The latest list could not refresh; try Refresh.", error) else error)
            }
        }
    }


    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.Suppliers, error) { current, message ->
            current.copy(
                suppliersLoading = false,
                supplierCreating = false,
                supplierUpdating = false,
                suppliersError = message,
            )
        }
    }
}
