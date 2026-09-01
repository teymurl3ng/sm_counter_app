package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.CostingManagementDto
import com.saaf.milkshop.application.readmodels.PlanningDto
import org.json.JSONObject

internal fun costingManagementJson(value: CostingManagementDto): JSONObject = JSONObject()
    .put("codes", JSONObject()
        .put("coverageCodes", jsonArray(value.codes.coverageCodes))
        .put("reportingBasisCodes", jsonArray(value.codes.reportingBasisCodes))
        .put("valuationScopeCodes", jsonArray(value.codes.valuationScopeCodes))
        .put("componentCodes", jsonArray(value.codes.componentCodes))
        .put("reviewReasonCodes", jsonArray(value.codes.reviewReasonCodes))
        .put("openingBasisKindCodes", jsonArray(value.codes.openingBasisKindCodes))
        .put("yieldStatusCodes", jsonArray(value.codes.yieldStatusCodes))
        .put("lossClassificationCodes", jsonArray(value.codes.lossClassificationCodes))
        .put("lossReasonCodes", jsonArray(value.codes.lossReasonCodes)))
    .put("policies", jsonArray(value.policies) { policy ->
        JSONObject().put("policyId", policy.policyId.toString()).put("calendarMonth", policy.calendarMonth)
            .put("normalCapacityKg", policy.normalCapacityKg).put("reviewReasonCode", policy.reviewReasonCode)
            .putNullable("notes", policy.notes).put("approvedAt", policy.approvedAt)
            .put("approvedByUserId", policy.approvedByUserId.toString())
            .putNullable("supersedesPolicyId", policy.supersedesPolicyId?.toString()).put("isSuperseded", policy.isSuperseded)
            .put("conversionCostPaisaPerKg", policy.conversionCostPaisaPerKg)
            .put("components", jsonArray(policy.components) { component ->
                JSONObject().put("componentCode", component.componentCode)
                    .put("sourceAmountPaisa", component.sourceAmountPaisa.toString())
                    .put("basisQuantityKg", component.basisQuantityKg)
            })
    })
    .put("openingCostBases", jsonArray(value.openingCostBases) { basis ->
        JSONObject().put("basisId", basis.basisId.toString()).put("openingEventId", basis.openingEventId.toString())
            .put("productId", basis.productId.toString()).put("quantity", basis.quantity)
            .put("totalCostPaisa", basis.totalCostPaisa.toString()).put("basisKindCode", basis.basisKindCode)
            .putNullable("evidenceNotes", basis.evidenceNotes).put("createdAt", basis.createdAt)
            .put("createdByUserId", basis.createdByUserId.toString())
            .putNullable("supersedesBasisId", basis.supersedesBasisId?.toString()).put("isSuperseded", basis.isSuperseded)
    })
    .put("yieldPolicies", jsonArray(value.yieldPolicies) { policy ->
        JSONObject().put("yieldPolicyId", policy.yieldPolicyId.toString()).put("sampleStartDate", policy.sampleStartDate)
            .put("sampleEndDate", policy.sampleEndDate).put("acceptedBatchCount", policy.acceptedBatchCount)
            .put("medianYieldRatio", policy.medianYieldRatio).put("madYieldRatio", policy.madYieldRatio)
            .put("lowerAlertLimit", policy.lowerAlertLimit).put("upperAlertLimit", policy.upperAlertLimit)
            .put("effectiveFrom", policy.effectiveFrom).put("approvedAt", policy.approvedAt)
            .put("approvedByUserId", policy.approvedByUserId.toString())
            .putNullable("supersedesYieldPolicyId", policy.supersedesYieldPolicyId?.toString()).put("isSuperseded", policy.isSuperseded)
    })
    .put("yieldAlerts", jsonArray(value.yieldAlerts) { alert ->
        JSONObject().put("productionEventId", alert.productionEventId.toString()).put("yieldRatio", alert.yieldRatio)
            .putNullable("appliedYieldPolicyId", alert.appliedYieldPolicyId?.toString()).put("statusCode", alert.statusCode)
    })
    .put("productionLossAssessments", jsonArray(value.productionLossAssessments) { assessment ->
        JSONObject().put("assessmentId", assessment.assessmentId.toString())
            .put("productionEventId", assessment.productionEventId.toString()).put("classificationCode", assessment.classificationCode)
            .put("abnormalMilkLiters", assessment.abnormalMilkLiters).put("reasonCode", assessment.reasonCode)
            .putNullable("notes", assessment.notes).put("assessedAt", assessment.assessedAt)
            .put("assessedByUserId", assessment.assessedByUserId.toString())
            .putNullable("supersedesAssessmentId", assessment.supersedesAssessmentId?.toString()).put("isSuperseded", assessment.isSuperseded)
    })

internal fun planningJson(value: PlanningDto): JSONObject = JSONObject()
    .put("budgets", jsonArray(value.budgets) { budget ->
        JSONObject().put("budgetId", budget.budgetId.toString()).put("categoryCode", budget.categoryCode)
            .put("periodStart", budget.periodStart).put("periodEndExclusive", budget.periodEndExclusive)
            .putNullable("plannedLiters", budget.plannedLiters)
            .putNullable("plannedAmountPaisa", budget.plannedAmountPaisa?.toString())
            .putNullable("vendorRateSnapshotPaisaPerUnit", budget.vendorRateSnapshotPaisaPerUnit?.toString())
            .putNullable("retailRateSnapshotPaisaPerUnit", budget.retailRateSnapshotPaisaPerUnit?.toString())
            .put("createdAt", budget.createdAt).putNullable("supersedesId", budget.supersedesId?.toString())
            .put("archived", budget.archived)
    })
    .put("collectionTargets", jsonArray(value.collectionTargets) { target ->
        JSONObject().put("targetId", target.targetId.toString()).put("weekStart", target.weekStart)
            .put("weekEndExclusive", target.weekEndExclusive).put("targetAmountPaisa", target.targetAmountPaisa.toString())
            .put("createdAt", target.createdAt).putNullable("supersedesId", target.supersedesId?.toString())
            .put("archived", target.archived)
    })
