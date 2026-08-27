package com.saaf.milkshop

import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.VendorPurchaseInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase5cCorrectionUiTest {
    @Test
    fun purchaseCorrection_updatesCanonicalState_andCreatesCorrectionEvent() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database
        val vendorName = "Vendor: Phase5C ${System.nanoTime()}"
        val vendorId = database.accountDao().insert(
            AccountEntity(
                name = vendorName,
                type = "Liability",
                category = "Vendor",
            )
        )

        val beforeStock = database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand
        val beforeEvents = database.eventDao().count()
        val purchaseResult = VendorPurchaseService(database).record(
            VendorPurchaseInput(
                vendorAccountId = vendorId,
                occurredAt = "2026-08-20",
                rawLiters = 100.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
                notes = "Phase5C correction source ${System.nanoTime()}",
            )
        )
        val sourceEventId = purchaseResult.eventId
        val sourceEvent = database.eventDao().findById(sourceEventId)!!
        val sourceStock = database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForCondition(scenario) { activity ->
                activity.findViewById<TextView>(R.id.transactionHistoryText)
                    .text.toString().contains(sourceEvent.notes.orEmpty())
            }

            scenario.onActivity { activity ->
                val history = activity.findViewById<TextView>(R.id.transactionHistoryText)
                val spanned = history.text as Spanned
                val spans = spanned.getSpans(0, spanned.length, ClickableSpan::class.java)
                val span = spans.firstOrNull { span ->
                    val start = spanned.getSpanStart(span)
                    val end = spanned.getSpanEnd(span)
                    spanned.subSequence(start, end).toString().contains(sourceEvent.notes.orEmpty())
                } ?: error("Source purchase history entry was not found")
                span.onClick(history)
            }

            waitForCondition(scenario) { activity ->
                activity.findViewById<Button>(R.id.correctTransactionButton).visibility == View.VISIBLE
            }

            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.correctTransactionButton).performClick()
                activity.findViewById<EditText>(R.id.correctionPurchaseRawLitersInput).setText("95")
                activity.findViewById<Button>(R.id.saveCorrectionButton).performClick()
            }

            waitForCondition(scenario) { activity ->
                activity.findViewById<TextView>(R.id.correctionStatusText)
                    .text.toString().startsWith("Correction saved")
            }

            assertEquals(beforeEvents + 2, database.eventDao().count())
            assertEquals(beforeStock + 95.0, database.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand, 1e-9)

            val correctionEvents = database.eventDao().findAllDescending().filter { it.correctsEventId == sourceEventId }
            assertEquals(1, correctionEvents.size)
            assertEquals(sourceEventId, correctionEvents.single().correctsEventId)
            assertEquals(95.0, correctionEvents.single().rawLiters!!, 1e-9)
        } finally {
            scenario.close()
        }

        // Keep the source stock value referenced so this remains visibly tied to the source event.
        assertTrue(sourceStock > beforeStock)
    }

    @Test
    fun monetaryCorrection_invalidAmount_showsError_andCreatesNoEvent() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database
        val cashAccount = database.accountDao().findByCategory("Cash/Bank")
            .first { it.name == "Cash" }
        val beforeEvents = database.eventDao().count()
        val expenseResult = ExpenseService(database).record(
            ExpenseInput(
                category = "Other",
                paymentAccountId = cashAccount.accountId,
                amount = 100.0,
                occurredAt = "2026-08-20",
                notes = "Phase5C monetary correction source ${System.nanoTime()}",
            )
        )
        assertEquals(beforeEvents + 1, database.eventDao().count())

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForCondition(scenario) { activity ->
                activity.findViewById<TextView>(R.id.transactionHistoryText)
                    .text.toString()
                    .contains("Phase5C monetary correction source")
            }
            scenario.onActivity { activity ->
                val history = activity.findViewById<TextView>(R.id.transactionHistoryText)
                val spanned = history.text as Spanned
                val spans = spanned.getSpans(0, spanned.length, ClickableSpan::class.java)
                val span = spans.firstOrNull { span ->
                    val start = spanned.getSpanStart(span)
                    val end = spanned.getSpanEnd(span)
                    spanned.subSequence(start, end).toString().contains("Phase5C monetary correction source")
                } ?: error("Expense history entry was not found")
                span.onClick(history)
            }
            waitForCondition(scenario) { activity ->
                activity.findViewById<Button>(R.id.correctTransactionButton).visibility == View.VISIBLE
            }
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.correctTransactionButton).performClick()
                activity.findViewById<EditText>(R.id.correctionMonetaryAmountInput).setText("0")
                activity.findViewById<Button>(R.id.saveCorrectionButton).performClick()
                val error = activity.findViewById<TextView>(R.id.correctionErrorText)
                assertEquals(View.VISIBLE, error.visibility)
                assertTrue(error.text.toString().contains("greater than zero"))
            }
            assertEquals(beforeEvents + 1, database.eventDao().count())
        } finally {
            scenario.close()
        }
        assertEquals(expenseResult.eventId, database.eventDao().findAllDescending().first { it.eventId == expenseResult.eventId }.eventId)
    }

    private fun waitForCondition(
        scenario: ActivityScenario<MainActivity>,
        timeoutMillis: Long = 5000L,
        condition: (MainActivity) -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            var satisfied = false
            scenario.onActivity { activity -> satisfied = condition(activity) }
            if (satisfied) return
            Thread.sleep(50L)
        }
        throw AssertionError("UI condition was not met within ${timeoutMillis}ms")
    }
}
