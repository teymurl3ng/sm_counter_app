package com.saaf.milkshop.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import com.saaf.milkshop.sync.LegacyUid

/**
 * Room schema 9 → 10: additive sync plumbing. Every synced table gains a
 * `uid` (legacy rows receive deterministic values — see [LegacyUid]); the
 * mutable tables gain `sync_updated_at`; events gain time-trust columns and
 * the creating installation's id. Two new tables appear: `sync_outbox` (the
 * row-level change feed filled by capture triggers) and `sync_pull_state`
 * (the pull cursor singleton, carried by backup format 4).
 *
 * Order matters inside MIGRATION_9_10: columns → tables → triggers → uid
 * backfill (which also enqueues every legacy row as the baseline the first
 * push must upload) → support backfills → unique indexes.
 */
internal object SyncMigrationSql {

    private const val NOW = "strftime('%Y-%m-%dT%H:%M:%fZ', 'now')"

    val alterStatements: List<String> = listOf(
        "ALTER TABLE events ADD COLUMN event_uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE events ADD COLUMN origin_device_id TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE events ADD COLUMN raw_device_ts TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE events ADD COLUMN trusted_estimated_at TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE events ADD COLUMN time_trust TEXT NOT NULL DEFAULT 'device_only'",
        "ALTER TABLE products ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE products ADD COLUMN sync_updated_at TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE accounts ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE accounts ADD COLUMN sync_updated_at TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE budget_plans ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE budget_plans ADD COLUMN sync_updated_at TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE collections_targets ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE collections_targets ADD COLUMN sync_updated_at TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE deliveries ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE deliveries ADD COLUMN sync_updated_at TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE attendance_records ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE attendance_records ADD COLUMN sync_updated_at TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE delivery_status_history ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE daily_close_actions ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE physical_cash_counts ADD COLUMN uid TEXT NOT NULL DEFAULT ''",
    )

