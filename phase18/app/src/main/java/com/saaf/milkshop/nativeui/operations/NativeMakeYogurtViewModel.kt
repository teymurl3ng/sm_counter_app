package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeMakeYogurtContext
import com.saaf.milkshop.application.operations.NativeMakeYogurtFacade
import com.saaf.milkshop.application.operations.NativeMakeYogurtReceipt
import com.saaf.milkshop.application.operations.NativeMakeYogurtRequest
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
import kotlinx.coroutines.delay

sealed interface NativeMakeYogurtState {
    data object Idle : NativeMakeYogurtState
    data object Loading : NativeMakeYogurtState
    data class Ready(
        val context: NativeMakeYogurtContext,
        val saving: Boolean = false,
        val receipt: NativeMakeYogurtReceipt? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeMakeYogurtState
    data class LoadFailure(val message: String) : NativeMakeYogurtState
}

class NativeMakeYogurtViewModel(
    private val facade: NativeMakeYogurtFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeMakeYogurtState>(NativeMakeYogurtState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeMakeYogurtState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeMakeYogurtState.Idle) {
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
        val current = _state.value as? NativeMakeYogurtState.Ready ?: return
        scope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) { facade.load(session) }
                if (sessionHandle != session) return@launch
                val latest = _state.value as? NativeMakeYogurtState.Ready ?: return@launch
                val merged = latest.copy(context = fresh, refreshing = false)
                if (merged != _state.value) _state.value = merged
            } catch (error: SecurityFailure.SessionExpired) {
                sessionHandle = null
                _state.value = NativeMakeYogurtState.Idle
                onSessionExpired()
            } catch (error: Throwable) {
                // Silent: the displayed data stays; it will revalidate on
                // the next entry or day-state change.
            }
        }
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val previous = _state.value as? NativeMakeYogurtState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeMakeYogurtState.Loading
        scope.launch {
            try {
                _state.value = NativeMakeYogurtState.Ready(
                    withContext(Dispatchers.IO) { facade.load(session) },
                    refreshing = false,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: publicMessage(error),
                    )
                }
            }
        }
    }

    fun record(request: NativeMakeYogurtRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeMakeYogurtState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeMakeYogurtState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                )
                autoClearReceipt(submission.receipt)
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeMakeYogurtState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }


    /** Auto-clears the success receipt after 4s unless a newer one replaced it. */
    private fun autoClearReceipt(receipt: Any?) {
        scope.launch {
            delay(4_000)
            val current = _state.value as? NativeMakeYogurtState.Ready ?: return@launch
            if (current.receipt === receipt) {
                _state.value = current.copy(receipt = null)
            }
        }
    }

    fun makeAnother() {
        val current = _state.value as? NativeMakeYogurtState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeMakeYogurtState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeMakeYogurtState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeMakeYogurtState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Make yogurt access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The batch was not recorded. Check it and try again."
        else -> "The batch was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeMakeYogurtFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeMakeYogurtViewModel::class.java))
            return NativeMakeYogurtViewModel(facade, onSessionExpired) as T
        }
    }
}
