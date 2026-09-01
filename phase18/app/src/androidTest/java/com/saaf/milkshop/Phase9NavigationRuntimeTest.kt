package com.saaf.milkshop

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
            val driver = HybridWebViewTestDriver.attach(scenario)
            val webView = driver.webView
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            val originalUrl = driver.evaluate("location.href")

            assertEquals("4", driver.evaluate("document.querySelectorAll('#primary-navigation > button:not(#navigation-more-button)').length"))
            assertEquals("true", driver.evaluate("document.getElementById('navigation-more-button') !== null"))
            assertEquals("true", driver.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth"))

            driver.evaluate("document.getElementById('navigation-more-button').click(); true")
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
            assertEquals("true", driver.evaluate("document.getElementById('navigation-more-panel').textContent.includes('Records & closing') && document.getElementById('navigation-more-panel').textContent.includes('People & stock') && document.getElementById('navigation-more-panel').textContent.includes('Shop controls')"))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'true'")
            driver.evaluate("document.getElementById('navigation-more-button').click(); true")
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")

            driver.evaluate("document.getElementById('products-nav').click(); true")
            driver.waitFor("document.querySelector('header h1').innerText === 'Products' && document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'true'")
            assertEquals(originalUrl, driver.evaluate("location.href"))
            assertEquals("1", driver.evaluate("performance.getEntriesByType('navigation').length"))
            assertEquals("true", driver.evaluate("document.getElementById('navigation-more-button').className.includes('text-emerald-800')"))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            driver.waitFor("document.querySelector('header h1').innerText === 'Dashboard'")
            assertEquals(originalUrl, driver.evaluate("location.href"))

            val currentWebView = HybridWebViewTestDriver.attach(scenario).webView
            assertSame(webView, currentWebView)
        } finally {
            scenario.close()
        }
    }

}
