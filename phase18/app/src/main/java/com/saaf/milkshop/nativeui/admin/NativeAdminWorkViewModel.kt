package com.saaf.milkshop.nativeui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.admin.NativeAdminWorkContext
import com.saaf.milkshop.application.admin.NativeAdminWorkFacade
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.SessionRequestGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NativeAdminWorkDestination {
    Home,
    RecordExpense,
}

sealed interface NativeAdminWorkState {
    data object Idle : NativeAdminWorkState
    data object Loading : NativeAdminWorkState
    data class Ready(
        val context: NativeAdminWorkContext,
        val destination: NativeAdminWorkDestination = NativeAdminWorkDestination.Home,
        val refreshing: Boolean = false,
        val error: String? = null,
    ) : NativeAdminWorkState
    data class LoadFailure(val message: String) : NativeAdminWorkState
}

class NativeAdminWorkViewModel(
    private val facade: NativeAdminWorkFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeAdminWorkState>(NativeAdminWorkState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeAdminWorkState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            (_state.value is NativeAdminWorkState.Ready ||
                _state.value is NativeAdminWorkState.Loading)
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeAdminWorkState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeAdminWorkState.Ready
        val previousDestination =
            previous?.destination ?: NativeAdminWorkDestination.Home
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeAdminWorkState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeAdminWorkState.Ready(
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

    fun openDestination(destination: NativeAdminWorkDestination) {
        val current = _state.value as? NativeAdminWorkState.Ready ?: return
        _state.value = current.copy(destination = destination, error = null)
    }

    fun backToHome() {
        val current = _state.value as? NativeAdminWorkState.Ready ?: return
        _state.value = current.copy(destination = NativeAdminWorkDestination.Home, error = null)
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            onSessionExpired()
            return
        }
        _state.value = NativeAdminWorkState.LoadFailure(
            error.message ?: "Work could not open. Try again.",
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val facade: NativeAdminWorkFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeAdminWorkViewModel::class.java))
            return NativeAdminWorkViewModel(facade, onSessionExpired) as T
        }
    }
}
