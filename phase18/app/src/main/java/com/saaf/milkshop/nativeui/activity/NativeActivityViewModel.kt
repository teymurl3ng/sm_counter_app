package com.saaf.milkshop.nativeui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.activity.NativeActivityDay
import com.saaf.milkshop.application.activity.NativeActivityFacade
import com.saaf.milkshop.application.activity.NativeActivityRecordDetail
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

sealed interface NativeActivityState {
    data object Idle : NativeActivityState
    data object Loading : NativeActivityState
    data class Ready(
        val day: NativeActivityDay,
        val detail: NativeActivityRecordDetail? = null,
        val loadingDetail: Boolean = false,
        val error: String? = null,
    ) : NativeActivityState
    data class LoadFailure(val message: String) : NativeActivityState
}

class NativeActivityViewModel(
    private val facade: NativeActivityFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeActivityState>(NativeActivityState.Idle)
    private var sessionHandle: String? = null
    private var detailGeneration = 0L
    val state: StateFlow<NativeActivityState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun refresh() {
        val session = sessionHandle ?: return
        detailGeneration += 1L
        _state.value = NativeActivityState.Loading
        scope.launch {
            try {
                val day = withContext(Dispatchers.IO) { facade.loadToday(session) }
                _state.value = NativeActivityState.Ready(day = day)
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun openDetail(eventId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeActivityState.Ready ?: return
        val generation = ++detailGeneration
        _state.value = current.copy(loadingDetail = true, error = null)
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { facade.loadDetail(session, eventId) }
                if (generation == detailGeneration) {
                    val latest = _state.value as? NativeActivityState.Ready ?: return@launch
                    _state.value = latest.copy(detail = detail, loadingDetail = false, error = null)
                }
            } catch (error: Throwable) {
                if (generation == detailGeneration) handleDetailFailure(current, error)
            }
        }
    }

    fun openDetailFromCollect(session: String, eventId: Long) {
        sessionHandle = session
        detailGeneration += 1L
        _state.value = NativeActivityState.Loading
        scope.launch {
            try {
                val day = withContext(Dispatchers.IO) { facade.loadToday(session) }
                val detail = withContext(Dispatchers.IO) { facade.loadDetail(session, eventId) }
                _state.value = NativeActivityState.Ready(day = day, detail = detail)
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun clearDetail() {
        val current = _state.value as? NativeActivityState.Ready ?: return
        detailGeneration += 1L
        _state.value = current.copy(detail = null, loadingDetail = false, error = null)
    }

    fun close() {
        detailGeneration += 1L
        sessionHandle = null
        _state.value = NativeActivityState.Idle
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleDetailFailure(previous: NativeActivityState.Ready, error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeActivityState.Idle
            onSessionExpired()
        } else {
            _state.value = previous.copy(
                loadingDetail = false,
                error = publicMessage(error),
            )
        }
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeActivityState.Idle
            onSessionExpired()
        } else {
            _state.value = NativeActivityState.LoadFailure(publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Activity is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "That record is not available."
        else -> "Activity could not be loaded. Try again."
    }

    class Factory(
        private val facade: NativeActivityFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeActivityViewModel::class.java))
            return NativeActivityViewModel(facade, onSessionExpired) as T
        }
    }
}
