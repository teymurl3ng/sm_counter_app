package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.AccountReferenceDto
import com.saaf.milkshop.application.readmodels.BootstrapStatusDto
import com.saaf.milkshop.application.readmodels.CurrentSessionDto
import com.saaf.milkshop.application.readmodels.CustomerDto
import com.saaf.milkshop.application.readmodels.EmployeeDto
import com.saaf.milkshop.application.readmodels.ExpenseCategoryDto
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.MasterDataDto
import com.saaf.milkshop.application.readmodels.ProductDto
import com.saaf.milkshop.application.readmodels.VendorDto
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HybridBridgeDispatcher(
    database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val onSessionChanged: (String?) -> Unit = {},
) {
    private val reads = HybridReadModelService(database, security)
    private val commands = HybridCommandService(database, security)
    private val inFlightRequestIds = ConcurrentHashMap.newKeySet<String>()

    suspend fun dispatch(rawMessage: String): String {
        var requestId = "unknown"
        var action = "unknown"
        var acceptedRequestId: String? = null
        return try {
            if (rawMessage.toByteArray(Charsets.UTF_8).size > MAX_MESSAGE_BYTES) {
                throw BridgeProtocolFailure("invalid_request", "The request is too large")
            }
            val envelope = JSONObject(rawMessage)
            requestId = envelope.optString("requestId", "unknown")
            action = envelope.optString("action", "unknown")
            val request = parseRequest(envelope)
            requestId = request.requestId
            action = request.action
            if (!inFlightRequestIds.add(requestId)) {
                throw BridgeCommandConflict("A request with this ID is already running")
            }
            acceptedRequestId = requestId
            successEnvelope(request, execute(request)).toString()
        } catch (error: Throwable) {
            failureEnvelope(requestId, action, mapFailure(error)).toString()
        } finally {
            acceptedRequestId?.let(inFlightRequestIds::remove)
        }
    }

    private suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        ACTION_BRIDGE_INFO -> JSONObject()
            .put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("contractSha256", BridgeContractSnapshot.CONTRACT_SHA256)
            .put("supportedActions", jsonArray(SUPPORTED_ACTIONS))

        ACTION_BOOTSTRAP_STATUS -> bootstrapStatusJson(reads.bootstrapStatus())

        ACTION_BOOTSTRAP_OWNER -> {
            strictPayload(request.payload, setOf("loginName", "displayName", "passphrase"))
            if (!security.needsOwnerBootstrap()) {
                throw BridgeCommandConflict("Owner setup has already been completed")
            }
            val passphrase = request.payload.requiredString("passphrase").toCharArray()
            try {
                val result = security.bootstrapOwner(
                    request.payload.requiredString("loginName"),
                    request.payload.requiredString("displayName"),
                    passphrase,
                )
                onSessionChanged(result.session.sessionId)
                JSONObject()
                    .put("sessionHandle", result.session.sessionId)
                    .put("currentSession", currentSessionJson(reads.currentSession(result.session.sessionId)))
                    .put("oneTimeRecoveryCode", result.recoveryCode)
            } finally {
                passphrase.fill('\u0000')
            }
        }

        ACTION_LOGIN -> {
            strictPayload(request.payload, setOf("loginName", "passphrase"))
            val passphrase = request.payload.requiredString("passphrase").toCharArray()
            try {
                val session = security.login(request.payload.requiredString("loginName"), passphrase)
                onSessionChanged(session.sessionId)
                JSONObject()
                    .put("sessionHandle", session.sessionId)
                    .put("currentSession", currentSessionJson(reads.currentSession(session.sessionId)))
            } finally {
                passphrase.fill('\u0000')
            }
        }

        ACTION_LOGOUT -> {
            strictPayload(request.payload, emptySet())
            security.logout(request.requireSession())
            onSessionChanged(null)
            JSONObject()
        }

        ACTION_CURRENT_SESSION -> {
            strictPayload(request.payload, emptySet())
            currentSessionJson(reads.currentSession(request.requireSession()))
        }

        ACTION_MASTER_DATA -> {
            strictPayload(request.payload, emptySet())
            masterDataJson(reads.masterData(request.requireSession()))
        }

        ACTION_CREATE_PRODUCT -> {
            val productId = commands.createProduct(
                request.requireSession(),
                request.payload.productInput(includeProductId = false).second,
            )
            JSONObject().put("entityId", productId.toString())
        }

        ACTION_UPDATE_PRODUCT -> {
            val (productId, input) = request.payload.productInput(includeProductId = true)
            commands.updateProduct(request.requireSession(), productId!!, input)
            JSONObject()
        }

        ACTION_SET_PRODUCT_ARCHIVED -> {
            strictPayload(request.payload, setOf("productId", "archived"))
            commands.setProductArchived(
                request.requireSession(),
                request.payload.requiredId("productId"),
                request.payload.requiredBoolean("archived"),
            )
            JSONObject()
        }

        else -> throw BridgeProtocolFailure("invalid_request", "This action is not available in the current app slice")
    }

    private fun parseRequest(envelope: JSONObject): BridgeRequest {
        strictObject(
            envelope,
            required = setOf("contractVersion", "requestId", "action", "payload"),
            allowed = setOf("contractVersion", "requestId", "action", "sessionHandle", "payload"),
        )
        val version = envelope.get("contractVersion")
        if (version !is Int && version !is Long || (version as Number).toLong() != BridgeContractSnapshot.VERSION.toLong()) {
            throw BridgeProtocolFailure("invalid_request", "Bridge contract version does not match")
        }
        val requestId = envelope.requiredString("requestId")
        runCatching { UUID.fromString(requestId) }.getOrElse {
            throw BridgeProtocolFailure("invalid_request", "requestId must be a UUID", "requestId")
        }
        val action = envelope.requiredString("action")
        if (action !in SUPPORTED_ACTIONS) {
            throw BridgeProtocolFailure("invalid_request", "This action is not available in the current app slice", "action")
        }
        val payload = envelope.get("payload") as? JSONObject
            ?: throw BridgeProtocolFailure("invalid_request", "payload must be an object", "payload")
        val session = when {
            envelope.has("sessionHandle") && !envelope.isNull("sessionHandle") -> envelope.requiredString("sessionHandle")
            else -> null
        }
        val auth = AUTH_BY_ACTION.getValue(action)
        if (auth == "session" && session == null) {
            throw BridgeProtocolFailure("unauthenticated", "Sign in to continue")
        }
        if (auth == "none" && envelope.has("sessionHandle")) {
            throw BridgeProtocolFailure("invalid_request", "sessionHandle is not accepted for this action", "sessionHandle")
        }
        return BridgeRequest(requestId, action, session, payload)
    }

    private fun successEnvelope(request: BridgeRequest, data: JSONObject): JSONObject = JSONObject()
        .put("contractVersion", BridgeContractSnapshot.VERSION)
        .put("requestId", request.requestId)
        .put("action", request.action)
        .put("ok", true)
        .put("data", data)

    private fun failureEnvelope(requestId: String, action: String, failure: BridgeFailure): JSONObject = JSONObject()
        .put("contractVersion", BridgeContractSnapshot.VERSION)
        .put("requestId", requestId)
        .put("action", action)
        .put("ok", false)
        .put(
            "error",
            JSONObject()
                .put("code", failure.code)
                .put("message", failure.message)
                .putNullable("field", failure.field)
                .put("retryable", failure.retryable),
        )

    private fun mapFailure(error: Throwable): BridgeFailure = when (error) {
        is BridgeProtocolFailure -> BridgeFailure(error.code, error.message ?: "The request is invalid", error.field)
        is BridgeCommandConflict -> BridgeFailure("conflict", error.message ?: "The current state no longer permits this action")
        is BridgeCommandNotFound -> BridgeFailure("not_found", error.message ?: "The requested record no longer exists")
        is SecurityFailure.Authentication -> BridgeFailure("authentication_failed", SecurityFailure.GENERIC_AUTH_FAILURE)
        is SecurityFailure.SessionExpired -> BridgeFailure("session_expired", "Your session expired. Sign in again.")
        is SecurityFailure.PermissionDenied -> BridgeFailure("permission_denied", "You do not have permission to perform this action.")
        is SecurityFailure.RecentAuthenticationRequired -> BridgeFailure(
            "recent_authentication_required",
            "Enter your passphrase again to perform this sensitive action.",
        )
        is SecurityFailure.Validation -> BridgeFailure("validation_failed", error.message ?: "Check the entered information")
        is JSONException -> BridgeFailure("invalid_request", "The request is not valid JSON")
        is IllegalArgumentException -> BridgeFailure("validation_failed", error.message ?: "Check the entered information")
        else -> BridgeFailure("internal_error", "The app could not complete this request")
    }

    private data class BridgeRequest(
        val requestId: String,
        val action: String,
        val sessionHandle: String?,
        val payload: JSONObject,
    ) {
        fun requireSession(): String = sessionHandle
            ?: throw BridgeProtocolFailure("unauthenticated", "Sign in to continue")
    }

    private data class BridgeFailure(
        val code: String,
        val message: String,
        val field: String? = null,
        val retryable: Boolean = false,
    )

    companion object {
        const val MAX_MESSAGE_BYTES = 1024 * 1024
        const val ACTION_BRIDGE_INFO = "system.bridgeInfo"
        const val ACTION_BOOTSTRAP_STATUS = "query.bootstrapStatus"
        const val ACTION_BOOTSTRAP_OWNER = "command.auth.bootstrapOwner"
        const val ACTION_LOGIN = "command.auth.login"
        const val ACTION_LOGOUT = "command.auth.logout"
        const val ACTION_CURRENT_SESSION = "query.currentSession"
        const val ACTION_MASTER_DATA = "query.masterData"
        const val ACTION_CREATE_PRODUCT = "command.createProduct"
        const val ACTION_UPDATE_PRODUCT = "command.updateProduct"
        const val ACTION_SET_PRODUCT_ARCHIVED = "command.setProductArchived"

        val SUPPORTED_ACTIONS = listOf(
            ACTION_BRIDGE_INFO,
            ACTION_BOOTSTRAP_STATUS,
            ACTION_BOOTSTRAP_OWNER,
            ACTION_LOGIN,
            ACTION_LOGOUT,
            ACTION_CURRENT_SESSION,
            ACTION_MASTER_DATA,
            ACTION_CREATE_PRODUCT,
            ACTION_UPDATE_PRODUCT,
            ACTION_SET_PRODUCT_ARCHIVED,
        )

        private val AUTH_BY_ACTION = mapOf(
            ACTION_BRIDGE_INFO to "none",
            ACTION_BOOTSTRAP_STATUS to "none",
            ACTION_BOOTSTRAP_OWNER to "none",
            ACTION_LOGIN to "none",
            ACTION_LOGOUT to "session",
            ACTION_CURRENT_SESSION to "session",
            ACTION_MASTER_DATA to "session",
            ACTION_CREATE_PRODUCT to "session",
            ACTION_UPDATE_PRODUCT to "session",
            ACTION_SET_PRODUCT_ARCHIVED to "session",
        )
    }
}

