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
class PhysicalCreditProbe {
    @Test fun runStage() = runBlocking {
        val base=InstrumentationRegistry.getInstrumentation().targetContext
        check(base.packageName=="com.saaf.milkshop.qa.tests")
        val args=InstrumentationRegistry.getArguments()
        val run=args.getString("qaRun")!!; check(run.matches(Regex("[a-f0-9]{12}")))
        val phone=args.getString("qaPhone")!!; check(phone in setOf("owner","counter"))
        val stage=args.getString("qaStage")!!
        val root=File(base.filesDir,"credit-phone-$run").apply { mkdirs() }
        var context=FixtureContext(base,root)
        SyncScheduler.suppressSchedulingForTest(); SyncIdentity.warmUp(context)
        var db=MilkShopDatabase.create(context)
        var security=UserSecurityService(db)
        val result=JSONObject().put("run",run).put("phone",phone).put("stage",stage)
        val calls=mutableListOf<String>()
        val real=UrlConnectionSyncHttp()
        val http=SyncHttp { request ->
            check(request.url.startsWith(DEV+"/"))
            val response=real.execute(request)
            calls+=request.url.substringAfter(DEV)+":"+response.status
            if(stage=="terms_lost_ack" && request.url.endsWith("credit_metadata_push_v1")) {
                assertTrue(JSONObject(response.bodyText()!!).getBoolean("ok"))
                throw java.io.IOException("Synthetic lost accepted credit acknowledgment")
            }
            response
        }
        val runtime=SyncRuntime(context,db,http)
        val day=LocalDate.parse(args.getString("qaDay")!!)
        suspend fun owner()=security.login("credit.owner",OWNER.toCharArray()).sessionId
        suspend fun customer()=db.accountDao().findByCategory("Customer").single()
        suspend fun credit(): CustomerCreditView {
            val id=customer().accountId
            val balance=db.readModelDao().accountBalances().single { it.accountId==id }.balance
            return CustomerCreditViews(db).load(mapOf(id to balance),day).getValue(id)
        }
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
                "activate" -> { check(phone=="owner"); runtime.enableCreditContract(); assertTrue(credit().enabled) }
                "terms_lost_ack" -> {
                    check(phone=="owner");val c=credit()
                    CreditReviewService(db,security).proposeTerms(owner(),customer().uid,c.revisionUid,CreditPaymentTerm.Monthly,"monthly-$run")
                    assertTrue(runtime.drainOnce() is SyncPushEngine.DrainOutcome.RetryLater)
                    assertNotNull(db.creditMetadataDao().pending("monthly-$run"))
                    assertEquals(CreditPaymentTerm.Weekly,credit().term)
                }
                "sale" -> {
                    check(phone=="counter");val c=credit();assertEquals(CreditPaymentTerm.Monthly,c.term)
                    val cashier=security.login("credit.cashier",CASHIER.toCharArray()).sessionId
                    val sale=com.saaf.milkshop.application.commands.OperationsBridgeCommandService(db,security).recordSale(cashier,
                        SaleInput(db.productDao().findByName("Milk")!!.productId,day.toString(),"credit-retail",100.0,value=200.0,payment=SalePayment.OnCredit(customer().accountId)))
                    val uid=db.eventDao().findById(sale.eventId)!!.eventUid
                    assertEquals("CALENDAR_MONTHS",db.creditMetadataDao().agreement(uid)!!.termKind)
                    File(root,"sale-uid.txt").writeText(uid)
                    result.put("saleUid",uid)
                    assertTrue(runCatching { CreditReviewService(db,security).setFlag(cashier,customer().uid,"Cashier denied") }.exceptionOrNull() is SecurityFailure.PermissionDenied)
                }
                "terms_weekly" -> { check(phone=="owner"); val c=credit();CreditReviewService(db,security).proposeTerms(owner(),customer().uid,c.revisionUid,CreditPaymentTerm.Weekly,"weekly-$run") }
                "correct_collect" -> {
                    check(phone=="owner")
                    val original=db.creditMetadataDao().agreements().single { it.evidence=="RECORDED" }
                    val event=db.syncEnvelopeDao().eventsByUid(listOf(original.rootSaleUid)).single()
                    CorrectionService(db).correct(CorrectionInput.Sale(event.eventId,day.toString(),100.0,value=250.0))
                    CreditCollectionService(db).record(CreditCollectionInput(customer().accountId,db.accountDao().findByCategory("Cash/Bank").first { it.name=="Cash" }.accountId,50.0,day.toString()))
                    assertEquals(original,db.creditMetadataDao().agreement(original.rootSaleUid))
                }
                "flag_a" -> { check(phone=="owner"); CreditReviewService(db,security).setFlag(owner(),customer().uid,"Reviewed A","set-a-$run") }
                "backup" -> {
                    check(phone=="owner")
                    BackupManager(context).exportEncryptedSnapshot(db,Uri.fromFile(File(root,"snapshot.milkbackup")),BACKUP.toCharArray()).getOrThrow()
                }
                "flags_clear_and_b" -> {
                    check(phone=="owner")
                    val service=CreditReviewService(db,security)
                    service.clearFlag(owner(),customer().uid,setOf("set-a-$run"),"Reviewed only A","clear-a-$run")
                    service.setFlag(owner(),customer().uid,"Concurrent unseen B","set-b-$run")
                }
                "restore" -> {
                    check(phone=="owner")
                    // Security-bearing recovery is intentionally allowed only into an uninitialized installation.
                    db.close()
                    File(root,"restored.marker").writeText("fresh-recovery-target")
                    context=FixtureContext(base,root)
                    db=MilkShopDatabase.create(context)
                    security=UserSecurityService(db)
                    BackupManager(context).restoreEncrypted(db,Uri.fromFile(File(root,"snapshot.milkbackup")),BackupManager.RestoreSelection(true,false,false,security=true),BACKUP.toCharArray(),NativeSessionManager()).getOrThrow()
                    assertEquals(0,db.creditMetadataDao().state()!!.contractVersion)
                    assertNull(SyncTokenStore(context).load())
                }
                "old_client_refused" -> {
                    check(phone=="counter")
                    val recorded=db.creditMetadataDao().agreements().single { it.evidence=="RECORDED" }
                    val event=db.syncEnvelopeDao().eventsByUid(listOf(recorded.rootSaleUid)).single()
                    val envelope=SyncEnvelopeFactory(db).build(listOf(SyncOutboxEntity(tableName="events",rowUid=event.eventUid,enqueuedAt=event.createdAt))).envelopes.single()
                    val newUid=java.util.UUID.randomUUID().toString()
                    envelope.put("uid",newUid);envelope.getJSONObject("payload").apply { put("event_uid",newUid);remove("credit_agreement");remove("credit_contract_version") }
                    val response=SyncServerClient(http,SyncServerConfigStore.load(context)!!,db,SyncTokenStore(context)) { SyncShopBindingStore.load(context)!!.shopId }.push(listOf(envelope),emptyList())
                    assertTrue(response is SyncPushResult.Permanent && response.reasonCode=="credit_contract_required")
                }
                "status" -> Unit
                "cleanup" -> { SyncEnrollment(context,http,db).unenroll();File(root,"snapshot.milkbackup").delete() }
                else -> error("Unknown credit stage")
            }
            if(stage !in setOf("init","cleanup") && db.accountDao().findByCategory("Customer").isNotEmpty()) {
                val c=credit()
                result.put("term",c.term.label).put("status",c.label).put("flags",JSONArray(c.activeSetUids.sorted()))
                    .put("agreements",JSONArray(db.creditMetadataDao().agreements().map { CreditMetadataWire.agreement(it) }))
                    .put("metadataPending",db.creditMetadataDao().pending().size)
            }
            result.put("httpStatuses",JSONArray(calls)).put("outbox",db.syncOutboxDao().count()).put("parks",db.syncParkedRowDao().count())
                .put("deviceId",SyncIdentity.currentOrNull()).put("pid",android.os.Process.myPid())
            val balances=db.readModelDao().accountBalances()
            result.put("balances",JSONObject().apply { balances.forEach { put(it.name,it.balance) } })
                .put("stock",JSONObject().apply { db.readModelDao().currentStock().forEach { put(it.name,it.quantityOnHand) } })
            db.close();result.put("dbFilename",context.dbDir.listFiles()!!.single { it.name.endsWith(".db") }.name)
                .put("dbSubdir",context.dbDir.name)
            File(root,"result.json").writeText(result.toString(2))
        } finally { db.close() }
    }
    private class FixtureContext(base: Context,root: File): ContextWrapper(base) {
        val dbDir=File(root,if(File(root,"restored.marker").exists()) "restored-db" else "db").apply { mkdirs() };private val noBackup=File(root,"no-backup").apply { mkdirs() }
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
