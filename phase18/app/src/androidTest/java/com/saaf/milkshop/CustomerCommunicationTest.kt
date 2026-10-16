package com.saaf.milkshop

import android.content.Intent
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.statements.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate
import java.util.UUID

class CustomerCommunicationTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var service: CustomerCommunicationService
    private lateinit var session: String
    private var customer=0L
    private var cash=0L
    private lateinit var uid: String
    private val today=LocalDate.parse("2026-10-16")
    @Before fun setup()=runBlocking {
        db=MilkShopDatabase.createInMemory();security=UserSecurityService(db);service=CustomerCommunicationService(db,security)
        session=security.bootstrapOwner("reminder.owner","Owner","synthetic reminder phrase".toCharArray()).session.sessionId
        customer=CustomerManagementService(db).create("Amina & Sons امینہ");uid=db.accountDao().findById(customer)!!.uid
        CustomerProfileService(db).save(customer,CustomerProfileInput(phone="+92 300 1234567",notes="PRIVATE CUSTOMER NOTE"))
        StoreIdentityService(db).save(StoreIdentityInput("Configured & Shop"))
        val vendor=VendorManagementService(db).create("Supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(vendor,"2026-10-01",100.0,4.0,28.0))
        cash=db.backupDataDao().accounts().single {it.name=="Cash"}.accountId
        db.creditMetadataDao().saveState(CreditMetadataSyncStateEntity(shopUid="synthetic",enabled=true,contractVersion=1))
    }
    @After fun close()=db.close()
    private suspend fun sale()=SaleService(db).record(SaleInput(db.productDao().findByName("Milk")!!.productId,"2026-10-01","credit-retail",100.0,value=200.0,payment=SalePayment.OnCredit(customer),notes="PRIVATE STAFF NOTE"))
    private fun action(event: CommunicationEvent=CommunicationEvent.PREPARED)=CustomerCommunicationAction(UUID.randomUUID().toString(),uid,"reminder","SMS",today.toString(),event)
    @Test fun reminderUsesCurrentBalanceAndKnownOverdueWithoutLeakingPrivateFacts()=runBlocking {
        sale();CreditCollectionService(db).record(CreditCollectionInput(customer,cash,50.0,"2026-10-15"))
        val events=db.eventDao().count();val audit=db.securityAuditDao().all().size
        val facts=service.reminder(session,customer,today)
        assertEquals(15000L,facts.balancePaisa);assertEquals(15000L,facts.knownOverduePaisa)
        assertTrue(facts.message!!.contains("PKR 150.00"));assertTrue(facts.message!!.contains("Configured & Shop"))
        assertFalse(facts.toString().contains("PRIVATE"));assertFalse(facts.message!!.contains("Bad"))
        assertEquals(events,db.eventDao().count());assertEquals(audit,db.securityAuditDao().all().size)
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,150.0,"2026-10-16"))
        assertNull(service.reminder(session,customer,today).message)
        CreditCollectionService(db).record(CreditCollectionInput(customer,cash,10.0,"2026-10-16"))
        val advance=service.reminder(session,customer,today);assertEquals(-1000L,advance.balancePaisa);assertNull(advance.message)
    }
    @Test fun explicitPhoneAndUnicodeMessagesAreEncodedForOneRecipientOnly() {
        assertEquals("+923001234567",ReminderIntents.phone(" +92 (300) 123-4567 "))
        listOf("03001234567","00923001234567","+92 300; +92 301","+923001234567 ext 1","+۹۲۳۰۰۱۲۳۴۵۶۷","+012345678","+123\n45678").forEach {
            assertTrue(it,runCatching {ReminderIntents.phone(it)}.isFailure)
        }
        val text="Amina & Sons امینہ: PKR 150.00? paid + due #1\nThank you"
        val whatsapp=ReminderIntents.intent(ReminderChannel.WhatsApp,"+923001234567",text)
        assertEquals("com.whatsapp",whatsapp.`package`);assertEquals("wa.me",whatsapp.data!!.host)
        assertEquals("/923001234567",whatsapp.data!!.path);assertEquals(text,whatsapp.data!!.getQueryParameter("text"))
        val sms=ReminderIntents.intent(ReminderChannel.SMS,"+923001234567",text)
        assertEquals(Intent.ACTION_SENDTO,sms.action);assertEquals("smsto",sms.data!!.scheme)
        assertEquals("+923001234567",sms.data!!.schemeSpecificPart);assertEquals(text,sms.getStringExtra("sms_body"));assertNull(sms.type)
        assertFalse(sms.hasExtra(Intent.EXTRA_STREAM))
    }
    @Test fun historyIsMinimalIdempotentCustomerBoundedAndBackupCompatible()=runBlocking {
        val eventCount=db.eventDao().count();val op=action()
        service.record(session,customer,op);service.record(session,customer,op)
        service.record(session,customer,op.copy(event=CommunicationEvent.COMPOSER_OPENED))
        assertEquals(2,service.history(session,customer).size)
        val other=CustomerManagementService(db).create("Other customer")
        assertTrue(service.history(session,other).isEmpty())
        assertTrue(runCatching {service.record(session,other,op)}.isFailure)
        val rows=db.securityAuditDao().customerCommunications(uid)
        assertFalse(rows.toString().contains("1234567"));assertFalse(rows.toString().contains("PKR"));assertFalse(rows.toString().contains("PRIVATE"))
        val restored=readSecurityAuditEntries(JSONArray(rows.map(::securityAuditEntryJson)))
        assertEquals(rows,restored)
        repeat(55) {service.record(session,customer,action())}
        assertEquals(50,service.history(session,customer).size);assertEquals(eventCount,db.eventDao().count())
    }
    @Test fun permissionsAndActionVocabularyAreEnforced()=runBlocking {
        val input=security.beginUserEnrollment(session,CreateUserInput("reminder.cashier","Cashier","cashier"))
        security.completeUserEnrollment(input.enrollmentToken,"synthetic cashier phrase".toCharArray())
        val cashier=security.login("reminder.cashier","synthetic cashier phrase".toCharArray()).sessionId
        for(handle in listOf(cashier,"invalid-session")) {
            assertTrue(runCatching {service.reminder(handle,customer,today)}.isFailure)
            assertTrue(runCatching {service.record(handle,customer,action())}.isFailure)
            assertTrue(runCatching {service.history(handle,customer)}.isFailure)
        }
        assertTrue(runCatching {service.reminder(session,cash,today)}.isFailure)
        assertTrue(runCatching {service.record(session,customer,action().copy(channel="automatic SMS"))}.isFailure)
        assertTrue(runCatching {service.record(session,customer,action().copy(kind="statement"))}.isFailure)
    }
}
