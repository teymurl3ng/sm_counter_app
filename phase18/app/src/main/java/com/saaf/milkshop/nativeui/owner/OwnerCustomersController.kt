package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.domain.CustomerProfileInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns customer list, detail, and editor behavior for Owner More. */
internal class OwnerCustomersController(
    private val facade: NativeOwnerCustomersFacade,
    private val runtime: OwnerMoreRuntime,
    private val enableCredit: suspend () -> Unit = {},
) {
    fun openCustomers() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.Customers,
            customersLoading = NativeOwnerMoreDestination.Customers !in current.loadedDestinations,
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
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadCustomers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                runtime.state.value = latest.copy(
                    customers = snapshot.customers,
                    customersLoading = false,
                    customersError = null,
                    loadedDestinations = latest.loadedDestinations + NativeOwnerMoreDestination.Customers,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshCustomers() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        runtime.state.value = current.copy(
            customersLoading = true,
            customersError = null,
            customerDetail = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
        )
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadCustomers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                runtime.state.value = latest.copy(
                    customers = snapshot.customers,
                    customersLoading = false,
                    customersError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openCustomerDetail(accountId: Long) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        runtime.state.value = current.copy(
            customersLoading = true,
            customersError = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
        )
        runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, accountId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                runtime.state.value = latest.copy(
                    customerDetail = detail,
                    customersLoading = false,
                    customersError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearCustomerDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            customerDetail = null,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
            customersLoading = false,
        )
    }

    suspend fun loadStatement(accountId: Long, period: com.saaf.milkshop.application.owner.CustomerStatementPeriod): com.saaf.milkshop.application.owner.CustomerStatement {
        val token = runtime.captureSession() ?: error("Sign in again to prepare a statement.")
        fun selected(): Boolean = runtime.isCurrent(token) && runtime.ready()?.let {
            it.destination == NativeOwnerMoreDestination.Customers && it.customerDetail?.row?.accountId == accountId && !it.customerEditOpen
        } == true
        check(selected()) { "Open this customer again before preparing a statement." }
        return try {
            val result = withContext(Dispatchers.IO) { facade.loadStatement(token.handle, accountId, period) }
            check(selected()) { "The selected customer or session changed. Open the customer again." }
            result
        } catch (error: com.saaf.milkshop.domain.SecurityFailure.SessionExpired) {
            if (runtime.isCurrent(token)) runtime.endSession()
            throw error
        }
    }

    fun openCustomerCreate() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        runtime.state.value = current.copy(
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
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
        )
    }

    fun createCustomer(name: String) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers || !current.customerCreateOpen) return
        if (current.customerCreating) return
        runtime.state.value = current.copy(customerCreating = true, customersError = null)
        runtime.scope.launch {
            var saved = false
            try {
                withContext(Dispatchers.IO) { facade.createCustomer(session, name) }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Customers) { it.copy(customerCreateOpen = false, customerCreating = false) }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Customer added")
                com.saaf.milkshop.nativeui.DataChangeBus.notifyDataChanged()
                val snapshot = withContext(Dispatchers.IO) { facade.loadCustomers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                runtime.state.value = latest.copy(
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
                handleFailure(token, if (saved && error !is com.saaf.milkshop.domain.SecurityFailure) IllegalStateException("Saved on this phone. The latest list could not refresh; try Refresh.", error) else error)
            }
        }
    }

    fun openCustomerEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers) return
        if (current.customerDetail == null) return
        runtime.state.value = current.copy(
            customerEditOpen = true,
            customerUpdating = false,
            customerCreateOpen = false,
            customerCreating = false,
            customersError = null,
            customersLoading = false,
        )
    }

    fun clearCustomerEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
        )
    }

    fun updateCustomer(name: String, profile: CustomerProfileInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.customerDetail?.row?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.Customers || !current.customerEditOpen) return
        if (current.customerUpdating) return
        runtime.state.value = current.copy(customerUpdating = true, customersError = null)
        runtime.scope.launch {
            var saved = false
            try {
                withContext(Dispatchers.IO) {
                    facade.updateCustomer(session, accountId, name, profile)
                }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Customers) { it.copy(customerEditOpen = false, customerUpdating = false) }
                saved = true
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Customer saved")
                com.saaf.milkshop.nativeui.DataChangeBus.notifyDataChanged()
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { facade.loadCustomers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.Customers) return@launch
                runtime.state.value = latest.copy(
                    customers = snapshot.customers,
                    customerDetail = detail,
                    customerEditOpen = false,
                    customerUpdating = false,
                    customersError = null,
                    customersLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, if (saved && error !is com.saaf.milkshop.domain.SecurityFailure) IllegalStateException("Saved on this phone. The latest list could not refresh; try Refresh.", error) else error)
            }
        }
    }


    fun creditAction(action: com.saaf.milkshop.application.owner.CustomerCreditCommand) {
        val token=runtime.captureSession()?:return
        val current=runtime.ready()?:return
        val id=current.customerDetail?.row?.accountId?:return
        if(current.customerUpdating || current.destination!=NativeOwnerMoreDestination.Customers) return
        runtime.state.value=current.copy(customerUpdating=true,customersError=null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.creditAction(token.handle,id,action)
                    if(action==com.saaf.milkshop.application.owner.CustomerCreditCommand.Enable) enableCredit()
                }
                val detail=withContext(Dispatchers.IO) { facade.loadDetail(token.handle,id) }
                val snapshot=withContext(Dispatchers.IO) { facade.loadCustomers(token.handle) }
                runtime.updateCurrent(token,NativeOwnerMoreDestination.Customers) {
                    if(it.customerDetail?.row?.accountId==id) it.copy(customerDetail=detail,customers=snapshot.customers,customerUpdating=false,customersError=null)
                    else it.copy(customerUpdating=false)
                }
                if(runtime.isCurrent(token)) com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved(
                    when(action) {
                        com.saaf.milkshop.application.owner.CustomerCreditCommand.Enable -> "Credit terms enabled"
                        is com.saaf.milkshop.application.owner.CustomerCreditCommand.Terms -> "Terms proposal saved. Effective after sync acceptance."
                        is com.saaf.milkshop.application.owner.CustomerCreditCommand.Retry -> "Held change queued for retry"
                        is com.saaf.milkshop.application.owner.CustomerCreditCommand.DiscardTerm -> "Held terms proposal discarded"
                        else -> "Review action saved. Pending sync acceptance."
                    })
            } catch(error: Throwable) { handleFailure(token,error) }
        }
    }

    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.Customers, error) { current, message ->
            current.copy(
                customersLoading = false,
                customerCreating = false,
                customerUpdating = false,
                customersError = message,
            )
        }
    }
}
