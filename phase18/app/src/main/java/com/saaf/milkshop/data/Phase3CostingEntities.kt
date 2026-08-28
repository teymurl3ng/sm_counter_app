package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "costing_policy_versions",
    foreignKeys = [
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["approved_by_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = CostingPolicyVersionEntity::class,
            parentColumns = ["policy_id"],
            childColumns = ["supersedes_policy_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["calendar_month"]),
        Index(value = ["approved_by_user_id"]),
        Index(value = ["supersedes_policy_id"], unique = true),
    ],
)
data class CostingPolicyVersionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "policy_id")
    val policyId: Long = 0,
    @ColumnInfo(name = "calendar_month")
    val calendarMonth: String,
    @ColumnInfo(name = "normal_capacity_kg")
    val normalCapacityKg: String,
    @ColumnInfo(name = "review_reason")
    val reviewReason: String,
    val notes: String? = null,
    @ColumnInfo(name = "approved_at")
    val approvedAt: String,
    @ColumnInfo(name = "approved_by_user_id")
    val approvedByUserId: Long,
    @ColumnInfo(name = "supersedes_policy_id")
    val supersedesPolicyId: Long? = null,
)

@Entity(
    tableName = "costing_policy_components",
    primaryKeys = ["policy_id", "component_code"],
    foreignKeys = [
        ForeignKey(
            entity = CostingPolicyVersionEntity::class,
            parentColumns = ["policy_id"],
            childColumns = ["policy_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
)
data class CostingPolicyComponentEntity(
    @ColumnInfo(name = "policy_id")
    val policyId: Long,
    @ColumnInfo(name = "component_code")
    val componentCode: String,
    @ColumnInfo(name = "source_amount_paisa")
    val sourceAmountPaisa: Long,
    @ColumnInfo(name = "basis_quantity_kg")
    val basisQuantityKg: String,
)

@Entity(
    tableName = "inventory_cost_basis_entries",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["opening_event_id"],
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
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["created_by_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = InventoryCostBasisEntity::class,
            parentColumns = ["basis_id"],
            childColumns = ["supersedes_basis_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["opening_event_id", "product_id"]),
        Index(value = ["product_id"]),
        Index(value = ["created_by_user_id"]),
        Index(value = ["supersedes_basis_id"], unique = true),
    ],
)
data class InventoryCostBasisEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "basis_id")
    val basisId: Long = 0,
    @ColumnInfo(name = "opening_event_id")
    val openingEventId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val quantity: String,
    @ColumnInfo(name = "total_cost_paisa")
    val totalCostPaisa: Long,
    @ColumnInfo(name = "basis_kind")
    val basisKind: String,
    @ColumnInfo(name = "evidence_notes")
    val evidenceNotes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "created_by_user_id")
    val createdByUserId: Long,
    @ColumnInfo(name = "supersedes_basis_id")
    val supersedesBasisId: Long? = null,
)

@Entity(
    tableName = "yield_policy_versions",
    foreignKeys = [
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["approved_by_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = YieldPolicyVersionEntity::class,
            parentColumns = ["yield_policy_id"],
            childColumns = ["supersedes_yield_policy_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["effective_from"]),
        Index(value = ["approved_by_user_id"]),
        Index(value = ["supersedes_yield_policy_id"], unique = true),
    ],
)
data class YieldPolicyVersionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "yield_policy_id")
    val yieldPolicyId: Long = 0,
    @ColumnInfo(name = "sample_start_date")
    val sampleStartDate: String,
    @ColumnInfo(name = "sample_end_date")
    val sampleEndDate: String,
    @ColumnInfo(name = "accepted_batch_count")
    val acceptedBatchCount: Int,
    @ColumnInfo(name = "median_yield_ratio")
    val medianYieldRatio: String,
    @ColumnInfo(name = "mad_yield_ratio")
    val madYieldRatio: String,
    @ColumnInfo(name = "lower_alert_limit")
    val lowerAlertLimit: String,
    @ColumnInfo(name = "upper_alert_limit")
    val upperAlertLimit: String,
    @ColumnInfo(name = "effective_from")
    val effectiveFrom: String,
    @ColumnInfo(name = "approved_at")
    val approvedAt: String,
    @ColumnInfo(name = "approved_by_user_id")
    val approvedByUserId: Long,
    @ColumnInfo(name = "supersedes_yield_policy_id")
    val supersedesYieldPolicyId: Long? = null,
)

@Entity(
    tableName = "production_loss_assessments",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["production_event_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["assessed_by_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = ProductionLossAssessmentEntity::class,
            parentColumns = ["assessment_id"],
            childColumns = ["supersedes_assessment_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["production_event_id"]),
        Index(value = ["assessed_by_user_id"]),
        Index(value = ["supersedes_assessment_id"], unique = true),
    ],
)
data class ProductionLossAssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "assessment_id")
    val assessmentId: Long = 0,
    @ColumnInfo(name = "production_event_id")
    val productionEventId: Long,
    val classification: String,
    @ColumnInfo(name = "abnormal_milk_liters")
    val abnormalMilkLiters: String,
    @ColumnInfo(name = "reason_code")
    val reasonCode: String,
    val notes: String? = null,
    @ColumnInfo(name = "assessed_at")
    val assessedAt: String,
    @ColumnInfo(name = "assessed_by_user_id")
    val assessedByUserId: Long,
    @ColumnInfo(name = "supersedes_assessment_id")
    val supersedesAssessmentId: Long? = null,
)

