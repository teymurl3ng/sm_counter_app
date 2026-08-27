package com.saaf.milkshop

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase4eCreditCollectionUiTest {

    @Test
    fun creditCollectionScreen_success_updatesCustomerAndCash() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val vendorId = database.accountDao().insert(
                AccountEntity(
                    name = "Vendor: Credit Collection Test ${System.nanoTime()}",
                    type = "Liability",
                    category = "Vendor",
                )
            )
            val customerName = "Customer: Credit Collection Test ${System.nanoTime()}"
            val customerId = database.accountDao().insert(
                AccountEntity(
                    name = customerName,
                    type = "Asset",
                    category = "Customer",
                )
            )
            val cashId = database.accountDao().findByCategory("Cash/Bank")
                .single { it.name == "Cash" }.accountId
            val milk = database.productDao().findByName("Milk")!!

            VendorPurchaseService(database).record(
                VendorPurchaseInput(
                    vendorAccountId = vendorId,
                    occurredAt = "2026-08-19",
                    rawLiters = 20.0,
                    fatPercentage = 4.0,
                    lrReading = 28.0,
                )
            )
            SaleService(database).record(
                SaleInput(
                    productId = milk.productId,
                    occurredAt = "2026-08-19",
                    channel = "credit-retail",
                    unitRate = 100.0,
                    quantity = 5.0,
                    payment = SalePayment.OnCredit(customerId),
                )
            )

            val beforeCustomer = database.readModelDao().accountBalances()
                .single { it.accountId == customerId }.balance
            val beforeCash = database.readModelDao().accountBalances()
                .single { it.accountId == cashId }.balance
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val customerSpinner = activity.findViewById<Spinner>(R.id.creditCollectionCustomerSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.creditCollectionAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveCreditCollectionButton)
                    spinnerPosition(customerSpinner, customerName) >= 0 &&
                        spinnerPosition(accountSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val customerSpinner = activity.findViewById<Spinner>(R.id.creditCollectionCustomerSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.creditCollectionAccountSpinner)
                    customerSpinner.setSelection(spinnerPosition(customerSpinner, customerName))
                    accountSpinner.setSelection(spinnerPosition(accountSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.creditCollectionAmountInput).setText("150")
                    assertTrue(activity.findViewById<Button>(R.id.saveCreditCollectionButton).performClick())
                }

                waitForCondition(scenario) { activity ->
                    val status = activity.findViewById<TextView>(R.id.creditCollectionStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.creditCollectionErrorText).text.toString()
                    status.startsWith("Credit collection saved") || error.isNotBlank()
                }

                scenario.onActivity { activity ->
                    val status = activity.findViewById<TextView>(R.id.creditCollectionStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.creditCollectionErrorText).text.toString()
                    assertTrue("Credit collection save failed: $error", status.startsWith("Credit collection saved"))
                }

                assertEquals(beforeEvents + 1, database.eventDao().count())
                assertEquals(beforeCustomer - 150.0, database.readModelDao().accountBalances().single { it.accountId == customerId }.balance, 1e-9)
                assertEquals(beforeCash + 150.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun creditCollectionScreen_invalidAmount_showsError_andDoesNotCreateEvent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val customerName = "Customer: Credit Collection Error Test ${System.nanoTime()}"
            database.accountDao().insert(
                AccountEntity(
                    name = customerName,
                    type = "Asset",
                    category = "Customer",
                )
            )
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val customerSpinner = activity.findViewById<Spinner>(R.id.creditCollectionCustomerSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.creditCollectionAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveCreditCollectionButton)
                    spinnerPosition(customerSpinner, customerName) >= 0 &&
                        spinnerPosition(accountSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val customerSpinner = activity.findViewById<Spinner>(R.id.creditCollectionCustomerSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.creditCollectionAccountSpinner)
                    customerSpinner.setSelection(spinnerPosition(customerSpinner, customerName))
                    accountSpinner.setSelection(spinnerPosition(accountSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.creditCollectionAmountInput).setText("0")
                    assertTrue(activity.findViewById<Button>(R.id.saveCreditCollectionButton).performClick())
                    val error = activity.findViewById<TextView>(R.id.creditCollectionErrorText)
                    assertEquals(View.VISIBLE, error.visibility)
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
