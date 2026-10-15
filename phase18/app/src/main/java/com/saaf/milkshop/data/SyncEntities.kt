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
 * push engine exists. Re-enqueue keeps the original slot (position-preserving
 * triggers), so draining by outbox_id preserves strict first-change order for
 * the dependencies that matter — a parent row always precedes the events that
 * reference it, and an original always precedes its correction.
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
    // go 8: a HELD entry is excluded from pushes (halt-at-head FIFO order is
    // preserved for everything else) and stays visible to the pending-edit
    // guard, so its park keeps the local row protected until the owner rules
    // on it in the sync-review surface. keep-mine clears the flag (the entry
    // pushes, audited server-side); take-server deletes the entry (the park
    // unblocks and the server version applies).
    @ColumnInfo(name = "held", defaultValue = "0")
    val held: Boolean = false,
)

/**
 * Pulled rows the apply engine could not apply YET, kept so the pull cursor
 * can advance without losing them (C1). Two park reasons:
 *  - dependency failure: a referenced parent row is not local yet;
 *  - pending-edit guard: a local outbox entry for this uid still holds an
 *    unpushed edit that a pulled update must not overwrite.
 * Lifecycle: applying ANY version for the uid clears the park; a successful
 * push of the uid clears it (after the parked_seq conflict hint reached the
 * server); dependency parks are attempted once per batch, including after
 * their diagnostic retry counter reaches its ceiling.
 * Parks are transient retry state, not data — restore clears them and
 * resets the pull cursor so everything re-pulls.
 */
@Entity(
    tableName = "sync_parked_rows",
    primaryKeys = ["table_name", "row_uid"],
)
data class SyncParkedRowEntity(
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "row_uid")
    val rowUid: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "server_seq")
    val serverSeq: Long,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int,
    @ColumnInfo(name = "parked_at")
    val parkedAt: String,
)

/**
 * go 8: a restored outbox entry whose server row was last written by ANOTHER
 * device is probably doubly stale — it is held out of the push for an owner
 * decision instead of auto-pushing (LWW would make it win, audited but
 * likely wrong). The payload snapshot is display material for the review
 * surface; the authoritative local row stays untouched (its park holds until
 * the owner rules), so keep-mine simply clears the outbox held flag.
 */
@Entity(
    tableName = "sync_rebaseline_holds",
    primaryKeys = ["table_name", "row_uid"],
)
data class SyncRebaselineHoldEntity(
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "row_uid")
    val rowUid: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "origin_device")
    val originDevice: String?,
    @ColumnInfo(name = "server_seq")
    val serverSeq: Long,
    @ColumnInfo(name = "held_at")
    val heldAt: String,
)

@androidx.room3.Dao
interface SyncRebaselineHoldDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hold: SyncRebaselineHoldEntity)

    @Query("SELECT * FROM sync_rebaseline_holds ORDER BY held_at")
    suspend fun all(): List<SyncRebaselineHoldEntity>

    @Query("DELETE FROM sync_rebaseline_holds WHERE table_name = :tableName AND row_uid = :rowUid")
    suspend fun delete(tableName: String, rowUid: String)

    @Query("SELECT COUNT(*) FROM sync_rebaseline_holds")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM sync_rebaseline_holds")
    fun observeCount(): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT * FROM sync_rebaseline_holds ORDER BY held_at")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<SyncRebaselineHoldEntity>>

    @Query("DELETE FROM sync_rebaseline_holds")
    suspend fun clear()
}

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
    @Query("SELECT * FROM sync_outbox WHERE held = 0 ORDER BY outbox_id LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox ORDER BY outbox_id")
    suspend fun all(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<SyncOutboxEntity>)

    @Query("DELETE FROM sync_outbox WHERE table_name = :tableName AND row_uid = :rowUid")
    suspend fun deleteEntry(tableName: String, rowUid: String)

    @Query("UPDATE sync_outbox SET held = :held WHERE table_name = :tableName AND row_uid = :rowUid")
    suspend fun setHeld(tableName: String, rowUid: String, held: Boolean)

    /** Boundary marker for the option-Z live re-baseline enqueue. */
    @Query("SELECT COALESCE(MAX(outbox_id), 0) FROM sync_outbox")
    suspend fun maxOutboxId(): Long

    /** Holds exactly the entries inserted after [id] — pre-existing pending edits keep held=0. */
    @Query("UPDATE sync_outbox SET held = 1 WHERE outbox_id > :id")
    suspend fun holdAllAfter(id: Long)

    @Query("DELETE FROM sync_outbox WHERE outbox_id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Long

    @Query("DELETE FROM sync_outbox")
    suspend fun deleteAll()

    /** Nonzero when a pulled row's uid still has an unpushed local edit (C1 pending-edit guard). */
    @Query("SELECT COUNT(*) FROM sync_outbox WHERE table_name = :tableName AND row_uid = :rowUid")
    suspend fun countByTableAndUid(tableName: String, rowUid: String): Int
}

