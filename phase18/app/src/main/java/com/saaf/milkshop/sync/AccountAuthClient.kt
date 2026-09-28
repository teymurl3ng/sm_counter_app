package com.saaf.milkshop.sync

import org.json.JSONObject
import java.io.IOException

/**
 * The Account Server's five-way authentication model (Account Bridge spec
 * §5, validated against DEV 2026-09-25). Network failures, rate limits, and
 * malformed bodies are always UNKNOWN — never an identity decision — and
 * an UNKNOWN never changes stored state.
 */
sealed interface AccountAuthResult {

    /** Auth and an active staff profile are definitive. */
    data class Allowed(
        val accessToken: String,
        val refreshToken: String?,
        val accessTokenExpiresAtEpochMs: Long?,
        val staffId: String,
        val role: String,
        val displayName: String?,
        val businessId: String,
        val shops: List<AuthShop>,
        val shopTimezone: String?,
    ) : AccountAuthResult

    /** Auth succeeds; the staff profile is definitively inactive. */
    data object Disabled : AccountAuthResult

    /** Auth explicitly returned the stable invalid-credential code. */
    data object InvalidCredentials : AccountAuthResult

    /** Auth identity has no staff profile. */
    data object NotAuthorized : AccountAuthResult

    /** Timeout, TLS/DNS, 429, 5xx, malformed/unexpected — preserve state. */
    data class Unknown(val reason: String) : AccountAuthResult
}

data class AuthShop(
    val shopId: String,
    val shopName: String?,
    val timezone: String?,
)

/**
 * Typed client for the frozen public auth contract:
 *  - `apikey` on every call; the publishable key is a Bearer only for
 *    sign-in and ping; a real user token is the Bearer for my_profile and
 *    logout (DEV probe P4 proved the boundary).
 *  - classification keys on HTTP status + the stable `error_code`, never on
 *    message text; `invalid_credentials` is the only credential verdict.
 *  - every 2xx feeds the trusted-time anchor (body `server_time_epoch_ms`
 *    first, HTTP Date header as fallback).
 */
