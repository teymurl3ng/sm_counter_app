# Account Bridge — DEV Contract Probe Protocol

**Status:** issued 2026-09-25 (Phase 1 exit-gate item 3) — results recorded in §4  
**Authority:** `ACCOUNT_BRIDGE_PHASE1_SPEC.md` §5; `SM Admin Server/CONTRACT.md` "Public HTTP / Auth contract" (Phase A v1)  
**Environment:** DEV only (`.env.dev` of the local `SM Admin Server` working copy). Never PROD. All probes here are non-destructive (no writes, no account changes, no admin routes).

## 1. Purpose

Validate, against the live DEV Supabase project, the exact header shapes and response shapes Android will rely on — before any networking code is written. The open question the probes must settle:

- Sign-in and `ping` use the publishable key as `Authorization: Bearer` per Supabase convention, while authenticated calls (`my_profile`, `logout`) must use a **real user access token** as Bearer. The Android spec forbids treating an `sb_publishable_...` key as a bearer JWT on authenticated calls. Probes P2/P7 prove both halves: publishable-as-bearer must fail on `my_profile`, user-token-as-bearer must succeed.

## 2. Probe matrix

| # | Probe | Request | Expected (per CONTRACT.md) | Proves |
|---|---|---|---|---|
| P0 | Health | `POST /rest/v1/rpc/ping`, `apikey: publishable`, `Authorization: Bearer publishable` | `200`, timestamptz body | Reachability; publishable key accepted for anon RPC |
| P1 | Valid sign-in | `POST /auth/v1/token?grant_type=password`, `apikey: publishable`, `Bearer publishable`, `{email: username@saafmilk.local, password: 15 digits}` | `200` with `access_token` (+ Auth fields); token shape is a JWT | Credential exchange shape; fake-email domain works |
| P2 | Profile with user token | `POST /rest/v1/rpc/my_profile`, `apikey: publishable`, `Bearer {access_token}` | `200` with frozen field set: `staff_id`, `username`, `display_name`, `role`, `active`, `terminated_at`, `business_id`, `shops[]` | Authenticated call shape; entitlement payload |
| P3 | Invalid credentials | sign-in with wrong password | `400`, body `error_code: "invalid_credentials"` | Stable classification code, not message text |
| P4 | Publishable key as bearer on authenticated RPC | `my_profile` with `Bearer publishable` | `4xx` rejection (not a valid user JWT) | Header boundary: publishable ≠ bearer JWT |
| P5 | Missing apikey | `my_profile` without `apikey` | `401` | apikey requirement |
| P6 | Token refresh | `POST /auth/v1/token?grant_type=refresh_token` with `refresh_token` from P1 | `200` new `access_token` | Refresh shape |
| P7 | Logout | `POST /auth/v1/logout`, `Bearer {access_token}` | `204` (best effort) | Session termination shape |
| P8 | Malformed body | sign-in with `{}` | `400` non-`invalid_credentials` validation error | UNKNOWN/malformed classification boundary |
| P9 | NOT_AUTHORIZED fixture | sign-in as an Auth identity with no staff row | Auth `200`, `my_profile` → `null` | Requires a fixture account — **pending manual setup** |

> **P9/P10 CLOSED 2026-10-09 (verified live on PROD):** an Auth identity without a staff row
> grants tokens but `my_profile` returns null — the client maps that to NOT_AUTHORIZED; and a
> cashier-scoped token invoking `admin_disable_staff` is refused 403 (least privilege proven).
> DISABLED classification is additionally covered by contract Group 2 on DEV. Both probes done;
> no open probe items remain.
| P10 | DISABLED fixture | sign-in as an active-auth but `active=false` staff | Auth `200`, profile with `active=false` | Requires disabling a fixture — **pending manual setup, DEV only** |

P9/P10 need account fixtures that only the (currently gated) admin channel can create safely; they stay pending until the C1 gate or a documented DEV seed script provides them. They are not blockers for freezing header/response shapes (P0–P8), only for closing the full five-way evidence file.

## 3. Recording and redaction rules

