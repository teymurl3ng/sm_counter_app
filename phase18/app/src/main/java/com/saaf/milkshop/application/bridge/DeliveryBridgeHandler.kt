package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.domain.DeliveryInput
import com.saaf.milkshop.domain.DeliveryService
import org.json.JSONObject

internal class DeliveryBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: DeliveryBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.DELIVERIES -> {
            strictPayload(request.payload, emptySet())
            deliveriesJson(reads.deliveries(request.requireSession()))
        }
        HybridBridgeActions.CREATE_DELIVERY -> create(request)
        HybridBridgeActions.TRANSITION_DELIVERY -> transition(request)
        HybridBridgeActions.LINK_DELIVERY_SALE -> linkSale(request)
        HybridBridgeActions.ARCHIVE_DELIVERY -> archive(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun create(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("customerAccountId", "productId", "quantity", "businessDate", "saleEventId"),
            allowed = setOf("customerAccountId", "productId", "quantity", "route", "businessDate", "saleEventId", "notes"),
        )
        val quantity = request.payload.requiredDecimal("quantity")
        if (quantity.signum() <= 0) {
            throw BridgeProtocolFailure("validation_failed", "Delivery quantity must be greater than zero", "quantity")
        }
        val id = commands.create(
            request.requireSession(),
            DeliveryInput(
                customerAccountId = request.payload.requiredId("customerAccountId"),
                productId = request.payload.requiredId("productId"),
                quantity = quantity.toDouble(),
                route = request.payload.optionalString("route"),
                businessDate = request.payload.requiredDate("businessDate"),
                saleEventId = request.payload.requiredNullableId("saleEventId"),
                notes = request.payload.optionalString("notes"),
            ),
        )
        return JSONObject().put("entityId", id.toString())
    }

    private suspend fun transition(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("deliveryId", "statusCode"),
            allowed = setOf("deliveryId", "statusCode", "notes"),
        )
        val status = request.payload.requiredString("statusCode")
        if (status !in DeliveryService.TERMINAL_STATUSES) {
            throw BridgeProtocolFailure("validation_failed", "Choose delivered or cancelled", "statusCode")
        }
        commands.transition(
            request.requireSession(),
            request.payload.requiredId("deliveryId"),
            status,
            request.payload.optionalString("notes"),
        )
        return JSONObject()
    }

    private suspend fun linkSale(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("deliveryId", "saleEventId"))
        commands.linkSale(
            request.requireSession(),
            request.payload.requiredId("deliveryId"),
            request.payload.requiredId("saleEventId"),
        )
        return JSONObject()
    }

    private suspend fun archive(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("deliveryId"))
        commands.archive(request.requireSession(), request.payload.requiredId("deliveryId"))
        return JSONObject()
    }
}

private fun JSONObject.requiredNullableId(name: String): Long? {
    if (!has(name)) throw BridgeProtocolFailure("invalid_request", "$name is required", name)
    if (isNull(name)) return null
    return requiredId(name)
}
