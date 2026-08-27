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
class Phase4gSalaryUiTest {

    @Test
    fun salaryScreen_success_updatesCashAndEmployeeBalance_andCreatesEvent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val employeeName = "Employee: Salary UI ${System.nanoTime()}"
            val employeeId = database.accountDao().insert(
                AccountEntity(
                    name = employeeName,
                    type = "Expense",
                    category = "Salary",
                )
            )
            val cashId = database.accountDao().findByCategory("Cash/Bank")
                .single { it.name == "Cash" }.accountId
            val beforeCash = database.readModelDao().accountBalances()
                .single { it.accountId == cashId }.balance
            val beforeEmployee = database.readModelDao().accountBalances()
                .single { it.accountId == employeeId }.balance
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val employeeSpinner = activity.findViewById<Spinner>(R.id.salaryEmployeeSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.salaryPaymentAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveSalaryButton)
                    spinnerPosition(employeeSpinner, employeeName) >= 0 &&
                        spinnerPosition(accountSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val employeeSpinner = activity.findViewById<Spinner>(R.id.salaryEmployeeSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.salaryPaymentAccountSpinner)
                    employeeSpinner.setSelection(spinnerPosition(employeeSpinner, employeeName))
                    accountSpinner.setSelection(spinnerPosition(accountSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.salaryAmountInput).setText("500")
                    assertTrue(activity.findViewById<Button>(R.id.saveSalaryButton).performClick())
                }

                waitForCondition(scenario) { activity ->
                    val status = activity.findViewById<TextView>(R.id.salaryStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.salaryErrorText).text.toString()
                    status.startsWith("Salary payment saved") || error.isNotBlank()
                }

                scenario.onActivity { activity ->
                    val status = activity.findViewById<TextView>(R.id.salaryStatusText).text.toString()
                    val error = activity.findViewById<TextView>(R.id.salaryErrorText).text.toString()
                    assertTrue("Salary save failed: $error", status.startsWith("Salary payment saved"))
                }

                assertEquals(beforeEvents + 1, database.eventDao().count())
                assertEquals(beforeCash - 500.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 1e-9)
                assertEquals(beforeEmployee + 500.0, database.readModelDao().accountBalances().single { it.accountId == employeeId }.balance, 1e-9)
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun salaryScreen_invalidAmount_showsError_andDoesNotCreateEvent() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val employeeName = "Employee: Salary Invalid ${System.nanoTime()}"
            database.accountDao().insert(
                AccountEntity(
                    name = employeeName,
                    type = "Expense",
                    category = "Salary",
                )
            )
            val beforeEvents = database.eventDao().count()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForCondition(scenario) { activity ->
                    val employeeSpinner = activity.findViewById<Spinner>(R.id.salaryEmployeeSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.salaryPaymentAccountSpinner)
                    val saveButton = activity.findViewById<Button>(R.id.saveSalaryButton)
                    spinnerPosition(employeeSpinner, employeeName) >= 0 &&
                        spinnerPosition(accountSpinner, "Cash") >= 0 &&
                        saveButton.isEnabled
                }

                scenario.onActivity { activity ->
                    val employeeSpinner = activity.findViewById<Spinner>(R.id.salaryEmployeeSpinner)
                    val accountSpinner = activity.findViewById<Spinner>(R.id.salaryPaymentAccountSpinner)
                    employeeSpinner.setSelection(spinnerPosition(employeeSpinner, employeeName))
                    accountSpinner.setSelection(spinnerPosition(accountSpinner, "Cash"))
                    activity.findViewById<EditText>(R.id.salaryAmountInput).setText("0")
                    assertTrue(activity.findViewById<Button>(R.id.saveSalaryButton).performClick())
                    val error = activity.findViewById<TextView>(R.id.salaryErrorText)
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
