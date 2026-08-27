package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface DailyCloseDao {
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
