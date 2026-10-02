package com.saaf.milkshop.sync

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase

/**
 * Enrollment (Account Bridge spec §6, minimal sync form): prove the server
 * credential online once, pick the shop this installation pushes as, then
 * persist config + session + binding and switch the push scheduler on.
 * Nothing is saved on any non-definitive outcome (UNKNOWN never creates
 * state). The interactive owner screen comes later; the debug build drives
 * this through an adb receiver for now.
 */
class SyncEnrollment(
    private val context: Context,
    private val http: SyncHttp = UrlConnectionSyncHttp(),
    private val database: MilkShopDatabase? = null,
) {

    sealed interface Outcome {
        data class Enrolled(
            val binding: SyncShopBinding,
            val shopTimezone: String?,
        ) : Outcome

        /** shop_id was omitted and the account sees several shops: pick one. */
        data class ShopsAvailable(val shops: List<AuthShop>) : Outcome

        data class Failed(val reason: String) : Outcome
    }

    suspend fun enroll(
        config: SyncServerConfig,
        username: String,
        credential: CharArray,
        shopId: String? = null,
    ): Outcome {
        val auth = when (val result = AccountAuthClient(http, config).signIn(username, credential)) {
            is AccountAuthResult.Allowed -> result
            AccountAuthResult.InvalidCredentials -> return Outcome.Failed("invalid_credentials")
            AccountAuthResult.Disabled -> return Outcome.Failed("disabled")
            AccountAuthResult.NotAuthorized -> return Outcome.Failed("not_authorized")
            is AccountAuthResult.Unknown -> return Outcome.Failed("unknown:${result.reason}")
        }
        // C1/M9: the enrollment's account is this installation's push
        // identity for every local user, so it must hold the role the sync
        // contract requires — owner (ACCOUNT_BRIDGE_PHASE1_SPEC: "Enrollment
        // requires ALLOWED, role owner"; master-data write gating server-side
        // assumes it).
        if (auth.role != "owner") {
            return Outcome.Failed("owner_enrollment_required")
        }

        val target = shopId?.let { wanted -> auth.shops.firstOrNull { it.shopId == wanted } }
            ?: auth.shops.singleOrNull()
            ?: return if (auth.shops.isEmpty()) {
                Outcome.Failed("no_shops")
            } else {
                Outcome.ShopsAvailable(auth.shops)
            }

        SyncServerConfigStore.save(context, config)
        SyncTokenStore(context).save(
            SyncTokenStore.Session(auth.accessToken, auth.refreshToken, auth.accessTokenExpiresAtEpochMs),
        )
        val binding = SyncShopBinding(
            environment = config.environment,
            businessId = auth.businessId,
            shopId = target.shopId,
            shopName = target.shopName,
            staffId = auth.staffId,
            role = auth.role,
        )
        SyncShopBindingStore.save(context, binding)
        (target.timezone ?: auth.shopTimezone)?.let(BusinessClock::setZone)
        // Re-baseline for the newly enrolled server: the complete local
        // history is enqueued (idempotent — rows already queued or already
        // on the server change nothing), and the pull cursor restarts at
        // zero so the device converges with the server from the beginning.
        database?.let { db ->
            SyncBaseline.enqueueAll(db)
            db.syncPullStateDao().upsert(
                com.saaf.milkshop.data.SyncPullStateEntity(
                    lastServerSeq = 0,
                    updatedAt = java.time.Instant.now().toString(),
                ),
            )
        }
        SyncScheduler.setSchedulingEnabled(true)
        SyncScheduler.requestPush()
        return Outcome.Enrolled(binding, target.timezone ?: auth.shopTimezone)
    }

    fun unenroll() {
        SyncServerConfigStore.clear(context)
        SyncShopBindingStore.clear(context)
        SyncTokenStore(context).clear()
        SyncScheduler.setSchedulingEnabled(false)
    }
}
