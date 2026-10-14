package com.saaf.milkshop.sync

import androidx.room3.withWriteTransaction
import androidx.room3.executeSQL
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductEntity
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.ProductProfileEntity
import com.saaf.milkshop.data.ExpenseCategoryProfileEntity
import com.saaf.milkshop.data.SettingEntity
import com.saaf.milkshop.productJson
import com.saaf.milkshop.accountJson
import com.saaf.milkshop.productProfileJson
import com.saaf.milkshop.expenseCategoryProfileJson
import com.saaf.milkshop.settingJson
import org.json.JSONObject

/**
 * Re-baselines the outbox on enrollment: every local row of every synced
 * table is enqueued (skipping rows already queued) so that enrolling against
 * a server — first time, or after switching environments — uploads the
 * installation's complete history. Server-side upserts are idempotent by
 * (shop, table, uid), so re-baselining a device whose data is already on the
 * server changes nothing there.
 */
object SyncBaseline {

    private const val NOW = "strftime('%Y-%m-%dT%H:%M:%fZ', 'now')"

    private val enqueueStatements = listOf(
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'events', event_uid, $NOW FROM events",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'products', uid, $NOW FROM products",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'accounts', uid, $NOW FROM accounts",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'budget_plans', uid, $NOW FROM budget_plans",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'collections_targets', uid, $NOW FROM collections_targets",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'deliveries', uid, $NOW FROM deliveries",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'attendance_records', uid, $NOW FROM attendance_records",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'delivery_status_history', uid, $NOW FROM delivery_status_history",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'daily_close_actions', uid, $NOW FROM daily_close_actions",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'physical_cash_counts', uid, $NOW FROM physical_cash_counts",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'settings', 'setting:' || setting_key, $NOW FROM settings",
        "INSERT OR IGNORE INTO sync_outbox(table_name, row_uid, enqueued_at) SELECT 'store_profile', 'u1_singleton_store_profile', $NOW FROM store_profile",
    )

    suspend fun enqueueAll(database: MilkShopDatabase) = database.withWriteTransaction {
        // OR IGNORE keeps the outbox's unique (table, uid) key honest and does
        // not fire the AFTER INSERT capture triggers for already-queued rows.
        enqueueStatements.forEach { executeSQL(it) }
    }

    /**
     * go 8 option Z: a PLAIN enrollment enqueues the full local history
     * PRE-HELD — the first drain classifies it against server row bodies
     * (release what the server lacks / drop in-sync rows / hold differing
     * rows) before anything pushes. Only entries inserted HERE are held:
     * max(outbox_id) taken before the enqueue marks the boundary, so
     * pending edits captured earlier keep held=0 and push normally. Counter
     * enrollment also holds exact factory defaults captured before this boundary.
     */
    suspend fun enqueueAllHeld(
        database: MilkShopDatabase,
        reconcileCapturedDefaults: Boolean = false,
    ) = database.withWriteTransaction {
        val boundary = database.syncOutboxDao().maxOutboxId()
        if (reconcileCapturedDefaults) {
            // Capture triggers run before factory seeding. Only exact, unchanged
            // defaults join the comparison pass; genuine pending work keeps FIFO
            // and its current held state. Differing server values still need review.
            val candidates = database.syncOutboxDao().all().filter {
                SyncRebaseline.seenKey(it.tableName, it.rowUid) in factoryDefaults
            }
            SyncEnvelopeFactory(database).build(candidates).envelopes.forEach { envelope ->
                val table = envelope.getString("table")
                val uid = envelope.getString("uid")
                val expected = factoryDefaults.getValue(SyncRebaseline.seenKey(table, uid))
                if (SyncRebaseline.livePayloadsEqual(envelope.getJSONObject("payload"), expected)) {
                    database.syncOutboxDao().setHeld(table, uid, true)
                }
            }
        }
        enqueueStatements.forEach { executeSQL(it) }
        database.syncOutboxDao().holdAllAfter(boundary)
    }

    // Deliberately exact fingerprints of DatabaseSql seeds plus profile backfill.
    // A changed default, missing profile or changed settings date is not a seed.
    private val factoryDefaults: Map<String, JSONObject> by lazy {
        buildMap {
            val date = "2026-01-01 00:00:00"
            for ((name, unit) in listOf("Milk" to "Liters", "Yogurt" to "Kg")) {
                val uid = requireNotNull(LegacyUid.seedProductUid(name, unit))
                put(SyncRebaseline.seenKey("products", uid),
                    productJson(ProductEntity(name = name, unit = unit, uid = uid))
                        .put("profile", productProfileJson(ProductProfileEntity(0, createdAt = date))))
            }
            val expenses = listOf("Rent", "Electricity", "Internet", "Water", "Fuel", "Poly bags", "LPG", "Other")
            for (name in listOf("Cash", "JazzCash", "Bank") + expenses.map { "Expense: $it" }) {
                val expense = name.startsWith("Expense: ")
                val category = if (expense) "Operating Expense" else "Cash/Bank"
                val uid = requireNotNull(LegacyUid.seedAccountUid(name, category))
                val payload = accountJson(AccountEntity(name = name,
                    type = if (expense) "Expense" else "Asset", category = category, uid = uid))
                if (expense) payload.put("profile", expenseCategoryProfileJson(
                    ExpenseCategoryProfileEntity(0, name.removePrefix("Expense: "), createdAt = date)))
                put(SyncRebaseline.seenKey("accounts", uid), payload)
            }
            for ((key, value) in mapOf("milk_rate_vdr" to 170.0, "delivery_fee_vdr" to 11.0,
                "ts_standard" to 13.0, "retail_rate_milk" to 190.0,
                "retail_rate_yogurt" to 280.0, "default_delivery_surcharge" to 50.0)) {
                put(SyncRebaseline.seenKey("settings", "setting:$key"), settingJson(SettingEntity(key, value, date)))
            }
        }
    }
}
