package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeOperationsWorkContext
import com.saaf.milkshop.application.operations.NativeOperationsWorkFacade
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
    ) : NativeOperationsWorkState
    data class LoadFailure(val message: String) : NativeOperationsWorkState
}

class NativeOperationsWorkViewModel(
    private val facade: NativeOperationsWorkFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOperationsWorkState>(NativeOperationsWorkState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeOperationsWorkState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            _state.value is NativeOperationsWorkState.Ready
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeOperationsWorkState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val previousDestination =
            (_state.value as? NativeOperationsWorkState.Ready)?.destination
                ?: NativeOperationsWorkDestination.Home
        _state.value = NativeOperationsWorkState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                _state.value = NativeOperationsWorkState.Ready(
                    context = context,
                    destination = previousDestination,
                )
            } catch (error: Throwable) {
                handleLoadFailure(error)
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
        private val facade: NativeOperationsWorkFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOperationsWorkViewModel::class.java))
            return NativeOperationsWorkViewModel(facade, onSessionExpired) as T
        }
    }
}
