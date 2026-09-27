package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.owner.NativeOwnerReportsFacade
import com.saaf.milkshop.application.owner.NativeOwnerReportsPreset
import com.saaf.milkshop.application.owner.NativeOwnerReportsSnapshot
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
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface NativeOwnerReportsState {
    data object Idle : NativeOwnerReportsState
    data object Loading : NativeOwnerReportsState
    data class Ready(
        val snapshot: NativeOwnerReportsSnapshot,
        val refreshing: Boolean = false,
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

    init {
        scope.launch {
            DayStateBus.changes.collect {
                if (sessionHandle != null) refresh()
            }
        }
    }

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

    fun selectCustomRange(
        startInclusive: String,
        endInclusive: String,
        today: LocalDate = LocalDate.now(),
    ) {
        val range = try {
            facade.validateRange(startInclusive, endInclusive)
        } catch (error: IllegalArgumentException) {
            val current = _state.value as? NativeOwnerReportsState.Ready ?: return
            _state.value = current.copy(error = error.message ?: "Check the custom date range")
            return
        }
        preset = NativeOwnerReportsPreset.Custom
        load(range.first, range.second, today)
    }

    fun refresh(today: LocalDate = LocalDate.now()) {
        val current = _state.value as? NativeOwnerReportsState.Ready
        if (preset == NativeOwnerReportsPreset.Custom && current != null) {
            load(current.snapshot.startInclusive, current.snapshot.endExclusive, today)
        } else {
            val (start, end) = facade.rangeForPreset(preset, today)
            load(start, end, today)
        }
    }

    private fun load(start: String, end: String, today: LocalDate) {
        val session = sessionHandle ?: return
        val request = loadGate.begin(session)
        val previous = _state.value as? NativeOwnerReportsState.Ready
        _state.value = previous?.copy(refreshing = true, error = null)
            ?: NativeOwnerReportsState.Loading
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.load(session, start, end, preset, today)
                }
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                _state.value = NativeOwnerReportsState.Ready(snapshot = snapshot)
            } catch (error: Throwable) {
                if (!loadGate.isCurrent(request, sessionHandle)) return@launch
                if (error is SecurityFailure.SessionExpired) {
                    handleLoadFailure(error)
                } else if (previous != null) {
                    _state.value = previous.copy(
                        refreshing = false,
                        error = error.message ?: "Month reports could not refresh. Try again.",
                    )
                } else {
                    handleLoadFailure(error)
                }
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
