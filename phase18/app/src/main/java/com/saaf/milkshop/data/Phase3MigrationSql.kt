package com.saaf.milkshop.data

internal object Phase3MigrationSql {
    val createStatements = listOf(
        """
        CREATE TABLE IF NOT EXISTS product_profiles (
            product_id INTEGER NOT NULL,
            default_sale_rate REAL,
            inventory_tracking INTEGER NOT NULL DEFAULT 1,
            archived INTEGER NOT NULL DEFAULT 0,
            notes TEXT,
            created_at TEXT NOT NULL,
            PRIMARY KEY(product_id),
            FOREIGN KEY(product_id) REFERENCES products(product_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS customer_profiles (
            account_id INTEGER NOT NULL,
            phone TEXT,
            area_route TEXT,
            address TEXT,
            notes TEXT,
            credit_limit REAL,
            daily_quota REAL,
            active INTEGER NOT NULL DEFAULT 1,
            created_at TEXT NOT NULL,
            PRIMARY KEY(account_id),
            FOREIGN KEY(account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS vendor_profiles (
            account_id INTEGER NOT NULL,
            phone TEXT,
            location TEXT,
            notes TEXT,
            active INTEGER NOT NULL DEFAULT 1,
            created_at TEXT NOT NULL,
            PRIMARY KEY(account_id),
            FOREIGN KEY(account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS employee_profiles (
            account_id INTEGER NOT NULL,
            role TEXT,
            phone TEXT,
            reference_salary REAL,
            salary_period TEXT,
            join_date TEXT,
            active INTEGER NOT NULL DEFAULT 1,
            created_at TEXT NOT NULL,
            PRIMARY KEY(account_id),
            FOREIGN KEY(account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS attendance_records (
            attendance_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            employee_account_id INTEGER NOT NULL,
            business_date TEXT NOT NULL,
            status TEXT NOT NULL,
            notes TEXT,
            created_at TEXT NOT NULL,
            supersedes_id INTEGER,
            archived INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(employee_account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(supersedes_id) REFERENCES attendance_records(attendance_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_attendance_records_employee_account_id_business_date_archived ON attendance_records(employee_account_id, business_date, archived)",
        "CREATE INDEX IF NOT EXISTS index_attendance_records_supersedes_id ON attendance_records(supersedes_id)",
        """
        CREATE TABLE IF NOT EXISTS deliveries (
            delivery_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            customer_account_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            quantity REAL NOT NULL,
            route TEXT,
            business_date TEXT NOT NULL,
            sale_event_id INTEGER,
            notes TEXT,
            created_at TEXT NOT NULL,
            archived INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(customer_account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(product_id) REFERENCES products(product_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(sale_event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_deliveries_customer_account_id_business_date ON deliveries(customer_account_id, business_date)",
        "CREATE INDEX IF NOT EXISTS index_deliveries_product_id ON deliveries(product_id)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_deliveries_sale_event_id ON deliveries(sale_event_id)",
        "CREATE INDEX IF NOT EXISTS index_deliveries_archived ON deliveries(archived)",
        """
        CREATE TABLE IF NOT EXISTS delivery_status_history (
            status_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            delivery_id INTEGER NOT NULL,
            status TEXT NOT NULL,
            occurred_at TEXT NOT NULL,
            notes TEXT,
            FOREIGN KEY(delivery_id) REFERENCES deliveries(delivery_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_delivery_status_history_delivery_id_status_id ON delivery_status_history(delivery_id, status_id)",
        """
        CREATE TABLE IF NOT EXISTS spoilage_details (
            event_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            quantity REAL NOT NULL,
            reason TEXT NOT NULL,
            PRIMARY KEY(event_id),
            FOREIGN KEY(event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(product_id) REFERENCES products(product_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_spoilage_details_product_id ON spoilage_details(product_id)",
        """
        CREATE TABLE IF NOT EXISTS physical_cash_counts (
            count_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            account_id INTEGER NOT NULL,
            business_date TEXT NOT NULL,
            counted_amount REAL NOT NULL,
            counted_at TEXT NOT NULL,
            notes TEXT,
            FOREIGN KEY(account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_physical_cash_counts_account_id_business_date_counted_at ON physical_cash_counts(account_id, business_date, counted_at)",
        """
        CREATE TABLE IF NOT EXISTS expense_category_profiles (
            account_id INTEGER NOT NULL,
            display_name TEXT NOT NULL,
            active INTEGER NOT NULL DEFAULT 1,
            created_at TEXT NOT NULL,
            PRIMARY KEY(account_id),
            FOREIGN KEY(account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_expense_category_profiles_display_name ON expense_category_profiles(display_name)",
    )

    val backfillStatements = listOf(
        """
        INSERT OR IGNORE INTO product_profiles(product_id, inventory_tracking, archived, created_at)
        SELECT product_id, 1, 0, CURRENT_TIMESTAMP FROM products
        """.trimIndent(),
        """
        INSERT OR IGNORE INTO customer_profiles(account_id, active, created_at)
        SELECT account_id, 1, CURRENT_TIMESTAMP FROM accounts WHERE category = 'Customer'
        """.trimIndent(),
        """
        INSERT OR IGNORE INTO vendor_profiles(account_id, active, created_at)
        SELECT account_id, 1, CURRENT_TIMESTAMP FROM accounts WHERE category = 'Vendor'
        """.trimIndent(),
        """
        INSERT OR IGNORE INTO employee_profiles(account_id, active, created_at)
        SELECT account_id, 1, CURRENT_TIMESTAMP FROM accounts WHERE category = 'Salary'
        """.trimIndent(),
        """
        INSERT OR IGNORE INTO expense_category_profiles(account_id, display_name, active, created_at)
        SELECT account_id, substr(name, length('Expense: ') + 1), 1, CURRENT_TIMESTAMP
        FROM accounts WHERE category = 'Operating Expense'
        """.trimIndent(),
    )
}