private fun bootstrapStatusJson(value: BootstrapStatusDto): JSONObject =
    JSONObject().put("needsOwnerBootstrap", value.needsOwnerBootstrap)

private fun currentSessionJson(value: CurrentSessionDto): JSONObject = JSONObject()
    .put("authenticationStatusCode", value.authenticationStatusCode)
    .put("userId", value.userId.toString())
    .put("loginName", value.loginName)
    .put("displayName", value.displayName)
    .put("roleCode", value.roleCode)
    .putNullable("employeeAccountId", value.employeeAccountId?.toString())
    .put("effectivePermissionCodes", jsonArray(value.effectivePermissionCodes))

private fun masterDataJson(value: MasterDataDto): JSONObject = JSONObject()
    .put("products", jsonArray(value.products, ::productJson))
    .put("customers", jsonArray(value.customers, ::customerJson))
    .put("vendors", jsonArray(value.vendors, ::vendorJson))
    .put("employees", jsonArray(value.employees, ::employeeJson))
    .put("expenseCategories", jsonArray(value.expenseCategories, ::expenseCategoryJson))

private fun productJson(value: ProductDto): JSONObject = JSONObject()
    .put("productId", value.productId.toString())
    .put("name", value.name)
    .put("unit", value.unit)
    .put("quantityOnHand", value.quantityOnHand)
    .putNullable("defaultSaleRatePaisaPerUnit", value.defaultSaleRatePaisaPerUnit?.toString())
    .put("inventoryTracking", value.inventoryTracking)
    .put("archived", value.archived)
    .putNullable("notes", value.notes)

