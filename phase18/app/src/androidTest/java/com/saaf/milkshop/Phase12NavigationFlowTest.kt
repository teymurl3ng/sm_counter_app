package com.saaf.milkshop

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
class Phase12NavigationFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun ownerOpensOnDashboardAndSecondaryAreaKeepsItsIdentity() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)

            assertEquals("true", driver.evaluate("document.querySelector('.hybrid-header h1').textContent === 'Dashboard'"))
            assertEquals("true", driver.evaluate("document.getElementById('dashboard-nav').getAttribute('aria-current') === 'page'"))

            driver.evaluate("document.getElementById('navigation-more-button').click(); true")
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
            driver.evaluate("document.getElementById('products-nav').click(); true")
            driver.waitFor("document.querySelector('.hybrid-header h1').textContent === 'Products'", 20_000L)

            assertEquals("true", driver.evaluate("document.getElementById('navigation-more-button').textContent.trim() === 'Products'"))
            assertEquals("true", driver.evaluate("document.getElementById('navigation-more-button').dataset.activeSecondaryArea === 'products'"))
            assertEquals(
                "true",
                driver.evaluate("document.getElementById('navigation-more-button').getAttribute('aria-label').includes('Products, current area')"),
            )

            driver.evaluate("document.getElementById('navigation-more-button').click(); true")
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
            assertEquals("true", driver.evaluate("document.getElementById('products-nav').getAttribute('aria-current') === 'page'"))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'true'")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            driver.waitFor("document.querySelector('.hybrid-header h1').textContent === 'Dashboard'")
            assertEquals("true", driver.evaluate("document.getElementById('navigation-more-button').textContent.trim() === 'More'"))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun everyAuthorizedDailyTaskIsVisibleWithoutHorizontalScrolling() {
        wakeDevice()
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)
            driver.evaluate("document.getElementById('operations-nav').click(); true")
            driver.waitFor("document.getElementById('daily-task-picker') !== null", 20_000L)
            driver.evaluate("document.getElementById('daily-task-picker').click(); true")
            driver.waitFor("document.getElementById('daily-task-navigation-layer').getAttribute('aria-hidden') === 'false'")

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            driver.waitFor("document.getElementById('daily-task-navigation-layer').getAttribute('aria-hidden') === 'true'")
            assertEquals("true", driver.evaluate("document.getElementById('daily-task-picker').getAttribute('aria-expanded') === 'false'"))
            driver.evaluate("document.getElementById('daily-task-picker').click(); true")
            driver.waitFor("document.getElementById('daily-task-navigation-layer').getAttribute('aria-hidden') === 'false'")

            assertEquals("7", driver.evaluate("document.querySelectorAll('#daily-task-navigation > button').length"))
            assertEquals(
                "true",
                driver.evaluate(
                    """
                        (() => {
                          const navigation = document.getElementById('daily-task-navigation');
                          const bounds = navigation.getBoundingClientRect();
                          return navigation.scrollWidth <= navigation.clientWidth + 1 &&
                            Array.from(navigation.children).every((button) => {
                              const item = button.getBoundingClientRect();
                              return item.left >= bounds.left - 1 && item.right <= bounds.right + 1 && item.height >= 47;
                            });
                        })()
                    """.trimIndent(),
                ),
            )

            val tasks = listOf(
                "purchase-work-tab" to "Receive milk",
                "sale-work-tab" to "Record sale",
                "production-work-tab" to "Make yogurt",
                "spoilage-work-tab" to "Spoilage",
                "expense-work-tab" to "Expense",
                "payments-work-tab" to "Payments",
                "salary-work-tab" to "Pay salary",
            )
            tasks.forEach { (taskId, label) ->
                if (driver.evaluate("document.getElementById('daily-task-navigation-layer').getAttribute('aria-hidden') === 'true'") == "true") {
                    driver.evaluate("document.getElementById('daily-task-picker').click(); true")
                    driver.waitFor("document.getElementById('daily-task-navigation-layer').getAttribute('aria-hidden') === 'false'")
                }
                driver.evaluate("document.getElementById(${JSONObject.quote(taskId)}).click(); true")
                driver.waitFor("document.getElementById(${JSONObject.quote(taskId)}).getAttribute('aria-pressed') === 'true'")
                driver.waitFor("document.getElementById('daily-task-navigation-layer').getAttribute('aria-hidden') === 'true'")
                assertEquals("true", driver.evaluate("document.getElementById(${JSONObject.quote(taskId)}).textContent.trim() === ${JSONObject.quote(label)}"))
                assertEquals("true", driver.evaluate("document.getElementById(${JSONObject.quote(taskId)}).getAttribute('aria-pressed') === 'true'"))
                assertEquals("true", driver.evaluate("document.getElementById('daily-task-picker').textContent.includes(${JSONObject.quote(label)})"))
                assertEquals("true", driver.evaluate("document.getElementById('daily-task-panel').textContent.trim().length > 0"))
            }
        } finally {
            scenario.close()
        }
    }

    private fun wakeDevice() {
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(300)
    }
}