@Dao
interface SyncParkedRowDao {
    @Query("SELECT * FROM sync_parked_rows ORDER BY server_seq")
    suspend fun all(): List<SyncParkedRowEntity>

    @Query("SELECT * FROM sync_parked_rows WHERE table_name = :tableName AND row_uid = :rowUid")
    suspend fun find(tableName: String, rowUid: String): SyncParkedRowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(park: SyncParkedRowEntity)

    @Query("DELETE FROM sync_parked_rows WHERE table_name = :tableName AND row_uid = :rowUid")
    suspend fun delete(tableName: String, rowUid: String)

    @Query("DELETE FROM sync_parked_rows")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM sync_parked_rows")
    suspend fun count(): Int
}

@Dao
interface SyncPullStateDao {
    @Query("SELECT * FROM sync_pull_state WHERE state_id = 1")
    suspend fun find(): SyncPullStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncPullStateEntity)
}

@Dao
interface SyncApplyDao {
    /** Preserve the installed guard exactly across a trusted remote history insert. */
    @Query("SELECT sql FROM sqlite_master WHERE type = 'trigger' AND name = 'validate_verified_daily_close_actor'")
    suspend fun verifiedCloseActorTriggerSql(): String?

    @Query("""UPDATE budget_plans SET archived = :archived, sync_updated_at = :stamp WHERE uid = :uid""")
    suspend fun updateBudgetStateByUid(uid: String, archived: Boolean, stamp: String): Int

    @Query("""UPDATE collections_targets SET archived = :archived, sync_updated_at = :stamp WHERE uid = :uid""")
    suspend fun updateCollectionsTargetStateByUid(uid: String, archived: Boolean, stamp: String): Int

    @Query("UPDATE products SET name = :name, unit = :unit, sync_updated_at = :stamp WHERE uid = :uid")
    suspend fun updateProductByUid(uid: String, name: String, unit: String, stamp: String): Int

    @Query("UPDATE accounts SET name = :name, type = :type, category = :category, sync_updated_at = :stamp WHERE uid = :uid")
    suspend fun updateAccountByUid(uid: String, name: String, type: String, category: String, stamp: String): Int

    @Query("UPDATE settings SET setting_value = :value, updated_at = :stamp WHERE setting_key = :key")
    suspend fun updateSettingByKey(key: String, value: Double, stamp: String): Int

    @Query(
        """UPDATE deliveries SET customer_account_id = :customerAccountId, product_id = :productId,
           quantity = :quantity, route = :route, business_date = :businessDate,
           sale_event_id = :saleEventId, notes = :notes, archived = :archived,
           sync_updated_at = :stamp WHERE uid = :uid""",
    )
    suspend fun updateDeliveryByUid(
        uid: String,
        customerAccountId: Long,
        productId: Long,
        quantity: Double,
        route: String?,
        businessDate: String,
        saleEventId: Long?,
        notes: String?,
        archived: Boolean,
        stamp: String,
    ): Int

