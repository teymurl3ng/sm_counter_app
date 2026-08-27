package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "daily_close_actions",
    foreignKeys = [
        ForeignKey(
            entity = PhysicalCashCountEntity::class,
            parentColumns = ["count_id"],
            childColumns = ["physical_cash_count_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = DailyCloseActionEntity::class,
            parentColumns = ["action_id"],
            childColumns = ["supersedes_action_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["actor_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["business_date"]),
        Index(value = ["physical_cash_count_id"], unique = true),
        Index(value = ["supersedes_action_id"], unique = true),
        Index(value = ["actor_user_id"]),
    ],
)
data class DailyCloseActionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "action_id")
    val actionId: Long = 0,
    @ColumnInfo(name = "business_date")
    val businessDate: String,
    val action: String,
    @ColumnInfo(name = "physical_cash_count_id")
    val physicalCashCountId: Long? = null,
    @ColumnInfo(name = "actor_name")
    val actorName: String,
    @ColumnInfo(name = "actor_role")
    val actorRole: String,
    @ColumnInfo(name = "actor_user_id")
    val actorUserId: Long? = null,
    val reason: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "supersedes_action_id")
    val supersedesActionId: Long? = null,
)
