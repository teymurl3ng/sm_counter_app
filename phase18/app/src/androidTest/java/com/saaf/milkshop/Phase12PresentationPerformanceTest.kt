package com.saaf.milkshop

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.hybrid.HybridActivity
import kotlin.math.ceil
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase12PresentationPerformanceTest {
    @Test
    fun packagedDocumentFirstContentfulPaintAndReactReadyStayWithinBudgets() {
        wakeAndUnlock()
        val firstContentfulPaintMs = mutableListOf<Double>()
        val reactReadyMs = mutableListOf<Double>()
        val hostObservedReadyMs = mutableListOf<Double>()

        repeat(STARTUP_SAMPLES) {
            val startedAt = SystemClock.elapsedRealtimeNanos()
            ActivityScenario.launch(HybridActivity::class.java).use { scenario ->
                val driver = HybridWebViewTestDriver.attach(scenario)
                driver.waitFor(
                    "performance.getEntriesByName('first-contentful-paint').length > 0 && " +
                        "document.documentElement.dataset.saafReactReady !== undefined",
                    10_000L,
                )
                firstContentfulPaintMs += requireMetric(
                    driver.evaluate("performance.getEntriesByName('first-contentful-paint')[0].startTime"),
                    "first-contentful-paint",
                )
                reactReadyMs += requireMetric(
                    driver.evaluate("performance.getEntriesByName('saaf-react-ready')[0].startTime"),
                    "saaf-react-ready",
                )
                hostObservedReadyMs += (SystemClock.elapsedRealtimeNanos() - startedAt) / 1_000_000.0
            }
            SystemClock.sleep(150)
        }

        val fcpP95 = percentile(firstContentfulPaintMs, 0.95)
        val readyP95 = percentile(reactReadyMs, 0.95)
        Log.i(
            TAG,
            "startup build=${BuildConfig.BUILD_TYPE} samples=$STARTUP_SAMPLES " +
                "fcp_ms=${summary(firstContentfulPaintMs)} react_ready_ms=${summary(reactReadyMs)} " +
                "host_observed_ready_ms=${summary(hostObservedReadyMs)} " +
                "fcp_values_ms=${series(firstContentfulPaintMs)} react_ready_values_ms=${series(reactReadyMs)} " +
                "host_observed_ready_values_ms=${series(hostObservedReadyMs)}",
        )

        assertTrue("React-ready must follow first contentful paint", reactReadyMs.zip(firstContentfulPaintMs).all { (ready, fcp) -> ready >= fcp })
        assertTrue("First-contentful-paint p95 ${format(fcpP95)} ms exceeds $FCP_P95_BUDGET_MS ms", fcpP95 <= FCP_P95_BUDGET_MS)
        assertTrue("React-ready p95 ${format(readyP95)} ms exceeds $REACT_READY_P95_BUDGET_MS ms", readyP95 <= REACT_READY_P95_BUDGET_MS)
    }

    @Test
    fun representativePaintedNavigationStaysResponsiveWithoutBridgeQueries() {
        wakeAndUnlock()
        ActivityScenario.launch(HybridActivity::class.java).use { scenario ->
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.ensureSignedIn()
            driver.waitFor("document.getElementById('app-navigation') !== null", 20_000L)

            repeat(NAVIGATION_WARMUPS) { index ->
                val destination = DESTINATIONS[index % DESTINATIONS.size]
                measurePaintedNavigation(driver, destination)
            }
            driver.evaluate("window.__saafMilkBridgeMetrics.reset(); true")

            val samples = MutableList(NAVIGATION_SAMPLES) { index ->
                measurePaintedNavigation(driver, DESTINATIONS[index % DESTINATIONS.size])
            }
            val bridgeCalls = driver.evaluate("window.__saafMilkBridgeMetrics.snapshot().totalCalls")?.toIntOrNull()
            val median = percentile(samples, 0.50)
            val p95 = percentile(samples, 0.95)
            val maximum = samples.max()
            Log.i(
                TAG,
                "navigation build=${BuildConfig.BUILD_TYPE} samples=$NAVIGATION_SAMPLES " +
                    "painted_ms=${summary(samples)} bridge_calls=$bridgeCalls values_ms=${series(samples)}",
            )

            assertEquals("Preloaded top-level navigation must not issue native bridge calls", 0, bridgeCalls)
            assertTrue("Painted navigation median ${format(median)} ms exceeds $NAVIGATION_MEDIAN_BUDGET_MS ms", median <= NAVIGATION_MEDIAN_BUDGET_MS)
            assertTrue("Painted navigation p95 ${format(p95)} ms exceeds $NAVIGATION_P95_BUDGET_MS ms", p95 <= NAVIGATION_P95_BUDGET_MS)
            assertTrue("Painted navigation max ${format(maximum)} ms exceeds $NAVIGATION_MAX_BUDGET_MS ms", maximum <= NAVIGATION_MAX_BUDGET_MS)
        }
    }

    private fun measurePaintedNavigation(
        driver: HybridWebViewTestDriver,
        destination: Destination,
    ): Double {
        val token = "${destination.area}-${SystemClock.elapsedRealtimeNanos()}"
        val quotedToken = JSONObject.quote(token)
        val quotedButtonId = JSONObject.quote(destination.buttonId)
        val quotedArea = JSONObject.quote(destination.area)
        driver.evaluate(
            """
                (() => {
                  const token = $quotedToken;
                  const targetArea = $quotedArea;
                  const button = document.getElementById($quotedButtonId);
                  if (!button) return false;
                  const startedAt = performance.now();
                  delete document.documentElement.dataset.saafNavigationSample;
                  button.click();
                  const waitForPaint = () => window.requestAnimationFrame(() => {
                    const active = document.querySelector('[data-app-area-active="true"]');
                    if (active?.dataset.appArea === targetArea) {
                      window.requestAnimationFrame(() => {
                        document.documentElement.dataset.saafNavigationSample = JSON.stringify({
                          token,
                          durationMs: performance.now() - startedAt,
                        });
                      });
                    } else {
                      waitForPaint();
                    }
                  });
                  waitForPaint();
                  return true;
                })()
            """.trimIndent(),
        )
        driver.waitFor(
            "document.documentElement.dataset.saafNavigationSample !== undefined && " +
                "JSON.parse(document.documentElement.dataset.saafNavigationSample).token === $quotedToken",
            5_000L,
        )
        return requireMetric(
            driver.evaluate("JSON.parse(document.documentElement.dataset.saafNavigationSample).durationMs"),
            "painted navigation to ${destination.area}",
        )
    }

    private fun wakeAndUnlock() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        automation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
    }

    private fun requireMetric(raw: String?, label: String): Double {
        val value = raw?.toDoubleOrNull()
        assertTrue("$label was not a finite non-negative number: $raw", value != null && value.isFinite() && value >= 0)
        return requireNotNull(value)
    }

    private fun percentile(values: List<Double>, percentile: Double): Double {
        require(values.isNotEmpty())
        val sorted = values.sorted()
        val index = (ceil(sorted.size * percentile).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index]
    }

    private fun summary(values: List<Double>): String =
        "min=${format(values.min())},median=${format(percentile(values, 0.50))},p95=${format(percentile(values, 0.95))},max=${format(values.max())}"

    private fun series(values: List<Double>): String = values.joinToString(",", transform = ::format)

    private fun format(value: Double): String = "%.1f".format(value)

    private data class Destination(
        val area: String,
        val buttonId: String,
    )

    private companion object {
        const val TAG = "SaafPhase12Performance"
        const val STARTUP_SAMPLES = 7
        const val NAVIGATION_WARMUPS = 6
        const val NAVIGATION_SAMPLES = 30
        const val FCP_P95_BUDGET_MS = 1_500.0
        const val REACT_READY_P95_BUDGET_MS = 2_000.0
        const val NAVIGATION_MEDIAN_BUDGET_MS = 100.0
        const val NAVIGATION_P95_BUDGET_MS = 180.0
        const val NAVIGATION_MAX_BUDGET_MS = 250.0

        val DESTINATIONS = listOf(
            Destination("operations", "operations-nav"),
            Destination("deliveries", "deliveries-nav"),
            Destination("accounts", "accounts-nav"),
        )
    }
}
