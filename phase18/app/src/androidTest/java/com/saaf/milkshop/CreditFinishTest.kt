package com.saaf.milkshop

import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

class CreditFinishTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var session: String
    private var customer=0L
    private lateinit var uid: String
    @Before fun setup()=runBlocking {
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db)
        session=security.bootstrapOwner("finish.owner","Finish Owner","synthetic finish owner phrase".toCharArray()).session.sessionId
        customer=CustomerManagementService(db).create("Finish Customer");uid=db.accountDao().findById(customer)!!.uid
        val vendor=VendorManagementService(db).create("Finish Supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(vendor,"2026-10-01",100.0,4.0,28.0))
        db.creditMetadataDao().saveState(CreditMetadataSyncStateEntity(shopUid="shop",enabled=true,contractVersion=1))
    }
    @After fun close()=db.close()
    private suspend fun view()=CustomerCreditViews(db).load(mapOf(customer to db.readModelDao().accountBalances().single { it.accountId==customer }.balance),LocalDate.parse("2026-10-15")).getValue(customer)
    private suspend fun sale(date: String)=SaleService(db).record(SaleInput(db.productDao().findByName("Milk")!!.productId,date,"credit-retail",100.0,value=100.0,payment=SalePayment.OnCredit(customer)))
    @Test fun batchStatusUsesEveryAgreementAndCanonicalLedger()=runBlocking {
        sale("2026-10-01")
        CreditMetadataStore(db).applyAcceptedRevision(CreditTermRevisionEntity("monthly",uid,"weekly-default-v1","CALENDAR_MONTHS",1,"local:owner","2026-10-02T00:00:00Z"))
        sale("2026-10-03")
        val result=view();assertEquals(CreditStatusKind.LATE,result.status.kind);assertEquals(10000L,result.status.overduePaisa)
        assertEquals(CreditPaymentTerm.Monthly,result.term)
        val detail=NativeOwnerCustomersFacade(db,security).loadDetail(session,customer)
        assertEquals(2,detail.ledger!!.entries.size)
        assertEquals(2,db.creditMetadataDao().agreements().size)
    }
    @Test fun pendingAndRestoredClearsCannotHideAcceptedFlag()=runBlocking {
        val service=CreditReviewService(db,security)
        service.setFlag(session,uid,"First review","set-a")
        assertFalse(view().status.ownerFlagged)
        assertEquals("Marked for Owner review",view().pending.single().description)
        assertTrue(view().history.single().contains("Waiting for sync"))
        db.creditMetadataDao().deletePending("set-a") // Simulates the already-tested identical server acceptance.
        assertTrue(view().status.ownerFlagged)
        assertTrue(view().history.single().contains("Confirmed by sync"))
        service.clearFlag(session,uid,setOf("set-a"),"Reviewed","clear-a")
        assertTrue(view().status.ownerFlagged)
        assertEquals("Reviewed marks cleared",view().pending.single().description)
        val clear=db.creditMetadataDao().pending().single()
        db.creditMetadataDao().setHeld(clear.operationUid,true)
        assertTrue(view().status.ownerFlagged);assertTrue(view().pending.single().held)
        db.creditMetadataDao().deletePending(clear.operationUid)
        assertFalse(view().status.ownerFlagged)
    }
    @Test fun heldTermReviewChecksOwnerCustomerAndNewAcceptedHead()=runBlocking {
        val facade=NativeOwnerCustomersFacade(db,security)
        facade.creditAction(session,customer,CustomerCreditCommand.Terms("weekly-default-v1",CreditPaymentTerm.Monthly))
        val pending=db.creditMetadataDao().pending().single();db.creditMetadataDao().setHeld(pending.operationUid,true)
        CreditMetadataStore(db).applyAcceptedRevision(CreditTermRevisionEntity("remote",uid,"weekly-default-v1","NET_DAYS",30,"remote-owner","2026-10-15T00:00:00Z"))
        assertTrue(runCatching { facade.creditAction(session,customer,CustomerCreditCommand.Retry(pending.operationUid)) }.isFailure)
        assertTrue(db.creditMetadataDao().pending(pending.operationUid)!!.held)
        facade.creditAction(session,customer,CustomerCreditCommand.DiscardTerm(pending.operationUid))
        assertTrue(db.creditMetadataDao().pending().isEmpty());assertEquals("remote",db.creditMetadataDao().head(uid)!!.revisionUid)
        assertTrue(db.securityAuditDao().all().any { it.action=="credit_proposal_discarded" })
        val input=security.beginUserEnrollment(session,CreateUserInput("finish.admin","Admin","admin"));security.completeUserEnrollment(input.enrollmentToken,"synthetic admin password".toCharArray())
        val admin=security.login("finish.admin","synthetic admin password".toCharArray()).sessionId
        assertNotNull(facade.loadDetail(admin,customer).row.credit)
        assertTrue(runCatching { facade.creditAction(admin,customer,CustomerCreditCommand.Flag("Denied")) }.isFailure)
        assertTrue(runCatching { facade.creditAction(admin,customer,CustomerCreditCommand.Enable) }.isFailure)
    }
}
