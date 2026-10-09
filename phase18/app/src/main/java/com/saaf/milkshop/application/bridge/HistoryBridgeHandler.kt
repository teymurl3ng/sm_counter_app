package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.commands.HistoryBridgeCommandService
import com.saaf.milkshop.domain.CorrectionInput
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

internal class HistoryBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: HistoryBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.EVENT_HISTORY -> eventHistory(request)
        HybridBridgeActions.EVENT_DETAIL -> eventDetail(request)
        HybridBridgeActions.CORRECT_EVENT -> correctEvent(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun eventHistory(request: BridgeRequest): JSONArray {
        strictPayload(request.payload, emptySet())
        return jsonArray(reads.eventHistory(request.requireSession()), ::eventHistoryJson)
    }

    private suspend fun eventDetail(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("eventId"))
        return try {
            eventDetailJson(reads.eventDetail(request.requireSession(), request.payload.requiredId("eventId")))
        } catch (failure: IllegalStateException) {
            if (failure.message?.contains("does not exist", ignoreCase = true) == true) {
                throw BridgeCommandNotFound("This transaction no longer exists")
            }
            throw failure
        }
    }

    private suspend fun correctEvent(request: BridgeRequest): JSONObject {
        val common = setOf("correctionTypeCode", "originalEventId", "occurredAt")
        val optional = setOf("notes")
        val type = request.payload.requiredString("correctionTypeCode")
        val originalId = request.payload.requiredId("originalEventId")
        val occurredAt = request.payload.requiredDate("occurredAt")
        val notes = request.payload.optionalString("notes")
        val input = when (type) {
            "purchase" -> {
                strictObject(request.payload, common + setOf("rawLiters", "fatPercent", "lrReading"), common + optional + setOf("rawLiters", "fatPercent", "lrReading"))
                CorrectionInput.Purchase(originalId, occurredAt, positiveDecimal(request.payload, "rawLiters"), nonNegativeDecimal(request.payload, "fatPercent"), positiveDecimal(request.payload, "lrReading"), notes)
            }
            "production" -> {
                strictObject(request.payload, common + setOf("milkLitersUsed", "yogurtKgProduced"), common + optional + setOf("milkLitersUsed", "yogurtKgProduced"))
                CorrectionInput.Production(originalId, occurredAt, positiveDecimal(request.payload, "milkLitersUsed"), positiveDecimal(request.payload, "yogurtKgProduced"), notes)
            }
            "sale" -> saleCorrection(request, common, optional, originalId, occurredAt, notes)
            "monetary" -> {
                strictObject(request.payload, common + setOf("amountPaisa"), common + optional + setOf("amountPaisa"))
                CorrectionInput.Monetary(originalId, occurredAt, positivePaisa(request.payload, "amountPaisa"), notes)
            }
            "spoilage" -> {
                strictObject(request.payload, common + setOf("quantity", "reasonCode"), common + optional + setOf("quantity", "reasonCode"))
                CorrectionInput.Spoilage(originalId, occurredAt, positiveDecimal(request.payload, "quantity"), request.payload.requiredString("reasonCode"), notes)
            }
            else -> throw BridgeProtocolFailure("validation_failed", "Choose a supported correction type", "correctionTypeCode")
        }
        val result = commands.correct(request.requireSession(), input)
        return JSONObject().put("eventId", result.eventId.toString())
    }

    private fun saleCorrection(request: BridgeRequest, common: Set<String>, optional: Set<String>, originalId: Long, occurredAt: String, notes: String?): CorrectionInput.Sale {
        val fields = setOf("unitRatePaisa", "quantity", "valuePaisa", "surchargePaisa", "channelCode")
        strictObject(request.payload, common + setOf("unitRatePaisa"), common + optional + fields)
        val quantity = request.payload.optionalDecimal("quantity")
        val valuePaisa = request.payload.optionalPaisa("valuePaisa")
        if ((quantity == null) == (valuePaisa == null)) throw BridgeProtocolFailure("validation_failed", "Enter either corrected quantity or corrected sale value", "quantity")
        return CorrectionInput.Sale(
            originalId, occurredAt, positivePaisa(request.payload, "unitRatePaisa"),
            quantity = quantity?.also { if (it <= 0.0) throw BridgeProtocolFailure("validation_failed", "Corrected quantity must be greater than zero", "quantity") },
            value = valuePaisa?.let { if (it <= 0L) throw BridgeProtocolFailure("validation_failed", "Corrected sale value must be greater than zero", "valuePaisa") else BigDecimal(it).movePointLeft(2).toDouble() },
            surchargeAmount = request.payload.optionalPaisa("surchargePaisa")?.let { if (it < 0L) throw BridgeProtocolFailure("validation_failed", "Delivery charge cannot be negative", "surchargePaisa") else BigDecimal(it).movePointLeft(2).toDouble() } ?: 0.0,
            channel = request.payload.optionalString("channelCode"), notes = notes,
        )
    }

    private fun positiveDecimal(payload: JSONObject, field: String): Double = payload.requiredDecimal(field).finite(field).also {
        if (it <= 0.0) throw BridgeProtocolFailure("validation_failed", "$field must be greater than zero", field)
    }

    private fun nonNegativeDecimal(payload: JSONObject, field: String): Double = payload.requiredDecimal(field).finite(field).also {
        if (it < 0.0) throw BridgeProtocolFailure("validation_failed", "$field cannot be negative", field)
    }

    private fun positivePaisa(payload: JSONObject, field: String): Double {
        val value = payload.requiredPaisa(field)
        if (value <= 0L) throw BridgeProtocolFailure("validation_failed", "$field must be greater than zero", field)
        return BigDecimal(value).movePointLeft(2).toDouble()
    }
}

private fun JSONObject.optionalDecimal(name: String): Double? = if (has(name)) requiredDecimal(name).finite(name) else null
private fun JSONObject.optionalPaisa(name: String): Long? = if (has(name)) requiredPaisa(name) else null
private fun BigDecimal.finite(field: String): Double = toDouble().also {
    if (!it.isFinite()) throw BridgeProtocolFailure("validation_failed", "$field is outside the supported range", field)
}
