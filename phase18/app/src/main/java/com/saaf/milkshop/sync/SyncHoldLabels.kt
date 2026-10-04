package com.saaf.milkshop.sync

import org.json.JSONObject

/**
 * Human-readable titles for the sync-review holds. Each hold row already
 * stores the device payload snapshot (`payload_json`); this reads the few
 * fields that name the row so the owner sees "Product — Milk" instead of a
 * bare UUID like `u1_seed_products_Milk`.
 *
 * Pure and JVM-tested. The payload's keys are the synced tables' column
 * names (the envelope is built from row columns). A malformed, absent, or
 * too-thin payload never throws — it falls back to a humanized table label
 * plus a short uid so the row is still decipherable.
 *
 * Deliberately no database lookups: delivery/attendance payloads carry
 * numeric account ids, and resolving those to names is a separate concern
 * (the label shows the human fields the payload actually holds).
 */
object SyncHoldLabels {

    fun holdLabel(tableName: String, payloadJson: String?, uid: String): String {
        val payload = payloadJson
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        val detail = payload?.let { describe(tableName, it) }?.takeIf { it.isNotBlank() }
        return if (detail == null) {
            "${humanTable(tableName)} · ${shortUid(uid)}"
        } else {
            "${humanTable(tableName)} — $detail"
        }
    }

    private fun describe(tableName: String, p: JSONObject): String? = when (tableName) {
        "products" -> str(p, "name")
        "accounts" -> {
            val name = str(p, "name")
            val type = str(p, "type")?.lowercase()
            when {
                name != null && type != null -> "$name ($type)"
                else -> name
            }
        }
        "events" -> join(
            humanize(str(p, "event_type") ?: "Event"),
            str(p, "amount") ?: str(p, "amount_owed"),
            str(p, "sale_quantity")?.let { "qty $it" },
            str(p, "raw_liters")?.let { "$it L" },
        )
        "settings" -> str(p, "setting_key")
        "store_profile" -> str(p, "store_name")
        "deliveries" -> join(
            str(p, "route")?.let { "route $it" },
            str(p, "quantity")?.let { "qty $it" },
            str(p, "business_date"),
        )
        "delivery_status_history" -> join(str(p, "status"), str(p, "occurred_at")?.take(10))
        "budget_plans" -> join(str(p, "category"), str(p, "period_start"))
        "collections_targets" -> join(str(p, "week_start"), str(p, "target_amount"))
        "attendance_records" -> join(str(p, "status"), str(p, "business_date"))
        "daily_close_actions" -> join(str(p, "action"), str(p, "business_date"))
        "physical_cash_counts" -> join(str(p, "business_date"), str(p, "counted_amount"))
        else -> null
    } ?: fallback(p)

    /** Anything human the payload happens to carry, else null. */
    private fun fallback(p: JSONObject): String? =
        listOf("name", "display_name", "title", "label", "type", "key", "setting_key", "business_date")
            .firstNotNullOfOrNull { str(p, it) }

    private fun humanTable(tableName: String): String = when (tableName) {
        "products" -> "Product"
        "accounts" -> "Account"
        "events" -> "Event"
        "settings" -> "Setting"
        "store_profile" -> "Shop profile"
        "deliveries" -> "Delivery"
        "delivery_status_history" -> "Delivery status"
        "budget_plans" -> "Budget"
        "collections_targets" -> "Collections target"
        "attendance_records" -> "Attendance"
        "daily_close_actions" -> "Daily close"
        "physical_cash_counts" -> "Cash count"
        else -> tableName.replaceFirstChar { it.uppercase() }
    }

    /** "PRODUCTION"/"SALE" (stored enum wire values) read as "Production"/"Sale". */
    private fun humanize(value: String): String {
        val base = if (value.any { it.isLowerCase() }) value else value.lowercase()
        return base.replaceFirstChar { it.uppercase() }
    }

    /** org.json returns "null" for JSON null and "" for missing. */
    private fun str(o: JSONObject, key: String): String? =
        o.optString(key).takeIf { it.isNotBlank() && it != "null" }?.let(::polishNumber)

    /** `1200.0` reads as "1200.0" in a label; drop the empty fraction. */
    private fun polishNumber(value: String): String =
        value.toDoubleOrNull()
            ?.takeIf { it == Math.floor(it) && !it.isInfinite() }
            ?.toLong()
            ?.toString()
            ?: value

    private fun join(vararg parts: String?): String? =
        parts.filterNotNull().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(" · ")

    private fun shortUid(uid: String): String =
        if (uid.length <= 12) uid else uid.take(8)
}
