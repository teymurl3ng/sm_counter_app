package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.owner.NativeOwnerOverviewFacade
import com.saaf.milkshop.application.owner.NativeOwnerOverviewSnapshot
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

enum class OwnerSection { Overview, Work, Records, More }

sealed interface NativeOwnerOverviewState {
    data object Idle : NativeOwnerOverviewState
    data object Loading : NativeOwnerOverviewState
    data class Ready(
        val snapshot: NativeOwnerOverviewSnapshot,
        val refreshing: Boolean = false,
        val error: String? = null,
    ) : NativeOwnerOverviewState
    data class LoadFailure(val message: String) : NativeOwnerOverviewState
}

class NativeOwnerOverviewViewModel(
    private val facade: NativeOwnerOverviewFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOwnerOverviewState>(NativeOwnerOverviewState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeOwnerOverviewState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            (_state.value is NativeOwnerOverviewState.Ready ||
                _state.value is NativeOwnerOverviewState.Loading)
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeOwnerOverviewState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeOwnerOverviewState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeOwnerOverviewState.Loading
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeOwnerOverviewState.Ready(
                    snapshot = snapshot,
                    refreshing = false,
                )
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: "Overview could not open. Try again.",
                    )
                }
            }
        }
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            onSessionExpired()
            return
        }
        _state.value = NativeOwnerOverviewState.LoadFailure(
            error.message ?: "Overview could not open. Try again.",
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val facade: NativeOwnerOverviewFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOwnerOverviewViewModel::class.java))
            return NativeOwnerOverviewViewModel(facade, onSessionExpired) as T
        }
    }
}
