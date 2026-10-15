package com.saaf.milkshop

import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

class CustomerStatementTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var session: String
    private var customer=0L
    private var milk=0L
    private var cash=0L
    private val today=LocalDate.parse("2026-10-15")
    @Before fun setup()=runBlocking {
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db)
        session=security.bootstrapOwner("statement.owner","Owner","synthetic statement owner phrase".toCharArray()).session.sessionId
        customer=CustomerManagementService(db).create("Statement Customer")
        StoreIdentityService(db).save(StoreIdentityInput("Configured shop",phone="Shop phone",address="Shop address"))
        val vendor=VendorManagementService(db).create("Supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(vendor,"2026-10-01",100.0,4.0,28.0))
        milk=db.productDao().findByName("Milk")!!.productId
        cash=db.backupDataDao().accounts().single { it.name=="Cash" }.accountId
    }
    @After fun close()=db.close()
    private suspend fun sale(amount: Double,date: String)=SaleService(db).record(SaleInput(milk,date,"credit-retail",100.0,value=amount,payment=SalePayment.OnCredit(customer),notes="PRIVATE STAFF NOTE"))
    private suspend fun read(start: String="2026-10-09",end: String="2026-10-15")=CustomerStatementReader(db,security)
        .load(session,customer,CustomerStatementPeriod(LocalDate.parse(start),LocalDate.parse(end)),today)
    @Test fun openingSalesCorrectionsAndPaymentReconcileWithoutWriting()=runBlocking {
        sale(100.0,"2026-10-02")
        val original=sale(200.0,"2026-10-11")
        CorrectionService(db).correct(CorrectionInput.Sale(original.eventId,"2026-10-12",100.0,value=250.0))
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,50.0,"2026-10-14"))
        val count=db.eventDao().count()
        val statement=read()
        assertEquals(10000L,statement.openingPaisa);assertEquals(30000L,statement.closingPaisa)
        assertEquals(listOf(20000L,5000L,-5000L),statement.lines.map {it.movementPaisa})
        assertEquals(listOf("Sale","Correction / adjustment","Payment received"),statement.lines.map {it.description})
        assertEquals(statement.closingPaisa,statement.openingPaisa+statement.lines.sumOf {it.movementPaisa})
        assertEquals("Configured shop",statement.shopName);assertFalse(statement.toString().contains("PRIVATE STAFF NOTE"))
        assertEquals(count,db.eventDao().count())
        val empty=read("2026-10-15");assertTrue(empty.lines.isEmpty());assertEquals(30000L,empty.openingPaisa)
    }
    @Test fun fractionalRowsUseCumulativePaisaAndDoNotLoseTheAggregate()=runBlocking {
        sale(0.004,"2026-10-10");sale(0.004,"2026-10-10")
        val statement=read()
        assertEquals(1L,statement.closingPaisa);assertEquals(listOf(0L,1L),statement.lines.map {it.movementPaisa})
        assertTrue(statement.lines.last().roundingAdjusted)
        assertEquals(statement.closingPaisa,statement.lines.sumOf {it.movementPaisa})
    }
    @Test fun advanceHistoricalPeriodsAndInvalidDatesStayDistinct()=runBlocking {
        sale(100.0,"2026-10-10")
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,150.0,"2026-10-14"))
        assertEquals(10000L,read(end="2026-10-12").closingPaisa)
        assertEquals(-5000L,read().closingPaisa);assertEquals("Customer advance",read().closingLabel)
        assertTrue(runCatching {read("2026-10-16","2026-10-16")}.isFailure)
        assertTrue(runCatching {read("2026-10-15","2026-10-14")}.isFailure)
        sale(5.0,"2026-10-16")
        assertTrue(runCatching {read()}.isFailure) // Future records cannot be disguised as today's full balance.
        assertEquals(10000L,read(end="2026-10-12").closingPaisa)
    }
    @Test fun customerAndSessionPermissionsAreRechecked()=runBlocking {
        val input=security.beginUserEnrollment(session,CreateUserInput("statement.cashier","Cashier","cashier"))
        security.completeUserEnrollment(input.enrollmentToken,"synthetic cashier phrase".toCharArray())
        val cashier=security.login("statement.cashier","synthetic cashier phrase".toCharArray()).sessionId
        val reader=CustomerStatementReader(db,security)
        val period=CustomerStatementPeriod.lastWeek(today)
        assertTrue(runCatching {reader.load(cashier,customer,period,today)}.isFailure)
        assertTrue(runCatching {reader.load(session,cash,period,today)}.isFailure)
        assertTrue(runCatching {reader.load("not-a-session",customer,period,today)}.isFailure)
        assertEquals(customer,reader.load(session,customer,period,today).customerId)
    }
}
