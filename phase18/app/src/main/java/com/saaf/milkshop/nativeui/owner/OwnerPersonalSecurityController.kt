package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerMoreFacade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the signed-in owner's personal quick-unlock security workflow. */
internal class OwnerPersonalSecurityController(
    private val facade: NativeOwnerMoreFacade,
    private val runtime: OwnerMoreRuntime,
) {
    fun openPersonalSecurity() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
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
        runtime.scope.launch {
            try {
                val security = withContext(Dispatchers.IO) { facade.loadSecurity(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                runtime.state.value = latest.copy(security = security)
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun requestRemoveQuickUnlock() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(confirmRemoveUnlock = true, error = null)
    }

    fun cancelRemoveQuickUnlock() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(confirmRemoveUnlock = false)
    }

    fun removeQuickUnlock(passphrase: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null || current.saving) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        runtime.state.value = current.copy(saving = true, error = null)
        runtime.scope.launch {
            try {
                val security = withContext(Dispatchers.IO) {
                    facade.removeQuickUnlock(session, passphrase)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                runtime.state.value = latest.copy(
                    security = security,
                    saving = false,
                    confirmRemoveUnlock = false,
                    error = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun enrollQuickPin(passphrase: CharArray, pin: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null || current.saving) {
            passphrase.fill('\u0000')
            pin.fill('\u0000')
            return
        }
        val session = token.handle
        runtime.state.value = current.copy(saving = true, error = null)
        runtime.scope.launch {
            try {
                val security = withContext(Dispatchers.IO) {
                    facade.enrollQuickUnlock(
                        sessionHandle = session,
                        currentPassphrase = passphrase,
                        enablePhoneSecurity = current.security?.phoneSecurityEnabled == true,
                        pin = pin,
                    )
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                runtime.state.value = latest.copy(security = security, saving = false, error = null)
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
                pin.fill('\u0000')
            }
        }
    }

    fun enrollPhoneSecurity(passphrase: CharArray, pin: CharArray?) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null || current.saving) {
            passphrase.fill('\u0000')
            pin?.fill('\u0000')
            return
        }
        val session = token.handle
        if (current.security?.quickPinEnabled == true && (pin == null || pin.size != 4)) {
            runtime.state.value = current.copy(
                error = "Enter your 4-digit Quick PIN to keep it while enabling phone security.",
            )
            passphrase.fill('\u0000')
            pin?.fill('\u0000')
            return
        }
        runtime.state.value = current.copy(saving = true, error = null)
        runtime.scope.launch {
            try {
                val security = withContext(Dispatchers.IO) {
                    facade.enrollQuickUnlock(
                        sessionHandle = session,
                        currentPassphrase = passphrase,
                        enablePhoneSecurity = true,
                        pin = pin,
                    )
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                runtime.state.value = latest.copy(security = security, saving = false, error = null)
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
                pin?.fill('\u0000')
            }
        }
    }


    fun resetTransient() {
        runtime.updateReady {
            it.copy(confirmRemoveUnlock = false, saving = false, error = null)
        }
    }

    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.PersonalSecurity, error) { current, message ->
            current.copy(saving = false, error = message)
        }
    }
}
