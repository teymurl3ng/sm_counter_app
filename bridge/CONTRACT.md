# Saaf Milk Canonical Bridge Contract

**Status: FROZEN — contract version 1 — 2026-08-28**

Manifest SHA-256: `30AA8461C61B1A99376CA9B45AFFD6EDAD829AE9B4E6746AA7FE67E7270C9F27`

## 1. Authority and scope

This is the single normative bridge/API contract between the packaged React UI and Kotlin. `bridge/contract.json` is its machine-readable endpoint/schema appendix and is incorporated into this contract. `scripts/verify-bridge-contract.py` proves that appendix, the Kotlin endpoint snapshot, the TypeScript action/types projection, the frozen Phase 5 DTO names, permissions, and native read methods remain aligned.

Contract version 1 defines messages only. It does not implement a WebView, JavaScript listener, Kotlin command façade, or React screen wiring. Those begin only after this phase.

Authority remains:

1. frozen Phase 18 specification and SQL;
2. approved native extensions;
3. Kotlin/Room for business truth, validation, authorization, and persistence;
4. this contract for transport shape only;
5. React for presentation and user input only.

React must never calculate COGS, balances, stock, corrections, permissions, actor identity, or any other business truth.

## 2. Transport

Phase 7 must expose one origin-restricted asynchronous message channel named `saafMilkNative` only to packaged assets served from `https://appassets.androidplatform.net`. No bridge object is exposed to arbitrary `file:`, `http:`, remote, or navigated origins.

JavaScript sends one UTF-8 JSON object through `saafMilkNative.postMessage(...)`. Kotlin sends exactly one response for every accepted request ID. Replies may arrive out of order; `requestId` is the sole correlation key.

### Request envelope

```json
{
  "contractVersion": 1,
  "requestId": "2f721b62-89bf-4e83-b7a8-bbe2a122d18f",
  "action": "query.dashboard",
  "sessionHandle": "opaque-memory-only-value",
  "payload": {
    "startDateInclusive": "2026-08-28",
    "endDateExclusive": "2026-08-29"
  }
}
```

- `contractVersion`, `requestId`, `action`, and `payload` are required.
- `requestId` is a caller-generated UUID and is not an accounting/event identifier.
- `sessionHandle` is required only for endpoints whose manifest `auth` is `session`.
- `sessionHandle` is forbidden inside `payload`.
- Enrollment completion uses its one-use enrollment token in payload and no authenticated session.
- Unknown top-level or payload fields are rejected as `invalid_request`; they are never ignored.
- The maximum request or response JSON size is 1 MiB. Backups never cross the bridge as JSON/base64 bytes.

### Success envelope

```json
{
  "contractVersion": 1,
  "requestId": "2f721b62-89bf-4e83-b7a8-bbe2a122d18f",
  "action": "query.dashboard",
  "ok": true,
  "data": {}
}
```

`data` is always present. A `BridgeUnit` response is `{}`.

### Failure envelope

```json
{
  "contractVersion": 1,
  "requestId": "2f721b62-89bf-4e83-b7a8-bbe2a122d18f",
  "action": "query.dashboard",
  "ok": false,
  "error": {
    "code": "session_expired",
    "message": "Your session expired. Sign in again.",
    "field": null,
    "retryable": false
  }
}
```

Failure responses never contain `data`, exception names, stack traces, SQL, filesystem paths, verifier state, lockout timing, or secret input.

## 3. Scalar encoding

| Logical type | JSON/TypeScript representation | Validation |
|---|---|---|
| native `Long`, IDs and counts | branded base-10 string (`Int64String`) | regex `^-?(0|[1-9][0-9]*)$`, then checked signed-64-bit parse |
| PKR money/rates | branded integer-paisa string (`PaisaString`) | base-10 `Long`; inputs requiring non-negative/positive amounts are checked natively |
| quantity/ratio/percentage | branded decimal string (`DecimalString`) | plain base-10 only; no exponent, `NaN`, infinity, commas, or localized digits |
| date | `YYYY-MM-DD` string | strict `LocalDate.parse` and domain period rules |
| month | `YYYY-MM` string | strict `YearMonth.parse` |
| timestamp | ISO instant string | native persisted instant |
| boolean | JSON boolean | strings/numbers are rejected |
| null | JSON null | accepted only where the schema explicitly marks `nullable` |

Money never travels as a JSON number. Kotlin converts paisa to the existing domain representation once at the command boundary using decimal arithmetic and does not duplicate formulas. TypeScript must not use `Number(...)`, `parseFloat`, arithmetic, or locale formatting as a source of truth.

