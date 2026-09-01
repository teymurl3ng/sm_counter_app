package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.ConversionCostComponentInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.ProductionLossAssessmentInput
import com.saaf.milkshop.domain.YieldPolicyInput
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.json.JSONArray
import org.json.JSONObject

internal class CostingPlanningBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: CostingPlanningBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.COSTING_MANAGEMENT -> costingManagement(request)
        HybridBridgeActions.PLANNING -> planning(request)
        HybridBridgeActions.UPSERT_BUDGET -> upsertBudget(request)
        HybridBridgeActions.COPY_PREVIOUS_BUDGET -> copyPreviousBudget(request)
        HybridBridgeActions.UPSERT_COLLECTIONS_TARGET -> upsertCollectionsTarget(request)
        HybridBridgeActions.APPROVE_COSTING_POLICY -> approveCostingPolicy(request)
        HybridBridgeActions.RECORD_OPENING_COST_BASIS -> recordOpeningCostBasis(request)
        HybridBridgeActions.APPROVE_YIELD_POLICY -> approveYieldPolicy(request)
        HybridBridgeActions.RECORD_PRODUCTION_LOSS_ASSESSMENT -> recordProductionLossAssessment(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun costingManagement(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        return costingManagementJson(reads.costingManagement(request.requireSession()))
    }

    private suspend fun planning(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        return planningJson(reads.planning(request.requireSession()))
    }

    private suspend fun upsertBudget(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("categoryCode", "periodStart", "periodEndExclusive", "plannedLiters", "plannedAmountPaisa"))
        val plannedAmountPaisa = request.payload.nullablePaisa("plannedAmountPaisa")
        val result = commands.upsertBudget(
            request.requireSession(),
            BudgetPlanInput(
                category = request.payload.requiredString("categoryCode"),
                periodStart = LocalDate.parse(request.payload.requiredDate("periodStart")),
                periodEndExclusive = LocalDate.parse(request.payload.requiredDate("periodEndExclusive")),
                plannedLiters = request.payload.nullableDecimal("plannedLiters")?.toDouble(),
                plannedAmount = plannedAmountPaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
            ),
        )
        return budgetMutationJson(result)
    }

    private suspend fun copyPreviousBudget(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("categoryCode", "periodStart", "periodEndExclusive"))
        return budgetMutationJson(
            commands.copyPreviousBudget(
                request.requireSession(),
                request.payload.requiredString("categoryCode"),
                LocalDate.parse(request.payload.requiredDate("periodStart")),
                LocalDate.parse(request.payload.requiredDate("periodEndExclusive")),
            ),
        )
    }

    private suspend fun upsertCollectionsTarget(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("weekStart", "targetAmountPaisa"))
        val amountPaisa = request.payload.requiredPaisa("targetAmountPaisa")
        if (amountPaisa < 0L) throw BridgeProtocolFailure("validation_failed", "Collection target cannot be negative", "targetAmountPaisa")
        val result = commands.upsertCollectionsTarget(
            request.requireSession(),
            LocalDate.parse(request.payload.requiredDate("weekStart")),
            BigDecimal(amountPaisa).movePointLeft(2).toDouble(),
        )
        return JSONObject().put("targetId", result.targetId.toString()).put("targetAmountPaisa", money(result.targetAmount).toString())
    }

    private suspend fun approveCostingPolicy(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("calendarMonth", "normalCapacityKg", "reviewReasonCode", "components", "supersedesPolicyId"),
            allowed = setOf("calendarMonth", "normalCapacityKg", "reviewReasonCode", "components", "notes", "supersedesPolicyId"),
        )
        val month = request.payload.requiredString("calendarMonth")
        runCatching { YearMonth.parse(month) }.getOrElse {
            throw BridgeProtocolFailure("validation_failed", "calendarMonth must be a valid month", "calendarMonth")
        }
        val components = request.payload.requiredObjectArray("components").map { item ->
            strictPayload(item, setOf("componentCode", "sourceAmountPaisa", "basisQuantityKg"))
            ConversionCostComponentInput(
                code = item.requiredString("componentCode"),
                sourceAmountPaisa = item.requiredPaisa("sourceAmountPaisa"),
                basisQuantityKg = item.requiredDecimal("basisQuantityKg"),
            )
        }
        val result = commands.approveCostingPolicy(
            request.requireSession(),
            CostingPolicyInput(
                calendarMonth = month,
                normalCapacityKg = request.payload.requiredDecimal("normalCapacityKg"),
                reviewReason = request.payload.requiredString("reviewReasonCode"),
                components = components,
                notes = request.payload.optionalString("notes"),
                supersedesPolicyId = request.payload.nullableIdValue("supersedesPolicyId"),
            ),
        )
        return JSONObject().put("entityId", result.version.policyId.toString())
    }

    private suspend fun recordOpeningCostBasis(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("openingEventId", "productId", "quantity", "totalCostPaisa", "basisKindCode", "supersedesBasisId"),
            allowed = setOf("openingEventId", "productId", "quantity", "totalCostPaisa", "basisKindCode", "evidenceNotes", "supersedesBasisId"),
        )
        val result = commands.recordOpeningCostBasis(
            request.requireSession(),
            OpeningCostBasisInput(
                openingEventId = request.payload.requiredId("openingEventId"),
                productId = request.payload.requiredId("productId"),
                quantity = request.payload.requiredDecimal("quantity"),
                totalCostPaisa = request.payload.requiredPaisa("totalCostPaisa"),
                basisKind = request.payload.requiredString("basisKindCode"),
                evidenceNotes = request.payload.optionalString("evidenceNotes"),
                supersedesBasisId = request.payload.nullableIdValue("supersedesBasisId"),
            ),
        )
        return JSONObject().put("entityId", result.basisId.toString())
    }

    private suspend fun approveYieldPolicy(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("acceptedProductionEventIds", "effectiveFrom", "supersedesYieldPolicyId"))
        val values = request.payload.requiredArray("acceptedProductionEventIds")
        val ids = (0 until values.length()).map { index ->
            val raw = values.get(index) as? String ?: throw BridgeProtocolFailure("invalid_request", "acceptedProductionEventIds entries must be IDs", "acceptedProductionEventIds")
            if (!Regex("[1-9][0-9]*").matches(raw)) throw BridgeProtocolFailure("validation_failed", "Accepted batch IDs must be positive", "acceptedProductionEventIds")
            raw.toLongOrNull() ?: throw BridgeProtocolFailure("validation_failed", "Accepted batch ID is outside the supported range", "acceptedProductionEventIds")
        }
        val result = commands.approveYieldPolicy(
            request.requireSession(),
            YieldPolicyInput(
                acceptedProductionEventIds = ids,
                effectiveFrom = request.payload.requiredDate("effectiveFrom"),
                supersedesYieldPolicyId = request.payload.nullableIdValue("supersedesYieldPolicyId"),
            ),
        )
        return JSONObject().put("entityId", result.yieldPolicyId.toString())
    }

    private suspend fun recordProductionLossAssessment(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("productionEventId", "classificationCode", "abnormalMilkLiters", "reasonCode", "supersedesAssessmentId"),
            allowed = setOf("productionEventId", "classificationCode", "abnormalMilkLiters", "reasonCode", "notes", "supersedesAssessmentId"),
        )
        val result = commands.recordProductionLossAssessment(
            request.requireSession(),
            ProductionLossAssessmentInput(
                productionEventId = request.payload.requiredId("productionEventId"),
                classification = request.payload.requiredString("classificationCode"),
                abnormalMilkLiters = request.payload.requiredDecimal("abnormalMilkLiters"),
                reasonCode = request.payload.requiredString("reasonCode"),
                notes = request.payload.optionalString("notes"),
                supersedesAssessmentId = request.payload.nullableIdValue("supersedesAssessmentId"),
            ),
        )
        return JSONObject().put("entityId", result.assessmentId.toString())
    }
}

private fun budgetMutationJson(result: com.saaf.milkshop.domain.BudgetPlanResult): JSONObject = JSONObject()
    .put("budgetId", result.budgetId.toString())
    .put("plannedAmountPaisa", money(result.plannedAmount).toString())
    .putNullable("salesTargetPaisa", result.salesTarget?.let { money(it).toString() })

private fun JSONObject.requiredArray(name: String): JSONArray {
    if (!has(name) || isNull(name) || get(name) !is JSONArray) {
        throw BridgeProtocolFailure("invalid_request", "$name must be a list", name)
    }
    return getJSONArray(name)
}

private fun JSONObject.requiredObjectArray(name: String): List<JSONObject> = requiredArray(name).let { values ->
    (0 until values.length()).map { index ->
        values.get(index) as? JSONObject
            ?: throw BridgeProtocolFailure("invalid_request", "$name entries must be objects", name)
    }
}

private fun JSONObject.nullableIdValue(name: String): Long? {
    val raw = nullableString(name) ?: return null
    if (!Regex("[1-9][0-9]*").matches(raw)) throw BridgeProtocolFailure("validation_failed", "$name must be a positive record ID", name)
    return raw.toLongOrNull() ?: throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name)
}
