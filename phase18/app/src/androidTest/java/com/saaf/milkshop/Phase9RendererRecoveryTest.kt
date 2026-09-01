package com.saaf.milkshop

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase9RendererRecoveryTest {
    @Before
    fun wakeDevice() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        automation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
    }

    @Test
    fun crashedRendererShowsNativeRecoveryAndRevalidatesTheLiveSession() {
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val original = HybridWebViewTestDriver.attach(scenario)
            original.ensureSignedIn()
            original.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            original.evaluate("document.getElementById('settings-nav').click(); true")
            original.waitFor("document.querySelector('header h1').innerText === 'Settings'")
            SystemClock.sleep(150)

            terminateRenderer(scenario)
            waitForRecovery(scenario)
            assertSafeRecoveryScreen(scenario)
            clickRecovery(scenario)
            waitForNewWebView(scenario, original.webView)

            val recovered = HybridWebViewTestDriver.attach(scenario)
            recovered.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            recovered.waitFor("document.querySelector('header h1').innerText === 'Settings'")
            assertEquals("false", recovered.evaluate("document.getElementById('login-button') !== null"))
            assertNotSame(original.webView, recovered.webView)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun expiredSessionCannotReturnToProtectedContentAfterRendererRecovery() {
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val original = HybridWebViewTestDriver.attach(scenario)
            original.ensureSignedIn()
            original.waitFor("document.getElementById('app-navigation') !== null", 20_000L)

            terminateRenderer(scenario)
            waitForRecovery(scenario)
            scenario.onActivity { (it.application as MilkShopApplication).nativeSessions.invalidateAll() }
            clickRecovery(scenario)
            waitForNewWebView(scenario, original.webView)

            val recovered = HybridWebViewTestDriver.attach(scenario)
            recovered.waitFor("document.getElementById('login-button') !== null", 20_000L)
            assertEquals("false", recovered.evaluate("document.getElementById('app-navigation') !== null"))
            scenario.onActivity { assertNull((it.application as MilkShopApplication).currentSessionId) }
        } finally {
            scenario.close()
        }
    }

    private fun terminateRenderer(scenario: ActivityScenario<HybridActivity>) {
        val terminated = AtomicBoolean(false)
        scenario.onActivity { activity ->
            terminated.set(activity.webViewForTesting().webViewRenderProcess?.terminate() == true)
        }
        assertTrue("The Samsung WebView renderer was not available to terminate", terminated.get())
    }

    private fun waitForRecovery(scenario: ActivityScenario<HybridActivity>) {
        waitForActivity(scenario) { activity ->
            activity.rendererRecoveryVisibleForTesting() && activity.findViewById<View>(R.id.hybrid_failure_root) != null
        }
    }

    private fun assertSafeRecoveryScreen(scenario: ActivityScenario<HybridActivity>) {
        scenario.onActivity { activity ->
            val title = activity.findViewById<TextView>(R.id.hybrid_failure_title).text.toString()
            val message = activity.findViewById<TextView>(R.id.hybrid_failure_message).text.toString()
            val session = (activity.application as MilkShopApplication).currentSessionId
            assertEquals("The app display needs to restart", title)
            assertTrue(message.contains("saved shop records were not erased", ignoreCase = true))
            assertFalse(session != null && message.contains(session))
            assertEquals(0, countWebViews(activity.findViewById(android.R.id.content)))
            assertTrue(activity.findViewById<Button>(R.id.hybrid_recovery_action).isEnabled)
        }
    }

    private fun clickRecovery(scenario: ActivityScenario<HybridActivity>) {
        scenario.onActivity { it.findViewById<Button>(R.id.hybrid_recovery_action).performClick() }
    }

    private fun waitForNewWebView(scenario: ActivityScenario<HybridActivity>, previous: WebView) {
        waitForActivity(scenario) { activity ->
            !activity.rendererRecoveryVisibleForTesting() && runCatching { activity.webViewForTesting() !== previous }.getOrDefault(false)
        }
    }

    private fun waitForActivity(scenario: ActivityScenario<HybridActivity>, condition: (HybridActivity) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 20_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val matched = AtomicBoolean(false)
            runCatching { scenario.onActivity { matched.set(condition(it)) } }
            if (matched.get()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Activity condition did not become true")
    }

    private fun countWebViews(view: View): Int {
        if (view is WebView) return 1
        if (view !is ViewGroup) return 0
        return (0 until view.childCount).sumOf { countWebViews(view.getChildAt(it)) }
    }
}
