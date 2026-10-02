package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * The production push transport (sync Phase 4): POSTs the engine-built
 * envelopes to the Account Server's `sync_push` RPC (added by sync Phase 5).
 * Outcome mapping feeds the Phase-3 engine directly:
 *  - 2xx → Success (rows may leave the queue);
 *  - network/429/5xx/401 → Transient (retry later; 401 means the session
 *    needs refresh or re-enrollment, never a data verdict);
 *  - 403/422 → Permanent with the server's stable reason code (or a status
 *    fallback) — the engine halts at the offending row and alerts the owner.
 * Envelope construction and orphan handling live in the engine (C1 ack
 * equality re-serializes from the live rows). Every 2xx feeds the
 * trusted-time anchor.
 */
class SyncServerClient(
    private val http: SyncHttp,
    private val config: SyncServerConfig,
    private val database: MilkShopDatabase,
    private val tokenStore: SyncTokenStore,
    /** The enrolled shop's server id, sent with every push (wired at activation). */
    private val shopIdProvider: (() -> String?)? = null,
) : SyncPushTransport {

    override suspend fun push(envelopes: List<JSONObject>, rows: List<SyncOutboxEntity>): SyncPushResult {
        val session = tokenStore.load() ?: return SyncPushResult.Transient("no_session")
        if (envelopes.isEmpty()) return SyncPushResult.Success(0)

        val body = JSONObject()
            .put("device_id", SyncIdentity.currentOrNull())
            .put("envelopes", JSONArray(envelopes))
            // C1 true-conflict detection: the server compares each existing
            // row's server_seq against this cursor — a pusher overwriting a
            // version it never saw is a conflict; one re-pushing its own
            // echo is not. Pre-migration servers ignore the extra key.
            .put("pull_cursor", database.syncPullStateDao().find()?.lastServerSeq ?: 0L)
            .apply {
                shopIdProvider?.invoke()?.takeIf { it.isNotEmpty() }?.let { shopId -> put("shop_id", shopId) }
            }
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

        if (response.status !in 200..299) {
            return when (response.status) {
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

        // The RPC answers 200 with an ok flag; a refused batch carries a
        // stable error code the engine halts on (e.g. a cashier pushing a
        // correction — correction_privilege_required).
        anchorFrom(response)
        val text = response.bodyText()?.trim().orEmpty()
        val json = if (text.isNotEmpty()) runCatching { JSONObject(text) }.getOrNull() else null
        if (json != null && json.optBoolean("ok", true) == false) {
            val error = json.optJSONObject("error")
            return SyncPushResult.Permanent(
                reasonCode = error?.optString("code")?.takeIf { it.isNotEmpty() } ?: "server_rejected",
                message = error?.optString("message")?.takeIf { it.isNotEmpty() } ?: "Server refused the batch",
            )
        }
        return SyncPushResult.Success(envelopes.size)
    }
}
