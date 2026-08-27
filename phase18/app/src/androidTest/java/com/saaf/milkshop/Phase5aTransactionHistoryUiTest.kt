package com.saaf.milkshop

import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class Phase5aTransactionHistoryUiTest {

    @Test
    fun transactionHistory_loadsPersistedEvents_inReverseChronologicalOrder() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val vendorName = "Vendor: History UI ${System.nanoTime()}"
            val vendorId = database.accountDao().insert(
                AccountEntity(
                    name = vendorName,
                    type = "Liability",
                    category = "Vendor",
                )
            )

            val purchase = VendorPurchaseService(database).record(
                VendorPurchaseInput(
                    vendorAccountId = vendorId,
                    occurredAt = LocalDate.now().toString(),
                    rawLiters = 100.0,
                    fatPercentage = 4.0,
                    lrReading = 28.0,
                )
            )

            val production = YogurtProductionService(database).record(
                YogurtProductionInput(
                    occurredAt = LocalDate.now().toString(),
                    milkLitersUsed = 10.0,
                    yogurtKgProduced = 8.0,
                )
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val history = activity.findViewById<TextView>(R.id.transactionHistoryText)
                        .text
                        .toString()
                    history.contains("Event #${production.eventId} · Yogurt Production") &&
                        history.contains("Event #${purchase.eventId} · Milk Purchase")
                }

                scenario.onActivity { activity ->
                    val history = activity.findViewById<TextView>(R.id.transactionHistoryText)
                        .text
                        .toString()
                    val productionPosition = history.indexOf("Event #${production.eventId} · Yogurt Production")
                    val purchasePosition = history.indexOf("Event #${purchase.eventId} · Milk Purchase")
                    assertTrue(productionPosition >= 0)
                    assertTrue(purchasePosition >= 0)
                    assertTrue(productionPosition < purchasePosition)
                    assertTrue(history.contains("10.00 L milk used · 8.00 kg yogurt"))
                    assertTrue(history.contains("100.00 L received"))
                }
            } finally {
                scenario.close()
            }
        }
    }

    private fun waitForCondition(
        scenario: ActivityScenario<MainActivity>,
        timeoutMillis: Long = 5000L,
        condition: (MainActivity) -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var satisfied = false
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity { activity ->
                if (condition(activity)) satisfied = true
            }
            if (satisfied) return
            Thread.sleep(50L)
        }
        throw AssertionError("UI condition was not met within ${timeoutMillis}ms")
    }
}
