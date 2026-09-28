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
                    ACTION_UNENROLL -> unenroll(context)
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
        when (val outcome = SyncEnrollment(context).enroll(config, username, credential.toCharArray())) {
            is SyncEnrollment.Outcome.Enrolled -> Log.i(
                TAG,
                "ENROLLED as ${outcome.binding.role} for shop '${outcome.binding.shopName}' " +
                    "(${outcome.binding.shopId}); timezone=${outcome.shopTimezone ?: "unset"}. " +
                    "Push is live — record entries in the app and watch sync_records on DEV.",
            )
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

    private fun unenroll(context: Context) {
        SyncEnrollment(context).unenroll()
        Log.i(TAG, "UNENROLLED — push scheduling off, config/session/binding cleared.")
    }

    private companion object {
        const val TAG = "SaafSync"
        const val ACTION_ENROLL = "com.saaf.milkshop.sync.ENROLL"
        const val ACTION_STATUS = "com.saaf.milkshop.sync.STATUS"
        const val ACTION_UNENROLL = "com.saaf.milkshop.sync.UNENROLL"
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
