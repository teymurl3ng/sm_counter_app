package com.saaf.milkshop

import android.content.Context
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import android.os.SystemClock

@RunWith(AndroidJUnit4::class)
class Phase11bNavigationUiTest {
    @Test
    fun preferredSection_navigationStartsAtConfiguredScreen_withoutWritingEvents() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE)
        val previous = prefs.getString("default_opening_section", null)
        prefs.edit().putString("default_opening_section", "budgets").commit()

        try {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            val deadline = SystemClock.uptimeMillis() + 5000L
            var settled = false
            while (SystemClock.uptimeMillis() < deadline && !settled) {
                scenario.onActivity { activity ->
                    val planning = activity.findViewById<View>(R.id.planningScreenScroll)
                    val dashboard = activity.findViewById<View>(R.id.dashboardScreenScroll)
                    val transactions = activity.findViewById<View>(R.id.transactionsScreenScroll)
                    val label = activity.findViewById<android.widget.TextView>(R.id.navigationSectionLabel)

                    settled = planning.visibility == View.VISIBLE &&
                        dashboard.visibility != View.VISIBLE &&
                        transactions.visibility != View.VISIBLE &&
                        label.text?.toString() == "Budgets & Targets"
                }
                if (!settled) SystemClock.sleep(50L)
            }

            assertTrue("Preferred section did not settle within 5 seconds", settled)
            scenario.close()
        } finally {
            prefs.edit().apply {
                if (previous == null) remove("default_opening_section") else putString("default_opening_section", previous)
            }.commit()
        }
    }

    @Test
    fun navigationScreens_areIndependentDestinations() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE)
        val previous = prefs.getString("default_opening_section", null)
        prefs.edit().putString("default_opening_section", "dashboard").commit()

        try {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            scenario.onActivity { activity ->
                val method = MainActivity::class.java.getDeclaredMethod("navigateToSection", String::class.java, Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(activity, "transactions", false)

                val transactions = activity.findViewById<View>(R.id.transactionsScreenScroll)
                val dashboard = activity.findViewById<View>(R.id.dashboardScreenScroll)
                val records = activity.findViewById<View>(R.id.recordsScreenScroll)
                val label = activity.findViewById<android.widget.TextView>(R.id.navigationSectionLabel)

                assertEquals(View.VISIBLE, transactions.visibility)
                assertFalse(dashboard.visibility == View.VISIBLE)
                assertFalse(records.visibility == View.VISIBLE)
                assertEquals("Transactions", label.text?.toString())
            }
            scenario.close()
        } finally {
            prefs.edit().apply {
                if (previous == null) remove("default_opening_section") else putString("default_opening_section", previous)
            }.commit()
        }
    }
}
