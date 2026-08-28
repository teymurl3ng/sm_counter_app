package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

data class CostingSourceWatermarks(
    val eventCount: Long,
    val maxEventId: Long,
    val maxPolicyId: Long,
    val maxBasisId: Long,
    val maxYieldPolicyId: Long,
    val maxAssessmentId: Long,
)

@Dao
interface CostingPolicyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: CostingPolicyVersionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertComponents(components: List<CostingPolicyComponentEntity>)

    @Query(
        """
        SELECT p.* FROM costing_policy_versions p
        WHERE p.calendar_month = :calendarMonth
          AND NOT EXISTS (
              SELECT 1 FROM costing_policy_versions child
              WHERE child.supersedes_policy_id = p.policy_id
          )
        ORDER BY p.policy_id DESC
        LIMIT 1
        """
    )
    suspend fun activeForMonth(calendarMonth: String): CostingPolicyVersionEntity?

    @Query(
        """
        SELECT p.* FROM costing_policy_versions p
        WHERE p.calendar_month < :calendarMonth
          AND NOT EXISTS (
              SELECT 1 FROM costing_policy_versions child
              WHERE child.supersedes_policy_id = p.policy_id
          )
        ORDER BY p.calendar_month DESC, p.policy_id DESC
        LIMIT 1
        """
    )
    suspend fun latestBeforeMonth(calendarMonth: String): CostingPolicyVersionEntity?

    @Query("SELECT * FROM costing_policy_versions WHERE policy_id = :policyId LIMIT 1")
    suspend fun findVersion(policyId: Long): CostingPolicyVersionEntity?

    @Query("SELECT * FROM costing_policy_components WHERE policy_id = :policyId ORDER BY component_code")
    suspend fun components(policyId: Long): List<CostingPolicyComponentEntity>

    @Query("SELECT * FROM costing_policy_versions ORDER BY calendar_month DESC, policy_id DESC")
    suspend fun allVersions(): List<CostingPolicyVersionEntity>
}

@Dao
interface InventoryCostBasisDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: InventoryCostBasisEntity): Long

    @Query(
        """
        SELECT b.* FROM inventory_cost_basis_entries b
        WHERE b.opening_event_id = :openingEventId
          AND b.product_id = :productId
          AND NOT EXISTS (
              SELECT 1 FROM inventory_cost_basis_entries child
              WHERE child.supersedes_basis_id = b.basis_id
          )
        ORDER BY b.basis_id DESC
        LIMIT 1
        """
    )
    suspend fun active(openingEventId: Long, productId: Long): InventoryCostBasisEntity?

    @Query("SELECT * FROM inventory_cost_basis_entries WHERE basis_id = :basisId LIMIT 1")
    suspend fun find(basisId: Long): InventoryCostBasisEntity?

    @Query("SELECT * FROM inventory_cost_basis_entries ORDER BY basis_id DESC")
    suspend fun all(): List<InventoryCostBasisEntity>
}

@Dao
interface YieldPolicyDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(version: YieldPolicyVersionEntity): Long

    @Query(
        """
        SELECT y.* FROM yield_policy_versions y
        WHERE y.effective_from <= :businessDate
          AND NOT EXISTS (
              SELECT 1 FROM yield_policy_versions child
              WHERE child.supersedes_yield_policy_id = y.yield_policy_id
          )
        ORDER BY y.effective_from DESC, y.yield_policy_id DESC
        LIMIT 1
        """
    )
    suspend fun activeForDate(businessDate: String): YieldPolicyVersionEntity?

    @Query("SELECT * FROM yield_policy_versions WHERE yield_policy_id = :id LIMIT 1")
    suspend fun find(id: Long): YieldPolicyVersionEntity?

    @Query("SELECT * FROM yield_policy_versions ORDER BY effective_from DESC, yield_policy_id DESC")
    suspend fun all(): List<YieldPolicyVersionEntity>
}

@Dao
interface ProductionLossAssessmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(assessment: ProductionLossAssessmentEntity): Long

    @Query(
        """
        SELECT a.* FROM production_loss_assessments a
        WHERE a.production_event_id = :eventId
          AND NOT EXISTS (
              SELECT 1 FROM production_loss_assessments child
              WHERE child.supersedes_assessment_id = a.assessment_id
          )
        ORDER BY a.assessment_id DESC
        LIMIT 1
        """
    )
    suspend fun activeForEvent(eventId: Long): ProductionLossAssessmentEntity?

    @Query("SELECT * FROM production_loss_assessments WHERE assessment_id = :id LIMIT 1")
    suspend fun find(id: Long): ProductionLossAssessmentEntity?

    @Query("SELECT * FROM production_loss_assessments ORDER BY assessed_at DESC, assessment_id DESC")
    suspend fun all(): List<ProductionLossAssessmentEntity>
}

@Dao
interface InventoryCostProjectionDao {
    @Query(
        """
        SELECT * FROM events e
        WHERE NOT EXISTS (
            SELECT 1 FROM events child WHERE child.corrects_event_id = e.event_id
        )
        ORDER BY e.occurred_at ASC, e.created_at ASC, e.event_id ASC
        """
    )
    suspend fun effectiveEvents(): List<EventEntity>

    @Query("DELETE FROM inventory_cost_movements")
    suspend fun clearMovements()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: InventoryCostMovementEntity): Long

    @Query("SELECT * FROM inventory_cost_movements ORDER BY sequence_number, movement_id")
    suspend fun allMovements(): List<InventoryCostMovementEntity>

    @Query(
        """
        SELECT * FROM inventory_cost_movements
        WHERE occurred_at >= :startInclusive AND occurred_at < :endExclusive
        ORDER BY sequence_number, movement_id
        """
    )
    suspend fun movementsForPeriod(startInclusive: String, endExclusive: String): List<InventoryCostMovementEntity>

    @Query("SELECT * FROM cost_projection_state WHERE state_id = 1 LIMIT 1")
    suspend fun state(): CostProjectionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: CostProjectionStateEntity)

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM events) AS eventCount,
            COALESCE((SELECT MAX(event_id) FROM events), 0) AS maxEventId,
            COALESCE((SELECT MAX(policy_id) FROM costing_policy_versions), 0) AS maxPolicyId,
            COALESCE((SELECT MAX(basis_id) FROM inventory_cost_basis_entries), 0) AS maxBasisId,
            COALESCE((SELECT MAX(yield_policy_id) FROM yield_policy_versions), 0) AS maxYieldPolicyId,
            COALESCE((SELECT MAX(assessment_id) FROM production_loss_assessments), 0) AS maxAssessmentId
        """
    )
    suspend fun sourceWatermarks(): CostingSourceWatermarks
}
