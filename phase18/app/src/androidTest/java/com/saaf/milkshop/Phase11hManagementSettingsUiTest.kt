package com.saaf.milkshop

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11hManagementSettingsUiTest {

    @Test
    fun managementAndSettings_useDistinctComposedSurfaces() {
        runBlocking {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    activity.findViewById<LinearLayout>(R.id.vendorManagementCard).visibility == View.VISIBLE &&
                        activity.findViewById<LinearLayout>(R.id.businessSettingsCard).visibility == View.VISIBLE
                }

                scenario.onActivity { activity ->
                    val vendorCard = activity.findViewById<LinearLayout>(R.id.vendorManagementCard)
                    val customerCard = activity.findViewById<LinearLayout>(R.id.customerManagementCard)
                    val employeeCard = activity.findViewById<LinearLayout>(R.id.employeeManagementCard)
                    val businessCard = activity.findViewById<LinearLayout>(R.id.businessSettingsCard)
                    val preferencesCard = activity.findViewById<LinearLayout>(R.id.appPreferencesCard)

                    assertTrue(vendorCard.background != null)
                    assertTrue(customerCard.background != null)
                    assertTrue(employeeCard.background != null)
                    assertTrue(businessCard.background != null)
                    assertTrue(preferencesCard.background != null)
                    assertEquals(View.VISIBLE, activity.findViewById<Button>(R.id.addVendorManagementButton).visibility)
                    assertEquals(View.VISIBLE, activity.findViewById<Button>(R.id.addCustomerManagementButton).visibility)
                    assertEquals(View.VISIBLE, activity.findViewById<Button>(R.id.addEmployeeManagementButton).visibility)
                    assertEquals(View.VISIBLE, activity.findViewById<Button>(R.id.saveBusinessSettingsButton).visibility)
                    assertEquals(View.VISIBLE, activity.findViewById<Button>(R.id.appPreferencesButton).visibility)
                }
            } finally {
                scenario.close()
            }
        }
    }

    private suspend fun waitForCondition(
        scenario: ActivityScenario<MainActivity>,
        timeoutMs: Long = 5000,
        condition: (MainActivity) -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var matched = false
            scenario.onActivity { activity -> matched = condition(activity) }
            if (matched) return
            delay(50)
        }
        throw AssertionError("UI condition was not met within ${timeoutMs}ms")
    }
}
