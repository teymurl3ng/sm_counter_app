package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "store_profile")
data class StoreProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "store_profile_id")
    val storeProfileId: Int = 1,
    @ColumnInfo(name = "store_name")
    val storeName: String,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "PKR",
    val phone: String? = null,
    val address: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "opening_batches",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
)
data class OpeningBatchEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: Long,
)

@Entity(
    tableName = "opening_inventory_entries",
    primaryKeys = ["event_id", "product_id"],
    foreignKeys = [
        ForeignKey(
            entity = OpeningBatchEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["product_id"])],
)
data class OpeningInventoryEntryEntity(
    @ColumnInfo(name = "event_id")
    val eventId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    /** Complete restated opening quantity, not a correction delta. */
    val quantity: Double,
)

@Entity(
    tableName = "opening_account_entries",
    primaryKeys = ["event_id", "account_id"],
    foreignKeys = [
        ForeignKey(
            entity = OpeningBatchEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["account_id"])],
)
data class OpeningAccountEntryEntity(
    @ColumnInfo(name = "event_id")
    val eventId: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    /** Complete restated opening balance, not a correction delta. */
    val balance: Double,
)

data class OpeningHistoryRow(
    val eventId: Long,
    val createdAt: String,
    val occurredAt: String,
    val correctsEventId: Long?,
    val superseded: Boolean,
    val notes: String?,
)
