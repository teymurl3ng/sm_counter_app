package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "product_profiles",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
)
data class ProductProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "default_sale_rate")
    val defaultSaleRate: Double? = null,
    @ColumnInfo(name = "inventory_tracking", defaultValue = "1")
    val inventoryTracking: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val archived: Boolean = false,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "customer_profiles",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
)
data class CustomerProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    val phone: String? = null,
    @ColumnInfo(name = "area_route")
    val areaRoute: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "credit_limit")
    val creditLimit: Double? = null,
    @ColumnInfo(name = "daily_quota")
    val dailyQuota: Double? = null,
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "vendor_profiles",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
)
data class VendorProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    val phone: String? = null,
    val location: String? = null,
    val notes: String? = null,
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "employee_profiles",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
)
data class EmployeeProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    val role: String? = null,
    val phone: String? = null,
    @ColumnInfo(name = "reference_salary")
    val referenceSalary: Double? = null,
    @ColumnInfo(name = "salary_period")
    val salaryPeriod: String? = null,
    @ColumnInfo(name = "join_date")
    val joinDate: String? = null,
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["employee_account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AttendanceRecordEntity::class,
            parentColumns = ["attendance_id"],
            childColumns = ["supersedes_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["employee_account_id", "business_date", "archived"]),
        Index(value = ["supersedes_id"]),
    ],
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attendance_id")
    val attendanceId: Long = 0,
    @ColumnInfo(name = "employee_account_id")
    val employeeAccountId: Long,
    @ColumnInfo(name = "business_date")
    val businessDate: String,
    val status: String,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "supersedes_id")
    val supersedesId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val archived: Boolean = false,
)

@Entity(
    tableName = "deliveries",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["customer_account_id"],
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
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["sale_event_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["customer_account_id", "business_date"]),
        Index(value = ["product_id"]),
        Index(value = ["sale_event_id"], unique = true),
        Index(value = ["archived"]),
    ],
)
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "delivery_id")
    val deliveryId: Long = 0,
    @ColumnInfo(name = "customer_account_id")
    val customerAccountId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val quantity: Double,
    val route: String? = null,
    @ColumnInfo(name = "business_date")
    val businessDate: String,
    @ColumnInfo(name = "sale_event_id")
    val saleEventId: Long? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(defaultValue = "0")
    val archived: Boolean = false,
)

@Entity(
    tableName = "delivery_status_history",
    foreignKeys = [
        ForeignKey(
            entity = DeliveryEntity::class,
            parentColumns = ["delivery_id"],
            childColumns = ["delivery_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
    indices = [Index(value = ["delivery_id", "status_id"])],
)
data class DeliveryStatusEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "status_id")
    val statusId: Long = 0,
    @ColumnInfo(name = "delivery_id")
    val deliveryId: Long,
    val status: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: String,
    val notes: String? = null,
)

@Entity(
    tableName = "spoilage_details",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
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
data class SpoilageDetailEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val quantity: Double,
    val reason: String,
)

data class SpoilageHistoryRow(
    val eventId: Long,
    val occurredAt: String,
    val createdAt: String,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val reason: String,
    val correctsEventId: Long?,
    val superseded: Boolean,
    val notes: String?,
)

@Entity(
    tableName = "physical_cash_counts",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
    indices = [Index(value = ["account_id", "business_date", "counted_at"])],
)
data class PhysicalCashCountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "count_id")
    val countId: Long = 0,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "business_date")
    val businessDate: String,
    @ColumnInfo(name = "counted_amount")
    val countedAmount: Double,
    @ColumnInfo(name = "counted_at")
    val countedAt: String,
    val notes: String? = null,
)

@Entity(
    tableName = "expense_category_profiles",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
    indices = [Index(value = ["display_name"], unique = true)],
)
data class ExpenseCategoryProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
