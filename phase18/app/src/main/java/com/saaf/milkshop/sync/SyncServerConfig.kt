package com.saaf.milkshop.sync

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Which Account Server environment this installation talks to (Account
 * Bridge spec §6: DEV only in developer builds; release is PROD-only —
 * chosen once at enrollment, stored outside Room and outside backups).
 * The publishable key is public by design (spec §5 header boundary); no
 * secret or service key ever lives on the device.
 */
data class SyncServerConfig(
    val environment: String,
    val baseUrl: String,
    val publishableKey: String,
) {
    fun url(path: String): String = baseUrl.trimEnd('/') + path

    companion object {
        const val ENVIRONMENT_DEV = "dev"
        const val ENVIRONMENT_PROD = "prod"
    }
}

object SyncServerConfigStore {

    fun load(context: Context): SyncServerConfig? {
        val text = runCatching { file(context).readText().trim() }.getOrDefault("")
        if (text.isEmpty()) return null
        return runCatching {
            val json = JSONObject(text)
            SyncServerConfig(
                environment = json.getString("environment"),
                baseUrl = json.getString("base_url"),
                publishableKey = json.getString("publishable_key"),
            )
        }.getOrNull()
    }

    fun save(context: Context, config: SyncServerConfig) {
        val target = file(context)
        target.parentFile?.let { dir -> check(dir.isDirectory || dir.mkdirs()) }
        target.writeText(
            JSONObject()
                .put("environment", config.environment)
                .put("base_url", config.baseUrl)
                .put("publishable_key", config.publishableKey)
                .toString(),
        )
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun file(context: Context): File =
        File(File(context.noBackupFilesDir, "sync"), "server_config.json")
}
