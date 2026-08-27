package com.saaf.milkshop

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class Phase4bProductionUiTest {
    @Test
    fun productionScreen_saveSuccess_updatesStockAndDashboard() {
        runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database

        val vendorName = "Vendor: Production UI Test ${System.nanoTime()}"
        val vendorId = database.accountDao().insert(
            AccountEntity(
                name = vendorName,
                type = "Liability",
                category = "Vendor",
            )
        )

        VendorPurchaseService(database).record(
            VendorPurchaseInput(
                vendorAccountId = vendorId,
                occurredAt = LocalDate.now().toString(),
                rawLiters = 100.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
            )
        )

        val beforeMilk = database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand
        val beforeYogurt = database.readModelDao().currentStock().single { it.name == "Yogurt" }.quantityOnHand
        val beforeEvents = database.eventDao().count()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.milkUsedInput).setText("10")
                activity.findViewById<EditText>(R.id.yogurtProducedInput).setText("8")
                activity.findViewById<Button>(R.id.saveProductionButton).performClick()
            }

            waitForCondition(scenario) { activity ->
                val status = activity.findViewById<TextView>(R.id.productionStatusText).text.toString()
                val error = activity.findViewById<TextView>(R.id.productionErrorText).text.toString()
                status.startsWith("Production saved") || error.isNotBlank()
            }

            scenario.onActivity { activity ->
                val status = activity.findViewById<TextView>(R.id.productionStatusText).text.toString()
                val error = activity.findViewById<TextView>(R.id.productionErrorText).text.toString()
                assertTrue(
                    "Production save failed: $error",
                    status.startsWith("Production saved"),
                )
            }

            assertEquals(beforeEvents + 1, database.eventDao().count())
            assertEquals(
                beforeMilk - 10.0,
                database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
                1e-9,
            )
            assertEquals(
                beforeYogurt + 8.0,
                database.readModelDao().currentStock().single { it.name == "Yogurt" }.quantityOnHand,
                1e-9,
            )

            scenario.onActivity { activity ->
                val milkText = activity.findViewById<TextView>(R.id.milkStockText).text.toString()
                val yogurtText = activity.findViewById<TextView>(R.id.yogurtStockText).text.toString()
                assertTrue(milkText.startsWith(String.format("%.2f L", beforeMilk - 10.0)))
                assertTrue(yogurtText.startsWith(String.format("%.2f kg", beforeYogurt + 8.0)))
            }
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun productionScreen_insufficientMilk_showsError_andDoesNotCreateEvent() {
        runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database
        val beforeEvents = database.eventDao().count()
        val beforeMilk = database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.milkUsedInput).setText((beforeMilk + 1.0).toString())
                activity.findViewById<EditText>(R.id.yogurtProducedInput).setText("8")
                activity.findViewById<Button>(R.id.saveProductionButton).performClick()
            }

            waitForCondition(scenario) { activity ->
                val error = activity.findViewById<TextView>(R.id.productionErrorText)
                error.visibility == View.VISIBLE && error.text.isNotBlank()
            }

            scenario.onActivity { activity ->
                val error = activity.findViewById<TextView>(R.id.productionErrorText).text.toString()
                assertTrue(error.contains("Insufficient stock"))
            }

            assertEquals(beforeEvents, database.eventDao().count())
            assertEquals(
                beforeMilk,
                database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
                1e-9,
            )
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