private fun customerJson(value: CustomerDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString())
    .put("name", value.name)
    .put("canonicalBalancePaisa", value.canonicalBalancePaisa.toString())
    .putNullable("phone", value.phone)
    .putNullable("areaRoute", value.areaRoute)
    .putNullable("address", value.address)
    .putNullable("notes", value.notes)
    .putNullable("creditLimitPaisa", value.creditLimitPaisa?.toString())
    .putNullable("dailyQuota", value.dailyQuota)
    .put("active", value.active)

private fun vendorJson(value: VendorDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString())
    .put("name", value.name)
    .put("canonicalBalancePaisa", value.canonicalBalancePaisa.toString())
    .putNullable("phone", value.phone)
    .putNullable("location", value.location)
    .putNullable("notes", value.notes)
    .put("active", value.active)

private fun employeeJson(value: EmployeeDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString())
    .put("name", value.name)
    .put("canonicalBalancePaisa", value.canonicalBalancePaisa.toString())
    .putNullable("roleLabel", value.roleLabel)
    .putNullable("phone", value.phone)
    .putNullable("referenceSalaryPaisa", value.referenceSalaryPaisa?.toString())
    .putNullable("salaryPeriodCode", value.salaryPeriodCode)
    .putNullable("joinDate", value.joinDate)
    .put("active", value.active)

