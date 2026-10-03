package com.saaf.milkshop.application.owner

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.NativeSyncHold
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

    /** go 8: restored entries held out of the push for an owner decision. */
    suspend fun holds(): List<NativeSyncHold> =
        database.syncRebaselineHoldDao().all().map {
            NativeSyncHold(it.tableName, it.rowUid, it.originDevice, it.heldAt)
        }

    /**
     * go 8: rules on a held restored entry. keep-mine releases the entry back
     * into the push (the local row still holds its backup-era values — the
     * park never applied — so this pushes and is audited server-side);
     * take-server deletes the entry, unblocking its park so the next pull
     * applies the server version over the local row.
     */
    suspend fun resolveHold(tableName: String, rowUid: String, keepMine: Boolean) {
        if (keepMine) {
            database.syncOutboxDao().setHeld(tableName, rowUid, false)
        } else {
            database.syncOutboxDao().deleteEntry(tableName, rowUid)
        }
        database.syncRebaselineHoldDao().delete(tableName, rowUid)
    }
}
