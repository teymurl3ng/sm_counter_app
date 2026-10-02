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

    /** Snapshot for the owner-facing sync screen; null while unenrolled. */
    suspend fun status(): NativeSyncStatus? {
        val config = SyncServerConfigStore.load(context) ?: return null
        val binding = SyncShopBindingStore.load(context) ?: return null
        return NativeSyncStatus(
            environment = config.environment,
            shopName = binding.shopName,
            role = binding.role,
            sessionPresent = tokenStore.load() != null,
            trustedTimeAnchored = TrustedTime.isAnchored(),
            outboxCount = database.syncOutboxDao().count(),
            pullCursor = database.syncPullStateDao().find()?.lastServerSeq ?: 0L,
        )
    }

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

    /**
     * Pulls and applies everything the server has beyond the local cursor,
     * batch by batch in server_seq order. Callers drain (push) BEFORE
     * pulling (C1 decision record): a local edit must reach the server
     * before a pull can overwrite it; the apply engine's pending-edit guard
     * and parked rows cover the remaining windows.
     */
    suspend fun pullOnce(): SyncPullSummary {
        val config = SyncServerConfigStore.load(context) ?: return SyncPullSummary.NotEnrolled
        val binding = SyncShopBindingStore.load(context) ?: return SyncPullSummary.NotEnrolled
        if (tokenStore.load() == null) return SyncPullSummary.NotEnrolled

        val client = SyncPullClient(http, config, tokenStore)
        val applyEngine = SyncApplyEngine(database)
        var applied = 0
        var updated = 0
        var skipped = 0
        var parked = 0
        var conflicts = 0

        while (true) {
            val cursor = database.syncPullStateDao().find()?.lastServerSeq ?: 0L
            var outcome = client.pull(cursor, maxRows = 100, shopId = binding.shopId)
            // Pull shares the push path's one-refresh rule: an expired access
            // token is replaced once, then the attempt retries.
            if (outcome is SyncPullOutcome.RetryLater && outcome.reason == "unauthorized") {
                if (refreshSession(config)) {
                    outcome = client.pull(cursor, maxRows = 100, shopId = binding.shopId)
                }
            }
            when (outcome) {
                is SyncPullOutcome.Fetched -> {
                    if (outcome.batch.records.isEmpty()) return SyncPullSummary.Done(applied, updated, skipped, parked, conflicts)
                    val report = applyEngine.apply(outcome.batch.records)
                    applied += report.applied
                    updated += report.updated
                    skipped += report.skipped
                    parked += report.parked
                    conflicts += report.conflicts
                    database.syncPullStateDao().upsert(
                        com.saaf.milkshop.data.SyncPullStateEntity(
                            lastServerSeq = outcome.batch.latestServerSeq,
                            updatedAt = java.time.Instant.now().toString(),
                        ),
                    )
                    if (!outcome.hasMore) return SyncPullSummary.Done(applied, updated, skipped, parked, conflicts)
                }
                SyncPullOutcome.NotConfigured -> return SyncPullSummary.NotEnrolled
                is SyncPullOutcome.RetryLater -> {
                    return if (applied + updated > 0) {
                        SyncPullSummary.Done(applied, updated, skipped, parked, conflicts)
                    } else {
                        SyncPullSummary.RetryLater(outcome.reason)
                    }
                }
            }
        }
    }

    sealed interface SyncPullSummary {
        data class Done(
            val applied: Int,
            val updated: Int,
            val skipped: Int,
            val parked: Int,
            val conflicts: Int,
        ) : SyncPullSummary
        data class RetryLater(val reason: String) : SyncPullSummary
        data object NotEnrolled : SyncPullSummary
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
