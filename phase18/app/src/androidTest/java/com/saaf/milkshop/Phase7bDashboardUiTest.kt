package com.saaf.milkshop

import android.widget.Button
import android.widget.TextView
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class Phase7bDashboardUiTest {

    @Test
    fun dashboard_rendersPeriodControls_andSwitchesReportPeriod_withoutWritingEvents() {
        val preferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE)
        val previousOpeningSection = preferences.getString("default_opening_section", null)
        preferences.edit().putString("default_opening_section", "dashboard").commit()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForCondition(scenario) { activity ->
                activity.findViewById<Button>(R.id.analyticsTodayButton).visibility == android.view.View.VISIBLE &&
                    activity.findViewById<Button>(R.id.analyticsWeekButton).visibility == android.view.View.VISIBLE &&
                    activity.findViewById<Button>(R.id.analyticsMonthButton).visibility == android.view.View.VISIBLE &&
                    activity.findViewById<Button>(R.id.analyticsYearButton).visibility == android.view.View.VISIBLE &&
                    activity.findViewById<TextView>(R.id.analyticsPeriodLabelText).text.isNotBlank()
            }

            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.analyticsWeekButton).performClick()
            }

            waitForCondition(scenario) { activity ->
                activity.findViewById<TextView>(R.id.analyticsPeriodLabelText)
                    .text.toString().contains("This Week")
            }
        } finally {
            scenario.close()
            val editor = preferences.edit()
            if (previousOpeningSection == null) {
                editor.remove("default_opening_section")
            } else {
                editor.putString("default_opening_section", previousOpeningSection)
            }
            editor.commit()
        }
    }

    private fun waitForCondition(
        scenario: ActivityScenario<MainActivity>,
        timeoutMillis: Long = 5000,
        condition: (MainActivity) -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            var satisfied = false
            scenario.onActivity { activity -> satisfied = condition(activity) }
            if (satisfied) return
            Thread.sleep(50)
        }
        throw AssertionError("UI condition was not met within ${timeoutMillis}ms")
    }
}
