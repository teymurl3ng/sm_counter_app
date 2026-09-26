package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeDeliveriesContext
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
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

sealed interface NativeDeliveriesState {
    data object Idle : NativeDeliveriesState
    data object Loading : NativeDeliveriesState
    data class Ready(
        val context: NativeDeliveriesContext,
        val markingDeliveryId: Long? = null,
        val lastDeliveredId: Long? = null,
        val linkingDeliveryId: Long? = null,
        val lastLinkedId: Long? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeDeliveriesState
    data class LoadFailure(val message: String) : NativeDeliveriesState
}

class NativeDeliveriesViewModel(
    private val facade: NativeDeliveriesFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeDeliveriesState>(NativeDeliveriesState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeDeliveriesState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeDeliveriesState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeDeliveriesState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeDeliveriesState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeDeliveriesState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
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

    fun markDelivered(deliveryId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        if (current.markingDeliveryId != null) return
        _state.value = current.copy(markingDeliveryId = deliveryId, error = null, lastDeliveredId = null)
        scope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    facade.markDelivered(session, deliveryId)
                }
                _state.value = NativeDeliveriesState.Ready(
                    context = refreshed,
                    lastDeliveredId = deliveryId,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeDeliveriesState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun linkSale(deliveryId: Long, saleNumber: String) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        if (current.linkingDeliveryId != null || current.markingDeliveryId != null) return
        _state.value = current.copy(linkingDeliveryId = deliveryId, error = null)
        scope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    facade.linkSale(session, deliveryId, saleNumber)
                }
                _state.value = NativeDeliveriesState.Ready(
                    context = refreshed,
                    lastLinkedId = deliveryId,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeDeliveriesState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(
                        linkingDeliveryId = null,
                        error = publicMessage(error),
                    )
                }
            }
        }
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeDeliveriesState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeDeliveriesState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeDeliveriesState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Deliveries access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The delivery could not be updated. Check it and try again."
        else -> "The delivery could not be updated. Check it and try again."
    }

    class Factory(
        private val facade: NativeDeliveriesFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeDeliveriesViewModel::class.java))
            return NativeDeliveriesViewModel(facade, onSessionExpired) as T
        }
    }
}
