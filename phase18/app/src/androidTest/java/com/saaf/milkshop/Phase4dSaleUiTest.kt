package com.saaf.milkshop

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
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

@RunWith(AndroidJUnit4::class)
class Phase4dSaleUiTest {
    @Test
    fun saleScreen_quantityCashWithSurcharge_updatesStockAndCash() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val vendorId = database.accountDao().insert(
                AccountEntity(
                    name = "Vendor: Sale UI Test ${System.nanoTime()}",
                    type = "Liability",
                    category = "Vendor",
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

            val beforeStock = database.readModelDao().currentStock().single { it.productId == milk.productId }.quantityOnHand
            val beforeCash = database.readModelDao().accountBalances().single { it.accountId == cashId }.balance
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val productSpinner = activity.findViewById<Spinner>(R.id.saleProductSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.saleAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveSaleButton)
                    spinnerPosition(productSpinner, "Milk") >= 0 &&
                        spinnerPosition(accountSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    activity.findViewById<Spinner>(R.id.saleProductSpinner).setSelection(0)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.saleAccountSpinner)
                    accountSpinner.setSelection(spinnerPosition(accountSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.saleQuantityInput).setText("2")
                    activity.findViewById<EditText>(R.id.saleRateInput).setText("190")
                    activity.findViewById<CheckBox>(R.id.saleSurchargeCheckbox).performClick()
                    activity.findViewById<EditText>(R.id.saleSurchargeInput).setText("50")
                    activity.findViewById<Button>(R.id.saveSaleButton).performClick()
                }

                waitForCondition(scenario) { activity ->
                    val status = activity.findViewById<TextView>(R.id.saleStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.saleErrorText).text.toString()
                    status.startsWith("Sale saved") || error.isNotBlank()
                }

                scenario.onActivity { activity ->
                    val status = activity.findViewById<TextView>(R.id.saleStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.saleErrorText).text.toString()
                    assertTrue("Sale save failed: $error", status.startsWith("Sale saved"))
                }

                assertEquals(beforeEvents + 1, database.eventDao().count())
                assertEquals(beforeStock - 2.0, database.readModelDao().currentStock().single { it.productId == milk.productId }.quantityOnHand, 1e-9)
                assertEquals(beforeCash + 430.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun saleScreen_valueOnCredit_usesEditedRateAndCustomer() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val vendorId = database.accountDao().insert(
                AccountEntity(
                    name = "Vendor: Sale Credit Test ${System.nanoTime()}",
                    type = "Liability",
                    category = "Vendor",
                )
            )
            val customerName = "Customer: Sale Credit Test ${System.nanoTime()}"
            val customerId = database.accountDao().insert(
                AccountEntity(
                    name = customerName,
                    type = "Asset",
                    category = "Customer",
                )
            )
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
            val beforeCustomer = database.readModelDao().accountBalances().single { it.accountId == customerId }.balance
            val beforeStock = database.readModelDao().currentStock().single { it.productId == milk.productId }.quantityOnHand

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val productSpinner = activity.findViewById<Spinner>(R.id.saleProductSpinner)
                    val customerSpinner = activity.findViewById<Spinner>(R.id.saleCustomerSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveSaleButton)
                    spinnerPosition(productSpinner, "Milk") >= 0 &&
                        spinnerPosition(customerSpinner, customerName) >= 0
                    && saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    activity.findViewById<RadioButton>(R.id.saleValueRadioButton).performClick()
                    activity.findViewById<RadioButton>(R.id.saleCreditRadioButton).performClick()
                    val customerSpinner = activity.findViewById<Spinner>(R.id.saleCustomerSpinner)
                    customerSpinner.setSelection(spinnerPosition(customerSpinner, customerName))
                    activity.findViewById<EditText>(R.id.saleValueInput).setText("500")
                    activity.findViewById<EditText>(R.id.saleRateInput).setText("200")
                    activity.findViewById<Spinner>(R.id.saleChannelSpinner).setSelection(1)
                    activity.findViewById<CheckBox>(R.id.saleSurchargeCheckbox).performClick()
                    activity.findViewById<EditText>(R.id.saleSurchargeInput).setText("50")
                    activity.findViewById<Button>(R.id.saveSaleButton).performClick()
                }

                waitForCondition(scenario) { activity ->
                    val status = activity.findViewById<TextView>(R.id.saleStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.saleErrorText).text.toString()
                    status.startsWith("Sale saved") || error.isNotBlank()
                }

                scenario.onActivity { activity ->
                    val status = activity.findViewById<TextView>(R.id.saleStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.saleErrorText).text.toString()
                    assertTrue("Credit sale failed: $error", status.startsWith("Sale saved"))
                }

                assertEquals(beforeStock - 2.5, database.readModelDao().currentStock().single { it.productId == milk.productId }.quantityOnHand, 1e-9)
                assertEquals(beforeCustomer + 550.0, database.readModelDao().accountBalances().single { it.accountId == customerId }.balance, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun saleScreen_invalidQuantity_showsErrorAndDoesNotCreateEvent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            database.accountDao().insert(
                AccountEntity(
                    name = "Vendor: Sale Error Test ${System.nanoTime()}",
                    type = "Liability",
                    category = "Vendor",
                )
            )
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val productSpinner = activity.findViewById<Spinner>(R.id.saleProductSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveSaleButton)
                    spinnerPosition(productSpinner, "Milk") >= 0 && saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    activity.findViewById<EditText>(R.id.saleQuantityInput).setText("0")
                    activity.findViewById<Button>(R.id.saveSaleButton).performClick()
                    val error = activity.findViewById<TextView>(R.id.saleErrorText)
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
