package com.saaf.milkshop

import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

class CreditMetadataTransportTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var owner: String
    private lateinit var customerUid: String
    private var customer = 0L
    private var milk = 0L
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val date = LocalDate.parse("2026-10-15")
    private val now = "2026-10-15T08:00:00Z"
    @Before fun setup() = runBlocking {
        SyncScheduler.suppressSchedulingForTest()
        db = MilkShopDatabase.createInMemory()
        security = UserSecurityService(db)
        owner = security.bootstrapOwner("credit.transport.owner","Transport Owner","credit-test-passphrase".toCharArray()).session.sessionId
        customer = CustomerManagementService(db).create("Synthetic terms customer")
        customerUid = db.accountDao().findById(customer)!!.uid
        val vendor = VendorManagementService(db).create("Synthetic stock supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(vendor,date.toString(),100.0,4.0,28.0))
        milk = db.productDao().findByName("Milk")!!.productId
    }
    @After fun close() {
        SyncRebaselineStore(context).clear(); SyncServerConfigStore.clear(context)
        SyncShopBindingStore.clear(context); SyncTokenStore(context).clear()
        db.close()
    }
    private suspend fun ready(version: Int = 1) = db.creditMetadataDao().saveState(
        CreditMetadataSyncStateEntity(shopUid="shop",enabled=true,contractVersion=version))
    private fun revision(id: String = "monthly", prior: String = CreditMetadataStore.DEFAULT_REVISION,
        kind: String = "CALENDAR_MONTHS", value: Int = 1) = JSONObject().put("revision_uid",id)
        .put("customer_uid",customerUid).put("prior_revision_uid",prior).put("term_kind",kind).put("term_value",value)
        .put("local_actor_uid","local-user:1").put("contract_version",1).put("accepted_at",now)
    private fun action(uid: String, target: String? = null) = JSONObject().put("action_uid",uid).put("customer_uid",customerUid)
        .put("kind",if(target == null) "SET" else "CLEAR").put("clears_set_uid",target ?: JSONObject.NULL)
        .put("reason","Synthetic Owner review").put("local_actor_uid","local-user:1").put("occurred_at",now)
        .put("business_date",date.toString()).put("contract_version",1)
    private fun record(seq: Long, kind: String, uid: String, body: JSONObject, shop: String = "shop") = JSONObject()
        .put("server_seq",seq).put("shop_id",shop).put("kind",kind).put("uid",uid).put("payload",body)
    private fun status(enabled: Boolean = true) = CreditRpcResult.Ok(JSONObject().put("ok",true).put("contract_version",1)
        .put("shop_id","shop").put("enabled",enabled))
    private fun feed(records: List<JSONObject> = emptyList()) = CreditRpcResult.Ok(JSONObject().put("ok",true)
        .put("contract_version",1).put("records",JSONArray(records)))
    private suspend fun sale() = SaleService(db).record(SaleInput(milk,date.toString(),"credit-retail",100.0,
        value=100.0,payment=SalePayment.OnCredit(customer)))
    private suspend fun envelope(id: Long): JSONObject {
        val uid = db.eventDao().findById(id)!!.eventUid
        return SyncEnvelopeFactory(db).build(listOf(SyncOutboxEntity(tableName="events",rowUid=uid,enqueuedAt=now))).envelopes.single()
    }

    @Test fun monthlySaleSnapshotsAcceptedTermsAndLaterChangesDoNotRewriteEnvelope() = runBlocking {
        ready()
        CreditMetadataStore(db).applyAcceptedRevision(CreditMetadataWire.revision(revision()))
        val result = sale()
        val root = db.eventDao().findById(result.eventId)!!.eventUid
        val original = envelope(result.eventId).getJSONObject("payload")
        assertEquals("monthly",original.getJSONObject("credit_agreement").getString("revision_uid"))
        assertEquals(1,original.getInt("credit_contract_version"))
        CreditMetadataStore(db).applyAcceptedRevision(CreditMetadataWire.revision(revision("thirty","monthly","NET_DAYS",30)))
        assertEquals("monthly",db.creditMetadataDao().agreement(root)!!.revisionUid)
        assertEquals(original.toString(),envelope(result.eventId).getJSONObject("payload").toString())
        val later = sale()
        assertEquals("thirty",db.creditMetadataDao().agreement(db.eventDao().findById(later.eventId)!!.eventUid)!!.revisionUid)
    }
    @Test fun unavailableContractRollsBackSaleWhilePaidNowAndInactiveLegacyStillWork() = runBlocking {
        val legacy = sale()
        assertEquals("LEGACY_WEEKLY",db.creditMetadataDao().agreement(db.eventDao().findById(legacy.eventId)!!.eventUid)!!.evidence)
        ready(0)
        val count = db.eventDao().count()
        val logs = db.backupDataDao().financialLogs()
        val stock = db.backupDataDao().inventoryLogs()
        assertTrue(runCatching { sale() }.isFailure)
        assertEquals(count,db.eventDao().count()); assertEquals(logs,db.backupDataDao().financialLogs())
        assertEquals(stock,db.backupDataDao().inventoryLogs())
        val cash = db.accountDao().findByCategory("Cash/Bank").first().accountId
        val paid = SaleService(db).record(SaleInput(milk,date.toString(),"retail",100.0,value=100.0,payment=SalePayment.Account(cash)))
        assertFalse(envelope(paid.eventId).getJSONObject("payload").has("credit_agreement"))
        ready()
        val weekly = sale()
        assertEquals("RECORDED",db.creditMetadataDao().agreement(db.eventDao().findById(weekly.eventId)!!.eventUid)!!.evidence)
    }
    @Test fun changedAcceptedTermsRequireFreshAdvisoryAcknowledgment() = runBlocking {
        ready()
        CustomerProfileService(db).save(customer,CustomerProfileInput(creditLimit=0.0))
        val facade = NativeSaleFacade(db,security)
        val request = NativeSaleRequest(milk,NativeSaleEntryMode.AMOUNT,100.0,NativeSalePaymentKind.CUSTOMER_ACCOUNT,null,customer)
        val old = facade.previewCredit(owner,request,date).warningKey!!
        CreditMetadataStore(db).applyAcceptedRevision(CreditMetadataWire.revision(revision()))
        val count = db.eventDao().count()
        val warning = runCatching { facade.record(owner,request.copy(creditAcknowledgment=old),date) }.exceptionOrNull() as CreditWarningRequired
        assertNotEquals(old,warning.warning.summary.warningKey)
        assertEquals(count,db.eventDao().count())
    }
    @Test fun lostAcknowledgmentIsRecoveredFromFeedAndNeverPublishesRestoredHeldWork() = runBlocking {
        ready()
        CreditReviewService(db,security).proposeTerms(owner,customerUid,CreditMetadataStore.DEFAULT_REVISION,CreditPaymentTerm.Monthly,"monthly")
        val accepted = revision()
        var serverAccepted = false
        var pushes = 0
        val sync = CreditMetadataSync(db)
        val rpc: suspend (String,JSONObject)->CreditRpcResult = { name,request -> when(name) {
            "credit_contract_status_v1" -> status()
            "credit_metadata_pull_v1" -> feed(if(serverAccepted && request.getLong("since_seq") < 1) listOf(record(1,"TERM_REVISION","monthly",accepted)) else emptyList())
            else -> { pushes++; serverAccepted=true; CreditRpcResult.Retry("lost_response") }
        } }
        assertTrue(sync.exchange("shop",rpc) is CreditMetadataSync.Outcome.Retry)
        assertNull(db.creditMetadataDao().head(customerUid))
        db.creditMetadataDao().setHeld("monthly",true)
        ready(0)
        assertEquals(CreditMetadataSync.Outcome.Done,sync.exchange("shop",rpc))
        assertEquals(1,pushes); assertTrue(db.creditMetadataDao().pending().isEmpty())
        assertEquals("monthly",db.creditMetadataDao().head(customerUid)!!.revisionUid)
        CreditReviewService(db,security).proposeTerms(owner,customerUid,"monthly",CreditPaymentTerm.Weekly,"unsent")
        db.creditMetadataDao().setHeld("unsent",true)
        assertEquals(CreditMetadataSync.Outcome.Done,sync.exchange("shop",rpc))
        assertTrue(db.creditMetadataDao().pending("unsent")!!.held); assertEquals(1,pushes)
    }
    @Test fun staleProposalAndChangedAcknowledgmentStayQueuedWithoutPromotingHead() = runBlocking {
        ready()
        CreditReviewService(db,security).proposeTerms(owner,customerUid,CreditMetadataStore.DEFAULT_REVISION,CreditPaymentTerm.Monthly,"monthly")
        var reject = false
        val rpc: suspend (String,JSONObject)->CreditRpcResult = { name,_ -> when(name) {
            "credit_contract_status_v1" -> status()
            "credit_metadata_pull_v1" -> feed()
            else -> if(reject) CreditRpcResult.Refused("credit_terms_revision_conflict") else CreditRpcResult.Ok(JSONObject().put("ok",true)
                .put("accepted",JSONArray().put(JSONObject().put("kind","TERM_REVISION").put("uid","monthly")
                    .put("payload",revision(value=2)))))
        } }
        assertTrue(CreditMetadataSync(db).exchange("shop",rpc) is CreditMetadataSync.Outcome.Retry)
        assertNotNull(db.creditMetadataDao().pending("monthly")); assertNull(db.creditMetadataDao().head(customerUid))
        reject=true
        assertEquals(CreditMetadataSync.Outcome.Halted("credit_terms_revision_conflict"),CreditMetadataSync(db).exchange("shop",rpc))
        assertTrue(db.creditMetadataDao().pending("monthly")!!.held)
        assertNull(db.creditMetadataDao().head(customerUid))
    }
    @Test fun malformedOrForeignFeedNeverAdvancesPastRefusedRecordAndDependencyRetries() = runBlocking {
        ready(0)
        var body = revision().put("term_value",1.5)
        var shop = "shop"
        val rpc: suspend (String,JSONObject)->CreditRpcResult = { name,_ -> if(name=="credit_contract_status_v1") status()
            else feed(listOf(record(1,"TERM_REVISION","monthly",body,shop))) }
        val sync = CreditMetadataSync(db)
        assertTrue(sync.exchange("shop",rpc) is CreditMetadataSync.Outcome.Retry)
        assertEquals(0L,db.creditMetadataDao().state()!!.lastServerSeq)
        assertTrue(db.creditMetadataDao().revisions().isEmpty())
        body=revision(); shop="other-shop"
        assertTrue(sync.exchange("shop",rpc) is CreditMetadataSync.Outcome.Retry)
        assertEquals(0L,db.creditMetadataDao().state()!!.lastServerSeq)
        shop="shop"; body=revision().put("customer_uid","missing-customer")
        assertTrue(sync.exchange("shop",rpc) is CreditMetadataSync.Outcome.Retry)
        body=revision()
        assertEquals(CreditMetadataSync.Outcome.Done,sync.exchange("shop",rpc))
        assertEquals(1L,db.creditMetadataDao().state()!!.lastServerSeq)
        assertEquals(1,db.creditMetadataDao().revisions().size)
    }
    @Test fun restoreReconcilesRemoteClearAndPreservesUnseenConcurrentFlag() = runBlocking {
        ready()
        CreditReviewService(db,security).setFlag(owner,customerUid,"Synthetic Owner review","set-old")
        db.creditMetadataDao().setHeld("set-old",true)
        ready(0)
        val records = listOf(record(1,"REVIEW_ACTION","set-old",action("set-old")),
            record(2,"REVIEW_ACTION","clear-old",action("clear-old","set-old")),
            record(3,"REVIEW_ACTION","set-unseen",action("set-unseen")))
        // Match the real local timestamp/body exactly for identity-safe restored acknowledgment.
        records[0].put("payload",JSONObject(db.creditMetadataDao().pending("set-old")!!.payloadJson))
        val result = CreditMetadataSync(db).exchange("shop",{ name,_ -> if(name=="credit_contract_status_v1") status() else feed(records) })
        assertEquals(CreditMetadataSync.Outcome.Done,result)
        assertTrue(db.creditMetadataDao().pending().isEmpty())
        val review = CreditFlagReviewCalculator.calculate(customerUid,db.creditMetadataDao().actionsFor(customerUid).map { it.toAction() })
        assertEquals(setOf("set-unseen"),review.activeSetUids)
        assertEquals(3,db.creditMetadataDao().actions().size)
    }
    @Test fun unconfirmedRestoredRevisionAndDisappearedContractCannotReenablePosting() = runBlocking {
        CreditMetadataStore(db).applyAcceptedRevision(CreditMetadataWire.revision(revision()))
        ready(0)
        val sync = CreditMetadataSync(db)
        assertEquals(CreditMetadataSync.Outcome.Halted("credit_restored_revision_unconfirmed"),sync.exchange("shop",{ name,_ ->
            if(name=="credit_contract_status_v1") status() else feed() }))
        assertTrue(runCatching { sale() }.isFailure)
        assertEquals(CreditMetadataSync.Outcome.Halted("credit_required_contract_missing"),sync.exchange("shop",{ _,_->status(false) }))
        assertTrue(db.creditMetadataDao().state()!!.enabled); assertEquals(0,db.creditMetadataDao().state()!!.contractVersion)
    }
    @Test fun incomingAgreementParksWholeSaleThenReplaysWithRevisionAndNoEchoOrDuplicate() = runBlocking {
        ready(); CreditMetadataStore(db).applyAcceptedRevision(CreditMetadataWire.revision(revision()))
        val result = sale()
        val wire = envelope(result.eventId)
        val target = MilkShopDatabase.createInMemory()
        try {
            target.accountDao().insert(AccountEntity(name="Customer: Different local ID",type="Asset",category="Customer",uid=customerUid))
            val vendor = VendorManagementService(target).create("Receiver stock fixture")
            VendorPurchaseService(target).record(VendorPurchaseInput(vendor,date.toString(),100.0,4.0,28.0))
            target.syncOutboxDao().deleteAll()
            val beforeFinance = target.backupDataDao().financialLogs()
            val pulled = SyncPulledRecord(1,"shop","events",wire.getString("uid"),wire.getJSONObject("payload"),"other-phone")
            assertEquals(1,SyncApplyEngine(target).apply(listOf(pulled)).parked)
            assertTrue(target.syncEnvelopeDao().eventsByUid(listOf(pulled.rowUid)).isEmpty())
            assertEquals(beforeFinance,target.backupDataDao().financialLogs())
            assertTrue(target.securityAuditDao().all().any { it.reasonCode?.contains("Credit agreement revision is missing") == true })
            CreditMetadataStore(target).applyAcceptedRevision(CreditMetadataWire.revision(revision()))
            SyncApplyEngine(target).apply(emptyList())
            assertNotNull(target.syncParkedRowDao().all().toString(),target.creditMetadataDao().agreement(pulled.rowUid))
            assertTrue(target.syncParkedRowDao().all().isEmpty())
            val finance = target.backupDataDao().financialLogs()
            assertEquals(beforeFinance.size+1,finance.size)
            val receivedId = target.syncEnvelopeDao().eventsByUid(listOf(pulled.rowUid)).single().eventId
            assertEquals(100.0,finance.single { it.eventId == receivedId }.amount,0.0)
            SyncApplyEngine(target).apply(listOf(pulled))
            assertEquals(finance,target.backupDataDao().financialLogs())
            assertEquals(0L,target.syncOutboxDao().count())
            val changed = JSONObject(pulled.payload.toString()).apply { getJSONObject("credit_agreement").put("term_value",7) }
            assertEquals(1,SyncApplyEngine(target).apply(listOf(pulled.copy(serverSeq=2,payload=changed))).parked)
            assertEquals(finance,target.backupDataDao().financialLogs())
            assertEquals(1,target.creditMetadataDao().agreement(pulled.rowUid)!!.termValue)
        } finally { target.close() }
    }

    @Test fun flagQueueSendsSetBeforeClearAndOnlyAcknowledgesExactBodies() = runBlocking {
        ready()
        val reviews = CreditReviewService(db,security)
        reviews.setFlag(owner,customerUid,"Synthetic review","zz-set")
        reviews.clearFlag(owner,customerUid,setOf("zz-set"),"Synthetic clear","clear-operation")
        val sent = mutableListOf<String>()
        val outcome = CreditMetadataSync(db).exchange("shop",{ name,body -> when(name) {
            "credit_contract_status_v1" -> status()
            "credit_metadata_pull_v1" -> feed()
            else -> {
                val op=body.getJSONArray("operations").getJSONObject(0)
                sent+=op.getJSONObject("payload").getString("kind")
                CreditRpcResult.Ok(JSONObject().put("ok",true).put("accepted",JSONArray().put(op)))
            }
        } })
        assertEquals(CreditMetadataSync.Outcome.Done,outcome)
        assertEquals(listOf("SET","CLEAR"),sent)
        assertTrue(db.creditMetadataDao().pending().isEmpty())
    }
    @Test fun httpClientUsesCurrentBearerAndDistinguishesUnknownFromRefusal() = runBlocking {
        val config=SyncServerConfig("dev","https://example.invalid","synthetic-public")
        val tokens=SyncTokenStore(context)
        tokens.save(SyncTokenStore.Session("synthetic-old",null,null))
        val statuses=listOf(401,404,503,422,200,200)
        val bodies=listOf("", "", "", """{"code":"PT422","message":"credit_invalid_term"}""",
            """{"ok":"true"}""", """{"ok":false,"error":{"code":"credit_terms_revision_conflict"}}""")
        var index=0
        val client=CreditMetadataClient(SyncHttp { request ->
            assertEquals("POST",request.method)
            assertTrue(request.url.endsWith("/rest/v1/rpc/credit_contract_status_v1"))
            assertEquals("Bearer synthetic-old",request.headers[AccountAuthClient.HEADER_AUTHORIZATION])
            assertEquals("shop",JSONObject(String(request.body!!)).getString("shop"))
            SyncHttpResponse(statuses[index],bodies[index++].toByteArray())
        },config,tokens)
        assertEquals(CreditRpcResult.Retry("unauthorized"),client.call("credit_contract_status_v1",JSONObject().put("shop","shop")))
        assertEquals(CreditRpcResult.Unsupported,client.call("credit_contract_status_v1",JSONObject().put("shop","shop")))
        assertTrue(client.call("credit_contract_status_v1",JSONObject().put("shop","shop")) is CreditRpcResult.Retry)
        assertTrue(client.call("credit_contract_status_v1",JSONObject().put("shop","shop")) is CreditRpcResult.Refused)
        assertEquals(CreditRpcResult.Retry("malformed_response"),client.call("credit_contract_status_v1",JSONObject().put("shop","shop")))
        assertEquals(CreditRpcResult.Refused("credit_terms_revision_conflict"),client.call("credit_contract_status_v1",JSONObject().put("shop","shop")))
        assertEquals("synthetic-old",tokens.load()!!.accessToken)
    }
    @Test fun runtimeRefreshesOnceAndUnknownRefreshKeepsRequiredContractAndTokens() = runBlocking {
        SyncRebaselineStore(context).clear()
        SyncServerConfigStore.save(context,SyncServerConfig("dev","https://example.invalid","synthetic-public"))
        SyncShopBindingStore.save(context,SyncShopBinding("dev","business","shop","Shop","staff","cashier"))
        val tokens=SyncTokenStore(context)
        tokens.save(SyncTokenStore.Session("old-access","synthetic-refresh",null))
        ready(0); db.syncOutboxDao().deleteAll()
        var refreshes=0
        var creditCalls=0
        var failRefresh=false
        val runtime=SyncRuntime(context,db,SyncHttp { request -> when {
            request.url.contains("/auth/v1/token") -> {
                refreshes++
                if(failRefresh) SyncHttpResponse(503,null) else SyncHttpResponse(200,
                    """{"access_token":"fresh-access","refresh_token":"fresh-refresh","expires_in":3600}""".toByteArray())
            }
            request.url.endsWith("my_profile") -> SyncHttpResponse(200,
                """{"staff_id":"staff","role":"cashier","active":true,"business_id":"business","shops":[{"id":"shop"}]}""".toByteArray())
            request.url.endsWith("credit_contract_status_v1") -> {
                creditCalls++
                if(request.headers[AccountAuthClient.HEADER_AUTHORIZATION]=="Bearer old-access") SyncHttpResponse(401,null)
                else SyncHttpResponse(200,status().body.toString().toByteArray())
            }
            request.url.endsWith("credit_metadata_pull_v1") -> SyncHttpResponse(200,feed().body.toString().toByteArray())
            else -> SyncHttpResponse(200,"""{"ok":true}""".toByteArray())
        } })
        assertTrue(runtime.drainOnce() is SyncPushEngine.DrainOutcome.Drained)
        assertEquals(1,refreshes); assertEquals(3,creditCalls)
        assertEquals("fresh-access",tokens.load()!!.accessToken)
        tokens.save(SyncTokenStore.Session("old-access","synthetic-refresh",null)); failRefresh=true
        assertTrue(runtime.drainOnce() is SyncPushEngine.DrainOutcome.RetryLater)
        assertEquals(2,refreshes)
        assertEquals("old-access",tokens.load()!!.accessToken)
        assertTrue(db.creditMetadataDao().state()!!.enabled)
    }
}
