package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name")
    suspend fun all(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE product_id = :productId LIMIT 1")
    suspend fun findById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Query("UPDATE products SET name = :name, unit = :unit WHERE product_id = :productId")
    suspend fun updateIdentity(productId: Long, name: String, unit: String): Int
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE account_id = :accountId LIMIT 1")
    suspend fun findById(accountId: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE category = :category ORDER BY name")
    suspend fun findByCategory(category: String): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Query("UPDATE accounts SET name = :name WHERE account_id = :accountId")
    suspend fun updateName(accountId: Long, name: String): Int
}

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: EventEntity): Long

    @Query("SELECT * FROM events WHERE event_id = :eventId LIMIT 1")
    suspend fun findById(eventId: Long): EventEntity?

    @Query("SELECT * FROM events ORDER BY occurred_at DESC, event_id DESC")
    suspend fun findAllDescending(): List<EventEntity>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int
}

@Dao
interface InventoryLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: InventoryLogEntity): Long

    @Query("SELECT * FROM inventory_log WHERE event_id = :eventId ORDER BY log_id")
    suspend fun findByEventId(eventId: Long): List<InventoryLogEntity>
}

@Dao
interface FinancialLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: FinancialLogEntity): Long

    @Query("SELECT * FROM financial_log WHERE event_id = :eventId ORDER BY log_id")
    suspend fun findByEventId(eventId: Long): List<FinancialLogEntity>

    @Query(
        """
        SELECT
            fl.log_id AS logId,
            fl.event_id AS eventId,
            e.created_at AS createdAt,
            e.occurred_at AS occurredAt,
            e.event_type AS eventType,
            e.channel AS channel,
            e.corrects_event_id AS correctsEventId,
            CASE WHEN EXISTS (
                SELECT 1 FROM events child
                WHERE child.corrects_event_id = e.event_id
            ) THEN 1 ELSE 0 END AS superseded,
            fl.amount AS movementAmount,
            e.notes AS notes
        FROM financial_log fl
        INNER JOIN events e ON e.event_id = fl.event_id
        WHERE fl.account_id = :accountId
        ORDER BY e.occurred_at ASC, e.event_id ASC, fl.log_id ASC
        """
    )
    suspend fun findAccountLedger(accountId: Long): List<AccountLedgerMovementRow>
}

@Dao
interface SettingsDao {
    @Query("SELECT setting_value FROM settings WHERE setting_key = :key LIMIT 1")
    suspend fun getValue(key: String): Double?

    @Query("SELECT * FROM settings ORDER BY setting_key")
    suspend fun getAll(): List<SettingEntity>

    @Query("UPDATE settings SET setting_value = :value, updated_at = CURRENT_TIMESTAMP WHERE setting_key = :key")
    suspend fun updateValue(key: String, value: Double): Int
}

@Dao
interface ReadModelDao {
    @Query("SELECT * FROM current_stock ORDER BY product_id")
    suspend fun currentStock(): List<CurrentStockView>

    @Query("SELECT * FROM account_balances ORDER BY account_id")
    suspend fun accountBalances(): List<AccountBalanceView>

    @Query("SELECT * FROM account_balances WHERE account_id = :accountId LIMIT 1")
    suspend fun accountBalance(accountId: Long): AccountBalanceView?

    @Query("SELECT * FROM purchase_calculation_audit WHERE event_id = :eventId LIMIT 1")
    suspend fun purchaseAudit(eventId: Long): PurchaseCalculationAuditView?

    @Query("SELECT * FROM sale_calculation_audit WHERE event_id = :eventId LIMIT 1")
    suspend fun saleAudit(eventId: Long): SaleCalculationAuditView?
}


