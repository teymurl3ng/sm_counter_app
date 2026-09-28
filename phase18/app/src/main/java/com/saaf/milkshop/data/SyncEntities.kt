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
