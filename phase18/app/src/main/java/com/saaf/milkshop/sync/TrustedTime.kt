package com.saaf.milkshop.sync

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.io.File
import java.time.Instant
import kotlin.math.abs

/**
 * Trusted-time anchor (P2a). Every server response carries server time; the
 * most recent one is stored with the device's elapsedRealtime reading and
 * boot count. trusted_now = serverTime + (elapsedRealtime now − elapsedRealtime
 * at anchor). A reboot invalidates the anchor — detected via boot count and
 * via elapsedRealtime running backwards — and time is treated as unanchored
 * until the next server contact.
 *
 * Reads never touch disk: the anchor is cached after [warmUp]. Nothing here
 * ever blocks entry (D9) — callers use it to *derive* dates and *flag*
 * divergence, never to gate a write.
 */
object TrustedTime {

    /** Skew beyond this is flagged on events and warned about in the audit trail. */
    const val DEFAULT_WARN_THRESHOLD_MS: Long = 5 * 60 * 1000

    /** Skew beyond this escalates the audit entry to an owner alert. */
    const val DEFAULT_ALERT_THRESHOLD_MS: Long = 60 * 60 * 1000

    @Volatile var warnThresholdMs: Long = DEFAULT_WARN_THRESHOLD_MS
    @Volatile var alertThresholdMs: Long = DEFAULT_ALERT_THRESHOLD_MS

    /** Server times further than this from the device clock are ignored as corrupt. */
    private const val MAX_SANE_SERVER_SKEW_MS: Long = 3650L * 24 * 60 * 60 * 1000 // ~10 years

    data class Anchor(
        val bootCount: Int,
        val serverEpochMs: Long,
        val elapsedRealtimeMs: Long,
    )

    @Volatile private var anchor: Anchor? = null
    @Volatile private var appContext: Context? = null
    @Volatile private var cachedBootCount: Int = -1

    fun warmUp(context: Context) {
        appContext = context.applicationContext
        val bootCount = currentBootCount(context)
        cachedBootCount = bootCount
        val stored = runCatching { anchorFile(context).readText().trim() }.getOrDefault("")
        val parts = stored.split('\n')
        val candidate = if (parts.size == 3) {
            runCatching {
                Anchor(parts[0].toInt(), parts[1].toLong(), parts[2].toLong())
            }.getOrNull()
        } else {
            null
        }
        anchor = if (candidate != null && candidate.bootCount == bootCount &&
            SystemClock.elapsedRealtime() >= candidate.elapsedRealtimeMs
        ) {
            candidate
        } else {
            null
        }
    }

    /**
     * Records server time from any server response. The in-memory anchor is
     * always refreshed (trusted time must never depend on disk state);
     * persistence is best-effort when a context is available. Server clocks
     * are authoritative; the device clock only sanity-checks the value.
     */
    fun anchor(serverEpochMs: Long) {
        val now = System.currentTimeMillis()
        if (abs(serverEpochMs - now) > MAX_SANE_SERVER_SKEW_MS) return
        val fresh = Anchor(cachedBootCount, serverEpochMs, SystemClock.elapsedRealtime())
        anchor = fresh
        val context = appContext ?: return
        runCatching {
            val file = anchorFile(context)
            file.parentFile?.let { dir -> check(dir.isDirectory || dir.mkdirs()) }
            file.writeText("${fresh.bootCount}\n${fresh.serverEpochMs}\n${fresh.elapsedRealtimeMs}")
        }
    }

    fun isAnchored(): Boolean = trustedNowEpochMs() != null

    /** Trusted now, or null while unanchored (never anchored yet, or rebooted). */
    fun trustedNowEpochMs(): Long? {
        val current = anchor ?: return null
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed < current.elapsedRealtimeMs) return null // rebooted since the anchor
        return current.serverEpochMs + (elapsed - current.elapsedRealtimeMs)
    }

    fun trustedNow(): Instant? = trustedNowEpochMs()?.let(Instant::ofEpochMilli)

    /** |device clock − trusted clock| while anchored, else null. */
    fun deviceSkewMs(): Long? =
        trustedNowEpochMs()?.let { trusted -> abs(System.currentTimeMillis() - trusted) }

    fun currentBootCount(context: Context): Int =
        runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }
            .getOrDefault(-1)

    /** Test-only: clears the in-memory anchor and the persisted file. */
    fun resetForTest(context: Context? = null) {
        anchor = null
        val target = context ?: appContext ?: return
        runCatching { anchorFile(target).delete() }
    }

    private fun anchorFile(context: Context): File =
        File(File(context.noBackupFilesDir, "sync"), "trusted_time_anchor.txt")
}
