package com.saaf.milkshop.nativeui.vendorpayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentContext
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentFacade
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentReceipt
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentRequest
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

sealed interface NativeVendorPaymentState {
    data object Idle : NativeVendorPaymentState
    data object Loading : NativeVendorPaymentState
    data class Ready(
        val context: NativeVendorPaymentContext,
        val saving: Boolean = false,
        val receipt: NativeVendorPaymentReceipt? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeVendorPaymentState
    data class LoadFailure(val message: String) : NativeVendorPaymentState
}

class NativeVendorPaymentViewModel(
    private val facade: NativeVendorPaymentFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeVendorPaymentState>(NativeVendorPaymentState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeVendorPaymentState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeVendorPaymentState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val previous = _state.value as? NativeVendorPaymentState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeVendorPaymentState.Loading
        scope.launch {
            try {
                _state.value = NativeVendorPaymentState.Ready(
                    withContext(Dispatchers.IO) { facade.load(session) },
                    refreshing = false,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: "The supplier payment could not be refreshed. Try again.",
                    )
                }
            }
        }
    }

    fun record(request: NativeVendorPaymentRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeVendorPaymentState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeVendorPaymentState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeVendorPaymentState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun recordAnother() {
        val current = _state.value as? NativeVendorPaymentState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun clearFeedback() {
        val current = _state.value as? NativeVendorPaymentState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeVendorPaymentState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeVendorPaymentState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeVendorPaymentState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Supplier payment access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The supplier payment was not recorded. Check it and try again."
        else ->
            "The supplier payment was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeVendorPaymentFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeVendorPaymentViewModel::class.java))
            return NativeVendorPaymentViewModel(facade, onSessionExpired) as T
        }
    }
}
