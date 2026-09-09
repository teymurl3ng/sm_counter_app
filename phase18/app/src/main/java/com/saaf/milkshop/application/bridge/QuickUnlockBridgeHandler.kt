package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.quickunlock.QuickUnlockMethod
import com.saaf.milkshop.quickunlock.QuickUnlockService
import com.saaf.milkshop.quickunlock.QuickUnlockStatus
import org.json.JSONArray
import org.json.JSONObject

internal class QuickUnlockBridgeHandler(
    private val reads: HybridReadModelService,
    private val quickUnlock: QuickUnlockService,
    private val onSessionChanged: (String?) -> Unit,
) {
    suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.QUICK_UNLOCK_STATUS -> status(request)
        HybridBridgeActions.ENROLL_QUICK_UNLOCK -> enroll(request)
        HybridBridgeActions.REMOVE_QUICK_UNLOCK -> remove(request)
        HybridBridgeActions.QUICK_LOGIN -> login(request)
        HybridBridgeActions.QUICK_REAUTHENTICATE -> reauthenticate(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun status(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        return statusJson(quickUnlock.status())
    }

    private suspend fun enroll(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("currentPassphrase", "enableDeviceAuthentication", "pin"))
        val passphrase = request.payload.requiredString("currentPassphrase").toCharArray()
        val pin = request.payload.nullableString("pin")?.toCharArray()
        return try {
            statusJson(
                quickUnlock.enroll(
                    sessionId = request.requireSession(),
                    currentPassphrase = passphrase,
                    enableDeviceAuthentication = request.payload.requiredBoolean("enableDeviceAuthentication"),
                    pin = pin,
                ),
            )
        } finally {
            passphrase.fill('\u0000')
            pin?.fill('\u0000')
        }
    }

    private suspend fun remove(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("currentPassphrase"))
        val passphrase = request.payload.requiredString("currentPassphrase").toCharArray()
        return try {
            statusJson(quickUnlock.remove(request.requireSession(), passphrase))
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private suspend fun login(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("loginName", "method", "pin"))
        val pin = request.payload.nullableString("pin")?.toCharArray()
        return try {
            val session = quickUnlock.login(
                loginName = request.payload.requiredString("loginName"),
                method = QuickUnlockMethod.parse(request.payload.requiredString("method")),
                pin = pin,
            )
            onSessionChanged(session.sessionId)
            JSONObject()
                .put("sessionHandle", session.sessionId)
                .put("currentSession", currentSessionJson(reads.currentSession(session.sessionId)))
        } finally {
            pin?.fill('\u0000')
        }
    }

    private suspend fun reauthenticate(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("method", "pin"))
        val pin = request.payload.nullableString("pin")?.toCharArray()
        return try {
            quickUnlock.reauthenticate(
                sessionId = request.requireSession(),
                method = QuickUnlockMethod.parse(request.payload.requiredString("method")),
                pin = pin,
            )
            JSONObject()
        } finally {
            pin?.fill('\u0000')
        }
    }

    private fun statusJson(status: QuickUnlockStatus) = JSONObject()
        .put("deviceAuthenticationAvailable", status.deviceAuthenticationAvailable)
        .put("deviceAuthenticationLabel", status.deviceAuthenticationLabel)
        .put("accounts", JSONArray().apply {
            status.accounts.forEach { account ->
                put(
                    JSONObject()
                        .put("loginName", account.loginName)
                        .put("displayName", account.displayName)
                        .put("deviceAuthenticationEnabled", account.deviceAuthenticationEnabled)
                        .put("pinEnabled", account.pinEnabled),
                )
            }
        })
}
