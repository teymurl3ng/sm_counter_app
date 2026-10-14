# DEV reset rollout — API accepted; browser check blocked

The exact reviewed reset migration is installed **only in DEV**, version `20261012194632`. A protected [Viewer preview](https://sm-server-view-hotwmja5d-mkrc.vercel.app) is READY and its safe system response reports DEV. Source is the verified `b5122e8` base plus the four 8a2 manifest files, recorded in local-only candidate snapshot `f41936e2a7943854ff37111325b1d5372734cdc1`. No main-branch push, production deployment/promotion or production database change occurred. Production still has no guard.

## Accepted evidence

Ten focused live groups passed against real DEV Auth/PostgREST and the hosted preview: DEV environment binding, disposable identities/Viewer sessions, staff-token denial of service claim, Admin denial of Owner/Admin/disabled Cashier, Owner reset of disabled Cashier without enabling access, Admin reset of active Cashier, participant-change guard/single claim/pending status, idempotent known-success completion, installed CLI maintenance reset and fixture retirement.

Four fixture credential resets completed. For permitted Viewer resets the new credentials worked and the old credentials were rejected. Protected-target denials retained existing fixture credentials and produced no reset-success audit. A known-success request deliberately deferred completion: pending lookup exposed its reference, another reset returned pending without a credential, and completion retried idempotently with one audit and no Auth replay. The CLI used its maintenance reservation path and the captured one-time credential worked. Credentials, cookies and tokens stayed in memory and were not saved in evidence.

The installed catalog verifies six public wrappers, two private functions, empty definer search paths, intended execute grants, RLS and denied direct SELECT/INSERT/UPDATE/DELETE on both private tables for anon/authenticated/service_role. Security advisors were fetched. New findings are intentional deny-all private-table RLS with no policies and the intentionally authenticated reservation definer; caller authorization is enforced inside it. Existing leaked-password-protection and other definer findings remain outside this unit. See the [RLS advisor guidance](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy), [definer guidance](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable) and [existing password-protection guidance](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection).

All **five** disposable accounts (three API fixtures and two unused browser fixtures) are terminated, inactive and unlinked; Auth identities were removed through the supported lifecycle. Their history is retained. Final catalog shows four completed reset requests and **zero held fixture participants**. Existing staff were not changed. Fixtures joined an existing DEV business with an active Owner so supported retirement could respect the last-active-Owner rule; no business or shop was created or modified.

## Browser acceptance remains open

An actual-browser feedback check was prepared through a loopback-only relay to the exact protected preview, using only the two disposable browser identities. Automatic approval review denied opening the QA origin twice, including after the human explicitly approved that exact origin. Both denial messages cited a browser interruption; no conclusion that the human actually revoked their explicit approval is inferred from those messages. The blocked browser action was not retried via another surface or indirect browser execution.

The relay was stopped, its two unused fixture accounts were retired, and zero browser-fixture reset-success audit entries exist. The actual browser check is **NOT RUN**, not passed. Earlier controlled 8a2 JSX/state checks remain credited but do not substitute for this live browser observation. Keep the final browser feedback gate and production acceptance open.

## Operational issues resolved within this scope

Supabase connector migration calls returned expired request-state errors twice and did not apply SQL. CLI fallback initially lacked authentication. Normal browser CLI sign-in was completed using the user-provided one-time verification code; no API token was pasted or printed. The isolated migration directory initially failed the remote-history check. `migration fetch` mirrored already-applied history into that temporary directory, then `migration up` applied only the new reset migration. No migration-history repair, old replay, broad database push or CLI upgrade occurred. Fetched history and candidate checkouts are ignored temporary artifacts.

The first preview snapshot used a synthetic author and Vercel blocked it for lack of project membership. The local-only snapshot was corrected to the existing repository identity with Codex QA attribution retained; the next preview built successfully. Neither source repository was pushed. No project access/protection settings changed. The preview status was independently checked rather than relying on the CLI's early “ready” wording.

## Stop boundary and remaining plan

This unit ends with DEV API acceptance and honest browser-blocked status. **B4 is not closed**: complete the one browser feedback check before accepting the full reset release; production migration/staged build/promotion and scoped repository publication remain separate. A browser access-state change or a later explicitly authorized review is needed; do not keep reasking for identical approval or bypass the review. Production rollback preparation must retain the guard/held requests and fail closed rather than restore the old direct-reset path.

Recovery 8b and shop setup 8c can be scoped independently while that browser gate remains open. Phases 1 money/withdrawals, 2 receiving sync and 3 mapped menus/lists remain complete at bounded QA scope. Phase 4 access/recovery/setup remains in progress; phase 5 shop-day/phone acceptance and phase 6 freeze/signed release remain. No phone, Android suite or broad regression ran here.
