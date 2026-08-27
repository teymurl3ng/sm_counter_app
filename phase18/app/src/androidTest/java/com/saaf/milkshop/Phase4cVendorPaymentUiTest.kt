package com.saaf.milkshop

import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase4cVendorPaymentUiTest {
    @Test
    fun vendorPaymentScreen_saveSuccess_updatesVendorAndCash() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val vendorName = "Vendor: Payment UI Test ${System.nanoTime()}"
            val vendorId = database.accountDao().insert(
                AccountEntity(
                    name = vendorName,
                    type = "Liability",
                    category = "Vendor",
                )
            )
            val cashId = database.accountDao().findByCategory("Cash/Bank")
                .single { it.name == "Cash" }
                .accountId

            VendorPurchaseService(database).record(
                VendorPurchaseInput(
                    vendorAccountId = vendorId,
                    occurredAt = "2026-08-19",
                    rawLiters = 100.0,
                    fatPercentage = 4.0,
                    lrReading = 28.0,
                )
            )

            val beforeVendor = database.readModelDao().accountBalances()
                .single { it.accountId == vendorId }.balance
            val beforeCash = database.readModelDao().accountBalances()
                .single { it.accountId == cashId }.balance
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val vendorSpinner = activity.findViewById<Spinner>(R.id.vendorPaymentSpinner)
                    val paymentSpinner = activity.findViewById<Spinner>(R.id.paymentAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveVendorPaymentButton)
                    spinnerPosition(vendorSpinner, vendorName) >= 0 &&
                        spinnerPosition(paymentSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val vendorSpinner = activity.findViewById<Spinner>(R.id.vendorPaymentSpinner)
                    val paymentSpinner = activity.findViewById<Spinner>(R.id.paymentAccountSpinner)
                    vendorSpinner.setSelection(spinnerPosition(vendorSpinner, vendorName))
                    paymentSpinner.setSelection(spinnerPosition(paymentSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.paymentAmountInput).setText("500")
                    activity.findViewById<Button>(R.id.saveVendorPaymentButton).performClick()
                }

                waitForCondition(scenario) { activity ->
                    val status = activity.findViewById<TextView>(R.id.vendorPaymentStatusText)
                        .text.toString()
                    val error = activity.findViewById<TextView>(R.id.vendorPaymentErrorText)
                        .text.toString()
                    status.startsWith("Vendor payment saved") || error.isNotBlank()
                }

                scenario.onActivity { activity ->
                    val status = activity.findViewById<TextView>(R.id.vendorPaymentStatusText)
                        .text.toString()
                    val error = activity.findViewById<TextView>(R.id.vendorPaymentErrorText)
                        .text.toString()
                    assertTrue(
                        "Vendor payment save failed: $error",
                        status.startsWith("Vendor payment saved"),
                    )
                }

                assertEquals(beforeEvents + 1, database.eventDao().count())
                assertEquals(beforeVendor - 500.0, database.readModelDao().accountBalances()
                    .single { it.accountId == vendorId }.balance, 1e-9)
                assertEquals(beforeCash - 500.0, database.readModelDao().accountBalances()
                    .single { it.accountId == cashId }.balance, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun vendorPaymentScreen_invalidAmount_showsError_andDoesNotCreateEvent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val vendorName = "Vendor: Payment Error Test ${System.nanoTime()}"
            database.accountDao().insert(
                AccountEntity(
                    name = vendorName,
                    type = "Liability",
                    category = "Vendor",
                )
            )
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val vendorSpinner = activity.findViewById<Spinner>(R.id.vendorPaymentSpinner)
                    val paymentSpinner = activity.findViewById<Spinner>(R.id.paymentAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveVendorPaymentButton)
                    spinnerPosition(vendorSpinner, vendorName) >= 0 &&
                        spinnerPosition(paymentSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val spinner = activity.findViewById<Spinner>(R.id.vendorPaymentSpinner)
                    spinner.setSelection(spinnerPosition(spinner, vendorName))
                    activity.findViewById<EditText>(R.id.paymentAmountInput).setText("0")
                    activity.findViewById<Button>(R.id.saveVendorPaymentButton).performClick()
                }

                scenario.onActivity { activity ->
                    val error = activity.findViewById<TextView>(R.id.vendorPaymentErrorText)
                    assertEquals(android.view.View.VISIBLE, error.visibility)
                    assertTrue(error.text.toString().contains("greater than zero"))
                }

                assertEquals(beforeEvents, database.eventDao().count())
            } finally {
                scenario.close()
            }
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
