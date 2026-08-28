package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.decimal
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.YogurtProductionInput
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

internal class OperationsBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: OperationsBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.DASHBOARD -> dashboard(request)
        HybridBridgeActions.CASHIER_SUMMARY -> cashierSummary(request)
        HybridBridgeActions.RECORD_PURCHASE -> recordPurchase(request)
        HybridBridgeActions.RECORD_SALE -> recordSale(request)
        HybridBridgeActions.RECORD_PRODUCTION -> recordProduction(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun dashboard(request: BridgeRequest): JSONObject {
        val (start, end) = request.dateRange()
        return dashboardJson(reads.dashboard(request.requireSession(), start, end))
    }

    private suspend fun cashierSummary(request: BridgeRequest): JSONObject {
        val (start, end) = request.dateRange()
        return cashierSummaryJson(reads.cashierSummary(request.requireSession(), start, end))
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

    private suspend fun recordSale(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf(
                "productId", "occurredAt", "channelCode", "unitRatePaisa", "quantity", "valuePaisa",
                "paymentKindCode", "paymentAccountId", "customerAccountId", "surchargeEnabled", "surchargePaisa",
            ),
            allowed = setOf(
                "productId", "occurredAt", "channelCode", "unitRatePaisa", "quantity", "valuePaisa",
                "paymentKindCode", "paymentAccountId", "customerAccountId", "surchargeEnabled", "surchargePaisa", "notes",
            ),
        )
        val channel = request.payload.nullableString("channelCode")
        if (channel != null && channel !in SALE_CHANNELS) {
            throw BridgeProtocolFailure("validation_failed", "Choose retail or wholesale sale type", "channelCode")
        }
        val unitRatePaisa = request.payload.requiredPaisa("unitRatePaisa")
        if (unitRatePaisa <= 0L) {
            throw BridgeProtocolFailure("validation_failed", "Selling rate must be greater than zero", "unitRatePaisa")
        }
        val quantity = request.payload.nullableDecimal("quantity")?.toFiniteDouble("quantity")
        val valuePaisa = request.payload.nullablePaisa("valuePaisa")
        if ((quantity == null) == (valuePaisa == null)) {
            throw BridgeProtocolFailure("validation_failed", "Enter either quantity or sale value, not both", "quantity")
        }
        if (quantity != null && quantity <= 0.0) {
            throw BridgeProtocolFailure("validation_failed", "Sale quantity must be greater than zero", "quantity")
        }
        if (valuePaisa != null && valuePaisa <= 0L) {
            throw BridgeProtocolFailure("validation_failed", "Sale value must be greater than zero", "valuePaisa")
        }
        val paymentKind = request.payload.requiredString("paymentKindCode")
        val paymentAccountId = request.payload.nullableId("paymentAccountId")
        val customerAccountId = request.payload.nullableId("customerAccountId")
        val payment = when (paymentKind) {
            "account" -> {
                if (paymentAccountId == null || customerAccountId != null) {
                    throw BridgeProtocolFailure("validation_failed", "Choose one cash or bank account", "paymentAccountId")
                }
                SalePayment.Account(paymentAccountId)
            }
            "credit" -> {
                if (customerAccountId == null || paymentAccountId != null) {
                    throw BridgeProtocolFailure("validation_failed", "Choose one customer for the credit sale", "customerAccountId")
                }
                SalePayment.OnCredit(customerAccountId)
            }
            else -> throw BridgeProtocolFailure("validation_failed", "Choose paid now or customer credit", "paymentKindCode")
        }
        val surchargeEnabled = request.payload.requiredBoolean("surchargeEnabled")
        val surchargePaisa = request.payload.nullablePaisa("surchargePaisa")
        if (!surchargeEnabled && surchargePaisa != null) {
            throw BridgeProtocolFailure("validation_failed", "Turn on delivery charge before entering an amount", "surchargePaisa")
        }
        if (surchargePaisa != null && surchargePaisa < 0L) {
            throw BridgeProtocolFailure("validation_failed", "Delivery charge cannot be negative", "surchargePaisa")
        }
        val result = commands.recordSale(
            request.requireSession(),
            SaleInput(
                productId = request.payload.requiredId("productId"),
                occurredAt = request.payload.requiredDate("occurredAt"),
                channel = channel,
                unitRate = BigDecimal(unitRatePaisa).movePointLeft(2).toDouble(),
                quantity = quantity,
                value = valuePaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
                payment = payment,
                surchargeEnabled = surchargeEnabled,
                surchargeAmount = surchargePaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
                notes = request.payload.optionalString("notes"),
            ),
        )
        return JSONObject()
            .put("eventId", result.eventId.toString())
            .put("quantity", decimal(result.quantity))
            .put("valuePaisa", money(result.value).toString())
            .put("surchargePaisa", money(result.surcharge).toString())
            .put("totalPaisa", money(result.total).toString())
    }

    private suspend fun recordProduction(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("occurredAt", "milkLitersUsed", "yogurtKgProduced"),
            allowed = setOf("occurredAt", "milkLitersUsed", "yogurtKgProduced", "notes"),
        )
        val milkUsed = positiveDecimal(
            request.payload,
            "milkLitersUsed",
            "Milk used must be greater than zero",
        )
        val yogurtProduced = positiveDecimal(
            request.payload,
            "yogurtKgProduced",
            "Yogurt produced must be greater than zero",
        )
        val result = commands.recordProduction(
            request.requireSession(),
            YogurtProductionInput(
                occurredAt = request.payload.requiredDate("occurredAt"),
                milkLitersUsed = milkUsed,
                yogurtKgProduced = yogurtProduced,
                notes = request.payload.optionalString("notes"),
            ),
        )
        return JSONObject().put("eventId", result.eventId.toString())
    }

    private fun positiveDecimal(payload: JSONObject, field: String, message: String): Double {
        val value = payload.requiredDecimal(field).toFiniteDouble(field)
        if (value <= 0.0) throw BridgeProtocolFailure("validation_failed", message, field)
        return value
    }
}

private fun BridgeRequest.dateRange(): Pair<String, String> {
    strictPayload(payload, setOf("startDateInclusive", "endDateExclusive"))
    val start = payload.requiredDate("startDateInclusive")
    val end = payload.requiredDate("endDateExclusive")
    if (!LocalDate.parse(end).isAfter(LocalDate.parse(start))) {
        throw BridgeProtocolFailure(
            "invalid_request",
            "endDateExclusive must be after startDateInclusive",
            "endDateExclusive",
        )
    }
    return start to end
}

private fun JSONObject.nullableId(name: String): Long? {
    val raw = nullableString(name) ?: return null
    if (!Regex("[1-9][0-9]*").matches(raw)) {
        throw BridgeProtocolFailure("validation_failed", "$name must be a positive record ID", name)
    }
    return raw.toLongOrNull()
        ?: throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name)
}

private fun BigDecimal.toFiniteDouble(field: String): Double = toDouble().also {
    if (!it.isFinite()) throw BridgeProtocolFailure("validation_failed", "$field is outside the supported range", field)
}

private val SALE_CHANNELS = setOf("retail", "wholesale", "credit-retail", "credit-wholesale")
