package com.saaf.milkshop.application.owner

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.NativeSyncStatus
import com.saaf.milkshop.sync.SyncEndpoints
import com.saaf.milkshop.sync.SyncEnrollment
import com.saaf.milkshop.sync.SyncPushEngine
import com.saaf.milkshop.sync.SyncRuntime
import com.saaf.milkshop.sync.SyncServerConfig

/**
 * Owner-facing sync enrollment and status (sync Phase 8.6 cutover). Wraps
 * the sync runtime behind the same facade pattern as the other owner
 * features. Release builds are PROD-only (spec §6); debug builds may pick
 * DEV for testing.
 */
class NativeOwnerSyncFacade(
    context: Context,
    private val database: MilkShopDatabase,
) {
    private val appContext = context.applicationContext
    private val runtime = SyncRuntime(appContext, database)

    fun isEnrolled(): Boolean = runtime.isEnrolled()

    suspend fun status(): NativeSyncStatus? = runtime.status()

    suspend fun enroll(
        username: String,
        credential: CharArray,
        shopId: String?,
        environment: String = SyncServerConfig.ENVIRONMENT_PROD,
    ): SyncEnrollment.Outcome = SyncEnrollment(appContext, database = database)
        .enroll(SyncEndpoints.forEnvironment(environment), username, credential, shopId)

    fun unenroll() {
        SyncEnrollment(appContext, database = database).unenroll()
    }

    /** Pulls everything new from the server and applies it locally. */
    suspend fun pull(): SyncRuntime.SyncPullSummary = runtime.pullOnce()

    /** Drains the outbox to the server. */
    suspend fun push(): SyncPushEngine.DrainOutcome = runtime.drainOnce()
}
