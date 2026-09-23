package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.operations.NativeOperationsMoreContext
import com.saaf.milkshop.application.operations.NativeOperationsMoreFacade
import com.saaf.milkshop.application.operations.NativeOperationsWorkRecord
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

enum class NativeOperationsMoreDestination {
    Menu,
    TodayWork,
    PersonalSecurity,
}

sealed interface NativeOperationsMoreState {
    data object Idle : NativeOperationsMoreState
    data object Loading : NativeOperationsMoreState
    data class Ready(
        val context: NativeOperationsMoreContext,
        val destination: NativeOperationsMoreDestination = NativeOperationsMoreDestination.Menu,
        val todayRecords: List<NativeOperationsWorkRecord> = emptyList(),
        val security: NativeMoreSecurityStatus? = null,
        val saving: Boolean = false,
        val confirmRemoveUnlock: Boolean = false,
        val error: String? = null,
    ) : NativeOperationsMoreState
    data class LoadFailure(val message: String) : NativeOperationsMoreState
}

class NativeOperationsMoreViewModel(
    private val facade: NativeOperationsMoreFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOperationsMoreState>(NativeOperationsMoreState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeOperationsMoreState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeOperationsMoreState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        _state.value = NativeOperationsMoreState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                val security = withContext(Dispatchers.IO) { facade.loadSecurity(session) }
                _state.value = NativeOperationsMoreState.Ready(context = context, security = security)
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun openTodayWork() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOperationsMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOperationsMoreDestination.TodayWork,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val today = withContext(Dispatchers.IO) { facade.loadTodayWork(session) }
                val latest = _state.value as? NativeOperationsMoreState.Ready ?: return@launch
                _state.value = latest.copy(
                    context = today.context,
                    todayRecords = today.records,
                )
            } catch (error: Throwable) {
                handleReadyFailure(error)
            }
        }
    }

    fun openPersonalSecurity() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOperationsMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOperationsMoreDestination.PersonalSecurity,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val security = withContext(Dispatchers.IO) { facade.loadSecurity(session) }
                val latest = _state.value as? NativeOperationsMoreState.Ready ?: return@launch
                _state.value = latest.copy(security = security)
            } catch (error: Throwable) {
                handleReadyFailure(error)
            }
        }
    }

    fun backToMenu() {
        val current = _state.value as? NativeOperationsMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeOperationsMoreDestination.Menu,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
    }

    fun requestRemoveQuickUnlock() {
        val current = _state.value as? NativeOperationsMoreState.Ready ?: return
        _state.value = current.copy(confirmRemoveUnlock = true, error = null)
    }

    fun cancelRemoveQuickUnlock() {
        val current = _state.value as? NativeOperationsMoreState.Ready ?: return
        _state.value = current.copy(confirmRemoveUnlock = false)
    }

    fun removeQuickUnlock(passphrase: CharArray) {
        val session = sessionHandle
        val current = _state.value as? NativeOperationsMoreState.Ready
        if (session == null || current == null || current.saving) {
            passphrase.fill('\u0000')
            return
        }
        _state.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                val security = withContext(Dispatchers.IO) {
                    facade.removeQuickUnlock(session, passphrase)
                }
                val latest = _state.value as? NativeOperationsMoreState.Ready ?: return@launch
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
        val session = sessionHandle
        val current = _state.value as? NativeOperationsMoreState.Ready
        if (session == null || current == null || current.saving) {
            passphrase.fill('\u0000')
            pin.fill('\u0000')
            return
        }
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
                val latest = _state.value as? NativeOperationsMoreState.Ready ?: return@launch
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
        val session = sessionHandle
        val current = _state.value as? NativeOperationsMoreState.Ready
        if (session == null || current == null || current.saving) {
            passphrase.fill('\u0000')
            pin?.fill('\u0000')
            return
        }
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
                val latest = _state.value as? NativeOperationsMoreState.Ready ?: return@launch
                _state.value = latest.copy(security = security, saving = false, error = null)
            } catch (error: Throwable) {
                handleReadyFailure(error)
            } finally {
                passphrase.fill('\u0000')
                pin?.fill('\u0000')
            }
        }
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeOperationsMoreState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOperationsMoreState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeOperationsMoreState.LoadFailure(publicMessage(error))
        }
    }

    private fun handleReadyFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOperationsMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeOperationsMoreState.Ready ?: return
        _state.value = current.copy(saving = false, error = publicMessage(error))
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "More is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "That action could not be completed. Check it and try again."
        else -> "That action could not be completed. Check it and try again."
    }

    class Factory(
        private val facade: NativeOperationsMoreFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOperationsMoreViewModel::class.java))
            return NativeOperationsMoreViewModel(facade, onSessionExpired) as T
        }
    }
}
