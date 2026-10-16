package com.saaf.milkshop

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room3.withWriteTransaction
import androidx.room3.executeSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import com.saaf.milkshop.application.owner.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate

/** Disposable real-HTTPS story; never opens the ordinary app's database/config. */
class PhysicalFollowUpProbe {
    @Test fun runStage() = runBlocking {
        val base=InstrumentationRegistry.getInstrumentation().targetContext
        check(base.packageName=="com.saaf.milkshop.qa.tests")
        val args=InstrumentationRegistry.getArguments()
        val run=args.getString("qaRun")!!; check(run.matches(Regex("[a-f0-9]{12}")))
        val phone=args.getString("qaPhone")!!; check(phone in setOf("owner","counter"))
        val stage=args.getString("qaStage")!!
        val root=File(base.filesDir,"followup-phone-$run").apply { mkdirs() }
        var context=FixtureContext(base,root)
        SyncScheduler.suppressSchedulingForTest(); SyncIdentity.warmUp(context)
        var db=MilkShopDatabase.create(context)
        var security=UserSecurityService(db)
        val result=JSONObject().put("run",run).put("phone",phone).put("stage",stage)
        val calls=mutableListOf<String>()
        val real=UrlConnectionSyncHttp()
        val http=SyncHttp { request ->
            check(request.url.startsWith(DEV+"/"))
            val sentAt=System.currentTimeMillis()
            val response=real.execute(request)
            val receivedAt=System.currentTimeMillis()
            if(request.url.endsWith("followup_command_v1")) {
                val kind=JSONObject(String(request.body!!)).getJSONObject("operation").getString("kind")
                File(root,"last-http.json").writeText(JSONObject().put("kind",kind).put("started",sentAt).put("finished",receivedAt).toString())
                if(stage=="start_lost"&&kind=="START") {check(response.status in 200..299);throw java.io.IOException("Deliberate lost START acknowledgment")}
            }
            calls+=request.url.substringAfter(DEV)+":"+response.status
            response
        }
        val runtime=SyncRuntime(context,db,http,security)
        val day=LocalDate.parse(args.getString("qaDay")!!)
        suspend fun owner()=security.login("credit.owner",OWNER.toCharArray()).sessionId
        suspend fun customer()=db.accountDao().findByCategory("Customer").single()
        suspend fun snapshot(a: FollowUpSessionAccess): FollowUpRemoteSnapshot {
            val loaded=FollowUpDataStore(db).refresh(a.binding,a.call)
            check(loaded is FollowUpLoad.Loaded) {"Live snapshot unavailable: $loaded"};return loaded.snapshot
        }
        fun heads(r: FollowUpRemoteSnapshot)=JSONObject().put("sync",r.heads.sync).put("credit",r.heads.credit)
            .put("metadata",r.heads.metadata).put("policy_revision",r.heads.policyRevision ?: JSONObject.NULL).put("business_date",r.heads.date.toString())
        suspend fun command(a: FollowUpSessionAccess,p: JSONObject)=FollowUpDataStore(db).execute(a.binding,java.util.UUID.randomUUID().toString(),p,a.call)
        suspend fun claimOperation(kind: String)=runtime.withFollowUpSession(owner()) {a->
            val r=snapshot(a);val c=customer();val claim=r.claims.single {it.customerUid==c.uid}
            val p=JSONObject().put("kind",kind).put("customer_uid",c.uid).put("attempt_uid",claim.attemptUid)
            if(kind=="RESOLVE") p.put("outcome","RETRY_UNCERTAIN").put("acknowledge_uncertainty",true)
                .put("expected_heads",heads(r)).put("expected_customer_revision",r.customerRevisions[c.uid] ?: JSONObject.NULL)
            check(command(a,p) is FollowUpCommandOutcome.Accepted)
        }
        var launches=0
        suspend fun handoff()=CustomerSharedHandoff(db,security,runtime,owner()).access
        suspend fun facts()=CustomerCommunicationService(db,security).reminder(owner(),customer().accountId)
        try {
            when(stage) {
                "init" -> {
                    assertEquals(0L,db.appUserDao().count())
                    if(phone=="counter") db.withWriteTransaction {
                        listOf("accounts","products","events","app_users").forEach {
                            executeSQL("DELETE FROM sqlite_sequence WHERE name='$it'")
                            executeSQL("INSERT INTO sqlite_sequence(name,seq) VALUES('$it',1000)")
                        }
                    }
                    val session=security.bootstrapOwner("credit.owner","Credit QA Owner",OWNER.toCharArray()).session.sessionId
                    val enrollment=security.beginUserEnrollment(session,CreateUserInput("credit.cashier","Credit QA Cashier","cashier"))
                    security.completeUserEnrollment(enrollment.enrollmentToken,CASHIER.toCharArray())
                    if(phone=="owner") {
                        StoreIdentityService(db).save(StoreIdentityInput("Credit QA Shop"))
                        CustomerManagementService(db).create("Credit QA Customer")
                        val vendor=VendorManagementService(db).create("Credit QA Supplier")
                        val cash=db.accountDao().findByCategory("Cash/Bank").first { it.name=="Cash" }
                        OpeningStateService(db).record(OpeningStateInput(day.toString(),accountBalances=listOf(OpeningAccountBalance(cash.accountId,10000.0))))
                        VendorPurchaseService(db).record(VendorPurchaseInput(vendor,day.toString(),100.0,4.0,28.0))
                        SaleService(db).record(SaleInput(db.productDao().findByName("Milk")!!.productId,day.toString(),"credit-retail",100.0,value=100.0,payment=SalePayment.OnCredit(customer().accountId)))
                    }
                }
                "enroll" -> {
                    val inputFile=File(root,"input.json");val input=JSONObject(inputFile.readText());inputFile.delete()
                    check(input.getString("baseUrl")==DEV)
                    val local=security.login("credit.owner",OWNER.toCharArray())
                    val enrolled=SyncEnrollment(context,http,db).enroll(SyncServerConfig("dev",DEV,input.getString("publishableKey")),
                        input.getString("username"),input.getString("credential").toCharArray(),input.getString("shopId"),if(phone=="owner") local.userId else null)
                    assertTrue(enrolled is SyncEnrollment.Outcome.Enrolled)
                }
                "exchange" -> {
                    // Pull always runs even when metadata waits on a global dependency.
                    val drain=runtime.drainOnce(); result.put("drain",drain.toString())
                    assertFalse(drain is SyncPushEngine.DrainOutcome.Halted)
                    assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
                    if(phone=="counter") {
                        for(hold in db.syncRebaselineHoldDao().all()) {
                            check(hold.tableName in setOf("products","accounts","settings"))
                            NativeOwnerSyncFacade(context,db).resolveHold(hold.tableName,hold.rowUid,false)
                        }
                        assertTrue(runtime.pullOnce() is SyncRuntime.SyncPullSummary.Done)
                    }
                    assertTrue(runtime.drainOnce() is SyncPushEngine.DrainOutcome.Drained)
                    assertEquals(0L,db.syncOutboxDao().count()); assertEquals(0,db.syncParkedRowDao().count())
                    assertEquals(0,db.syncRebaselineHoldDao().count())
                }
                "activate" -> { check(phone=="owner"); runtime.enableCreditContract(); assertTrue(db.creditMetadataDao().state()!!.enabled) }
                "policy" , "transfer" -> {
                    val local=owner();val binding=com.saaf.milkshop.nativeui.owner.FollowUpWorkspaceBinding(db,security,runtime,local)
                    val before=binding.load()
                    val time=java.time.Instant.now().atZone(java.time.ZoneId.of("Asia/Karachi")).toLocalTime().withSecond(0).withNano(0)
                    val policy=before.policy.copy(enabled=true,statementSchedule=StatementReviewSchedule.Weekly(day.dayOfWeek),
                        preferredTime=java.time.LocalTime.MIDNIGHT,quietStart=time.plusHours(1),quietEnd=time.plusHours(2))
                    val saved=binding.access.command(com.saaf.milkshop.nativeui.owner.FollowUpCommand.Policy(policy,true,true),before.saveToken)
                    assertTrue("Policy was not accepted",saved.accepted);assertEquals("This device",saved.snapshot.primaryDevice)
                }
                "schedule" -> {
                    val binding=com.saaf.milkshop.nativeui.owner.FollowUpWorkspaceBinding(db,security,runtime,owner());val before=binding.load()
                    val saved=binding.access.command(com.saaf.milkshop.nativeui.owner.FollowUpCommand.Preference(customer().uid,
                        CustomerFollowUpPreference("new",day,schedule=StatementReviewSchedule.Monthly(15))),before.saveToken)
                    assertTrue(saved.accepted)
                }
                "verify_schedule" -> {
                    val binding=com.saaf.milkshop.nativeui.owner.FollowUpWorkspaceBinding(db,security,runtime,owner());val current=binding.load()
                    assertEquals(StatementReviewSchedule.Monthly(15),current.preferences[customer().uid]!!.schedule)
                    assertTrue(current.reviewReady)
                }
                "pause", "resume" -> {
                    val binding=com.saaf.milkshop.nativeui.owner.FollowUpWorkspaceBinding(db,security,runtime,owner());val current=binding.load()
                    val pref=current.preferences[customer().uid]!!.copy(paused=stage=="pause")
                    assertTrue(binding.access.command(com.saaf.milkshop.nativeui.owner.FollowUpCommand.Preference(customer().uid,pref),current.saveToken).accepted)
                }
                "verify_paused" -> {
                    val state=handoff().load(customer().accountId);assertTrue(state.blockedReason!!.contains("paused"))
                    assertTrue(runCatching {handoff().open(customer().accountId,facts(),"+923001234567","SMS") {launches++}}.isFailure)
                    assertEquals(0,launches)
                }
                "race" -> runtime.withFollowUpSession(owner()) {a->
                    val r=snapshot(a);val c=customer();val attempt=java.util.UUID.randomUUID().toString()
                    val p=JSONObject().put("kind","ACQUIRE").put("customer_uid",c.uid).put("expected_heads",heads(r))
                        .put("expected_customer_revision",r.customerRevisions[c.uid] ?: JSONObject.NULL).put("attempt_uid",attempt).put("cycle","manual:$day")
                    val target=args.getString("qaStartAt")!!.toLong()
                    check(System.currentTimeMillis()<target) {"Race barrier arrived too late"}
                    check(r.heads.date==java.time.Instant.ofEpochMilli(target).atZone(java.time.ZoneId.of("Asia/Karachi")).toLocalDate()) {"Race barrier crosses the shop date; reprepare fresh facts"}
                    while(System.currentTimeMillis()<target) kotlinx.coroutines.delay(10)
                    val outcome=command(a,p)
                    result.put("raceAccepted",outcome is FollowUpCommandOutcome.Accepted).put("attempt",attempt)
                    if(outcome is FollowUpCommandOutcome.NeedsReview) {result.put("refusal",outcome.code);db.followUpDataDao().complete(outcome.commandUid)}
                    File(root,"race-result.json").writeText(result.toString(2))
                    val after=snapshot(a);assertEquals("Race outcome: $outcome",1,after.claims.count {it.customerUid==c.uid&&it.phase=="PREPARING"})
                }
                "release" -> claimOperation("RELEASE")
                "start_lost" -> {
                    assertTrue(runCatching {handoff().open(customer().accountId,facts(),"+923001234567","SMS") {launches++}}.isFailure)
                    assertEquals(0,launches);assertEquals("START",JSONObject(db.followUpDataDao().commands().single().operationJson).getString("kind"))
                }
                "recover" -> {
                    val pending=db.followUpDataDao().commands().single();val state=handoff().recover(customer().accountId,pending.commandUid)
                    assertEquals("HANDOFF_STARTED",state.phase);assertTrue(db.followUpDataDao().commands().isEmpty());assertEquals(0,launches)
                }
                "blocked" -> {
                    val state=handoff().load(customer().accountId);assertEquals("HANDOFF_STARTED",state.phase)
                    assertTrue(runCatching {handoff().open(customer().accountId,facts(),"+923001234567","SMS") {launches++}}.isFailure)
                    assertEquals(0,launches)
                }
                "resolve" -> claimOperation("RESOLVE")
                "open" -> {
                    val state=handoff().open(customer().accountId,facts(),"+923001234567","SMS") {launches++}
                    assertEquals("HANDOFF_STARTED",state.phase);assertEquals(1,launches)
                }
                "digest" -> runtime.withFollowUpSession(owner()) {a->
                    val r=snapshot(a);val p=JSONObject().put("kind","DIGEST").put("expected_heads",heads(r)).put("has_work",true)
                    val uid=java.util.UUID.randomUUID().toString();val first=FollowUpDataStore(db).execute(a.binding,uid,p,a.call)
                    assertTrue(first is FollowUpCommandOutcome.Accepted);first as FollowUpCommandOutcome.Accepted
                    result.put("notifyGrant",first.grant.name)
                    val replay=FollowUpDataStore(db).execute(a.binding,uid,p,a.call)
                    assertTrue(replay is FollowUpCommandOutcome.Accepted);assertEquals(FollowUpLiveGrant.NONE,(replay as FollowUpCommandOutcome.Accepted).grant)
                }
                "status" -> runtime.withFollowUpSession(owner()) {a->
                    val r=snapshot(a);result.put("sharedHeads",heads(r)).put("claims",r.claims.size).put("actions",r.actions.size)
                        .put("primary",r.primaryInstallation).put("customerUid",customer().uid)
                }
                "cleanup" -> {SyncEnrollment(context,http,db).unenroll();File(root,"input.json").delete()}
                else -> error("Unknown follow-up stage")
            }
            result.put("httpStatuses",JSONArray(calls)).put("launches",launches).put("pid",android.os.Process.myPid())
                .put("deviceId",SyncIdentity.currentOrNull()).put("outbox",db.syncOutboxDao().count()).put("pending",db.followUpDataDao().commands().size)
                .put("balances",JSONObject().apply {db.readModelDao().accountBalances().forEach {put(it.name,it.balance)}})
                .put("stock",JSONObject().apply {db.readModelDao().currentStock().forEach {put(it.name,it.quantityOnHand)}})
            File(root,"last-http.json").takeIf {it.exists()}?.let {result.put("lastCommandHttp",JSONObject(it.readText()))}
            db.close();result.put("dbFilename",context.dbDir.listFiles()!!.single {it.name.endsWith(".db")}.name)
            File(root,"result.json").writeText(result.toString(2))
        } finally {db.close()}
    }
    private class FixtureContext(base: Context,root: File): ContextWrapper(base) {
        val dbDir=File(root,"db").apply { mkdirs() };private val noBackup=File(root,"no-backup").apply { mkdirs() }
        override fun getApplicationContext(): Context=this
        override fun getDatabasePath(name: String)=File(dbDir,name)
        override fun getNoBackupFilesDir()=noBackup
    }
    companion object {
        private const val DEV="https://tghzmofkkdsaasvnixps.supabase.co"
        private const val OWNER="synthetic credit owner phrase 2026"
        private const val CASHIER="synthetic credit cashier phrase 2026"
        private const val BACKUP="synthetic credit backup phrase 2026"
    }
}