## 4. Authentication and secret handling

- The session handle is an opaque bearer secret created by Kotlin. React keeps it in memory only—never localStorage, sessionStorage, IndexedDB, cookies, URLs, DOM attributes, analytics, console output, saved UI state, or backups.
- A reload or renderer/process loss requires native session revalidation; no authenticated content is rendered until `query.currentSession` succeeds.
- `session_expired` clears all React authenticated state and routes to sign-in. `permission_denied` does not log the user out.
- Owner bootstrap succeeds only when no user exists.
- Login, unavailable-account, disabled-account, and setup-required failures return the same `authentication_failed` message.
- `command.auth.completeEnrollment` consumes a private one-use enrollment token. It does not establish a session; the staff user signs in normally afterward.
- Recent authentication is enforced in Kotlin for every endpoint marked `recentAuth`; React cannot assert freshness.
- Passphrases, recovery codes, enrollment tokens, restore tickets, and session handles are redacted from logs and crash reports. Kotlin uses mutable character buffers where supported and clears them after use.
- A one-time recovery code is returned only by successful bootstrap/recovery. The UI must require deliberate acknowledgement and must never retain it after leaving that flow.

The bridge never accepts a caller-supplied actor name, actor role, actor user ID, effective permission set, or authoritative current-user ID. Target role/status fields in authorized user-administration commands describe the target change; they do not grant authority to the caller.

## 5. Endpoint categories

The exact payload field names, required/nullable rules, response type, permission, persistence effect, error set, and refresh invalidations for every endpoint are in the hash-bound manifest.

### Queries

Queries are read-only and map one-for-one to the frozen Phase 5 service:

- bootstrap/session: `query.bootstrapStatus`, `query.currentSession`;
- operational/cashier/financial: `query.dashboard`, `query.cashierSummary`, `query.financialOverview`;
- configuration/master/reference: `query.businessSettings`, `query.masterData`, `query.transactionReferences`;
- history/ledger: `query.eventHistory`, `query.eventDetail`, `query.accountLedger`;
- operations: `query.deliveries`, `query.attendance`, `query.spoilageHistory`, `query.reconciliationArchive`;
- reporting/planning: `query.profitabilityReport`, `query.costingManagement`, `query.planning`;
- security administration: `query.users`, `query.securityAudit`.

A query cannot write, fabricate defaults, or replace null financial coverage with zero.

### Commands

Commands mutate native state and are never retried automatically:

- authentication/user lifecycle: all `command.auth.*` actions;
- transactions and corrections: `command.record*` and `command.correctEvent`;
- store/settings/opening state;
- product, customer, vendor, employee, and expense-category administration;
- attendance and delivery lifecycle;
- cash count and close/reopen;
- budgets/targets;
- costing, opening basis, yield policy, and loss assessment.

Phase 7/8 implementation must route protected commands through an authenticated Kotlin application-command façade. For event-producing commands, permission verification and `event_actor_links` attribution must occur in the same database transaction as the event. Existing unauthenticated domain methods are implementation internals and may not be directly exposed to JavaScript.

### System operations

- `system.bridgeInfo` is the only unauthenticated system read. React uses it before any other action and refuses a version/hash mismatch.
- `system.exportEncryptedBackup` invokes an Android-owned create-document flow. Only metadata returns over the bridge.
- `system.inspectEncryptedBackup` invokes an Android-owned open-document flow, authenticates the encrypted backup, and returns a short-lived one-use in-memory restore ticket plus non-secret metadata.
- `system.restoreInspectedBackup` accepts only that ticket and explicit confirmation. Restore invalidates every session; success returns the UI to bootstrap/sign-in.

React never supplies arbitrary filesystem paths or content URIs and never receives backup bytes.

## 6. Conditional command validation

The manifest defines structural required/nullable fields. Kotlin additionally enforces these cross-field rules:

