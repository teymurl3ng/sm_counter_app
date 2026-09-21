package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.owner.NativeOwnerRecordsDetail
import com.saaf.milkshop.application.owner.NativeOwnerRecordsFacade
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.application.owner.NativeOwnerRecordsSnapshot
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

sealed interface NativeOwnerRecordsState {
    data object Idle : NativeOwnerRecordsState
    data object Loading : NativeOwnerRecordsState
    data class Ready(
        val snapshot: NativeOwnerRecordsSnapshot,
        val searchQuery: String = "",
        val visibleRecords: List<NativeOwnerRecordsRow> = snapshot.records,
        val detail: NativeOwnerRecordsDetail? = null,
        val loadingDetail: Boolean = false,
        val error: String? = null,
    ) : NativeOwnerRecordsState
    data class LoadFailure(val message: String) : NativeOwnerRecordsState
}

class NativeOwnerRecordsViewModel(
    private val facade: NativeOwnerRecordsFacade,
    private val onSessionExpired: () -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeOwnerRecordsState>(NativeOwnerRecordsState.Idle)
    private var sessionHandle: String? = null
    private var detailGeneration = 0L
    val state: StateFlow<NativeOwnerRecordsState> = _state.asStateFlow()

    fun open(nextSessionHandle: String) {
        if (sessionHandle == nextSessionHandle &&
            _state.value is NativeOwnerRecordsState.Ready
        ) {
            return
        }
        sessionHandle = nextSessionHandle
        refresh()
    }

    fun close() {
        detailGeneration += 1L
        sessionHandle = null
        _state.value = NativeOwnerRecordsState.Idle
    }

    fun refresh() {
        val session = sessionHandle ?: return
        val previousQuery = (_state.value as? NativeOwnerRecordsState.Ready)?.searchQuery.orEmpty()
        detailGeneration += 1L
        _state.value = NativeOwnerRecordsState.Loading
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadHistory(session) }
                _state.value = NativeOwnerRecordsState.Ready(
                    snapshot = snapshot,
                    searchQuery = previousQuery,
                    visibleRecords = facade.filterRecords(snapshot.records, previousQuery),
                )
            } catch (error: Throwable) {
                handleLoadFailure(error)
            }
        }
    }

    fun setSearchQuery(query: String) {
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        _state.value = current.copy(
            searchQuery = query,
            visibleRecords = facade.filterRecords(current.snapshot.records, query),
            error = null,
        )
    }

    fun openDetail(eventId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        val generation = ++detailGeneration
        _state.value = current.copy(loadingDetail = true, error = null)
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { facade.loadDetail(session, eventId) }
                if (generation == detailGeneration) {
                    val latest = _state.value as? NativeOwnerRecordsState.Ready ?: return@launch
                    _state.value = latest.copy(detail = detail, loadingDetail = false, error = null)
                }
            } catch (error: Throwable) {
                if (generation == detailGeneration) handleDetailFailure(current, error)
            }
        }
    }

    fun clearDetail() {
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        detailGeneration += 1L
        _state.value = current.copy(detail = null, loadingDetail = false, error = null)
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun handleDetailFailure(previous: NativeOwnerRecordsState.Ready, error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerRecordsState.Idle
            onSessionExpired()
            return
        }
        _state.value = previous.copy(
            loadingDetail = false,
            error = publicMessage(error),
        )
    }

    private fun handleLoadFailure(error: Throwable) {
        if (error is SecurityFailure.SessionExpired) {
            sessionHandle = null
            _state.value = NativeOwnerRecordsState.Idle
            onSessionExpired()
            return
        }
        _state.value = NativeOwnerRecordsState.LoadFailure(publicMessage(error))
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is SecurityFailure -> error.message ?: "Records is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "That record is not available."
        else -> "Records could not be loaded. Try again."
    }

    class Factory(
        private val facade: NativeOwnerRecordsFacade,
        private val onSessionExpired: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeOwnerRecordsViewModel::class.java))
            return NativeOwnerRecordsViewModel(facade, onSessionExpired) as T
        }
    }
}
