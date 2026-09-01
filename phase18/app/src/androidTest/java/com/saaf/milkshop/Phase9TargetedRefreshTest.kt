package com.saaf.milkshop

import android.os.SystemClock
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
class Phase9TargetedRefreshTest {
    @Test
    fun navigationTransfersNothingAndProductSaveRefreshesOnlyProductData() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null && window.__saafMilkBridgeMetrics !== undefined", 20_000L)
            val initialTransferChars = metric(driver, "totalResponseChars")

            resetMetrics(driver)
            driver.evaluate("document.getElementById('navigation-more-button').click(); true")
            driver.waitFor("document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
            driver.evaluate("document.getElementById('products-nav').click(); true")
            driver.waitFor("document.getElementById('add-product-button') !== null")
            assertEquals(0, metric(driver, "totalCalls"))

            driver.evaluate("document.getElementById('add-product-button').click(); true")
            driver.waitFor("document.getElementById('product-name') !== null")
            val productName = "Refresh QA ${System.currentTimeMillis()}"
            driver.setInput("product-name", productName)
            driver.evaluate("document.getElementById('save-product-button').click(); true")
            driver.waitFor("document.getElementById('product-name') === null && document.body.innerText.includes('Product added successfully.') && document.getElementById('native-product-list').innerText.includes(${JSONObject.quote(productName)})", 20_000L)

            assertEquals(3, metric(driver, "totalCalls"))
            assertEquals(2, metric(driver, "totalQueryCalls"))
            assertEquals(1, actionCalls(driver, "command.createProduct"))
            assertEquals(1, actionCalls(driver, "query.masterData"))
            assertEquals(1, actionCalls(driver, "query.transactionReferences"))
            assertEquals(0, actionCalls(driver, "query.dashboard"))
            assertEquals(0, actionCalls(driver, "query.cashierSummary"))
            assertEquals(0, actionCalls(driver, "query.financialOverview"))
            assertEquals(0, actionCalls(driver, "query.deliveries"))
            assertEquals(0, actionCalls(driver, "query.eventHistory"))
            assertEquals(0, actionCalls(driver, "query.reconciliationArchive"))
            assertTrue(metric(driver, "totalResponseChars") in 1 until initialTransferChars)
        } finally {
            scenario.close()
        }
    }

    private fun resetMetrics(driver: HybridWebViewTestDriver) {
        driver.evaluate("window.__saafMilkBridgeMetrics.reset(); true")
    }

    private fun metric(driver: HybridWebViewTestDriver, field: String): Int =
        requireNotNull(driver.evaluate("window.__saafMilkBridgeMetrics.snapshot()[${JSONObject.quote(field)}]" )).toInt()

    private fun actionCalls(driver: HybridWebViewTestDriver, action: String): Int =
        requireNotNull(driver.evaluate("window.__saafMilkBridgeMetrics.snapshot().actions[${JSONObject.quote(action)}]?.calls ?? 0")).toInt()
}
