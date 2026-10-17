package com.saaf.milkshop.application.owner

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.BusinessClock
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CustomerReminder(val customerId: Long,val customerUid: String,val customerName: String,
    val shopName: String,val phone: String?,val balancePaisa: Long,val asOf: String,
    val knownOverduePaisa: Long=0,val partialOverdue: Boolean=false) {
    val message: String? get()=if(balancePaisa<=0) null else
        "Hello $customerName, your outstanding balance with $shopName is ${statementMoney(balancePaisa)} as of $asOf."+
        (if(knownOverduePaisa>0) " ${if(partialOverdue) "Confirmed overdue amount (some records need checking)" else "Overdue amount"}: ${statementMoney(knownOverduePaisa)}." else "")+
        " Please arrange payment according to your agreed terms. If you have already paid, please let us know. Thank you."
}
enum class CommunicationEvent(val label: String) { PREPARED("Draft prepared"),CHOOSER_OPENED("Share menu opened"),COMPOSER_OPENED("Message app opened"),HANDOFF_FAILED("Could not confirm the app opened") }
data class CustomerCommunicationAction(val operationId: String,val customerUid: String,val kind: String,
    val channel: String,val context: String,val event: CommunicationEvent) {
    fun json(): String=JSONObject().put("version",1).put("operation",operationId).put("customer",customerUid)
        .put("kind",kind).put("channel",channel).put("context",context).put("event",event.name).toString()
    fun validate() {
        UUID.fromString(operationId)
        require(customerUid.isNotBlank() && customerUid.length<=200)
        require((kind=="statement" && channel in setOf("PDF","Images") && event!=CommunicationEvent.COMPOSER_OPENED) ||
            (kind=="reminder" && channel in setOf("WhatsApp","SMS") && event!=CommunicationEvent.CHOOSER_OPENED))
        if(kind=="reminder") LocalDate.parse(context) else {
            val dates=context.split('/')
            require(dates.size==2 && LocalDate.parse(dates[0])<=LocalDate.parse(dates[1]))
        }
    }
}
data class CustomerCommunicationHistory(val occurredAt: String,val kind: String,val channel: String,val context: String,val event: CommunicationEvent)

/** Local observable actions only: backup includes them, live business sync does not. */
internal class CustomerCommunicationService(private val db: MilkShopDatabase,private val security: UserSecurityService) {
    private suspend fun authorized(session: String,accountId: Long): Pair<AppUserEntity,AccountEntity> {
        val actor=security.requirePermission(session,AppPermission.VIEW_FULL_FINANCIAL)
        require(AppRole.parse(actor.role) in setOf(AppRole.OWNER,AppRole.MANAGER,AppRole.ADMIN)) {"Sign in as Owner, Admin or Manager to prepare customer messages."}
        val account=requireNotNull(db.accountDao().findById(accountId)) {"Customer is no longer available."}
        require(account.category=="Customer") {"Choose a customer account."}
        return actor to account
    }
    suspend fun reminder(session: String,accountId: Long,today: LocalDate=BusinessClock.today()): CustomerReminder=db.withReadTransaction {
        val (_,account)=authorized(session,accountId)
        val identity=requireNotNull(db.storeProfileDao().get()) {"Set the shop name in Shop settings first."}
        require(identity.storeName.isNotBlank() && identity.currencyCode=="PKR") {"Check the shop name and supported currency in Shop settings."}
        val balance=db.readModelDao().accountBalances().singleOrNull {it.accountId==accountId}?.balance ?: 0.0
        val status=CustomerCreditViews(db).load(mapOf(accountId to balance),today).getValue(accountId).status
        CustomerReminder(accountId,account.uid,account.name.removePrefix("Customer: ").trim(),identity.storeName,
            db.customerProfileDao().findByAccountId(accountId)?.phone,money(balance),today.toString(),status.overduePaisa,status.coverageIssues.isNotEmpty())
    }
    suspend fun record(session: String,accountId: Long,action: CustomerCommunicationAction)=db.withWriteTransaction {
        val (actor,account)=authorized(session,accountId)
        action.validate();require(account.uid==action.customerUid) {"The customer changed. Open this customer again."}
        val details=action.json()
        if(!db.securityAuditDao().hasCommunicationAction(details)) db.securityAuditDao().insert(SecurityAuditEntryEntity(
            occurredAt=Instant.now().toString(),action=ACTION,result="info",reasonCode=account.uid,
            actorUserId=actor.userId,actorNameSnapshot=actor.displayName,actorRoleSnapshot=actor.role,details=details))
        Unit
    }
    suspend fun history(session: String,accountId: Long): List<CustomerCommunicationHistory> = db.withReadTransaction {
        val (_,account)=authorized(session,accountId)
        db.securityAuditDao().customerCommunications(account.uid).mapNotNull {row->runCatching {
            val body=JSONObject(requireNotNull(row.details))
            require(body.getInt("version")==1 && body.getString("customer")==account.uid)
            val action=CustomerCommunicationAction(body.getString("operation"),account.uid,body.getString("kind"),body.getString("channel"),body.getString("context"),CommunicationEvent.valueOf(body.getString("event")))
            action.validate();CustomerCommunicationHistory(row.occurredAt,action.kind,action.channel,action.context,action.event)
        }.getOrNull()}
    }
    companion object {const val ACTION="customer_communication_v1"}
}
