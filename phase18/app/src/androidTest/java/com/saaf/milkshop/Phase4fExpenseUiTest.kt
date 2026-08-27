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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase4fExpenseUiTest {

    @Test
    fun expenseScreen_success_updatesCashAndCreatesEvent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val cashId = database.accountDao().findByCategory("Cash/Bank")
                .single { it.name == "Cash" }.accountId
            val beforeCash = database.readModelDao().accountBalances()
                .single { it.accountId == cashId }.balance
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val categorySpinner = activity.findViewById<Spinner>(R.id.expenseCategorySpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.expenseAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveExpenseButton)
                    spinnerPosition(categorySpinner, "Rent") >= 0 &&
                        spinnerPosition(accountSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val categorySpinner = activity.findViewById<Spinner>(R.id.expenseCategorySpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.expenseAccountSpinner)
                    categorySpinner.setSelection(spinnerPosition(categorySpinner, "Rent"))
                    accountSpinner.setSelection(spinnerPosition(accountSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.expenseAmountInput).setText("100")
                    assertTrue(activity.findViewById<Button>(R.id.saveExpenseButton).performClick())
                }

                waitForCondition(scenario) { activity ->
                    val status = activity.findViewById<TextView>(R.id.expenseStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.expenseErrorText).text.toString()
                    status.startsWith("Expense saved") || error.isNotBlank()
                }

                scenario.onActivity { activity ->
                    val status = activity.findViewById<TextView>(R.id.expenseStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.expenseErrorText).text.toString()
                    assertTrue("Expense save failed: $error", status.startsWith("Expense saved"))
                }

                assertEquals(beforeEvents + 1, database.eventDao().count())
                assertEquals(beforeCash - 100.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun expenseScreen_invalidAmount_showsError_andDoesNotCreateEvent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val categorySpinner = activity.findViewById<Spinner>(R.id.expenseCategorySpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.expenseAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveExpenseButton)
                    spinnerPosition(categorySpinner, "Rent") >= 0 &&
                        spinnerPosition(accountSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val categorySpinner = activity.findViewById<Spinner>(R.id.expenseCategorySpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.expenseAccountSpinner)
                    categorySpinner.setSelection(spinnerPosition(categorySpinner, "Rent"))
                    accountSpinner.setSelection(spinnerPosition(accountSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.expenseAmountInput).setText("0")
                    assertTrue(activity.findViewById<Button>(R.id.saveExpenseButton).performClick())
                    val error = activity.findViewById<TextView>(R.id.expenseErrorText)
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
