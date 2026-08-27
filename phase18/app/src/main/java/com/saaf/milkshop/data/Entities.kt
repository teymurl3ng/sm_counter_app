package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.DatabaseView
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["name"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "product_id")
    val productId: Long = 0,

    val name: String,

    val unit: String,
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "account_id")
    val accountId: Long = 0,

    val name: String,

    val type: String,

    val category: String,
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["corrects_event_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        )
    ],
    indices = [
        Index(value = ["event_type"]),
        Index(value = ["occurred_at"]),
        Index(value = ["corrects_event_id"]),
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "event_id")
    val eventId: Long = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "occurred_at")
    val occurredAt: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    val channel: String? = null,

    @ColumnInfo(name = "corrects_event_id")
    val correctsEventId: Long? = null,

    @ColumnInfo(name = "raw_liters")
    val rawLiters: Double? = null,

    @ColumnInfo(name = "fat_percentage")
    val fatPercentage: Double? = null,

    @ColumnInfo(name = "lr_reading")
    val lrReading: Double? = null,

    @ColumnInfo(name = "snf_percentage")
    val snfPercentage: Double? = null,

    @ColumnInfo(name = "ts_percentage")
    val tsPercentage: Double? = null,

    @ColumnInfo(name = "adjusted_liters")
    val adjustedLiters: Double? = null,

    @ColumnInfo(name = "amount_owed")
    val amountOwed: Double? = null,

    @ColumnInfo(name = "milk_liters_used")
    val milkLitersUsed: Double? = null,

    @ColumnInfo(name = "yogurt_kg_produced")
    val yogurtKgProduced: Double? = null,

    @ColumnInfo(name = "amount")
    val amount: Double? = null,

    @ColumnInfo(name = "milk_rate_used")
    val milkRateUsed: Double? = null,

    @ColumnInfo(name = "delivery_fee_used")
    val deliveryFeeUsed: Double? = null,

    @ColumnInfo(name = "ts_standard_used")
    val tsStandardUsed: Double? = null,

    @ColumnInfo(name = "sale_quantity")
    val saleQuantity: Double? = null,

    @ColumnInfo(name = "sale_value")
    val saleValue: Double? = null,

    @ColumnInfo(name = "unit_rate")
    val unitRate: Double? = null,

    @ColumnInfo(name = "surcharge_applied")
    val surchargeApplied: Double = 0.0,

    val notes: String? = null,
)

@Entity(
    tableName = "inventory_log",
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
        )
    ],
    indices = [
        Index(value = ["event_id"]),
        Index(value = ["product_id"]),
    ]
)
data class InventoryLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    val logId: Long = 0,

    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "product_id")
    val productId: Long,

    val quantity: Double,
)

@Entity(
    tableName = "financial_log",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
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
        )
    ],
    indices = [
        Index(value = ["event_id"]),
        Index(value = ["account_id"]),
    ]
)
data class FinancialLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    val logId: Long = 0,

    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    val amount: Double,
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "setting_key")
    val key: String,

    @ColumnInfo(name = "setting_value")
    val value: Double,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)


@Entity(
    tableName = "budget_plans",
    indices = [
        Index(value = ["category", "period_start", "archived"]),
        Index(value = ["supersedes_id"]),
    ]
)
data class BudgetPlanEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "budget_id")
    val budgetId: Long = 0,

    val category: String,

    @ColumnInfo(name = "period_start")
    val periodStart: String,

    @ColumnInfo(name = "period_end_exclusive")
    val periodEndExclusive: String,

    @ColumnInfo(name = "planned_liters")
    val plannedLiters: Double? = null,

    @ColumnInfo(name = "planned_amount")
    val plannedAmount: Double? = null,

    @ColumnInfo(name = "vendor_rate_snapshot")
    val vendorRateSnapshot: Double? = null,

    @ColumnInfo(name = "retail_rate_snapshot")
    val retailRateSnapshot: Double? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "supersedes_id")
    val supersedesId: Long? = null,

    val archived: Boolean = false,
)

@Entity(
    tableName = "collections_targets",
    indices = [
        Index(value = ["week_start", "archived"]),
        Index(value = ["supersedes_id"]),
    ]
)
data class CollectionsTargetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "target_id")
    val targetId: Long = 0,

    @ColumnInfo(name = "week_start")
    val weekStart: String,

    @ColumnInfo(name = "week_end_exclusive")
    val weekEndExclusive: String,

    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "supersedes_id")
    val supersedesId: Long? = null,

    val archived: Boolean = false,
)

