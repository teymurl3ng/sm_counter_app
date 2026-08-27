package com.saaf.milkshop.presentation.settings

import androidx.lifecycle.ViewModel
import com.saaf.milkshop.domain.BusinessSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presentation state for Settings.
 *
 * Existing services and SharedPreferences remain the authorities for
 * persistence and business semantics. This ViewModel provides one observable
 * presentation-state contract for both settings surfaces.
 */
class SettingsViewModel : ViewModel() {
    data class PreferencesDraft(
        val theme: String = "system",
        val language: String = "en",
        val density: String = "comfortable",
        val period: String = "month",
        val openingSection: String = "dashboard",
    )

    data class State(
        val businessSettings: BusinessSettings? = null,
        val preferencesDraft: PreferencesDraft = PreferencesDraft(),
        val businessSettingsError: String? = null,
        val businessSettingsSaved: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _state.asStateFlow()
    val state: State get() = _state.value

    val businessSettings: BusinessSettings? get() = state.businessSettings
    val preferencesDraft: PreferencesDraft get() = state.preferencesDraft
    val businessSettingsError: String? get() = state.businessSettingsError
    val businessSettingsSaved: Boolean get() = state.businessSettingsSaved

    fun setBusinessSettings(settings: BusinessSettings) {
        _state.value = state.copy(
            businessSettings = settings,
            businessSettingsError = null,
            businessSettingsSaved = false,
        )
    }

    fun setBusinessSettingsError(message: String?) {
        _state.value = state.copy(
            businessSettingsError = message,
            businessSettingsSaved = false,
        )
    }

    fun markBusinessSettingsSaved() {
        _state.value = state.copy(
            businessSettingsSaved = true,
            businessSettingsError = null,
        )
    }

    fun loadPreferences(
        theme: String?,
        language: String?,
        density: String?,
        period: String?,
        openingSection: String?,
    ) {
        _state.value = state.copy(
            preferencesDraft = PreferencesDraft(
                theme = theme ?: "system",
                language = language ?: "en",
                density = density ?: "comfortable",
                period = period ?: "month",
                openingSection = openingSection ?: "dashboard",
            )
        )
    }

    fun updatePreferences(draft: PreferencesDraft) {
        _state.value = state.copy(preferencesDraft = draft)
    }
}
