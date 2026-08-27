package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface StoreProfileDao {
    @Query("SELECT * FROM store_profile WHERE store_profile_id = 1 LIMIT 1")
    suspend fun get(): StoreProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: StoreProfileEntity)
}

@Dao
interface OpeningStateDao {
    @Query(
        """
        SELECT COUNT(*)
        FROM opening_batches ob
        INNER JOIN events e ON e.event_id = ob.event_id
        WHERE e.corrects_event_id IS NULL
        """
    )
    suspend fun initialCount(): Int

    @Query(
        """
        SELECT ob.*
        FROM opening_batches ob
        INNER JOIN events e ON e.event_id = ob.event_id
        WHERE NOT EXISTS (
            SELECT 1 FROM events child WHERE child.corrects_event_id = e.event_id
        )
        ORDER BY e.event_id DESC
        LIMIT 1
        """
    )
    suspend fun currentBatch(): OpeningBatchEntity?

    @Query("SELECT * FROM opening_batches WHERE event_id = :eventId LIMIT 1")
    suspend fun findBatch(eventId: Long): OpeningBatchEntity?

    @Query("SELECT * FROM opening_inventory_entries WHERE event_id = :eventId ORDER BY product_id")
    suspend fun inventoryEntries(eventId: Long): List<OpeningInventoryEntryEntity>

    @Query("SELECT * FROM opening_account_entries WHERE event_id = :eventId ORDER BY account_id")
    suspend fun accountEntries(eventId: Long): List<OpeningAccountEntryEntity>

    @Query(
        """
        SELECT
            e.event_id AS eventId,
            e.created_at AS createdAt,
            e.occurred_at AS occurredAt,
            e.corrects_event_id AS correctsEventId,
            CASE WHEN EXISTS (
                SELECT 1 FROM events child WHERE child.corrects_event_id = e.event_id
            ) THEN 1 ELSE 0 END AS superseded,
            e.notes AS notes
        FROM opening_batches ob
        INNER JOIN events e ON e.event_id = ob.event_id
        ORDER BY e.event_id DESC
        """
    )
    suspend fun history(): List<OpeningHistoryRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBatch(batch: OpeningBatchEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInventoryEntries(entries: List<OpeningInventoryEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccountEntries(entries: List<OpeningAccountEntryEntity>)
}