    @Query(
        """UPDATE attendance_records SET employee_account_id = :employeeAccountId,
           business_date = :businessDate, status = :status, notes = :notes,
           supersedes_id = :supersedesId, archived = :archived,
           sync_updated_at = :stamp WHERE uid = :uid""",
    )
    suspend fun updateAttendanceByUid(
        uid: String,
        employeeAccountId: Long,
        businessDate: String,
        status: String,
        notes: String?,
        supersedesId: Long?,
        archived: Boolean,
        stamp: String,
    ): Int
}

/**
 * Read-side queries for the sync push envelopes: rows by uid, their event
 * children, and their profile satellites. Read-only by design — the outbox
 * and the capture triggers own all writes.
 */
@Dao
interface SyncEnvelopeDao {
    @Query("SELECT * FROM budget_plans WHERE supersedes_id = :parentId AND uid != :uid LIMIT 1")
    suspend fun otherBudgetRevision(parentId: Long, uid: String): BudgetPlanEntity?

    @Query("SELECT * FROM collections_targets WHERE supersedes_id = :parentId AND uid != :uid LIMIT 1")
    suspend fun otherCollectionsRevision(parentId: Long, uid: String): CollectionsTargetEntity?

    @Query("SELECT * FROM events WHERE event_uid IN (:uids)")
    suspend fun eventsByUid(uids: List<String>): List<EventEntity>

    @Query("SELECT * FROM events WHERE event_id IN (:ids)")
    suspend fun eventsByIds(ids: List<Long>): List<EventEntity>

    @Query("SELECT * FROM daily_close_actions WHERE action_id IN (:ids)")
    suspend fun dailyCloseActionsByIds(ids: List<Long>): List<DailyCloseActionEntity>

    @Query("SELECT * FROM attendance_records WHERE attendance_id IN (:ids)")
    suspend fun attendanceByIds(ids: List<Long>): List<AttendanceRecordEntity>

    @Query("SELECT * FROM budget_plans WHERE budget_id IN (:ids)")
    suspend fun budgetsByIds(ids: List<Long>): List<BudgetPlanEntity>

    @Query("SELECT * FROM collections_targets WHERE target_id IN (:ids)")
    suspend fun targetsByIds(ids: List<Long>): List<CollectionsTargetEntity>

    @Query("SELECT * FROM products WHERE uid IN (:uids)")
    suspend fun productsByUid(uids: List<String>): List<ProductEntity>

    @Query("SELECT * FROM products WHERE product_id IN (:ids)")
    suspend fun productsByIds(ids: List<Long>): List<ProductEntity>

    @Query("SELECT * FROM accounts WHERE account_id IN (:ids)")
    suspend fun accountsByIds(ids: List<Long>): List<AccountEntity>

    @Query("SELECT * FROM deliveries WHERE delivery_id IN (:ids)")
    suspend fun deliveriesByIds(ids: List<Long>): List<DeliveryEntity>

    @Query("SELECT * FROM physical_cash_counts WHERE count_id IN (:ids)")
    suspend fun physicalCashCountsByIds(ids: List<Long>): List<PhysicalCashCountEntity>

    @Query("SELECT * FROM events WHERE corrects_event_id = :eventId")
    suspend fun correctionsOf(eventId: Long): List<EventEntity>

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

    @Query("SELECT * FROM opening_batches WHERE event_id IN (:eventIds)")
    suspend fun openingBatchesForEvents(eventIds: List<Long>): List<OpeningBatchEntity>

    @Query("SELECT * FROM opening_inventory_entries WHERE event_id IN (:eventIds)")
    suspend fun openingInventoryForEvents(eventIds: List<Long>): List<OpeningInventoryEntryEntity>

    @Query("SELECT * FROM opening_account_entries WHERE event_id IN (:eventIds)")
    suspend fun openingAccountsForEvents(eventIds: List<Long>): List<OpeningAccountEntryEntity>

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