private fun expenseCategoryJson(value: ExpenseCategoryDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString())
    .put("displayName", value.displayName)
    .put("active", value.active)

@Suppress("unused")
private fun accountReferenceJson(value: AccountReferenceDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString())
    .put("name", value.name)

private fun JSONObject.requiredString(name: String): String {
    if (!has(name) || isNull(name) || get(name) !is String) {
        throw BridgeProtocolFailure("invalid_request", "$name must be text", name)
    }
    return getString(name)
}

private fun JSONObject.requiredBoolean(name: String): Boolean {
    if (!has(name) || isNull(name) || get(name) !is Boolean) {
        throw BridgeProtocolFailure("invalid_request", "$name must be true or false", name)
    }
    return getBoolean(name)
}

private fun JSONObject.nullableString(name: String): String? {
    if (!has(name)) throw BridgeProtocolFailure("invalid_request", "$name is required", name)
    if (isNull(name)) return null
    if (get(name) !is String) throw BridgeProtocolFailure("invalid_request", "$name must be text or null", name)
    return getString(name)
}

private fun JSONObject.nullablePaisa(name: String): Long? {
    val raw = nullableString(name) ?: return null
    if (!Regex("-?(0|[1-9][0-9]*)").matches(raw)) {
        throw BridgeProtocolFailure("validation_failed", "$name must be whole PKR paisa", name)
    }
    return raw.toLongOrNull()
        ?: throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name)
}

private fun JSONObject.requiredId(name: String): Long {
    val raw = requiredString(name)
    if (!Regex("[1-9][0-9]*").matches(raw)) {
        throw BridgeProtocolFailure("validation_failed", "$name must be a positive record ID", name)
    }
    return raw.toLongOrNull()
        ?: throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name)
}

private fun JSONObject.productInput(includeProductId: Boolean): Pair<Long?, ProductCatalogInput> {
    val commonFields = setOf(
        "name",
        "unitCode",
        "defaultSaleRatePaisaPerUnit",
        "inventoryTracking",
        "notes",
    )
    strictPayload(this, if (includeProductId) commonFields + "productId" else commonFields)
    val productId = if (includeProductId) requiredId("productId") else null
    val unit = requiredString("unitCode")
    if (unit !in setOf("Liters", "Kg")) {
        throw BridgeProtocolFailure("validation_failed", "Choose Liters or Kg", "unitCode")
    }
    val ratePaisa = nullablePaisa("defaultSaleRatePaisaPerUnit")
    if (ratePaisa != null && ratePaisa <= 0L) {
        throw BridgeProtocolFailure(
            "validation_failed",
            "Default sale rate must be greater than zero",
            "defaultSaleRatePaisaPerUnit",
        )
    }
    return productId to ProductCatalogInput(
        name = requiredString("name"),
        unit = unit,
        defaultSaleRate = ratePaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
        inventoryTracking = requiredBoolean("inventoryTracking"),
        notes = nullableString("notes"),
    )
}

private fun strictPayload(payload: JSONObject, fields: Set<String>) =
    strictObject(payload, required = fields, allowed = fields)

private fun strictObject(value: JSONObject, required: Set<String>, allowed: Set<String>) {
    val actual = value.keys().asSequence().toSet()
    val missing = required - actual
    val unknown = actual - allowed
    if (missing.isNotEmpty()) {
        val field = missing.sorted().first()
        throw BridgeProtocolFailure("invalid_request", "Missing field: $field", field)
    }
    if (unknown.isNotEmpty()) {
        val field = unknown.sorted().first()
        throw BridgeProtocolFailure("invalid_request", "Unknown field: $field", field)
    }
}

private class BridgeProtocolFailure(
    val code: String,
    message: String,
    val field: String? = null,
) : IllegalArgumentException(message)

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject =
    put(name, value ?: JSONObject.NULL)

private fun jsonArray(values: Iterable<String>): JSONArray = JSONArray().apply {
    values.forEach(::put)
}

private fun <T> jsonArray(values: Iterable<T>, transform: (T) -> JSONObject): JSONArray = JSONArray().apply {
    values.forEach { put(transform(it)) }
}
