package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeOperationsWorkContext
import com.saaf.milkshop.application.operations.NativeOperationsWorkOperations
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.SessionRequestGate
import com.saaf.milkshop.nativeui.DataChangeBus
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

enum class OperationsSection { Work, Deliveries, Attendance, More }

enum class NativeOperationsWorkDestination {
    Home,
    ReceiveMilk,
    MakeYogurt,
    RecordSpoilage,
}

sealed interface NativeOperationsWorkState {
    data object Idle : NativeOperationsWorkState
    data object Loading : NativeOperationsWorkState
    data class Ready(
        val context: NativeOperationsWorkContext,
        val destination: NativeOperationsWorkDestination = NativeOperationsWorkDestination.Home,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeOperationsWorkState
    data class LoadFailure(val message: String) : NativeOperationsWorkState
}

class NativeOperationsWorkViewModel(
    private val facade: NativeOperationsWorkOperations,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOperationsWorkState>(NativeOperationsWorkState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeOperationsWorkState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
        scope.launch {
            DataChangeBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            (_state.value is NativeOperationsWorkState.Ready ||
                _state.value is NativeOperationsWorkState.Loading)
        ) {
            // Warm re-entry: keep the content on screen and re-validate in
            // place (thin refresh bar) instead of showing stale data.
            refresh()
            return
    }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeOperationsWorkState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeOperationsWorkState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeOperationsWorkState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeOperationsWorkState.Ready(
                    context = context,
                    destination = (_state.value as? NativeOperationsWorkState.Ready)?.destination
                        ?: NativeOperationsWorkDestination.Home,
                    refreshing = false,
                )
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = ((_state.value as? NativeOperationsWorkState.Ready) ?: previous).copy(
                        refreshing = false,
                        error = error.message ?: "Work could not open. Try again.",
                    )
                }
            }
        }
    }

    fun openReceiveMilk() = openDestination(NativeOperationsWorkDestination.ReceiveMilk)
    fun openMakeYogurt() = openDestination(NativeOperationsWorkDestination.MakeYogurt)
    fun openRecordSpoilage() = openDestination(NativeOperationsWorkDestination.RecordSpoilage)

    fun backToHome() {
        val current = _state.value as? NativeOperationsWorkState.Ready ?: return
        _state.value = current.copy(destination = NativeOperationsWorkDestination.Home, error = null)
    }

    private fun openDestination(destination: NativeOperationsWorkDestination) {
        val current = _state.value as? NativeOperationsWorkState.Ready ?: return
        _state.value = current.copy(destination = destination, error = null)
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            onSessionExpired()
            return
        }
        _state.value = NativeOperationsWorkState.LoadFailure(
            error.message ?: "Work could not open. Try again.",
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val facade: NativeOperationsWorkOperations,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOperationsWorkViewModel::class.java))
            return NativeOperationsWorkViewModel(facade, onSessionExpired) as T
        }
    }
}