@DatabaseView(
    viewName = "current_stock",
    value = """
        SELECT
            p.product_id,
            p.name,
            p.unit,
            COALESCE(SUM(il.quantity), 0) AS quantity_on_hand
        FROM products p
        LEFT JOIN inventory_log il ON il.product_id = p.product_id
        GROUP BY p.product_id, p.name, p.unit
    """)
data class CurrentStockView(
    @ColumnInfo(name = "product_id") val productId: Long,
    val name: String,
    val unit: String,
    @ColumnInfo(name = "quantity_on_hand") val quantityOnHand: Double,
)

@DatabaseView(
    viewName = "account_balances",
    value = """
        SELECT
            a.account_id,
            a.name,
            a.type,
            a.category,
            ROUND(COALESCE(SUM(fl.amount), 0), 2) AS balance
        FROM accounts a
        LEFT JOIN financial_log fl ON fl.account_id = a.account_id
        GROUP BY a.account_id, a.name, a.type, a.category
    """)
data class AccountBalanceView(
    @ColumnInfo(name = "account_id") val accountId: Long,
    val name: String,
    val type: String,
    val category: String,
    val balance: Double,
)

@DatabaseView(
    viewName = "purchase_calculation_audit",
    value = """
        SELECT
            e.event_id,
            e.raw_liters,
            e.fat_percentage,
            e.lr_reading,
            e.milk_rate_used,
            e.delivery_fee_used,
            e.ts_standard_used,
            (0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72 AS expected_snf_percentage,
            e.fat_percentage + ((0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72) AS expected_ts_percentage,
            e.raw_liters * (
                (
                    e.fat_percentage +
                    ((0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72)
                ) / e.ts_standard_used
            ) AS expected_adjusted_liters,
            (
                e.raw_liters * (
                    (
                        e.fat_percentage +
                        ((0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72)
                    ) / e.ts_standard_used
                ) * e.milk_rate_used
            ) + (e.raw_liters * e.delivery_fee_used) AS expected_amount_owed
        FROM events e
        WHERE e.event_type = 'PURCHASE_RECEIPT'
    """)
data class PurchaseCalculationAuditView(
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "raw_liters") val rawLiters: Double?,
    @ColumnInfo(name = "fat_percentage") val fatPercentage: Double?,
    @ColumnInfo(name = "lr_reading") val lrReading: Double?,
    @ColumnInfo(name = "milk_rate_used") val milkRateUsed: Double?,
    @ColumnInfo(name = "delivery_fee_used") val deliveryFeeUsed: Double?,
    @ColumnInfo(name = "ts_standard_used") val tsStandardUsed: Double?,
    @ColumnInfo(name = "expected_snf_percentage") val expectedSnfPercentage: Double?,
    @ColumnInfo(name = "expected_ts_percentage") val expectedTsPercentage: Double?,
    @ColumnInfo(name = "expected_adjusted_liters") val expectedAdjustedLiters: Double?,
    @ColumnInfo(name = "expected_amount_owed") val expectedAmountOwed: Double?,
)

@DatabaseView(
    viewName = "sale_calculation_audit",
    value = """
        SELECT
            e.event_id,
            e.sale_quantity,
            e.sale_value,
            e.unit_rate,
            e.surcharge_applied,
            ROUND(e.sale_quantity * e.unit_rate, 2) AS expected_value_from_quantity,
            ROUND(e.sale_value / e.unit_rate, 4) AS expected_quantity_from_value,
            ROUND(e.sale_value + e.surcharge_applied, 2) AS expected_total
        FROM events e
        WHERE e.event_type = 'SALE'
    """)
data class SaleCalculationAuditView(
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "sale_quantity") val saleQuantity: Double?,
    @ColumnInfo(name = "sale_value") val saleValue: Double?,
    @ColumnInfo(name = "unit_rate") val unitRate: Double?,
    @ColumnInfo(name = "surcharge_applied") val surchargeApplied: Double,
    @ColumnInfo(name = "expected_value_from_quantity") val expectedValueFromQuantity: Double?,
    @ColumnInfo(name = "expected_quantity_from_value") val expectedQuantityFromValue: Double?,
    @ColumnInfo(name = "expected_total") val expectedTotal: Double?,
)
