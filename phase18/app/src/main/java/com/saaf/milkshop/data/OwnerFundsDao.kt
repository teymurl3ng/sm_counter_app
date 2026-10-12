package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface OwnerFundsDao {
    @Query("SELECT * FROM owner_funds_details WHERE event_id = :eventId")
    suspend fun find(eventId: Long): OwnerFundsDetailEntity?
    @Query("SELECT * FROM owner_funds_details ORDER BY event_id")
    suspend fun all(): List<OwnerFundsDetailEntity>
    @Insert suspend fun insert(detail: OwnerFundsDetailEntity)
    @Query("DELETE FROM owner_funds_details") suspend fun deleteAll()
}
