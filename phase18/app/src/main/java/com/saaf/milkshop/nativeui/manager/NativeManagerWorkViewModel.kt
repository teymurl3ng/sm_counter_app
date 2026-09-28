package com.saaf.milkshop.nativeui.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.manager.NativeManagerWorkContext
import com.saaf.milkshop.application.manager.NativeManagerWorkFacade
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

enum class NativeManagerWorkDestination {
    Home,
    RecordSale,
    RecordPayment,
    ReceiveMilk,
    MakeYogurt,
    RecordSpoilage,
    Deliveries,
    RecordExpense,
    PaySupplier,
}

sealed interface NativeManagerWorkState {
    data object Idle : NativeManagerWorkState
    data object Loading : NativeManagerWorkState
    data class Ready(
        val context: NativeManagerWorkContext,
        val destination: NativeManagerWorkDestination = NativeManagerWorkDestination.Home,
        val refreshing: Boolean = false,
        val error: String? = null,
    ) : NativeManagerWorkState
    data class LoadFailure(val message: String) : NativeManagerWorkState
}

class NativeManagerWorkViewModel(
    private val facade: NativeManagerWorkFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeManagerWorkState>(NativeManagerWorkState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeManagerWorkState> = _state.asStateFlow()

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
            (_state.value is NativeManagerWorkState.Ready ||
                _state.value is NativeManagerWorkState.Loading)
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
        _state.value = NativeManagerWorkState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeManagerWorkState.Ready
        val previousDestination =
            previous?.destination ?: NativeManagerWorkDestination.Home
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeManagerWorkState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeManagerWorkState.Ready(
                    context = context,
                    destination = previousDestination,
                    refreshing = false,
                )
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: "Work could not open. Try again.",
                    )
                }
            }
        }
    }

    fun openDestination(destination: NativeManagerWorkDestination) {
        val current = _state.value as? NativeManagerWorkState.Ready ?: return
        _state.value = current.copy(destination = destination, error = null)
    }

    fun backToHome() {
        val current = _state.value as? NativeManagerWorkState.Ready ?: return
        _state.value = current.copy(destination = NativeManagerWorkDestination.Home, error = null)
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            onSessionExpired()
            return
        }
        _state.value = NativeManagerWorkState.LoadFailure(
            error.message ?: "Work could not open. Try again.",
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val facade: NativeManagerWorkFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeManagerWorkViewModel::class.java))
            return NativeManagerWorkViewModel(facade, onSessionExpired) as T
        }
    }
}
