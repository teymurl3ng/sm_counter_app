package com.saaf.milkshop

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.sync.FollowUpAttention
import org.junit.*
import org.junit.Assert.*
import java.io.File

class FollowUpAttentionUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val attention=mutableStateOf(FollowUpAttention(count=2,stale=false))
    @Test fun normalAttentionRetainsChromeAcrossZeroAndStaleUpdates()=flow(1f,false)
    @Test fun darkLargeAttentionRetainsChromeAcrossZeroAndStaleUpdates()=flow(2f,true)
    private fun flow(scale: Float,dark: Boolean) {
        var opens=0
        rule.activityRule.scenario.onActivity {
            it.enableEdgeToEdge();it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.getInsetsController(it.window,it.window.decorView).apply {
                isAppearanceLightStatusBars=false;isAppearanceLightNavigationBars=!dark
            }
        }
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {SaafTheme(darkTheme=dark) {
                OwnerScaffold(SnackbarHostState(),OwnerSection.More,{},content={padding->Column(Modifier.fillMaxSize().padding(padding)) {
                    CashierScreenHeader("Customers","Configured shop","Owner","2026-10-18",true)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        FollowUpAttentionEntry(attention.value) {opens++}
                        Text("Existing customer search and list",Modifier.padding(top=16.dp))
                    }
                }})
            }}
        }
        val header=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val footer=rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("followup-attention-description",useUnmergedTree=true).assertTextEquals("2 customers need review")
        rule.onNodeWithTag("followup-attention-entry").performClick();assertEquals(1,opens)
        rule.runOnIdle {attention.value=FollowUpAttention(count=0,stale=false)}
        rule.onNodeWithTag("followup-attention-entry").assertIsDisplayed()
        rule.onNodeWithTag("followup-attention-description",useUnmergedTree=true).assertTextEquals("No follow-ups need review")
        rule.runOnIdle {attention.value=FollowUpAttention(count=2,stale=true)}
        rule.onNodeWithTag("followup-attention-description",useUnmergedTree=true).assertTextEquals("2 customers · Sync to check current records")
        assertEquals(header,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        assertEquals(footer,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
        val context=InstrumentationRegistry.getInstrumentation().targetContext;assertEquals("com.saaf.milkshop.qa.tests",context.packageName)
        val dir=File(context.getExternalFilesDir(null),"followup-alert").apply {mkdirs()}
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir,"entry-$scale-$dark.png")))
    }
}
