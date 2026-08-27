package com.saaf.milkshop.presentation.shell

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.saaf.milkshop.R
import kotlin.math.roundToInt

/**
 * Presentation-only application shell responsibilities extracted from MainActivity.
 * Navigation, window insets, saved visual preferences, and shared spinner styling are
 * preserved exactly; no business/data behavior belongs here.
 */
class AppShellUi(
    private val activity: Activity,
    private val preferences: SharedPreferences,
) {
    private var allowedSections: Set<String> = setOf(
        "dashboard", "budgets", "transactions", "records", "management", "settings", "backup"
    )

    fun setAllowedSections(sections: Set<String>) {
        allowedSections = sections + "dashboard" + "settings"
    }

    fun applySavedTheme() {
        when (preferences.getString("theme", "system")) {
            "dark" -> activity.setTheme(android.R.style.Theme_Material_NoActionBar)
            "light" -> activity.setTheme(android.R.style.Theme_Material_Light_NoActionBar)
            else -> {
                val isNight = (activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                activity.setTheme(
                    if (isNight) android.R.style.Theme_Material_NoActionBar
                    else android.R.style.Theme_Material_Light_NoActionBar
                )
            }
        }
    }

    fun applySavedDisplayDensity() {
        val padding = if (preferences.getString("display_density", "comfortable") == "compact") 12 else 20
        listOf(
            R.id.dashboardScreenContent,
            R.id.planningScreenContent,
            R.id.backupScreenContent,
            R.id.settingsScreenContent,
            R.id.managementScreenContent,
            R.id.transactionsScreenContent,
            R.id.recordsScreenContent
        ).forEach { id ->
            activity.findViewById<View>(id)?.setPadding(padding, padding, padding, padding)
        }
    }

    fun setupWindowInsets() {
        val appRoot = activity.findViewById<View>(R.id.appRoot)
        ViewCompat.setOnApplyWindowInsetsListener(appRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(appRoot)
    }

    fun setupNavigation() {
        val navigationMenuButton = activity.findViewById<View>(R.id.navigationMenuButton)
        val navigationSectionLabel = activity.findViewById<TextView>(R.id.navigationSectionLabel)
        navigationMenuButton.setOnClickListener { anchor ->
            val popup = PopupMenu(activity, anchor)
            if ("dashboard" in allowedSections) popup.menu.add(0, 1, 0, activity.getString(R.string.navigation_dashboard))
            if ("budgets" in allowedSections) popup.menu.add(0, 2, 1, activity.getString(R.string.navigation_planning))
            if ("transactions" in allowedSections) popup.menu.add(0, 3, 2, activity.getString(R.string.navigation_transactions))
            if ("records" in allowedSections) popup.menu.add(0, 4, 3, activity.getString(R.string.navigation_records))
            if ("management" in allowedSections) popup.menu.add(0, 5, 4, activity.getString(R.string.navigation_management))
            if ("settings" in allowedSections) popup.menu.add(0, 6, 5, activity.getString(R.string.navigation_settings))
            if ("backup" in allowedSections) popup.menu.add(0, 7, 6, activity.getString(R.string.navigation_backup))
            popup.setOnMenuItemClickListener { item ->
                val section = when (item.itemId) {
                    1 -> "dashboard"
                    2 -> "budgets"
                    3 -> "transactions"
                    4 -> "records"
                    5 -> "management"
                    6 -> "settings"
                    7 -> "backup"
                    else -> "dashboard"
                }
                navigateToSection(section, persist = false)
                true
            }
            popup.show()
        }
        navigationMenuButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun scrollToPreferredOpeningSection() {
        val key = preferences.getString("default_opening_section", "dashboard")
        when (key) {
            "budgets" -> navigateToSection("budgets", persist = false)
            "backup" -> navigateToSection("backup", persist = false)
            "settings" -> navigateToSection("settings", persist = false)
            else -> navigateToSection("dashboard", persist = false)
        }
    }

    fun navigateToSection(section: String, persist: Boolean) {
        val planning = activity.findViewById<ScrollView>(R.id.planningScreenScroll)
        val backup = activity.findViewById<ScrollView>(R.id.backupScreenScroll)
        val settings = activity.findViewById<ScrollView>(R.id.settingsScreenScroll)
        val management = activity.findViewById<ScrollView>(R.id.managementScreenScroll)
        val transactions = activity.findViewById<ScrollView>(R.id.transactionsScreenScroll)
        val records = activity.findViewById<ScrollView>(R.id.recordsScreenScroll)
        val dashboard = activity.findViewById<ScrollView>(R.id.dashboardScreenScroll)
        val labelView = activity.findViewById<TextView>(R.id.navigationSectionLabel)
        val permittedSection = if (section in allowedSections) section else "dashboard"
        val target = when (permittedSection) {
            "budgets" -> planning
            "backup" -> backup
            "settings" -> settings
            "management" -> management
            "transactions" -> transactions
            "records" -> records
            else -> dashboard
        }
        val label = when (permittedSection) {
            "budgets" -> R.string.navigation_planning
            "transactions" -> R.string.navigation_transactions
            "records" -> R.string.navigation_records
            "management" -> R.string.navigation_management
            "settings" -> R.string.navigation_settings
            "backup" -> R.string.navigation_backup
            else -> R.string.navigation_dashboard
        }
        labelView.text = activity.getString(label)
        listOf(dashboard, planning, backup, settings, management, transactions, records)
            .forEach { screen -> screen.visibility = if (screen === target) View.VISIBLE else View.GONE }
        target.scrollTo(0, 0)
        if (persist) {
            preferences.edit().putString("last_navigation_section", section).apply()
        }
    }

    fun <T> createSpinnerAdapter(items: List<T>): ArrayAdapter<T> {
        return object : ArrayAdapter<T>(activity, android.R.layout.simple_spinner_item, items) {
            private fun styleView(view: View): View {
                (view as? TextView)?.apply {
                    setTextColor(activity.getColor(R.color.milk_shop_color_on_surface))
                    textSize = 16f
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    setBackgroundColor(activity.getColor(R.color.milk_shop_color_surface))
                }
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                styleView(super.getView(position, convertView, parent))

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                styleView(super.getDropDownView(position, convertView, parent))
        }
    }

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).roundToInt()
}
