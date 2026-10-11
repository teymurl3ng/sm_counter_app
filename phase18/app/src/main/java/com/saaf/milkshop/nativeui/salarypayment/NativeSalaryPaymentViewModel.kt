package com.saaf.milkshop.nativeui.salarypayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentContext
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentFacade
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentReceipt
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentRequest
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.DayStateBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface NativeSalaryPaymentState {
    data object Idle : NativeSalaryPaymentState
    data object Loading : NativeSalaryPaymentState
    data class Ready(
        val context: NativeSalaryPaymentContext,
        val saving: Boolean = false,
        val receipt: NativeSalaryPaymentReceipt? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeSalaryPaymentState
    data class LoadFailure(val message: String) : NativeSalaryPaymentState
}

class NativeSalaryPaymentViewModel(
    private val facade: NativeSalaryPaymentFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeSalaryPaymentState>(NativeSalaryPaymentState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = com.saaf.milkshop.nativeui.SessionRequestGate()
    val state: StateFlow<NativeSalaryPaymentState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeSalaryPaymentState.Idle) {
            // Warm re-entry: revalidate reference data silently — keep the
            // visible form exactly as the user left it.
            silentRefresh()
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    /**
     * Warm-open reload: revalidates reference data without announcing it —
     * no refreshing flag, no state replacement unless the fresh data differs.
     * Receipt survives; failures leave the form as-is.
     */
    private fun silentRefresh() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeSalaryPaymentState.Ready ?: return
        scope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) { facade.load(session) }
                if (sessionHandle != session) return@launch
                val latest = _state.value as? NativeSalaryPaymentState.Ready ?: return@launch
                val merged = latest.copy(context = fresh, refreshing = false)
                if (merged != _state.value) _state.value = merged
            } catch (error: SecurityFailure.SessionExpired) {
                sessionHandle = null
                _state.value = NativeSalaryPaymentState.Idle
                onSessionExpired()
            } catch (error: Throwable) {
                // Silent: the displayed data stays; it will revalidate on
                // the next entry or day-state change.
            }
        }
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeSalaryPaymentState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeSalaryPaymentState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = (_state.value as? NativeSalaryPaymentState.Ready)?.copy(context = context, refreshing = false, error = null)
                    ?: NativeSalaryPaymentState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = ((_state.value as? NativeSalaryPaymentState.Ready) ?: previous).copy(
                        refreshing = false,
                        error = error.message ?: "The salary payment could not be refreshed. Try again.",
                    )
                }
            }
        }
    }

    fun record(request: NativeSalaryPaymentRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeSalaryPaymentState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeSalaryPaymentState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                )

            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeSalaryPaymentState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun recordAnother() {
        val current = _state.value as? NativeSalaryPaymentState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }


    /** Auto-clears the success receipt after 4s unless a newer one replaced it. */


    fun clearFeedback() {
        val current = _state.value as? NativeSalaryPaymentState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeSalaryPaymentState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeSalaryPaymentState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeSalaryPaymentState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Salary payment access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The salary payment was not recorded. Check it and try again."
        else ->
            "The salary payment was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeSalaryPaymentFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeSalaryPaymentViewModel::class.java))
            return NativeSalaryPaymentViewModel(facade, onSessionExpired) as T
        }
    }
}
