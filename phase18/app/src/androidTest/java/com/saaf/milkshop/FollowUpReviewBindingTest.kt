package com.saaf.milkshop

import android.app.NotificationManager
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

internal class FollowUpReviewBindingTest: FollowUpWorkspaceBindingTest() {
    private val binding get()=FollowUpBinding("dev","https://example.invalid",shop,staff,SyncIdentity.warmUp(context))
    private fun acquire()=JSONObject().put("kind","ACQUIRE").put("customer_uid",uid).put("attempt_uid",id())
        .put("cycle","fixture").put("expected_heads",heads()).put("expected_customer_revision",JSONObject.NULL)
    private suspend fun pending(p: JSONObject,foreign: Boolean=false,restored: Boolean=true): String {
        val request=id();db.followUpDataDao().insertCommand(FollowUpPendingEntity(request,if(foreign) "f".repeat(64) else binding.key,p.toString(),instant,restored=restored));return request
    }
    private fun claim(actor: String=staff,installation: String=binding.installation,started: Boolean=true)=JSONObject()
        .put("customer_uid",uid).put("attempt_uid",id()).put("actor_id",actor).put("installation",installation).put("cycle","fixture")
        .put("phase",if(started) "HANDOFF_STARTED" else "PREPARING")
        .put("lease_until",if(started) JSONObject.NULL else Instant.parse(instant).plusSeconds(300).toString())
        .put("started_at",if(started) instant else JSONObject.NULL).put("channel",if(started) "SMS" else JSONObject.NULL)
    @Test fun activityMapsActorsAndNeverClaimsDelivery(): Unit=runBlocking {
        val before=db.backupDataDao().financialLogs();metadata=3
        actionRows=JSONArray()
        for((index,kind) in listOf("POLICY","START","RETRY_UNCERTAIN").withIndex()) actionRows!!.put(JSONObject()
            .put("seq",index+1).put("command_uid",id()).put("kind",kind).put("actor_id",if(index==1) id() else staff)
            .put("installation",if(index==1) "other-device" else binding.installation).put("customer_uid",if(index==0) JSONObject.NULL else uid)
            .put("attempt_uid",if(index==0) JSONObject.NULL else id()).put("cycle",JSONObject.NULL).put("accepted_at",instant))
        val activity=workspace.load().activity
        assertEquals(listOf(3L,2L,1L),activity.map {it.sequence})
        assertEquals("Staff allowed another attempt",activity[0].label);assertEquals("Message app handoff started",activity[1].label)
        assertEquals("Another staff account · Another device",activity[1].by);assertEquals("Cafe",activity[1].customer)
        assertFalse(activity.any {it.label.contains("delivered",true)||it.label.contains("sent",true)||it.label.contains("paid",true)})
        assertEquals(before,db.backupDataDao().financialLogs());assertTrue(sent.isEmpty())
    }
    @Test fun journalSeparatesCurrentAndRestoredForeignBinding(): Unit=runBlocking {
        val own=pending(acquire());val other=pending(acquire(),foreign=true)
        val state=workspace.load();val rows=state.requests.associateBy {it.uid}
        assertTrue(rows.getValue(own).canCheck);assertTrue(rows.getValue(own).restored)
        assertEquals("Cafe",rows.getValue(own).customer)
        assertFalse(rows.getValue(other).sameBinding);assertFalse(rows.getValue(other).canCheck);assertNull(rows.getValue(other).customer)
        assertTrue(rows.getValue(other).canSetAside);assertTrue(state.unfinished.isEmpty())
    }
    @Test fun digestRecoveryRetainsOriginalRequestWithoutNotification(): Unit=runBlocking {
        val op=JSONObject().put("kind","DIGEST").put("expected_heads",heads()).put("has_work",true)
        val request=pending(op);val before=context.getSystemService(NotificationManager::class.java).activeNotifications.map {it.id}
        onCommand={body->metadata++;JSONObject().put("ok",true).put("replayed",false).put("launch_authorized",false).put("notify_authorized",true)
            .put("heads",heads()).put("result",JSONObject().put("business_date",today.toString()).put("reserved",true))}
        val result=workspace.access.reviewRequest(request,false)
        assertTrue(result.accepted);assertNull(db.followUpDataDao().command(request))
        assertEquals(request,sent.single().getString("command_uid"));assertEquals(op.toString(),sent.single().getJSONObject("operation").toString())
        assertEquals(before,context.getSystemService(NotificationManager::class.java).activeNotifications.map {it.id})
    }
    @Test fun foreignRequestCannotReplayAndNeedsOwnerToSetAside(): Unit=runBlocking {
        val request=pending(acquire(),foreign=true);claimRows.put(claim(actor=id(),installation="other-device"))
        assertTrue(runCatching {workspace.access.reviewRequest(request,false)}.isFailure)
        val enrollment=security.beginUserEnrollment(session,CreateUserInput("review.manager","Manager","manager"))
        security.completeUserEnrollment(enrollment.enrollmentToken,"synthetic review password".toCharArray())
        val manager=security.login("review.manager","synthetic review password".toCharArray()).sessionId
        assertTrue(runCatching {bound(manager).access.reviewRequest(request,true)}.isFailure)
        assertNotNull(db.followUpDataDao().command(request));assertTrue(sent.isEmpty())
        val result=workspace.access.reviewRequest(request,true)
        assertTrue(result.accepted);assertNull(db.followUpDataDao().command(request));assertTrue(sent.isEmpty())
        assertEquals("HANDOFF_STARTED",result.snapshot.claims.single().phase)
    }
    @Test fun sharedClaimReviewUsesExistingCustomerRoute(): Unit=runBlocking {
        claimRows.put(claim(actor=id(),installation="other-device"))
        val state=workspace.load();val current=state.claims.single()
        assertEquals(customer,current.accountId);assertTrue(current.canReview);assertEquals("HANDOFF_STARTED",current.phase)
        claimRows=JSONArray().put(claim(started=false).put("lease_until",Instant.parse(instant).minusSeconds(1).toString()))
        assertTrue(workspace.load().claims.isEmpty());assertTrue(sent.isEmpty())
    }
    @Test fun scheduleForSettledCustomerChangesNoFinancialTerms(): Unit=runBlocking {
        val cash=db.backupDataDao().accounts().single {it.name=="Cash"}.accountId
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,200.0,today.toString()));db.syncOutboxDao().deleteAll()
        val logs=db.backupDataDao().financialLogs();val before=workspace.load();val current=before.customers.single()
        assertEquals(0L,current.balancePaisa)
        assertNull(CustomerFollowUps.evaluate(current,before.policy,null,FollowUpAcknowledgment(),today))
        val result=workspace.access.command(FollowUpCommand.Preference(uid,CustomerFollowUpPreference("shop",today,StatementReviewSchedule.Monthly(15),true)),before.saveToken)
        assertTrue(result.accepted);assertEquals(StatementReviewSchedule.Monthly(15),result.snapshot.preferences[uid]!!.schedule)
        assertTrue(result.snapshot.preferences[uid]!!.paused);assertEquals(logs,db.backupDataDao().financialLogs())
        assertEquals("PREFERENCE",sent.single().getJSONObject("operation").getString("kind"))
    }
}
