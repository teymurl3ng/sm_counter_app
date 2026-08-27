package com.saaf.milkshop

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3MigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val databaseName = "phase3-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = context.getDatabasePath(databaseName),
        driver = BundledSQLiteDriver(),
        databaseClass = MilkShopDatabase::class,
        databaseFactory = {
            Room.databaseBuilder<MilkShopDatabase>(context, databaseName)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MilkShopDatabase.MIGRATION_3_4, MilkShopDatabase.MIGRATION_4_5, MilkShopDatabase.MIGRATION_5_6, MilkShopDatabase.MIGRATION_6_7)
                .build()
        },
        autoMigrationSpecs = emptyList(),
    )

    @Test
    fun migration3To4_validatesSchemaAndBackfillsCanonicalIdentities() = runBlocking {
        context.deleteDatabase(databaseName)
        helper.createDatabase(3).apply {
            execSQL("INSERT INTO products(product_id, name, unit) VALUES (1, 'Milk', 'Liters')")
            execSQL("INSERT INTO accounts(account_id, name, type, category) VALUES (1, 'Customer: Existing', 'Asset', 'Customer')")
            execSQL("INSERT INTO accounts(account_id, name, type, category) VALUES (2, 'Vendor: Existing', 'Liability', 'Vendor')")
            execSQL("INSERT INTO accounts(account_id, name, type, category) VALUES (3, 'Employee: Existing', 'Expense', 'Salary')")
            execSQL("INSERT INTO accounts(account_id, name, type, category) VALUES (4, 'Expense: Rent', 'Expense', 'Operating Expense')")
            close()
        }

        helper.runMigrationsAndValidate(4, listOf(MilkShopDatabase.MIGRATION_3_4)).use { connection ->
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM product_profiles WHERE product_id = 1"))
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM customer_profiles WHERE account_id = 1"))
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM vendor_profiles WHERE account_id = 2"))
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM employee_profiles WHERE account_id = 3"))
            assertEquals("Rent", connection.scalarText("SELECT display_name FROM expense_category_profiles WHERE account_id = 4"))
        }
    }

    @Test
    fun migration4To5_preservesExistingLedger_andCreatesNoArtificialOpeningState() = runBlocking {
        context.deleteDatabase(databaseName)
        helper.createDatabase(4).apply {
            execSQL(
                """
                INSERT INTO events(created_at, occurred_at, event_type, amount, surcharge_applied)
                VALUES ('2026-08-27T00:00:00Z', '2026-08-27', 'VENDOR_PAYMENT', 100, 0)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(5, listOf(MilkShopDatabase.MIGRATION_4_5)).use { connection ->
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM events"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM store_profile"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM opening_batches"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM opening_inventory_entries"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM opening_account_entries"))
        }
    }

    @Test
    fun migration5To6_preservesExistingLedger_andCreatesNoArtificialClose() = runBlocking {
        context.deleteDatabase(databaseName)
        helper.createDatabase(5).apply {
            execSQL(
                """
                INSERT INTO events(created_at, occurred_at, event_type, amount, surcharge_applied)
                VALUES ('2026-08-27T00:00:00Z', '2026-08-27', 'VENDOR_PAYMENT', 100, 0)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(6, listOf(MilkShopDatabase.MIGRATION_5_6)).use { connection ->
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM events"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM daily_close_actions"))
        }
    }

    @Test
    fun migration6To7_preservesExistingLedger_andCreatesNoArtificialUser() = runBlocking {
        context.deleteDatabase(databaseName)
        helper.createDatabase(6).apply {
            execSQL(
                """
                INSERT INTO events(created_at, occurred_at, event_type, amount, surcharge_applied)
                VALUES ('2026-08-27T00:00:00Z', '2026-08-27', 'VENDOR_PAYMENT', 100, 0)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(7, listOf(MilkShopDatabase.MIGRATION_6_7)).use { connection ->
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM events"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM app_users"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM user_credentials"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM saved_recovery_codes"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM security_audit_entries"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM event_actor_links"))
        }
    }

    private fun androidx.sqlite.SQLiteConnection.scalarLong(sql: String): Long =
        prepare(sql).use { statement ->
            check(statement.step()) { "Query returned no row: $sql" }
            statement.getLong(0)
        }

    private fun androidx.sqlite.SQLiteConnection.scalarText(sql: String): String =
        prepare(sql).use { statement ->
            check(statement.step()) { "Query returned no row: $sql" }
            statement.getText(0)
        }
}
