package com.saaf.milkshop.application.owner

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.NativeSyncHold
import com.saaf.milkshop.sync.NativeSyncStatus
import com.saaf.milkshop.sync.SyncEndpoints
import com.saaf.milkshop.sync.SyncEnrollment
import com.saaf.milkshop.sync.SyncHoldLabels
import com.saaf.milkshop.sync.SyncPushEngine
import com.saaf.milkshop.sync.SyncRuntime
import com.saaf.milkshop.sync.SyncServerConfig
import com.saaf.milkshop.sync.SyncOperationGate
import com.saaf.milkshop.sync.SyncApplyEngine
import com.saaf.milkshop.sync.SyncPulledRecord
import androidx.room3.withWriteTransaction
import org.json.JSONObject
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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

    /** Local review truth, independent of transport configuration or notification access. */
    fun observeReviewCount(sessionHandle: String, security: UserSecurityService) =
        database.syncRebaselineHoldDao().observeCount().onEach {
            requireReviewOwner(sessionHandle, security)
        }

    fun observeReviewHolds(sessionHandle: String, security: UserSecurityService) =
        database.syncRebaselineHoldDao().observeAll().map { entries ->
            requireReviewOwner(sessionHandle, security)
            entries.map { entry ->
                NativeSyncHold(entry.tableName, entry.rowUid, entry.originDevice, entry.heldAt,
                    SyncHoldLabels.holdLabel(entry.tableName, entry.payloadJson, entry.rowUid))
            }
        }

    private suspend fun requireReviewOwner(sessionHandle: String, security: UserSecurityService) {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        require(AppRole.parse(actor.role) == AppRole.OWNER) { "Sync review is only available to Owner." }
    }

    suspend fun enroll(
        username: String,
        credential: CharArray,
        shopId: String?,
        environment: String = SyncServerConfig.ENVIRONMENT_PROD,
        localUserId: Long? = null,
    ): SyncEnrollment.Outcome = SyncEnrollment(appContext, database = database)
        .enroll(SyncEndpoints.forEnvironment(environment), username, credential, shopId, localUserId)

    suspend fun unenroll() {
        SyncEnrollment(appContext, database = database).unenroll()
    }

    /** Pulls everything new from the server and applies it locally. */
    suspend fun pull(): SyncRuntime.SyncPullSummary = runtime.pullOnce()

    /** Drains the outbox to the server. */
    suspend fun push(): SyncPushEngine.DrainOutcome = runtime.drainOnce()

    internal suspend fun enableCreditContract() = runtime.enableCreditContract()

    /** go 8: restored entries held out of the push for an owner decision. */
    suspend fun holds(): List<NativeSyncHold> =
        database.syncRebaselineHoldDao().all().map {
            NativeSyncHold(
                tableName = it.tableName,
                rowUid = it.rowUid,
                originDevice = it.originDevice,
                heldAt = it.heldAt,
                label = SyncHoldLabels.holdLabel(it.tableName, it.payloadJson, it.rowUid),
            )
        }

    /**
     * go 8: rules on a held restored entry. keep-mine releases the entry back
     * into the push (the local row still holds its backup-era values — the
     * park never applied — so this pushes and is audited server-side);
     * take-server deletes the entry, unblocking its park so the next pull
     * applies the server version over the local row.
     */
    suspend fun resolveHold(tableName: String, rowUid: String, keepMine: Boolean) = SyncOperationGate.exclusive {
        database.withWriteTransaction {
            if (keepMine) {
                database.syncOutboxDao().setHeld(tableName, rowUid, false)
            } else {
                val park = requireNotNull(database.syncParkedRowDao().find(tableName, rowUid)) {
                    "The server version is unavailable. Sync again before resolving this item."
                }
                database.syncOutboxDao().deleteEntry(tableName, rowUid)
                SyncApplyEngine(database).apply(listOf(SyncPulledRecord(
                    park.serverSeq, null, tableName, rowUid, JSONObject(park.payloadJson), null,
                )))
                require(database.syncParkedRowDao().find(tableName, rowUid) == null) {
                    "The server version cannot be applied yet. The review item has been retained."
                }
            }
            database.syncRebaselineHoldDao().delete(tableName, rowUid)
        }
    }
}
