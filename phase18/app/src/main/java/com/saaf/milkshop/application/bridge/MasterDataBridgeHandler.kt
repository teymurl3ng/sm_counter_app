package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.VendorProfileInput
import org.json.JSONObject
import java.math.BigDecimal

internal class MasterDataBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: HybridCommandService,
) {
    suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.MASTER_DATA -> {
            strictPayload(request.payload, emptySet())
            masterDataJson(reads.masterData(request.requireSession()))
        }
        HybridBridgeActions.TRANSACTION_REFERENCES -> {
            strictPayload(request.payload, emptySet())
            transactionReferencesJson(reads.transactionReferences(request.requireSession()))
        }
        HybridBridgeActions.CREATE_PRODUCT -> JSONObject().put(
            "entityId",
            commands.createProduct(request.requireSession(), request.payload.productInput(false).second).toString(),
        )
        HybridBridgeActions.UPDATE_PRODUCT -> {
            val (productId, input) = request.payload.productInput(true)
            commands.updateProduct(request.requireSession(), productId!!, input)
            JSONObject()
        }
        HybridBridgeActions.SET_PRODUCT_ARCHIVED -> {
            strictPayload(request.payload, setOf("productId", "archived"))
            commands.setProductArchived(
                request.requireSession(),
                request.payload.requiredId("productId"),
                request.payload.requiredBoolean("archived"),
            )
            JSONObject()
        }
        HybridBridgeActions.CREATE_CUSTOMER -> {
            strictPayload(request.payload, setOf("name"))
            JSONObject().put(
                "entityId",
                commands.createCustomer(request.requireSession(), request.payload.requiredString("name")).toString(),
            )
        }
        HybridBridgeActions.RENAME_CUSTOMER -> {
            strictPayload(request.payload, setOf("accountId", "name"))
            commands.renameCustomer(
                request.requireSession(),
                request.payload.requiredId("accountId"),
                request.payload.requiredString("name"),
            )
            JSONObject()
        }
        HybridBridgeActions.SAVE_CUSTOMER_PROFILE -> saveCustomerProfile(request)
        HybridBridgeActions.CREATE_VENDOR -> {
            strictPayload(request.payload, setOf("name"))
            JSONObject().put(
                "entityId",
                commands.createVendor(request.requireSession(), request.payload.requiredString("name")).toString(),
            )
        }
        HybridBridgeActions.RENAME_VENDOR -> {
            strictPayload(request.payload, setOf("accountId", "name"))
            commands.renameVendor(
                request.requireSession(),
                request.payload.requiredId("accountId"),
                request.payload.requiredString("name"),
            )
            JSONObject()
        }
        HybridBridgeActions.SAVE_VENDOR_PROFILE -> saveVendorProfile(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun saveCustomerProfile(request: BridgeRequest): JSONObject {
        strictPayload(
            request.payload,
            setOf("accountId", "phone", "areaRoute", "address", "notes", "creditLimitPaisa", "dailyQuota", "active"),
        )
        val creditLimitPaisa = request.payload.nullablePaisa("creditLimitPaisa")
        if (creditLimitPaisa != null && creditLimitPaisa < 0L) {
            throw BridgeProtocolFailure("validation_failed", "Credit limit cannot be negative", "creditLimitPaisa")
        }
        val dailyQuota = request.payload.nullableDecimal("dailyQuota")
        if (dailyQuota != null && dailyQuota < BigDecimal.ZERO) {
            throw BridgeProtocolFailure("validation_failed", "Daily quota cannot be negative", "dailyQuota")
        }
        commands.saveCustomerProfile(
            request.requireSession(),
            request.payload.requiredId("accountId"),
            CustomerProfileInput(
                phone = request.payload.nullableString("phone"),
                areaRoute = request.payload.nullableString("areaRoute"),
                address = request.payload.nullableString("address"),
                notes = request.payload.nullableString("notes"),
                creditLimit = creditLimitPaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
                dailyQuota = dailyQuota?.toDouble(),
                active = request.payload.requiredBoolean("active"),
            ),
        )
        return JSONObject()
    }

    private suspend fun saveVendorProfile(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("accountId", "phone", "location", "notes", "active"))
        commands.saveVendorProfile(
            request.requireSession(),
            request.payload.requiredId("accountId"),
            VendorProfileInput(
                phone = request.payload.nullableString("phone"),
                location = request.payload.nullableString("location"),
                notes = request.payload.nullableString("notes"),
                active = request.payload.requiredBoolean("active"),
            ),
        )
        return JSONObject()
    }
}
