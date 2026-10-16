package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.saaf.milkshop.nativeui.GuardedFollowUpDestination
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.owner.FollowUpNotificationSettings
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.*
import org.junit.Assert.*
import java.util.regex.Pattern

internal class FollowUpIntegrationUiTest: FollowUpNavigationUiTest() {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun setup() {assertEquals("com.saaf.milkshop.qa.tests",context.packageName)
        com.saaf.milkshop.sync.SyncScheduler.suppressSchedulingForTest()}
    @Test fun contextualAction() {
        context.getSharedPreferences("followup_notification_ui",0).edit().clear().commit()
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        rule.setContent {SaafTheme {Column {FollowUpNotificationSettings()}}}
        rule.onNodeWithTag("followup-notification-settings").assertIsDisplayed()
        val deny=By.res(Pattern.compile(".*:id/permission_deny_button"))
        assertFalse(device.hasObject(deny)) // No prompt until the deliberate action.
        rule.onNodeWithTag("followup-notification-settings").performClick()
        val button=device.wait(Until.findObject(deny),7000)
        assertNotNull("Expected contextual Android permission dialog",button);button.click()
        rule.waitForIdle();rule.onNodeWithTag("followup-notification-settings").assertIsDisplayed()
        assertTrue(context.getSharedPreferences("followup_notification_ui",0).getBoolean("requested",false))
        rule.onNodeWithTag("followup-notification-settings").performClick()
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")),7000))
        device.pressBack();rule.waitForIdle()
        rule.onNodeWithTag("followup-notification-settings").assertIsDisplayed()
    }
    @Test fun guardedOpen() {
        val guard=EntryLeaveGuard().apply {markDirty()}
        var request by mutableStateOf<String?>("first")
        var opens=0;var consumed=0
        rule.setContent {SaafTheme {GuardedFollowUpDestination(request,"fixture",guard,{opens++},{consumed++;request=null})}}
        rule.waitForIdle();assertTrue(guard.discardConfirmationOpen);assertEquals(0,opens)
        rule.runOnIdle {guard.keepEditing()};rule.waitForIdle();assertEquals(0,opens);assertTrue(guard.dirty)
        rule.runOnIdle {request="second"};rule.waitForIdle();assertTrue(guard.discardConfirmationOpen)
        rule.runOnIdle {guard.confirmDiscard()};rule.waitForIdle()
        assertEquals(1,opens);assertEquals(1,consumed);assertFalse(guard.dirty)
    }
    @Test fun repeatedOpen() {
        val request=kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        show(routeRequest=request)
        rule.onNodeWithTag("followup-workspace").assertDoesNotExist()
        rule.runOnIdle {request.value="first"}
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-workspace").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Back to customers").performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-attention-entry").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle {request.value="second"}
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-workspace").fetchSemanticsNodes().isNotEmpty()}
        assertNull(request.value);assertEquals(0,gates.get());capture("notification-followups-return")
    }
}
