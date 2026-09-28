package com.saaf.milkshop.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.saaf.milkshop.MilkShopApplication
import java.util.concurrent.TimeUnit

/**
 * Push activation (sync Phase 6): every committed write kicks this worker
 * through SyncScheduler; it drains the outbox in strict order whenever a
 * connection exists. Transient failures retry with backoff; a halt reports
 * to the owner and waits for the next kick. Cashier entry never waits on
 * any of it (D9) — the worker is entirely background.
 */
class SyncPushWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MilkShopApplication ?: return Result.success()
        return when (app.syncRuntime.drainOnce()) {
            SyncPushEngine.DrainOutcome.NotConfigured,
            is SyncPushEngine.DrainOutcome.Drained,
            is SyncPushEngine.DrainOutcome.Halted,
            -> Result.success()

            is SyncPushEngine.DrainOutcome.RetryLater -> Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "sync_push_drain"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncPushWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