@Entity(
    tableName = "inventory_cost_movements",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["source_event_id"],
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
    indices = [
        Index(value = ["source_event_id", "product_id", "movement_role"], unique = true),
        Index(value = ["product_id", "sequence_number"]),
        Index(value = ["occurred_at"]),
    ],
)
data class InventoryCostMovementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "movement_id")
    val movementId: Long = 0,
    @ColumnInfo(name = "source_event_id")
    val sourceEventId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "movement_role")
    val movementRole: String,
    @ColumnInfo(name = "movement_kind")
    val movementKind: String,
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Long,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "quantity_in")
    val quantityIn: String? = null,
    @ColumnInfo(name = "quantity_out")
    val quantityOut: String? = null,
    @ColumnInfo(name = "cost_in_paisa")
    val costInPaisa: Long? = null,
    @ColumnInfo(name = "cost_out_paisa")
    val costOutPaisa: Long? = null,
    @ColumnInfo(name = "running_quantity")
    val runningQuantity: String,
    @ColumnInfo(name = "running_cost_paisa")
    val runningCostPaisa: Long? = null,
    @ColumnInfo(name = "coverage_code")
    val coverageCode: String,
    @ColumnInfo(name = "applied_policy_id")
    val appliedPolicyId: Long? = null,
    @ColumnInfo(name = "applied_yield_policy_id")
    val appliedYieldPolicyId: Long? = null,
    @ColumnInfo(name = "applied_assessment_id")
    val appliedAssessmentId: Long? = null,
    @ColumnInfo(name = "applied_basis_id")
    val appliedBasisId: Long? = null,
)

@Entity(tableName = "cost_projection_state")
data class CostProjectionStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "state_id")
    val stateId: Int = 1,
    @ColumnInfo(name = "engine_version")
    val engineVersion: Int,
    @ColumnInfo(name = "source_event_count")
    val sourceEventCount: Long,
    @ColumnInfo(name = "source_max_event_id")
    val sourceMaxEventId: Long,
    @ColumnInfo(name = "source_max_policy_id")
    val sourceMaxPolicyId: Long,
    @ColumnInfo(name = "source_max_basis_id")
    val sourceMaxBasisId: Long,
    @ColumnInfo(name = "source_max_yield_policy_id")
    val sourceMaxYieldPolicyId: Long,
    @ColumnInfo(name = "source_max_assessment_id")
    val sourceMaxAssessmentId: Long,
    @ColumnInfo(name = "rebuilt_at")
    val rebuiltAt: String,
    @ColumnInfo(name = "coverage_code")
    val coverageCode: String,
)
