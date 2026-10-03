package com.saaf.milkshop.sync

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Stable per-installation identity stamped as the origin of newly created
 * event rows. It lives outside Room (noBackupFilesDir) on purpose: backups and
 * restores must never clone one device's identity onto another device.
 *
 * [warmUp] runs once at process start; entity construction reads the cached
 * value via [currentOrNull] so no disk access happens on write paths.
 */
object SyncIdentity {
    @Volatile
    private var cached: String = ""

    /** The installation id, or "" before [warmUp] has run (origin unknown). */
    fun currentOrNull(): String = cached

    fun warmUp(context: Context): String {
        if (cached.length >= 32) return cached
        val directory = File(context.noBackupFilesDir, "sync")
        val file = File(directory, "installation_id.txt")
        val stored = runCatching { file.readText().trim() }.getOrDefault("")
        if (stored.length >= 32) {
            cached = stored
            return stored
        }
        val created = UUID.randomUUID().toString().replace("-", "")
        check(directory.isDirectory || directory.mkdirs()) { "Could not create sync identity directory" }
        file.writeText(created)
        cached = created
        return created
    }

    /**
     * go 8: mints a fresh installation identity, replacing the stored id and
     * the process cache. Consumed at enrollment when the installation was
     * restored or reset (SyncRebaselineStore.needsFreshIdentity): a restored
     * device must never push as the identity that wrote its backup, because
     * same-origin pushes escape the server's conflict audit. The replaced id
     * survives only in the caller's hands (prev_device_id).
     */
    fun regenerate(context: Context): String {
        val directory = File(context.noBackupFilesDir, "sync")
        check(directory.isDirectory || directory.mkdirs()) { "Could not create sync identity directory" }
        val created = UUID.randomUUID().toString().replace("-", "")
        File(directory, "installation_id.txt").writeText(created)
        cached = created
        return created
    }
}
