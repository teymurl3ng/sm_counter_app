package com.saaf.milkshop.presentation.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.saaf.milkshop.R

/**
 * Presentation-only owner for the App Preferences dialog.
 *
 * Preference keys, defaults, stored values, and save semantics remain unchanged.
 * The host activity remains responsible for applying the saved state after the
 * dialog is accepted.
 */
class AppPreferencesUi(
    private val activity: Activity,
    private val preferences: SharedPreferences,
    private val viewModel: SettingsViewModel,
    private val onPreferencesSaved: () -> Unit,
) {
    fun showAppPreferencesDialog() {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 8)
        }

        fun label(text: String): TextView = TextView(activity).apply {
            this.text = text
            textSize = 16f
            setPadding(0, 14, 0, 4)
        }

        val themeGroup = RadioGroup(activity).apply { orientation = RadioGroup.VERTICAL }
        val themeValues = listOf(
            "system" to activity.getString(R.string.pref_theme_system),
            "light" to activity.getString(R.string.pref_theme_light),
            "dark" to activity.getString(R.string.pref_theme_dark),
        )
        viewModel.loadPreferences(
            preferences.getString("theme", null),
            preferences.getString("language", null),
            preferences.getString("display_density", null),
            preferences.getString("default_reporting_period", null),
            preferences.getString("default_opening_section", null),
        )
        val draft = viewModel.preferencesDraft
        val currentTheme = draft.theme
        themeValues.forEach { (value, title) ->
            val rb = RadioButton(activity).apply { text = title; tag = value }
            rb.isChecked = value == currentTheme
            themeGroup.addView(rb)
        }
        container.addView(label(activity.getString(R.string.pref_theme_title)))
        container.addView(themeGroup)

        val languageGroup = RadioGroup(activity).apply { orientation = RadioGroup.VERTICAL }
        val languageValues = listOf(
            "en" to activity.getString(R.string.pref_language_english),
            "ur" to activity.getString(R.string.pref_language_urdu),
        )
        val currentLanguage = draft.language
        languageValues.forEach { (value, title) ->
            val rb = RadioButton(activity).apply { text = title; tag = value }
            rb.isChecked = value == currentLanguage
            languageGroup.addView(rb)
        }
        container.addView(label(activity.getString(R.string.pref_language_title)))
        container.addView(languageGroup)
        val languageNote = TextView(activity).apply {
            text = activity.getString(R.string.pref_language_note)
            textSize = 13f
        }
        container.addView(languageNote)

        val densityGroup = RadioGroup(activity).apply { orientation = RadioGroup.VERTICAL }
        val densityValues = listOf(
            "comfortable" to activity.getString(R.string.pref_density_comfortable),
            "compact" to activity.getString(R.string.pref_density_compact),
        )
        val currentDensity = draft.density
        densityValues.forEach { (value, title) ->
            val rb = RadioButton(activity).apply { text = title; tag = value }
            rb.isChecked = value == currentDensity
            densityGroup.addView(rb)
        }
        container.addView(label(activity.getString(R.string.pref_density_title)))
        container.addView(densityGroup)

        val periodGroup = RadioGroup(activity).apply { orientation = RadioGroup.VERTICAL }
        val periodValues = listOf(
            "today" to activity.getString(R.string.analytics_period_today),
            "week" to activity.getString(R.string.analytics_period_week),
            "month" to activity.getString(R.string.analytics_period_month),
            "year" to activity.getString(R.string.analytics_period_year),
        )
        val currentPeriod = draft.period
        periodValues.forEach { (value, title) ->
            val rb = RadioButton(activity).apply { text = title; tag = value }
            rb.isChecked = value == currentPeriod
            periodGroup.addView(rb)
        }
        container.addView(label(activity.getString(R.string.pref_reporting_period_title)))
        container.addView(periodGroup)

        val openingGroup = RadioGroup(activity).apply { orientation = RadioGroup.VERTICAL }
        val openingValues = listOf(
            "dashboard" to activity.getString(R.string.pref_opening_dashboard),
            "budgets" to activity.getString(R.string.pref_opening_budgets),
            "backup" to activity.getString(R.string.pref_opening_backup),
            "settings" to activity.getString(R.string.pref_opening_settings),
        )
        val currentOpening = draft.openingSection
        openingValues.forEach { (value, title) ->
            val rb = RadioButton(activity).apply { text = title; tag = value }
            rb.isChecked = value == currentOpening
            openingGroup.addView(rb)
        }
        container.addView(label(activity.getString(R.string.pref_opening_section_title)))
        container.addView(openingGroup)

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.app_preferences_title)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save_preferences, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val theme = themeGroup.findViewById<RadioButton>(themeGroup.checkedRadioButtonId)
                    ?.tag?.toString() ?: "system"
                val language = languageGroup.findViewById<RadioButton>(languageGroup.checkedRadioButtonId)
                    ?.tag?.toString() ?: "en"
                val density = densityGroup.findViewById<RadioButton>(densityGroup.checkedRadioButtonId)
                    ?.tag?.toString() ?: "comfortable"
                val period = periodGroup.findViewById<RadioButton>(periodGroup.checkedRadioButtonId)
                    ?.tag?.toString() ?: "month"
                val opening = openingGroup.findViewById<RadioButton>(openingGroup.checkedRadioButtonId)
                    ?.tag?.toString() ?: "dashboard"

                val newDraft = SettingsViewModel.PreferencesDraft(
                    theme = theme,
                    language = language,
                    density = density,
                    period = period,
                    openingSection = opening,
                )
                viewModel.updatePreferences(newDraft)
                preferences.edit()
                    .putString("theme", newDraft.theme)
                    .putString("language", newDraft.language)
                    .putString("display_density", newDraft.density)
                    .putString("default_reporting_period", newDraft.period)
                    .putString("default_opening_section", newDraft.openingSection)
                    .apply()

                onPreferencesSaved()
                dialog.dismiss()
                activity.recreate()
            }
        }
        dialog.show()
    }
}
