package com.saaf.milkshop

import android.net.Uri
import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.io.IOException
import java.time.*
import java.util.UUID

class FollowUpDataTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val migrationName="followup-data-migration.db"
    @get:Rule val migration=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),context.getDatabasePath(migrationName),
        BundledSQLiteDriver(),MilkShopDatabase::class,
        {Room.databaseBuilder<MilkShopDatabase>(context,migrationName).setDriver(BundledSQLiteDriver()).addMigrations(MilkShopDatabase.MIGRATION_18_19).build()},emptyList())
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var session: String
    private val shop="10000000-0000-4000-8000-000000000001"
    private val staff="10000000-0000-4000-8000-000000000002"
    private val revision="10000000-0000-4000-8000-000000000003"
    private val scheduleRevision="10000000-0000-4000-8000-000000000004"
    private val customerRevision="10000000-0000-4000-8000-000000000005"
    private val attempt="10000000-0000-4000-8000-000000000006"
    private val day="2026-10-18"
    private val instant="2026-10-18T07:00:00Z"
    private val binding get()=FollowUpBinding("dev","https://example.invalid",shop,staff,"fixture-installation-one")
    private fun id()=UUID.randomUUID().toString()
    @Before fun setup(): Unit=runBlocking {
        SyncScheduler.suppressSchedulingForTest();SyncRebaselineStore(context).clear()
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db)
        session=security.bootstrapOwner("followup.data.owner","Data Owner",PASSWORD.toCharArray()).session.sessionId
    }
    @After fun close() {
        SyncServerConfigStore.clear(context);SyncShopBindingStore.clear(context);SyncTokenStore(context).clear();SyncRebaselineStore(context).clear()
        TrustedTime.resetForTest(context)
        db.close();context.deleteDatabase(migrationName)
    }
    private fun heads(metadata: Long=1)=JSONObject().put("sync",10L).put("credit",4L).put("metadata",metadata)
        .put("policy_revision",revision).put("business_date",day)
    private fun policy()=JSONObject().put("revision",revision).put("schedule_revision",scheduleRevision).put("effective_from","2026-10-16")
        .put("enabled",true).put("weekday",7).put("repeat_days",7).put("preferred_time","10:00:00").put("quiet_start","20:00:00").put("quiet_end","09:00:00")
        .put("primary_installation",binding.installation).put("primary_actor",staff).put("updated_by",staff).put("updated_at",instant)
    private fun customer()=JSONObject().put("customer_uid","qa").put("revision",customerRevision).put("schedule_revision",JSONObject.NULL)
        .put("effective_from",JSONObject.NULL).put("schedule",JSONObject.NULL).put("paused",false).put("snoozed_until",JSONObject.NULL)
        .put("statement_cycle",JSONObject.NULL).put("reviewed_on",JSONObject.NULL)
    private fun snapshot(metadata: Long=1)=JSONObject().put("ok",true).put("contract_version",1).put("shop_id",shop).put("shop_timezone","Asia/Karachi")
        .put("server_time_epoch_ms",Instant.parse(instant).toEpochMilli()).put("heads",heads(metadata)).put("policy",policy())
        .put("customers",JSONArray().put(customer())).put("claims",JSONArray()).put("actions",JSONArray().put(JSONObject().put("seq",metadata)
            .put("command_uid",id()).put("kind","POLICY").put("actor_id",staff).put("installation",binding.installation)
            .put("customer_uid",JSONObject.NULL).put("attempt_uid",JSONObject.NULL).put("cycle",JSONObject.NULL).put("accepted_at",instant)))
    private fun operation(kind: String="START")=JSONObject().put("kind",kind).apply {
        if(kind=="DIGEST") put("has_work",true) else {put("customer_uid","qa");put("expected_customer_revision",customerRevision);put("attempt_uid",attempt);put("channel","SMS")}
        put("expected_heads",heads())
    }
    private fun ack(request: JSONObject,replay: Boolean=false)=JSONObject().put("ok",true).put("replayed",replay)
        .put("launch_authorized",!replay&&request.getString("kind")=="START").put("notify_authorized",!replay&&request.getString("kind")=="DIGEST")
        .put("result",if(request.getString("kind")=="DIGEST") JSONObject().put("business_date",day).put("reserved",true)
            else JSONObject().put("attempt_uid",attempt).put("phase","HANDOFF_STARTED"))
        .apply {if(!replay) put("heads",heads(2))}
    private suspend fun loaded(store: FollowUpDataStore=FollowUpDataStore(db))=store.refresh(binding) {_,_->FollowUpRpcResult.Ok(snapshot())}
    private fun signed(root: JSONObject): ByteArray {root.remove("integrity_sha256");root.put("integrity_sha256",sha256(root.toString().toByteArray()));return root.toString().toByteArray()}

    @Test fun migrationPreservesLedgerAndStartsEmpty()=runBlocking {
        context.deleteDatabase(migrationName)
        migration.createDatabase(18).use {c->
            c.execSQL("INSERT INTO accounts(account_id,name,type,category,uid) VALUES(701,'Customer: Retained','Asset','Customer','retained')")
            c.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,event_uid,surcharge_applied) VALUES(702,'$instant','2026-10-16','OPENING_BALANCE','retained-event',0)")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES(702,701,12.345)")
        }
        migration.runMigrationsAndValidate(19,listOf(MilkShopDatabase.MIGRATION_18_19)).use {c->
            c.prepare("SELECT amount FROM financial_log WHERE event_id=702").use {assertTrue(it.step());assertEquals(12.345,it.getDouble(0),0.0)}
            for(table in listOf("followup_cache","followup_commands")) c.prepare("SELECT count(*) FROM $table").use {assertTrue(it.step());assertEquals(0,it.getLong(0))}
        }
    }
    @Test fun snapshotCodecRejectsCoercionAndConflictingFacts() {
        val good=snapshot();assertEquals(scheduleRevision,FollowUpWire.snapshot(good,shop).policy!!.revision)
        val mutations: List<(JSONObject)->Unit> = listOf(
            {it.put("contract_version",2)}, {it.put("shop_id",id())}, {it.put("server_time_epoch_ms","123")},
            {it.getJSONObject("heads").put("sync",10.5)}, {it.getJSONObject("policy").put("enabled","true")},
            {it.getJSONObject("policy").put("revision",id())}, {it.getJSONArray("customers").put(customer())},
            {it.getJSONArray("customers").getJSONObject(0).put("reviewed_on","2026-10-19")},
            {it.getJSONObject("heads").put("metadata",2)}, {it.put("financial_balance",100)},
            {it.getJSONArray("claims").put(JSONObject().put("customer_uid","qa").put("attempt_uid",attempt).put("actor_id",staff)
                .put("installation",binding.installation).put("cycle","cycle").put("phase","HANDOFF_STARTED").put("lease_until",instant)
                .put("started_at",JSONObject.NULL).put("channel",JSONObject.NULL))}
        )
        mutations.forEachIndexed {index,change->val body=JSONObject(good.toString());change(body);assertTrue("Mutation $index",runCatching {FollowUpWire.snapshot(body,shop)}.isFailure)}
    }
    @Test fun cacheMapsScheduleVersionsWithoutFinancialWrites()=runBlocking {
        val logs=db.backupDataDao().financialLogs();val events=db.eventDao().count();val store=FollowUpDataStore(db)
        assertTrue(loaded(store) is FollowUpLoad.Loaded)
        val value=store.cached(binding)!!;assertFalse(value.stale);assertEquals("shop",value.snapshot.preferences["qa"]!!.revision)
        assertEquals(scheduleRevision,value.snapshot.policy!!.revision);assertEquals(revision,value.snapshot.heads.policyRevision)
        val next=snapshot(2);next.getJSONObject("policy").put("repeat_days",10)
        assertTrue(store.refresh(binding) {_,_->FollowUpRpcResult.Ok(next)} is FollowUpLoad.Loaded)
        assertEquals(scheduleRevision,store.cached(binding)!!.snapshot.policy!!.revision)
        assertEquals(logs,db.backupDataDao().financialLogs());assertEquals(events,db.eventDao().count())
    }
    @Test fun refreshFailureAndRevocationNeverLeaveCurrentCache()=runBlocking {
        val store=FollowUpDataStore(db);loaded(store)
        assertTrue(store.refresh(binding) {_,_->throw IOException("synthetic")} is FollowUpLoad.Unavailable)
        assertTrue(store.cached(binding)!!.stale)
        loaded(store)
        assertTrue(store.refresh(binding) {_,_->FollowUpRpcResult.Ok(snapshot().put("contract_version",3))} is FollowUpLoad.Unavailable)
        assertTrue(store.cached(binding)!!.stale)
        assertTrue(store.refresh(binding) {_,_->FollowUpRpcResult.Refused("followup_not_authorized")} is FollowUpLoad.Unavailable)
        assertNull(store.cached(binding))
    }
    @Test fun unknownAndRecoveredCommandsNeverAuthorizeSideEffects(): Unit=runBlocking {
        val store=FollowUpDataStore(db)
        for(kind in listOf("START","DIGEST")) {
            val body=operation(kind);val uid=id()
            assertTrue(store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Retry("network")} is FollowUpCommandOutcome.NeedsReview)
            assertNotNull(db.followUpDataDao().command(uid))
            var sent: JSONObject?=null
            val recovered=store.execute(binding,uid,body) {_,p->sent=p;FollowUpRpcResult.Ok(ack(body))} as FollowUpCommandOutcome.Accepted
            assertEquals(uid,sent!!.getString("command_uid"));assertEquals(FollowUpLiveGrant.NONE,recovered.grant)
            assertFalse(recovered.receipt.launch);assertFalse(recovered.receipt.notify);assertNull(db.followUpDataDao().command(uid))
            val first=store.execute(binding,id(),body) {_,_->FollowUpRpcResult.Ok(ack(body))} as FollowUpCommandOutcome.Accepted
            assertEquals(if(kind=="START") FollowUpLiveGrant.LAUNCH else FollowUpLiveGrant.NOTIFY,first.grant)
        }
        val uid=id();val body=operation()
        assertTrue(runCatching {store.execute(binding,uid,body) {_,_->throw CancellationException("synthetic cancellation")}}.isFailure)
        assertNotNull(db.followUpDataDao().command(uid))
        val recovered=store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Ok(ack(body,true))} as FollowUpCommandOutcome.Accepted
        assertEquals(FollowUpLiveGrant.NONE,recovered.grant)
    }
    @Test fun mismatchedAcknowledgmentRetainsOriginalRequest(): Unit=runBlocking {
        val store=FollowUpDataStore(db);val body=operation();val uid=id()
        val malformed=ack(body);malformed.getJSONObject("result").put("attempt_uid",id())
        assertTrue(store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Ok(malformed)} is FollowUpCommandOutcome.NeedsReview)
        assertEquals(body.toString(),db.followUpDataDao().command(uid)!!.operationJson)
        val replay=ack(body,true).put("launch_authorized",true)
        assertTrue(store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Ok(replay)} is FollowUpCommandOutcome.NeedsReview)
        assertNotNull(db.followUpDataDao().command(uid))
    }
    @Test fun bindingAndIdentityConflictsCannotRetargetCommands(): Unit=runBlocking {
        val store=FollowUpDataStore(db);val uid=id();val body=operation()
        store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Retry("network")}
        var calls=0
        assertTrue(runCatching {store.execute(binding.copy(installation="another-installation"),uid,body) {_,_->calls++;FollowUpRpcResult.Ok(ack(body))}}.isFailure)
        assertTrue(runCatching {store.execute(binding,uid,JSONObject(body.toString()).put("channel","WHATSAPP")) {_,_->calls++;FollowUpRpcResult.Ok(ack(body))}}.isFailure)
        assertEquals(0,calls)
        val replay=store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Ok(ack(body,true))} as FollowUpCommandOutcome.Accepted
        assertEquals(FollowUpLiveGrant.NONE,replay.grant)
        val duplicate=store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Ok(ack(body,true))} as FollowUpCommandOutcome.Accepted
        assertEquals(FollowUpLiveGrant.NONE,duplicate.grant)
    }
    @Test fun restoredJournalIsHeldAndCacheIsNotRestored(): Unit=runBlocking {
        val store=FollowUpDataStore(db);loaded(store);val uid=id();val body=operation()
        store.execute(binding,uid,body) {_,_->FollowUpRpcResult.Retry("network")}
        val manager=BackupManager(context);val target=MilkShopDatabase.createInMemory();val file=File(context.cacheDir,"followup-roundtrip.milkbackup")
        val password="synthetic followup backup phrase".toCharArray()
        try {
            assertTrue(manager.exportSnapshot(db,Uri.fromFile(file)).isFailure)
            manager.exportEncryptedSnapshot(db,Uri.fromFile(file),password).getOrThrow()
            manager.restoreEncrypted(target,Uri.fromFile(file),BackupManager.RestoreSelection(true,false,false,security=true),password,NativeSessionManager()).getOrThrow()
            val journal=target.followUpDataDao().command(uid)!!;assertTrue(journal.restored)
            assertNull(target.followUpDataDao().cache(binding.key));assertEquals(body.toString(),journal.operationJson)
            val recovery=FollowUpDataStore(target).execute(binding,uid,body) {_,_->FollowUpRpcResult.Ok(ack(body))} as FollowUpCommandOutcome.Accepted
            assertEquals(FollowUpLiveGrant.NONE,recovery.grant);assertFalse(recovery.receipt.launch)
        } finally {password.fill('\u0000');target.close();file.delete()}
    }
    @Test fun malformedRestoreRollsBackAndLegacyBackupStillWorks(): Unit=runBlocking {
        val store=FollowUpDataStore(db);val uid=id();store.execute(binding,uid,operation()) {_,_->FollowUpRpcResult.Retry("network")}
        val manager=BackupManager(context);val bytes=manager.buildCompleteSnapshotBytes(db)
        val malformed=JSONObject(bytes.toString(Charsets.UTF_8));malformed.getJSONObject("data").getJSONArray("followup_commands").getJSONObject(0).put("operation_json","{}")
        val before=db.backupDataDao().accounts()
        assertTrue(manager.restoreCompleteSnapshotReplacing(db,signed(malformed),NativeSessionManager(),"invalid-followup").isFailure)
        assertEquals(before,db.backupDataDao().accounts());assertNotNull(db.followUpDataDao().command(uid))
        val legacy=JSONObject(bytes.toString(Charsets.UTF_8));legacy.getJSONObject("metadata").put("schema_version",13)
        assertTrue(manager.restoreCompleteSnapshotReplacing(db,signed(legacy),NativeSessionManager(),"hidden-followup").isFailure)
        legacy.getJSONObject("data").remove("followup_commands")
        manager.restoreCompleteSnapshotReplacing(db,signed(legacy),NativeSessionManager(),"legacy-followup").getOrThrow()
        assertTrue(db.followUpDataDao().commands().isEmpty());assertNull(db.followUpDataDao().cache(binding.key))
    }
    private fun enrolled() {
        SyncServerConfigStore.save(context,SyncServerConfig("dev","https://example.invalid","synthetic-public"))
        SyncShopBindingStore.save(context,SyncShopBinding("dev",id(),shop,"Fixture shop",staff,"owner"))
        SyncTokenStore(context).save(SyncTokenStore.Session("old-access","synthetic-refresh",null))
    }
    @Test fun runtimeRefreshesOnceAndChecksLocalRole(): Unit=runBlocking {
        enrolled();var refreshes=0;var reads=0
        val runtime=SyncRuntime(context,db,SyncHttp {request->when {
            request.url.contains("/auth/v1/token")-> {refreshes++;SyncHttpResponse(200,"""{"access_token":"fresh-access","refresh_token":"fresh-refresh","expires_in":3600}""".toByteArray())}
            request.url.endsWith("my_profile")->SyncHttpResponse(200,"""{"staff_id":"$staff","role":"owner","active":true,"business_id":"${id()}","shops":[{"id":"$shop"}]}""".toByteArray())
            request.url.endsWith("followup_snapshot_v1")-> {reads++;if(request.headers[AccountAuthClient.HEADER_AUTHORIZATION]=="Bearer old-access") SyncHttpResponse(401,null) else SyncHttpResponse(200,snapshot().toString().toByteArray())}
            else->error("Unexpected follow-up data request")
        }},followUpSecurity=security)
        assertTrue(runtime.loadFollowUps(session) is FollowUpLoad.Loaded);assertEquals(1,refreshes);assertEquals(2,reads)
        assertNotNull(runtime.cachedFollowUps(session));assertEquals("fresh-access",SyncTokenStore(context).load()!!.accessToken)
        val token=security.beginUserEnrollment(session,CreateUserInput("followup.data.cashier","Cashier","cashier"))
        security.completeUserEnrollment(token.enrollmentToken,PASSWORD.toCharArray())
        val cashier=security.login("followup.data.cashier",PASSWORD.toCharArray()).sessionId
        assertTrue(runCatching {runtime.loadFollowUps(cashier)}.isFailure)
        security.logout(session);assertTrue(runCatching {runtime.cachedFollowUps(session)}.isFailure);assertEquals(2,reads)
    }
    @Test fun runtimeUnknownRefreshPreservesTokens(): Unit=runBlocking {
        enrolled();var refreshes=0
        TrustedTime.resetForTest(context)
        val malformed=snapshot().put("server_time_epoch_ms","123")
        val client=FollowUpClient(SyncHttp {SyncHttpResponse(200,malformed.toString().toByteArray())},
            SyncServerConfig("dev","https://example.invalid","synthetic-public"),SyncTokenStore(context))
        assertTrue(client.call("followup_snapshot_v1",JSONObject().put("shop",shop)) is FollowUpRpcResult.Retry)
        assertFalse(TrustedTime.isAnchored())
        val store=FollowUpDataStore(db);val actual=binding.copy(installation=SyncIdentity.warmUp(context))
        store.refresh(actual) {_,_->FollowUpRpcResult.Ok(snapshot())}
        var revoked=false
        val runtime=SyncRuntime(context,db,SyncHttp {r->if(r.url.contains("/auth/v1/token")) {
            refreshes++;if(revoked) SyncHttpResponse(400,"""{"error":"invalid_grant","error_description":"Invalid Refresh Token"}""".toByteArray()) else SyncHttpResponse(503,null)
        } else SyncHttpResponse(401,null)},followUpSecurity=security)
        assertTrue(runtime.loadFollowUps(session) is FollowUpLoad.Unavailable);assertEquals(1,refreshes)
        assertEquals("old-access",SyncTokenStore(context).load()!!.accessToken);assertTrue(runtime.cachedFollowUps(session)!!.stale)
        revoked=true
        assertTrue(runtime.loadFollowUps(session) is FollowUpLoad.Unavailable);assertEquals(2,refreshes)
        assertNull(SyncTokenStore(context).load());assertNull(store.cached(actual))
    }
    companion object {const val PASSWORD="synthetic followup data owner phrase"}
}
