package com.saaf.milkshop

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.statements.*
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class StatementSharingUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture=statementExportFixture(20)
    private val launched=AtomicReference<Intent?>()
    private val refreshes=AtomicInteger()
    private fun show(scale: Float=1f,dark: Boolean=false,load: CustomerStatementLoader={_,_->fixture},launch: ((Intent)->Unit)?={launched.set(it)},record: suspend (CustomerCommunicationAction)->Unit={}) {
        val engine=StatementDocumentEngine(context);val document=engine.plan(fixture)
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
                SaafTheme(darkTheme=dark) {
                    OwnerScaffold(SnackbarHostState(),OwnerSection.More,{},content={padding->Column(Modifier.fillMaxSize().padding(padding)) {
                        CashierScreenHeader("Customers","Synthetic shop","Owner","2026-10-16",true)
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                            Text("Synthetic statement")
                            StatementShareControls(document,engine,true,load,{refreshes.incrementAndGet()},launcher=launch,record=record)
                        }
                    }})
                }
            }
        }
    }
    @Test fun observableShareHistoryDoesNotTurnPostLaunchAuditFailureIntoHandoffFailure() {
        val actions=java.util.concurrent.CopyOnWriteArrayList<CustomerCommunicationAction>()
        show(record={actions.add(it);if(it.event==CommunicationEvent.CHOOSER_OPENED) error("Synthetic audit failure")})
        rule.onNodeWithTag("statement-share").performClick()
        rule.onNodeWithTag("statement-open-share-menu").performClick()
        rule.waitUntil(20000) {actions.size==2 && launched.get()!=null}
        rule.onNodeWithTag("statement-share-notice").performScrollTo().assertTextEquals("Share menu opened. Sending is managed in the other app. History on this device could not be saved.")
        assertEquals(listOf(CommunicationEvent.PREPARED,CommunicationEvent.CHOOSER_OPENED),actions.map {it.event})
        assertEquals(1,actions.map {it.operationId}.distinct().size)
        assertEquals("statement",actions.first().kind)
        assertFalse(actions.toString().contains("Amina"))
    }
    @Test fun lightChooserHandoffHasTruthfulFeedbackAndStableChrome()=handoff(1f,false)
    @Test fun darkLargeTextChooserHandoffHasTruthfulFeedbackAndStableChrome()=handoff(2f,true)
    private fun handoff(scale: Float,dark: Boolean) {
        show(scale,dark)
        val title=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val nav=rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("statement-share").performClick()
        rule.onNodeWithText("Images",useUnmergedTree=true).performClick()
        val dir=File(context.getExternalFilesDir(null),"statement-export-ui").apply {mkdirs()}
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        rule.mainClock.advanceTimeBy(500);rule.waitForIdle();device.waitForIdle()
        assertTrue(device.takeScreenshot(File(dir,"format-$scale-$dark.png")))
        rule.onNodeWithTag("statement-open-share-menu").performClick()
        rule.waitUntil(20000) {launched.get()!=null}
        rule.onNodeWithTag("statement-share-notice").performScrollTo().assertTextEquals("Share menu opened. Sending is managed in the other app.")
        assertEquals(Intent.ACTION_CHOOSER,launched.get()!!.action)
        @Suppress("DEPRECATION") val send=launched.get()!!.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("image/png",send.type);assertTrue(send.clipData!!.itemCount>1)
        assertEquals(title,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        assertEquals(nav,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
        rule.mainClock.advanceTimeBy(500);rule.waitForIdle();device.waitForIdle()
        assertTrue(device.takeScreenshot(File(dir,"share-$scale-$dark.png")))
    }
    @Test fun cancelDoesNotExportAndMissingHandlerReportsFailure() {
        show(load={_,_->kotlinx.coroutines.delay(200);fixture},launch={throw ActivityNotFoundException()})
        val root=File(context.cacheDir,StatementExportStore.DIRECTORY)
        val before=root.listFiles()?.map {it.name}?.toSet() ?: emptySet()
        rule.onNodeWithTag("statement-share").performClick()
        rule.onNodeWithTag("statement-cancel-share").performClick()
        assertEquals(before,root.listFiles()?.map {it.name}?.toSet() ?: emptySet<String>())
        rule.onNodeWithTag("statement-share").performClick();rule.onNodeWithTag("statement-open-share-menu").performClick()
        rule.onNodeWithTag("statement-cancel-share").performClick()
        rule.onNodeWithTag("statement-share-notice").assertTextEquals("Sharing cancelled. No share menu opened.")
        rule.onNodeWithTag("statement-share").performClick();rule.onNodeWithTag("statement-open-share-menu").performClick()
        rule.waitUntil(20000) {rule.onAllNodesWithText("No sharing app is available for this format. Enable a suitable app and try again.").fetchSemanticsNodes().isNotEmpty()}
        assertNull(launched.get())
        rule.onNodeWithTag("statement-share").assertIsEnabled()
    }
    @Test fun changedFactsBeforeOrAfterRenderingRequireReviewInsteadOfSharing() {
        var calls=0
        show(load={_,_->calls++;if(calls==2) fixture.copy(customerName="Updated customer") else fixture})
        rule.onNodeWithTag("statement-share").performClick();rule.onNodeWithTag("statement-open-share-menu").performClick()
        rule.waitUntil(20000) {refreshes.get()==1}
        assertEquals(2,calls);assertNull(launched.get())
        rule.onNodeWithTag("statement-share-notice").assertTextEquals("Records changed. Review the refreshed statement before sharing.")
        rule.onNodeWithTag("statement-share").assertIsEnabled()
    }
    @Test fun nativeChooserCanBeDismissedWithoutSending() {
        show(launch=null)
        rule.onNodeWithTag("statement-share").performClick();rule.onNodeWithTag("statement-open-share-menu").performClick()
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val deadline=System.currentTimeMillis()+20000
        while(System.currentTimeMillis()<deadline && (device.currentPackageName==null || device.currentPackageName==context.packageName)) Thread.sleep(100)
        assertNotNull(device.currentPackageName);assertNotEquals(context.packageName,device.currentPackageName)
        device.pressBack();rule.waitForIdle()
        rule.onNodeWithTag("statement-share-notice").performScrollTo().assertTextEquals("Share menu opened. Sending is managed in the other app.")
        assertNull(launched.get()) // No fake launcher and no target/sending action was selected.
    }
}
