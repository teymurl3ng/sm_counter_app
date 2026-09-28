package com.saaf.milkshop.sync

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Business-date derivation (P2b). The business date of a new entry comes
 * from trusted time in the shop's timezone — falling back to the device
 * clock (in the same zone) while unanchored, exactly like today's behavior.
 * The shop timezone is one server-side setting (D5): cached locally, default
 * the device zone until the first sync delivers the server value. No
 * hardcoded zones anywhere.
 */
object BusinessClock {

    @Volatile private var zone: ZoneId = ZoneId.systemDefault()
    @Volatile private var appContext: Context? = null

    fun warmUp(context: Context) {
        appContext = context.applicationContext
        val stored = runCatching { zoneFile(context).readText().trim() }.getOrDefault("")
        if (stored.isNotEmpty()) {
            runCatching { ZoneId.of(stored) }.getOrNull()?.let { zone = it }
        }
    }

    fun currentZone(): ZoneId = zone

    /** Server sync (later phase) delivers the shop timezone; persists it. */
    fun setZone(zoneId: String) {
        val parsed = ZoneId.of(zoneId)
        zone = parsed
        val context = appContext ?: return
        runCatching {
            val file = zoneFile(context)
            file.parentFile?.let { dir -> check(dir.isDirectory || dir.mkdirs()) }
            file.writeText(parsed.id)
        }
    }

    /** Today's business date: trusted time when anchored, device clock otherwise. */
    fun today(): LocalDate {
        val trusted = TrustedTime.trustedNowEpochMs() ?: return LocalDate.now(zone)
        return Instant.ofEpochMilli(trusted).atZone(zone).toLocalDate()
    }

    private fun zoneFile(context: Context): File =
        File(File(context.noBackupFilesDir, "sync"), "shop_timezone.txt")
}
