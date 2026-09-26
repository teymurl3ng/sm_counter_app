package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.owner.NativeOwnerWorkContext
import com.saaf.milkshop.application.owner.NativeOwnerWorkFacade
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.SessionRequestGate
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

enum class NativeOwnerWorkDestination {
    Home,
    RecordSale,
    ReceiveMilk,
    RecordPayment,
    MakeYogurt,
    RecordSpoilage,
    Deliveries,
    RecordExpense,
    PaySupplier,
    PaySalary,
    CashCountAndClose,
}

sealed interface NativeOwnerWorkState {
    data object Idle : NativeOwnerWorkState
    data object Loading : NativeOwnerWorkState
    data class Ready(
        val context: NativeOwnerWorkContext,
        val destination: NativeOwnerWorkDestination = NativeOwnerWorkDestination.Home,
        val refreshing: Boolean = false,
        val error: String? = null,
    ) : NativeOwnerWorkState
    data class LoadFailure(val message: String) : NativeOwnerWorkState
}

class NativeOwnerWorkViewModel(
    private val facade: NativeOwnerWorkFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOwnerWorkState>(NativeOwnerWorkState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeOwnerWorkState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            (_state.value is NativeOwnerWorkState.Ready ||
                _state.value is NativeOwnerWorkState.Loading)
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeOwnerWorkState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeOwnerWorkState.Ready
        val previousDestination =
            previous?.destination ?: NativeOwnerWorkDestination.Home
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeOwnerWorkState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeOwnerWorkState.Ready(
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

    fun openDestination(destination: NativeOwnerWorkDestination) {
        val current = _state.value as? NativeOwnerWorkState.Ready ?: return
        _state.value = current.copy(destination = destination, error = null)
    }

    fun backToHome() {
        val current = _state.value as? NativeOwnerWorkState.Ready ?: return
        _state.value = current.copy(destination = NativeOwnerWorkDestination.Home, error = null)
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            onSessionExpired()
            return
        }
        _state.value = NativeOwnerWorkState.LoadFailure(
            error.message ?: "Work could not open. Try again.",
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val facade: NativeOwnerWorkFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOwnerWorkViewModel::class.java))
            return NativeOwnerWorkViewModel(facade, onSessionExpired) as T
        }
    }
}
