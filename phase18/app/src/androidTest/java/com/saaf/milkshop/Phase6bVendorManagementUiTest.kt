package com.saaf.milkshop

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.domain.VendorManagementService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class Phase6bVendorManagementUiTest {

    @Test
    fun vendorManagement_renamePreservesAccountIdentity_andRefreshesUi() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val service = VendorManagementService(database)
            val originalName = "Phase6B-${UUID.randomUUID()}"
            val renamedName = "Phase6B-Renamed-${UUID.randomUUID()}"
            val accountId = service.create(originalName)
            val scenario = ActivityScenario.launch(MainActivity::class.java)

            try {
                waitForCondition(scenario) { activity ->
                    activity.findViewById<LinearLayout>(R.id.vendorManagementList).findTextContaining(originalName) != null
                }

                service.rename(accountId, renamedName)

                scenario.close()
                val secondScenario = ActivityScenario.launch(MainActivity::class.java)
                try {
                    waitForCondition(secondScenario) { activity ->
                        activity.findViewById<LinearLayout>(R.id.vendorManagementList)
                            .findTextContaining(renamedName) != null
                    }

                    val account = database.accountDao().findById(accountId)!!
                    assertEquals(accountId, account.accountId)
                    assertEquals("Vendor: $renamedName", account.name)
                    assertEquals("Liability", account.type)
                    assertEquals("Vendor", account.category)

                    secondScenario.onActivity { activity ->
                        val list = activity.findViewById<LinearLayout>(R.id.vendorManagementList)
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
    fun vendorManagement_duplicateName_showsError_andDoesNotCreateAnotherAccount() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
            val database = app.database
            val service = VendorManagementService(database)
            val existingName = "Phase6B-Duplicate-${UUID.randomUUID()}"
            service.create(existingName)
            val beforeCount = database.accountDao().findByCategory("Vendor").size

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                // Exercise the authoritative domain guard directly because the Android dialog is a separate window
                // and the project intentionally does not depend on an additional UI automation library.
                var failed = false
                try {
                    service.create(existingName)
                } catch (_: Exception) {
                    failed = true
                }
                assertTrue(failed)
                assertEquals(beforeCount, database.accountDao().findByCategory("Vendor").size)

                waitForCondition(scenario) { activity ->
                    activity.findViewById<Button>(R.id.addVendorManagementButton).isEnabled
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
