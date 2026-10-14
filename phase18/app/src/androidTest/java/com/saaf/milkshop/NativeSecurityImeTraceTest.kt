package com.saaf.milkshop

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.qa.NativeEvidenceCapture
import com.saaf.milkshop.qa.NativeLayoutImeEvidence
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real IME on the shared security settings form; never saves/enrolls a credential. */
class NativeSecurityImeTraceTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val frames=JSONArray()
    private var path="initial"
    private var viewportBottom=0f
    private var headerBottom=0f
    private var securityOpen by mutableStateOf(true)
    private var initialMode=0

    @Test fun pinSetupKeyboardTrace() {
        rule.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            it.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            initialMode=it.window.attributes.softInputMode
        }
        rule.setContent {
            SaafTheme {
                AdaptiveNavigationScaffold(SnackbarHostState(),
                    compactNavigation={ OwnerNavigation(OwnerSection.More) {} },expandedNavigation={}) {
                    Column(Modifier.fillMaxSize()) {
                        CashierScreenHeader("Personal security","Security Fixture","Owner","2026-10-14",true,
                            Modifier.onGloballyPositioned { headerBottom=it.boundsInWindow().bottom })
                        Column(Modifier.weight(1f).fillMaxWidth().imePadding()
                            .onGloballyPositioned { viewportBottom=it.boundsInWindow().bottom }.verticalScroll(rememberScrollState())
                            .padding(horizontal=16.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            if (securityOpen) NativePersonalSecurityPanel(81,"Security Fixture",NativeMoreSecurityStatus(false,false,true,"Phone security"),
                                false,null,false,NativePersonalSecurityTags("security-status","security-passphrase","security-pin","security-save",
                                    "security-phone","security-remove","security-confirm","security-error"),
                                {},{}, { it.fill('\u0000'); error("No removal") },
                                { a,b -> a.fill('\u0000');b.fill('\u0000');error("No PIN enrollment") },
                                { a,b -> a.fill('\u0000');b?.fill('\u0000');error("No biometric enrollment") })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        val listener=ViewTreeObserver.OnPreDrawListener {
            val insets=NativeLayoutImeEvidence.imeInsets(rule.activity)
            frames.put(JSONObject().put("path",path).put("timeMs",SystemClock.uptimeMillis())
                .put("imeBottom",insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
                .put("imeVisible",insets.isVisible(WindowInsetsCompat.Type.ime()))
                .put("viewportBottom",viewportBottom.toDouble()).put("headerBottom",headerBottom.toDouble()))
            true
        }
        rule.runOnIdle { rule.activity.window.decorView.viewTreeObserver.addOnPreDrawListener(listener) }
        for (round in 1..2) {
            path="pin-$round"
            val node=rule.onNodeWithTag("security-pin")
            node.performScrollTo()
            tap(node)
            captureBurst(path)
            waitForIme()
            NativeLayoutImeEvidence.assertFocusedFieldNotOccluded(rule.activity,node)
            NativeLayoutImeEvidence.dismissImeAndWait(rule.activity,rule)
        }
        path="passphrase-to-pin"
        rule.onNodeWithTag("security-passphrase").performScrollTo().performClick()
        waitForIme()
        rule.onNodeWithTag("security-pin").performScrollTo().performClick()
        captureBurst(path)
        waitForIme()
        NativeLayoutImeEvidence.assertFocusedFieldNotOccluded(rule.activity,rule.onNodeWithTag("security-pin"))
        assertFalse("No transient Auto-fill setup popup remains",UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).hasObject(By.text("Auto-fill")))
        rule.onNodeWithTag("security-pin").performImeAction()
        rule.waitUntil(5_000) {
            var hidden=false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                hidden=!NativeLayoutImeEvidence.imeInsets(rule.activity).isVisible(WindowInsetsCompat.Type.ime())
            };hidden
        }
        rule.runOnIdle { securityOpen=false }
        rule.waitForIdle()
        assertEquals("Leaving security restores the window mode",initialMode,rule.activity.window.attributes.softInputMode)
        rule.runOnIdle { rule.activity.window.decorView.viewTreeObserver.removeOnPreDrawListener(listener) }
        val directory=File(requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)),"security-ime-trace").apply { mkdirs() }
        File(directory,"frames.json").writeText(JSONObject().put("synthetic",true).put("frames",frames).toString(2))
        assertTrue(frames.length()>0)
        val headerPositions=(0 until frames.length()).map { frames.getJSONObject(it).getDouble("headerBottom") }
        assertTrue("Keyboard must not pan the fixed header: $headerPositions",headerPositions.max()-headerPositions.min()<=1.0)
    }

    private fun tap(node:SemanticsNodeInteraction) {
        val bounds=node.fetchSemanticsNode().boundsInWindow
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view=rule.activity.window.decorView
            val time=SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                val event=MotionEvent.obtain(time,time,action,bounds.center.x,bounds.center.y,0)
                view.dispatchTouchEvent(event);event.recycle()
            }
        }
    }
    private fun captureBurst(label:String) {
        for (frame in 1..4) {
            Thread.sleep(40)
            NativeEvidenceCapture.captureWholeWindow(rule.activity,NativeEvidenceCapture.CaptureSpec("security-ime","$label-$frame",
                "NativeSecurityImeTraceTest",note="Synthetic security form; no secret entered; real IME animation."))
        }
    }
    private fun waitForIme() {
        rule.waitUntil(5_000) {
            var visible=false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                visible=NativeLayoutImeEvidence.imeInsets(rule.activity).isVisible(WindowInsetsCompat.Type.ime())
            };visible
        }
        rule.waitForIdle()
    }
}
