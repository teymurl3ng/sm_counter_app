package com.saaf.milkshop.nativeui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.more.NativeBlindCashCountReceipt
import com.saaf.milkshop.application.more.NativeMoreContext
import com.saaf.milkshop.application.more.NativeMoreFacade
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.SessionRequestGate
import com.saaf.milkshop.nativeui.DayStateBus
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

enum class NativeMoreDestination {
    Menu,
    CountCash,
    CashClose,
    Expense,
    Deliveries,
    PersonalSecurity,
}

sealed interface NativeMoreState {
    data object Idle : NativeMoreState
    data object Loading : NativeMoreState
    data class Ready(
        val context: NativeMoreContext,
        val destination: NativeMoreDestination = NativeMoreDestination.Menu,
        val security: NativeMoreSecurityStatus? = null,
        val countReceipt: NativeBlindCashCountReceipt? = null,
        val saving: Boolean = false,
        val confirmRemoveUnlock: Boolean = false,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeMoreState
    data class LoadFailure(val message: String) : NativeMoreState
}

class NativeMoreViewModel(
    private val facade: NativeMoreFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeMoreState>(NativeMoreState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeMoreState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (
            sessionHandle == nextSessionHandle &&
            (_state.value is NativeMoreState.Ready || _state.value is NativeMoreState.Loading)
        ) {
            // Warm re-entry: keep the content on screen and re-validate in
            // place (thin refresh bar) instead of showing stale data.
            refresh()
            return
    }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeMoreState.Ready
        _state.value = (_state.value as? NativeMoreState.Ready)?.copy(refreshing = true, error = null)
            ?: NativeMoreState.Loading
        scope.launch {
            try {
                val (context, security) = coroutineScope {
                    val contextRequest = async(Dispatchers.IO) { facade.load(session) }
                    val securityRequest = async(Dispatchers.IO) { facade.loadSecurity(session) }
                    contextRequest.await() to securityRequest.await()
                }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                // Merge into the previous pane: a warm refresh must never
                // reset the open destination or in-progress dialogs.
                _state.value = (_state.value as? NativeMoreState.Ready)?.copy(
                    context = context,
                    security = security,
                    refreshing = false,
                    error = null,
                ) ?: NativeMoreState.Ready(
                    context = context,
                    security = security,
                    refreshing = false,
                )
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = ((_state.value as? NativeMoreState.Ready) ?: previous).copy(
                        refreshing = false,
                        error = error.message ?: "More could not be refreshed. Try again.",
                    )
                }
            }
        }
    }

    fun openCashClose() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(destination = NativeMoreDestination.CashClose, error = null)
    }

    fun openCountCash() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeMoreDestination.CountCash,
            countReceipt = null,
            error = null,
            saving = false,
        )
    }

    fun openExpense() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeMoreDestination.Expense,
            countReceipt = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
    }

    fun openDeliveries() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeMoreDestination.Deliveries,
            countReceipt = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
    }

    fun openPersonalSecurity() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeMoreDestination.PersonalSecurity,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        scope.launch {
            try {
                val security = withContext(Dispatchers.IO) { facade.loadSecurity(session) }
                val latest = _state.value as? NativeMoreState.Ready ?: return@launch
                _state.value = latest.copy(security = security)
            } catch (error: Throwable) {
                handleReadyFailure(error)
            }
        }
    }

    fun backToMenu() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(
            destination = NativeMoreDestination.Menu,
            countReceipt = null,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
    }

    fun recordCashCount(amountPaisa: Long, notes: String?) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeMoreState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                val receipt = withContext(Dispatchers.IO) {
                    facade.recordBlindCashCount(session, amountPaisa, notes)
                }
                val latest = _state.value as? NativeMoreState.Ready ?: return@launch
                _state.value = latest.copy(
                    countReceipt = receipt,
                    saving = false,
                    error = null,
                )
            } catch (error: Throwable) {
                handleReadyFailure(error)
            }
        }
    }

    fun requestRemoveQuickUnlock() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(confirmRemoveUnlock = true, error = null)
    }

    fun cancelRemoveQuickUnlock() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(confirmRemoveUnlock = false)
    }

    fun removeQuickUnlock(passphrase: CharArray) {
        val session = sessionHandle
        val current = _state.value as? NativeMoreState.Ready
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
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Quick unlock settings saved")
                val latest = _state.value as? NativeMoreState.Ready ?: return@launch
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
        val current = _state.value as? NativeMoreState.Ready
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
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Quick unlock settings saved")
                val latest = _state.value as? NativeMoreState.Ready ?: return@launch
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
        val current = _state.value as? NativeMoreState.Ready
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
                com.saaf.milkshop.nativeui.common.NativeFeedbackBus.saved("Quick unlock settings saved")
                val latest = _state.value as? NativeMoreState.Ready ?: return@launch
                _state.value = latest.copy(security = security, saving = false, error = null)
            } catch (error: Throwable) {
                handleReadyFailure(error)
            } finally {
                passphrase.fill('\u0000')
                pin?.fill('\u0000')
            }
        }
    }

    fun clearError() {
        val current = _state.value as? NativeMoreState.Ready ?: return
        _state.value = current.copy(error = null)
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeMoreState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleReadyFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeMoreState.Idle
            onSessionExpired()
            return
        }
        val current = _state.value as? NativeMoreState.Ready
        if (current != null) {
            _state.value = current.copy(saving = false, error = publicMessage(error))
        } else {
            handleLoadFailure(error)
        }
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeMoreState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeMoreState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "More is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "That action could not be completed."
        else -> "More could not complete that action. Try again."
    }

    class Factory(
        private val facade: NativeMoreFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeMoreViewModel::class.java))
            return NativeMoreViewModel(facade, onSessionExpired) as T
        }
    }
}
