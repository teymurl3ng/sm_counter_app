package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeDeliveriesContext
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.application.operations.NativeDeliveryDraft
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
import kotlinx.coroutines.delay
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
        val archivingDeliveryId: Long? = null,
        val draftCustomerId: Long? = null,
        val draftProductId: Long? = null,
        val draftQuantity: String = "",
        val draftRoute: String = "",
        val draftNotes: String = "",
        val creating: Boolean = false,
        val lastCreatedId: Long? = null,
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

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

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
                autoDismissSuccess("delivered", deliveryId)
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
                autoDismissSuccess("linked", deliveryId)
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

    fun selectDraftCustomer(id: Long?) {
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        _state.value = current.copy(draftCustomerId = id)
    }

    fun selectDraftProduct(id: Long?) {
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        _state.value = current.copy(draftProductId = id)
    }

    fun updateDraftQuantity(value: String) {
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        _state.value = current.copy(draftQuantity = value.filter { it.isDigit() || it == '.' })
    }

    fun updateDraftRoute(value: String) {
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        _state.value = current.copy(draftRoute = value)
    }

    fun updateDraftNotes(value: String) {
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        _state.value = current.copy(draftNotes = value)
    }

    fun createDelivery() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        if (current.creating || current.markingDeliveryId != null || current.linkingDeliveryId != null) return
        _state.value = current.copy(creating = true, error = null, lastCreatedId = null)
        scope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    facade.createDelivery(
                        session,
                        NativeDeliveryDraft(
                            customerAccountId = current.draftCustomerId,
                            productId = current.draftProductId,
                            quantity = current.draftQuantity,
                            route = current.draftRoute,
                            notes = current.draftNotes,
                        ),
                    )
                }
                val createdId = refreshed.today.firstOrNull()?.deliveryId
                    ?: refreshed.earlier.firstOrNull()?.deliveryId
                _state.value = NativeDeliveriesState.Ready(
                    context = refreshed,
                    lastCreatedId = createdId,
                )
                createdId?.let { autoDismissSuccess("created", it) }
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeDeliveriesState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(creating = false, error = publicMessage(error))
                }
            }
        }
    }

    fun archiveDelivery(deliveryId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeDeliveriesState.Ready ?: return
        if (current.archivingDeliveryId != null) return
        _state.value = current.copy(archivingDeliveryId = deliveryId, error = null)
        scope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    facade.archiveDelivery(session, deliveryId)
                }
                _state.value = NativeDeliveriesState.Ready(context = refreshed)
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeDeliveriesState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(archivingDeliveryId = null, error = publicMessage(error))
                }
            }
        }
    }


    /**
     * Auto-dismisses the success banner after [feedbackMillis] unless a newer
     * success has replaced it (guarded per field).
     */
    private fun autoDismissSuccess(field: String, value: Long) {
        scope.launch {
            delay(feedbackMillis)
            val current = _state.value as? NativeDeliveriesState.Ready ?: return@launch
            val stillShowing = when (field) {
                "delivered" -> current.lastDeliveredId == value
                "linked" -> current.lastLinkedId == value
                "created" -> current.lastCreatedId == value
                else -> false
            }
            if (stillShowing) {
                _state.value = current.copy(
                    lastDeliveredId = if (field == "delivered") null else current.lastDeliveredId,
                    lastLinkedId = if (field == "linked") null else current.lastLinkedId,
                    lastCreatedId = if (field == "created") null else current.lastCreatedId,
                )
            }
        }
    }

    private companion object {
        const val feedbackMillis = 4_000L
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
