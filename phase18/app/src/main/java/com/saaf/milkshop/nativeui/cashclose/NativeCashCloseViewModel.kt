package com.saaf.milkshop.nativeui.cashclose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.cashclose.NativeCashCloseContext
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountSnapshot
import com.saaf.milkshop.application.cashclose.NativeCashCloseDayRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseFacade
import com.saaf.milkshop.application.cashclose.NativeCashCloseReceipt
import com.saaf.milkshop.application.cashclose.NativeCashCloseReopenSubmission
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

sealed interface NativeCashCloseState {
    data object Idle : NativeCashCloseState
    data object Loading : NativeCashCloseState
    data class Ready(
        val context: NativeCashCloseContext,
        val saving: Boolean = false,
        val lastRecordedCount: NativeCashCloseCountSnapshot? = null,
        val receipt: NativeCashCloseReceipt? = null,
        val reopenSuccess: NativeCashCloseReopenSubmission? = null,
        val error: String? = null,
        val refreshing: Boolean = false,
    ) : NativeCashCloseState
    data class LoadFailure(val message: String) : NativeCashCloseState
}

class NativeCashCloseViewModel(
    private val facade: NativeCashCloseFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeCashCloseState>(NativeCashCloseState.Idle)
    private var sessionHandle: String? = null
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeCashCloseState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeCashCloseState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeCashCloseState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeCashCloseState.Loading
        scope.launch {
            try {
                val context = withContext(Dispatchers.IO) { facade.load(session) }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeCashCloseState.Ready(context, refreshing = false)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired || previous == null) {
                    handleLoadFailure(error)
                } else {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: "Cash count and close could not be refreshed. Try again.",
                    )
                }
            }
        }
    }

    fun recordCount(request: NativeCashCloseCountRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeCashCloseState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, reopenSuccess = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.recordCount(session, request) }
                _state.value = NativeCashCloseState.Ready(
                    context = submission.refreshedContext,
                    lastRecordedCount = submission.count,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeCashCloseState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun closeDay(request: NativeCashCloseDayRequest) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeCashCloseState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, receipt = null, reopenSuccess = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) { facade.closeDay(session, request) }
                _state.value = NativeCashCloseState.Ready(
                    context = submission.refreshedContext,
                    receipt = submission.receipt,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeCashCloseState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun reopenDay(reason: String, passphrase: CharArray) {
        val session = sessionHandle
        val current = _state.value as? NativeCashCloseState.Ready
        if (session == null || current == null || current.saving) {
            passphrase.fill('\u0000')
            return
        }
        _state.value = current.copy(saving = true, receipt = null, reopenSuccess = null, error = null)
        scope.launch {
            try {
                val submission = withContext(Dispatchers.IO) {
                    facade.reopenDay(session, reason, passphrase)
                }
                _state.value = NativeCashCloseState.Ready(
                    context = submission.refreshedContext,
                    reopenSuccess = submission,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeCashCloseState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun clearFeedback() {
        val current = _state.value as? NativeCashCloseState.Ready ?: return
        _state.value = current.copy(
            receipt = null,
            lastRecordedCount = null,
            reopenSuccess = null,
            error = null,
        )
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        _state.value = NativeCashCloseState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeCashCloseState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeCashCloseState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Cash count and close access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "Cash count and close could not finish. Check it and try again."
        else ->
            "Cash count and close could not finish. Your entries are still here—check them and try again."
    }

    class Factory(
        private val facade: NativeCashCloseFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeCashCloseViewModel::class.java))
            return NativeCashCloseViewModel(facade, onSessionExpired) as T
        }
    }
}
