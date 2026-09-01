package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AnalyticsBudgetActuals
import com.saaf.milkshop.domain.AnalyticsService
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.CostCoverageCode
import com.saaf.milkshop.domain.CostReportingScope
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.OpeningCostBasisService
import com.saaf.milkshop.domain.OPERATING_BUDGET_CATEGORIES
import com.saaf.milkshop.domain.ProductionLossAssessmentService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.YieldPolicyService
import com.saaf.milkshop.domain.YogurtProductionService

internal class HybridCostingPlanningReadService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun costingManagement(sessionHandle: String): CostingManagementDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val policies = database.costingPolicyDao().allVersions()
        val supersededPolicyIds = policies.mapNotNull { it.supersedesPolicyId }.toSet()
        val bases = database.inventoryCostBasisDao().all()
        val supersededBasisIds = bases.mapNotNull { it.supersedesBasisId }.toSet()
        val yieldPolicies = database.yieldPolicyDao().all()
        val supersededYieldPolicyIds = yieldPolicies.mapNotNull { it.supersedesYieldPolicyId }.toSet()
        val assessments = database.productionLossAssessmentDao().all()
        val supersededAssessmentIds = assessments.mapNotNull { it.supersedesAssessmentId }.toSet()
        val productionEvents = database.inventoryCostProjectionDao().effectiveEvents()
            .filter { it.eventType == YogurtProductionService.EVENT_TYPE_PRODUCTION }
        val yieldService = YieldPolicyService(database, security)

        return CostingManagementDto(
            codes = CostingCodeCatalogDto(
                coverageCodes = listOf(
                    CostCoverageCode.COMPLETE,
                    CostCoverageCode.PARTIAL,
                    CostCoverageCode.MISSING_BASIS,
                    CostCoverageCode.UNSUPPORTED_FLOW,
                    CostCoverageCode.STALE_CONVERSION_POLICY,
                ),
                reportingBasisCodes = listOf(CostReportingScope.REPORTING_BASIS),
                valuationScopeCodes = listOf(CostReportingScope.VALUATION_SCOPE),
                componentCodes = CostingPolicyService.COMPONENT_CODES.sorted(),
                reviewReasonCodes = CostingPolicyService.REVIEW_REASONS.sorted(),
                openingBasisKindCodes = OpeningCostBasisService.BASIS_KINDS.sorted(),
                yieldStatusCodes = listOf("calibration_pending", "review_required", "within_expected_range"),
                lossClassificationCodes = ProductionLossAssessmentService.CLASSIFICATIONS.sorted(),
                lossReasonCodes = ProductionLossAssessmentService.REASON_CODES.sorted(),
            ),
            policies = policies.map { policy ->
                val components = database.costingPolicyDao().components(policy.policyId)
                CostingPolicyDto(
                    policyId = policy.policyId,
                    calendarMonth = policy.calendarMonth,
                    normalCapacityKg = policy.normalCapacityKg,
                    reviewReasonCode = policy.reviewReason,
                    notes = policy.notes,
                    approvedAt = policy.approvedAt,
                    approvedByUserId = policy.approvedByUserId,
                    supersedesPolicyId = policy.supersedesPolicyId,
                    isSuperseded = policy.policyId in supersededPolicyIds,
                    conversionCostPaisaPerKg = decimal(CostingPolicyService.ratePerKg(components)),
                    components = components.map {
                        CostingPolicyComponentDto(it.componentCode, it.sourceAmountPaisa, it.basisQuantityKg)
                    },
                )
            },
            openingCostBases = bases.map {
                OpeningCostBasisDto(
                    basisId = it.basisId,
                    openingEventId = it.openingEventId,
                    productId = it.productId,
                    quantity = it.quantity,
                    totalCostPaisa = it.totalCostPaisa,
                    basisKindCode = it.basisKind,
                    evidenceNotes = it.evidenceNotes,
                    createdAt = it.createdAt,
                    createdByUserId = it.createdByUserId,
                    supersedesBasisId = it.supersedesBasisId,
                    isSuperseded = it.basisId in supersededBasisIds,
                )
            },
            yieldPolicies = yieldPolicies.map {
                YieldPolicyDto(
                    yieldPolicyId = it.yieldPolicyId,
                    sampleStartDate = it.sampleStartDate,
                    sampleEndDate = it.sampleEndDate,
                    acceptedBatchCount = it.acceptedBatchCount,
                    medianYieldRatio = it.medianYieldRatio,
                    madYieldRatio = it.madYieldRatio,
                    lowerAlertLimit = it.lowerAlertLimit,
                    upperAlertLimit = it.upperAlertLimit,
                    effectiveFrom = it.effectiveFrom,
                    approvedAt = it.approvedAt,
                    approvedByUserId = it.approvedByUserId,
                    supersedesYieldPolicyId = it.supersedesYieldPolicyId,
                    isSuperseded = it.yieldPolicyId in supersededYieldPolicyIds,
                )
            },
            yieldAlerts = productionEvents.map { event ->
                val assessment = yieldService.assess(event.eventId)
                YieldAlertDto(
                    productionEventId = assessment.productionEventId,
                    yieldRatio = decimal(assessment.yieldRatio),
                    appliedYieldPolicyId = assessment.policyId,
                    statusCode = assessment.status,
                )
            },
            productionLossAssessments = assessments.map {
                ProductionLossAssessmentDto(
                    assessmentId = it.assessmentId,
                    productionEventId = it.productionEventId,
                    classificationCode = it.classification,
                    abnormalMilkLiters = it.abnormalMilkLiters,
                    reasonCode = it.reasonCode,
                    notes = it.notes,
                    assessedAt = it.assessedAt,
                    assessedByUserId = it.assessedByUserId,
                    supersedesAssessmentId = it.supersedesAssessmentId,
                    isSuperseded = it.assessmentId in supersededAssessmentIds,
                )
            },
        )
    }

    suspend fun planning(sessionHandle: String): PlanningDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val budgets = buildList {
            val categories = listOf(BUDGET_CATEGORY_MILK_PURCHASES) + OPERATING_BUDGET_CATEGORIES
            categories.forEach { category -> addAll(database.budgetPlanDao().findAllForCategory(category)) }
        }.sortedWith(compareByDescending<BudgetPlanEntity> { it.periodStart }.thenByDescending { it.budgetId })
        val collectionTargets = database.collectionsTargetDao().findAll()
        val periods = buildSet {
            budgets.forEach { add(it.periodStart to it.periodEndExclusive) }
            collectionTargets.forEach { add(it.weekStart to it.weekEndExclusive) }
        }
        val analytics = AnalyticsService(database)
        val actualsByPeriod = mutableMapOf<Pair<String, String>, AnalyticsBudgetActuals>()
        periods.forEach { period ->
            actualsByPeriod[period] = analytics.budgetActuals(period.first, period.second)
        }
        return PlanningDto(
            budgets = budgets.map {
                val actuals = actualsByPeriod.getValue(it.periodStart to it.periodEndExclusive)
                val actualAmountPaisa = money(
                    if (it.category == BUDGET_CATEGORY_MILK_PURCHASES) actuals.milkPurchaseAmount
                    else actuals.expenseByCategory[it.category] ?: 0.0,
                )
                val plannedAmountPaisa = it.plannedAmount?.let(::money)
                val salesTargetPaisa = if (it.category == BUDGET_CATEGORY_MILK_PURCHASES) {
                    val liters = it.plannedLiters
                    val retailRate = it.retailRateSnapshot
                    if (liters != null && retailRate != null) money(liters * retailRate) else null
                } else null
                val actualSalesPaisa = salesTargetPaisa?.let { money(actuals.salesValue) }
                BudgetPlanDto(
                    budgetId = it.budgetId,
                    categoryCode = it.category,
                    periodStart = it.periodStart,
                    periodEndExclusive = it.periodEndExclusive,
                    plannedLiters = it.plannedLiters?.let(::decimal),
                    plannedAmountPaisa = plannedAmountPaisa,
                    actualAmountPaisa = actualAmountPaisa,
                    variancePaisa = plannedAmountPaisa?.let { planned -> actualAmountPaisa - planned },
                    vendorRateSnapshotPaisaPerUnit = it.vendorRateSnapshot?.let(::money),
                    retailRateSnapshotPaisaPerUnit = it.retailRateSnapshot?.let(::money),
                    salesTargetPaisa = salesTargetPaisa,
                    actualSalesPaisa = actualSalesPaisa,
                    salesVariancePaisa = if (salesTargetPaisa != null && actualSalesPaisa != null) actualSalesPaisa - salesTargetPaisa else null,
                    createdAt = it.createdAt,
                    supersedesId = it.supersedesId,
                    archived = it.archived,
                )
            },
            collectionTargets = collectionTargets.map {
                val actualCollectedPaisa = money(actualsByPeriod.getValue(it.weekStart to it.weekEndExclusive).creditCollections)
                val targetAmountPaisa = money(it.targetAmount)
                CollectionsTargetDto(
                    targetId = it.targetId,
                    weekStart = it.weekStart,
                    weekEndExclusive = it.weekEndExclusive,
                    targetAmountPaisa = targetAmountPaisa,
                    actualCollectedPaisa = actualCollectedPaisa,
                    variancePaisa = actualCollectedPaisa - targetAmountPaisa,
                    createdAt = it.createdAt,
                    supersedesId = it.supersedesId,
                    archived = it.archived,
                )
            },
        )
    }
}
