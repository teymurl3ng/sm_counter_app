package com.saaf.milkshop

import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11dPurchaseUiTest {
    @Test
    fun purchaseScreen_usesDedicatedCardLayout_andKeepsExistingControls() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                val card = activity.findViewById<LinearLayout>(R.id.purchaseCard)
                val vendorSpinner = activity.findViewById<Spinner>(R.id.vendorSpinner)
                val rawLiters = activity.findViewById<EditText>(R.id.rawLitersInput)
                val fat = activity.findViewById<EditText>(R.id.fatInput)
                val lr = activity.findViewById<EditText>(R.id.lrInput)
                val save = activity.findViewById<Button>(R.id.savePurchaseButton)

                assertTrue("purchase card must be visible", card.visibility == android.view.View.VISIBLE)
                assertTrue("purchase card must have a surface background", card.background != null)
                assertTrue(vendorSpinner.id == R.id.vendorSpinner)
                assertTrue(rawLiters.id == R.id.rawLitersInput)
                assertTrue(fat.id == R.id.fatInput)
                assertTrue(lr.id == R.id.lrInput)
                assertTrue(save.id == R.id.savePurchaseButton)
            }
        } finally {
            scenario.close()
        }
    }
}
