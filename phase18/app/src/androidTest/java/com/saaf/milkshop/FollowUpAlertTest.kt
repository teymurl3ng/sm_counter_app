package com.saaf.milkshop

import android.Manifest
import android.app.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.*
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.time.*
import java.util.UUID

class FollowUpAlertTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var session: String
    private lateinit var store: FollowUpAttentionStore
    private lateinit var root: File
    private lateinit var uid: String
    private var customer=0L
    private val shop="30000000-0000-4000-8000-000000000001"
    private val staff="30000000-0000-4000-8000-000000000002"
    private val revision="30000000-0000-4000-8000-000000000003"
    private var now=Instant.parse("2026-10-18T07:00:00Z")
    private val day get()=now.atZone(ZoneId.of("Asia/Karachi")).toLocalDate()
    private val binding get()=FollowUpBinding("dev","https://example.invalid",shop,staff,SyncIdentity.warmUp(context))
    private var metadata=1L
    private var enabled=true
    private var primary: String?=null
    private var actor=staff
    private var preference: JSONObject?=null
    private var failure: FollowUpRpcResult?=null
    private var digestFailure=false
    private var trusted=true
    private val commands=mutableListOf<JSONObject>()
    private val reserved=mutableSetOf<String>()
    private val sink=object: FollowUpAlertSink {
        var permit=true;var posted=0;var cancelled=0
        override fun allowed()=permit
        override fun post() {posted++}
        override fun cancel() {cancelled++}
    }
    private fun id()=UUID.randomUUID().toString()
    private fun heads()=JSONObject().put("sync",10L).put("credit",4L).put("metadata",metadata).put("policy_revision",revision).put("business_date",day.toString())
    private fun snapshot()=JSONObject().put("ok",true).put("contract_version",1).put("shop_id",shop).put("shop_timezone","Asia/Karachi")
        .put("server_time_epoch_ms",now.toEpochMilli()).put("heads",heads())
        .put("policy",JSONObject().put("revision",revision).put("schedule_revision",revision).put("effective_from","2026-10-16")
            .put("enabled",enabled).put("weekday",7).put("repeat_days",7).put("preferred_time","10:00:00").put("quiet_start","20:00:00").put("quiet_end","09:00:00")
            .put("primary_installation",primary ?: binding.installation).put("primary_actor",actor).put("updated_by",staff).put("updated_at",now.toString()))
        .put("customers",JSONArray().apply {preference?.let {put(it)}}).put("claims",JSONArray())
        .put("actions",JSONArray().put(JSONObject().put("seq",metadata).put("command_uid",id()).put("kind","POLICY")
            .put("actor_id",staff).put("installation",binding.installation).put("customer_uid",JSONObject.NULL).put("attempt_uid",JSONObject.NULL)
            .put("cycle",JSONObject.NULL).put("accepted_at",now.toString())))
    private fun pref()=JSONObject().put("customer_uid",uid).put("revision",id()).put("schedule_revision",JSONObject.NULL).put("effective_from",JSONObject.NULL)
        .put("schedule",JSONObject.NULL).put("paused",false).put("snoozed_until",JSONObject.NULL).put("statement_cycle",JSONObject.NULL).put("reviewed_on",JSONObject.NULL)
    private suspend fun call(name: String,body: JSONObject): FollowUpRpcResult {
        failure?.let {return it}
        if(name=="followup_snapshot_v1") return FollowUpRpcResult.Ok(snapshot())
        commands+=JSONObject(body.toString())
        if(digestFailure) return FollowUpRpcResult.Retry("network")
        val reservedNow=reserved.add(day.toString());metadata++
        return FollowUpRpcResult.Ok(JSONObject().put("ok",true).put("replayed",false).put("launch_authorized",false).put("notify_authorized",reservedNow)
            .put("heads",heads()).put("result",JSONObject().put("business_date",day.toString()).put("reserved",reservedNow)))
    }
    private suspend fun check()=FollowUpDetector(db,store,sink,{trusted}).check(binding,::call)
    @Before fun setup(): Unit=runBlocking {
        assertEquals("com.saaf.milkshop.qa.tests",context.packageName)
        SyncScheduler.suppressSchedulingForTest();SyncRebaselineStore(context).clear();TrustedTime.warmUp(context)
        root=File(context.cacheDir,"followup-alert-${id()}");store=FollowUpAttentionStore(context,root)
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db)
        session=security.bootstrapOwner("alert.fixture.owner","Owner","synthetic alert owner password".toCharArray()).session.sessionId
        customer=CustomerManagementService(db).create("PRIVATE CAFE NAME");uid=db.backupDataDao().accounts().single {it.accountId==customer}.uid
        val supplier=VendorManagementService(db).create("Supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(supplier,"2026-10-01",100.0,4.0,28.0))
        db.creditMetadataDao().saveState(CreditMetadataSyncStateEntity(shopUid=shop,enabled=true,contractVersion=1,lastServerSeq=4))
        SaleService(db).record(SaleInput(db.productDao().findByName("Milk")!!.productId,"2026-10-01","credit-retail",100.0,value=200.0,payment=SalePayment.OnCredit(customer)))
        db.creditMetadataDao().clearPending();db.syncOutboxDao().deleteAll();db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq=10,updatedAt=now.toString()))
        SyncServerConfigStore.save(context,SyncServerConfig("dev","https://example.invalid","synthetic-public"))
        SyncShopBindingStore.save(context,SyncShopBinding("dev",id(),shop,"Fixture",staff,"owner"))
        SyncTokenStore(context).save(SyncTokenStore.Session("synthetic-access","synthetic-refresh",null))
    }
    @After fun close() {
        SyncServerConfigStore.clear(context);SyncShopBindingStore.clear(context);SyncTokenStore(context).clear();SyncRebaselineStore(context).clear();TrustedTime.resetForTest(context)
        root.walkBottomUp().forEach {it.delete()};db.close()
    }
    @Test fun quietBoundariesAndMissedDaysUseCurrentShopDate(): Unit=runBlocking {
        now=Instant.parse("2026-10-18T04:59:59Z");check();assertEquals(0,sink.posted)
        now=Instant.parse("2026-10-18T05:00:00Z");check();assertEquals(1,sink.posted)
        now=Instant.parse("2026-10-19T15:00:00Z");check();assertEquals(1,sink.posted)
        now=Instant.parse("2026-11-08T07:00:00Z");check();assertEquals(2,sink.posted)
        assertEquals(listOf("2026-10-18","2026-11-08"),commands.map {it.getJSONObject("operation").getJSONObject("expected_heads").getString("business_date")})
    }
    @Test fun pausedSnoozedAndSettledNeverReserve(): Unit=runBlocking {
        preference=pref().put("paused",true);check();assertEquals(0,store.current(binding.key).count)
        preference=pref().put("snoozed_until",day.plusDays(1).toString());check();assertEquals(0,store.current(binding.key).count)
        preference=null
        val cash=db.backupDataDao().accounts().single {it.name=="Cash"}.accountId
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,200.0,day.toString()));db.syncOutboxDao().deleteAll()
        check();assertEquals(0,store.current(binding.key).count);assertTrue(commands.isEmpty());assertEquals(0,sink.posted)
    }
    @Test fun onlyCurrentPrimaryActorMayAlert(): Unit=runBlocking {
        primary="another-device";check();assertEquals(1,store.current(binding.key).count);assertEquals(0,sink.posted)
        primary=null;actor=id();check();assertEquals(0,sink.posted)
        actor=staff;check();assertEquals(1,sink.posted)
        enabled=false;check();assertEquals(0,store.current(binding.key).count);assertEquals(1,sink.posted)
    }
    @Test fun deniedPermissionKeepsPersistentAttention(): Unit=runBlocking {
        sink.permit=false;check();assertEquals(1,store.current(binding.key).count);assertFalse(store.current(binding.key).stale)
        assertTrue(commands.isEmpty());sink.permit=true;check();assertEquals(1,sink.posted)
    }
    @Test fun firstGrantAndRestartNeverRealert(): Unit=runBlocking {
        val ledger=db.backupDataDao().financialLogs();check();assertEquals(1,sink.posted)
        store.forgetMemoryForTest(binding.key);store=FollowUpAttentionStore(context,root)
        check();check();assertEquals(1,sink.posted);assertEquals(1,commands.size)
        assertEquals(ledger,db.backupDataDao().financialLogs());assertTrue(db.followUpDataDao().commands().isEmpty())
    }
    @Test fun unknownAndRestoredDigestNeverPost(): Unit=runBlocking {
        digestFailure=true;check();assertEquals(0,sink.posted);assertTrue(store.current(binding.key).stale)
        check();assertEquals(1,commands.size);assertEquals(0,sink.posted)
        // Simulate a restored journal with no local attempt marker. Even first acceptance is recovery-only.
        val pending=db.followUpDataDao().commands().single()
        db.followUpDataDao().complete(pending.commandUid);db.followUpDataDao().insertCommand(pending.copy(restored=true))
        store.update(binding.key) {it.copy(attemptedOn=null)};digestFailure=false
        check();assertEquals(2,commands.size);assertEquals(commands.first().toString(),commands.last().toString());assertEquals(0,sink.posted)
        assertTrue(db.followUpDataDao().commands().isEmpty())
    }
    @Test fun staleLedgerAndRevocationNeverPublishCurrentCount(): Unit=runBlocking {
        sink.permit=false;check();db.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq=9,updatedAt=now.toString()))
        assertEquals(FollowUpDetection.RETRY,check());assertEquals(1,store.current(binding.key).count);assertTrue(store.current(binding.key).stale)
        trusted=false;check();assertTrue(commands.isEmpty())
        failure=FollowUpRpcResult.Refused("followup_not_authorized");assertEquals(FollowUpDetection.STOPPED,check());assertNull(store.current(binding.key).count)
    }
    @Test fun backgroundRefreshNeedsNoLocalSessionAndCannotRetarget(): Unit=runBlocking {
        var changeBinding=false;var refreshes=0
        val runtime=SyncRuntime(context,db,SyncHttp {request->
            if(request.url.contains("/auth/v1/token")) {refreshes++;return@SyncHttp SyncHttpResponse(200,"""{"access_token":"fresh-access","refresh_token":"fresh-refresh","expires_in":3600}""".toByteArray())}
            if(request.url.endsWith("my_profile")) return@SyncHttp SyncHttpResponse(200,"""{"staff_id":"$staff","role":"owner","active":true,"business_id":"${SyncShopBindingStore.load(context)!!.businessId}","shops":[{"id":"$shop"}]}""".toByteArray())
            if(request.headers[AccountAuthClient.HEADER_AUTHORIZATION]=="Bearer synthetic-access") return@SyncHttp SyncHttpResponse(401,null)
            val json=call(if(request.url.endsWith("followup_snapshot_v1")) "followup_snapshot_v1" else "followup_command_v1",JSONObject(request.body!!.toString(Charsets.UTF_8))) as FollowUpRpcResult.Ok
            if(changeBinding&&request.url.endsWith("followup_command_v1")) SyncShopBindingStore.save(context,SyncShopBindingStore.load(context)!!.copy(staffId=id()))
            SyncHttpResponse(200,json.body.toString().toByteArray())
        },followUpSecurity=security)
        assertNull(runtime.followUpAttention(session,store).first().count)
        security.logout(session);assertTrue(runCatching {runtime.followUpAttention(session,store).first()}.isFailure)
        changeBinding=true;runtime.detectFollowUps(store,sink);assertEquals(0,sink.posted)
        assertEquals(1,commands.size);assertEquals(1,refreshes)
    }
    @Test fun attentionRestartAndFailureKeepStaleCount(): Unit=runBlocking {
        sink.permit=false;check();store.forgetMemoryForTest(binding.key);store=FollowUpAttentionStore(context,root)
        assertEquals(1,store.current(binding.key).count)
        assertTrue(store.current(binding.key).stale)
        failure=FollowUpRpcResult.Retry("network");assertEquals(FollowUpDetection.RETRY,check())
        assertTrue(store.current(binding.key).stale);assertEquals(1,store.current(binding.key).count)
        val raw=root.listFiles()!!.single().readText();assertFalse(raw.contains("PRIVATE"));assertFalse(raw.contains(uid));assertFalse(raw.contains("20000"))
    }
    @Test fun workerRetryCancellationAndScheduleConstraints(): Unit=runBlocking {
        var result=FollowUpDetection.RETRY
        var cancelled=false
        val factory=object: WorkerFactory() {override fun createWorker(c: android.content.Context,n: String,p: WorkerParameters): ListenableWorker? =
            FollowUpWorker(c,p) {if(cancelled) throw CancellationException("fixture");result}}
        fun worker()=TestListenableWorkerBuilder<FollowUpWorker>(context).setWorkerFactory(factory).build()
        assertEquals(ListenableWorker.Result.retry(),worker().doWork());result=FollowUpDetection.CHECKED
        assertEquals(ListenableWorker.Result.success(),worker().doWork());cancelled=true
        assertTrue(runCatching {worker().doWork()}.exceptionOrNull() is CancellationException)
        val spec=FollowUpScheduler.periodicRequest().workSpec
        assertEquals(java.util.concurrent.TimeUnit.HOURS.toMillis(6),spec.intervalDuration)
        assertEquals(NetworkType.CONNECTED,spec.constraints.requiredNetworkType);assertFalse(spec.expedited)
    }
    @Test fun notificationPrivacyAndPermissionChannelChecks() {
        val notifier=FollowUpNotifier(context)
        val manager=context.getSystemService(NotificationManager::class.java)
        try {
            // The host sets permission before instrumentation; revoking this process's permission kills it.
            if(context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) {
                assertFalse(notifier.allowed());notifier.post();assertTrue(manager.activeNotifications.none {it.id==FollowUpNotifier.ID})
                return
            }
            val n=notifier.notification();assertEquals(Notification.VISIBILITY_PRIVATE,n.visibility)
            assertEquals("Credit follow-ups need review",n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertEquals("Open the app to check current records",n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertEquals("followups",notifier.destinationIntent().getStringExtra(FollowUpNotifier.EXTRA_DESTINATION))
            notifier.post()
            val deadline=android.os.SystemClock.elapsedRealtime()+3000
            while(manager.activeNotifications.none {it.id==FollowUpNotifier.ID}&&android.os.SystemClock.elapsedRealtime()<deadline) android.os.SystemClock.sleep(25)
            assertTrue(manager.activeNotifications.any {it.id==FollowUpNotifier.ID})
            manager.createNotificationChannel(NotificationChannel("followup_fixture_blocked","Blocked fixture",NotificationManager.IMPORTANCE_NONE))
            assertFalse(FollowUpNotifier(context,"followup_fixture_blocked").allowed())
        } finally {notifier.cancel();manager.deleteNotificationChannel(FollowUpNotifier.CHANNEL)
            manager.deleteNotificationChannel("followup_fixture_blocked")}
    }
}
