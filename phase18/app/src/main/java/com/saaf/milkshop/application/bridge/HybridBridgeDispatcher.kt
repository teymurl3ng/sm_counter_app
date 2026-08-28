package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class HybridBridgeDispatcher(
    database: MilkShopDatabase,
    security: com.saaf.milkshop.domain.UserSecurityService,
    onSessionChanged: (String?) -> Unit = {},
) {
    private val reads = HybridReadModelService(database, security)
    private val authentication = AuthenticationBridgeHandler(reads, security, onSessionChanged)
    private val masterData = MasterDataBridgeHandler(reads, HybridCommandService(database, security))
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
            val request = parseBridgeRequest(envelope)
            requestId = request.requestId
            action = request.action
            if (!inFlightRequestIds.add(requestId)) throw BridgeCommandConflict("A request with this ID is already running")
            acceptedRequestId = requestId
            successEnvelope(request, execute(request)).toString()
        } catch (error: Throwable) {
            failureEnvelope(requestId, action, mapBridgeFailure(error)).toString()
        } finally {
            acceptedRequestId?.let(inFlightRequestIds::remove)
        }
    }

    private suspend fun execute(request: BridgeRequest): JSONObject = when (request.action) {
        HybridBridgeActions.BRIDGE_INFO -> JSONObject()
            .put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("contractSha256", BridgeContractSnapshot.CONTRACT_SHA256)
            .put("supportedActions", jsonArray(HybridBridgeActions.supported))
        in HybridBridgeActions.authentication -> authentication.execute(request)
        in HybridBridgeActions.masterData -> masterData.execute(request)
        else -> throw BridgeProtocolFailure("invalid_request", "This action is not available in the current app slice")
    }

    companion object {
        const val MAX_MESSAGE_BYTES = 1024 * 1024
        val SUPPORTED_ACTIONS: List<String> get() = HybridBridgeActions.supported
    }
}
