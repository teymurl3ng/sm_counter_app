package com.saaf.milkshop

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject
import org.junit.Assert.assertTrue

internal class HybridWebViewTestDriver private constructor(
    val webView: WebView,
) {
    fun ensureSignedIn(timeoutMs: Long = 20_000L) {
        waitFor("document.getElementById('login-button') !== null || document.getElementById('app-navigation') !== null", timeoutMs)
        if (evaluate("document.getElementById('login-button') !== null") == "true") {
            setInput("login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput("login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate("document.getElementById('login-button').click(); true")
        }
    }

    fun setInput(id: String, value: String) {
        evaluate("""(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('input', { bubbles: true })); return input.value; })()""")
    }

    fun waitFor(expression: String, timeoutMs: Long = 10_000L) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (evaluate("Boolean($expression)") == "true") return
            SystemClock.sleep(100)
        }
        throw AssertionError("Web condition did not become true: $expression")
    }

    fun evaluate(script: String): String? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<String?>()
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { webView.evaluateJavascript(script) { result.set(it); latch.countDown() } }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    companion object {
        fun attach(scenario: ActivityScenario<HybridActivity>): HybridWebViewTestDriver {
            val holder = AtomicReference<WebView>()
            scenario.onActivity { holder.set(it.webViewForTesting()) }
            return HybridWebViewTestDriver(holder.get())
        }
    }
}
