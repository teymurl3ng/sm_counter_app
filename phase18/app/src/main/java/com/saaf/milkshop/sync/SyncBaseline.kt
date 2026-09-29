package com.saaf.milkshop.sync

import androidx.room3.withWriteTransaction
import androidx.room3.executeSQL
import com.saaf.milkshop.data.MilkShopDatabase

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
}
