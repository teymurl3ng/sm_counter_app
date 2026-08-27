package com.saaf.milkshop

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards against the exact bug class that caused vendorPaymentSpinner to ship
 * with no adapter: a new Spinner gets added to the layout, but the loader
 * function that populates its data source only remembers to update *some* of
 * the views that depend on it.
 *
 * Rather than asserting on one specific spinner by id (which only protects the
 * spinners that already exist today), this walks the whole view tree and
 * checks every Spinner. Any spinner added in a future phase is covered
 * automatically, and a failure here names the exact unwired view instead of
 * surfacing as a generic "UI condition was not met within 5000ms" timeout
 * three frames away from the real cause.
 */
@RunWith(AndroidJUnit4::class)
class SpinnerWiringSmokeTest {
    @Test
    fun everySpinner_hasAPopulatedAdapter_afterInitialLoad() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            // Ensure at least one row exists for every category a spinner might
            // be sourced from, so an empty-on-purpose spinner can't hide behind
            // "there was no data yet".
            database.accountDao().insert(
                AccountEntity(
                    name = "Vendor: Spinner Wiring Smoke Test ${System.nanoTime()}",
                    type = "Liability",
                    category = "Vendor",
                )
            )
            database.accountDao().insert(
                AccountEntity(
                    name = "Customer: Spinner Wiring Smoke Test ${System.nanoTime()}",
                    type = "Asset",
                    category = "Customer",
                )
            )
            database.accountDao().insert(
                AccountEntity(
                    name = "Employee: Spinner Wiring Smoke Test ${System.nanoTime()}",
                    type = "Expense",
                    category = "Salary",
                )
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                // saveVendorPaymentButton only becomes enabled once both the
                // vendor-loading and payment-account-loading coroutines have
                // finished, so it's a reliable "initial load is complete" signal.
                waitForCondition(scenario) { activity ->
                    activity.findViewById<Button>(R.id.saveVendorPaymentButton).isEnabled &&
                            ((activity.findViewById<Spinner>(R.id.saleProductSpinner).adapter?.count ?: 0) > 0) &&
                            ((activity.findViewById<Spinner>(R.id.creditCollectionCustomerSpinner).adapter?.count ?: 0) > 0) &&
                            ((activity.findViewById<Spinner>(R.id.creditCollectionAccountSpinner).adapter?.count ?: 0) > 0) &&
                            ((activity.findViewById<Spinner>(R.id.expenseCategorySpinner).adapter?.count ?: 0) > 0) &&
                            ((activity.findViewById<Spinner>(R.id.expenseAccountSpinner).adapter?.count ?: 0) > 0) &&
                            ((activity.findViewById<Spinner>(R.id.salaryEmployeeSpinner).adapter?.count ?: 0) > 0) &&
                            ((activity.findViewById<Spinner>(R.id.salaryPaymentAccountSpinner).adapter?.count ?: 0) > 0)
                }

                scenario.onActivity { activity ->
                    val root = activity.findViewById<View>(android.R.id.content)
                    val spinners = mutableListOf<Spinner>()
                    collectSpinners(root, spinners)

                    assertTrue("Expected to find at least one Spinner in the layout", spinners.isNotEmpty())

                    spinners.forEach { spinner ->
                        val name = try {
                            activity.resources.getResourceEntryName(spinner.id)
                        } catch (_: Exception) {
                            "id=${spinner.id}"
                        }
                        val adapter = spinner.adapter
                        assertTrue(
                            "Spinner '$name' has no adapter assigned — its data loader " +
                                "never wired it up. Every Spinner must be wired to the loader that supplies its data.",
                            adapter != null,
                        )
                        assertTrue(
                            "Spinner '$name' has an adapter but zero items even though " +
                                "matching data was seeded before launch — check the query " +
                                "backing it.",
                            adapter!!.count > 0,
                        )
                    }
                }
            } finally {
                scenario.close()
            }
        }
    }

    private fun collectSpinners(view: View, out: MutableList<Spinner>) {
        if (view is Spinner) {
            out.add(view)
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectSpinners(view.getChildAt(i), out)
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