    val createStatements: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS sync_outbox (
            outbox_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            table_name TEXT NOT NULL,
            row_uid TEXT NOT NULL,
            enqueued_at TEXT NOT NULL
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_sync_outbox_table_name_row_uid ON sync_outbox(table_name, row_uid)",
        """
        CREATE TABLE IF NOT EXISTS sync_pull_state (
            state_id INTEGER NOT NULL PRIMARY KEY,
            last_server_seq INTEGER NOT NULL,
            updated_at TEXT NOT NULL
        )
        """.trimIndent(),
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

    /** Tables captured directly, keyed by their own uid column. */
    private data class CaptureTable(
        val table: String,
        val pkColumn: String,
        val uidColumn: String,
        /** Maintains sync_updated_at on the row (tables with UPDATE paths). */
        val stampsUpdated: Boolean,
        /** Has an AFTER UPDATE capture trigger (tables app code can update). */
        val capturesUpdates: Boolean,
    )

    private val capturedTables = listOf(
        CaptureTable("events", "event_id", "event_uid", stampsUpdated = false, capturesUpdates = false),
        CaptureTable("products", "product_id", "uid", stampsUpdated = true, capturesUpdates = true),
        CaptureTable("accounts", "account_id", "uid", stampsUpdated = true, capturesUpdates = true),
        CaptureTable("budget_plans", "budget_id", "uid", stampsUpdated = true, capturesUpdates = true),
        CaptureTable("collections_targets", "target_id", "uid", stampsUpdated = true, capturesUpdates = true),
        CaptureTable("deliveries", "delivery_id", "uid", stampsUpdated = true, capturesUpdates = true),
        CaptureTable("attendance_records", "attendance_id", "uid", stampsUpdated = true, capturesUpdates = true),
        CaptureTable("delivery_status_history", "status_id", "uid", stampsUpdated = false, capturesUpdates = false),
        CaptureTable("daily_close_actions", "action_id", "uid", stampsUpdated = false, capturesUpdates = false),
        CaptureTable("physical_cash_counts", "count_id", "uid", stampsUpdated = false, capturesUpdates = false),
    )

    /** Profile satellites: a change re-enqueues the parent master row. */
    private data class ProfileTable(
        val table: String,
        val parentTable: String,
        val parentKey: String,
    )

    private val profileTables = listOf(
        ProfileTable("product_profiles", "products", "product_id"),
        ProfileTable("customer_profiles", "accounts", "account_id"),
        ProfileTable("vendor_profiles", "accounts", "account_id"),
        ProfileTable("employee_profiles", "accounts", "account_id"),
        ProfileTable("expense_category_profiles", "accounts", "account_id"),
    )

    val captureTriggers: List<String> = buildList {
        capturedTables.forEach { captured ->
            add(insertTrigger(captured))
            if (captured.capturesUpdates) add(updateTrigger(captured))
        }
        add(settingsTrigger("insert"))
        add(settingsTrigger("update"))
        profileTables.forEach { profile ->
            add(profileTrigger(profile, "insert"))
            add(profileTrigger(profile, "update"))
        }
        add(
            """
            CREATE TRIGGER IF NOT EXISTS sync_capture_store_profile_insert AFTER INSERT ON store_profile
            BEGIN
                DELETE FROM sync_outbox WHERE table_name = 'store_profile' AND row_uid = 'u1_singleton_store_profile';
                INSERT INTO sync_outbox(table_name, row_uid, enqueued_at)
                    VALUES ('store_profile', 'u1_singleton_store_profile', $NOW);
            END
            """.trimIndent(),
        )
    }

    val triggerNames: List<String> = captureTriggers.map { sql ->
        sql.substringAfter("CREATE TRIGGER IF NOT EXISTS ").substringBefore(" AFTER").trim()
    }

    private fun insertTrigger(captured: CaptureTable): String = buildString {
        append("CREATE TRIGGER IF NOT EXISTS sync_capture_${captured.table}_insert AFTER INSERT ON ${captured.table}\n")
        append("BEGIN\n")
        append("    DELETE FROM sync_outbox WHERE table_name = '${captured.table}' AND row_uid = NEW.${captured.uidColumn};\n")
        append("    INSERT INTO sync_outbox(table_name, row_uid, enqueued_at)\n")
        append("        VALUES ('${captured.table}', NEW.${captured.uidColumn}, $NOW);\n")
        if (captured.stampsUpdated) {
            append("    UPDATE ${captured.table} SET sync_updated_at = $NOW\n")
            append("        WHERE ${captured.pkColumn} = NEW.${captured.pkColumn} AND sync_updated_at = '';\n")
        }
        append("END")
    }

    /**
     * The guard skips updates that only change sync_updated_at, which is what
     * stops the trigger's own stamp UPDATE from recursing: each real update
     * enqueues exactly once. The outbox write is an explicit DELETE + INSERT
     * (not INSERT OR REPLACE) because the outer statement's conflict clause
     * can override a trigger's own clause on some SQLite builds, and the
     * expense-category path inserts with ABORT — a re-enqueue must never
     * inherit it.
     */
    private fun updateTrigger(captured: CaptureTable): String = buildString {
        append("CREATE TRIGGER IF NOT EXISTS sync_capture_${captured.table}_update AFTER UPDATE ON ${captured.table}\n")
        append("WHEN NEW.sync_updated_at = OLD.sync_updated_at\n")
        append("BEGIN\n")
        append("    DELETE FROM sync_outbox WHERE table_name = '${captured.table}' AND row_uid = NEW.${captured.uidColumn};\n")
        append("    INSERT INTO sync_outbox(table_name, row_uid, enqueued_at)\n")
        append("        VALUES ('${captured.table}', NEW.${captured.uidColumn}, $NOW);\n")
        if (captured.stampsUpdated) {
            append("    UPDATE ${captured.table} SET sync_updated_at = $NOW\n")
            append("        WHERE ${captured.pkColumn} = NEW.${captured.pkColumn};\n")
        }
        append("END")
    }

    private fun settingsTrigger(kind: String): String =
        """
        CREATE TRIGGER IF NOT EXISTS sync_capture_settings_$kind AFTER $kind ON settings
        BEGIN
            DELETE FROM sync_outbox WHERE table_name = 'settings' AND row_uid = 'setting:' || NEW.setting_key;
            INSERT INTO sync_outbox(table_name, row_uid, enqueued_at)
                VALUES ('settings', 'setting:' || NEW.setting_key, $NOW);
        END
        """.trimIndent()

    private fun profileTrigger(profile: ProfileTable, kind: String): String =
        """
        CREATE TRIGGER IF NOT EXISTS sync_capture_${profile.table}_$kind AFTER $kind ON ${profile.table}
        BEGIN
            DELETE FROM sync_outbox WHERE table_name = '${profile.parentTable}'
                AND row_uid = (SELECT parent.uid FROM ${profile.parentTable} parent WHERE parent.${profile.parentKey} = NEW.${profile.parentKey});
            INSERT INTO sync_outbox(table_name, row_uid, enqueued_at)
                SELECT '${profile.parentTable}', parent.uid, $NOW
                FROM ${profile.parentTable} parent
                WHERE parent.${profile.parentKey} = NEW.${profile.parentKey};
        END
        """.trimIndent()

    /**
     * Deterministic legacy uids, applied collect-then-update so no SELECT
     * cursor walks a table being modified. Each backfilled row is also
     * enqueued: the first push must upload the full pre-sync history, and
     * this is the only moment that history exists as a change set.
     */
    fun backfillUids(connection: SQLiteConnection) {
        connection.backfill(
            select = "SELECT event_id, event_type, created_at FROM events",
            uid = { LegacyUid.event(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE events SET event_uid = '$uid' WHERE event_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('events', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT product_id, name, unit FROM products",
            uid = { LegacyUid.product(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE products SET uid = '$uid' WHERE product_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('products', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT account_id, name, category FROM accounts",
            uid = { LegacyUid.account(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE accounts SET uid = '$uid' WHERE account_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('accounts', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT budget_id, category, period_start, created_at FROM budget_plans",
            uid = { LegacyUid.budget(getLong(0), getText(1), getText(2), getText(3)) },
            update = { id, uid -> "UPDATE budget_plans SET uid = '$uid' WHERE budget_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('budget_plans', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT target_id, week_start, created_at FROM collections_targets",
            uid = { LegacyUid.collectionsTarget(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE collections_targets SET uid = '$uid' WHERE target_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('collections_targets', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT delivery_id, business_date, created_at FROM deliveries",
            uid = { LegacyUid.delivery(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE deliveries SET uid = '$uid' WHERE delivery_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('deliveries', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT attendance_id, business_date, created_at FROM attendance_records",
            uid = { LegacyUid.attendance(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE attendance_records SET uid = '$uid' WHERE attendance_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('attendance_records', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT status_id, delivery_id, occurred_at FROM delivery_status_history",
            uid = { LegacyUid.deliveryStatus(getLong(0), getLong(1), getText(2)) },
            update = { id, uid -> "UPDATE delivery_status_history SET uid = '$uid' WHERE status_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('delivery_status_history', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT action_id, business_date, action, created_at FROM daily_close_actions",
            uid = { LegacyUid.dailyCloseAction(getLong(0), getText(1), getText(2), getText(3)) },
            update = { id, uid -> "UPDATE daily_close_actions SET uid = '$uid' WHERE action_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('daily_close_actions', '$uid', $NOW)" },
        )
        connection.backfill(
            select = "SELECT count_id, business_date, counted_at FROM physical_cash_counts",
            uid = { LegacyUid.physicalCashCount(getLong(0), getText(1), getText(2)) },
            update = { id, uid -> "UPDATE physical_cash_counts SET uid = '$uid' WHERE count_id = $id" },
            baselineEnqueue = { uid -> "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) VALUES ('physical_cash_counts', '$uid', $NOW)" },
        )
    }

    /**
     * Legacy rows' support columns: events keep their original device
     * timestamp as raw_device_ts, mutable rows get an initial sync_updated_at,
     * and settings rows join the baseline (they have no uid backfill pass).
     */
    fun backfillSupportColumns(connection: SQLiteConnection) {
        connection.execSQL("UPDATE events SET raw_device_ts = created_at WHERE raw_device_ts = ''")
        for (table in capturedTables.filter { it.stampsUpdated }.map { it.table }) {
            connection.execSQL("UPDATE $table SET sync_updated_at = $NOW WHERE sync_updated_at = ''")
        }
        connection.execSQL(
            "INSERT OR REPLACE INTO sync_outbox(table_name, row_uid, enqueued_at) " +
                "SELECT 'settings', 'setting:' || setting_key, $NOW FROM settings",
        )
    }

    private inline fun SQLiteConnection.backfill(
        select: String,
        uid: SQLiteStatement.() -> String,
        update: (id: Long, uid: String) -> String,
        baselineEnqueue: (uid: String) -> String,
    ) {
        val pending = ArrayList<Pair<Long, String>>(64)
        prepare(select).use { statement ->
            while (statement.step()) {
                pending += statement.getLong(0) to statement.uid()
            }
        }
        for ((id, value) in pending) {
            execSQL(update(id, value))
            execSQL(baselineEnqueue(value))
        }
    }
}
