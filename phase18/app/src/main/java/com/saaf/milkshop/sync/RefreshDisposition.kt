package com.saaf.milkshop.sync

/**
 * H1 (audit 2026-10-02): a failed token refresh must never destroy a
 * session that a transient failure could have survived. The frozen Account
 * Bridge spec §5 is explicit — UNKNOWN never revokes. Before this decision
 * existed, `refreshSession()` cleared the tokens on ANY non-Allowed result,
 * so one timeout mid-drain silently de-synced the device until manual
 * re-enrollment.
 *
 * Definitive results are proof the session is genuinely dead (revoked
 * account, rejected credential, or a server-stated dead refresh token):
 * the caller clears the stored tokens and the installation re-verifies by
 * re-enrolling. Everything else — timeout, 5xx, rate limit, malformed
 * response, any unrecognized rejection — keeps the tokens and retries on
 * the next drain.
 */
internal enum class RefreshDisposition {
    /** Session is genuinely dead: clear the stored tokens (nothing else). */
    CLEAR,

    /** Transient or already-fresh: keep the session untouched. */
    RETRY;

    companion object {
        fun from(result: AccountAuthResult): RefreshDisposition = when (result) {
            is AccountAuthResult.Allowed -> RETRY
            AccountAuthResult.Disabled,
            AccountAuthResult.InvalidCredentials,
            AccountAuthResult.NotAuthorized,
            AccountAuthResult.RefreshTokenInvalid,
            -> CLEAR
            is AccountAuthResult.Unknown -> RETRY
        }
    }
}
