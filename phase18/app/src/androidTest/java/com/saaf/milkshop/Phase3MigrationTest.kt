package com.saaf.milkshop

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.LegacyUid
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                .addMigrations(MilkShopDatabase.MIGRATION_3_4, MilkShopDatabase.MIGRATION_4_5, MilkShopDatabase.MIGRATION_5_6, MilkShopDatabase.MIGRATION_6_7, MilkShopDatabase.MIGRATION_7_8, MilkShopDatabase.MIGRATION_8_9, MilkShopDatabase.MIGRATION_9_10)
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

    @Test
    fun migration7To8_preservesExistingLedger_andCreatesNoArtificialCostTruth() = runBlocking {
        context.deleteDatabase(databaseName)
        helper.createDatabase(7).apply {
            execSQL(
                """
                INSERT INTO events(created_at, occurred_at, event_type, amount, surcharge_applied)
                VALUES ('2026-08-27T00:00:00Z', '2026-08-27', 'VENDOR_PAYMENT', 100, 0)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(8, listOf(MilkShopDatabase.MIGRATION_7_8)).use { connection ->
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM events"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM costing_policy_versions"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM costing_policy_components"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM inventory_cost_basis_entries"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM yield_policy_versions"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM production_loss_assessments"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM inventory_cost_movements"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM cost_projection_state"))

            connection.execSQL(
                """
                INSERT INTO app_users(login_name, display_name, role, status, credential_setup_required, created_at, updated_at)
                VALUES ('migration.owner', 'Migration Owner', 'owner', 'active', 0, '2026-08-27T00:00:00Z', '2026-08-27T00:00:00Z')
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO costing_policy_versions(calendar_month, normal_capacity_kg, review_reason, approved_at, approved_by_user_id)
                VALUES ('2026-08', '100', 'monthly', '2026-08-27T00:00:00Z', 1)
                """.trimIndent()
            )
            var immutable = false
            try {
                connection.execSQL("UPDATE costing_policy_versions SET notes = 'changed' WHERE policy_id = 1")
            } catch (_: Exception) {
                immutable = true
            }
            assertEquals(true, immutable)
        }
    }

    @Test
    fun migration8To9_isAdditive_preservesRows_andCreatesNoServerIdentityState() = runBlocking {
        context.deleteDatabase(databaseName)
        helper.createDatabase(8).apply {
            execSQL("INSERT INTO products(product_id, name, unit) VALUES (1, 'Milk', 'Liters')")
            execSQL(
                """
                INSERT INTO app_users(login_name, display_name, role, status, credential_setup_required, created_at, updated_at)
                VALUES ('migration.owner', 'Migration Owner', 'owner', 'active', 0, '2026-08-27T00:00:00Z', '2026-08-27T00:00:00Z')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(9, listOf(MilkShopDatabase.MIGRATION_8_9)).use { connection ->
            // Every schema-8 row survives the migration untouched.
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM products"))
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM app_users"))
            // The new Account Bridge tables start empty: an upgraded installation
            // is unbound and must invent no server identity state.
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM account_installation_binding"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM server_identity_links"))
            assertEquals(0L, connection.scalarLong("SELECT COUNT(*) FROM server_shop_snapshots"))
            // The identity link is FK-bound to the preserved user row.
            connection.execSQL(
                """
                INSERT INTO server_identity_links(local_user_id, server_staff_id, business_id, status)
                VALUES (1, '11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222', 'pending')
                """.trimIndent()
            )
        }
    }

    @Test
    fun legacyUid_knownAnswerVectorsAreFrozen() {
        // Any change to these values forks row identity for every legacy row;
        // if one legitimately changes, regenerate the vector and say so loudly.
        assertEquals("u1_7f6844c02d08f7b4749c8b99cfdd1cb5", LegacyUid.event(1, "SALE", "2026-01-01T00:00:00Z"))
        assertEquals("u1_8c2d1bb8208e448bca8b7293ba1e97bc", LegacyUid.event(2, "SALE", "2026-01-02T00:00:00Z"))
        assertEquals("u1_798c656933df2ff0f0af7163c181dd69", LegacyUid.product(7, "Butter", "Kg"))
        assertEquals("u1_eb7360e6f91498b315250dcbad1d61d8", LegacyUid.account(5, "Customer: Noor Store", "Customer"))
        assertEquals("u1_42d05bdd864d13f54df59bb7107a4ccd", LegacyUid.dailyCloseAction(3, "2026-01-01", "close", "2026-01-01T20:00:00Z"))
        // Seed identities are fixed well-known values shared by every install.
        assertEquals("u1_seed_products_Milk", LegacyUid.seedProductUid("Milk", "Liters"))
        assertEquals("u1_seed_accounts_Cash", LegacyUid.seedAccountUid("Cash", "Cash/Bank"))
        assertNull(LegacyUid.seedProductUid("Milk", "Kg"))
        assertNull(LegacyUid.seedAccountUid("Cash", "Vendor"))
    }

    @Test
    fun migration9To10_backfillsDeterministicUniqueUids_andPreservesRows() = runBlocking {
        val first = migrateSchema9SampleTo10()

        // Seed rows adopt the shared well-known identities; custom rows adopt
        // the frozen deterministic identities for their immutable fields.
        assertEquals(listOf("u1_seed_products_Milk", "u1_798c656933df2ff0f0af7163c181dd69"), first.getValue("products"))
        assertEquals(listOf("u1_seed_accounts_Cash", "u1_eb7360e6f91498b315250dcbad1d61d8"), first.getValue("accounts"))
        assertEquals(
            listOf("u1_7f6844c02d08f7b4749c8b99cfdd1cb5", "u1_8c2d1bb8208e448bca8b7293ba1e97bc"),
            first.getValue("events"),
        )
        assertEquals(listOf("u1_42d05bdd864d13f54df59bb7107a4ccd"), first.getValue("closes"))
        first.values.forEach { uids ->
            assertEquals(uids.size, uids.distinct().size)
            uids.forEach { uid -> assertTrue(uid.isNotBlank()) }
        }

        // Determinism: migrating the identical schema-9 data again yields the
        // identical identities, which is what makes legacy-backup imports
        // idempotent across devices.
        val second = migrateSchema9SampleTo10()
        assertEquals(first, second)
    }

    private suspend fun migrateSchema9SampleTo10(): Map<String, List<String>> {
        context.deleteDatabase(databaseName)
        helper.createDatabase(9).apply {
            execSQL("INSERT INTO products(product_id, name, unit) VALUES (1, 'Milk', 'Liters')")
            execSQL("INSERT INTO products(product_id, name, unit) VALUES (7, 'Butter', 'Kg')")
            execSQL("INSERT INTO accounts(account_id, name, type, category) VALUES (1, 'Cash', 'Asset', 'Cash/Bank')")
            execSQL("INSERT INTO accounts(account_id, name, type, category) VALUES (5, 'Customer: Noor Store', 'Asset', 'Customer')")
            execSQL("INSERT INTO events(event_id, created_at, occurred_at, event_type, surcharge_applied) VALUES (1, '2026-01-01T00:00:00Z', '2026-01-01', 'SALE', 0)")
            execSQL("INSERT INTO events(event_id, created_at, occurred_at, event_type, corrects_event_id, surcharge_applied) VALUES (2, '2026-01-02T00:00:00Z', '2026-01-01', 'SALE', 1, 0)")
            execSQL("INSERT INTO physical_cash_counts(count_id, account_id, business_date, counted_amount, counted_at) VALUES (2, 1, '2026-01-01', 500, '2026-01-01T20:00:00Z')")
            execSQL("INSERT INTO daily_close_actions(action_id, business_date, action, physical_cash_count_id, actor_name, actor_role, created_at) VALUES (3, '2026-01-01', 'close', 2, 'Owner', 'owner', '2026-01-01T20:00:00Z')")
            close()
        }
        helper.runMigrationsAndValidate(10, listOf(MilkShopDatabase.MIGRATION_9_10)).use { connection ->
            // Every schema-9 row survives the migration.
            assertEquals(2L, connection.scalarLong("SELECT COUNT(*) FROM products"))
            assertEquals(2L, connection.scalarLong("SELECT COUNT(*) FROM accounts"))
            assertEquals(2L, connection.scalarLong("SELECT COUNT(*) FROM events"))
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM physical_cash_counts"))
            assertEquals(1L, connection.scalarLong("SELECT COUNT(*) FROM daily_close_actions"))
            // Legacy events carry no origin: it is unknown, never invented.
            assertEquals("", connection.scalarText("SELECT origin_device_id FROM events WHERE event_id = 1"))
            return mapOf(
                "products" to connection.uidColumn("SELECT uid FROM products ORDER BY product_id"),
                "accounts" to connection.uidColumn("SELECT uid FROM accounts ORDER BY account_id"),
                "events" to connection.uidColumn("SELECT event_uid FROM events ORDER BY event_id"),
                "counts" to connection.uidColumn("SELECT uid FROM physical_cash_counts ORDER BY count_id"),
                "closes" to connection.uidColumn("SELECT uid FROM daily_close_actions ORDER BY action_id"),
            )
        }
    }

    private fun androidx.sqlite.SQLiteConnection.uidColumn(sql: String): List<String> =
        prepare(sql).use { statement ->
            buildList {
                while (statement.step()) add(statement.getText(0))
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
