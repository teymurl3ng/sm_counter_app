package com.saaf.milkshop

import android.text.Spanned
import android.text.style.ClickableSpan
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.VendorPaymentService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class Phase5bTransactionDetailUiTest {

    @Test
    fun transactionHistory_selectingEvent_showsRecordedDetailsAndLedgerMovements() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val vendorName = "Vendor: Detail UI ${System.nanoTime()}"
            val vendorId = database.accountDao().insert(
                AccountEntity(
                    name = vendorName,
                    type = "Liability",
                    category = "Vendor",
                )
            )
            val cashId = database.accountDao().findByCategory("Cash/Bank")
                .first { it.name == "Cash" }
                .accountId

            val payment = VendorPaymentService(database).record(
                VendorPaymentInput(
                    vendorAccountId = vendorId,
                    paymentAccountId = cashId,
                    occurredAt = LocalDate.now().toString(),
                    amount = 125.0,
                )
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                val targetPrefix = "Event #${payment.eventId} · Vendor Payment"
                waitForCondition(scenario) { activity ->
                    activity.findViewById<TextView>(R.id.transactionHistoryText)
                        .text
                        .toString()
                        .contains(targetPrefix)
                }

                scenario.onActivity { activity ->
                    val history = activity.findViewById<TextView>(R.id.transactionHistoryText)
                    val spanned = history.text as Spanned
                    val start = history.text.indexOf(targetPrefix)
                    assertTrue(start >= 0)
                    val end = start + targetPrefix.length
                    val spans = spanned.getSpans(start, end, ClickableSpan::class.java)
                    assertTrue(spans.isNotEmpty())
                    spans.first().onClick(history)
                }

                waitForCondition(scenario) { activity ->
                    val detail = activity.findViewById<TextView>(R.id.transactionDetailText)
                        .text
                        .toString()
                    detail.contains("Event #${payment.eventId}") &&
                        detail.contains("Type: VENDOR_PAYMENT") &&
                        detail.contains("Financial movements") &&
                        detail.contains(vendorName) &&
                        detail.contains("Cash: ₨-125.00")
                }

                scenario.onActivity { activity ->
                    val detail = activity.findViewById<TextView>(R.id.transactionDetailText)
                        .text
                        .toString()
                    assertTrue(detail.contains("Amount: ₨125.00"))
                    assertTrue(detail.contains("Cash: ₨-125.00"))
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
