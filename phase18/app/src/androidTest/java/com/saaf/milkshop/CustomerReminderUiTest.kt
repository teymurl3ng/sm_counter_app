package com.saaf.milkshop

import android.content.ActivityNotFoundException
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.statements.ReminderChannel
import kotlinx.coroutines.delay
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class CustomerReminderUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture=CustomerReminder(7,"synthetic-customer","Northern community cafe & delivery counter امینہ","Configured shop","+923001234567",15000,"2026-10-16",10000)
    private val events=CopyOnWriteArrayList<CustomerCommunicationAction>()
    private val launches=CopyOnWriteArrayList<Triple<ReminderChannel,String,String>>()
    @Before fun setup() {rule.activityRule.scenario.onActivity {
        it.enableEdgeToEdge()
        it.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }}
    private fun show(scale: Float=1f,dark: Boolean=false,load: suspend (Long)->CustomerReminder={fixture},
        shared: CustomerSharedHandoffAccess?=null,
        launch: (ReminderChannel,String,String)->Unit={c,p,m->launches.add(Triple(c,p,m))}) {
        rule.activityRule.scenario.onActivity {WindowCompat.getInsetsController(it.window,it.window.decorView).apply {
            isAppearanceLightStatusBars=false;isAppearanceLightNavigationBars=!dark
        }}
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {SaafTheme(darkTheme=dark) {
                var guard by remember {mutableStateOf<EntryLeaveGuard?>(null)}
                var visible by remember {mutableStateOf(true)}
                val saved=rememberSaveableStateHolder()
                OwnerScaffold(SnackbarHostState(),OwnerSection.More,{guard?.requestLeave {visible=false} ?: run {visible=false}},content={padding->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        CashierScreenHeader("Customers","Configured shop","Owner","2026-10-16",true)
                        Box(Modifier.weight(1f)) {
                            if(visible) saved.SaveableStateProvider("reminder") {CustomerReminderScreen(7,
                                CustomerCommunicationAccess(reminder=load,record={_,a->events.add(a)},sharedHandoff=shared),{visible=false},{visible=false},{guard=it},launch)}
                            else TextButton(onClick={visible=true}) {Text("Reopen reminder")}
                        }
                    }
                })
            }}
        }
        rule.waitUntil(10000) {rule.onAllNodesWithTag("reminder-message").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun confirm()=rule.onNodeWithTag("reminder-confirm-recipient").performScrollTo().performClick()
    private fun open()=rule.onNodeWithTag("reminder-open").performScrollTo().performClick()
    private fun capture(name: String) {
        val dir=File(context.getExternalFilesDir(null),"reminder-ui").apply {mkdirs()}
        rule.mainClock.advanceTimeBy(500);rule.waitForIdle()
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());device.waitForIdle()
        assertTrue(device.takeScreenshot(File(dir,"$name.png")))
    }
    @Test fun lightReviewHandoffHasStableChromeAndTruthfulHistory()=review(1f,false)
    @Test fun darkLargeTextReviewHandoffHasStableChromeAndTruthfulHistory()=review(2f,true)
    private fun review(scale: Float,dark: Boolean) {
        show(scale,dark)
        val title=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val nav=rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Prepare reminder").performScrollTo();capture("top-$scale-$dark")
        rule.onNodeWithTag("reminder-open").performScrollTo().assertIsNotEnabled()
        confirm();capture("recipient-$scale-$dark")
        rule.onNodeWithTag("reminder-message").performScrollTo().assertTextEquals(fixture.message!!)
        capture("message-$scale-$dark");open()
        rule.waitUntil(10000) {launches.size==1 && events.size==2}
        rule.onNodeWithTag("reminder-notice").performScrollTo().assertTextEquals("Message app opened. Review and send there. Sending is not confirmed.")
        assertEquals(listOf(CommunicationEvent.PREPARED,CommunicationEvent.COMPOSER_OPENED),events.map {it.event})
        assertEquals(1,events.map {it.operationId}.distinct().size)
        assertEquals(title,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        assertEquals(nav,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
        capture("opened-$scale-$dark")
    }
    @Test fun changedBalanceAndPhoneRequireFreshReviewBeforeAnyLaunch() {
        val calls=AtomicInteger();val updated=fixture.copy(balancePaisa=5000,knownOverduePaisa=5000,phone="+923009876543")
        show(load={if(calls.incrementAndGet()>=3) updated else fixture})
        confirm();open()
        rule.waitUntil(10000) {rule.onAllNodesWithText("Records changed. Review the updated message and check the recipient again.").fetchSemanticsNodes().isNotEmpty()}
        assertTrue(launches.isEmpty());rule.onNodeWithTag("reminder-open").assertIsNotEnabled()
        rule.onNodeWithTag("reminder-recipient").performScrollTo().assertTextEquals(updated.phone!!)
        rule.onNodeWithTag("reminder-message").performScrollTo().assertTextEquals(updated.message!!)
        confirm();open();rule.waitUntil(10000) {launches.size==1}
        assertEquals(updated.phone,launches.single().second);assertEquals(updated.message,launches.single().third)
    }
    @Test fun missingPhoneOverrideKeyboardAndDiscardStayInsideTheDraft() {
        show(load={fixture.copy(phone=null)})
        val title=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("reminder-open").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("reminder-phone").performScrollTo().performClick().performTextInput("+923001234567")
        rule.waitUntil(10000) {ViewCompat.getRootWindowInsets(rule.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime())==true}
        rule.onNodeWithTag("reminder-phone").performScrollTo()
        val bottom=rule.onNodeWithTag("reminder-phone").fetchSemanticsNode().boundsInWindow.bottom
        val ime=ViewCompat.getRootWindowInsets(rule.activity.window.decorView)!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
        assertTrue("Phone input must remain above keyboard",bottom<=rule.activity.window.decorView.height-ime+2)
        capture("override-keyboard")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();rule.waitForIdle()
        rule.onNodeWithText("Back to statement").performScrollTo().performClick()
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithTag("reminder-phone").performScrollTo().assertTextContains("+923001234567")
        rule.onNodeWithText("Back to statement").performScrollTo().performClick()
        rule.onNodeWithTag("reminder-discard").performClick()
        rule.onNodeWithText("Reopen reminder").performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("reminder-phone").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("reminder-phone").assertTextContains("")
        assertFalse(rule.onNodeWithTag("reminder-phone").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text.contains("+923001234567"))
        assertEquals(title,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        assertTrue(events.isEmpty());assertTrue(launches.isEmpty())
    }
    @Test fun unavailableWhatsAppAllowsDeliberateSmsAndGuardsBusyTaps() {
        show(load={delay(150);fixture},launch={c,p,m->if(c==ReminderChannel.WhatsApp) throw ActivityNotFoundException() else launches.add(Triple(c,p,m))})
        confirm();open();rule.onNodeWithTag("reminder-open").assertIsNotEnabled()
        rule.waitUntil(10000) {events.any {it.event==CommunicationEvent.HANDOFF_FAILED}}
        rule.onNodeWithTag("reminder-notice").performScrollTo().assertTextEquals("WhatsApp is not available. Set it up, or choose another option.")
        assertTrue(launches.isEmpty())
        rule.onNodeWithText("SMS",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithText("Review and send in your SMS app. Carrier charges may apply. No file is attached.").performScrollTo().assertIsDisplayed()
        open();rule.waitUntil(10000) {launches.size==1}
        assertEquals(ReminderChannel.SMS,launches.single().first)
        assertEquals(listOf(CommunicationEvent.PREPARED,CommunicationEvent.HANDOFF_FAILED,CommunicationEvent.PREPARED,CommunicationEvent.COMPOSER_OPENED),events.map {it.event})
    }
    @Test fun settledAdvanceAndExpiredAccessNeverOpenComposer() {
        val calls=AtomicInteger()
        show(load={when(calls.incrementAndGet()) {1->fixture.copy(balancePaisa=0);2->fixture.copy(balancePaisa=-1000);else->error("Sign in again.")}})
        confirm();rule.onNodeWithTag("reminder-open").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("reminder-message").performScrollTo().assertTextEquals("Settled. No payment reminder is needed.")
        rule.onNodeWithText("Refresh current balance").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText("Customer advance balance",substring=true).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("reminder-open").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Refresh current balance").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText("Sign in again.").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("reminder-open").assertIsNotEnabled();assertTrue(launches.isEmpty())
    }
    private fun review(outcome: String) {
        rule.onNodeWithTag("reminder-review-handoff").performScrollTo().performClick()
        rule.onNodeWithTag("reminder-resolution-confirm").assertIsNotEnabled()
        rule.onNodeWithTag("reminder-resolution-$outcome").performScrollTo().performClick()
        rule.onNodeWithTag("reminder-resolution-confirm").assertIsNotEnabled()
        rule.onNodeWithTag("reminder-resolution-ack").performScrollTo().performClick()
        rule.onNodeWithTag("reminder-resolution-confirm").performClick()
        rule.waitForIdle()
    }
    @Test fun sharedGateOwnsLaunchForBothChannels() {
        var state=CustomerSharedHandoffState()
        val channels=CopyOnWriteArrayList<String>()
        val shared=CustomerSharedHandoffAccess(load={state},open={id,facts,phone,channel,launch->
            assertEquals(7L,id);assertEquals(fixture,facts);assertEquals(fixture.phone,phone)
            channels+=channel;launch();state=CustomerSharedHandoffState("attempt-$channel","HANDOFF_STARTED",canResolve=true,blockedReason="Review this attempt");state
        },recover={_,_->error("Unexpected recovery")},release={_,_->error("Unexpected release")},resolve={id,attempt,outcome,ack->
            assertEquals(7L,id);assertEquals(state.attemptUid,attempt);assertEquals("RETRY_UNCERTAIN",outcome);assertTrue(ack)
            state=CustomerSharedHandoffState();state
        })
        show(shared=shared)
        confirm();open();rule.waitUntil(10000) {launches.size==1}
        rule.onNodeWithTag("reminder-open").assertIsNotEnabled()
        review("RETRY_UNCERTAIN")
        rule.onNodeWithText("SMS",useUnmergedTree=true).performScrollTo().performClick()
        confirm();open();rule.waitUntil(10000) {launches.size==2}
        assertEquals(listOf("WhatsApp","SMS"),channels.toList())
        assertEquals(listOf(ReminderChannel.WhatsApp,ReminderChannel.SMS),launches.map {it.first})
        rule.onNodeWithTag("reminder-open").assertIsNotEnabled();capture("shared-opened-light")
    }
    @Test fun sharedUncertaintyRequiresExactConfirmedResolution() {
        val attempt="pinned-attempt";val calls=CopyOnWriteArrayList<String>()
        var state=CustomerSharedHandoffState(attempt,"HANDOFF_STARTED",canResolve=true,
            pending=listOf(CustomerSharedRequest("original-request","START")),blockedReason="Check unfinished action")
        val shared=CustomerSharedHandoffAccess(load={state},open={_,_,_,_,_->error("Recovery must never open")},recover={id,request->
            assertEquals(7L,id);assertEquals("original-request",request);calls+="recover"
            state
        },release={_,_->error("Unexpected release")},resolve={id,exact,outcome,ack->
            assertEquals(7L,id);assertEquals(attempt,exact);assertEquals("REVIEWED",outcome);assertTrue(ack);calls+="resolve"
            state=CustomerSharedHandoffState();state
        },setAside={id,request,ack->
            assertEquals(7L,id);assertEquals("original-request",request);assertTrue(ack);calls+="set-aside"
            state=state.copy(pending=emptyList(),blockedReason="Review this attempt");state
        })
        show(scale=2f,dark=true,shared=shared)
        rule.onNodeWithTag("reminder-review-handoff").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("reminder-check-request").performScrollTo().performClick()
        rule.waitUntil(10000) {calls.size==1};rule.waitForIdle()
        rule.onNodeWithTag("reminder-set-aside").performScrollTo().performClick()
        rule.onNodeWithText("Keep for review").performClick();assertEquals(listOf("recover"),calls.toList())
        rule.onNodeWithTag("reminder-set-aside").performScrollTo().performClick()
        rule.onNodeWithTag("reminder-set-aside-confirm").performClick()
        rule.waitUntil(10000) {calls.size==2};rule.waitForIdle()
        rule.onNodeWithTag("reminder-review-handoff").performScrollTo().performClick()
        rule.onNodeWithTag("reminder-resolution-confirm").assertIsNotEnabled()
        rule.onNodeWithTag("reminder-resolution-ack").performScrollTo();capture("shared-review-dark-large")
        rule.onNodeWithText("Keep unresolved").performClick();assertEquals(listOf("recover","set-aside"),calls.toList())
        review("REVIEWED");rule.waitUntil(10000) {calls.size==3}
        assertTrue(launches.isEmpty());assertTrue(events.isEmpty())
        rule.onNodeWithTag("reminder-open").performScrollTo().assertIsNotEnabled()
    }
    @Test fun sharedMissingHandlerNeverSilentlySwitchesChannel() {
        var state=CustomerSharedHandoffState();val calls=AtomicInteger()
        val shared=CustomerSharedHandoffAccess(load={state},open={_,_,_,channel,launch->
            assertEquals("WhatsApp",channel);calls.incrementAndGet()
            state=CustomerSharedHandoffState("failed-attempt","HANDOFF_STARTED",canResolve=true,blockedReason="Review this attempt")
            launch();state
        },recover={_,_->error("Unexpected recovery")},release={_,_->error("Unexpected release")},resolve={_,_,_,_->error("Must require an explicit review")})
        show(shared=shared,launch={_,_,_->throw ActivityNotFoundException("Synthetic missing handler")})
        confirm();open();rule.waitUntil(10000) {events.any {it.event==CommunicationEvent.HANDOFF_FAILED}}
        rule.onNodeWithTag("reminder-notice").performScrollTo().assertTextEquals("The message app did not open. Review this attempt before trying another option. You can keep the draft or share the statement.")
        rule.onNodeWithText("SMS",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithTag("reminder-open").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("reminder-review-handoff").performScrollTo().assertIsEnabled()
        assertEquals(1,calls.get());assertTrue(launches.isEmpty());capture("shared-missing-handler-light")
    }
}
