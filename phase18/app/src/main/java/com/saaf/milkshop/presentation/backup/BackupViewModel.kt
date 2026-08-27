package com.saaf.milkshop.presentation.backup

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation state for backup/restore progress and status. */
class BackupViewModel : ViewModel() {
    data class State(
        val isBusy: Boolean = false,
        val statusMessage: String = "",
    )

    private val _state = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _state.asStateFlow()
    val state: State get() = _state.value

    fun beginRestore() {
        _state.value = state.copy(isBusy = true, statusMessage = "Restoring…")
    }

    fun setStatus(message: String) {
        _state.value = state.copy(statusMessage = message)
    }

    fun finishRestore() {
        _state.value = state.copy(isBusy = false)
    }

    fun clearStatus() {
        _state.value = state.copy(statusMessage = "")
    }

    fun beginBackup() {
        _state.value = state.copy(isBusy = true, statusMessage = "")
    }

    fun finishBackup() {
        _state.value = state.copy(isBusy = false)
    }
}
