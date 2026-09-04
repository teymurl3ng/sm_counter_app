package com.saaf.milkshop

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.WindowCompat
import androidx.core.view.insets.ProtectionLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase12PresentationFoundationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun androidAndCssUseTheLockedPresentationFoundation() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null && document.documentElement.dataset.viewportReady === 'true'", 20_000L)

            scenario.onActivity { activity ->
                val activityContent = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue(activityContent.getChildAt(0) is ProtectionLayout)
                assertFalse(
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .isAppearanceLightStatusBars,
                )
            }

            driver.waitFor("parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--saaf-system-top')) > 0")
            assertTrue(driver.evaluate("getComputedStyle(document.querySelector('.hybrid-header')).backgroundColor === 'rgb(0, 79, 59)'") == "true")
            assertTrue(driver.evaluate("getComputedStyle(document.getElementById('app-navigation')).backdropFilter === 'none'") == "true")
            assertTrue(driver.evaluate("getComputedStyle(document.documentElement).overflowY === 'auto'") == "true")
            assertTrue(
                driver.evaluate(
                    "document.querySelector('.hybrid-header').getBoundingClientRect().top === 0 && document.querySelector('.hybrid-header p').getBoundingClientRect().top >= parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--saaf-system-top'))",
                ) == "true",
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun repeatedSecondaryDestinationChangesPaintTheirSharedChrome() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)

            val destinations = listOf(
                "customers-nav" to "Customers",
                "costing-planning-nav" to "Cost & plans",
                "backup-nav" to "Backup & restore",
            )
            repeat(3) {
                destinations.forEach { (navigationId, title) ->
                    driver.evaluate("document.getElementById('navigation-more-button').click(); true")
                    driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
                    driver.evaluate("document.getElementById(${JSONObject.quote(navigationId)}).click(); true")
                    driver.waitFor(
                        "document.querySelector('.hybrid-header h1').textContent === ${JSONObject.quote(title)} && document.querySelector('[data-app-area-active=\"true\"]') !== null",
                        20_000L,
                    )
                    driver.evaluate("window.scrollTo(0, 0); true")
                    SystemClock.sleep(550)
                    assertSharedChromePainted(driver)
                }
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backupTabChangesPaintTheirSharedChrome() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            driver.evaluate("document.getElementById('navigation-more-button').click(); true")
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
            driver.evaluate("document.getElementById('backup-nav').click(); true")
            driver.waitFor("document.getElementById('encrypted-backup-form') !== null", 20_000L)

            val tabs = listOf(
                "backup-create-tab" to "encrypted-backup-form",
                "backup-cloud-tab" to "connect-cloud-backup-form",
                "backup-restore-tab" to "inspect-backup-form",
            )
            repeat(3) {
                tabs.forEach { (tabId, readyId) ->
                    driver.evaluate("document.getElementById(${JSONObject.quote(tabId)}).click(); true")
                    driver.waitFor("document.getElementById(${JSONObject.quote(readyId)}) !== null", 20_000L)
                    driver.evaluate("window.scrollTo(0, 0); true")
                    SystemClock.sleep(550)
                    assertSharedChromePainted(driver)
                }
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun sustainedDestinationChangesKeepTheFinalWindowPainted() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            val destinations = listOf(
                "customers-nav" to "Customers",
                "costing-planning-nav" to "Cost & plans",
                "backup-nav" to "Backup & restore",
            )
            repeat(20) {
                destinations.forEach { (navigationId, title) ->
                    driver.evaluate("document.getElementById(${JSONObject.quote(navigationId)}).click(); true")
                    driver.waitFor("document.querySelector('.hybrid-header h1').textContent === ${JSONObject.quote(title)}", 20_000L)
                }
            }
            driver.evaluate("window.scrollTo(0, 0); true")
            SystemClock.sleep(550)
            assertSharedChromePainted(driver)
        } finally {
            scenario.close()
        }
    }

    private fun assertSharedChromePainted(driver: HybridWebViewTestDriver) {
        awaitVisualState(driver.webView)
        val screenshot = driver.captureWindow()
        try {
            assertPainted(driver, screenshot, ".hybrid-header p:first-of-type", ::isLightInk)
            assertPainted(driver, screenshot, ".hybrid-header h1", ::isLightInk)
            assertPainted(driver, screenshot, "#signed-in-user", ::isLightInk)
            assertPainted(driver, screenshot, "#logout-button svg", ::isLightInk)
            assertPainted(driver, screenshot, "#navigation-more-button svg", ::isBrandInk)
            assertPainted(driver, screenshot, "#navigation-more-button span", ::isBrandInk)
        } finally {
            screenshot.recycle()
        }
    }

    private fun awaitVisualState(webView: WebView) {
        val ready = CountDownLatch(1)
        instrumentation.runOnMainSync {
            webView.postVisualStateCallback(
                SystemClock.uptimeMillis(),
                object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        webView.postInvalidateOnAnimation()
                        ready.countDown()
                    }
                },
            )
        }
        assertTrue("WebView visual state did not become ready", ready.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        SystemClock.sleep(350)
    }

    private fun wakeDevice() {
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(300)
    }

    private fun assertPainted(
        driver: HybridWebViewTestDriver,
        screenshot: Bitmap,
        selector: String,
        pixelMatches: (Int) -> Boolean,
    ) {
        val bounds = JSONObject(
            requireNotNull(
                driver.evaluate(
                    """
                        (() => {
                          const element = document.querySelector(${JSONObject.quote(selector)});
                          if (!element) return null;
                          const bounds = element.getBoundingClientRect();
                          return { left: bounds.left, top: bounds.top, right: bounds.right, bottom: bounds.bottom, viewportWidth: document.documentElement.clientWidth };
                        })()
                    """.trimIndent(),
                ),
            ),
        )
        val location = IntArray(2)
        instrumentation.runOnMainSync { driver.webView.getLocationOnScreen(location) }
        val scale = driver.webView.width.toDouble() / bounds.getDouble("viewportWidth")
        val left = (location[0] + bounds.getDouble("left") * scale).toInt().coerceIn(0, screenshot.width - 1)
        val top = (location[1] + bounds.getDouble("top") * scale).toInt().coerceIn(0, screenshot.height - 1)
        val right = (location[0] + bounds.getDouble("right") * scale).toInt().coerceIn(left + 1, screenshot.width)
        val bottom = (location[1] + bounds.getDouble("bottom") * scale).toInt().coerceIn(top + 1, screenshot.height)

        var matchingPixels = 0
        for (y in top until bottom) {
            for (x in left until right) {
                if (pixelMatches(screenshot.getPixel(x, y))) matchingPixels += 1
            }
        }
        assertTrue("Expected $selector to be painted, but found only $matchingPixels foreground pixels", matchingPixels >= 12)
    }

    private fun isLightInk(pixel: Int): Boolean =
        Color.red(pixel) >= 120 && Color.green(pixel) >= 150 && Color.blue(pixel) >= 130

    private fun isBrandInk(pixel: Int): Boolean =
        Color.red(pixel) < 80 && Color.green(pixel) in 55..155 && Color.blue(pixel) < 135
}
