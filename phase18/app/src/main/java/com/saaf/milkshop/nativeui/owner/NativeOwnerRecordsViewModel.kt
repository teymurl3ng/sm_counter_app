package com.saaf.milkshop.nativeui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.owner.NativeCorrectionPrefill
import com.saaf.milkshop.application.owner.NativeOwnerRecordsDetail
import com.saaf.milkshop.application.owner.NativeOwnerRecordsFacade
import com.saaf.milkshop.application.owner.NativeOwnerRecordsRow
import com.saaf.milkshop.application.owner.NativeOwnerRecordsSnapshot
import com.saaf.milkshop.application.owner.NativeRecordsGroup
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

data class NativeCorrectionEditorState(
    val prefill: NativeCorrectionPrefill,
    val notes: String = prefill.notes,
    val purchaseRawLiters: String = prefill.purchaseRawLiters,
    val purchaseFat: String = prefill.purchaseFat,
    val purchaseLr: String = prefill.purchaseLr,
    val productionMilk: String = prefill.productionMilk,
    val productionYogurt: String = prefill.productionYogurt,
    val saleRate: String = prefill.saleRate,
    val saleSurcharge: String = prefill.saleSurcharge,
    val saleQuantityMode: Boolean = true,
    val saleQuantity: String = prefill.saleQuantity,
    val saleValue: String = prefill.saleValue,
    val saleChannel: String = prefill.saleChannel,
    val monetaryAmount: String = prefill.monetaryAmount,
    val spoilageQuantity: String = prefill.spoilageQuantity,
    val spoilageReason: String = prefill.spoilageReason,
    val saving: Boolean = false,
    val error: String? = null,
    val draftTouched: Boolean = false,
) {
    val dirty: Boolean get() = draftTouched && !saving
}

sealed interface NativeOwnerRecordsState {
    data object Idle : NativeOwnerRecordsState
    data object Loading : NativeOwnerRecordsState
    data class Ready(
        val snapshot: NativeOwnerRecordsSnapshot,
        val searchQuery: String = "",
        val activeGroup: NativeRecordsGroup = NativeRecordsGroup.Recent,
        val visibleRecords: List<NativeOwnerRecordsRow> = snapshot.records,
        val detail: NativeOwnerRecordsDetail? = null,
        val correction: NativeCorrectionEditorState? = null,
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
        val previous = _state.value as? NativeOwnerRecordsState.Ready
        val previousQuery = previous?.searchQuery.orEmpty()
        val previousGroup = previous?.activeGroup ?: NativeRecordsGroup.Recent
        detailGeneration += 1L
        _state.value = NativeOwnerRecordsState.Loading
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadHistory(session) }
                _state.value = NativeOwnerRecordsState.Ready(
                    snapshot = snapshot,
                    searchQuery = previousQuery,
                    activeGroup = previousGroup,
                    visibleRecords = facade.filterRecords(snapshot.records, previousQuery, previousGroup),
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
            visibleRecords = facade.filterRecords(current.snapshot.records, query, current.activeGroup),
            error = null,
        )
    }

    fun setActiveGroup(group: NativeRecordsGroup) {
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        if (current.activeGroup == group) return
        _state.value = current.copy(
            activeGroup = group,
            visibleRecords = facade.filterRecords(current.snapshot.records, current.searchQuery, group),
            error = null,
        )
    }

    fun openDetail(eventId: Long) {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        val generation = ++detailGeneration
        _state.value = current.copy(loadingDetail = true, error = null, correction = null)
        scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { facade.loadDetail(session, eventId) }
                if (generation == detailGeneration) {
                    val latest = _state.value as? NativeOwnerRecordsState.Ready ?: return@launch
                    _state.value = latest.copy(
                        detail = detail,
                        loadingDetail = false,
                        error = null,
                        correction = null,
                    )
                }
            } catch (error: Throwable) {
                if (generation == detailGeneration) handleDetailFailure(current, error)
            }
        }
    }

    fun clearDetail() {
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        detailGeneration += 1L
        _state.value = current.copy(detail = null, loadingDetail = false, error = null, correction = null)
    }

    fun openCorrection(prefill: NativeCorrectionPrefill) {
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        _state.value = current.copy(
            correction = NativeCorrectionEditorState(prefill = prefill),
            error = null,
        )
    }

    fun clearCorrection() {
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        _state.value = current.copy(correction = null, error = null)
    }

    fun updateCorrection(transform: (NativeCorrectionEditorState) -> NativeCorrectionEditorState) {
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        val editor = current.correction ?: return
        if (editor.saving) return
        _state.value = current.copy(correction = transform(editor.copy(draftTouched = true, error = null)))
    }

    fun saveCorrection() {
        val session = sessionHandle ?: return
        val current = _state.value as? NativeOwnerRecordsState.Ready ?: return
        val editor = current.correction ?: return
        if (editor.saving) return
        val input = try {
            facade.buildCorrectionInput(
                prefill = editor.prefill,
                notes = editor.notes,
                purchaseRawLiters = editor.purchaseRawLiters,
                purchaseFat = editor.purchaseFat,
                purchaseLr = editor.purchaseLr,
                productionMilk = editor.productionMilk,
                productionYogurt = editor.productionYogurt,
                saleRate = editor.saleRate,
                saleSurcharge = editor.saleSurcharge,
                saleQuantityMode = editor.saleQuantityMode,
                saleQuantity = editor.saleQuantity,
                saleValue = editor.saleValue,
                saleChannel = editor.saleChannel,
                monetaryAmount = editor.monetaryAmount,
                spoilageQuantity = editor.spoilageQuantity,
                spoilageReason = editor.spoilageReason,
            )
        } catch (error: Throwable) {
            _state.value = current.copy(
                correction = editor.copy(error = publicMessage(error)),
            )
            return
        }
        _state.value = current.copy(correction = editor.copy(saving = true, error = null))
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { facade.correct(session, input) }
                val snapshot = withContext(Dispatchers.IO) { facade.loadHistory(session) }
                val detail = withContext(Dispatchers.IO) { facade.loadDetail(session, result.eventId) }
                val latest = _state.value as? NativeOwnerRecordsState.Ready ?: return@launch
                _state.value = latest.copy(
                    snapshot = snapshot,
                    visibleRecords = facade.filterRecords(
                        snapshot.records,
                        latest.searchQuery,
                        latest.activeGroup,
                    ),
                    detail = detail,
                    correction = null,
                    loadingDetail = false,
                    error = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.SessionExpired) {
                    sessionHandle = null
                    _state.value = NativeOwnerRecordsState.Idle
                    onSessionExpired()
                    return@launch
                }
                val latest = _state.value as? NativeOwnerRecordsState.Ready ?: return@launch
                val currentEditor = latest.correction ?: return@launch
                _state.value = latest.copy(
                    correction = currentEditor.copy(saving = false, error = publicMessage(error)),
                )
            }
        }
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
