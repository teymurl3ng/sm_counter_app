package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

/** One not-yet-sealed business date and how many events sit on it (H8). */
data class OpenDayEvents(
    @ColumnInfo(name = "business_date") val businessDate: String,
    @ColumnInfo(name = "event_count") val eventCount: Int,
)

@Dao
interface DailyCloseDao {

    /**
     * H8: event dates BEFORE :today that no active close action covers —
     * exactly the days today's close will absorb (mirrors isPostingClosed:
     * an active close with business_date >= D closes D).
     */
    @Query(
        """
        SELECT e.occurred_at AS business_date, COUNT(*) AS event_count
        FROM events e
        WHERE e.occurred_at < :today
          AND NOT EXISTS (
              SELECT 1 FROM daily_close_actions lock_action
              WHERE lock_action.business_date >= e.occurred_at
                AND lock_action.action = 'close'
                AND NOT EXISTS (
                    SELECT 1 FROM daily_close_actions child
                    WHERE child.supersedes_action_id = lock_action.action_id
                )
          )
        GROUP BY e.occurred_at
        ORDER BY e.occurred_at
        """
    )
    suspend fun absorbedOpenDays(today: String): List<OpenDayEvents>

    @Query(
        """
        SELECT CASE WHEN
          NOT EXISTS (
              SELECT 1 FROM daily_close_actions exact_action
              WHERE exact_action.business_date = :businessDate
                AND exact_action.action = 'reopen'
                AND NOT EXISTS (
                    SELECT 1 FROM daily_close_actions child
                    WHERE child.supersedes_action_id = exact_action.action_id
                )
          )
          AND EXISTS (
              SELECT 1 FROM daily_close_actions lock_action
              WHERE lock_action.business_date >= :businessDate
                AND lock_action.action = 'close'
                AND NOT EXISTS (
                    SELECT 1 FROM daily_close_actions child
                    WHERE child.supersedes_action_id = lock_action.action_id
                )
          )
        THEN 1 ELSE 0 END
        """
    )
    suspend fun isPostingClosed(businessDate: String): Boolean

    @Query(
        """
        SELECT * FROM daily_close_actions dca
        WHERE dca.business_date = :businessDate
          AND NOT EXISTS (
              SELECT 1 FROM daily_close_actions child
              WHERE child.supersedes_action_id = dca.action_id
          )
        ORDER BY dca.action_id DESC
        LIMIT 1
        """
    )
    suspend fun currentAction(businessDate: String): DailyCloseActionEntity?

    @Query("SELECT * FROM daily_close_actions WHERE business_date = :businessDate ORDER BY action_id")
    suspend fun history(businessDate: String): List<DailyCloseActionEntity>

    @Query("SELECT * FROM daily_close_actions ORDER BY business_date, action_id")
    suspend fun all(): List<DailyCloseActionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(action: DailyCloseActionEntity): Long
}
