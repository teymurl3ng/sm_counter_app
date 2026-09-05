package com.saaf.milkshop

import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase12ResponsiveAccessibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun increasedTextReflowsAcrossRepresentativeScreens() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            driver.evaluate("document.documentElement.style.fontSize = '125%'; true")

            val destinations = listOf(
                Triple("dashboard-nav", "Dashboard", "run-report-button"),
                Triple("operations-nav", "Daily work", "daily-task-picker"),
                Triple("products-nav", "Products", "native-product-list"),
                Triple("settings-nav", "Settings", "business-settings-tab"),
                Triple("access-nav", "Access & users", "security-account-tab"),
                Triple("backup-nav", "Backup & restore", "backup-create-tab"),
            )
            destinations.forEach { (navigationId, title, readyId) ->
                driver.evaluate("document.getElementById(${JSONObject.quote(navigationId)}).click(); true")
                driver.waitFor(
                    "document.querySelector('.hybrid-header h1').textContent === ${JSONObject.quote(title)} && document.getElementById(${JSONObject.quote(readyId)}) !== null",
                    20_000L,
                )
                assertEquals("true", driver.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1"))
                assertEquals(
                    "true",
                    driver.evaluate(
                        "Array.from(document.querySelectorAll('#app-navigation button')).every((button) => button.scrollWidth <= button.clientWidth + 1 || getComputedStyle(button).whiteSpace === 'normal')",
                    ),
                )
            }

            driver.evaluate("document.getElementById('settings-nav').click(); true")
            driver.waitFor("document.getElementById('business-settings-tab') !== null", 20_000L)
            assertEquals(
                "true",
                driver.evaluate("Array.from(document.querySelectorAll('#business-settings-tab, #opening-state-tab, #expense-categories-tab')).every((button) => button.scrollWidth <= button.clientWidth + 1)"),
            )
            driver.evaluate("document.documentElement.style.fontSize = ''; true")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun landscapeKeepsNavigationAndWorkFormsInsideTheViewport() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            SystemClock.sleep(1_500)
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("window.innerWidth > window.innerHeight && document.getElementById('app-navigation') !== null", 20_000L)
            assertEquals("true", driver.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1"))

            driver.evaluate("document.getElementById('operations-nav').click(); true")
            driver.waitFor("document.getElementById('daily-task-panel') !== null", 20_000L)
            assertEquals("true", driver.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1"))
            assertEquals(
                "true",
                driver.evaluate("document.getElementById('daily-task-panel').getBoundingClientRect().right <= document.documentElement.clientWidth + 1"),
            )
        } finally {
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            SystemClock.sleep(500)
            scenario.close()
        }
    }

    private fun wakeDevice() {
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(300)
    }
}
