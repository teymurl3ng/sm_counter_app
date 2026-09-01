package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.SecurityFailure
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal object HybridBridgeActions {
    const val BRIDGE_INFO = "system.bridgeInfo"
    const val BOOTSTRAP_STATUS = "query.bootstrapStatus"
    const val BOOTSTRAP_OWNER = "command.auth.bootstrapOwner"
    const val LOGIN = "command.auth.login"
    const val LOGOUT = "command.auth.logout"
    const val REAUTHENTICATE = "command.auth.reauthenticate"
    const val CURRENT_SESSION = "query.currentSession"
    const val MASTER_DATA = "query.masterData"
    const val CREATE_PRODUCT = "command.createProduct"
    const val UPDATE_PRODUCT = "command.updateProduct"
    const val SET_PRODUCT_ARCHIVED = "command.setProductArchived"
    const val CREATE_CUSTOMER = "command.createCustomer"
    const val RENAME_CUSTOMER = "command.renameCustomer"
    const val SAVE_CUSTOMER_PROFILE = "command.saveCustomerProfile"
    const val CREATE_VENDOR = "command.createVendor"
    const val RENAME_VENDOR = "command.renameVendor"
    const val SAVE_VENDOR_PROFILE = "command.saveVendorProfile"
    const val TRANSACTION_REFERENCES = "query.transactionReferences"
    const val DASHBOARD = "query.dashboard"
    const val CASHIER_SUMMARY = "query.cashierSummary"
    const val FINANCIAL_OVERVIEW = "query.financialOverview"
    const val RECORD_PURCHASE = "command.recordPurchase"
    const val RECORD_SALE = "command.recordSale"
    const val RECORD_PRODUCTION = "command.recordProduction"
    const val RECORD_VENDOR_PAYMENT = "command.recordVendorPayment"
    const val RECORD_CREDIT_COLLECTION = "command.recordCreditCollection"
    const val RECORD_EXPENSE = "command.recordExpense"
    const val RECORD_SALARY_PAYMENT = "command.recordSalaryPayment"
    const val SPOILAGE_HISTORY = "query.spoilageHistory"
    const val RECORD_SPOILAGE = "command.recordSpoilage"
    const val DELIVERIES = "query.deliveries"
    const val CREATE_DELIVERY = "command.createDelivery"
    const val TRANSITION_DELIVERY = "command.transitionDelivery"
    const val LINK_DELIVERY_SALE = "command.linkDeliverySale"
    const val ARCHIVE_DELIVERY = "command.archiveDelivery"
    const val ACCOUNT_LEDGER = "query.accountLedger"
    const val EVENT_HISTORY = "query.eventHistory"
    const val EVENT_DETAIL = "query.eventDetail"
    const val CORRECT_EVENT = "command.correctEvent"
    const val ATTENDANCE = "query.attendance"
    const val CREATE_EMPLOYEE = "command.createEmployee"
    const val RENAME_EMPLOYEE = "command.renameEmployee"
    const val SAVE_EMPLOYEE_PROFILE = "command.saveEmployeeProfile"
    const val RECORD_ATTENDANCE = "command.recordAttendance"
    const val BUSINESS_SETTINGS = "query.businessSettings"
    const val SAVE_STORE_IDENTITY = "command.saveStoreIdentity"
    const val UPDATE_BUSINESS_SETTINGS = "command.updateBusinessSettings"
    const val CREATE_EXPENSE_CATEGORY = "command.createExpenseCategory"
    const val SET_EXPENSE_CATEGORY_ARCHIVED = "command.setExpenseCategoryArchived"
    const val RECORD_OPENING_STATE = "command.recordOpeningState"
    const val CORRECT_OPENING_STATE = "command.correctOpeningState"

    val authentication = setOf(BOOTSTRAP_STATUS, BOOTSTRAP_OWNER, LOGIN, LOGOUT, REAUTHENTICATE, CURRENT_SESSION)
    val masterData = setOf(
        MASTER_DATA,
        CREATE_PRODUCT,
        UPDATE_PRODUCT,
        SET_PRODUCT_ARCHIVED,
        CREATE_CUSTOMER,
        RENAME_CUSTOMER,
        SAVE_CUSTOMER_PROFILE,
        CREATE_VENDOR,
        RENAME_VENDOR,
        SAVE_VENDOR_PROFILE,
        TRANSACTION_REFERENCES,
    )
    val employees = setOf(ATTENDANCE, CREATE_EMPLOYEE, RENAME_EMPLOYEE, SAVE_EMPLOYEE_PROFILE, RECORD_ATTENDANCE)
    val settings = setOf(
        BUSINESS_SETTINGS,
        SAVE_STORE_IDENTITY,
        UPDATE_BUSINESS_SETTINGS,
        CREATE_EXPENSE_CATEGORY,
        SET_EXPENSE_CATEGORY_ARCHIVED,
        RECORD_OPENING_STATE,
        CORRECT_OPENING_STATE,
    )
    val operations = setOf(
        DASHBOARD,
        CASHIER_SUMMARY,
        FINANCIAL_OVERVIEW,
        RECORD_PURCHASE,
        RECORD_SALE,
        RECORD_PRODUCTION,
        RECORD_VENDOR_PAYMENT,
        RECORD_CREDIT_COLLECTION,
        RECORD_EXPENSE,
        RECORD_SALARY_PAYMENT,
        SPOILAGE_HISTORY,
        RECORD_SPOILAGE,
    )
    val deliveries = setOf(DELIVERIES, CREATE_DELIVERY, TRANSITION_DELIVERY, LINK_DELIVERY_SALE, ARCHIVE_DELIVERY)
    val ledgers = setOf(ACCOUNT_LEDGER)
    val history = setOf(EVENT_HISTORY, EVENT_DETAIL, CORRECT_EVENT)
    val supported = listOf(BRIDGE_INFO) + authentication + masterData + employees + settings + operations + deliveries + ledgers + history
    val authByAction = supported.associateWith { action ->
        if (action in setOf(BRIDGE_INFO, BOOTSTRAP_STATUS, BOOTSTRAP_OWNER, LOGIN)) "none" else "session"
    }
}

