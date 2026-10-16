package com.saaf.milkshop

import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

class FollowUpReaderTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var session: String
    private var customer=0L
    @Before fun setup(): Unit=runBlocking {
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db)
        session=security.bootstrapOwner("followup.owner","Owner","synthetic followup owner phrase".toCharArray()).session.sessionId
        customer=CustomerManagementService(db).create("Cafe")
        CustomerProfileService(db).save(customer,CustomerProfileInput(phone="+923001234567",notes="PRIVATE CUSTOMER NOTE"))
        val supplier=VendorManagementService(db).create("Supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(supplier,"2026-10-01",100.0,4.0,28.0))
        db.creditMetadataDao().saveState(CreditMetadataSyncStateEntity(shopUid="synthetic",enabled=true,contractVersion=1))
        SaleService(db).record(SaleInput(db.productDao().findByName("Milk")!!.productId,"2026-10-01","credit-retail",100.0,value=200.0,payment=SalePayment.OnCredit(customer)))
    }
    @After fun close()=db.close()
    @Test fun canonicalFactsExcludePrivateNotesAndReflectPaymentsWithoutWrites()=runBlocking {
        val reader=CustomerFollowUpReader(db,security);val date=LocalDate.parse("2026-10-18")
        val count=db.eventDao().count();val audit=db.securityAuditDao().all().size
        val row=reader.load(session,date).single()
        assertEquals(customer,row.accountId);assertEquals(20000L,row.balancePaisa);assertEquals(20000L,row.status!!.overduePaisa)
        assertFalse(row.toString().contains("PRIVATE"));assertEquals(count,db.eventDao().count());assertEquals(audit,db.securityAuditDao().all().size)
        val cash=db.backupDataDao().accounts().single {it.name=="Cash"}.accountId
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,225.0,"2026-10-18"))
        val paid=reader.load(session,date).single();assertEquals(-2500L,paid.balancePaisa)
        assertNull(CustomerFollowUps.evaluate(paid,FollowUpPolicy("v1",date,true),null,FollowUpAcknowledgment(),date))
    }
    @Test fun cashierAndExpiredSessionCannotReadFollowUps()=runBlocking {
        val token=security.beginUserEnrollment(session,CreateUserInput("followup.cashier","Cashier","cashier"))
        security.completeUserEnrollment(token.enrollmentToken,"synthetic cashier phrase".toCharArray())
        val cashier=security.login("followup.cashier","synthetic cashier phrase".toCharArray()).sessionId
        val reader=CustomerFollowUpReader(db,security)
        assertTrue(runCatching {reader.load(cashier,LocalDate.parse("2026-10-18"))}.isFailure)
        assertTrue(runCatching {reader.load("invalid",LocalDate.parse("2026-10-18"))}.isFailure)
    }
}
