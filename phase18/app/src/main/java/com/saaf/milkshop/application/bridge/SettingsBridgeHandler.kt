package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.OpeningAccountBalance
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.StoreIdentityInput
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

internal class SettingsBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: SettingsBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.BUSINESS_SETTINGS -> {
            strictPayload(request.payload, emptySet())
            businessSettingsJson(reads.businessSettings(request.requireSession()))
        }
        HybridBridgeActions.SAVE_STORE_IDENTITY -> {
            strictPayload(request.payload, setOf("storeName", "phone", "address"))
            commands.saveStoreIdentity(
                request.requireSession(),
                StoreIdentityInput(
                    storeName = request.payload.requiredString("storeName"),
                    phone = request.payload.nullableString("phone"),
                    address = request.payload.nullableString("address"),
                ),
            )
            JSONObject()
        }
        HybridBridgeActions.UPDATE_BUSINESS_SETTINGS -> updateBusinessSettings(request)
        HybridBridgeActions.CREATE_EXPENSE_CATEGORY -> {
            strictPayload(request.payload, setOf("displayName"))
            JSONObject().put(
                "entityId",
                commands.createExpenseCategory(
                    request.requireSession(),
                    request.payload.requiredString("displayName"),
                ).toString(),
            )
        }
        HybridBridgeActions.SET_EXPENSE_CATEGORY_ARCHIVED -> {
            strictPayload(request.payload, setOf("accountId", "archived"))
            commands.setExpenseCategoryArchived(
                request.requireSession(),
                request.payload.requiredId("accountId"),
                request.payload.requiredBoolean("archived"),
            )
            JSONObject()
        }
        HybridBridgeActions.RECORD_OPENING_STATE -> openingState(request, correction = false)
        HybridBridgeActions.CORRECT_OPENING_STATE -> openingState(request, correction = true)
        else -> unavailableBridgeAction()
    }

    private suspend fun openingState(request: BridgeRequest, correction: Boolean): JSONObject {
        strictObject(
            request.payload,
            required = setOf("openingDate", "inventoryBalances", "accountBalances"),
            allowed = setOf("openingDate", "inventoryBalances", "accountBalances", "notes"),
        )
        val input = OpeningStateInput(
            openingDate = request.payload.requiredDate("openingDate"),
            inventoryBalances = request.payload.requiredArray("inventoryBalances")
                .mapObjects("inventoryBalances") { item ->
                    strictPayload(item, setOf("productId", "quantity"))
                    val quantity = item.requiredDecimal("quantity")
                    if (quantity < BigDecimal.ZERO) {
                        throw BridgeProtocolFailure("validation_failed", "Opening quantity cannot be negative", "quantity")
                    }
                    OpeningInventoryBalance(item.requiredId("productId"), quantity.toDouble())
                },
            accountBalances = request.payload.requiredArray("accountBalances")
                .mapObjects("accountBalances") { item ->
                    strictPayload(item, setOf("accountId", "balancePaisa"))
                    val balancePaisa = item.requiredPaisa("balancePaisa")
                    if (balancePaisa < 0L) {
                        throw BridgeProtocolFailure("validation_failed", "Opening balance cannot be negative", "balancePaisa")
                    }
                    OpeningAccountBalance(
                        item.requiredId("accountId"),
                        BigDecimal(balancePaisa).movePointLeft(2).toDouble(),
                    )
                },
            notes = request.payload.optionalString("notes"),
        )
        val eventId = if (correction) {
            commands.correctOpeningState(request.requireSession(), input)
        } else {
            commands.recordOpeningState(request.requireSession(), input)
        }
        return JSONObject().put("eventId", eventId.toString())
    }

    private suspend fun updateBusinessSettings(request: BridgeRequest): JSONObject {
        strictPayload(
            request.payload,
            setOf(
                "vendorMilkRatePaisaPerUnit", "vendorDeliveryFeePaisaPerUnit", "tsStandardPercent",
                "retailMilkRatePaisaPerUnit", "retailYogurtRatePaisaPerUnit", "defaultDeliverySurchargePaisa",
            ),
        )
        fun positiveMoney(field: String): Double {
            val value = request.payload.requiredPaisa(field)
            if (value <= 0L) {
                throw BridgeProtocolFailure("validation_failed", "$field must be greater than zero", field)
            }
            return BigDecimal(value).movePointLeft(2).toDouble()
        }
        val tsStandard = request.payload.requiredDecimal("tsStandardPercent").toDouble()
        if (tsStandard <= 0.0 || !tsStandard.isFinite()) {
            throw BridgeProtocolFailure(
                "validation_failed",
                "tsStandardPercent must be greater than zero",
                "tsStandardPercent",
            )
        }
        commands.updateBusinessSettings(
            request.requireSession(),
            BusinessSettings(
                milkRateVendor = positiveMoney("vendorMilkRatePaisaPerUnit"),
                deliveryFeeVendor = positiveMoney("vendorDeliveryFeePaisaPerUnit"),
                tsStandard = tsStandard,
                retailRateMilk = positiveMoney("retailMilkRatePaisaPerUnit"),
                retailRateYogurt = positiveMoney("retailYogurtRatePaisaPerUnit"),
                defaultDeliverySurcharge = positiveMoney("defaultDeliverySurchargePaisa"),
            ),
        )
        return JSONObject()
    }
}

private fun JSONObject.requiredArray(name: String): JSONArray {
    if (!has(name) || isNull(name) || get(name) !is JSONArray) {
        throw BridgeProtocolFailure("invalid_request", "$name must be a list", name)
    }
    return getJSONArray(name)
}

private inline fun <T> JSONArray.mapObjects(field: String, transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { index ->
        val item = get(index) as? JSONObject
            ?: throw BridgeProtocolFailure("invalid_request", "$field entries must be objects", field)
        transform(item)
    }