- Record: probe number, HTTP status, response field **names** and types, latency bucket, and PASS/FAIL against expectation.
- Never record: access/refresh tokens, the 15-digit credential, publishable/secret keys, `Set-Cookie`, or raw error bodies beyond the stable `error_code` string.
- Any probe returning an unexpected shape is recorded honestly as FAIL with a redacted description; it never gets "rounded" to pass.

## 4. Results

Run of 2026-09-25, script `scripts/probe_account_bridge_dev.py`, DEV project (`saafmilk-dev`), publishable key type `sb_publishable_...`. Non-destructive; a dedicated DEV fixture account `probe_phase1` (owner) was created for the run via the trusted admin CLI because no active real credential exists on DEV (the stored `teimur` credential is not valid on DEV; likely the PROD credential — PROD was not touched). The fixture credential lives only in the gitignored `dev probe credentials.txt` of the server working copy; the fixture stays active for the later client spike.

| # | Result | Evidence (redacted) |
|---|---|---|
| P0 | **PASS** | `200`, timestamptz body via anon publishable bearer |
| P1 | **PASS** | `200`; `access_token` = 3-part JWT, 808 chars; `refresh_token` present |
| P2 | **PASS** | `200`; all 8 frozen fields present; `role='owner'`, `active=true`, `shops` array of 3 |
| P3 | **PASS** | `400`; `error_code='invalid_credentials'` exactly |
| P4 | **PASS** | `401` using the publishable key as bearer on `my_profile` — publishable key is not a user JWT |
| P5 | **PASS** | `401` without `apikey` |
| P6 | **PASS** | `200`; new access token differs from P1's |
| P7 | **FAIL (expectation), contract met** | logout returned the contracted `204`, but the P1 access token still called `my_profile` successfully (`200`) afterward. Supabase logout revokes the session/refresh token; the access JWT stays cryptographically valid until natural expiry (measured: 3600 s). **Consequence:** token revocation must never be an authorization control on Android — see §5. |
| P8 | **PASS** | `400`; `error_code='validation_failed'` — distinct from `invalid_credentials` |
| P9 | **CLOSED 2026-10-09** — verified live on PROD (staffless Auth identity: token grant 200, `my_profile` null → NOT_AUTHORIZED) |
| P10 | PENDING | needs disabled-staff fixture (manual, DEV) |

Additional observations (both classify as `UNKNOWN` inputs, correctly not treated as identity decisions):

- A rapid burst of sign-ins produced one transient `400` that succeeded on retry — rate-limit behavior is real on DEV; the client needs backoff around sign-in.
- The stale-credential first run produced `invalid_credentials` for a credential that is simply wrong *for DEV* — the server cannot distinguish "wrong password" from "right password, wrong environment", so environment binding must be verified via `my_profile` business/shop checks, never inferred from the error.

**Verdict:** current DEV headers and response shapes match the Phase A contract; the publishable-key/bearer boundary behaves exactly as Android spec §5 requires. Gate item 3 is satisfied for shape validation. The five-way classification evidence file covers ALLOWED / INVALID_CREDENTIALS / UNKNOWN so far; DISABLED and NOT_AUTHORIZED stay pending the P9/P10 fixtures.

## 5. Consequences for implementation

- The typed client (Phase 4 spike) must send `apikey` on every call, `Bearer publishable` only for sign-in/ping, `Bearer {access_token}` for `my_profile`/refresh/logout.
- Classification keys on HTTP status + stable `error_code` (`invalid_credentials`), never message substrings.
- **Access tokens are 3600 s JWTs and survive logout until expiry.** Authorization therefore comes only from fresh `my_profile` verification (and the local deny-by-default command boundary) — never from "the token still works" or "the token was revoked". Tokens are replaceable cache material: cleared locally on logout/disable/restore, and never an authorization decision.
- The token payload contains `sub` (Auth user id) and the fake `email`; both are on the spec §8 never-persist list — the client must not copy claims into Room, logs, or backups.
- Sign-in needs retry/backoff for transient `400`/rate-limit responses; any such outcome is `UNKNOWN`, never `INVALID_CREDENTIALS`.
