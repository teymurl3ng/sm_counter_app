package com.saaf.milkshop.nativeui.owner

import android.content.Context
import com.saaf.milkshop.application.owner.CustomerReminderMode
import com.saaf.milkshop.application.owner.CustomerSharedHandoffAccess
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.*
import com.saaf.milkshop.statements.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Opaque ordinary-screen bundle; authority stays in current-session readers/RPCs. */
class CustomerFollowUpAccess internal constructor(internal val workspace: FollowUpWorkspaceAccess,
    internal val attention: Flow<FollowUpAttention>,internal val handoff: CustomerSharedHandoffAccess,
    internal val openRequested: StateFlow<Boolean> = MutableStateFlow(false),internal val consumeOpen: ()->Unit = {})

internal fun customerFollowUpAccess(context: Context,db: MilkShopDatabase,security: UserSecurityService,
    runtime: SyncRuntime,session: String,validateCurrent: ()->Unit): CustomerFollowUpAccess {
    val binding=FollowUpWorkspaceBinding(db,security,runtime,session,
        notificationsAllowed={FollowUpNotifier(context).allowed()},hasMessageApp={
            listOf(ReminderIntents.intent(ReminderChannel.SMS,"+923001234567","Review"),
                ReminderIntents.intent(ReminderChannel.WhatsApp,"+923001234567","Review"),
                ReminderIntents.intent(ReminderChannel.WhatsApp,"+923001234567","Review","com.whatsapp.w4b"))
                .any {it.resolveActivity(context.packageManager)!=null}
        })
    suspend fun <T> checked(work: suspend ()->T): T {
        validateCurrent();val result=withContext(Dispatchers.IO) {work()};validateCurrent();return result
    }
    suspend fun <T> changed(work: suspend ()->T): T = try {checked(work)} finally {FollowUpIntegration.changed(context)}
    val workspace=FollowUpWorkspaceAccess(load={checked {binding.load()}},command={c,t->changed {binding.access.command(c,t)}},
        recover={uid,setAside->changed {binding.access.recover(uid,setAside)}},
        reviewRequest={uid,setAside->changed {binding.access.reviewRequest(uid,setAside)}})
    val attention=flow {validateCurrent();runtime.followUpAttention(session,FollowUpAttentionStore(context)).collect {
        validateCurrent();emit(it)
    }}.catch {failure->if(failure is CancellationException) throw failure else emit(FollowUpAttention())}
    val original=CustomerReminderMode(context,db,security,runtime,session,validateCurrent).access
    val handoff=original.copy(open={id,facts,to,channel,launch->changed {original.open(id,facts,to,channel,launch)}},
        recover={id,uid->changed {original.recover(id,uid)}},release={id,uid->changed {original.release(id,uid)}},
        resolve={id,uid,outcome,confirmed->changed {original.resolve(id,uid,outcome,confirmed)}},
        setAside={id,uid,confirmed->changed {original.setAside(id,uid,confirmed)}})
    return CustomerFollowUpAccess(workspace,attention,handoff)
}
