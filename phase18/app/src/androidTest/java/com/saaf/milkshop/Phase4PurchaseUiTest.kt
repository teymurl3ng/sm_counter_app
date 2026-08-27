package com.saaf.milkshop

import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.VendorPurchaseCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase4PurchaseUiTest {
    @Test
    fun purchaseScreen_saveSuccess_updatesDatabaseAndDashboardState() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database
        val vendorName = "Vendor: UI Test ${System.nanoTime()}"
        val vendorId = database.accountDao().insert(
            AccountEntity(
                name = vendorName,
                type = "Liability",
                category = "Vendor",
            )
        )

        val beforeStock = database.readModelDao().currentStock()
            .single { it.name == "Milk" }
            .quantityOnHand
        val beforeEventCount = database.eventDao().count()

        val settingsDao = database.settingsDao()
        val milkRate = settingsDao.getValue("milk_rate_vdr")!!
        val deliveryFee = settingsDao.getValue("delivery_fee_vdr")!!
        val tsStandard = settingsDao.getValue("ts_standard")!!
        val expectedAmount = VendorPurchaseCalculator.calculate(
            rawLiters = 100.0,
            fatPercentage = 4.0,
            lrReading = 28.0,
            milkRate = milkRate,
            deliveryFee = deliveryFee,
            tsStandard = tsStandard,
        ).amountOwed

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForCondition(scenario) { activity ->
                val spinner = activity.findViewById<Spinner>(R.id.vendorSpinner)
                spinnerPosition(spinner, vendorName) >= 0
            }

            scenario.onActivity { activity ->
                val spinner = activity.findViewById<Spinner>(R.id.vendorSpinner)
                spinner.setSelection(spinnerPosition(spinner, vendorName))
                activity.findViewById<EditText>(R.id.rawLitersInput).setText("100")
                activity.findViewById<EditText>(R.id.fatInput).setText("4.0")
                activity.findViewById<EditText>(R.id.lrInput).setText("28")
                activity.findViewById<android.widget.Button>(R.id.savePurchaseButton).performClick()
            }

            waitForCondition(scenario) { activity ->
                activity.findViewById<TextView>(R.id.purchaseStatusText)
                    .text
                    .toString()
                    .startsWith("Purchase saved")
            }

            assertEquals(beforeEventCount + 1, database.eventDao().count())
            assertEquals(
                beforeStock + 100.0,
                database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
                1e-9,
            )
            assertEquals(
                expectedAmount,
                database.readModelDao().accountBalances().single { it.accountId == vendorId }.balance,
                0.01,
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun purchaseScreen_invalidLr_showsUserError_andDoesNotCreateEvent() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database
        val vendorName = "Vendor: UI Error Test ${System.nanoTime()}"
        database.accountDao().insert(
            AccountEntity(
                name = vendorName,
                type = "Liability",
                category = "Vendor",
            )
        )
        val beforeEventCount = database.eventDao().count()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForCondition(scenario) { activity ->
                val spinner = activity.findViewById<Spinner>(R.id.vendorSpinner)
                spinnerPosition(spinner, vendorName) >= 0
            }

            scenario.onActivity { activity ->
                val spinner = activity.findViewById<Spinner>(R.id.vendorSpinner)
                spinner.setSelection(spinnerPosition(spinner, vendorName))
                activity.findViewById<EditText>(R.id.rawLitersInput).setText("100")
                activity.findViewById<EditText>(R.id.fatInput).setText("4.0")
                activity.findViewById<EditText>(R.id.lrInput).setText("-1")
                activity.findViewById<android.widget.Button>(R.id.savePurchaseButton).performClick()
            }

            waitForCondition(scenario) { activity ->
                val error = activity.findViewById<TextView>(R.id.purchaseErrorText)
                error.visibility == android.view.View.VISIBLE && error.text.isNotBlank()
            }

            scenario.onActivity { activity ->
                val error = activity.findViewById<TextView>(R.id.purchaseErrorText).text.toString()
                assertTrue(error.contains("LR"))
            }
            assertEquals(beforeEventCount, database.eventDao().count())
        } finally {
            scenario.close()
        }
    }

    private fun spinnerPosition(spinner: Spinner, value: String): Int {
        val adapter = spinner.adapter ?: return -1
        for (position in 0 until adapter.count) {
            if (adapter.getItem(position)?.toString() == value) return position
        }
        return -1
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
