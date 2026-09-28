package com.saaf.milkshop.nativeui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.collection.NativeCollectionContext
import com.saaf.milkshop.application.collection.NativeCollectionCustomerBalance
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.collection.NativeCollectionReceipt
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.SessionRequestGate
import com.saaf.milkshop.nativeui.DayStateBus
import com.saaf.milkshop.nativeui.DataChangeBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

sealed interface NativeCollectionState {
    data object Idle : NativeCollectionState
    data object Loading : NativeCollectionState
    data class Ready(
        val context: NativeCollectionContext,
        val selectedCustomer: NativeCollectionCustomerBalance? = null,
        val selectingCustomer: Boolean = false,
        val saving: Boolean = false,
        val receipt: NativeCollectionReceipt? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeCollectionState
    data class LoadFailure(val message: String) : NativeCollectionState
}

class NativeCollectionViewModel(
    private val facade: NativeCollectionFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeCollectionState>(NativeCollectionState.Idle)
    private var sessionHandle: String? = null
    private var selectionGeneration = 0L
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeCollectionState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeCollectionState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeCollectionState.Ready
        selectionGeneration += 1L
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeCollectionState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeCollectionState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: "Collection could not be refreshed. Try again.",
                    )
                }
            }
        }
    }

    fun selectCustomer(customerAccountId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeCollectionState.Ready ?: return
        if (current.saving) return
        val generation = ++selectionGeneration
        _state.value = current.copy(
            selectedCustomer = null,
            selectingCustomer = true,
            receipt = null,
            error = null,
        )
        scope.launch {
            try {
                val selected = withContext(Dispatchers.IO) {
                    facade.loadCustomer(session, customerAccountId)
                }
                if (generation == selectionGeneration) {
                    val latest = _state.value as? NativeCollectionState.Ready ?: return@launch
                    _state.value = latest.copy(selectedCustomer = selected, selectingCustomer = false)
                }
            } catch (error: Throwable) {
                if (generation == selectionGeneration) handleSelectionFailure(current, error)
            }
        }
    }

    fun record(request: NativeCollectionRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeCollectionState.Ready ?: return
        if (current.saving || current.selectingCustomer) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeCollectionState.Ready(
                    context = submission.refreshedContext,
                    selectedCustomer = NativeCollectionCustomerBalance(
                        submission.receipt.customer,
                        submission.receipt.customerBalanceAfterPaisa,
                    ),
                    receipt = submission.receipt,
                )
                autoClearReceipt(submission.receipt)
                DataChangeBus.notifyDataChanged()
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeCollectionState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun collectAnother() {
        val current = _state.value as? NativeCollectionState.Ready ?: return
        selectionGeneration += 1L
        _state.value = current.copy(
            selectedCustomer = null,
            selectingCustomer = false,
            receipt = null,
            error = null,
        )
    }


    /** Auto-clears the success receipt after 4s unless a newer one replaced it. */
    private fun autoClearReceipt(receipt: Any?) {
        scope.launch {
            delay(4_000)
            val current = _state.value as? NativeCollectionState.Ready ?: return@launch
            if (current.receipt === receipt) {
                _state.value = current.copy(receipt = null)
            }
        }
    }

    fun clearFeedback() {
        val current = _state.value as? NativeCollectionState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        loadGate.invalidate()
        selectionGeneration += 1L
        sessionHandle = null
        _state.value = NativeCollectionState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleSelectionFailure(
        previous: NativeCollectionState.Ready,
        error: Throwable,
    ) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeCollectionState.Idle
            onSessionExpired()
        } else {
            _state.value = previous.copy(
                selectedCustomer = null,
                selectingCustomer = false,
                error = publicMessage(error),
            )
        }
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeCollectionState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeCollectionState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Collection access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The payment was not recorded. Check it and try again."
        else -> "The payment was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeCollectionFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeCollectionViewModel::class.java))
            return NativeCollectionViewModel(facade, onSessionExpired) as T
        }
    }
}
