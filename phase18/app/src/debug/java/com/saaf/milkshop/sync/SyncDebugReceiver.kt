package com.saaf.milkshop.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.saaf.milkshop.data.MilkShopDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only enrollment/status surface for the preview build, so the sync
 * runtime can be driven from a phone with adb before the owner-facing
 * enrollment screen exists. Never compiled into release.
 *
 *   adb shell am broadcast -a com.saaf.milkshop.sync.ENROLL \
 *     --es username <dev-username> --es credential <15-digit> \
 *     [--es shop_id <uuid>] [--es base_url ...] [--es publishable_key ...]
 *   adb shell am broadcast -a com.saaf.milkshop.sync.STATUS
 *   adb shell am broadcast -a com.saaf.milkshop.sync.UNENROLL
 *
 * Watch with: adb logcat -s SaafSync
 */
class SyncDebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_ENROLL -> enroll(context, intent)
                    ACTION_STATUS -> status(context)
                    ACTION_DRAIN -> drain(context)
                    ACTION_UNENROLL -> unenroll(context)
                    ACTION_SNAPSHOT -> snapshot(context)
                    ACTION_RESTORE_LATEST -> restoreLatest(context)
                    else -> Log.i(TAG, "Unknown action: ${intent.action}")
                }
            } catch (error: Throwable) {
                Log.i(TAG, "Command failed: ${error.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun enroll(context: Context, intent: Intent) {
        val username = intent.getStringExtra(EXTRA_USERNAME)
        val credential = intent.getStringExtra(EXTRA_CREDENTIAL)
        if (username.isNullOrBlank() || credential.isNullOrBlank()) {
            Log.i(TAG, "ENROLL needs --es $EXTRA_USERNAME and --es $EXTRA_CREDENTIAL")
            return
        }
        val config = SyncServerConfig(
            environment = intent.getStringExtra(EXTRA_ENVIRONMENT) ?: SyncServerConfig.ENVIRONMENT_DEV,
            baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: DEFAULT_DEV_BASE_URL,
            publishableKey = intent.getStringExtra(EXTRA_PUBLISHABLE_KEY) ?: DEFAULT_DEV_PUBLISHABLE_KEY,
        )
        Log.i(TAG, "Enrolling $username against ${config.baseUrl} (${config.environment})…")
        val app = context.applicationContext as? com.saaf.milkshop.MilkShopApplication
        when (
            val outcome = SyncEnrollment(
                context,
                database = app?.database,
            ).enroll(
                config,
                username,
                credential.toCharArray(),
                intent.getStringExtra(EXTRA_SHOP_ID)?.takeIf { it.isNotBlank() },
            )
        ) {
            is SyncEnrollment.Outcome.Enrolled -> Log.i(
                TAG,
                "ENROLLED as ${outcome.binding.role} for shop '${outcome.binding.shopName}' " +
                    "(${outcome.binding.shopId}); timezone=${outcome.shopTimezone ?: "unset"}. " +
                    "Push is live — record entries in the app and watch sync_records on DEV.",
            )
            is SyncEnrollment.Outcome.OwnersAvailable -> {
                Log.i(TAG, "Several local owners — re-run with an explicit --es owner pick " +
                    "through the app's Sync screen, or keep only one active owner: " +
                    outcome.owners.joinToString { it.displayName })
            }
            is SyncEnrollment.Outcome.ShopsAvailable -> {
                Log.i(TAG, "Pick one shop and re-run with --es shop_id <uuid>:")
                outcome.shops.forEach { Log.i(TAG, "  ${it.shopId}  ${it.shopName ?: "(unnamed)"}") }
            }
            is SyncEnrollment.Outcome.Failed -> Log.i(TAG, "ENROLL FAILED: ${outcome.reason}")
        }
    }

    private suspend fun status(context: Context) {
        val binding = SyncShopBindingStore.load(context)
        val config = SyncServerConfigStore.load(context)
        val session = runCatching { SyncTokenStore(context).load() }.getOrNull()
        Log.i(TAG, "— sync status —")
        Log.i(TAG, "config: ${config?.let { "${it.environment} @ ${it.baseUrl}" } ?: "not enrolled"}")
        Log.i(TAG, "binding: ${binding?.let { "${it.role} → ${it.shopName ?: it.shopId}" } ?: "none"}")
        Log.i(TAG, "session: ${if (session != null) "present" else "none"}")
        Log.i(TAG, "trusted time: ${if (TrustedTime.isAnchored()) "anchored" else "device_only"}")
        Log.i(TAG, "kicks: ${SyncScheduler.kickCountSnapshot()}")
        if (binding != null) {
            val database = MilkShopDatabase.create(context.applicationContext)
            try {
                Log.i(TAG, "outbox: ${database.syncOutboxDao().count()} rows waiting")
            } finally {
                database.close()
            }
        } else {
            Log.i(TAG, "outbox: n/a (not enrolled)")
        }
    }

    private suspend fun drain(context: Context) {
        val app = context.applicationContext as? com.saaf.milkshop.MilkShopApplication
        if (app == null) {
            Log.i(TAG, "DRAIN needs the app process")
            return
        }
        // Push first (local edits reach the server first — C1), then pull.
        Log.i(TAG, "Draining outbox…")
        when (val outcome = app.syncRuntime.drainOnce()) {
            SyncPushEngine.DrainOutcome.NotConfigured -> Log.i(TAG, "DRAIN: not enrolled")
            is SyncPushEngine.DrainOutcome.Drained ->
                Log.i(TAG, "DRAIN: pushed ${outcome.pushed} rows; outbox now ${app.database.syncOutboxDao().count()}")
            is SyncPushEngine.DrainOutcome.RetryLater ->
                Log.i(TAG, "DRAIN: retry later (transient; pushed ${outcome.pushed})")
            is SyncPushEngine.DrainOutcome.Halted ->
                Log.i(TAG, "DRAIN: HALTED (${outcome.reasonCode}; pushed ${outcome.pushed}) — see security history")
        }
        when (val pull = app.syncRuntime.pullOnce()) {
            is com.saaf.milkshop.sync.SyncRuntime.SyncPullSummary.Done ->
                Log.i(TAG, "PULL: applied=${pull.applied} updated=${pull.updated} skipped=${pull.skipped} parked=${pull.parked} conflicts=${pull.conflicts}")
            is com.saaf.milkshop.sync.SyncRuntime.SyncPullSummary.RetryLater ->
                Log.i(TAG, "PULL: retry later (${pull.reason})")
            com.saaf.milkshop.sync.SyncRuntime.SyncPullSummary.NotEnrolled ->
                Log.i(TAG, "PULL: not enrolled")
        }
    }

    private fun unenroll(context: Context) {
        // Best-effort device logout is inside unenroll(); the debug path has
        // no coroutine here, so run it on a detached dispatcher.
        kotlinx.coroutines.runBlocking {
            SyncEnrollment(context).unenroll()
        }
        Log.i(TAG, "UNENROLLED — push scheduling off, config/session/binding cleared.")
    }

    /**
     * go 8 test hooks (debug builds never ship): create a local f5 snapshot,
     * or restore the newest local snapshot through the REAL restore engine
     * (same hooks as the production manager, minus the user-setup guard —
     * this is the same-installation QA path for the standing restore test:
     * snapshot with a pending outbox -> server edits -> restore -> re-enroll
     * -> classified pull-first re-baseline).
     */
    private suspend fun snapshot(context: Context) {
        val app = context.applicationContext as? com.saaf.milkshop.MilkShopApplication
        if (app == null) {
            Log.i(TAG, "SNAPSHOT needs the app process")
            return
        }
        val manager = com.saaf.milkshop.BackupManager(context)
        val meta = manager.createLocalSnapshot(app.database).getOrThrow()
        Log.i(TAG, "SNAPSHOT created: ${meta.fileName} (format ${meta.formatVersion})")
    }

    private suspend fun restoreLatest(context: Context) {
        val app = context.applicationContext as? com.saaf.milkshop.MilkShopApplication
        if (app == null) {
            Log.i(TAG, "RESTORE needs the app process")
            return
        }
        val manager = com.saaf.milkshop.BackupManager(context)
        val newest = manager.localBackups().firstOrNull()?.first
        if (newest == null) {
            Log.i(TAG, "RESTORE failed: no local snapshots")
            return
        }
        val crypto = com.saaf.milkshop.BackupCryptography(context)
        val json = crypto.readLocalAndVerify(newest.readBytes())
        val engine = com.saaf.milkshop.BackupRestoreEngine(
            createSafetySnapshot = { database -> manager.createLocalSnapshot(database).getOrThrow() },
            buildVerifiedBytes = { database, _, _ ->
                manager.buildCompleteSnapshotBytes(database)
            },
            capturePreferences = { org.json.JSONObject() },
            restorePreferences = { },
            rebaselineStore = SyncRebaselineStore(context),
            afterRestore = {
                SyncTokenStore(context).clear()
                SyncScheduler.warmUp(context)
            },
        )
        engine.restore(
            app.database,
            json,
            com.saaf.milkshop.BackupManager.RestoreSelection(
                business = true,
                planning = true,
                preferences = false,
                security = false,
            ),
            includeSecurity = false,
        )
        Log.i(
            TAG,
            "RESTORED ${newest.name}: outbox=${app.database.syncOutboxDao().count()} rows imported; " +
                "re-baseline flags set (fresh identity + classified pull-first on next enroll)",
        )
    }

    private companion object {
        const val TAG = "SaafSync"
        const val ACTION_ENROLL = "com.saaf.milkshop.sync.ENROLL"
        const val ACTION_STATUS = "com.saaf.milkshop.sync.STATUS"
        const val ACTION_DRAIN = "com.saaf.milkshop.sync.DRAIN"
        const val ACTION_UNENROLL = "com.saaf.milkshop.sync.UNENROLL"
        const val ACTION_SNAPSHOT = "com.saaf.milkshop.sync.SNAPSHOT"
        const val ACTION_RESTORE_LATEST = "com.saaf.milkshop.sync.RESTORE_LATEST"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_CREDENTIAL = "credential"
        const val EXTRA_SHOP_ID = "shop_id"
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_PUBLISHABLE_KEY = "publishable_key"
        const val EXTRA_ENVIRONMENT = "environment"

        // The DEV publishable key is public by design (spec §5); it ships in
        // debug builds only and never grants anything beyond anon API access.
        const val DEFAULT_DEV_BASE_URL = "https://tghzmofkkdsaasvnixps.supabase.co"
        const val DEFAULT_DEV_PUBLISHABLE_KEY = "sb_publishable_agJulQ-YO1UXjGPpLkR3cA_Hqtgwbvn"
    }
}
