package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query

/**
 * Row-level change feed for sync push. Capture triggers on the synced tables
 * enqueue (table, row uid) pairs here; nothing consumes the queue until the
 * push engine exists. `INSERT OR REPLACE` moves a re-enqueued row to the
 * tail, so draining by outbox_id preserves strict first-change order for the
 * dependencies that matter — an event always precedes the daily close that
 * covers it, and an original always precedes its correction.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["table_name", "row_uid"], unique = true)],
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "outbox_id")
    val outboxId: Long = 0,
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "row_uid")
    val rowUid: String,
    @ColumnInfo(name = "enqueued_at")
    val enqueuedAt: String,
)

/**
 * Cursor of the last applied pull batch (singleton row, state_id = 1). It
 * rides inside backup format 4 so a replaced device resumes exactly where
 * its snapshot left off instead of re-pulling history.
 */
@Entity(tableName = "sync_pull_state")
data class SyncPullStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "state_id")
    val stateId: Int = 1,
    @ColumnInfo(name = "last_server_seq")
    val lastServerSeq: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox ORDER BY outbox_id LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<SyncOutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE outbox_id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Long

    @Query("DELETE FROM sync_outbox")
    suspend fun deleteAll()
}

@Dao
interface SyncPullStateDao {
    @Query("SELECT * FROM sync_pull_state WHERE state_id = 1")
    suspend fun find(): SyncPullStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncPullStateEntity)
}

/**
 * Read-side queries for the sync push envelopes: rows by uid, their event
 * children, and their profile satellites. Read-only by design — the outbox
 * and the capture triggers own all writes.
 */
@Dao
interface SyncEnvelopeDao {
    @Query("SELECT * FROM events WHERE event_uid IN (:uids)")
    suspend fun eventsByUid(uids: List<String>): List<EventEntity>

    @Query("SELECT * FROM products WHERE uid IN (:uids)")
    suspend fun productsByUid(uids: List<String>): List<ProductEntity>

    @Query("SELECT * FROM accounts WHERE uid IN (:uids)")
    suspend fun accountsByUid(uids: List<String>): List<AccountEntity>

    @Query("SELECT * FROM budget_plans WHERE uid IN (:uids)")
    suspend fun budgetsByUid(uids: List<String>): List<BudgetPlanEntity>

    @Query("SELECT * FROM collections_targets WHERE uid IN (:uids)")
    suspend fun targetsByUid(uids: List<String>): List<CollectionsTargetEntity>

    @Query("SELECT * FROM deliveries WHERE uid IN (:uids)")
    suspend fun deliveriesByUid(uids: List<String>): List<DeliveryEntity>

    @Query("SELECT * FROM attendance_records WHERE uid IN (:uids)")
    suspend fun attendanceByUid(uids: List<String>): List<AttendanceRecordEntity>

    @Query("SELECT * FROM delivery_status_history WHERE uid IN (:uids)")
    suspend fun deliveryStatusesByUid(uids: List<String>): List<DeliveryStatusEntity>

    @Query("SELECT * FROM daily_close_actions WHERE uid IN (:uids)")
    suspend fun dailyCloseActionsByUid(uids: List<String>): List<DailyCloseActionEntity>

    @Query("SELECT * FROM physical_cash_counts WHERE uid IN (:uids)")
    suspend fun physicalCashCountsByUid(uids: List<String>): List<PhysicalCashCountEntity>

    @Query("SELECT * FROM settings WHERE setting_key IN (:keys)")
    suspend fun settingsByKeys(keys: List<String>): List<SettingEntity>

    @Query("SELECT * FROM store_profile LIMIT 1")
    suspend fun storeProfile(): StoreProfileEntity?

    @Query("SELECT * FROM inventory_log WHERE event_id IN (:eventIds)")
    suspend fun inventoryLogsForEvents(eventIds: List<Long>): List<InventoryLogEntity>

    @Query("SELECT * FROM financial_log WHERE event_id IN (:eventIds)")
    suspend fun financialLogsForEvents(eventIds: List<Long>): List<FinancialLogEntity>

    @Query("SELECT * FROM spoilage_details WHERE event_id IN (:eventIds)")
    suspend fun spoilageDetailsForEvents(eventIds: List<Long>): List<SpoilageDetailEntity>

    @Query("SELECT * FROM event_actor_links WHERE event_id IN (:eventIds)")
    suspend fun actorLinksForEvents(eventIds: List<Long>): List<EventActorLinkEntity>

    @Query("SELECT * FROM product_profiles WHERE product_id IN (:productIds)")
    suspend fun productProfilesFor(productIds: List<Long>): List<ProductProfileEntity>

    @Query("SELECT * FROM customer_profiles WHERE account_id IN (:accountIds)")
    suspend fun customerProfilesFor(accountIds: List<Long>): List<CustomerProfileEntity>

    @Query("SELECT * FROM vendor_profiles WHERE account_id IN (:accountIds)")
    suspend fun vendorProfilesFor(accountIds: List<Long>): List<VendorProfileEntity>

    @Query("SELECT * FROM employee_profiles WHERE account_id IN (:accountIds)")
    suspend fun employeeProfilesFor(accountIds: List<Long>): List<EmployeeProfileEntity>

    @Query("SELECT * FROM expense_category_profiles WHERE account_id IN (:accountIds)")
    suspend fun expenseCategoryProfilesFor(accountIds: List<Long>): List<ExpenseCategoryProfileEntity>
}
