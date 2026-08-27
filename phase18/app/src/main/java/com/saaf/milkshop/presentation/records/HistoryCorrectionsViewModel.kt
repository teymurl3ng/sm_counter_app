package com.saaf.milkshop.presentation.records

import androidx.lifecycle.ViewModel
import com.saaf.milkshop.data.EventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation state for History & Corrections selection/detail. */
class HistoryCorrectionsViewModel : ViewModel() {
    data class State(
        val selectedEventId: Long? = null,
        val detailLoading: Boolean = false,
        val detailText: String = "",
    )

    private val _state = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _state.asStateFlow()
    val state: State get() = _state.value

    fun selectEvent(event: EventEntity) {
        _state.value = state.copy(selectedEventId = event.eventId, detailLoading = true, detailText = "")
    }

    fun setDetail(detail: String) {
        _state.value = state.copy(detailLoading = false, detailText = detail)
    }

    fun clearSelection() {
        _state.value = State()
    }
}
