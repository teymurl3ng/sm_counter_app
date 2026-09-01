package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.domain.CreateUserInput
import org.json.JSONObject

internal class SecurityAdministrationBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: SecurityAdministrationBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.USERS -> users(request)
        HybridBridgeActions.SECURITY_AUDIT -> audit(request)
        HybridBridgeActions.BEGIN_USER_ENROLLMENT -> beginEnrollment(request)
        HybridBridgeActions.RESUME_USER_ENROLLMENT -> resumeEnrollment(request)
        HybridBridgeActions.COMPLETE_ENROLLMENT -> completeEnrollment(request)
        HybridBridgeActions.CHANGE_USER_ROLE -> changeRole(request)
        HybridBridgeActions.SET_USER_ENABLED -> setEnabled(request)
        HybridBridgeActions.BEGIN_CREDENTIAL_RESET -> beginReset(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun users(request: BridgeRequest): Any {
        strictPayload(request.payload, emptySet())
        return jsonArray(reads.users(request.requireSession()), ::userAdministrationJson)
    }

    private suspend fun audit(request: BridgeRequest): Any {
        strictPayload(request.payload, emptySet())
        return jsonArray(reads.securityAudit(request.requireSession()), ::securityAuditJson)
    }

    private suspend fun beginEnrollment(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("loginName", "displayName", "roleCode", "employeeAccountId"))
        val result = commands.beginEnrollment(request.requireSession(), CreateUserInput(
            loginName = request.payload.requiredString("loginName"),
            displayName = request.payload.requiredString("displayName"),
            role = request.payload.requiredString("roleCode"),
            employeeAccountId = request.payload.nullableIdValue("employeeAccountId"),
        ))
        return enrollmentJson(result.user.userId, result.enrollmentToken)
    }

    private suspend fun resumeEnrollment(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("targetUserId"))
        val result = commands.resumeEnrollment(request.requireSession(), request.payload.requiredId("targetUserId"))
        return enrollmentJson(result.user.userId, result.enrollmentToken)
    }

    private suspend fun completeEnrollment(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("enrollmentToken", "passphrase"))
        val passphrase = request.payload.requiredString("passphrase").toCharArray()
        return try {
            userAdministrationJson(commands.completeEnrollment(request.payload.requiredString("enrollmentToken"), passphrase))
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun changeRole(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("targetUserId", "roleCode"))
        commands.changeRole(request.requireSession(), request.payload.requiredId("targetUserId"), request.payload.requiredString("roleCode"))
        return JSONObject()
    }

    private suspend fun setEnabled(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("targetUserId", "enabled"))
        commands.setEnabled(request.requireSession(), request.payload.requiredId("targetUserId"), request.payload.requiredBoolean("enabled"))
        return JSONObject()
    }

    private suspend fun beginReset(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("targetUserId"))
        val result = commands.beginCredentialReset(request.requireSession(), request.payload.requiredId("targetUserId"))
        return enrollmentJson(result.user.userId, result.enrollmentToken)
    }
}

private fun enrollmentJson(userId: Long, token: String) = JSONObject()
    .put("targetUserId", userId.toString())
    .put("enrollmentToken", token)

private fun JSONObject.nullableIdValue(name: String): Long? {
    val raw = nullableString(name) ?: return null
    if (!Regex("[1-9][0-9]*").matches(raw)) throw BridgeProtocolFailure("validation_failed", "$name must be a positive record ID", name)
    return raw.toLongOrNull() ?: throw BridgeProtocolFailure("validation_failed", "$name is outside the supported range", name)
}
