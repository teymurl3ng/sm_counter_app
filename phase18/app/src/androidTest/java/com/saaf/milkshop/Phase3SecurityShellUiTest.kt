package com.saaf.milkshop

import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3SecurityShellUiTest {
    @Test
    fun authenticatedOwnerSessionUnlocksShellAndAccountControls() {
        // A complete device suite can outlast the emulator's screen timeout.
        // Normalize that external device state before testing Activity visibility.
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            val deadline = SystemClock.uptimeMillis() + 5_000L
            var visible = false
            while (!visible && SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity { activity ->
                    visible = activity.findViewById<View>(R.id.appRoot).visibility == View.VISIBLE
                }
                if (!visible) SystemClock.sleep(50)
            }
            assertTrue("Authenticated shell did not become visible", visible)
            scenario.onActivity { activity ->
                val current = activity.findViewById<TextView>(R.id.currentUserText).text.toString()
                assertTrue(current.contains("Instrumentation Owner"))
                assertTrue(current.contains("owner"))
                assertEquals(View.VISIBLE, activity.findViewById<Button>(R.id.manageUsersButton).visibility)
                assertTrue(activity.findViewById<Button>(R.id.restoreBackupButton).isEnabled)
            }
        } finally {
            scenario.close()
        }
    }
}
