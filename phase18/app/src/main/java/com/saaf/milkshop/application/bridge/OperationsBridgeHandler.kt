package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.decimal
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.domain.VendorPurchaseInput
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

internal class OperationsBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: OperationsBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.DASHBOARD -> dashboard(request)
        HybridBridgeActions.RECORD_PURCHASE -> recordPurchase(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun dashboard(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("startDateInclusive", "endDateExclusive"))
        val start = request.payload.requiredDate("startDateInclusive")
        val end = request.payload.requiredDate("endDateExclusive")
        if (!LocalDate.parse(end).isAfter(LocalDate.parse(start))) {
            throw BridgeProtocolFailure(
                "invalid_request",
                "endDateExclusive must be after startDateInclusive",
                "endDateExclusive",
            )
        }
        return dashboardJson(reads.dashboard(request.requireSession(), start, end))
    }

    private suspend fun recordPurchase(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("vendorAccountId", "occurredAt", "rawLiters", "fatPercent", "lrReading"),
            allowed = setOf("vendorAccountId", "occurredAt", "rawLiters", "fatPercent", "lrReading", "notes"),
        )
        val rawLiters = positiveDecimal(request.payload, "rawLiters", "Milk received must be greater than zero")
        val fatPercent = request.payload.requiredDecimal("fatPercent").toFiniteDouble("fatPercent")
        if (fatPercent < 0.0) {
            throw BridgeProtocolFailure("validation_failed", "Fat percentage cannot be negative", "fatPercent")
        }
        val lrReading = positiveDecimal(request.payload, "lrReading", "LR reading must be greater than zero")
        val result = commands.recordPurchase(
            request.requireSession(),
            VendorPurchaseInput(
                vendorAccountId = request.payload.requiredId("vendorAccountId"),
                occurredAt = request.payload.requiredDate("occurredAt"),
                rawLiters = rawLiters,
                fatPercentage = fatPercent,
                lrReading = lrReading,
                notes = request.payload.optionalString("notes"),
            ),
        )
        return JSONObject()
            .put("eventId", result.eventId.toString())
            .put("snfPercent", decimal(result.snfPercentage))
            .put("tsPercent", decimal(result.tsPercentage))
            .put("adjustedLiters", decimal(result.adjustedLiters))
            .put("amountOwedPaisa", money(result.amountOwed).toString())
    }

    private fun positiveDecimal(payload: JSONObject, field: String, message: String): Double {
        val value = payload.requiredDecimal(field).toFiniteDouble(field)
        if (value <= 0.0) throw BridgeProtocolFailure("validation_failed", message, field)
        return value
    }
}

private fun BigDecimal.toFiniteDouble(field: String): Double = toDouble().also {
    if (!it.isFinite()) throw BridgeProtocolFailure("validation_failed", "$field is outside the supported range", field)
}
