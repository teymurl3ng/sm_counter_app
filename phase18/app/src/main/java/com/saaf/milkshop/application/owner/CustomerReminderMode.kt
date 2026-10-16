package com.saaf.milkshop.application.owner

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.statements.ReminderIntents
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** No cached/unknown response can silently turn coordinated contact into standalone contact. */
internal class CustomerReminderMode(private val context: Context,private val db: MilkShopDatabase,
    private val security: UserSecurityService,private val runtime: SyncRuntime,private val session: String,
    private val validateCurrent: ()->Unit={},private val shared: CustomerSharedHandoffAccess=CustomerSharedHandoff(db,security,runtime,session).access) {
    val access=CustomerSharedHandoffAccess(::load,::open,
        {id,request->checked {shared.recover(id,request)}},{id,attempt->checked {shared.release(id,attempt)}},
        {id,attempt,outcome,ack->checked {shared.resolve(id,attempt,outcome,ack)}},
        {id,request,ack->checked {shared.setAside(id,request,ack)}})
    private suspend fun authorize() {
        validateCurrent()
        val actor=security.requirePermission(session,AppPermission.VIEW_FULL_FINANCIAL)
        check(AppRole.parse(actor.role) in setOf(AppRole.OWNER,AppRole.ADMIN,AppRole.MANAGER))
    }
    private suspend fun <T> checked(work: suspend ()->T): T {authorize();val result=work();authorize();return result}
    private suspend fun connected()=SyncShopBindingStore.load(context)!=null||SyncTokenStore(context).load()!=null||
        SyncRebaselineStore(context).needsFreshIdentity||db.followUpDataDao().commands().isNotEmpty()
    /** Null means use the coordinated adapter outside this gate; Kotlin's mutex is not reentrant. */
    private suspend fun manualOrNull(id: Long,manual: suspend (Boolean)->CustomerSharedHandoffState): CustomerSharedHandoffState? {
        authorize()
        if(!connected()) {authorize();return manual(true)}
        return runtime.withFollowUpSession(session) {a->
            val response=FollowUpDataStore(db).refresh(a.binding,a.call)
            check(response is FollowUpLoad.Loaded) {"Connect to check message settings before opening a messaging app."}
            val r=response.snapshot
            val uid=db.accountDao().findById(id)?.uid ?: error("Open this customer again.")
            val pending=db.followUpDataDao().commands().any {it.bindingKey==a.binding.key}
            if(r.policy?.enabled==true||r.claims.any {it.customerUid==uid}||pending) null
            else {authorize();a.validate();manual(false)}
        }
    }
    private suspend fun load(id: Long)=manualOrNull(id) {CustomerSharedHandoffState()} ?: checked {shared.load(id)}
    private suspend fun open(id: Long,reviewed: CustomerReminder,phone: String,channel: String,launch: suspend ()->Unit): CustomerSharedHandoffState {
        require(channel in setOf("SMS","WhatsApp"));ReminderIntents.phone(phone)
        val result=manualOrNull(id) {standalone->
            val current=CustomerCommunicationService(db,security).reminder(session,id)
            check(current==reviewed&&current.message!=null) {"Records changed. Refresh the message and check the recipient again."}
            check(db.customerProfileDao().findByAccountId(id)?.active!=false) {"This customer is inactive."}
            authorize();check(!standalone||!connected()) {"Message settings changed. Refresh before opening."}
            currentCoroutineContext().ensureActive();launch()
            CustomerSharedHandoffState()
        }
        return result ?: checked {shared.open(id,reviewed,phone,channel,launch)}
    }
}
