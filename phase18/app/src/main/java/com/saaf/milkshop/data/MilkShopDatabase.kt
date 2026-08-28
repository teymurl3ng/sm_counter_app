package com.saaf.milkshop.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

@Database(
    version = 8,
    exportSchema = true,
    entities = [
        ProductEntity::class,
        AccountEntity::class,
        EventEntity::class,
        InventoryLogEntity::class,
        FinancialLogEntity::class,
        SettingEntity::class,
        BudgetPlanEntity::class,
        CollectionsTargetEntity::class,
        ProductProfileEntity::class,
        CustomerProfileEntity::class,
        VendorProfileEntity::class,
        EmployeeProfileEntity::class,
        AttendanceRecordEntity::class,
        DeliveryEntity::class,
        DeliveryStatusEntity::class,
        SpoilageDetailEntity::class,
        PhysicalCashCountEntity::class,
        ExpenseCategoryProfileEntity::class,
        StoreProfileEntity::class,
        OpeningBatchEntity::class,
        OpeningInventoryEntryEntity::class,
        OpeningAccountEntryEntity::class,
        DailyCloseActionEntity::class,
        AppUserEntity::class,
        UserCredentialEntity::class,
        SavedRecoveryCodeEntity::class,
        SecurityAuditEntryEntity::class,
        EventActorLinkEntity::class,
        CostingPolicyVersionEntity::class,
        CostingPolicyComponentEntity::class,
        InventoryCostBasisEntity::class,
        YieldPolicyVersionEntity::class,
        ProductionLossAssessmentEntity::class,
        InventoryCostMovementEntity::class,
        CostProjectionStateEntity::class,
    ],
    views = [
        CurrentStockView::class,
        AccountBalanceView::class,
        PurchaseCalculationAuditView::class,
        SaleCalculationAuditView::class,
    ],
)
abstract class MilkShopDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun accountDao(): AccountDao
    abstract fun eventDao(): EventDao
    abstract fun inventoryLogDao(): InventoryLogDao
    abstract fun financialLogDao(): FinancialLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun readModelDao(): ReadModelDao
    abstract fun analyticsReadDao(): AnalyticsReadDao
    abstract fun budgetPlanDao(): BudgetPlanDao
    abstract fun collectionsTargetDao(): CollectionsTargetDao
    abstract fun backupDataDao(): BackupDataDao
    abstract fun productProfileDao(): ProductProfileDao
    abstract fun customerProfileDao(): CustomerProfileDao
    abstract fun vendorProfileDao(): VendorProfileDao
    abstract fun employeeProfileDao(): EmployeeProfileDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun spoilageDao(): SpoilageDao
    abstract fun physicalCashCountDao(): PhysicalCashCountDao
    abstract fun expenseCategoryProfileDao(): ExpenseCategoryProfileDao
    abstract fun storeProfileDao(): StoreProfileDao
    abstract fun openingStateDao(): OpeningStateDao
    abstract fun dailyCloseDao(): DailyCloseDao
    abstract fun appUserDao(): AppUserDao
    abstract fun userCredentialDao(): UserCredentialDao
    abstract fun savedRecoveryCodeDao(): SavedRecoveryCodeDao
    abstract fun securityAuditDao(): SecurityAuditDao
    abstract fun eventActorLinkDao(): EventActorLinkDao
    abstract fun costingPolicyDao(): CostingPolicyDao
    abstract fun inventoryCostBasisDao(): InventoryCostBasisDao
    abstract fun yieldPolicyDao(): YieldPolicyDao
    abstract fun productionLossAssessmentDao(): ProductionLossAssessmentDao
    abstract fun inventoryCostProjectionDao(): InventoryCostProjectionDao

    companion object {
        private const val DATABASE_NAME = "milk_shop.db"
        val MIGRATION_1_2 = object : androidx.room3.migration.Migration(1, 2) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                connection.execSQL("ALTER TABLE events ADD COLUMN milk_liters_used REAL")
                connection.execSQL("ALTER TABLE events ADD COLUMN yogurt_kg_produced REAL")
                connection.execSQL("ALTER TABLE events ADD COLUMN amount REAL")

                DatabaseSql.triggerNames.forEach { name ->
                    connection.execSQL("DROP TRIGGER IF EXISTS $name")
                }
                DatabaseSql.version4CompatibleTriggers.forEach(connection::execSQL)
            }
        }


        val MIGRATION_2_3 = object : androidx.room3.migration.Migration(2, 3) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_plans (
                        budget_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        period_start TEXT NOT NULL,
                        period_end_exclusive TEXT NOT NULL,
                        planned_liters REAL,
                        planned_amount REAL,
                        vendor_rate_snapshot REAL,
                        retail_rate_snapshot REAL,
                        created_at TEXT NOT NULL,
                        supersedes_id INTEGER,
                        archived INTEGER NOT NULL DEFAULT 0
                    )
                """)
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_budget_plans_category_period_start_archived ON budget_plans(category, period_start, archived)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_budget_plans_supersedes_id ON budget_plans(supersedes_id)")
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS collections_targets (
                        target_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        week_start TEXT NOT NULL,
                        week_end_exclusive TEXT NOT NULL,
                        target_amount REAL NOT NULL,
                        created_at TEXT NOT NULL,
                        supersedes_id INTEGER,
                        archived INTEGER NOT NULL DEFAULT 0
                    )
                """)
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_collections_targets_week_start_archived ON collections_targets(week_start, archived)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_collections_targets_supersedes_id ON collections_targets(supersedes_id)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room3.migration.Migration(3, 4) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                Phase3MigrationSql.createStatements.forEach(connection::execSQL)
                Phase3MigrationSql.backfillStatements.forEach(connection::execSQL)

                DatabaseSql.triggerNames.forEach { name ->
                    connection.execSQL("DROP TRIGGER IF EXISTS $name")
                }
                DatabaseSql.version4CompatibleTriggers.forEach(connection::execSQL)
            }
        }

        val MIGRATION_4_5 = object : androidx.room3.migration.Migration(4, 5) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                Phase3OpeningMigrationSql.createStatements.forEach(connection::execSQL)

                DatabaseSql.triggerNames.forEach { name ->
                    connection.execSQL("DROP TRIGGER IF EXISTS $name")
                }
                DatabaseSql.version5CompatibleTriggers.forEach(connection::execSQL)
            }
        }

        val MIGRATION_5_6 = object : androidx.room3.migration.Migration(5, 6) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                Phase3DailyCloseMigrationSql.createStatements.forEach(connection::execSQL)

                DatabaseSql.triggerNames.forEach { name ->
                    connection.execSQL("DROP TRIGGER IF EXISTS $name")
                }
                DatabaseSql.createTriggers.forEach(connection::execSQL)
            }
        }

        val MIGRATION_6_7 = object : androidx.room3.migration.Migration(6, 7) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                Phase3SecurityMigrationSql.createStatements.forEach(connection::execSQL)
                Phase3SecurityMigrationSql.triggerNames.forEach { name ->
                    connection.execSQL("DROP TRIGGER IF EXISTS $name")
                }
                Phase3SecurityMigrationSql.createTriggers.forEach(connection::execSQL)
            }
        }

        val MIGRATION_7_8 = object : androidx.room3.migration.Migration(7, 8) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                Phase3CostingMigrationSql.createStatements.forEach(connection::execSQL)
                Phase3CostingMigrationSql.immutableTriggers.forEach(connection::execSQL)
            }
        }

        fun create(context: Context): MilkShopDatabase =
            Room.databaseBuilder<MilkShopDatabase>(
                context = context.applicationContext,
                name = DATABASE_NAME,
            )
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .addCallback(DatabaseCallback())
                .build()

        /** Test-only database builder. It uses the exact same schema callback. */
        fun createInMemory(): MilkShopDatabase =
            Room.inMemoryDatabaseBuilder<MilkShopDatabase>()
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .addCallback(DatabaseCallback())
                .build()
    }

    private class DatabaseCallback : Callback() {
        override suspend fun onCreate(connection: androidx.sqlite.SQLiteConnection) {
            DatabaseSql.createTriggers.forEach(connection::execSQL)
            DatabaseSql.seedStatements.forEach(connection::execSQL)
            Phase3MigrationSql.backfillStatements.forEach(connection::execSQL)
            Phase3SecurityMigrationSql.createTriggers.forEach(connection::execSQL)
            Phase3CostingMigrationSql.immutableTriggers.forEach(connection::execSQL)
        }

        override suspend fun onOpen(connection: androidx.sqlite.SQLiteConnection) {
            // SQLite foreign-key enforcement is a connection setting. Reassert
            // it whenever a database connection is opened.
            connection.execSQL("PRAGMA foreign_keys = ON")
        }
    }
}
