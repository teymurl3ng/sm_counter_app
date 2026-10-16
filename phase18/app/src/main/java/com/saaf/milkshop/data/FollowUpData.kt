package com.saaf.milkshop.data

import androidx.room3.*

/** Derived server metadata only. Every action still needs a fresh, online gate. */
@Entity(tableName="followup_cache")
data class FollowUpCacheEntity(
    @PrimaryKey @ColumnInfo(name="binding_key") val bindingKey: String,
    @ColumnInfo(name="snapshot_json") val snapshotJson: String,
    val stale: Boolean=true,
)

/** Presence means recovery-only on the next invocation. No side-effect grant is persisted. */
@Entity(tableName="followup_commands",indices=[Index("binding_key")])
data class FollowUpPendingEntity(
    @PrimaryKey @ColumnInfo(name="command_uid") val commandUid: String,
    @ColumnInfo(name="binding_key") val bindingKey: String,
    @ColumnInfo(name="operation_json") val operationJson: String,
    @ColumnInfo(name="created_at") val createdAt: String,
    val restored: Boolean=false,
    @ColumnInfo(name="last_error") val lastError: String?=null,
)

@Dao
interface FollowUpDataDao {
    @Query("SELECT * FROM followup_cache WHERE binding_key=:key") suspend fun cache(key: String): FollowUpCacheEntity?
    @Upsert suspend fun saveCache(row: FollowUpCacheEntity)
    @Query("UPDATE followup_cache SET stale=1 WHERE binding_key=:key") suspend fun invalidate(key: String)
    @Query("DELETE FROM followup_cache WHERE binding_key=:key") suspend fun dropCache(key: String)
    @Query("DELETE FROM followup_cache") suspend fun clearCache()
    @Query("SELECT * FROM followup_commands WHERE command_uid=:uid") suspend fun command(uid: String): FollowUpPendingEntity?
    @Query("SELECT * FROM followup_commands ORDER BY created_at,command_uid") suspend fun commands(): List<FollowUpPendingEntity>
    @Insert suspend fun insertCommand(row: FollowUpPendingEntity)
    @Query("UPDATE followup_commands SET last_error=:code WHERE command_uid=:uid") suspend fun error(uid: String,code: String)
    @Query("DELETE FROM followup_commands WHERE command_uid=:uid") suspend fun complete(uid: String)
    @Query("DELETE FROM followup_commands") suspend fun clearCommands()
}

internal object FollowUpDataMigration {
    val statements=listOf(
        "CREATE TABLE IF NOT EXISTS followup_cache (binding_key TEXT NOT NULL PRIMARY KEY,snapshot_json TEXT NOT NULL,stale INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS followup_commands (command_uid TEXT NOT NULL PRIMARY KEY,binding_key TEXT NOT NULL,operation_json TEXT NOT NULL,created_at TEXT NOT NULL,restored INTEGER NOT NULL,last_error TEXT)",
        "CREATE INDEX IF NOT EXISTS index_followup_commands_binding_key ON followup_commands(binding_key)",
    )
}
