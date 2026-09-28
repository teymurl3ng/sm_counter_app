package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * The production push transport (sync Phase 4): builds envelopes for queued
 * rows and POSTs them to the Account Server's `sync_push` RPC (added by sync
 * Phase 5). Outcome mapping feeds the Phase-3 engine directly:
 *  - 2xx → Success (rows may leave the queue);
 *  - network/429/5xx/401 → Transient (retry later; 401 means the session
 *    needs refresh or re-enrollment, never a data verdict);
 *  - 403/422 → Permanent with the server's stable reason code (or a status
 *    fallback) — the engine halts at the offending row and alerts the owner.
 * Outbox rows whose local record has vanished are dropped: there is nothing
 * to push for them and their queue slots must not block dependents.
 * Every 2xx feeds the trusted-time anchor.
 */
class SyncServerClient(
    private val http: SyncHttp,
    private val config: SyncServerConfig,
    private val database: MilkShopDatabase,
    private val tokenStore: SyncTokenStore,
) : SyncPushTransport {

    override suspend fun push(rows: List<SyncOutboxEntity>): SyncPushResult {
        val session = tokenStore.load() ?: return SyncPushResult.Transient("no_session")

        val build = SyncEnvelopeFactory(database).build(rows)
        if (build.skippedRows.isNotEmpty()) {
            database.syncOutboxDao().deleteByIds(build.skippedRows.map { it.outboxId })
        }
        if (build.envelopes.isEmpty()) return SyncPushResult.Success(0)

        val body = JSONObject()
            .put("device_id", SyncIdentity.currentOrNull())
            .put("envelopes", JSONArray(build.envelopes))
            .toString()
            .toByteArray(Charsets.UTF_8)

        val response = try {
            http.execute(
                SyncHttpRequest(
                    method = "POST",
                    url = config.url("/rest/v1/rpc/sync_push"),
                    headers = mapOf(
                        AccountAuthClient.HEADER_APIKEY to config.publishableKey,
                        AccountAuthClient.HEADER_AUTHORIZATION to "Bearer ${session.accessToken}",
                        "Content-Type" to "application/json",
                    ),
                    body = body,
                ),
            )
        } catch (failure: IOException) {
            return SyncPushResult.Transient("network")
        }

        return when (response.status) {
            in 200..299 -> {
                anchorFrom(response)
                SyncPushResult.Success(build.envelopes.size)
            }
            401 -> SyncPushResult.Transient("unauthorized")
            403, 422 -> {
                anchorFrom(response)
                val code = stableErrorCodeOf(response)
                SyncPushResult.Permanent(
                    reasonCode = code ?: "rejected_${response.status}",
                    message = "Server refused a queued record",
                )
            }
            429 -> SyncPushResult.Transient("rate_limited")
            in 500..599 -> SyncPushResult.Transient("server_${response.status}")
            else -> SyncPushResult.Transient("http_${response.status}")
        }
    }
}
