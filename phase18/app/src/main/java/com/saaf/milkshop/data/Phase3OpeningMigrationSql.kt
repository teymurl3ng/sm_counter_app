package com.saaf.milkshop.data

object Phase3OpeningMigrationSql {
    val createStatements = listOf(
        """
        CREATE TABLE IF NOT EXISTS store_profile (
            store_profile_id INTEGER NOT NULL PRIMARY KEY,
            store_name TEXT NOT NULL,
            currency_code TEXT NOT NULL,
            phone TEXT,
            address TEXT,
            updated_at TEXT NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS opening_batches (
            event_id INTEGER NOT NULL PRIMARY KEY,
            FOREIGN KEY(event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS opening_inventory_entries (
            event_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            quantity REAL NOT NULL,
            PRIMARY KEY(event_id, product_id),
            FOREIGN KEY(event_id) REFERENCES opening_batches(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(product_id) REFERENCES products(product_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_opening_inventory_entries_product_id ON opening_inventory_entries(product_id)",
        """
        CREATE TABLE IF NOT EXISTS opening_account_entries (
            event_id INTEGER NOT NULL,
            account_id INTEGER NOT NULL,
            balance REAL NOT NULL,
            PRIMARY KEY(event_id, account_id),
            FOREIGN KEY(event_id) REFERENCES opening_batches(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_opening_account_entries_account_id ON opening_account_entries(account_id)",
    )
}