@Dao
interface AnalyticsReadDao {
    /**
     * Aggregates only effective (leaf) events for the requested occurred_at
     * range. A corrected event replaces the event it corrects for reporting
     * purposes, so the original is excluded whenever it has a child correction.
     */
    @Query("""
        SELECT
            COUNT(*) AS eventCount,
            COALESCE(SUM(CASE WHEN event_type = 'SALE' THEN COALESCE(sale_value, 0) ELSE 0 END), 0) AS salesValue,
            COALESCE(SUM(CASE WHEN event_type = 'SALE' THEN COALESCE(sale_quantity, 0) ELSE 0 END), 0) AS saleQuantity,
            COALESCE(SUM(CASE WHEN event_type = 'PURCHASE_RECEIPT' THEN COALESCE(amount_owed, 0) ELSE 0 END), 0) AS purchaseAmount,
            COALESCE(SUM(CASE WHEN event_type = 'PURCHASE_RECEIPT' THEN COALESCE(raw_liters, 0) ELSE 0 END), 0) AS purchasedMilkLiters,
            COALESCE(SUM(CASE WHEN event_type = 'PRODUCTION' THEN COALESCE(yogurt_kg_produced, 0) ELSE 0 END), 0) AS producedYogurtKg,
            COALESCE(SUM(CASE WHEN event_type = 'PRODUCTION' THEN COALESCE(milk_liters_used, 0) ELSE 0 END), 0) AS productionMilkUsedLiters,
            COALESCE(SUM(CASE WHEN event_type = 'VENDOR_PAYMENT' THEN COALESCE(amount, 0) ELSE 0 END), 0) AS vendorPayments,
            COALESCE(SUM(CASE WHEN event_type = 'CREDIT_COLLECTION' THEN COALESCE(amount, 0) ELSE 0 END), 0) AS creditCollections,
            COALESCE(SUM(CASE WHEN event_type = 'EXPENSE' THEN COALESCE(amount, 0) ELSE 0 END), 0) AS operatingExpenses,
            COALESCE(SUM(CASE WHEN event_type = 'SALARY_PAYMENT' THEN COALESCE(amount, 0) ELSE 0 END), 0) AS salaryPayments
        FROM events e
        WHERE e.occurred_at >= :startDateInclusive
          AND e.occurred_at < :endDateExclusive
          AND NOT EXISTS (
              SELECT 1
              FROM events child
              WHERE child.corrects_event_id = e.event_id
          )
    """)
    suspend fun periodSummary(
        startDateInclusive: String,
        endDateExclusive: String,
    ): AnalyticsPeriodRow

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN e.event_type = 'PURCHASE_RECEIPT' THEN COALESCE(e.amount_owed, 0) ELSE 0 END), 0) AS milkPurchaseAmount,
            COALESCE(SUM(CASE WHEN e.event_type = 'SALE' THEN COALESCE(e.sale_value, 0) ELSE 0 END), 0) AS salesValue,
            COALESCE(SUM(CASE WHEN e.event_type = 'SALE' AND p.name = 'Milk' THEN COALESCE(e.sale_value, 0) ELSE 0 END), 0) AS milkSalesValue,
            COALESCE(SUM(CASE WHEN e.event_type = 'SALE' AND p.name = 'Yogurt' THEN COALESCE(e.sale_value, 0) ELSE 0 END), 0) AS yogurtSalesValue,
            COALESCE(SUM(CASE WHEN e.event_type = 'CREDIT_COLLECTION' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS creditCollections,
            COALESCE(SUM(CASE WHEN e.event_type = 'SALARY_PAYMENT' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS salaryPayments,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: Rent' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS rent,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: Electricity' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS electricity,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: Internet' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS internet,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: Water' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS water,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: Fuel' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS fuel,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: Poly bags' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS polyBags,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: LPG' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS lpg,
            COALESCE(SUM(CASE WHEN e.event_type = 'EXPENSE' AND a.name = 'Expense: Other' THEN COALESCE(e.amount, 0) ELSE 0 END), 0) AS other
        FROM events e
        LEFT JOIN inventory_log il ON il.event_id = e.event_id
        LEFT JOIN products p ON p.product_id = il.product_id
        LEFT JOIN financial_log fl ON fl.event_id = e.event_id AND fl.amount > 0
        LEFT JOIN accounts a ON a.account_id = fl.account_id AND a.category = 'Operating Expense'
        WHERE e.occurred_at >= :startDateInclusive
          AND e.occurred_at < :endDateExclusive
          AND NOT EXISTS (
              SELECT 1 FROM events child WHERE child.corrects_event_id = e.event_id
          )
    """)
    suspend fun budgetActuals(
        startDateInclusive: String,
        endDateExclusive: String,
    ): AnalyticsBudgetActualRow
}

@Dao
interface BudgetPlanDao {
    @Query("""
        SELECT * FROM budget_plans
        WHERE category = :category
          AND period_start = :periodStart
          AND archived = 0
        ORDER BY budget_id DESC
        LIMIT 1
    """)
    suspend fun findActive(category: String, periodStart: String): BudgetPlanEntity?

    @Query("""
        SELECT * FROM budget_plans
        WHERE category = :category
        ORDER BY period_start DESC, budget_id DESC
    """)
    suspend fun findAllForCategory(category: String): List<BudgetPlanEntity>

    @Query("""
        SELECT * FROM budget_plans
        WHERE category = :category
          AND period_start < :periodEndExclusive
        ORDER BY period_start DESC, budget_id DESC
        LIMIT 1
    """)
    suspend fun findLatestBefore(category: String, periodEndExclusive: String): BudgetPlanEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BudgetPlanEntity): Long

    @Query("UPDATE budget_plans SET archived = 1 WHERE budget_id = :budgetId")
    suspend fun archive(budgetId: Long): Int
}

@Dao
interface CollectionsTargetDao {
    @Query("""
        SELECT * FROM collections_targets
        WHERE week_start = :weekStart
          AND archived = 0
        ORDER BY target_id DESC
        LIMIT 1
    """)
    suspend fun findActive(weekStart: String): CollectionsTargetEntity?

    @Query("""
        SELECT * FROM collections_targets
        ORDER BY week_start DESC, target_id DESC
    """)
    suspend fun findAll(): List<CollectionsTargetEntity>

    @Query("""
        SELECT * FROM collections_targets
        WHERE week_start < :weekStart
        ORDER BY week_start DESC, target_id DESC
        LIMIT 1
    """)
    suspend fun findPrevious(weekStart: String): CollectionsTargetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CollectionsTargetEntity): Long

    @Query("UPDATE collections_targets SET archived = 1 WHERE target_id = :targetId")
    suspend fun archive(targetId: Long): Int
}
