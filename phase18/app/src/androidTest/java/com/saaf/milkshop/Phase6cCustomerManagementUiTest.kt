package com.saaf.milkshop

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.domain.CustomerManagementService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class Phase6cCustomerManagementUiTest {

    @Test
    fun customerManagement_renamePreservesAccountIdentity_andRefreshesUi() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val service = CustomerManagementService(database)
            val originalName = "Phase6C-${UUID.randomUUID()}"
            val renamedName = "Phase6C-Renamed-${UUID.randomUUID()}"
            val accountId = service.create(originalName)
            val scenario = ActivityScenario.launch(MainActivity::class.java)

            try {
                waitForCondition(scenario) { activity ->
                    activity.findViewById<LinearLayout>(R.id.customerManagementList)
                        .findTextContaining(originalName) != null
                }

                service.rename(accountId, renamedName)

                scenario.close()
                val secondScenario = ActivityScenario.launch(MainActivity::class.java)
                try {
                    waitForCondition(secondScenario) { activity ->
                        activity.findViewById<LinearLayout>(R.id.customerManagementList)
                            .findTextContaining(renamedName) != null
                    }

                    val account = database.accountDao().findById(accountId)!!
                    assertEquals(accountId, account.accountId)
                    assertEquals("Customer: $renamedName", account.name)
                    assertEquals("Asset", account.type)
                    assertEquals("Customer", account.category)

                    secondScenario.onActivity { activity ->
                        val list = activity.findViewById<LinearLayout>(R.id.customerManagementList)
                        assertTrue(list.childCount > 0)
                        assertTrue(list.getChildAt(0) is LinearLayout)
                        assertTrue((list.getChildAt(0) as LinearLayout).childCount >= 2)
                    }
                } finally {
                    secondScenario.close()
                }
            } finally {
                if (scenario.state == androidx.lifecycle.Lifecycle.State.RESUMED) {
                    scenario.close()
                }
            }
        }
    }

    @Test
    fun customerManagement_duplicateName_isRejected_andDoesNotCreateAnotherAccount() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val service = CustomerManagementService(database)
            val existingName = "Phase6C-Duplicate-${UUID.randomUUID()}"
            service.create(existingName)
            val beforeCount = database.accountDao().findByCategory("Customer").size

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                var failed = false
                try {
                    service.create(existingName)
                } catch (_: Exception) {
                    failed = true
                }
                assertTrue(failed)
                assertEquals(beforeCount, database.accountDao().findByCategory("Customer").size)

                waitForCondition(scenario) { activity ->
                    activity.findViewById<Button>(R.id.addCustomerManagementButton).isEnabled
                }
            } finally {
                scenario.close()
            }
        }
    }

    private suspend fun waitForCondition(
        scenario: ActivityScenario<MainActivity>,
        timeoutMs: Long = 5000,
        condition: (MainActivity) -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var matched = false
            scenario.onActivity { activity -> matched = condition(activity) }
            if (matched) return
            delay(50)
        }
        throw AssertionError("UI condition was not met within ${timeoutMs}ms")
    }

    private fun LinearLayout.findTextContaining(text: String): TextView? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is TextView && child.text.toString().contains(text)) {
                return child
            }
            if (child is LinearLayout) {
                val nested = child.findTextContaining(text)
                if (nested != null) return nested
            }
        }
        return null
    }
}
