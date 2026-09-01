package com.saaf.milkshop

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase9LifecycleRecreationTest {
    @Before
    fun wakeDevice() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        automation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
    }

    @Test
    fun validMemoryOnlySessionAndAuthorizedAreaSurviveActivityRecreation() {
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val original = HybridWebViewTestDriver.attach(scenario)
            original.ensureSignedIn()
            original.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            original.evaluate("document.getElementById('settings-nav').click(); true")
            original.waitFor("document.querySelector('header h1').innerText === 'Settings'")
            SystemClock.sleep(150)
            scenario.onActivity { assertEquals("settings", it.activeAreaForTesting()) }

            scenario.recreate()

            val restored = HybridWebViewTestDriver.attach(scenario)
            restored.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            restored.waitFor("document.querySelector('header h1').innerText === 'Settings'")
            assertEquals("false", restored.evaluate("document.getElementById('login-button') !== null"))
            assertEquals(HybridActivity.HYBRID_ENTRY_URL, restored.evaluate("location.href")?.trim('"'))
            assertNotSame(original.webView, restored.webView)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun invalidatedSessionCannotRestoreProtectedContentAfterRecreation() {
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val original = HybridWebViewTestDriver.attach(scenario)
            original.ensureSignedIn()
            original.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            scenario.onActivity {
                (it.application as MilkShopApplication).nativeSessions.invalidateAll()
            }

            scenario.recreate()

            val restored = HybridWebViewTestDriver.attach(scenario)
            restored.waitFor("document.getElementById('login-button') !== null", 20_000L)
            assertEquals("false", restored.evaluate("document.getElementById('app-navigation') !== null"))
            assertNotSame(original.webView, restored.webView)
            scenario.onActivity {
                assertNull((it.application as MilkShopApplication).currentSessionId)
            }
        } finally {
            scenario.close()
        }
    }
}
