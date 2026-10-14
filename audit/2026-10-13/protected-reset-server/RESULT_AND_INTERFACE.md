# 8a1 — prepared server reset guard

The [generated migration](supabase/migrations/20261012194632_protected_credential_reset_guard.sql) implements the scoped database policy and reset reservation. It is prepared in this workspace; it has not been copied into the external server checkout, applied remotely or integrated into the current Viewer/CLI reset routes. **B4 remains open until 8a2 and deployment acceptance.** Installing this migration alone would not protect the old unguarded Auth calls.

## Delivered policy and behavior

- Active, linked, non-terminated Owners can reserve linked non-terminated targets in the same business across the five known roles, including disabled targets. Reset does not enable them.
- Admins can reserve only active Manager/Cashier/Operator targets. Owner/Admin targets, unknown roles and ineligible callers fail closed.
- Requests lock business first and both participant rows in UUID order, then re-read authority. Participant row versions advance so an older repeatable-read snapshot cannot miss the reservation and proceed.
- A trigger blocks role, business, active, termination, Auth link, username changes and deletion for both participants while unresolved. Display-name edits remain possible. One participant cannot join overlapping reservations.
- Only the trusted service gateway can claim a reserved request once, report its outcome or read its private pending metadata. The privileged CLI maintenance tier reserves the target through its own service-only entry point. No plaintext credential is stored and no Auth password is changed by the migration.
- Successful completion inserts the actor-attributed audit, changes state and releases participants atomically. An audit failure preserves the guard. Repeated completion is idempotent. An uncertain dispatch has no expiry, cannot be replayed and cannot be cancelled as “not applied”; known success can finish it. The gateway must not claim known failure unless it has evidence the Auth mutation did not occur.

## RPC contract for 8a2

| Function | Authorized tier | Meaning |
|---|---|---|
| admin_begin_credential_reset(p_staff_id, p_request_id) | Authenticated staff caller | Fresh policy decision; return stable UUID reservation, with same-request retries idempotent |
| admin_begin_maintenance_credential_reset(p_staff_id, p_request_id) | Owner-controlled service-key maintenance | Reserve target without claiming a staff identity from caller-supplied data |
| admin_claim_credential_reset(p_request_id) | Service gateway | Move reserved → dispatching once; return reservation, target staff and Auth IDs for one Auth request |
| admin_mark_credential_reset_uncertain(p_request_id, p_code) | Service gateway | Retain unresolved guard for AUTH_UNREACHABLE or AUTH_UNKNOWN; store no raw response body |
| admin_find_pending_credential_reset(p_staff_id) | Service gateway | Recover pending reservation metadata after a lost response; no Auth ID/credential returned. Apply current Viewer caller/business policy before displaying it |
| admin_finish_credential_reset(p_request_id, p_outcome) | Service gateway | completed for known Auth success; not_applied for undispatched cancellation or a known rejection. An uncertain dispatch cannot use not_applied |

Errors: 42501 unauthorized; 42P01 hidden/missing target or request; 22023 invalid input/ineligible target; 55000 conflicting/unresolved/replayed state; 40001 stale transaction snapshot requiring transaction restart. Retrying the database transaction never means automatically replaying the Auth request.

The service key is a trusted server/maintenance authority, not an ordinary staff capability. It must remain outside browser/client code. The database guard does not protect against a compromised service key calling Auth directly. Existing supported Viewer and CLI reset paths must adopt this protocol in 8a2.

## Verification and limits

The final SQL passed **20 focused groups** in a disposable native PostgreSQL 18.6 cluster bound to localhost. The fixture loads the current external C1 migrations through C1.6, supplying synthetic Auth UID/role helpers and identities. It checks caller/target permission matrices, private-table/function permissions, protected direct writes, maintenance, duplicate dispatch, audit attribution/rollback, uncertainty retention and pending recovery. Four two-connection READ COMMITTED cases verify both race orderings and overlapping requests; two older REPEATABLE READ cases verify stale participant/caller snapshots fail with 40001. See [verification.json](verification.json) and source hashes. This is SQL/locking proof with Auth stand-ins, not a real Supabase Auth reset or live catalog parity proof.

The runner's first attempt hung after startup because Windows inherited a captured output pipe. That exact disposable cluster was stopped, the runner was corrected, and subsequent runs completed with cluster shutdown and removal. No leftover isolated-pg directory remains. Supabase CLI 2.117.0 generated the migration; its timestamp is retained as generated. No CLI/dependency upgrade was made. No Android tests were run because app code was not touched in this unit.

## Next boundary and deployment notes

8a2 changes only Viewer reset orchestration, its necessary RPC allowlist/response handling, the supported CLI reset command and contract/UI eligibility. Route tests must show no Auth call on denial, one call per claim, no retry after unknown dispatch, explicit pending feedback, durable completion/audit and current Owner/Admin target eligibility. An unresolved reservation deliberately prevents participant authorization changes; 8a2 must provide visible recovery guidance rather than silently leaving the user waiting. A trusted operator must establish a known outcome before release; no guessed outcome, TTL unlock or blind Auth replay is acceptable.

Deploy the SQL before enabling guarded Viewer/CLI paths. Do not remove the guard, schema or request rows during rollback while any participant remains reserved. If integration is not deployed, this SQL is not an accepted fix. Remote preflight, migration/advisor checks, real disposable-identity acceptance and publication remain separate; no production password reset or server change was performed here.
