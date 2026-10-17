package com.saaf.milkshop

import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.*
import org.junit.Test
import org.junit.Assert.*

internal class FollowUpReviewUiTest: FollowUpNavigationUiTest() {
    private var state=FollowUpWorkspaceSnapshot(FollowUpPolicy("fixture",date.minusDays(2),true),date,
        listOf(FollowUpCustomer(7,"fixture",row.name,null,true,0,null)),online=true,owner=true,saveToken="fixture",freshness="Checked against synced records")
    private val commands=mutableListOf<FollowUpCommand>()
    private val requests=mutableListOf<Pair<String,Boolean>>()
    private var loadFailure=false
    private val access=FollowUpWorkspaceAccess(load={if(loadFailure) error("Synthetic offline check");state},command={command,_->
        commands+=command
        val pref=(command as FollowUpCommand.Preference).value
        state=state.copy(preferences=mapOf("fixture" to pref.copy(revision="accepted")),saveToken="accepted")
        FollowUpWorkspaceSave(state,true)
    },reviewRequest={uid,aside->requests+=uid to aside;state=state.copy(requests=state.requests.filterNot {it.uid==uid});FollowUpWorkspaceSave(state,true)})
    private fun statement() {
        rule.onNodeWithTag(NativeUiTags.OwnerCustomersList).performScrollToNode(hasText(row.name))
        rule.onNodeWithText(row.name,useUnmergedTree=true).performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("customer-statement-entry").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("customer-statement-entry").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("statement-followup-schedule").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun schedule() {
        statement();rule.onNodeWithTag("statement-followup-schedule").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-save-schedule").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun activity() {
        rule.onNodeWithTag("followup-attention-entry").performClick()
        rule.onNodeWithTag("followup-settings").performScrollTo().performClick()
        rule.onNodeWithTag("followup-shared-activity").performScrollTo().performClick()
        rule.onNodeWithText("Follow-up activity").assertIsDisplayed()
    }
    @Test fun directScheduleForSettledCustomerKeepsReturnAndTerms() {
        show(workspaceAccess=access,balancePaisa=0);schedule()
        rule.onNodeWithText("Monthly",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithTag("followup-monthly-day").performScrollTo().performTextReplacement("15")
        rule.onNodeWithTag("followup-save-schedule").performScrollTo().performClick()
        rule.waitUntil(10000) {commands.size==1}
        assertEquals(StatementReviewSchedule.Monthly(15),(commands.single() as FollowUpCommand.Preference).value.schedule)
        rule.onNodeWithTag("customer-schedule-notice").performScrollTo().assertTextEquals("Statement schedule saved. Payment terms are unchanged.")
        loadFailure=true
        rule.onNodeWithText("Refresh schedule").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("customer-schedule-error").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("followup-save-schedule").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Back to statement").performScrollTo().performClick()
        rule.onNodeWithTag("statement-followup-schedule").assertExists();assertEquals(0,gates.get());capture("settled-schedule-return")
    }
    @Test fun darkLargeScheduleKeepsDraftAndChrome() {
        show(2f,true,workspaceAccess=access,balancePaisa=0);schedule()
        val header=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val nav=rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Custom interval",useUnmergedTree=true).performScrollTo().performClick()
        rule.onNodeWithTag("followup-custom-days").performScrollTo().performTextReplacement("14")
        rule.onNodeWithText("Back to statement").performScrollTo().performClick()
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithTag("followup-custom-days").performScrollTo().assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("14")));capture("schedule-dark-large-draft")
        assertEquals(header,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        assertEquals(nav,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
        rule.onNodeWithText("Back to statement").performScrollTo().performClick();rule.onNodeWithTag("customer-schedule-discard").performClick()
        rule.onNodeWithTag("statement-followup-schedule").performScrollTo().performClick()
        rule.onNodeWithTag("followup-custom-days").assertDoesNotExist();assertTrue(commands.isEmpty())
    }
    @Test fun sharedActivityAndRecoveryNeverLaunch() {
        state=state.copy(requests=listOf(FollowUpRequestRow("original","Owner alert check recorded",null,true,true,true,true)),
            activity=listOf(FollowUpActivityRow(1,"Staff reported the message app did not open",row.name,"Another staff account · Another device","18 Oct 2026, 12:00")))
        show(workspaceAccess=access);activity()
        rule.onNodeWithText("Restored from backup · Result needs review").performScrollTo().assertIsDisplayed();capture("shared-restored-request")
        rule.onNodeWithTag("followup-review-check-original").performScrollTo().performClick()
        rule.waitUntil(10000) {requests.size==1}
        assertEquals(listOf("original" to false),requests);assertEquals(0,gates.get())
        rule.onNodeWithTag("followup-notice").performScrollTo().assertTextEquals("Action checked. No message app opened and no alert was shown.")
        rule.onNodeWithText("Staff reported the message app did not open").performScrollTo().assertIsDisplayed()
    }
    @Test fun foreignRestoreRequiresExplicitOwnerSetAside() {
        state=state.copy(owner=false,requests=listOf(FollowUpRequestRow("foreign","Message preparation started",null,true,false,false,false)),
            claims=listOf(FollowUpClaimRow("shared-attempt",7,row.name,"HANDOFF_STARTED",false)))
        show(initialRole=AppRole.MANAGER,workspaceAccess=access);activity()
        rule.onNodeWithTag("followup-review-aside-foreign").performScrollTo().assertIsNotEnabled()
        rule.runOnIdle {role=AppRole.OWNER;state=state.copy(owner=true,requests=state.requests.map {it.copy(canSetAside=true)})}
        rule.waitForIdle();activity()
        rule.onNodeWithTag("followup-review-check-foreign").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("followup-review-aside-foreign").performScrollTo().performClick()
        rule.onNodeWithText("Keep for review").performClick();assertTrue(requests.isEmpty())
        rule.onNodeWithTag("followup-review-aside-foreign").performScrollTo().performClick()
        rule.onNodeWithTag("followup-review-aside-confirm").performClick();rule.waitUntil(10000) {requests.size==1}
        assertEquals(listOf("foreign" to true),requests);assertEquals(1,state.claims.size);assertEquals(0,gates.get())
    }
    @Test fun sharedStartedClaimOpensReviewAndReturnsActivity() {
        state=state.copy(claims=listOf(FollowUpClaimRow("exact-attempt",7,row.name,"HANDOFF_STARTED",true)))
        val handoff=CustomerSharedHandoffAccess(load={CustomerSharedHandoffState("exact-attempt","HANDOFF_STARTED",canResolve=true,blockedReason="Review this handoff")},
            open={_,_,_,_,_->gates.incrementAndGet();error("Review must not open")},recover={_,_->error("Unexpected")},release={_,_->error("Unexpected")},resolve={_,_,_,_->error("Unexpected")})
        show(workspaceAccess=access,handoff=handoff);activity()
        rule.onNodeWithTag("followup-review-claim-7").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("reminder-review-handoff").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("reminder-open").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Back to follow-up").performScrollTo().performClick()
        rule.onNodeWithText("Follow-up activity").assertIsDisplayed();assertEquals(0,gates.get())
    }
}
