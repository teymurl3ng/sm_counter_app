package com.saaf.milkshop.nativeui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.drawing.NativeDrawingContext
import com.saaf.milkshop.application.drawing.NativeDrawingFacade
import com.saaf.milkshop.application.drawing.NativeDrawingReceipt
import com.saaf.milkshop.application.drawing.NativeDrawingRequest
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

sealed interface NativeDrawingState {
    data object Idle : NativeDrawingState
    data object Loading : NativeDrawingState
    data class Ready(
        val context: NativeDrawingContext,
        val saving: Boolean = false,
        val receipt: NativeDrawingReceipt? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeDrawingState
    data class LoadFailure(val message: String) : NativeDrawingState
}

class NativeDrawingViewModel(
    private val facade: NativeDrawingFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeDrawingState>(NativeDrawingState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = com.saaf.milkshop.nativeui.SessionRequestGate()
    val state: StateFlow<NativeDrawingState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeDrawingState.Idle) {
            // Warm re-entry: revalidate silently — keep the visible form
            // exactly as the user left it.
            silentRefresh()
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    private fun silentRefresh() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeDrawingState.Ready ?: return
        scope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) { facade.load(session) }
                if (sessionHandle != session) return@launch
                val latest = _state.value as? NativeDrawingState.Ready ?: return@launch
                val merged = latest.copy(context = fresh, refreshing = false)
                if (merged != _state.value) _state.value = merged
            } catch (error: SecurityFailure.SessionExpired) {
                sessionHandle = null
                _state.value = NativeDrawingState.Idle
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
        val previous = _state.value as? NativeDrawingState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeDrawingState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = (_state.value as? NativeDrawingState.Ready)?.copy(context = context, refreshing = false, error = null)
                    ?: NativeDrawingState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = ((_state.value as? NativeDrawingState.Ready) ?: previous).copy(
                        refreshing = false,
                        error = error.message ?: "The drawing could not be refreshed. Try again.",
                    )
                }
            }
        }
    }

    fun record(request: NativeDrawingRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeDrawingState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeDrawingState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                )

            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeDrawingState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun recordAnother() {
        val current = _state.value as? NativeDrawingState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    /** Auto-clears the success receipt after 4s unless a newer one replaced it. */


    fun clearFeedback() {
        val current = _state.value as? NativeDrawingState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeDrawingState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeDrawingState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeDrawingState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Owner drawing is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The drawing was not recorded. Check it and try again."
        else -> "The drawing was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeDrawingFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeDrawingViewModel::class.java))
            return NativeDrawingViewModel(facade, onSessionExpired) as T
        }
    }
}
