package com.saaf.milkshop.sync

import android.content.Context
import androidx.work.*
import com.saaf.milkshop.MilkShopApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class FollowUpWorker internal constructor(context: Context,params: WorkerParameters,
    private val detect: suspend ()->FollowUpDetection): CoroutineWorker(context,params) {
    constructor(context: Context,params: WorkerParameters): this(context,params,{
        val app=context.applicationContext as MilkShopApplication
        // Separate lock acquisitions: the detector must not recursively acquire the existing sync gate.
        app.syncRuntime.drainOnce();app.syncRuntime.pullOnce()
        app.syncRuntime.detectFollowUps(FollowUpAttentionStore(context),FollowUpNotifier(context))
    })
    override suspend fun doWork(): Result = try {
        if(detect()==FollowUpDetection.RETRY) Result.retry() else Result.success()
    } catch(cancelled: CancellationException) {throw cancelled}
      catch(_: Exception) {Result.retry()}
}

/** Explicit integration only. Startup/resume/accepted-policy hooks stay closed until B's message gate. */
internal object FollowUpScheduler {
    const val PERIODIC="credit_followup_periodic"
    const val KICK="credit_followup_check"
    private val constraints get()=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun periodicRequest()=PeriodicWorkRequestBuilder<FollowUpWorker>(6,TimeUnit.HOURS)
        .setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
    fun configure(context: Context,enabled: Boolean) {
        val work=WorkManager.getInstance(context)
        if(enabled) work.enqueueUniquePeriodicWork(PERIODIC,ExistingPeriodicWorkPolicy.UPDATE,periodicRequest())
        else {work.cancelUniqueWork(PERIODIC);work.cancelUniqueWork(KICK);FollowUpNotifier(context).cancel()}
    }
    fun kick(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(KICK,ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FollowUpWorker>().setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
    }
}
