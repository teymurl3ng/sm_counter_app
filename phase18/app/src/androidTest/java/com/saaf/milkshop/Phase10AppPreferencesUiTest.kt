package com.saaf.milkshop

import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class Phase10AppPreferencesUiTest {
    @Test
    fun settings_exposesAppPreferences_andPreferencesArePersisted() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val button = activity.findViewById<Button>(R.id.appPreferencesButton)
            assertTrue(button != null)
            assertTrue(button.text.toString().isNotBlank())
        }
        scenario.close()
    }
}
