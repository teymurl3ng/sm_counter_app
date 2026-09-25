package com.saaf.milkshop.data

/**
 * Schema 8 -> 9 (Account Bridge, ACCOUNT_BRIDGE_PHASE1_SPEC.md §4).
 * Additive only: no existing table, column, or row is touched. The new
 * tables cache server-derived identity state; an upgraded installation
 * starts unbound (no rows) and stays fully functional offline.
 */
internal object Phase3AccountBridgeMigrationSql {
    val createStatements: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS account_installation_binding (
            binding_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            environment TEXT NOT NULL,
            business_id TEXT NOT NULL,
            shop_id TEXT NOT NULL,
            shop_name_snapshot TEXT NOT NULL,
            status TEXT NOT NULL,
            verified_at_epoch_ms INTEGER,
            contract_version TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_account_installation_binding_environment_business_id_shop_id ON account_installation_binding(environment, business_id, shop_id)",
        """
        CREATE TABLE IF NOT EXISTS server_identity_links (
            local_user_id INTEGER NOT NULL PRIMARY KEY,
            server_staff_id TEXT NOT NULL,
            business_id TEXT NOT NULL,
            username_snapshot TEXT,
            role_snapshot TEXT,
            status TEXT NOT NULL,
            verified_at_epoch_ms INTEGER,
            server_changed_at_epoch_ms INTEGER,
            last_error_code TEXT,
            FOREIGN KEY(local_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_server_identity_links_server_staff_id ON server_identity_links(server_staff_id)",
        """
        CREATE TABLE IF NOT EXISTS server_shop_snapshots (
            snapshot_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            server_shop_id TEXT NOT NULL,
            shop_name_snapshot TEXT NOT NULL,
            observed_at_epoch_ms INTEGER NOT NULL,
            local_user_id INTEGER NOT NULL,
            FOREIGN KEY(local_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_server_shop_snapshots_server_shop_id_local_user_id ON server_shop_snapshots(server_shop_id, local_user_id)",
        "CREATE INDEX IF NOT EXISTS index_server_shop_snapshots_local_user_id ON server_shop_snapshots(local_user_id)",
    )
}
