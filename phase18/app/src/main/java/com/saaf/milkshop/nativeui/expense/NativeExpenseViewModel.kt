package com.saaf.milkshop.nativeui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.expense.NativeExpenseContext
import com.saaf.milkshop.application.expense.NativeExpenseFacade
import com.saaf.milkshop.application.expense.NativeExpenseReceipt
import com.saaf.milkshop.application.expense.NativeExpenseRequest
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

sealed interface NativeExpenseState {
    data object Idle : NativeExpenseState
    data object Loading : NativeExpenseState
    data class Ready(
        val context: NativeExpenseContext,
        val saving: Boolean = false,
        val receipt: NativeExpenseReceipt? = null,
        val error: String? = null,
    ) : NativeExpenseState
    data class LoadFailure(val message: String) : NativeExpenseState
}

class NativeExpenseViewModel(
    private val facade: NativeExpenseFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeExpenseState>(NativeExpenseState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeExpenseState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeExpenseState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        _state.value = NativeExpenseState.Loading
        scope.launch {
            try {
                _state.value = NativeExpenseState.Ready(withContext(Dispatchers.IO) { facade.load(session) })
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun record(request: NativeExpenseRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeExpenseState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.record(session, request) }
                _state.value = NativeExpenseState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeExpenseState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun recordAnother() {
        val current = _state.value as? NativeExpenseState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun clearFeedback() {
        val current = _state.value as? NativeExpenseState.Ready ?: return
        _state.value = current.copy(receipt = null, error = null)
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeExpenseState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeExpenseState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeExpenseState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Expense access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "The expense was not recorded. Check it and try again."
        else -> "The expense was not recorded. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeExpenseFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeExpenseViewModel::class.java))
            return NativeExpenseViewModel(facade, onSessionExpired) as T
        }
    }
}
