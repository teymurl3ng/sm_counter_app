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

    /** Refresh-only verdict: the server itself states the refresh token is
     *  dead (invalid_grant / refresh_token_not_found). Definitive — the
     *  session cannot recover; sign-in classification is unaffected. */
    data object RefreshTokenInvalid : AccountAuthResult

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
        // The server credential is the 15 digits — separators like the
        // 98828-87931-48384 display form are stripped, matching the DEV
        // probe script's wire behavior.
        val normalizedCredential = String(credential).filter(Char::isDigit)
        val body = JSONObject()
            .put("email", "${username.trim().lowercase()}@saafmilk.local")
            .put("password", normalizedCredential)
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
            return AccountAuthResult.Unknown(describeNetworkFailure(failure))
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

    /**
     * Stable detail for field diagnostics: exception type plus message, with
     * anything that could resemble a credential or token redacted by only
     * ever carrying the exception's own text (never request bodies).
     */
    private fun describeNetworkFailure(failure: IOException): String =
        "network:${failure.javaClass.simpleName}:${failure.message?.take(120) ?: "no detail"}"

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
            400 -> when (val code = refreshErrorCodeOf(response)) {
                CODE_INVALID_CREDENTIALS -> AccountAuthResult.InvalidCredentials
                CODE_INVALID_GRANT, CODE_REFRESH_TOKEN_NOT_FOUND ->
                    AccountAuthResult.RefreshTokenInvalid
                else -> {
                    val detail = code?.let { "_$it" } ?: ""
                    AccountAuthResult.Unknown("refresh_rejected$detail")
                }
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

    /**
     * Best-effort session termination; result is diagnostics only (probe P7).
     * scope=local: kill ONLY this device's refresh token — the Supabase
     * default scope is global, and both phones share the owner identity, so
     * a global logout from one phone would sever the other's sync session.
     */
    suspend fun logout(accessToken: String): Boolean = try {
        val response = http.execute(
            SyncHttpRequest(
                method = "POST",
                url = config.url("/auth/v1/logout?scope=local"),
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

    suspend fun verifySession(session: SyncTokenStore.Session): AccountAuthResult = verifyProfile(
        TokenSet(session.accessToken, session.refreshToken, session.accessTokenExpiresAtEpochMs),
    )

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
        val active = profile.opt("active") as? Boolean
            ?: return AccountAuthResult.Unknown("malformed_profile")
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
        const val CODE_INVALID_GRANT = "invalid_grant"
        const val CODE_REFRESH_TOKEN_NOT_FOUND = "refresh_token_not_found"
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
 * Refresh-only error code: Supabase's token endpoint reports dead refresh
 * tokens as a top-level STRING error (`{"error":"invalid_grant", ...}`) —
 * a stable code, not message text — which [stableErrorCodeOf] (built for
 * `error_code` / `error.code` objects) cannot see. Sign-in parsing is
 * untouched; only refresh classification reads this.
 */
internal fun refreshErrorCodeOf(response: SyncHttpResponse): String? =
    stableErrorCodeOf(response) ?: run {
        val text = response.bodyText()?.trim().orEmpty()
        if (text.isEmpty()) return null
        runCatching {
            (JSONObject(text).opt("error") as? String)?.takeIf { it.isNotBlank() }
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
