package com.saaf.milkshop.sync

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** Read-only detection kicks. No cached policy or local login authorizes an alert. */
internal object FollowUpIntegration {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val changes=Channel<Context>(Channel.CONFLATED)
    init {scope.launch {for(context in changes) runCatching {reconcile(context)}}}
    fun changed(context: Context) {
        if(!SyncScheduler.isSuppressedForTest()) changes.trySend(context.applicationContext)
    }
    internal suspend fun reconcile(context: Context,kick: ()->Unit={FollowUpScheduler.kick(context)},
        stop: ()->Unit={FollowUpScheduler.configure(context,false)}) = SyncOperationGate.exclusive {
        val store=FollowUpAttentionStore(context)
        // Any recorded change can make an earlier count/notification obsolete, even after a binding switch.
        store.invalidateAll()
        FollowUpNotifier(context).cancel()
        val config=SyncServerConfigStore.load(context)
        val binding=SyncShopBindingStore.load(context)
        val connected=config!=null&&binding!=null&&config.environment==binding.environment&&
            binding.role in setOf("owner","admin","manager")&&SyncTokenStore(context).load()!=null&&
            !SyncRebaselineStore(context).needsFreshIdentity
        if(connected) kick() else {store.invalidateAll(clear=true);stop()}
    }
    internal fun checked(context: Context,enabled: Boolean) {
        if(!SyncScheduler.isSuppressedForTest()) FollowUpScheduler.periodic(context,enabled)
    }
    internal fun afterDetection(outcome: FollowUpDetection,configure: (Boolean)->Unit) {
        when(outcome) {
            FollowUpDetection.STOPPED->configure(false)
            FollowUpDetection.CHECKED->configure(true)
            FollowUpDetection.RETRY->Unit // Temporary failures retain the existing periodic retry.
        }
    }
}
