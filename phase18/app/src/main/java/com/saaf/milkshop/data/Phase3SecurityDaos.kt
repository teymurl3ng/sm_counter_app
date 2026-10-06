package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface AppUserDao {
    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun count(): Long

    @Query("SELECT COUNT(*) FROM app_users WHERE role = 'owner' AND status = 'active'")
    suspend fun activeOwnerCount(): Long

    @Query("SELECT * FROM app_users WHERE user_id = :userId")
    suspend fun findById(userId: Long): AppUserEntity?

    @Query("SELECT * FROM app_users WHERE login_name = :loginName COLLATE NOCASE LIMIT 1")
    suspend fun findByLoginName(loginName: String): AppUserEntity?

    @Query("SELECT * FROM app_users ORDER BY display_name COLLATE NOCASE, user_id")
    suspend fun all(): List<AppUserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: AppUserEntity): Long

    @Update
    suspend fun update(user: AppUserEntity)
}

@Dao
interface UserCredentialDao {
    @Query("SELECT * FROM user_credentials WHERE user_id = :userId")
    suspend fun find(userId: Long): UserCredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(credential: UserCredentialEntity)

    @Query("UPDATE user_credentials SET failed_attempt_count = :count, last_failed_at_epoch_ms = :failedAt, locked_until_epoch_ms = :lockedUntil WHERE user_id = :userId")
    suspend fun recordFailure(userId: Long, count: Int, failedAt: Long, lockedUntil: Long?)

    @Query("UPDATE user_credentials SET failed_attempt_count = 0, last_failed_at_epoch_ms = NULL, locked_until_epoch_ms = NULL WHERE user_id = :userId")
    suspend fun clearFailures(userId: Long)
}

@Dao
interface SavedRecoveryCodeDao {
    @Query("SELECT * FROM saved_recovery_codes WHERE user_id = :userId AND used_at IS NULL ORDER BY recovery_code_id DESC LIMIT 1")
    suspend fun current(userId: Long): SavedRecoveryCodeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(code: SavedRecoveryCodeEntity): Long

    @Update
    suspend fun update(code: SavedRecoveryCodeEntity)
}

@Dao
interface SecurityAuditDao {

    /** H8: dates absorbed (sealed) by the given day's close, in seal order. */
    @Query(
        """
        SELECT reason_code FROM security_audit_entries
        WHERE action = 'daily_close_seal_absorbed'
          AND details LIKE '%' || 'sealed_by=' || :sealedBy || '%'
        ORDER BY reason_code
        """
    )
    suspend fun absorbedDaysSealedBy(sealedBy: String): List<String>

    /** H8: refused pushes since the given ISO instant (closed-day refusals surface as halts). */
    @Query(
        """
        SELECT COUNT(*) FROM security_audit_entries
        WHERE action = 'sync_push_halted'
          AND occurred_at >= :sinceIso
        """
    )
    suspend fun refusalsSince(sinceIso: String): Int
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: SecurityAuditEntryEntity): Long

    @Query("SELECT * FROM security_audit_entries ORDER BY audit_id")
    suspend fun all(): List<SecurityAuditEntryEntity>
}

@Dao
interface EventActorLinkDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(link: EventActorLinkEntity)

    @Query("SELECT * FROM event_actor_links WHERE event_id = :eventId")
    suspend fun find(eventId: Long): EventActorLinkEntity?
}
