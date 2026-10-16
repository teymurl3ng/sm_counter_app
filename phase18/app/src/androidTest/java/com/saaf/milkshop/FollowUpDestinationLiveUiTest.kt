package com.saaf.milkshop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Actual native Activity/auth/More host stack; local synthetic QA identity, no server credentials. */
class FollowUpDestinationLiveUiTest {
    @get:Rule val rule=createEmptyComposeRule()
    @Test fun coldAuthRecreationAndWarmTap() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        assertEquals("com.saaf.milkshop.qa.tests",context.packageName)
        val app=context.applicationContext as MilkShopApplication
        SyncScheduler.suppressSchedulingForTest()
        runBlocking {
            check(app.userSecurity.needsOwnerBootstrap()) {"This one-use native QA fixture requires a fresh QA base database"}
            val result=app.userSecurity.bootstrapOwner("followup.route.owner","Follow-up route QA",PHRASE.toCharArray())
            app.userSecurity.logout(result.session.sessionId)
        }
        val intent=FollowUpNotifier(context).destinationIntent().addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<NativePreviewActivity>(intent).use {scenario->
            rule.waitUntil(15000) {rule.onAllNodesWithTag(NativeUiTags.authAccount("followup.route.owner")).fetchSemanticsNodes().isNotEmpty()}
            rule.onNodeWithTag("followup-workspace").assertDoesNotExist()
            scenario.recreate()
            rule.waitUntil(15000) {rule.onAllNodesWithTag(NativeUiTags.authAccount("followup.route.owner")).fetchSemanticsNodes().isNotEmpty()}
            rule.onNodeWithTag(NativeUiTags.authAccount("followup.route.owner")).performClick()
            rule.waitUntil(10000) {rule.onAllNodesWithTag(NativeUiTags.AuthPassphraseInput).fetchSemanticsNodes().isNotEmpty()}
            rule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).performTextInput(PHRASE)
            rule.onNodeWithTag(NativeUiTags.AuthSignIn).performScrollTo().performClick()
            rule.waitUntil(20000) {rule.onAllNodesWithTag("followup-workspace").fetchSemanticsNodes().isNotEmpty()}
            rule.onNodeWithText("Back to customers").performClick()
            rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-attention-entry").fetchSemanticsNodes().isNotEmpty()}
            scenario.onActivity {it.startActivity(FollowUpNotifier(it).destinationIntent())}
            rule.waitUntil(20000) {rule.onAllNodesWithTag("followup-workspace").fetchSemanticsNodes().isNotEmpty()}
            val device=UiDevice.getInstance(instrumentation)
            device.takeScreenshot(File(File(context.getExternalFilesDir(null),"followup-navigation").apply {mkdirs()},"native-notification-route.png"))
            device.pressBack()
            rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-attention-entry").fetchSemanticsNodes().isNotEmpty()}
        }
        app.currentSessionId?.let {runBlocking {app.userSecurity.logout(it)}}
        app.currentSessionId=null
    }
    companion object {private const val PHRASE="synthetic follow-up route phrase 2026"}
}
