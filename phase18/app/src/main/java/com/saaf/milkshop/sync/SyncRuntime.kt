package com.saaf.milkshop.sync

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity
import com.saaf.milkshop.domain.ServerAccountPolicy
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    companion object {
        /** See [drainOnceLocked] — process-wide drain/pull serialization. */
        private val gate = Mutex()
    }

    fun isEnrolled(): Boolean =
        SyncServerConfigStore.load(context) != null &&
            SyncShopBindingStore.load(context) != null &&
            tokenStore.load() != null

    /** Snapshot for the owner-facing sync screen; null while unenrolled. */
    suspend fun status(): NativeSyncStatus? {
        val config = SyncServerConfigStore.load(context) ?: return null
        val binding = SyncShopBindingStore.load(context) ?: return null
        val roomBinding = database.serverAccountDao().findBinding()
        val link = database.serverAccountDao().identityLinks().firstOrNull()
        val linkedOwner = link?.let { database.appUserDao().findById(it.localUserId) }
        val now = System.currentTimeMillis()
        return NativeSyncStatus(
            environment = config.environment,
            shopName = binding.shopName,
            role = binding.role,
            sessionPresent = tokenStore.load() != null,
            trustedTimeAnchored = TrustedTime.isAnchored(),
            outboxCount = database.syncOutboxDao().count(),
            pullCursor = database.syncPullStateDao().find()?.lastServerSeq ?: 0L,
            holdCount = database.syncRebaselineHoldDao().count(),
            identityStatus = link?.status
                ?: (roomBinding?.status ?: ServerAccountPolicy.BindingStatus.ACTIVE)
                    .let { if (it == ServerAccountPolicy.BindingStatus.ACTIVE) "active_unlinked" else it },
            identityServerUsername = link?.usernameSnapshot,
            identityOwnerName = linkedOwner?.displayName,
            graceDaysLeft = roomBinding?.verifiedAtEpochMs?.let { verifiedAt ->
                val remaining = ServerAccountPolicy.GRACE_DURATION_MS - (now - verifiedAt)
                if (remaining <= 0) 0 else ((remaining + 86_400_000L - 1) / 86_400_000L).toInt()
            },
            graceWarningLevel = ServerAccountPolicy.graceWarningLevel(
                roomBinding?.verifiedAtEpochMs, now,
            ),
        )
    }

    suspend fun drainOnce(): SyncPushEngine.DrainOutcome = gate.withLock {
        drainOnceLocked()
    }

    /**
     * H2 (audit 2026-10-02): every drain/pull entry point in the process
     * serializes on [gate] — the start-up kick, the WorkManager worker, the
     * owner's Sync-now, the debug receiver, and the facade's second runtime
     * instance all funnel through this one gate. Without it, two concurrent
     * 401s fire two refresh POSTs and, under token rotation, the losing
     * refresher could delete the winner's fresh tokens mid-flight. Acquired
     * ONLY at the public entry points: the internals (including
     * refreshSession, which runs under an already-held lock) must never
     * re-acquire — Kotlin's Mutex is not reentrant. PROCESS-WIDE on purpose
     * (companion): NativeOwnerSyncFacade builds its own SyncRuntime instance,
     * and per-instance locks would leave those two draining concurrently.
     */
    private suspend fun drainOnceLocked(): SyncPushEngine.DrainOutcome {
        val config = SyncServerConfigStore.load(context) ?: return SyncPushEngine.DrainOutcome.NotConfigured
        val binding = SyncShopBindingStore.load(context) ?: return SyncPushEngine.DrainOutcome.NotConfigured
        if (tokenStore.load() == null) return SyncPushEngine.DrainOutcome.NotConfigured

        runRestoredRebaselineIfPending()
        runLiveRebaselineIfPending()

        val outcome = engineDrainLocked(config, binding)
        if (outcome != SyncPushEngine.DrainOutcome.NotConfigured) {
            refreshLinkVerificationIfDue()
        }
        return outcome
    }

    private suspend fun engineDrainLocked(
        config: SyncServerConfig,
        binding: SyncShopBinding,
    ): SyncPushEngine.DrainOutcome {
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
     * go 8 classified re-baseline: after a format-5 restore the FIRST drain
     * must NOT push the restored snapshot first (LWW would make stale rows
     * win). Instead: pull-0 to exhaustion with origin collection (the
     * pending-edit guard parks every restored uid's server version, so
     * nothing overwrites the local rows during the pass), then classify the
     * outbox entries against the observed origins (keep/drop/hold), and only
     * then fall through to the normal push. The flag clears only after a
     * completed classification; a failed pull retries pull-first next drain.
     * If the cursor already advanced (crash mid-re-baseline), the flag is
     * cleared and the remaining entries push normally — audited server-side,
     * because the restored device identity is fresh by then.
     */
    private suspend fun runRestoredRebaselineIfPending() {
        val rebaseline = SyncRebaselineStore(context)
        if (!rebaseline.restoredRebaseline) return
        val cursor = database.syncPullStateDao().find()?.lastServerSeq ?: 0L
        if (cursor != 0L) {
            rebaseline.restoredRebaseline = false
            return
        }
        val collector = HashMap<String, SyncRebaseline.SeenServerRow>()
        val pull = pullOnce(collector)
        if (pull !is SyncPullSummary.Done) return
        val report = SyncRebaseline.classifyAndApply(
            database = database,
            seen = collector,
            prevDeviceId = rebaseline.prevDeviceId,
            currentDeviceId = SyncIdentity.currentOrNull().takeIf { it.isNotEmpty() },
        )
        rebaseline.restoredRebaseline = false
        runCatching {
            database.securityAuditDao().insert(
                com.saaf.milkshop.data.SecurityAuditEntryEntity(
                    auditId = 0,
                    occurredAt = java.time.Instant.now().toString(),
                    action = "sync_rebaseline_classified",
                    result = "info",
                    reasonCode = "restored_rebaseline",
                    details = report.describe(),
                ),
            )
        }
    }

    /**
     * go 8 option Z (2026-10-04 owner decision): the FIRST drain after a
     * plain enrollment must NOT blind-push the enqueued history (LWW would
     * let whichever side is newer lose silently — this device after a
     * server rollback, the server after this device's offline gap). The
     * enrollment enqueued everything PRE-HELD; this gate runs the pull-0
     * with body collection (the guard parks every server version, since
     * every local uid has an entry) and classifies: release / drop-equal /
     * hold-differ — then the normal drain pushes only what was released.
     * Pre-held entries must never blind-push, so unlike the restore path
     * the crash fallback RE-RUNS the classification from cursor 0 — it is
     * idempotent: released entries are already unheld, dropped entries are
     * gone, holds re-upsert.
     */
    private suspend fun runLiveRebaselineIfPending() {
        val rebaseline = SyncRebaselineStore(context)
        if (!rebaseline.liveRebaselinePending) return
        val cursor = database.syncPullStateDao().find()?.lastServerSeq ?: 0L
        if (cursor != 0L) {
            database.syncPullStateDao().upsert(
                com.saaf.milkshop.data.SyncPullStateEntity(
                    lastServerSeq = 0,
                    updatedAt = java.time.Instant.now().toString(),
                ),
            )
        }
        val collector = HashMap<String, SyncRebaseline.SeenServerRow>()
        val pull = pullOnce(collector)
        if (pull !is SyncPullSummary.Done) return
        val report = SyncRebaseline.classifyLiveAndApply(database, collector)
        rebaseline.liveRebaselinePending = false
        runCatching {
            database.securityAuditDao().insert(
                com.saaf.milkshop.data.SecurityAuditEntryEntity(
                    auditId = 0,
                    occurredAt = java.time.Instant.now().toString(),
                    action = "sync_rebaseline_classified",
                    result = "info",
                    reasonCode = "live_rebaseline",
                    details = report.describe(),
                ),
            )
        }
    }

    /**
     * Pulls and applies everything the server has beyond the local cursor,
     * batch by batch in server_seq order. Callers drain (push) BEFORE
     * pulling (C1 decision record): a local edit must reach the server
     * before a pull can overwrite it; the apply engine's pending-edit guard
     * and parked rows cover the remaining windows.
     */
    suspend fun pullOnce(): SyncPullSummary = gate.withLock {
        val summary = pullOnce(originCollector = null)
        if (summary is SyncPullSummary.Done) {
            refreshLinkVerificationIfDue()
        }
        summary
    }

    /**
     * H13: a successful authenticated exchange is a live server entitlement
     * proof — refresh the link's verification clock (throttled) so a device
     * that was offline past its grace unblocks as soon as it is genuinely
     * back online. Only the blocked→unblocked transition is audited; routine
     * bumps stay silent. Runs under the H2 gate; never touches refresh
     * (H1) or permission (H5) logic.
     */
    private suspend fun refreshLinkVerificationIfDue() {
        runCatching {
            val now = System.currentTimeMillis()
            // The BINDING clock is authoritative for the grace (Track 1
            // A2'/A6): bump it first, then mirror the bump onto the link row
            // for display. The recovered audit fires on the binding's
            // blocked→unblocked transition.
            var binding = database.serverAccountDao().findBinding()
            if (binding == null) {
                // Clock backfill for devices enrolled before Track 1: the
                // sync-runtime binding file carries shop/staff snapshots;
                // mirror it into the Room row with a FRESH clock so the
                // 14-day degraded-grace window starts from this build.
                SyncShopBindingStore.load(context)?.let { fileBinding ->
                    val nowIso = java.time.Instant.now().toString()
                    database.serverAccountDao().insertBinding(
                        com.saaf.milkshop.data.AccountInstallationBindingEntity(
                            environment = fileBinding.environment,
                            businessId = fileBinding.businessId,
                            shopId = fileBinding.shopId,
                            shopNameSnapshot = fileBinding.shopName ?: fileBinding.shopId,
                            status = ServerAccountPolicy.BindingStatus.ACTIVE,
                            verifiedAtEpochMs = now,
                            createdAt = nowIso,
                            updatedAt = nowIso,
                        ),
                    )
                    binding = database.serverAccountDao().findBinding()
                }
            }
            if (binding != null &&
                ServerAccountPolicy.shouldRefreshBindingVerification(binding.status, binding.verifiedAtEpochMs, now)
            ) {
                val wasBlocked = ServerAccountPolicy.bindingGraceExpired(
                    binding.status, binding.verifiedAtEpochMs, now,
                )
                database.serverAccountDao().updateBindingStatus(
                    binding.bindingId, binding.status, now, java.time.Instant.now().toString(),
                )
                if (wasBlocked) {
                    database.securityAuditDao().insert(
                        com.saaf.milkshop.data.SecurityAuditEntryEntity(
                            auditId = 0,
                            occurredAt = java.time.Instant.now().toString(),
                            action = "server_verification_recovered",
                            result = "info",
                            reasonCode = ServerAccountPolicy.REASON_SERVER_GRACE_RECOVERED,
                            details = "binding=${binding.bindingId}",
                        ),
                    )
                }
            }
            val link = database.serverAccountDao().identityLinks()
                .firstOrNull {
                    ServerAccountPolicy.shouldRefreshVerification(it.status, it.verifiedAtEpochMs, now)
                } ?: return
            val wasBlocked = ServerAccountPolicy.writeBlockedByGrace(link.status, link.verifiedAtEpochMs, now)
            database.serverAccountDao().updateIdentityLinkStatus(
                link.localUserId, link.status, now, link.lastErrorCode,
            )
            if (wasBlocked) {
                database.securityAuditDao().insert(
                    com.saaf.milkshop.data.SecurityAuditEntryEntity(
                        auditId = 0,
                        occurredAt = java.time.Instant.now().toString(),
                        action = "server_verification_recovered",
                        result = "info",
                        reasonCode = ServerAccountPolicy.REASON_SERVER_GRACE_RECOVERED,
                        details = "link=${link.localUserId}",
                    ),
                )
            }
        }
    }

    /**
     * [originCollector], when non-null, receives (table, uid) -> origin for
     * EVERY record the server delivers — the classified re-baseline's raw
     * material (local tables do not store origin_device).
     */
    private suspend fun pullOnce(
        originCollector: MutableMap<String, SyncRebaseline.SeenServerRow>?,
    ): SyncPullSummary {
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
                    originCollector?.let { collector ->
                        outcome.batch.records.forEach { record ->
                            collector[SyncRebaseline.seenKey(record.tableName, record.rowUid)] =
                                SyncRebaseline.SeenServerRow(
                                    record.originDevice,
                                    record.serverSeq,
                                    record.payload,
                                )
                        }
                    }
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
    ): SyncPushTransport = SyncPushTransport { envelopes, rows ->
        val first = transport.push(envelopes, rows)
        if (first is SyncPushResult.Transient && first.reason == "unauthorized") {
            if (refreshSession(config)) transport.push(envelopes, rows) else first
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
                // H1 (audit 2026-10-02; spec §5 "UNKNOWN never revoke"): only
                // a DEFINITIVE verdict clears the session — revoked account,
                // rejected credential, or a server-stated dead refresh
                // token. Transient failures (timeout, 5xx, rate limit, any
                // unrecognized rejection) keep the tokens; the next drain
                // retries and this same decision runs again. Scheduling is
                // deliberately left enabled: warmUp re-enables it on app
                // start anyway, and a worker that finds no session no-ops.
                if (RefreshDisposition.from(result) == RefreshDisposition.CLEAR) {
                    tokenStore.clear()
                }
                // Track 1 A1' — verdicts RECORD + DISPLAY only while
                // ENFORCE_LINK_STATES is false: a server disable is a
                // SUSPENDED entitlement, a rejected credential or dead
                // refresh is REVOKED. Enforcement ships behind the
                // policy flag, not here.
                runCatching {
                    val newStatus = when (result) {
                        is AccountAuthResult.Disabled ->
                            ServerAccountPolicy.LinkStatus.SUSPENDED
                        is AccountAuthResult.InvalidCredentials,
                        is AccountAuthResult.NotAuthorized,
                        is AccountAuthResult.RefreshTokenInvalid,
                        -> ServerAccountPolicy.LinkStatus.REVOKED
                        else -> null
                    }
                    if (newStatus != null) {
                        val link = database.serverAccountDao().identityLinks().firstOrNull()
                        if (link != null && link.status != newStatus) {
                            database.serverAccountDao().updateIdentityLinkStatus(
                                link.localUserId, newStatus, link.verifiedAtEpochMs, "server_verdict",
                            )
                        }
                    }
                }
                false
            }
        }
    }
}
