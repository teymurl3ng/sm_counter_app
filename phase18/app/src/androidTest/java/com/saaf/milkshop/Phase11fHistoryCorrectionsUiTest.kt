package com.saaf.milkshop

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11fHistoryCorrectionsUiTest {

    @Test
    fun recordsScreen_exposesHistoryDetailAndCorrectionSurfaces() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                activity.findViewById<LinearLayout>(R.id.historyListCard).also {
                    assertEquals(View.VISIBLE, it.visibility)
                }
                activity.findViewById<TextView>(R.id.transactionHistoryText).also {
                    assertTrue(it.width >= 0)
                }
                activity.findViewById<LinearLayout>(R.id.historyDetailCard).also {
                    assertEquals(View.VISIBLE, it.visibility)
                }
                activity.findViewById<Button>(R.id.correctTransactionButton).also {
                    assertEquals(View.GONE, it.visibility)
                }
                activity.findViewById<LinearLayout>(R.id.correctionEditorContainer).also {
                    assertEquals(View.GONE, it.visibility)
                }
                activity.findViewById<EditText>(R.id.correctionNotesInput).also {
                    assertTrue(it.minHeight > 0)
                }
            }
        } finally {
            scenario.close()
        }
    }
}
