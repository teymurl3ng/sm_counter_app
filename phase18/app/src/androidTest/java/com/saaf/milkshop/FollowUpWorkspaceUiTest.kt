package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class FollowUpWorkspaceUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val today=LocalDate.parse("2026-10-18")
    private val cafe=FollowUpCustomer(41,"cafe","Northern community cafe and delivery counter امینہ",null,true,1234567890,
        CustomerCreditStatus(CreditStatusKind.LATE,25000,4))
    private val unknown=FollowUpCustomer(42,"unknown","Market counter",null,true,20000,null)
    private var state=FollowUpWorkspaceSnapshot(FollowUpPolicy("v1",today.minusDays(2),true),today,listOf(cafe,unknown,
        cafe.copy(accountId=43,uid="settled",name="Settled",balancePaisa=0,status=CustomerCreditStatus(CreditStatusKind.SETTLED))),
        online=true,owner=true,freshness="Checked against local records",primaryDevice="Owner tablet",shopZone="Asia/Karachi")
    private val commands=CopyOnWriteArrayList<FollowUpCommand>()
    private val opens=CopyOnWriteArrayList<Long>()
    private val recoveries=CopyOnWriteArrayList<Pair<String,Boolean>>()
    private fun show(scale: Float=1f,dark: Boolean=false,failLoadOnce: Boolean=false,failSave: Boolean=false,unfinishedSave: Boolean=false) {
        rule.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            WindowCompat.getInsetsController(it.window,it.window.decorView).apply {
                isAppearanceLightStatusBars=false
                isAppearanceLightNavigationBars=!dark
            }
        }
        val loads=AtomicInteger()
        val access=FollowUpWorkspaceAccess(load={if(failLoadOnce && loads.incrementAndGet()==1) error("Synthetic connection unavailable") else state},command={command,_->
            commands.add(command)
            if(failSave) error("Synthetic save failed")
            if(unfinishedSave) {
                state=state.copy(policy=state.policy.copy(revision="changed-on-server",repeatDays=25),unfinished=listOf(FollowUpUnfinishedSave("synthetic-id","POLICY")))
                FollowUpWorkspaceSave(state,false)
            } else {
            state=when(command) {
                is FollowUpCommand.Policy->state.copy(policy=command.value.copy(revision="v2"))
                is FollowUpCommand.Preference->state.copy(preferences=state.preferences+(command.customerUid to command.value.copy(revision="pref-v2")))
                is FollowUpCommand.Reviewed->state.copy(acknowledgments=state.acknowledgments+(command.customerUid to FollowUpAcknowledgment(command.expectedCycle,command.on)))
                is FollowUpCommand.Snooze->state.copy(preferences=state.preferences+(command.customerUid to CustomerFollowUpPreference("pref-snooze",today,snoozedUntil=command.until)))
            };FollowUpWorkspaceSave(state,true)
            }
        },recover={uid,setAside->recoveries.add(uid to setAside);state=state.copy(unfinished=emptyList());FollowUpWorkspaceSave(state,true)})
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {SaafTheme(darkTheme=dark) {
                var guard by remember {mutableStateOf<EntryLeaveGuard?>(null)}
                OwnerScaffold(SnackbarHostState(),OwnerSection.More,{guard?.requestLeave {}},content={padding->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        CashierScreenHeader("Customers","Configured shop","Owner",today.toString(),true)
                        Box(Modifier.weight(1f)) {FollowUpWorkspace(access,{},onStatement={opens.add(it)},onReminder={opens.add(it)},publishGuard={guard=it})}
                    }
                })
            }}
        }
        rule.waitUntil(10000) {rule.onAllNodesWithTag(if(failLoadOnce) "followup-error" else "followup-count").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun capture(name: String) {
        rule.mainClock.advanceTimeBy(500);rule.waitForIdle()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.saaf.milkshop.qa.tests",context.packageName)
        val dir=File(context.getExternalFilesDir(null),"followup-a").apply {mkdirs()}
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());device.waitForIdle()
        assertTrue(device.takeScreenshot(File(dir,"$name.png")))
    }
    @Test fun lightListReviewAndSettingsRetainChromeAndManualFallback()=flow(1f,false)
    @Test fun darkLargeTextListReviewAndSettingsRetainChromeAndManualFallback()=flow(2f,true)
    private fun flow(scale: Float,dark: Boolean) {
        show(scale,dark)
        val title=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val nav=rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("followup-count").performScrollTo().assertTextEquals("2 customers")
        rule.onNodeWithTag("followup-row-41").performScrollTo().assertIsDisplayed();capture("list-$scale-$dark")
        rule.onNodeWithTag("followup-row-41").performClick()
        rule.onNodeWithTag("followup-manual-fallback").performScrollTo().assertIsDisplayed();capture("review-$scale-$dark")
        rule.onNodeWithTag("followup-prepare").performScrollTo().performClick();assertEquals(listOf(41L),opens.toList())
        rule.onNodeWithTag("followup-reviewed").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-count").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("followup-count").performScrollTo().assertTextEquals("1 customer")
        rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithText("Automatic SMS is not set up. To use the separate shop phone, type and send the message there.").performScrollTo().assertIsDisplayed()
        capture("settings-$scale-$dark")
        assertEquals(title,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        assertEquals(nav,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
    }
    @Test fun invalidPolicyCannotSaveAndDirtyExitCanBeKeptOrDiscarded() {
        show();rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithTag("followup-repeat").performScrollTo().performTextReplacement("0")
        rule.onNodeWithTag("followup-save-policy").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Back",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithTag("followup-repeat").performScrollTo().assertTextContains("0")
        rule.onNodeWithText("Back",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithText("Discard changes").performClick()
        rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithTag("followup-repeat").performScrollTo().assertTextContains("7")
        rule.onNodeWithTag("followup-repeat").performTextReplacement("10")
        rule.onNodeWithTag("followup-repeat").assertTextContains("10")
        rule.onNodeWithTag("followup-repeat").performImeAction()
        rule.waitUntil(10000) {androidx.core.view.ViewCompat.getRootWindowInsets(rule.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())!=true}
        rule.onNodeWithTag("followup-save-policy").performScrollTo().assertIsEnabled()
        rule.waitForIdle()
        rule.onNodeWithTag("followup-save-policy").performClick()
        rule.onNodeWithTag("followup-confirm-updated").performClick()
        rule.onNodeWithTag("followup-confirm-policy").performClick()
        capture("policy-save")
        rule.waitUntil(10000) {commands.size==1};assertEquals(10,(commands.single() as FollowUpCommand.Policy).value.repeatDays)
        rule.onNodeWithTag("followup-notice").assertIsDisplayed().assertTextEquals("Follow-up settings saved.")
    }
    @Test fun customerMonthlyAndCustomValidationDoesNotEditPaymentTerms() {
        show();rule.onNodeWithTag("followup-row-41").performScrollTo().performClick()
        rule.onNodeWithTag("followup-schedule").performScrollTo().performClick()
        rule.onNodeWithText("Custom interval",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithTag("followup-custom-days").performScrollTo().performTextReplacement("366")
        rule.onNodeWithTag("followup-save-schedule").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Monthly",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithTag("followup-monthly-day").performScrollTo().performTextReplacement("32")
        rule.onNodeWithTag("followup-save-schedule").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("followup-monthly-day").performScrollTo().performTextReplacement("0")
        capture("monthly-preference")
        rule.onNodeWithTag("followup-save-schedule").performScrollTo().performClick()
        rule.waitUntil(10000) {commands.size==1}
        assertEquals(StatementReviewSchedule.Monthly(0),(commands.single() as FollowUpCommand.Preference).value.schedule)
        assertEquals(CreditStatusKind.LATE,state.customers.first().status!!.kind)
        rule.onNodeWithTag("followup-row-41").performScrollTo().performClick()
        rule.onNodeWithTag("followup-schedule").performScrollTo().performClick()
        rule.onNodeWithText("Paused",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithTag("followup-save-schedule").performScrollTo().performClick()
        rule.waitUntil(10000) {commands.size==2}
        val paused=(commands.last() as FollowUpCommand.Preference).value
        assertTrue(paused.paused);assertEquals(StatementReviewSchedule.Monthly(0),paused.schedule)
    }
    @Test fun failedLoadAndFailedSaveRetainFeedbackAndCanRetry() {
        show(failLoadOnce=true,failSave=true)
        rule.onNodeWithTag("followup-error").assertTextEquals("Synthetic connection unavailable")
        rule.onNodeWithTag("followup-refresh").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-count").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithTag("followup-repeat").performScrollTo().performTextReplacement("8")
        rule.onNodeWithTag("followup-repeat").performImeAction()
        rule.waitUntil(10000) {androidx.core.view.ViewCompat.getRootWindowInsets(rule.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())!=true}
        rule.onNodeWithTag("followup-save-policy").performScrollTo().performClick()
        rule.onNodeWithTag("followup-confirm-updated").performClick()
        rule.onNodeWithTag("followup-confirm-policy").performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText("Synthetic save failed").fetchSemanticsNodes().isNotEmpty()}
        rule.waitForIdle()
        rule.onNodeWithTag("followup-error").assertIsDisplayed()
        rule.onNodeWithTag("followup-repeat").performScrollTo().assertTextContains("8")
        rule.onNodeWithTag("followup-save-policy").performScrollTo().assertIsEnabled()
    }
    @Test fun offlineLeadershipKeepsReviewAvailableWithoutSavingSharedState() {
        state=state.copy(owner=false,online=false)
        show();rule.onNodeWithTag("followup-row-41").performScrollTo().performClick()
        rule.onNodeWithTag("followup-prepare").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("followup-reviewed").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Back",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithTag("followup-enabled").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("followup-save-policy").performScrollTo().assertIsNotEnabled()
        assertTrue(commands.isEmpty())
    }
    @Test fun primaryTransferRequiresExplicitCompatibleDeviceConfirmation() {
        show(2f,true);rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithTag("followup-primary").performScrollTo().performClick()
        rule.onNodeWithTag("followup-confirm-policy").assertIsNotEnabled()
        assertTrue(commands.isEmpty())
        rule.onNodeWithTag("followup-confirm-updated").performScrollTo().performClick()
        rule.onNodeWithTag("followup-confirm-primary").performScrollTo().assertIsDisplayed()
        capture("primary-confirmation-dark-2x")
        rule.onNodeWithTag("followup-confirm-policy").assertIsEnabled().performClick()
        rule.waitUntil(10000) {commands.size==1}
        val command=commands.single() as FollowUpCommand.Policy
        assertTrue(command.makePrimary);assertTrue(command.phonesUpdated)
        rule.onNodeWithTag("followup-notice").assertTextEquals("Follow-up settings saved.")
    }
    @Test fun unfinishedSaveBlocksEditsAndSetAsideExplainsServerOutcome() {
        show(2f,true,unfinishedSave=true);rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithTag("followup-repeat").performScrollTo().performTextReplacement("10")
        rule.onNodeWithTag("followup-repeat").performImeAction()
        rule.waitUntil(10000) {androidx.core.view.ViewCompat.getRootWindowInsets(rule.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())!=true}
        rule.onNodeWithTag("followup-save-policy").performScrollTo().performClick()
        rule.onNodeWithTag("followup-confirm-updated").performScrollTo().performClick()
        rule.onNodeWithTag("followup-confirm-policy").performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-check-save").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("followup-repeat").performScrollTo().assertTextContains("10")
        rule.onNodeWithTag("followup-save-policy").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("followup-set-aside").performScrollTo().performClick()
        rule.onNodeWithText("Keep for review").performClick();assertTrue(recoveries.isEmpty())
        rule.onNodeWithTag("followup-set-aside").performClick()
        rule.onNode(hasText("Remove from this device") and hasAnyAncestor(isDialog())).performClick()
        rule.waitUntil(10000) {recoveries.size==1}
        assertEquals("synthetic-id" to true,recoveries.single());assertEquals(1,commands.size)
        rule.onNodeWithTag("followup-notice").assertTextEquals("Removed from this device. Any change already synced stays in place.")
        rule.onNodeWithTag("followup-check-save").assertDoesNotExist()
    }
}
