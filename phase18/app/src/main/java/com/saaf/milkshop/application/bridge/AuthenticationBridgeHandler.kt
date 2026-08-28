package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.domain.UserSecurityService
import org.json.JSONObject

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
        HybridBridgeActions.REAUTHENTICATE -> reauthenticate(request)
        HybridBridgeActions.CURRENT_SESSION -> currentSession(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun bootstrapOwner(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("loginName", "displayName", "passphrase"))
        if (!security.needsOwnerBootstrap()) throw BridgeCommandConflict("Owner setup has already been completed")
        val passphrase = request.payload.requiredString("passphrase").toCharArray()
        return try {
            val result = security.bootstrapOwner(
                request.payload.requiredString("loginName"),
                request.payload.requiredString("displayName"),
                passphrase,
            )
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
            JSONObject().put("sessionHandle", session.sessionId)
                .put("currentSession", currentSessionJson(reads.currentSession(session.sessionId)))
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

    private suspend fun reauthenticate(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("passphrase"))
        val passphrase = request.payload.requiredString("passphrase").toCharArray()
        return try {
            security.reauthenticate(request.requireSession(), passphrase)
            JSONObject()
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun currentSession(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        return currentSessionJson(reads.currentSession(request.requireSession()))
    }
}