internal data class BridgeRequest(
    val requestId: String,
    val action: String,
    val sessionHandle: String?,
    val payload: JSONObject,
) {
    fun requireSession(): String = sessionHandle
        ?: throw BridgeProtocolFailure("unauthenticated", "Sign in to continue")
}

internal data class BridgeFailure(
    val code: String,
    val message: String,
    val field: String? = null,
    val retryable: Boolean = false,
)

internal fun parseBridgeRequest(envelope: JSONObject): BridgeRequest {
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
    if (action !in HybridBridgeActions.supported) {
        throw BridgeProtocolFailure("invalid_request", "This action is not available in the current app slice", "action")
    }
    val payload = envelope.get("payload") as? JSONObject
        ?: throw BridgeProtocolFailure("invalid_request", "payload must be an object", "payload")
    val session = if (envelope.has("sessionHandle") && !envelope.isNull("sessionHandle")) {
        envelope.requiredString("sessionHandle")
    } else null
    val auth = HybridBridgeActions.authByAction.getValue(action)
    if (auth == "session" && session == null) throw BridgeProtocolFailure("unauthenticated", "Sign in to continue")
    if (auth == "none" && envelope.has("sessionHandle")) {
        throw BridgeProtocolFailure("invalid_request", "sessionHandle is not accepted for this action", "sessionHandle")
    }
    return BridgeRequest(requestId, action, session, payload)
}

internal fun successEnvelope(request: BridgeRequest, data: Any): JSONObject = JSONObject()
    .put("contractVersion", BridgeContractSnapshot.VERSION)
    .put("requestId", request.requestId)
    .put("action", request.action)
    .put("ok", true)
    .put("data", data)

internal fun failureEnvelope(requestId: String, action: String, failure: BridgeFailure): JSONObject = JSONObject()
    .put("contractVersion", BridgeContractSnapshot.VERSION)
    .put("requestId", requestId)
    .put("action", action)
    .put("ok", false)
    .put("error", JSONObject().put("code", failure.code).put("message", failure.message).putNullable("field", failure.field).put("retryable", failure.retryable))

internal fun mapBridgeFailure(error: Throwable): BridgeFailure = when (error) {
    is BridgeProtocolFailure -> BridgeFailure(error.code, error.message ?: "The request is invalid", error.field)
    is BridgeCommandConflict -> BridgeFailure("conflict", error.message ?: "The current state no longer permits this action")
    is BridgeCommandNotFound -> BridgeFailure("not_found", error.message ?: "The requested record no longer exists")
    is BridgeCommandDayClosed -> BridgeFailure("day_closed", error.message ?: "This business day is closed")
    is BridgeCommandInsufficientStock -> BridgeFailure("insufficient_stock", error.message ?: "There is not enough stock for this operation")
    is SecurityFailure.Authentication -> BridgeFailure("authentication_failed", SecurityFailure.GENERIC_AUTH_FAILURE)
    is SecurityFailure.SessionExpired -> BridgeFailure("session_expired", "Your session expired. Sign in again.")
    is SecurityFailure.PermissionDenied -> BridgeFailure("permission_denied", "You do not have permission to perform this action.")
    is SecurityFailure.RecentAuthenticationRequired -> BridgeFailure("recent_authentication_required", "Enter your passphrase again to perform this sensitive action.")
    is SecurityFailure.Validation -> BridgeFailure("validation_failed", error.message ?: "Check the entered information")
    is JSONException -> BridgeFailure("invalid_request", "The request is not valid JSON")
    is IllegalArgumentException -> BridgeFailure("validation_failed", error.message ?: "Check the entered information")
    else -> BridgeFailure("internal_error", "The app could not complete this request")
}

