package com.saaf.milkshop.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM tests for the H1 refresh-session safety (audit 2026-10-02): only a
 * definitive verdict may clear the session; UNKNOWN (and everything else
 * unrecognized) retries. Covers the disposition mapping and the refresh-only
 * wire parsing that recognizes Supabase's top-level string error form.
 */
class RefreshDispositionTest {

    @Test
    fun definitiveResults_clear() {
        assertEquals(RefreshDisposition.CLEAR, RefreshDisposition.from(AccountAuthResult.Disabled))
        assertEquals(RefreshDisposition.CLEAR, RefreshDisposition.from(AccountAuthResult.InvalidCredentials))
        assertEquals(RefreshDisposition.CLEAR, RefreshDisposition.from(AccountAuthResult.NotAuthorized))
        assertEquals(RefreshDisposition.CLEAR, RefreshDisposition.from(AccountAuthResult.RefreshTokenInvalid))
    }

    @Test
    fun unknownResults_neverClear() {
        // Every Unknown shape the client produces: timeout, rate limit,
        // server error, malformed body, unrecognized 400 rejection.
        listOf("network", "rate_limited", "server_502", "malformed_token_response", "refresh_rejected")
            .forEach { reason ->
                assertEquals(
                    "Unknown($reason) must RETRY, not clear",
                    RefreshDisposition.RETRY,
                    RefreshDisposition.from(AccountAuthResult.Unknown(reason)),
                )
            }
    }

    @Test
    fun exhaustiveness_everySealedMemberMapped() {
        // Guards the `when` against new AccountAuthResult members: any
        // unmapped member fails compilation, so this test documents intent
        // rather than guarding runtime behavior.
        val all = listOf(
            AccountAuthResult.Disabled,
            AccountAuthResult.InvalidCredentials,
            AccountAuthResult.NotAuthorized,
            AccountAuthResult.RefreshTokenInvalid,
            AccountAuthResult.Unknown("probe"),
        )
        assertEquals(4, all.count { RefreshDisposition.from(it) == RefreshDisposition.CLEAR })
        assertEquals(1, all.count { RefreshDisposition.from(it) == RefreshDisposition.RETRY })
    }

    private fun response(status: Int, body: String): SyncHttpResponse =
        SyncHttpResponse(status = status, body = body.toByteArray(Charsets.UTF_8))

    @Test
    fun refreshErrorCode_topLevelStringForm_recognized() {
        // Supabase's actual dead-token shape: {"error":"invalid_grant"}.
        val code = refreshErrorCodeOf(response(400, """{"error":"invalid_grant","error_description":"Invalid Refresh Token"}"""))
        assertEquals("invalid_grant", code)
    }

    @Test
    fun refreshErrorCode_structuredForms_stillRecognized() {
        assertEquals(
            "refresh_token_not_found",
            refreshErrorCodeOf(response(400, """{"error_code":"refresh_token_not_found"}""")),
        )
        assertEquals(
            "refresh_token_not_found",
            refreshErrorCodeOf(response(400, """{"error":{"code":"refresh_token_not_found"}}""")),
        )
    }

    @Test
    fun refreshErrorCode_otherCodesAndGarbage_passThroughOrNull() {
        assertEquals("some_other_code", refreshErrorCodeOf(response(400, """{"error":"some_other_code"}""")))
        assertNull(refreshErrorCodeOf(response(400, """{"error":{"message":"human text"}}""")))
        assertNull(refreshErrorCodeOf(response(400, "not json at all")))
        assertNull(refreshErrorCodeOf(response(400, "")))
    }

    @Test
    fun refreshErrorCode_objectErrorFormNotMessageText() {
        // `error` as a JSON object carries .code, not prose — the stable-code
        // contract must not start reading message text.
        assertNull(refreshErrorCodeOf(response(400, """{"error":"  "}""")))
    }
}