- Sale: exactly one of `quantity` or `valuePaisa`; `account` payment requires only `paymentAccountId`; `credit` requires only `customerAccountId`; disabled surcharge requires null/zero surcharge according to the native adapter.
- Corrections: `correctionTypeCode` selects the allowed field family. Unrelated correction fields must be absent. Sale correction requires exactly one of quantity/value. Monetary corrections accept amount only. Spoilage corrections require quantity and physical reason.
- Opening state: product/account IDs are unique; values are non-negative; initial state is permitted only before the first business event and never created by migration.
- Profiles: required nullable fields are intentional replacement values—null clears the stored optional value. Omitted optional fields on event commands mean “not supplied,” not “clear.”
- Budgets: milk plans use liters and a Monday-to-Monday week; operating categories use amount and a calendar month. Closed periods cannot be revised.
- Costing: component set is exact, policy month/reason rules remain native, and superseding IDs must name the current active version.
- Yield policy: accepted production IDs are unique and native calibration/sample-span rules apply.
- Loss assessment: ordinary requires zero abnormal quantity; abnormal requires positive quantity; `other` requires notes.
- Close/reopen: actor identity always comes from the session. Close uses the latest physical count; non-zero variance requires a reason; reopen is recent-authenticated owner/admin only.
- Backup restore: ticket is memory-only, short-lived, single-use, bound to the inspecting user/session and selected document, and `confirmRestore` must be true.

All other existing domain/database validation remains authoritative and must not be copied into React.

## 7. Errors

Stable error codes are:

| Code | Meaning / UI behavior |
|---|---|
| `invalid_request` | Malformed envelope, unknown/missing field, invalid scalar encoding, contract mismatch. Do not retry unchanged. |
| `validation_failed` | Native business validation rejected input. Show safe message and optional field. |
| `authentication_failed` | Generic login/reauth/recovery failure; never reveal account availability. |
| `unauthenticated` | Protected call omitted a session handle. Route to sign-in. |
| `session_expired` | Handle invalid/expired/revoked. Clear authenticated UI and route to sign-in. |
| `permission_denied` | Valid user lacks native permission. Refresh current session/visibility; do not retry. |
| `recent_authentication_required` | Prompt for current passphrase, then explicitly repeat the user-approved action. |
| `rate_limited` | Authentication/recovery throttled. Do not expose exact lockout state. |
| `not_found` | Referenced native record does not exist. Refresh relevant query. |
| `conflict` | Current state no longer permits action or expected active version changed. Refresh before retry. |
| `day_closed` | Posting date is closed. Authorized owner/admin may use the explicit reopen flow. |
| `insufficient_stock` | Inventory validation rejected the operation. Refresh stock. |
| `user_cancelled` | Android picker was dismissed. Treat as neutral, not failure toast. |
| `backup_wrong_passphrase` | Generic backup authentication failure. Do not reveal encrypted metadata. |
| `backup_tampered` | Authentication/integrity failed. Never offer unsafe import. |
| `backup_incompatible` | Format/schema/security state cannot be safely restored. |
| `restore_ticket_expired` | Repeat inspect flow; never reuse ticket. |
| `io_failed` | Native document/storage operation failed. Offer deliberate retry. |
| `internal_error` | Safe generic unexpected failure. Never expose implementation detail. |

`retryable` is native-owned. React may automatically retry read queries only when `retryable=true`, never commands/system writes. `recent_authentication_required` repetition occurs only after successful reauthentication and explicit preservation of the pending user intent.

## 8. Refresh and concurrency behavior

- Each manifest `refresh` entry is a query invalidation tag, not a second automatic command.
- After command success, React reruns only currently mounted/permitted invalidated queries, preserving their existing parameters.
- No success UI appears before the success response and required refresh completes or visibly fails.
- Failure never triggers optimistic persistence or a fake “Saved” message.
- Duplicate in-flight `requestId` is `conflict`. Completed command IDs are not an idempotency guarantee; the client must never auto-retry a command after an unknown transport outcome. The user refreshes native state before deciding whether to repeat it.
- Kotlin serializes commands through its application boundary and Room transactions. Queries may run concurrently but must return one internally consistent native result.
- Phase 11 retains persisted idempotency/concurrent-write and backup-during-write hardening; this contract does not pretend those tests are already complete.

## 9. Version negotiation and drift gate

`system.bridgeInfo` returns the native version, manifest SHA-256, and actions. Before authentication, TypeScript compares both version and hash with its generated constants. A mismatch blocks the UI with a non-destructive “App components do not match” state.

The contract changes only by:

1. editing `bridge/CONTRACT.md` and `bridge/contract.json` together;
2. running `python scripts/verify-bridge-contract.py --write`;
3. reviewing generated Kotlin and TypeScript projections;
4. running the checker without `--write`, TypeScript compilation, and Gradle compilation/tests;
5. recording compatibility and version impact in the active phase document.

Gradle `preBuild` and the UI `prebuild` script must run the checker. Phase 7 must not begin bridge implementation while the gate is red.
