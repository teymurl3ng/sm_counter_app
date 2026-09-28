package com.saaf.milkshop.sync

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity

/**
 * Phase 6 runtime glue: builds the production transport from the stored
 * config/session/binding and drains the outbox. An expired access token is
 * refreshed once mid-drain; a refused batch surfaces through the engine's
 * halt path (audit entry + notification). Nothing here runs unless the
 * installation is enrolled — an unenrolled device stays exactly as offline
 * as before (D9).
 */
class SyncRuntime(
    private val context: Context,
    private val database: MilkShopDatabase,
    private val http: SyncHttp = UrlConnectionSyncHttp(),
) {

    private val tokenStore = SyncTokenStore(context)

    fun isEnrolled(): Boolean =
        SyncServerConfigStore.load(context) != null &&
            SyncShopBindingStore.load(context) != null &&
            tokenStore.load() != null

    suspend fun drainOnce(): SyncPushEngine.DrainOutcome {
        val config = SyncServerConfigStore.load(context) ?: return SyncPushEngine.DrainOutcome.NotConfigured
        val binding = SyncShopBindingStore.load(context) ?: return SyncPushEngine.DrainOutcome.NotConfigured
        if (tokenStore.load() == null) return SyncPushEngine.DrainOutcome.NotConfigured

        val baseTransport = SyncServerClient(http, config, database, tokenStore) { binding.shopId }
        val notifier = SyncNotifier(context)
        val engine = SyncPushEngine(
            database = database,
            transport = withSessionRefresh(baseTransport, config),
            haltedListener = { reasonCode, message, _ ->
                runCatching { notifier.pushHalted(reasonCode, message) }
            },
        )
        return engine.drainOnce()
    }

    /** One refresh attempt per push when the access token is rejected. */
    private fun withSessionRefresh(
        transport: SyncPushTransport,
        config: SyncServerConfig,
    ): SyncPushTransport = SyncPushTransport { rows: List<SyncOutboxEntity> ->
        val first = transport.push(rows)
        if (first is SyncPushResult.Transient && first.reason == "unauthorized") {
            if (refreshSession(config)) transport.push(rows) else first
        } else {
            first
        }
    }

    private suspend fun refreshSession(config: SyncServerConfig): Boolean {
        val session = tokenStore.load() ?: return false
        val refreshToken = session.refreshToken ?: return false
        return when (val result = AccountAuthClient(http, config).refresh(refreshToken)) {
            is AccountAuthResult.Allowed -> {
                tokenStore.save(
                    SyncTokenStore.Session(result.accessToken, result.refreshToken, result.accessTokenExpiresAtEpochMs),
                )
                result.shopTimezone?.let(BusinessClock::setZone)
                true
            }
            else -> {
                // Sessions are cache material: drop it and let the next
                // enrollment-lifetime flow (or re-enroll) restore access.
                tokenStore.clear()
                SyncScheduler.setSchedulingEnabled(false)
                false
            }
        }
    }
}
