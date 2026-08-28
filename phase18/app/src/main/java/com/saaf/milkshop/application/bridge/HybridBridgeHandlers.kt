package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorProfileInput
import org.json.JSONObject
import java.math.BigDecimal

internal class AuthenticationBridgeHandler(
    private val reads: HybridReadModelService,
    private val security: UserSecurityService,
    private val onSessionChanged: (String?) -> Unit,
) {
    suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.BOOTSTRAP_STATUS -> bootstrapStatusJson(reads.bootstrapStatus())
        HybridBridgeActions.BOOTSTRAP_OWNER -> bootstrapOwner(request)
        HybridBridgeActions.LOGIN -> login(request)
        HybridBridgeActions.LOGOUT -> logout(request)
        HybridBridgeActions.CURRENT_SESSION -> currentSession(request)
        else -> unavailable()
    }

    private suspend fun bootstrapOwner(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("loginName", "displayName", "passphrase"))
        if (!security.needsOwnerBootstrap()) throw BridgeCommandConflict("Owner setup has already been completed")
        val passphrase = request.payload.requiredString("passphrase").toCharArray()
        return try {
            val result = security.bootstrapOwner(request.payload.requiredString("loginName"), request.payload.requiredString("displayName"), passphrase)
            onSessionChanged(result.session.sessionId)
            JSONObject().put("sessionHandle", result.session.sessionId)
                .put("currentSession", currentSessionJson(reads.currentSession(result.session.sessionId)))
                .put("oneTimeRecoveryCode", result.recoveryCode)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun login(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("loginName", "passphrase"))
        val passphrase = request.payload.requiredString("passphrase").toCharArray()
        return try {
            val session = security.login(request.payload.requiredString("loginName"), passphrase)
            onSessionChanged(session.sessionId)
            JSONObject().put("sessionHandle", session.sessionId).put("currentSession", currentSessionJson(reads.currentSession(session.sessionId)))
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun logout(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        security.logout(request.requireSession())
        onSessionChanged(null)
        return JSONObject()
    }

    private suspend fun currentSession(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        return currentSessionJson(reads.currentSession(request.requireSession()))
    }
}

internal class MasterDataBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: HybridCommandService,
) {
    suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.MASTER_DATA -> {
            strictPayload(request.payload, emptySet())
            masterDataJson(reads.masterData(request.requireSession()))
        }
        HybridBridgeActions.CREATE_PRODUCT -> JSONObject().put("entityId", commands.createProduct(request.requireSession(), request.payload.productInput(false).second).toString())
        HybridBridgeActions.UPDATE_PRODUCT -> {
            val (productId, input) = request.payload.productInput(true)
            commands.updateProduct(request.requireSession(), productId!!, input)
            JSONObject()
        }
        HybridBridgeActions.SET_PRODUCT_ARCHIVED -> {
            strictPayload(request.payload, setOf("productId", "archived"))
            commands.setProductArchived(request.requireSession(), request.payload.requiredId("productId"), request.payload.requiredBoolean("archived"))
            JSONObject()
        }
        HybridBridgeActions.CREATE_CUSTOMER -> {
            strictPayload(request.payload, setOf("name"))
            JSONObject().put("entityId", commands.createCustomer(request.requireSession(), request.payload.requiredString("name")).toString())
        }
        HybridBridgeActions.RENAME_CUSTOMER -> {
            strictPayload(request.payload, setOf("accountId", "name"))
            commands.renameCustomer(request.requireSession(), request.payload.requiredId("accountId"), request.payload.requiredString("name"))
            JSONObject()
        }
        HybridBridgeActions.SAVE_CUSTOMER_PROFILE -> saveCustomerProfile(request)
        HybridBridgeActions.CREATE_VENDOR -> {
            strictPayload(request.payload, setOf("name"))
            JSONObject().put("entityId", commands.createVendor(request.requireSession(), request.payload.requiredString("name")).toString())
        }
        HybridBridgeActions.RENAME_VENDOR -> {
            strictPayload(request.payload, setOf("accountId", "name"))
            commands.renameVendor(request.requireSession(), request.payload.requiredId("accountId"), request.payload.requiredString("name"))
            JSONObject()
        }
        HybridBridgeActions.SAVE_VENDOR_PROFILE -> saveVendorProfile(request)
        else -> unavailable()
    }

    private suspend fun saveCustomerProfile(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("accountId", "phone", "areaRoute", "address", "notes", "creditLimitPaisa", "dailyQuota", "active"))
        val creditLimitPaisa = request.payload.nullablePaisa("creditLimitPaisa")
        if (creditLimitPaisa != null && creditLimitPaisa < 0L) throw BridgeProtocolFailure("validation_failed", "Credit limit cannot be negative", "creditLimitPaisa")
        val dailyQuota = request.payload.nullableDecimal("dailyQuota")
        if (dailyQuota != null && dailyQuota < BigDecimal.ZERO) throw BridgeProtocolFailure("validation_failed", "Daily quota cannot be negative", "dailyQuota")
        commands.saveCustomerProfile(
            request.requireSession(),
            request.payload.requiredId("accountId"),
            CustomerProfileInput(
                phone = request.payload.nullableString("phone"), areaRoute = request.payload.nullableString("areaRoute"),
                address = request.payload.nullableString("address"), notes = request.payload.nullableString("notes"),
                creditLimit = creditLimitPaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
                dailyQuota = dailyQuota?.toDouble(), active = request.payload.requiredBoolean("active"),
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

private fun unavailable(): Nothing = throw BridgeProtocolFailure("invalid_request", "This action is not available in the current app slice")
