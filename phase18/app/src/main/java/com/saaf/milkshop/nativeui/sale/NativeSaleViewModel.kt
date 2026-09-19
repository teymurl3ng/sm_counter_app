package com.saaf.milkshop.nativeui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.sale.NativeSaleContext
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.application.sale.NativeSaleReceipt
import com.saaf.milkshop.application.sale.NativeSaleRequest
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

sealed interface NativeSaleState {
    data object Idle : NativeSaleState
    data object Loading : NativeSaleState
    data class Ready(
        val context: NativeSaleContext,
        val saving: Boolean = false,
        val receipt: NativeSaleReceipt? = null,
        val error: String? = null,
    ) : NativeSaleState
    data class LoadFailure(val message: String) : NativeSaleState
}

class NativeSaleViewModel(
    private val facade: NativeSaleFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeSaleState>(NativeSaleState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeSaleState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeSaleState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        _state.value = NativeSaleState.Loading
        scope.launch {
            try {
                _state.value = NativeSaleState.Ready(withContext(Dispatchers.IO) { facade.load(session) })
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun record(request: NativeSaleRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeSaleState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeSaleState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeSaleState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun clearFeedback() {
        val current = _state.value as? NativeSaleState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeSaleState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeSaleState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeSaleState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Sale access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The sale could not be recorded. Check it and try again."
        else -> "The sale could not be recorded. Check it and try again."
    }

    class Factory(
        private val facade: NativeSaleFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeSaleViewModel::class.java))
            return NativeSaleViewModel(facade, onSessionExpired) as T
        }
    }
}
