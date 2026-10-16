package com.saaf.milkshop

import android.content.ActivityNotFoundException
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.util.UUID

/** Real local authorization/Room/journal/runtime, synthetic RPC responses; no hosted concurrency claim. */
open class FollowUpHandoffTest {
    protected val context=InstrumentationRegistry.getInstrumentation().targetContext
    protected lateinit var db: MilkShopDatabase
    protected lateinit var security: UserSecurityService
    protected lateinit var session: String
    protected lateinit var runtime: SyncRuntime
    private lateinit var access: CustomerSharedHandoffAccess
    private lateinit var uid: String
    protected var customer=0L
    private val shop="40000000-0000-4000-8000-000000000001"
    private val staff="40000000-0000-4000-8000-000000000002"
    private val revision="40000000-0000-4000-8000-000000000003"
    private val customerRevision="40000000-0000-4000-8000-000000000004"
    private val now=Instant.parse("2026-10-18T07:00:00Z")
    private val day=LocalDate.parse("2026-10-18")
    protected var snapshotUnavailable=false
    private var metadata=1L
    private var paused=false
    protected var enabled=true
    private var remoteCredit=4L
    private var claim: JSONObject?=null
    protected var lose: String?=null
    private var refuse: String?=null
    private var launches=0
    private val sent=mutableListOf<JSONObject>()
    private val receipts=mutableMapOf<String,JSONObject>()
    private var afterAccepted: suspend (String)->Unit={}
    private val installation get()=SyncIdentity.warmUp(context)
    private fun id()=UUID.randomUUID().toString()
    private fun heads()=JSONObject().put("sync",10).put("credit",remoteCredit).put("metadata",metadata)
        .put("policy_revision",revision).put("business_date",day.toString())
    private fun snapshot()=JSONObject().put("ok",true).put("contract_version",1).put("shop_id",shop)
        .put("shop_timezone","Asia/Karachi").put("server_time_epoch_ms",now.toEpochMilli()).put("heads",heads())
        .put("policy",JSONObject().put("revision",revision).put("schedule_revision",revision).put("effective_from","2026-10-16")
            .put("enabled",enabled).put("weekday",7).put("repeat_days",7).put("preferred_time","10:00:00").put("quiet_start","20:00:00")
            .put("quiet_end","09:00:00").put("primary_installation",installation).put("primary_actor",staff).put("updated_by",staff).put("updated_at",now.toString()))
        .put("customers",JSONArray().put(JSONObject().put("customer_uid",uid).put("revision",customerRevision)
            .put("schedule_revision",JSONObject.NULL).put("effective_from",JSONObject.NULL).put("schedule",JSONObject.NULL)
            .put("paused",paused).put("snoozed_until",JSONObject.NULL).put("statement_cycle",JSONObject.NULL).put("reviewed_on",JSONObject.NULL)))
        .put("claims",JSONArray().apply {claim?.let {put(it)}})
        .put("actions",JSONArray().put(JSONObject().put("seq",metadata).put("command_uid",id()).put("kind","POLICY")
            .put("actor_id",staff).put("installation",installation).put("customer_uid",JSONObject.NULL).put("attempt_uid",JSONObject.NULL)
            .put("cycle",JSONObject.NULL).put("accepted_at",now.toString())))
    private fun preparing(attempt: String=id(),actor: String=staff,install: String=installation)=JSONObject()
        .put("customer_uid",uid).put("attempt_uid",attempt).put("actor_id",actor).put("installation",install)
        .put("cycle","fixture-cycle").put("phase","PREPARING").put("lease_until",now.plusSeconds(300).toString())
        .put("started_at",JSONObject.NULL).put("channel",JSONObject.NULL)
    private fun started(actor: String=staff,install: String=installation)=preparing(actor=actor,install=install)
        .put("phase","HANDOFF_STARTED").put("lease_until",JSONObject.NULL).put("started_at",now.minusSeconds(86400*30).toString()).put("channel","SMS")
    private suspend fun http(request: SyncHttpRequest): SyncHttpResponse {
        if(request.url.endsWith("followup_snapshot_v1")) return if(snapshotUnavailable) SyncHttpResponse(503,null) else SyncHttpResponse(200,snapshot().toString().toByteArray())
        check(request.url.endsWith("followup_command_v1")) {"Unexpected network call"}
        val body=JSONObject(request.body!!.toString(Charsets.UTF_8));sent+=JSONObject(body.toString())
        val command=body.getString("command_uid");val p=body.getJSONObject("operation");val kind=FollowUpWire.operation(p)
        if(refuse==kind) return SyncHttpResponse(409,"""{"code":"followup_claim_changed"}""".toByteArray())
        val old=receipts[command]
        if(old!=null) return SyncHttpResponse(200,JSONObject(old.toString()).apply {
            put("replayed",true);put("launch_authorized",false);remove("heads")
        }.toString().toByteArray())
        val attempt=p.getString("attempt_uid")
        val result=when(kind) {
            "ACQUIRE"->{check(claim==null);claim=preparing(attempt).put("cycle",p.getString("cycle"))
                JSONObject().put("attempt_uid",attempt).put("phase","PREPARING").put("lease_until",now.plusSeconds(300).toString())}
            "START"->{check(claim!!.getString("attempt_uid")==attempt);claim=claim!!.put("phase","HANDOFF_STARTED")
                .put("lease_until",JSONObject.NULL).put("started_at",now.toString()).put("channel",p.getString("channel"))
                JSONObject().put("attempt_uid",attempt).put("phase","HANDOFF_STARTED")}
            "RELEASE"->{check(claim!!.getString("attempt_uid")==attempt);claim=null;JSONObject().put("attempt_uid",attempt).put("phase","RELEASED")}
            "RESOLVE"->{check(claim!!.getString("attempt_uid")==attempt);claim=null;JSONObject().put("attempt_uid",attempt).put("operator_report",p.getString("outcome"))}
            else->error("Unexpected handoff command $kind")
        }
        metadata++
        val receipt=JSONObject().put("ok",true).put("replayed",false).put("launch_authorized",kind=="START")
            .put("notify_authorized",false).put("heads",heads()).put("result",result)
        receipts[command]=receipt
        afterAccepted(kind)
        if(lose==kind) {lose=null;return SyncHttpResponse(503,null)}
        return SyncHttpResponse(200,receipt.toString().toByteArray())
    }
    @Before fun setup(): Unit=runBlocking {
        assertEquals("com.saaf.milkshop.qa.tests",context.packageName)
        SyncScheduler.suppressSchedulingForTest();SyncRebaselineStore(context).clear();TrustedTime.warmUp(context)
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db)
        session=security.bootstrapOwner("handoff.owner","Owner",PASSWORD.toCharArray()).session.sessionId
        customer=CustomerManagementService(db).create("PRIVATE cafe");uid=db.accountDao().findById(customer)!!.uid
        CustomerProfileService(db).save(customer,CustomerProfileInput(phone="+923001234567",notes="PRIVATE note"))
        StoreIdentityService(db).save(StoreIdentityInput("Configured shop"))
        val supplier=VendorManagementService(db).create("Supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(supplier,"2026-10-01",100.0,4.0,28.0))
        db.creditMetadataDao().saveState(CreditMetadataSyncStateEntity(shopUid=shop,enabled=true,contractVersion=1,lastServerSeq=4))
        SaleService(db).record(SaleInput(db.productDao().findByName("Milk")!!.productId,"2026-10-01","credit-retail",100.0,value=200.0,payment=SalePayment.OnCredit(customer)))
        db.creditMetadataDao().clearPending();db.syncOutboxDao().deleteAll()
        db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq=10,updatedAt=now.toString()))
        SyncServerConfigStore.save(context,SyncServerConfig("dev","https://example.invalid","synthetic-public"))
        SyncShopBindingStore.save(context,SyncShopBinding("dev",id(),shop,"Fixture",staff,"owner"))
        SyncTokenStore(context).save(SyncTokenStore.Session("synthetic-access","synthetic-refresh",null))
        runtime=SyncRuntime(context,db,SyncHttp(::http),followUpSecurity=security)
        access=CustomerSharedHandoff(db,security,runtime,session,sync={}).access
    }
    @After fun close() {
        SyncServerConfigStore.clear(context);SyncShopBindingStore.clear(context);SyncTokenStore(context).clear()
        SyncRebaselineStore(context).clear();TrustedTime.resetForTest(context);db.close()
    }
    private suspend fun reminder()=CustomerCommunicationService(db,security).reminder(session,customer,day)
    protected suspend fun open(channel: String="SMS",facts: CustomerReminder?=null,launch: suspend ()->Unit={launches++})=
        access.open(customer,facts ?: reminder(),"+923001234567",channel,launch)
    private fun kinds()=sent.map {it.getJSONObject("operation").getString("kind")}
    @Test fun currentStartSingleLaunchNoFinancialWrites(): Unit=runBlocking {
        val ledger=db.backupDataDao().financialLogs();val events=db.eventDao().count()
        for(channel in listOf("SMS","WhatsApp")) {
            val state=open(channel);assertEquals("HANDOFF_STARTED",state.phase);assertTrue(state.canResolve)
            assertTrue(runCatching {open(channel)}.isFailure)
            access.resolve(customer,state.attemptUid!!,"REVIEWED",true)
        }
        assertEquals(2,launches);assertEquals(listOf("ACQUIRE","START","RESOLVE","ACQUIRE","START","RESOLVE"),kinds())
        assertEquals(listOf("SMS","WHATSAPP"),sent.filter {it.getJSONObject("operation").getString("kind")=="START"}.map {it.getJSONObject("operation").getString("channel")})
        assertEquals(ledger,db.backupDataDao().financialLogs());assertEquals(events,db.eventDao().count())
        assertTrue(db.followUpDataDao().commands().isEmpty());assertFalse(sent.toString().contains("1234567"));assertFalse(sent.toString().contains("PRIVATE"))
    }
    @Test fun changedFactsStaleHeadsOrPauseNeverLaunch(): Unit=runBlocking {
        paused=true;assertTrue(runCatching {open()}.isFailure);paused=false
        remoteCredit=5;assertTrue(runCatching {open()}.isFailure);remoteCredit=4
        // Cache heads cannot regress: drop only synthetic cached response between cases.
        db.followUpDataDao().dropCache(FollowUpBinding("dev","https://example.invalid",shop,staff,installation).key)
        val reviewed=reminder();CustomerProfileService(db).save(customer,CustomerProfileInput(phone="+923009876543"))
        assertTrue(runCatching {open(facts=reviewed)}.isFailure);assertTrue(sent.isEmpty())
        // Treat that profile as caught up before separately testing a mutation after ACQUIRE.
        db.syncOutboxDao().deleteAll()
        afterAccepted={kind->if(kind=="ACQUIRE") CustomerProfileService(db).save(customer,CustomerProfileInput(phone="+923001234567"))}
        assertTrue(runCatching {open()}.isFailure);assertEquals(listOf("ACQUIRE"),kinds());assertEquals(0,launches)
        val state=access.load(customer);assertTrue(state.canRelease);access.release(customer,state.attemptUid!!)
        assertNull(claim)
    }
    @Test fun busyForeignClaimNeverLaunch(): Unit=runBlocking {
        claim=preparing(actor=id(),install="other-phone");var state=access.load(customer)
        assertFalse(state.canRelease);assertFalse(state.canResolve);assertTrue(runCatching {open()}.isFailure)
        claim=started(actor=id(),install="other-phone");state=access.load(customer)
        assertTrue(state.canResolve);assertTrue(runCatching {open()}.isFailure)
        assertEquals(0,launches);assertTrue(sent.isEmpty())
    }
    @Test fun lostAcquireOrStartNeverReplays(): Unit=runBlocking {
        for(kind in listOf("ACQUIRE","START")) {
            lose=kind;assertTrue(runCatching {open()}.isFailure);assertEquals(0,launches)
            val row=db.followUpDataDao().commands().single();val original=sent.last().toString()
            assertTrue(runCatching {open()}.isFailure)
            val state=access.recover(customer,row.commandUid)
            assertEquals(original,sent.last().toString());assertTrue(db.followUpDataDao().commands().isEmpty());assertEquals(0,launches)
            if(kind=="ACQUIRE") access.release(customer,state.attemptUid!!) else access.resolve(customer,state.attemptUid!!,"RETRY_UNCERTAIN",true)
        }
        refuse="START";assertTrue(runCatching {open()}.isFailure)
        val pending=db.followUpDataDao().commands().single()
        val unresolved=access.recover(customer,pending.commandUid);assertEquals(1,unresolved.pending.size)
        assertTrue(runCatching {access.setAside(customer,pending.commandUid,false)}.isFailure)
        val state=access.setAside(customer,pending.commandUid,true)
        assertTrue(state.pending.isEmpty());assertTrue(state.canRelease);assertNotNull(state.blockedReason)
        access.release(customer,state.attemptUid!!);assertEquals(0,launches)
    }
    @Test fun restoredRequestsStayOriginalAndRecoveryNeverLaunches(): Unit=runBlocking {
        lose="START";assertTrue(runCatching {open()}.isFailure)
        val row=db.followUpDataDao().commands().single();db.followUpDataDao().complete(row.commandUid)
        db.followUpDataDao().insertCommand(row.copy(restored=true))
        val original=sent.last().toString()
        val state=access.recover(customer,row.commandUid);assertEquals(original,sent.last().toString());assertEquals(0,launches)
        assertTrue(state.canResolve);assertEquals("HANDOFF_STARTED",state.phase)
        // A restored journal tied to another installation is never silently retargeted.
        db.followUpDataDao().insertCommand(row.copy(commandUid=id(),bindingKey="other-binding",restored=true))
        val other=db.followUpDataDao().commands().single();val calls=sent.size
        assertTrue(runCatching {access.recover(customer,other.commandUid)}.isFailure);assertEquals(calls,sent.size)
        assertTrue(runCatching {access.setAside(customer,other.commandUid,true)}.isFailure)
        assertNotNull(db.followUpDataDao().command(other.commandUid))
    }
    @Test fun cancellationOrFailedHandlerLeavesStartedUncertainty(): Unit=runBlocking {
        assertTrue(runCatching {open(launch={throw ActivityNotFoundException("fixture")})}.exceptionOrNull() is ActivityNotFoundException)
        var state=access.load(customer);assertEquals("HANDOFF_STARTED",state.phase);assertTrue(state.canResolve)
        access.resolve(customer,state.attemptUid!!,"REPORTED_NOT_OPENED",true)
        afterAccepted={if(it=="START") throw CancellationException("fixture interrupted before response")}
        assertTrue(runCatching {open()}.exceptionOrNull() is CancellationException);afterAccepted={}
        state=access.load(customer);assertEquals("HANDOFF_STARTED",state.phase);assertEquals(1,state.pending.size)
        access.recover(customer,state.pending.single().uid);assertEquals(0,launches)
        assertTrue(runCatching {open()}.isFailure)
    }
    @Test fun explicitResolutionPinsAttemptRoleAndRevisions(): Unit=runBlocking {
        claim=started(actor=id(),install="other-phone");val attempt=claim!!.getString("attempt_uid")
        assertTrue(runCatching {access.resolve(customer,id(),"REVIEWED",true)}.isFailure)
        assertTrue(runCatching {access.resolve(customer,attempt,"REVIEWED",false)}.isFailure)
        val enrollment=security.beginUserEnrollment(session,CreateUserInput("handoff.manager","Manager","manager"))
        security.completeUserEnrollment(enrollment.enrollmentToken,PASSWORD.toCharArray())
        val manager=security.login("handoff.manager",PASSWORD.toCharArray()).sessionId
        val limited=CustomerSharedHandoff(db,security,runtime,manager,sync={}).access
        assertFalse(limited.load(customer).canResolve)
        assertTrue(runCatching {limited.resolve(customer,attempt,"REVIEWED",true)}.isFailure)
        assertTrue(sent.isEmpty());enabled=false // Disabling new handoffs must not strand a started one.
        access.resolve(customer,attempt,"RETRY_UNCERTAIN",true)
        val op=sent.single().getJSONObject("operation")
        assertEquals(attempt,op.getString("attempt_uid"));assertEquals(customerRevision,op.getString("expected_customer_revision"))
        assertEquals(4,op.getJSONObject("expected_heads").getInt("credit"));assertTrue(op.getBoolean("acknowledge_uncertainty"))
        assertTrue(runCatching {open()}.isFailure);assertEquals(0,launches)
    }
    @Test fun expiredSessionAndBindingChangeCannotLaunch(): Unit=runBlocking {
        afterAccepted={if(it=="START") SyncShopBindingStore.save(context,SyncShopBindingStore.load(context)!!.copy(staffId=id()))}
        assertTrue(runCatching {open()}.isFailure);assertEquals("HANDOFF_STARTED",claim!!.getString("phase"));assertEquals(0,launches)
        SyncShopBindingStore.save(context,SyncShopBindingStore.load(context)!!.copy(staffId=staff));claim=null;afterAccepted={if(it=="START") security.logout(session)}
        assertTrue(runCatching {open()}.isFailure);assertEquals(0,launches)
        val calls=sent.size;assertTrue(runCatching {access.load(customer)}.isFailure);assertEquals(calls,sent.size)
    }
    companion object {const val PASSWORD="synthetic handoff owner phrase"}
}