class AccountAuthClient(
    private val http: SyncHttp,
    private val config: SyncServerConfig,
) {

    suspend fun signIn(username: String, credential: CharArray): AccountAuthResult {
        val body = JSONObject()
            .put("email", "${username.trim().lowercase()}@saafmilk.local")
            .put("password", String(credential))
            .toString()
            .toByteArray(Charsets.UTF_8)

        val response = try {
            http.execute(
                SyncHttpRequest(
                    method = "POST",
                    url = config.url("/auth/v1/token?grant_type=password"),
                    headers = mapOf(
                        HEADER_APIKEY to config.publishableKey,
                        HEADER_AUTHORIZATION to "Bearer ${config.publishableKey}",
                        "Content-Type" to "application/json",
                    ),
                    body = body,
                ),
            )
        } catch (failure: IOException) {
            return AccountAuthResult.Unknown("network")
        }

        return when (response.status) {
            200 -> onSignedIn(response)
            400 -> {
                anchorFrom(response)
                when (stableErrorCode(response)) {
                    CODE_INVALID_CREDENTIALS -> AccountAuthResult.InvalidCredentials
                    else -> AccountAuthResult.Unknown("sign_in_rejected_${stableErrorCode(response) ?: "no_code"}")
                }
            }
            429 -> AccountAuthResult.Unknown("rate_limited")
            in 500..599 -> AccountAuthResult.Unknown("server_${response.status}")
            else -> AccountAuthResult.Unknown("sign_in_${response.status}")
        }
    }

    suspend fun refresh(refreshToken: String): AccountAuthResult {
        val body = JSONObject()
            .put("refresh_token", refreshToken)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val response = try {
            http.execute(
                SyncHttpRequest(
                    method = "POST",
                    url = config.url("/auth/v1/token?grant_type=refresh_token"),
                    headers = mapOf(
                        HEADER_APIKEY to config.publishableKey,
                        HEADER_AUTHORIZATION to "Bearer ${config.publishableKey}",
                        "Content-Type" to "application/json",
                    ),
                    body = body,
                ),
            )
        } catch (failure: IOException) {
            return AccountAuthResult.Unknown("network")
        }
        return when (response.status) {
            200 -> parseTokenResponse(response)?.let { tokens ->
                // A refreshed token still needs a definitive profile check.
                verifyProfile(tokens)
            } ?: AccountAuthResult.Unknown("malformed_token_response")
            400 -> when (stableErrorCode(response)) {
                CODE_INVALID_CREDENTIALS -> AccountAuthResult.InvalidCredentials
                else -> AccountAuthResult.Unknown("refresh_rejected")
            }
            429 -> AccountAuthResult.Unknown("rate_limited")
            in 500..599 -> AccountAuthResult.Unknown("server_${response.status}")
            else -> AccountAuthResult.Unknown("refresh_${response.status}")
        }
    }

    suspend fun ping(): Boolean = try {
        val response = http.execute(
            SyncHttpRequest(
                method = "POST",
                url = config.url("/rest/v1/rpc/ping"),
                headers = mapOf(
                    HEADER_APIKEY to config.publishableKey,
                    HEADER_AUTHORIZATION to "Bearer ${config.publishableKey}",
                ),
            ),
        )
        anchorFrom(response)
        response.status == 200
    } catch (failure: IOException) {
        false
    }

    /** Best-effort session termination; result is diagnostics only (probe P7). */
    suspend fun logout(accessToken: String): Boolean = try {
        val response = http.execute(
            SyncHttpRequest(
                method = "POST",
                url = config.url("/auth/v1/logout"),
                headers = mapOf(
                    HEADER_APIKEY to config.publishableKey,
                    HEADER_AUTHORIZATION to "Bearer $accessToken",
                ),
            ),
        )
        anchorFrom(response)
        response.status in 200..299
    } catch (failure: IOException) {
        false
    }

    private suspend fun onSignedIn(tokenResponse: SyncHttpResponse): AccountAuthResult {
        val tokens = parseTokenResponse(tokenResponse) ?: return AccountAuthResult.Unknown("malformed_token_response")
        return verifyProfile(tokens)
    }

    private suspend fun verifyProfile(tokens: TokenSet): AccountAuthResult {
        val response = try {
            http.execute(
                SyncHttpRequest(
                    method = "POST",
                    url = config.url("/rest/v1/rpc/my_profile"),
                    headers = mapOf(
                        HEADER_APIKEY to config.publishableKey,
                        HEADER_AUTHORIZATION to "Bearer ${tokens.accessToken}",
                    ),
                ),
            )
        } catch (failure: IOException) {
            return AccountAuthResult.Unknown("network")
        }
        if (response.status != 200) {
            return when (response.status) {
                401 -> AccountAuthResult.Unknown("profile_unauthorized")
                429 -> AccountAuthResult.Unknown("rate_limited")
                in 500..599 -> AccountAuthResult.Unknown("server_${response.status}")
                else -> AccountAuthResult.Unknown("profile_${response.status}")
            }
        }
        anchorFrom(response)
        val text = response.bodyText()?.trim().orEmpty()
        if (text.isEmpty() || text == "null") return AccountAuthResult.NotAuthorized
        val profile = runCatching { JSONObject(text) }.getOrNull()
            ?: return AccountAuthResult.Unknown("malformed_profile")
        val staffId = profile.optString("staff_id").takeIf { it.isNotEmpty() }
            ?: return AccountAuthResult.Unknown("malformed_profile")
        val active = profile.optBoolean("active", true)
        val terminated = !profile.isNull("terminated_at") && profile.optString("terminated_at").isNotEmpty()
        if (!active || terminated) return AccountAuthResult.Disabled

        val shops = profile.optJSONArray("shops")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val shop = array.optJSONObject(index) ?: return@mapNotNull null
                val shopId = shop.optString("shop_id").takeIf { it.isNotEmpty() }
                    ?: shop.optString("id").takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                AuthShop(
                    shopId = shopId,
                    shopName = shop.optString("shop_name").takeIf { it.isNotEmpty() }
                        ?: shop.optString("name").takeIf { it.isNotEmpty() },
                    timezone = shop.optString("shop_timezone").takeIf { it.isNotEmpty() }
                        ?: shop.optString("timezone").takeIf { it.isNotEmpty() },
                )
            }
        } ?: emptyList()

        return AccountAuthResult.Allowed(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            accessTokenExpiresAtEpochMs = tokens.expiresAtEpochMs,
            staffId = staffId,
            role = profile.optString("role"),
            displayName = profile.optString("display_name").takeIf { it.isNotEmpty() },
            businessId = profile.optString("business_id"),
            shops = shops,
            // The frozen profile fields carry no top-level timezone (DEV probe
            // P2); a per-shop timezone is accepted, else the zone stays unset
            // until the server contract adds one (sync Phase 5 amendment).
            shopTimezone = profile.optString("shop_timezone").takeIf { it.isNotEmpty() }
                ?: shops.firstOrNull()?.timezone,
        )
    }

    private data class TokenSet(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAtEpochMs: Long?,
    )

    private fun parseTokenResponse(response: SyncHttpResponse): TokenSet? {
        anchorFrom(response)
        val text = response.bodyText()?.trim().orEmpty()
        if (text.isEmpty()) return null
        return runCatching {
            val json = JSONObject(text)
            val access = json.getString("access_token")
            val refresh = json.optString("refresh_token").takeIf { it.isNotEmpty() }
            val expiresIn = json.optLong("expires_in", -1L)
            val expiresAt = if (expiresIn > 0) {
                (serverTimeEpochMs(response) ?: System.currentTimeMillis()) + expiresIn * 1000
            } else {
                null
            }
            TokenSet(access, refresh, expiresAt)
        }.getOrNull()
    }

    private fun stableErrorCode(response: SyncHttpResponse): String? = stableErrorCodeOf(response)

    companion object {
        const val HEADER_APIKEY = "apikey"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val CODE_INVALID_CREDENTIALS = "invalid_credentials"
    }
}

