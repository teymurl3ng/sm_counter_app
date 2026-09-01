package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.BudgetManagementService
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.BudgetPlanResult
import com.saaf.milkshop.domain.CollectionsTargetResult
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.CostingPolicySnapshot
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.OpeningCostBasisService
import com.saaf.milkshop.domain.ProductionLossAssessmentInput
import com.saaf.milkshop.domain.ProductionLossAssessmentService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.domain.YieldPolicyService
import java.time.Instant
import java.time.LocalDate

internal class CostingPlanningBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun upsertBudget(sessionHandle: String, input: BudgetPlanInput): BudgetPlanResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val result = BudgetManagementService(database).createOrRevise(input)
        audit(ACTION_UPSERT_BUDGET, "budget_id=${result.budgetId};category=${input.category};period_start=${input.periodStart}", actor.userId, actor.displayName, actor.role)
        result
    }

    suspend fun copyPreviousBudget(sessionHandle: String, category: String, start: LocalDate, end: LocalDate): BudgetPlanResult =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
            if (database.budgetPlanDao().findLatestBefore(category, start.toString()) == null) {
                throw BridgeCommandNotFound("No earlier budget exists for this category")
            }
            val result = BudgetManagementService(database).copyPreviousPeriod(category, start, end)
            audit(ACTION_COPY_BUDGET, "budget_id=${result.budgetId};category=$category;period_start=$start", actor.userId, actor.displayName, actor.role)
            result
        }

    suspend fun upsertCollectionsTarget(sessionHandle: String, weekStart: LocalDate, amount: Double): CollectionsTargetResult =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
            val result = BudgetManagementService(database).createOrReviseCollectionsTarget(weekStart, amount)
            audit(ACTION_UPSERT_COLLECTIONS_TARGET, "target_id=${result.targetId};week_start=$weekStart", actor.userId, actor.displayName, actor.role)
            result
        }

    suspend fun approveCostingPolicy(sessionHandle: String, input: CostingPolicyInput): CostingPolicySnapshot =
        database.withWriteTransaction {
            val result = CostingPolicyService(database, security).approve(sessionHandle, input)
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
            audit(ACTION_APPROVE_COSTING_POLICY, "policy_id=${result.version.policyId};calendar_month=${input.calendarMonth}", actor.userId, actor.displayName, actor.role)
            result
        }

    suspend fun recordOpeningCostBasis(sessionHandle: String, input: OpeningCostBasisInput) = database.withWriteTransaction {
        val result = OpeningCostBasisService(database, security).record(sessionHandle, input)
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        audit(ACTION_RECORD_OPENING_COST_BASIS, "basis_id=${result.basisId};opening_event_id=${input.openingEventId};product_id=${input.productId}", actor.userId, actor.displayName, actor.role)
        result
    }

    suspend fun approveYieldPolicy(sessionHandle: String, input: YieldPolicyInput) = database.withWriteTransaction {
        val result = YieldPolicyService(database, security).approve(sessionHandle, input)
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        audit(ACTION_APPROVE_YIELD_POLICY, "yield_policy_id=${result.yieldPolicyId};accepted_batches=${input.acceptedProductionEventIds.size}", actor.userId, actor.displayName, actor.role)
        result
    }

    suspend fun recordProductionLossAssessment(sessionHandle: String, input: ProductionLossAssessmentInput) =
        database.withWriteTransaction {
            val result = ProductionLossAssessmentService(database, security).record(sessionHandle, input)
            val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
            audit(ACTION_RECORD_PRODUCTION_LOSS, "assessment_id=${result.assessmentId};production_event_id=${input.productionEventId}", actor.userId, actor.displayName, actor.role)
            result
        }

    private suspend fun audit(action: String, details: String, actorUserId: Long, actorName: String, actorRole: String) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.now().toString(),
                action = action,
                result = RESULT_SUCCESS,
                actorUserId = actorUserId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                details = details,
            ),
        )
    }

    companion object {
        const val ACTION_UPSERT_BUDGET = "bridge_upsert_budget"
        const val ACTION_COPY_BUDGET = "bridge_copy_previous_budget"
        const val ACTION_UPSERT_COLLECTIONS_TARGET = "bridge_upsert_collections_target"
        const val ACTION_APPROVE_COSTING_POLICY = "bridge_approve_costing_policy"
        const val ACTION_RECORD_OPENING_COST_BASIS = "bridge_record_opening_cost_basis"
        const val ACTION_APPROVE_YIELD_POLICY = "bridge_approve_yield_policy"
        const val ACTION_RECORD_PRODUCTION_LOSS = "bridge_record_production_loss_assessment"
        const val RESULT_SUCCESS = "success"
    }
}
