package com.saaf.milkshop.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import com.saaf.milkshop.sync.LegacyUid

/**
 * Room schema 9 → 10: additive sync-identity columns. Every synced table
 * gains a `uid`; legacy rows receive deterministic values (see [LegacyUid]) so
 * the same historical row maps to the same uid on every device that migrates
 * the same lineage or imports the same pre-schema-10 backup. `events`
 * additionally records which installation created the row — empty for legacy
 * rows, whose origin is the device that performed the migration.
 */
internal object SyncMigrationSql {

    val alterStatements: List<String> = listOf(
        "ALTER TABLE events ADD COLUMN event_uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE events ADD COLUMN origin_device_id TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE products ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE accounts ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE budget_plans ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE collections_targets ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE deliveries ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE attendance_records ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE delivery_status_history ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE daily_close_actions ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE physical_cash_counts ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
    )

    val indexStatements: List<String> = listOf(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_events_event_uid ON events(event_uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_products_uid ON products(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_uid ON accounts(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_budget_plans_uid ON budget_plans(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_collections_targets_uid ON collections_targets(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_deliveries_uid ON deliveries(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_records_uid ON attendance_records(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_delivery_status_history_uid ON delivery_status_history(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_close_actions_uid ON daily_close_actions(uid)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_physical_cash_counts_uid ON physical_cash_counts(uid)",
    )

    /**
     * Runs after [alterStatements] and before [indexStatements]. Each table's
     * deterministic uids are collected first and applied after the scan, so a
     * SELECT cursor never walks a table that is being modified underneath it.
     */
    fun backfillUids(connection: SQLiteConnection) {
        connection.backfill(
            select = "SELECT event_id, event_type, created_at FROM events",
            uid = { LegacyUid.event(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE events SET event_uid = '$uid' WHERE event_id = $id" },
        )
        connection.backfill(
            select = "SELECT product_id, name, unit FROM products",
            uid = { LegacyUid.product(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE products SET uid = '$uid' WHERE product_id = $id" },
        )
        connection.backfill(
            select = "SELECT account_id, name, category FROM accounts",
            uid = { LegacyUid.account(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE accounts SET uid = '$uid' WHERE account_id = $id" },
        )
        connection.backfill(
            select = "SELECT budget_id, category, period_start, created_at FROM budget_plans",
            uid = { LegacyUid.budget(getLong(0), getText(1), getText(2), getText(3)) },
            update = { id, uid -> "UPDATE budget_plans SET uid = '$uid' WHERE budget_id = $id" },
        )
        connection.backfill(
            select = "SELECT target_id, week_start, created_at FROM collections_targets",
            uid = { LegacyUid.collectionsTarget(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE collections_targets SET uid = '$uid' WHERE target_id = $id" },
        )
        connection.backfill(
            select = "SELECT delivery_id, business_date, created_at FROM deliveries",
            uid = { LegacyUid.delivery(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE deliveries SET uid = '$uid' WHERE delivery_id = $id" },
        )
        connection.backfill(
            select = "SELECT attendance_id, business_date, created_at FROM attendance_records",
            uid = { LegacyUid.attendance(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE attendance_records SET uid = '$uid' WHERE attendance_id = $id" },
        )
        connection.backfill(
            select = "SELECT status_id, delivery_id, occurred_at FROM delivery_status_history",
            uid = { LegacyUid.deliveryStatus(getLong(0), getLong(1), getText(2)) },
            update = { id, uid -> "UPDATE delivery_status_history SET uid = '$uid' WHERE status_id = $id" },
        )
        connection.backfill(
            select = "SELECT action_id, business_date, action, created_at FROM daily_close_actions",
            uid = { LegacyUid.dailyCloseAction(getLong(0), getText(1), getText(2), getText(3)) },
            update = { id, uid -> "UPDATE daily_close_actions SET uid = '$uid' WHERE action_id = $id" },
        )
        connection.backfill(
            select = "SELECT count_id, business_date, counted_at FROM physical_cash_counts",
            uid = { LegacyUid.physicalCashCount(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE physical_cash_counts SET uid = '$uid' WHERE count_id = $id" },
        )
    }

    private inline fun SQLiteConnection.backfill(
        select: String,
        uid: SQLiteStatement.() -> String,
        update: (id: Long, uid: String) -> String,
    ) {
        val pending = ArrayList<Pair<Long, String>>(64)
        prepare(select).use { statement ->
            while (statement.step()) {
                pending += statement.getLong(0) to statement.uid()
            }
        }
        for ((id, value) in pending) {
            execSQL(update(id, value))
        }
    }
}
