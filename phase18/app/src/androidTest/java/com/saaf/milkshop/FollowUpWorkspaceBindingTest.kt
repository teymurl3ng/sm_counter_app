package com.saaf.milkshop

import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import com.saaf.milkshop.nativeui.owner.*
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.time.*
import java.util.UUID

/** Real Room reader/runtime/journal with an injected transport; never a remote shop. */
class FollowUpWorkspaceBindingTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var session: String
    private lateinit var runtime: SyncRuntime
    private lateinit var workspace: FollowUpWorkspaceBinding
    private var customer=0L
    private lateinit var uid: String
    private val shop="20000000-0000-4000-8000-000000000001"
    private val staff="20000000-0000-4000-8000-000000000002"
    private var edit="20000000-0000-4000-8000-000000000003"
    private val schedule="20000000-0000-4000-8000-000000000004"
    private val today=LocalDate.parse("2026-10-18")
    private val instant="2026-10-18T07:00:00Z"
    private var metadata=1L
    private var repeat=7
    private var primary: String?=null
    private var customerState: JSONObject?=null
    private var offline=false
    private var revoke=false
    private var afterSnapshot: suspend ()->Unit={}
    private val sent=mutableListOf<JSONObject>()
    private var onCommand: (JSONObject)->JSONObject={request->accepted(request)}
    private fun id()=UUID.randomUUID().toString()
    private fun heads()=JSONObject().put("sync",10L).put("credit",4L).put("metadata",metadata)
        .put("policy_revision",edit).put("business_date",today.toString())
    private fun policy()=JSONObject().put("revision",edit).put("schedule_revision",schedule).put("effective_from","2026-10-16")
        .put("enabled",true).put("weekday",7).put("repeat_days",repeat).put("preferred_time","10:00:00").put("quiet_start","20:00:00").put("quiet_end","09:00:00")
        .put("primary_installation",primary ?: "another-device").put("primary_actor",staff).put("updated_by",staff).put("updated_at",instant)
    private fun snapshot()=JSONObject().put("ok",true).put("contract_version",1).put("shop_id",shop).put("shop_timezone","Asia/Karachi")
        .put("server_time_epoch_ms",Instant.parse(instant).toEpochMilli()).put("heads",heads()).put("policy",policy())
        .put("customers",JSONArray().apply {customerState?.let {put(it)}}).put("claims",JSONArray())
        .put("actions",JSONArray().put(JSONObject().put("seq",metadata).put("command_uid",id()).put("kind","POLICY")
            .put("actor_id",staff).put("installation","another-device").put("customer_uid",JSONObject.NULL).put("attempt_uid",JSONObject.NULL)
            .put("cycle",JSONObject.NULL).put("accepted_at",instant)))
    private fun customerRow()=JSONObject().put("customer_uid",uid).put("revision",id()).put("schedule_revision",JSONObject.NULL)
        .put("effective_from",JSONObject.NULL).put("schedule",JSONObject.NULL).put("paused",false).put("snoozed_until",JSONObject.NULL)
        .put("statement_cycle",JSONObject.NULL).put("reviewed_on",JSONObject.NULL)
    private fun accepted(request: JSONObject,replay: Boolean=false): JSONObject {
        val op=request.getJSONObject("operation");val kind=op.getString("kind")
        if(!replay) metadata++
        val result=when(kind) {
            "POLICY"->{if(!replay) {edit=id();repeat=op.getJSONObject("value").getInt("repeat_days");if(op.getBoolean("make_primary")) primary=request.getString("installation")}
                JSONObject().put("revision",edit).put("schedule_revision",schedule)}
            "PREFERENCE"->{if(!replay) customerState=customerRow().put("schedule",op.getJSONObject("value").get("schedule"))
                .put("schedule_revision",id()).put("effective_from",today.toString()).put("paused",op.getJSONObject("value").getBoolean("paused"))
                JSONObject().put("revision",customerState!!.getString("revision")).put("schedule_revision",customerState!!.get("schedule_revision"))}
            else->{if(!replay) customerState=customerRow().apply {if(kind=="REVIEW") {put("statement_cycle",op.getString("cycle"));put("reviewed_on",today.toString())} else put("snoozed_until",op.getString("until"))}
                JSONObject().put("revision",customerState!!.getString("revision")).put("reviewed_on",if(kind=="REVIEW") today.toString() else JSONObject.NULL)
                    .put("snoozed_until",if(kind=="SNOOZE") op.getString("until") else JSONObject.NULL)}
        }
        return JSONObject().put("ok",true).put("replayed",replay).put("launch_authorized",false).put("notify_authorized",false).put("result",result)
            .apply {if(!replay) put("heads",heads())}
    }
    @Before fun setup(): Unit=runBlocking {
        SyncScheduler.suppressSchedulingForTest();SyncRebaselineStore(context).clear()
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db)
        session=security.bootstrapOwner("followup.binding.owner","Owner",PASSWORD.toCharArray()).session.sessionId
        customer=CustomerManagementService(db).create("Cafe");uid=db.backupDataDao().accounts().single {it.accountId==customer}.uid
        val supplier=VendorManagementService(db).create("Supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(supplier,"2026-10-01",100.0,4.0,28.0))
        db.creditMetadataDao().saveState(CreditMetadataSyncStateEntity(shopUid=shop,enabled=true,contractVersion=1,lastServerSeq=4))
        SaleService(db).record(SaleInput(db.productDao().findByName("Milk")!!.productId,"2026-10-01","credit-retail",100.0,value=200.0,payment=SalePayment.OnCredit(customer)))
        db.creditMetadataDao().clearPending();db.syncOutboxDao().deleteAll()
        db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq=10,updatedAt=instant))
        SyncServerConfigStore.save(context,SyncServerConfig("dev","https://example.invalid","synthetic-public"))
        SyncShopBindingStore.save(context,SyncShopBinding("dev",id(),shop,"Fixture shop",staff,"owner"))
        SyncTokenStore(context).save(SyncTokenStore.Session("synthetic-access","synthetic-refresh",null))
        runtime=SyncRuntime(context,db,SyncHttp {request->
            if(offline) throw IOException("synthetic unavailable")
            if(revoke) SyncHttpResponse(403,"""{"code":"followup_not_authorized"}""".toByteArray())
            else if(request.url.endsWith("followup_snapshot_v1")) {val body=snapshot().toString().toByteArray();afterSnapshot();SyncHttpResponse(200,body)}
            else if(request.url.endsWith("followup_command_v1")) {val body=JSONObject(request.body!!.toString(Charsets.UTF_8));sent+=body;SyncHttpResponse(200,onCommand(body).toString().toByteArray())}
            else error("Unexpected request")
        },followUpSecurity=security)
        workspace=bound(session)
    }
    private fun bound(local: String)=FollowUpWorkspaceBinding(db,security,runtime,local)
    @After fun close() {
        SyncServerConfigStore.clear(context);SyncShopBindingStore.clear(context);SyncTokenStore(context).clear();SyncRebaselineStore(context).clear()
        TrustedTime.resetForTest(context);db.close()
    }
    @Test fun acceptedLoadUsesActualReaderAndServerSchedule(): Unit=runBlocking {
        val before=db.eventDao().count();val state=workspace.access.load()
        assertTrue(state.online);assertTrue(state.reviewReady);assertTrue(state.owner)
        assertEquals(schedule,state.policy.revision);assertEquals("Another device",state.primaryDevice)
        assertEquals(20000L,state.customers.single().balancePaisa);assertEquals(20000L,state.customers.single().status!!.overduePaisa)
        assertEquals(before,db.eventDao().count());assertTrue(sent.isEmpty())
    }
    @Test fun policyAndPrimaryRequireExplicitOwnerConfirmation(): Unit=runBlocking {
        val state=workspace.access.load();val changed=state.policy.copy(repeatDays=10)
        assertTrue(runCatching {workspace.access.command(FollowUpCommand.Policy(changed),state.saveToken)}.isFailure)
        assertTrue(sent.isEmpty())
        val result=workspace.access.command(FollowUpCommand.Policy(changed,true,true),state.saveToken)
        assertTrue(result.accepted);assertEquals(10,result.snapshot.policy.repeatDays);assertEquals("This device",result.snapshot.primaryDevice)
        assertEquals("20000000-0000-4000-8000-000000000003",sent.single().getJSONObject("operation").getString("expected_revision"))
        val e=security.beginUserEnrollment(session,CreateUserInput("followup.binding.manager","Manager","manager"));security.completeUserEnrollment(e.enrollmentToken,PASSWORD.toCharArray())
        val manager=security.login("followup.binding.manager",PASSWORD.toCharArray()).sessionId;val view=bound(manager);val m=view.access.load()
        assertFalse(m.owner);assertTrue(runCatching {view.access.command(FollowUpCommand.Policy(m.policy,true,true),m.saveToken)}.isFailure);assertEquals(1,sent.size)
    }
    @Test fun customerCommandsKeepFrozenRevisionsAndFinancialRecords(): Unit=runBlocking {
        val logs=db.backupDataDao().financialLogs();var state=workspace.access.load()
        val preference=CustomerFollowUpPreference("shop",today,StatementReviewSchedule.Monthly(0))
        val saved=workspace.access.command(FollowUpCommand.Preference(uid,preference),state.saveToken)
        assertTrue(saved.accepted);assertEquals(StatementReviewSchedule.Monthly(0),saved.snapshot.preferences[uid]!!.schedule)
        assertTrue(sent.single().getJSONObject("operation").isNull("expected_customer_revision"))
        state=saved.snapshot
        val snoozed=workspace.access.command(FollowUpCommand.Snooze(uid,today.plusDays(1)),state.saveToken)
        assertTrue(snoozed.accepted);assertEquals(today.plusDays(1),snoozed.snapshot.preferences[uid]!!.snoozedUntil)
        customerState=null;state=workspace.access.load()
        val row=CustomerFollowUps.evaluate(state.customers.single(),state.policy,null,FollowUpAcknowledgment(),today)!!
        val reviewed=workspace.access.command(FollowUpCommand.Reviewed(uid,row.statementCycle,today),state.saveToken)
        assertTrue(reviewed.accepted);assertEquals(today,reviewed.snapshot.acknowledgments[uid]!!.reviewedOn)
        assertEquals(logs,db.backupDataDao().financialLogs());assertTrue(db.followUpDataDao().commands().isEmpty())
    }
    @Test fun staleViewOrLocalFinancialChangesCannotSubmitReview(): Unit=runBlocking {
        val old=workspace.access.load();val current=workspace.access.load()
        assertTrue(runCatching {workspace.access.command(FollowUpCommand.Snooze(uid,today.plusDays(1)),old.saveToken)}.isFailure)
        val cash=db.backupDataDao().accounts().single {it.name=="Cash"}.accountId
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,200.0,today.toString()))
        assertTrue(runCatching {workspace.access.command(FollowUpCommand.Snooze(uid,today.plusDays(1)),current.saveToken)}.isFailure)
        assertTrue(sent.isEmpty());assertFalse(workspace.access.load().reviewReady)
    }
    @Test fun uncertainSaveRecoversOriginalIdAndNeverDuplicates(): Unit=runBlocking {
        val state=workspace.access.load()
        onCommand={accepted(it);throw IOException("lost after acceptance")}
        val uncertain=workspace.access.command(FollowUpCommand.Snooze(uid,today.plusDays(1)),state.saveToken)
        assertFalse(uncertain.accepted);assertEquals(1,uncertain.snapshot.unfinished.size)
        assertTrue(runCatching {workspace.access.command(FollowUpCommand.Snooze(uid,today.plusDays(2)),uncertain.snapshot.saveToken)}.isFailure)
        onCommand={accepted(it,true)}
        val recovered=workspace.access.recover(uncertain.snapshot.unfinished.single().uid,false)
        assertTrue(recovered.accepted);assertTrue(recovered.snapshot.unfinished.isEmpty())
        assertEquals(2,sent.size);assertEquals(sent.first().toString(),sent.last().toString());assertEquals(2L,metadata)
    }
    @Test fun abandonedMetadataDoesNotUndoRemoteStateOrHandleExternalRequests(): Unit=runBlocking {
        val state=workspace.access.load();onCommand={accepted(it);throw IOException("synthetic lost")}
        val failed=workspace.access.command(FollowUpCommand.Snooze(uid,today.plusDays(1)),state.saveToken)
        val setAside=workspace.access.recover(failed.snapshot.unfinished.single().uid,true)
        assertTrue(setAside.accepted);assertEquals(today.plusDays(1),setAside.snapshot.preferences[uid]!!.snoozedUntil);assertEquals(1,sent.size)
        val b=FollowUpBinding("dev","https://example.invalid",shop,staff,SyncIdentity.warmUp(context));val attempt=id();val command=id()
        val start=JSONObject().put("kind","START").put("customer_uid",uid).put("expected_customer_revision",JSONObject.NULL)
            .put("expected_heads",heads()).put("attempt_uid",attempt).put("channel","SMS")
        db.followUpDataDao().insertCommand(FollowUpPendingEntity(command,b.key,start.toString(),instant))
        assertTrue(runCatching {workspace.access.recover(command,true)}.isFailure);assertNotNull(db.followUpDataDao().command(command))
    }
    @Test fun bindingChangeAndLogoutCannotPublishOrRetarget(): Unit=runBlocking {
        val state=workspace.access.load();val original=SyncShopBindingStore.load(context)!!
        SyncShopBindingStore.save(context,original.copy(staffId=id()))
        assertTrue(runCatching {workspace.access.command(FollowUpCommand.Snooze(uid,today.plusDays(1)),state.saveToken)}.isFailure)
        assertTrue(sent.isEmpty());SyncShopBindingStore.save(context,original)
        afterSnapshot={security.logout(session)}
        assertTrue(runCatching {workspace.access.load()}.isFailure)
    }
    @Test fun unavailableDataStaysStaleAndCannotSave(): Unit=runBlocking {
        workspace.access.load();offline=true
        val cached=workspace.access.load();assertFalse(cached.online);assertFalse(cached.reviewReady)
        assertTrue(runCatching {workspace.access.command(FollowUpCommand.Policy(cached.policy,true,true),cached.saveToken)}.isFailure)
        offline=false;revoke=true
        assertTrue(runCatching {workspace.access.load()}.isFailure);assertTrue(sent.isEmpty())
    }
    companion object {const val PASSWORD="synthetic followup binding password"}
}