/** Stable server error code: `error_code` at the top level or `error.code`; never message text. */
internal fun stableErrorCodeOf(response: SyncHttpResponse): String? {
    val text = response.bodyText()?.trim().orEmpty()
    if (text.isEmpty()) return null
    return runCatching {
        val json = JSONObject(text)
        json.optString("error_code").takeIf { it.isNotEmpty() }
            ?: json.optJSONObject("error")?.optString("code")?.takeIf { it.isNotEmpty() }
    }.getOrNull()
}

/**
 * Feeds [TrustedTime] from any server response: an explicit
 * `server_time_epoch_ms` body field wins (set by the sync RPCs), the
 * standard HTTP Date header is the fallback. No-ops when neither is usable.
 */
internal fun anchorFrom(response: SyncHttpResponse, bodyJson: JSONObject? = null) {
    val explicit = bodyJson?.optLong("server_time_epoch_ms", -1L)?.takeIf { it > 0 }
    if (explicit != null) {
        TrustedTime.anchor(explicit)
        return
    }
    val epochMs = serverTimeEpochMs(response) ?: return
    TrustedTime.anchor(epochMs)
}

internal fun serverTimeEpochMs(response: SyncHttpResponse): Long? {
    val text = response.bodyText()?.trim().orEmpty()
    if (text.isNotEmpty()) {
        val fromBody = runCatching {
            JSONObject(text).optLong("server_time_epoch_ms", -1L)
        }.getOrNull()
        if (fromBody != null && fromBody > 0) return fromBody
        // The ping RPC answers with a bare timestamptz string.
        val bare = runCatching { java.time.Instant.parse(text) }.getOrNull()
        if (bare != null) return bare.toEpochMilli()
    }
    return response.dateEpochMs
}
