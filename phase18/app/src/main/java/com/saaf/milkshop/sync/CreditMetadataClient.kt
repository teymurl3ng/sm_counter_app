package com.saaf.milkshop.sync

import org.json.JSONObject
import java.io.IOException

internal sealed interface CreditRpcResult {
    data class Ok(val body: JSONObject) : CreditRpcResult
    data class Retry(val reason: String) : CreditRpcResult
    data class Refused(val code: String) : CreditRpcResult
    data object Unsupported : CreditRpcResult
}

/** Uses the existing redacted HTTP seam; refresh belongs to the runtime's process-wide gate. */
internal class CreditMetadataClient(private val http: SyncHttp, private val config: SyncServerConfig,
    private val tokens: SyncTokenStore) {
    suspend fun call(name: String, body: JSONObject): CreditRpcResult {
        require(name in setOf("credit_contract_status_v1", "credit_metadata_pull_v1", "credit_metadata_push_v1"))
        val session = tokens.load() ?: return CreditRpcResult.Retry("no_session")
        val response = try {
            http.execute(SyncHttpRequest("POST", config.url("/rest/v1/rpc/$name"), mapOf(
                AccountAuthClient.HEADER_APIKEY to config.publishableKey,
                AccountAuthClient.HEADER_AUTHORIZATION to "Bearer ${session.accessToken}",
                "Content-Type" to "application/json"), body.toString().toByteArray(Charsets.UTF_8)))
        } catch (_: IOException) { return CreditRpcResult.Retry("network") }
        if (response.status == 401) return CreditRpcResult.Retry("unauthorized")
        if (response.status == 404) return CreditRpcResult.Unsupported
        if (response.status in setOf(403, 422)) return CreditRpcResult.Refused(stableErrorCodeOf(response) ?: "credit_refused_${response.status}")
        if (response.status !in 200..299) return CreditRpcResult.Retry("http_${response.status}")
        val json = runCatching { JSONObject(response.bodyText().orEmpty()) }.getOrNull()
            ?: return CreditRpcResult.Retry("malformed_response")
        if (json.opt("ok") !is Boolean) return CreditRpcResult.Retry("malformed_response")
        anchorFrom(response)
        return if (json.getBoolean("ok")) CreditRpcResult.Ok(json) else CreditRpcResult.Refused(
            json.optJSONObject("error")?.optString("code")?.takeIf { it.isNotBlank() } ?: "credit_rejected")
    }
}
