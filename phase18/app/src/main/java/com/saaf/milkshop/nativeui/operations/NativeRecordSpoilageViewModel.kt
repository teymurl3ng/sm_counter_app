package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeRecordSpoilageContext
import com.saaf.milkshop.application.operations.NativeRecordSpoilageFacade
import com.saaf.milkshop.application.operations.NativeRecordSpoilageReceipt
import com.saaf.milkshop.application.operations.NativeRecordSpoilageRequest
import com.saaf.milkshop.application.operations.NativeSpoilageProduct
import com.saaf.milkshop.application.operations.NativeSpoilageReason
import com.saaf.milkshop.domain.SecurityFailure
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

sealed interface NativeRecordSpoilageState {
    data object Idle : NativeRecordSpoilageState
    data object Loading : NativeRecordSpoilageState
    data class Ready(
        val context: NativeRecordSpoilageContext,
        val selectedProduct: NativeSpoilageProduct? = null,
        val selectedReason: NativeSpoilageReason? = null,
        val selectingProduct: Boolean = false,
        val selectingReason: Boolean = false,
        val saving: Boolean = false,
        val receipt: NativeRecordSpoilageReceipt? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeRecordSpoilageState
    data class LoadFailure(val message: String) : NativeRecordSpoilageState
}

class NativeRecordSpoilageViewModel(
    private val facade: NativeRecordSpoilageFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeRecordSpoilageState>(NativeRecordSpoilageState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = com.saaf.milkshop.nativeui.SessionRequestGate()
    val state: StateFlow<NativeRecordSpoilageState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeRecordSpoilageState.Idle) {
            // Warm re-entry: revalidate reference data silently — keep the
            // visible form exactly as the user left it.
            silentRefresh()
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    /**
     * Warm-open reload: revalidates reference data without announcing it —
     * no refreshing flag, no state replacement unless the fresh data differs.
     * The product selection survives if still valid (reasons are a static
     * enum); failures leave the form as-is.
     */
    private fun silentRefresh() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        scope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) { facade.load(session) }
                if (sessionHandle != session) return@launch
                val latest = _state.value as? NativeRecordSpoilageState.Ready ?: return@launch
                val selectedProduct = latest.selectedProduct?.let { selection ->
                    fresh.products.singleOrNull { it.productId == selection.productId }
                }
                val merged = latest.copy(
                    context = fresh,
                    selectedProduct = selectedProduct,
                    refreshing = false,
                )
                if (merged != _state.value) _state.value = merged
            } catch (error: SecurityFailure.SessionExpired) {
                sessionHandle = null
                _state.value = NativeRecordSpoilageState.Idle
                onSessionExpired()
            } catch (error: Throwable) {
                // Silent: the displayed data stays; it will revalidate on
                // the next entry or day-state change.
            }
        }
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeRecordSpoilageState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeRecordSpoilageState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = (_state.value as? NativeRecordSpoilageState.Ready)?.copy(context = context, refreshing = false, error = null)
                    ?: NativeRecordSpoilageState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = ((_state.value as? NativeRecordSpoilageState.Ready) ?: previous).copy(
                        refreshing = false,
                        error = error.message ?: publicMessage(error),
                    )
                }
            }
        }
    }

    fun openProductPicker() {
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(selectingProduct = true, selectingReason = false, error = null)
    }

    fun closeProductPicker() {
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        _state.value = current.copy(selectingProduct = false)
    }

    fun selectProduct(productId: Long) {
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        if (current.saving) return
        val product = current.context.products.singleOrNull { it.productId == productId }
        _state.value = current.copy(
            selectedProduct = product,
            selectingProduct = false,
            receipt = null,
            error = if (product == null) "Choose an active product." else null,
        )
    }

    fun openReasonPicker() {
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(selectingReason = true, selectingProduct = false, error = null)
    }

    fun closeReasonPicker() {
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        _state.value = current.copy(selectingReason = false)
    }

    fun selectReason(reasonCode: String) {
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        if (current.saving) return
        val reason = current.context.reasons.singleOrNull { it.code == reasonCode }
        _state.value = current.copy(
            selectedReason = reason,
            selectingReason = false,
            receipt = null,
            error = if (reason == null) "Choose a spoilage reason." else null,
        )
    }

    fun record(request: NativeRecordSpoilageRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeRecordSpoilageState.Ready(
                    context = submission.refreshedContext,
                    selectedProduct = submission.receipt.product,
                    selectedReason = current.context.reasons.singleOrNull {
                        it.code == submission.receipt.reasonCode
                    },
                    receipt = submission.receipt,
                )

            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeRecordSpoilageState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun recordAnother() {
        val current = _state.value as? NativeRecordSpoilageState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }


    /** Auto-clears the success receipt after 4s unless a newer one replaced it. */


    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeRecordSpoilageState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeRecordSpoilageState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeRecordSpoilageState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Record spoilage access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The spoilage was not recorded. Check it and try again."
        else -> "The spoilage was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeRecordSpoilageFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeRecordSpoilageViewModel::class.java))
            return NativeRecordSpoilageViewModel(facade, onSessionExpired) as T
        }
    }
}
