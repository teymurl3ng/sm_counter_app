package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeReceiveMilkContext
import com.saaf.milkshop.application.operations.NativeReceiveMilkFacade
import com.saaf.milkshop.application.operations.NativeReceiveMilkPreview
import com.saaf.milkshop.application.operations.NativeReceiveMilkReceipt
import com.saaf.milkshop.application.operations.NativeReceiveMilkRequest
import com.saaf.milkshop.application.operations.NativeReceiveMilkVendor
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

sealed interface NativeReceiveMilkState {
    data object Idle : NativeReceiveMilkState
    data object Loading : NativeReceiveMilkState
    data class Ready(
        val context: NativeReceiveMilkContext,
        val selectedVendor: NativeReceiveMilkVendor? = null,
        val selectingVendor: Boolean = false,
        val saving: Boolean = false,
        val receipt: NativeReceiveMilkReceipt? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeReceiveMilkState
    data class LoadFailure(val message: String) : NativeReceiveMilkState
}

class NativeReceiveMilkViewModel(
    private val facade: NativeReceiveMilkFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeReceiveMilkState>(NativeReceiveMilkState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeReceiveMilkState> = _state.asStateFlow()

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeReceiveMilkState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val previous = _state.value as? NativeReceiveMilkState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeReceiveMilkState.Loading
        scope.launch {
            try {
                _state.value = NativeReceiveMilkState.Ready(
                    withContext(Dispatchers.IO) { facade.load(session) },
                    refreshing = false,
                )
            } catch (error: Throwable) {
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

    fun selectVendor(vendorAccountId: Long) {
        val current = _state.value as? NativeReceiveMilkState.Ready ?: return
        if (current.saving) return
        val vendor = current.context.vendors.singleOrNull { it.accountId == vendorAccountId }
        _state.value = current.copy(
            selectedVendor = vendor,
            selectingVendor = false,
            receipt = null,
            error = if (vendor == null) "Choose an active supplier." else null,
        )
    }

    fun openVendorPicker() {
        val current = _state.value as? NativeReceiveMilkState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(selectingVendor = true, error = null)
    }

    fun closeVendorPicker() {
        val current = _state.value as? NativeReceiveMilkState.Ready ?: return
        _state.value = current.copy(selectingVendor = false)
    }

    fun preview(
        rawLiters: Double,
        fatPercentage: Double,
        lrReading: Double,
    ): NativeReceiveMilkPreview? {
        val current = _state.value as? NativeReceiveMilkState.Ready ?: return null
        return facade.preview(current.context, rawLiters, fatPercentage, lrReading)
    }

    fun record(request: NativeReceiveMilkRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeReceiveMilkState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeReceiveMilkState.Ready(
                    context = submission.refreshedContext,
                    selectedVendor = submission.receipt.vendor,
                    receipt = submission.receipt,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeReceiveMilkState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun receiveAnother() {
        val current = _state.value as? NativeReceiveMilkState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeReceiveMilkState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeReceiveMilkState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeReceiveMilkState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Receive milk access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The receipt was not recorded. Check it and try again."
        else -> "The receipt was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeReceiveMilkFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeReceiveMilkViewModel::class.java))
            return NativeReceiveMilkViewModel(facade, onSessionExpired) as T
        }
    }
}
