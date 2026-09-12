package com.saaf.milkshop

import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase14DefectClosureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun wakeDevice() {
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(300)
    }

    @Test
    fun backConfirmsBeforeDiscardingAnUnfinishedForm() {
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            openSettings(driver)

            val draft = "phase14-unsaved-${SystemClock.uptimeMillis()}"
            driver.setInput("store-phone", draft)
            assertEquals("true", driver.evaluate("document.getElementById('store-phone').closest('form').dataset.saafDirty === 'true'"))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitForDialog(scenario)
            scenario.onActivity { activity -> assertTrue(activity.keepDraftForTesting()) }
            driver.waitFor("document.querySelector('.hybrid-header h1').textContent === 'Settings'")
            assertEquals(draft, driver.evaluate("document.getElementById('store-phone').value")?.trim('"'))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitForDialog(scenario)
            scenario.onActivity { activity -> assertTrue(activity.discardDraftForTesting()) }
            driver.waitFor("document.querySelector('.hybrid-header h1').textContent === 'Dashboard'")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun rotationKeepsTheSameWebViewAndUnfinishedFormValues() {
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            SystemClock.sleep(600)
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            openSettings(driver)

            val originalWebView: WebView = driver.webView
            val draft = "phase14-rotation-${SystemClock.uptimeMillis()}"
            driver.setInput("store-phone", draft)
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            driver.waitFor("window.innerWidth > window.innerHeight", 20_000L)

            val rotated = HybridWebViewTestDriver.attach(scenario)
            assertSame(originalWebView, rotated.webView)
            assertEquals("true", rotated.evaluate("document.querySelector('.hybrid-header h1').textContent === 'Settings'"))
            assertEquals(draft, rotated.evaluate("document.getElementById('store-phone').value")?.trim('"'))
        } finally {
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            SystemClock.sleep(500)
            scenario.close()
        }
    }

    @Test
    fun dialogCloseIsVisibleSemanticAndFullSized() {
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            driver.evaluate("document.getElementById('products-nav').click(); true")
            driver.waitFor("document.getElementById('add-product-button') !== null", 20_000L)
            driver.evaluate("document.getElementById('add-product-button').click(); true")
            driver.waitFor("document.querySelector('.hybrid-dialog-panel [data-native-back-close]') !== null")

            assertEquals(
                "true",
                driver.evaluate(
                    """
                        (() => {
                          const close = document.querySelector('.hybrid-dialog-panel [data-native-back-close]');
                          const bounds = close.getBoundingClientRect();
                          return close.tagName === 'BUTTON' &&
                            close.textContent.trim() === 'Close' &&
                            close.getAttribute('aria-label').startsWith('Close ') &&
                            bounds.width >= 48 && bounds.height >= 48;
                        })()
                    """.trimIndent(),
                ),
            )
            driver.evaluate("document.querySelector('.hybrid-dialog-panel [data-native-back-close]').click(); true")
            driver.waitFor("document.querySelector('.hybrid-dialog-panel') === null")
        } finally {
            scenario.close()
        }
    }

    private fun openSettings(driver: HybridWebViewTestDriver) {
        driver.evaluate("document.getElementById('settings-nav').click(); true")
        driver.waitFor(
            "document.querySelector('.hybrid-header h1').textContent === 'Settings' && document.getElementById('store-phone') !== null",
            20_000L,
        )
    }

    private fun waitForDialog(scenario: ActivityScenario<HybridActivity>) {
        val deadline = SystemClock.uptimeMillis() + 5_000L
        while (SystemClock.uptimeMillis() < deadline) {
            var visible = false
            scenario.onActivity { visible = it.discardDraftDialogVisibleForTesting() }
            if (visible) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Discard confirmation did not appear")
    }
}
