package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.owner.NativeOwnerReportsFacade
import com.saaf.milkshop.application.owner.NativeOwnerReportsPreset
import com.saaf.milkshop.application.owner.NativeOwnerReportsSnapshot
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
import java.time.LocalDate

sealed interface NativeOwnerReportsState {
    data object Idle : NativeOwnerReportsState
    data object Loading : NativeOwnerReportsState
    data class Ready(
        val snapshot: NativeOwnerReportsSnapshot,
        val error: String? = null,
    ) : NativeOwnerReportsState
    data class LoadFailure(val message: String) : NativeOwnerReportsState
}

class NativeOwnerReportsViewModel(
    private val facade: NativeOwnerReportsFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOwnerReportsState>(NativeOwnerReportsState.Idle)
    private var sessionHandle: String? = null
    private var preset: NativeOwnerReportsPreset = NativeOwnerReportsPreset.ThisMonth
    private val loadGate = SessionRequestGate()
    val state: StateFlow<NativeOwnerReportsState> = _state.asStateFlow()

    fun open(nextSessionHandle: String, today: LocalDate = LocalDate.now()) {
        if (
            sessionHandle == nextSessionHandle &&
            (_state.value is NativeOwnerReportsState.Ready ||
                _state.value is NativeOwnerReportsState.Loading)
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        preset = NativeOwnerReportsPreset.ThisMonth
        refresh(today)
    }

    fun close() {
        loadGate.invalidate()
        sessionHandle = null
        preset = NativeOwnerReportsPreset.ThisMonth
        _state.value = NativeOwnerReportsState.Idle
    }

    fun selectPreset(next: NativeOwnerReportsPreset, today: LocalDate = LocalDate.now()) {
        if (preset == next && _state.value is NativeOwnerReportsState.Ready) return
        preset = next
        refresh(today)
    }

    fun refresh(today: LocalDate = LocalDate.now()) {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        _state.value = NativeOwnerReportsState.Loading
        scope.launch {
            try {
                val (start, end) = facade.rangeForPreset(preset, today)
                val snapshot = withContext(Dispatchers.IO) {
                    facade.load(session, start, end, preset, today)
                }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeOwnerReportsState.Ready(snapshot = snapshot)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                handleLoadFailure(error)
            }
        }
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerReportsState.Idle
            onSessionExpired()
            return
        }
        _state.value = NativeOwnerReportsState.LoadFailure(
            error.message ?: "Month reports could not open. Try again.",
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    class Factory(
        private val facade: NativeOwnerReportsFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOwnerReportsViewModel::class.java))
            return NativeOwnerReportsViewModel(facade, onSessionExpired) as T
        }
    }
}
