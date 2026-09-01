package com.saaf.milkshop

import android.os.SystemClock
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase9WindowInsetsTest {
    @Test
    fun systemBarsAndKeyboardKeepTheActiveDialogFieldUsable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null && document.documentElement.dataset.viewportReady === 'true'", 20_000L)
            driver.waitFor("parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--saaf-system-top')) > 0")
            assertEquals("true", driver.evaluate("document.documentElement.dataset.imeVisible === 'false'"))
            scenario.onActivity { activity ->
                val adjustMode = activity.window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
                assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE, adjustMode)
            }

            driver.evaluate("document.getElementById('navigation-more-button').click(); true")
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
            driver.evaluate("document.getElementById('products-nav').click(); true")
            driver.waitFor("document.getElementById('add-product-button') !== null")
            driver.evaluate("document.getElementById('add-product-button').click(); true")
            driver.waitFor("document.getElementById('product-notes') !== null")
            driver.evaluate("document.getElementById('product-notes').scrollIntoView({ block: 'center' }); true")
            SystemClock.sleep(300)

            tapElement(driver, "product-notes")
            driver.waitFor("document.activeElement?.id === 'product-notes' && document.documentElement.dataset.imeVisible === 'true'", 15_000L)
            driver.waitFor("(() => { const field = document.getElementById('product-notes').getBoundingClientRect(); const viewport = window.visualViewport; return field.bottom <= viewport.offsetTop + viewport.height + 2; })()")

            assertEquals("true", driver.evaluate("getComputedStyle(document.getElementById('app-navigation')).display === 'none'"))
            assertEquals("true", driver.evaluate("document.querySelector('.hybrid-dialog-panel').getBoundingClientRect().height <= window.visualViewport.height + 2"))
            assertTrue(driver.evaluate("parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--saaf-ime-bottom')) > 0") == "true")

            instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
            driver.waitFor("document.documentElement.dataset.imeVisible === 'false'", 10_000L)
            assertEquals("true", driver.evaluate("getComputedStyle(document.getElementById('app-navigation')).display !== 'none'"))
        } finally {
            scenario.close()
        }
    }

    private fun tapElement(driver: HybridWebViewTestDriver, elementId: String) {
        val bounds = JSONObject(
            requireNotNull(
                driver.evaluate(
                    """
                        (() => {
                          const element = document.getElementById(${JSONObject.quote(elementId)});
                          const bounds = element.getBoundingClientRect();
                          return { x: bounds.left + bounds.width / 2, y: bounds.top + bounds.height / 2, viewportWidth: document.documentElement.clientWidth };
                        })()
                    """.trimIndent(),
                ),
            ),
        )
        val location = IntArray(2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { driver.webView.getLocationOnScreen(location) }
        val scale = driver.webView.width.toDouble() / bounds.getDouble("viewportWidth")
        val screenX = (location[0] + bounds.getDouble("x") * scale).toInt()
        val screenY = (location[1] + bounds.getDouble("y") * scale).toInt()
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input tap $screenX $screenY").close()
    }
}
