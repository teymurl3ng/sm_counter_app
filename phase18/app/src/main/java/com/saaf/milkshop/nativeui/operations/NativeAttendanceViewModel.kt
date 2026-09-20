package com.saaf.milkshop.nativeui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.operations.NativeAttendanceContext
import com.saaf.milkshop.application.operations.NativeAttendanceFacade
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

sealed interface NativeAttendanceState {
    data object Idle : NativeAttendanceState
    data object Loading : NativeAttendanceState
    data class Ready(
        val context: NativeAttendanceContext,
        val saving: Boolean = false,
        val error: String? = null,
    ) : NativeAttendanceState
    data class LoadFailure(val message: String) : NativeAttendanceState
}

class NativeAttendanceViewModel(
    private val facade: NativeAttendanceFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeAttendanceState>(NativeAttendanceState.Idle)
    private var sessionHandle: String? = null
    val state: StateFlow<NativeAttendanceState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle && _state.value !is NativeAttendanceState.Idle) return
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        _state.value = NativeAttendanceState.Loading
        scope.launch {
            try {
                _state.value = NativeAttendanceState.Ready(
                    withContext(Dispatchers.IO) { facade.load(session) },
                )
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun markPresent() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeAttendanceState.Ready ?: return
        if (current.saving) return
        _state.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) { facade.markPresent(session) }
                _state.value = NativeAttendanceState.Ready(context = refreshed)
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeAttendanceState.Idle
                    onSessionExpired()
                } else {
                    _state.value = current.copy(error = publicMessage(error))
                }
            }
        }
    }

    fun close() {
        sessionHandle = null
        _state.value = NativeAttendanceState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeAttendanceState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeAttendanceState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Attendance access is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "Attendance could not be updated. Check it and try again."
        else -> "Attendance could not be updated. Check it and try again."
    }

    class Factory(
        private val facade: NativeAttendanceFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeAttendanceViewModel::class.java))
            return NativeAttendanceViewModel(facade, onSessionExpired) as T
        }
    }
}
