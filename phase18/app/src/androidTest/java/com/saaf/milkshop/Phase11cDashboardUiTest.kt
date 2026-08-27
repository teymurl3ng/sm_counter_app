package com.saaf.milkshop

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class Phase11cDashboardUiTest {

    @Test
    fun dashboard_hasReadableContentAndStablePeriodControls() {
        val preferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE)
        val previousOpeningSection = preferences.getString("default_opening_section", null)
        preferences.edit().putString("default_opening_section", "dashboard").commit()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                val milk = activity.findViewById<TextView>(R.id.milkStockText)
                val sales = activity.findViewById<TextView>(R.id.analyticsSalesCardText)
                val today = activity.findViewById<Button>(R.id.analyticsTodayButton)
                val month = activity.findViewById<Button>(R.id.analyticsMonthButton)

                assertTrue(milk.currentTextColor != Color.WHITE)
                assertTrue(sales.currentTextColor != Color.WHITE)
                assertTrue(today.currentTextColor == Color.WHITE)
                assertTrue(month.currentTextColor == Color.WHITE)
                assertTrue(today.visibility == View.VISIBLE)
                assertTrue(month.visibility == View.VISIBLE)
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
}
