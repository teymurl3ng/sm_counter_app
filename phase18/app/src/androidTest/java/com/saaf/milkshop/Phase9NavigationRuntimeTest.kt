package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase9NavigationRuntimeTest {
    @Test
    fun packagedNavigationIsGroupedAndChangesAreasWithoutReloadingTheWebView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            ensureSignedIn(webView)
            waitFor(webView, "document.getElementById('app-navigation') !== null", 20_000L)
            val originalUrl = evaluate(webView, "location.href")

            assertEquals("4", evaluate(webView, "document.querySelectorAll('#primary-navigation > button:not(#navigation-more-button)').length"))
            assertEquals("true", evaluate(webView, "document.getElementById('navigation-more-button') !== null"))
            assertEquals("true", evaluate(webView, "document.documentElement.scrollWidth <= document.documentElement.clientWidth"))

            evaluate(webView, "document.getElementById('navigation-more-button').click(); true")
            waitFor(webView, "document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
            assertEquals("true", evaluate(webView, "document.getElementById('navigation-more-panel').textContent.includes('Records & closing') && document.getElementById('navigation-more-panel').textContent.includes('People & stock') && document.getElementById('navigation-more-panel').textContent.includes('Shop controls')"))

            evaluate(webView, "document.getElementById('products-nav').click(); true")
            waitFor(webView, "document.querySelector('header h1').innerText === 'Products' && document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'true'")
            assertEquals(originalUrl, evaluate(webView, "location.href"))
            assertEquals("1", evaluate(webView, "performance.getEntriesByType('navigation').length"))
            assertEquals("true", evaluate(webView, "document.getElementById('navigation-more-button').className.includes('text-emerald-800')"))

            val currentWebView = activityWebView(scenario)
            assertSame(webView, currentWebView)
        } finally {
            scenario.close()
        }
    }

    private fun ensureSignedIn(webView: WebView) {
        waitFor(webView, "document.getElementById('login-button') !== null || document.getElementById('app-navigation') !== null", 20_000L)
        if (evaluate(webView, "document.getElementById('login-button') !== null") == "true") {
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
        }
    }

    private fun activityWebView(scenario: ActivityScenario<HybridActivity>): WebView {
        val holder = AtomicReference<WebView>()
        scenario.onActivity { holder.set(it.webViewForTesting()) }
        return holder.get()
    }

    private fun setInput(webView: WebView, id: String, value: String) {
        evaluate(webView, """(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('input', { bubbles: true })); return input.value; })()""")
    }

    private fun waitFor(webView: WebView, expression: String, timeoutMs: Long = 10_000L) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (evaluate(webView, "Boolean($expression)") == "true") return
            SystemClock.sleep(100)
        }
        throw AssertionError("Web condition did not become true: $expression")
    }

    private fun evaluate(webView: WebView, script: String): String? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<String?>()
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { webView.evaluateJavascript(script) { result.set(it); latch.countDown() } }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }
}
