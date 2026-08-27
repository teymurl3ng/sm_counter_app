package com.saaf.milkshop

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BusinessSettingsService
import com.saaf.milkshop.domain.SettingKeys
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase6aBusinessSettingsUiTest {

    @Test
    fun businessSettings_savePersistsAllValues_andUpdatedRetailRateFeedsSaleUi() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database
        val settingsService = BusinessSettingsService(database)
        val original = settingsService.get()
        val updated = BusinessSettings(
            milkRateVendor = 175.0,
            deliveryFeeVendor = 12.0,
            tsStandard = 13.5,
            retailRateMilk = 195.0,
            retailRateYogurt = 285.0,
            defaultDeliverySurcharge = 55.0,
        )
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        try {
            waitForCondition(scenario) { activity ->
                activity.findViewById<EditText>(R.id.businessSettingsRetailRateMilkInput).text
                    .toString().isNotBlank()
            }

            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.businessSettingsMilkRateVendorInput).setText("175")
                activity.findViewById<EditText>(R.id.businessSettingsDeliveryFeeVendorInput).setText("12")
                activity.findViewById<EditText>(R.id.businessSettingsTsStandardInput).setText("13.5")
                activity.findViewById<EditText>(R.id.businessSettingsRetailRateMilkInput).setText("195")
                activity.findViewById<EditText>(R.id.businessSettingsRetailRateYogurtInput).setText("285")
                activity.findViewById<EditText>(R.id.businessSettingsDefaultSurchargeInput).setText("55")
                assertTrue(activity.findViewById<Button>(R.id.saveBusinessSettingsButton).performClick())
            }

            waitForCondition(scenario) { activity ->
                activity.findViewById<TextView>(R.id.businessSettingsStatusText).text
                    .toString() == activity.getString(R.string.business_settings_saved)
            }

            assertEquals(updated.milkRateVendor, database.settingsDao().getValue(SettingKeys.MILK_RATE_VENDOR)!!, 1e-9)
            assertEquals(updated.deliveryFeeVendor, database.settingsDao().getValue(SettingKeys.DELIVERY_FEE_VENDOR)!!, 1e-9)
            assertEquals(updated.tsStandard, database.settingsDao().getValue(SettingKeys.TS_STANDARD)!!, 1e-9)
            assertEquals(updated.retailRateMilk, database.settingsDao().getValue(SettingKeys.RETAIL_RATE_MILK)!!, 1e-9)
            assertEquals(updated.retailRateYogurt, database.settingsDao().getValue(SettingKeys.RETAIL_RATE_YOGURT)!!, 1e-9)
            assertEquals(updated.defaultDeliverySurcharge, database.settingsDao().getValue(SettingKeys.DEFAULT_DELIVERY_SURCHARGE)!!, 1e-9)

            waitForCondition(scenario) { activity ->
                activity.findViewById<EditText>(R.id.saleRateInput).text.toString() == "195.00"
            }
        } finally {
            scenario.close()
            settingsService.update(original)
        }
    }

    @Test
    fun businessSettings_invalidValue_showsError_andDoesNotChangePersistedValues() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MilkShopApplication>()
        val database = app.database
        val settingsService = BusinessSettingsService(database)
        val before = settingsService.get()
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        try {
            waitForCondition(scenario) { activity ->
                activity.findViewById<EditText>(R.id.businessSettingsRetailRateMilkInput).text
                    .toString().isNotBlank()
            }

            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.businessSettingsRetailRateMilkInput).setText("0")
                assertTrue(activity.findViewById<Button>(R.id.saveBusinessSettingsButton).performClick())
            }

            waitForCondition(scenario) { activity ->
                activity.findViewById<TextView>(R.id.businessSettingsErrorText).visibility == View.VISIBLE
            }

            val after = settingsService.get()
            assertEquals(before.milkRateVendor, after.milkRateVendor, 1e-9)
            assertEquals(before.deliveryFeeVendor, after.deliveryFeeVendor, 1e-9)
            assertEquals(before.tsStandard, after.tsStandard, 1e-9)
            assertEquals(before.retailRateMilk, after.retailRateMilk, 1e-9)
            assertEquals(before.retailRateYogurt, after.retailRateYogurt, 1e-9)
            assertEquals(before.defaultDeliverySurcharge, after.defaultDeliverySurcharge, 1e-9)
        } finally {
            scenario.close()
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
            scenario.onActivity { activity ->
                matched = condition(activity)
            }
            if (matched) return
            delay(50)
        }
        throw AssertionError("UI condition was not met within ${timeoutMs}ms")
    }
}
