package com.saaf.milkshop.sync

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Which shop this installation pushes as, under which server identity —
 * chosen once at enrollment and stored outside Room and outside backups.
 * (The Room account_installation_binding table stays reserved for the full
 * Account Bridge client; this file is the sync runtime's working binding.)
 */
data class SyncShopBinding(
    val environment: String,
    val businessId: String,
    val shopId: String,
    val shopName: String?,
    val staffId: String,
    val role: String,
)

object SyncShopBindingStore {

    fun load(context: Context): SyncShopBinding? {
        val text = runCatching { file(context).readText().trim() }.getOrDefault("")
        if (text.isEmpty()) return null
        return runCatching {
            val json = JSONObject(text)
            SyncShopBinding(
                environment = json.getString("environment"),
                businessId = json.getString("business_id"),
                shopId = json.getString("shop_id"),
                shopName = json.optString("shop_name").takeIf { it.isNotEmpty() },
                staffId = json.getString("staff_id"),
                role = json.getString("role"),
            )
        }.getOrNull()
    }

    fun save(context: Context, binding: SyncShopBinding) {
        val target = file(context)
        target.parentFile?.let { dir -> check(dir.isDirectory || dir.mkdirs()) }
        target.writeText(
            JSONObject()
                .put("environment", binding.environment)
                .put("business_id", binding.businessId)
                .put("shop_id", binding.shopId)
                .put("shop_name", binding.shopName ?: JSONObject.NULL)
                .put("staff_id", binding.staffId)
                .put("role", binding.role)
                .toString(),
        )
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
        FollowUpIntegration.changed(context)
    }

    private fun file(context: Context): File =
        File(File(context.noBackupFilesDir, "sync"), "shop_binding.json")
}