internal fun JSONObject.requiredString(name: String): String {
    if (!has(name) || isNull(name) || get(name) !is String) throw BridgeProtocolFailure("invalid_request", "$name must be text", name)
    return getString(name)
}

internal fun JSONObject.requiredBoolean(name: String): Boolean {
    if (!has(name) || isNull(name) || get(name) !is Boolean) throw BridgeProtocolFailure("invalid_request", "$name must be true or false", name)
    return getBoolean(name)
}

internal fun JSONObject.nullableString(name: String): String? {
    if (!has(name)) throw BridgeProtocolFailure("invalid_request", "$name is required", name)
    if (isNull(name)) return null
    if (get(name) !is String) throw BridgeProtocolFailure("invalid_request", "$name must be text or null", name)
    return getString(name)
}

internal fun JSONObject.nullablePaisa(name: String): Long? {
    val raw = nullableString(name) ?: return null
    if (!Regex("-?(0|[1-9][0-9]*)").matches(raw)) throw BridgeProtocolFailure("validation_failed", "$name must be whole PKR paisa", name)
    return raw.toLongOrNull() ?: throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name)
}

internal fun JSONObject.requiredPaisa(name: String): Long = nullablePaisa(name)
    ?: throw BridgeProtocolFailure("invalid_request", "$name must be an integer paisa string", name)

internal fun JSONObject.requiredId(name: String): Long {
    val raw = requiredString(name)
    if (!Regex("[1-9][0-9]*").matches(raw)) throw BridgeProtocolFailure("validation_failed", "$name must be a positive record ID", name)
    return raw.toLongOrNull() ?: throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name)
}

internal fun JSONObject.nullableDecimal(name: String): BigDecimal? {
    val raw = nullableString(name) ?: return null
    if (!Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?").matches(raw)) throw BridgeProtocolFailure("validation_failed", "$name must be a decimal value", name)
    return runCatching { BigDecimal(raw) }.getOrElse { throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name) }
}

internal fun JSONObject.requiredDecimal(name: String): BigDecimal = nullableDecimal(name)
    ?: throw BridgeProtocolFailure("invalid_request", "$name must be a decimal string", name)

internal fun JSONObject.requiredDate(name: String): String {
    val value = requiredString(name)
    runCatching { LocalDate.parse(value) }.getOrElse {
        throw BridgeProtocolFailure("validation_failed", "$name must be a valid date", name)
    }
    return value
}

internal fun JSONObject.nullableDate(name: String): String? {
    val value = nullableString(name) ?: return null
    runCatching { LocalDate.parse(value) }.getOrElse {
        throw BridgeProtocolFailure("validation_failed", "$name must be a valid date", name)
    }
    return value
}

internal fun JSONObject.optionalString(name: String): String? {
    if (!has(name)) return null
    if (isNull(name) || get(name) !is String) throw BridgeProtocolFailure("invalid_request", "$name must be text", name)
    return getString(name)
}

internal fun JSONObject.productInput(includeProductId: Boolean): Pair<Long?, ProductCatalogInput> {
    val commonFields = setOf("name", "unitCode", "defaultSaleRatePaisaPerUnit", "inventoryTracking", "notes")
    strictPayload(this, if (includeProductId) commonFields + "productId" else commonFields)
    val productId = if (includeProductId) requiredId("productId") else null
    val unit = requiredString("unitCode")
    if (unit !in setOf("Liters", "Kg")) throw BridgeProtocolFailure("validation_failed", "Choose Liters or Kg", "unitCode")
    val ratePaisa = nullablePaisa("defaultSaleRatePaisaPerUnit")
    if (ratePaisa != null && ratePaisa <= 0L) throw BridgeProtocolFailure("validation_failed", "Default sale rate must be greater than zero", "defaultSaleRatePaisaPerUnit")
    return productId to ProductCatalogInput(
        name = requiredString("name"),
        unit = unit,
        defaultSaleRate = ratePaisa?.let { BigDecimal(it).movePointLeft(2).toDouble() },
        inventoryTracking = requiredBoolean("inventoryTracking"),
        notes = nullableString("notes"),
    )
}

internal fun strictPayload(payload: JSONObject, fields: Set<String>) = strictObject(payload, required = fields, allowed = fields)

internal fun strictObject(value: JSONObject, required: Set<String>, allowed: Set<String>) {
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

internal class BridgeProtocolFailure(val code: String, message: String, val field: String? = null) : IllegalArgumentException(message)

internal fun unavailableBridgeAction(): Nothing =
    throw BridgeProtocolFailure("invalid_request", "This action is not available in the current app slice")

internal fun JSONObject.putNullable(name: String, value: Any?): JSONObject = put(name, value ?: JSONObject.NULL)

internal fun jsonArray(values: Iterable<String>): JSONArray = JSONArray().apply { values.forEach(::put) }

internal fun <T> jsonArray(values: Iterable<T>, transform: (T) -> JSONObject): JSONArray = JSONArray().apply { values.forEach { put(transform(it)) } }
