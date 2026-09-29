package com.saaf.milkshop.sync

import org.json.JSONObject
import java.io.IOException

/**
 * One synced record as the server stores it: the wire envelope plus the
 * server-assigned sequence (the ordering authority across devices).
 */
data class SyncPulledRecord(
    val serverSeq: Long,
    val shopId: String?,
    val tableName: String,
    val rowUid: String,
    val payload: JSONObject,
    val originDevice: String?,
)

data class SyncPullBatch(
    val records: List<SyncPulledRecord>,
    val latestServerSeq: Long,
)

sealed interface SyncPullOutcome {
    data class Fetched(val batch: SyncPullBatch, val hasMore: Boolean) : SyncPullOutcome
    data object NotConfigured : SyncPullOutcome
    data class RetryLater(val reason: String) : SyncPullOutcome
}

/**
 * Calls the server's sync_pull RPC and parses the batch. Every response
 * anchors trusted time. A network/5xx outcome is retryable; auth trouble
 * surfaces as retryable "unauthorized" so the runtime can refresh.
 */
class SyncPullClient(
    private val http: SyncHttp,
    private val config: SyncServerConfig,
    private val tokenStore: SyncTokenStore,
) {

    suspend fun pull(
        sinceSeq: Long,
        maxRows: Int = 200,
        shopId: String? = null,
    ): SyncPullOutcome {
        val session = tokenStore.load() ?: return SyncPullOutcome.NotConfigured
        val body = JSONObject()
            .put("since_seq", sinceSeq)
            .put("max_rows", maxRows)
            .apply { shopId?.let { put("shop", it) } }
            .toString()
            .toByteArray(Charsets.UTF_8)

        val response = try {
            http.execute(
                SyncHttpRequest(
                    method = "POST",
                    url = config.url("/rest/v1/rpc/sync_pull"),
                    headers = mapOf(
                        AccountAuthClient.HEADER_APIKEY to config.publishableKey,
                        AccountAuthClient.HEADER_AUTHORIZATION to "Bearer ${session.accessToken}",
                        "Content-Type" to "application/json",
                    ),
                    body = body,
                ),
            )
        } catch (failure: IOException) {
            return SyncPullOutcome.RetryLater("network")
        }

        return when (response.status) {
            in 200..299 -> {
                anchorFrom(response)
                val text = response.bodyText()?.trim().orEmpty()
                if (text.isEmpty()) return SyncPullOutcome.RetryLater("malformed_response")
                val json = runCatching { JSONObject(text) }.getOrNull()
                    ?: return SyncPullOutcome.RetryLater("malformed_response")
                val array = json.optJSONArray("records") ?: return SyncPullOutcome.RetryLater("malformed_response")
                val records = (0 until array.length()).mapNotNull { index ->
                    val record = array.optJSONObject(index) ?: return@mapNotNull null
                    SyncPulledRecord(
                        serverSeq = record.getLong("server_seq"),
                        shopId = record.optString("shop_id").takeIf { it.isNotEmpty() },
                        tableName = record.getString("table"),
                        rowUid = record.getString("uid"),
                        payload = record.getJSONObject("payload"),
                        originDevice = record.optString("origin_device").takeIf { it.isNotEmpty() },
                    )
                }
                SyncPullOutcome.Fetched(
                    batch = SyncPullBatch(records, records.maxOfOrNull { it.serverSeq } ?: sinceSeq),
                    hasMore = records.size >= maxRows,
                )
            }
            401 -> SyncPullOutcome.RetryLater("unauthorized")
            429 -> SyncPullOutcome.RetryLater("rate_limited")
            in 500..599 -> SyncPullOutcome.RetryLater("server_${response.status}")
            else -> SyncPullOutcome.RetryLater("http_${response.status}")
        }
    }
}
