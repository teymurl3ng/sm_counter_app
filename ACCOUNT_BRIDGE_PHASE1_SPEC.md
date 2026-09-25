# Account Bridge Phase 1 — Integration Contract Freeze

**Status:** Started — freeze candidate 1  
**Date:** 2026-09-24  
**Depends on:** Android Room schema 8 / backup format 2, Account Server Phase A public contract, C0 read contract  
**Does not depend on:** C1 write administration being release-ready

## 1. Goal

Define the boundary between the offline-first Android ERP and the Account Server before adding networking, schema 9, or provisioning UI. Phase 1 changes documentation only. It deliberately prevents either system from silently becoming a second source of truth.

## 2. Non-negotiable ownership

| Concern | Authority | Consequence |
|---|---|---|
| Business, shop, and staff UUIDs | Account Server | Android stores stable mappings; it never invents server UUIDs. |
| Username, account status, termination, role string, shop assignment | Account Server | A verified server profile supersedes linked local identity metadata. |
| Sales, inventory, accounting, production, customers, vendors, employees, attendance, close/corrections | Android Room | No Account Server or Admin Viewer route writes ERP truth. |
| Local passphrase, recovery verifier, Quick PIN, biometric/device credential, local session | Android | None is sent to or interpreted by the Account Server. |
| Server credential and Supabase session | Account Server/Auth | Android uses them only for enrollment/session renewal; they are not local ERP credentials. |
| Google Drive backup schedule, encryption, retention, and restore | Android | The Account Server neither stores nor decrypts business backups. |
| Admin provisioning and remote identity writes | Admin Viewer / trusted server path | Android never receives an admin/service key and never calls C1 admin routes. |

## 3. Identity model

Local and server identities are related, not merged.

```text
Account business UUID
        │
        ├── Account shop UUID ── one enrolled Android installation
        │
        └── Account staff UUID ── explicit 1:1 link ── local app_users.user_id
```

Rules:

- One production installation binds to exactly one Account Server environment, one business, and one shop.
- A local user may have zero or one server link. A server staff UUID may link to at most one local user inside an installation.
- Existing users are never auto-linked by matching username or display name. An owner explicitly resolves each link.
- The stable link key is `staff_id`, not username, email, display name, role, or Auth user ID.
- `auth_user_id` is never exposed to Android.
- A server role is an entitlement label. Kotlin remains the only implementation of the role-to-permission matrix and checks every command locally.
- Server disable/termination prevents future cloud verification and, once definitively observed, revokes the linked identity according to the enforcement policy. Network failure is never interpreted as disablement.

## 4. Schema 9 contract

Phase 3 will implement additive Room schema 9. Phase 1 freezes these logical records; exact Kotlin names may differ only if the persisted columns and invariants remain equivalent.

### `account_installation_binding` — singleton

| Field | Contract |
|---|---|
| `binding_id` | Locally generated immutable ID for this binding record. |
| `environment` | `dev` or `prod`; immutable after activation without an explicit destructive reset. |
| `business_id` | Stable server UUID. |
| `shop_id` | Stable server UUID for this installation's shop. |
| `shop_name_snapshot` | Display-only last verified value. |
| `status` | `pending`, `active`, `suspended`, `restored_unverified`, or `conflict`. |
| `verified_at` | Last definitive successful verification timestamp. |
| `contract_version` | Server contract observed at verification. |
| `created_at`, `updated_at` | Audit timestamps. |

There is at most one row. Changing environment/business/shop is not an update-in-place operation; it requires an owner-confirmed unlink/reset flow that preserves ERP data and audit history.

### `server_identity_links`

| Field | Contract |
|---|---|
| `local_user_id` | PK/FK to `app_users.user_id`. |
| `server_staff_id` | Unique stable server UUID. |
| `business_id` | Must equal the installation binding. |
| `username_snapshot` | Last verified normalized username; display/reference only. |
| `role_snapshot` | Last verified frozen server role. |
| `status` | `pending`, `verified`, `offline_grace`, `suspended`, `revoked`, `restored_unverified`, or `conflict`. |
| `verified_at` | Last successful `my_profile` verification. |
| `server_changed_at` | Optional future entitlement version/timestamp; null until the server exposes one. |
| `last_error_code` | Stable diagnostic code only; no tokens or raw response bodies. |

### `server_shop_snapshots`

Stores the active shops returned for a verified identity: `server_shop_id`, name snapshot, observation timestamp, and linked local user. It is a cache, never an ERP shop ledger.

### Audit additions

The local append-only security audit gains safe actions for enrollment, link/unlink, verification, environment mismatch, remote suspension/revocation, restore-to-unverified, and session clearing. It records stable IDs and result codes but never credentials or tokens.

## 5. Public network contract used by Android

Android may call only:

1. Supabase password sign-in for `{username}@saafmilk.local` plus the 15-digit server credential;
2. `my_profile` with the returned access token;
3. token refresh/logout through the Auth client;
4. `ping` for diagnostics, never for authorization.

Android must not call C0 admin-list RPCs or any C1 write/admin route.

Header boundary:

- the publishable key is sent in `apikey`;
- `Authorization: Bearer ...` is used only for a real user access token on authenticated calls;
- an `sb_publishable_...` key must not be treated as a bearer JWT;
- no secret/service-role/admin key is packaged, downloaded, logged, or accepted by Android.

The implementation must preserve the server's five-way result model:

| Result | Meaning | Local reaction |
|---|---|---|
| `ALLOWED` | Auth and active profile are definitive | Verify/update link and secure session. |
| `DISABLED` | Auth succeeds; profile definitively inactive | Suspend link; clear remote session; do not relabel as bad password. |
| `INVALID_CREDENTIALS` | Auth explicitly returns the stable invalid-credential code | Show a generic credential error; do not change an existing link. |
| `NOT_AUTHORIZED` | Auth identity has no staff profile | Clear remote session; block enrollment/linking. |
| `UNKNOWN` | Timeout, TLS/DNS, 429, 5xx, malformed/unexpected response | Preserve last known state; apply offline policy; never revoke. |

## 6. Enrollment and daily authentication

### Installation enrollment

1. Owner chooses DEV only in a developer build; release builds are PROD-only.
2. Owner enters a server username and one-time/distributed server credential in a native secure screen.
3. Android signs in and calls `my_profile`.
4. Enrollment requires `ALLOWED`, role `owner`, and a deliberate choice of one returned active shop.
5. Android creates the installation binding and explicitly links the verified owner to an existing or newly created local owner.
6. Android persists the refreshed server session securely, writes a local audit event, and clears credential input state.

No account record, shop binding, or local user is created after an `UNKNOWN` result.

### Staff linking

- An owner/admin creates staff through the Admin Viewer or trusted CLI.
- On the Android device, the staff member proves the server credential online once.
- `staff_id`, business, role, and shop membership must match the installation binding and selected shop.
- The user then establishes or confirms the separate local passphrase/Quick-unlock setup.
- Server credentials never become the daily offline unlock secret.

### Daily use

- Local passphrase or device-bound Quick unlock opens the offline ERP session.
- A background/foreground verification attempt refreshes remote entitlement when connectivity permits.
- A definitive server denial changes link state and invalidates the affected local session.
- An `UNKNOWN` network outcome uses the last verified entitlement and a separately approved offline-grace policy.

The exact offline-grace duration and post-expiry operational tier remain the only product decision not frozen here. They must be approved before enforcement work begins; implementation must not smuggle in an arbitrary timeout.

## 7. Token and secret handling

- Use a maintained Supabase/Ktor client stack only after a focused spike proves the exact Auth, refresh, logout, and `my_profile` shapes against DEV.
- Supply a custom Android-Keystore-backed session store. Do not use plain SharedPreferences, Room, logs, saved-instance state, or Compose state persistence for tokens.
- Access and refresh tokens are replaceable cache material, not backup data.
- Clear remote tokens on logout, definitive disable/termination, environment reset, business/shop conflict, and every restore.
- Redact `Authorization`, `apikey`, password/credential fields, token responses, and raw Auth errors from network logging.
- Network Security Config remains HTTPS-only for release; no user-added CA trust or cleartext fallback.
- No MCP, browser extension, or custom protocol is needed. This is an application-layer integration with ordinary HTTPS and contract tests.

Recommended implementation candidates for the Phase 4 spike are `supabase-kt` Auth/PostgREST modules, Ktor's OkHttp engine, and Kotlin serialization. Adoption is conditional on secure custom session persistence and exact header behavior; a small typed Ktor client is the fallback if the SDK obscures required classifications.

## 8. Backup and restore contract

Schema 9 introduces portable backup **format 3**. Format 2 imports remain supported.

### Included inside the existing encrypted/authenticated payload

- installation environment, business UUID, shop UUID, and safe name snapshots;
- local-user ↔ server-staff mappings;
- last verified server role/shop snapshots;
- binding/link state and verification timestamps;
- local audit events for linking and verification;
- the existing local identity, Argon2id credential verifier, recovery state, ERP data, and actor attribution already protected by format 2.

### Never included

- Supabase access or refresh tokens;
- plaintext server credentials;
- admin/service-role/publishable keys from runtime configuration;
- Auth user IDs or fake emails;
- Android Keystore keys or aliases treated as portable secrets;
- Quick PIN plaintext, biometric data, or device-credential material;
- active local sessions, transient lockout timers, raw network bodies, or request headers.

### Restore behavior

1. Verify/decrypt the backup and restore Room atomically through the existing replacement boundary.
2. Invalidate every local and remote session and delete all persisted remote tokens.
3. Convert every restored installation/link state to `restored_unverified` regardless of its backed-up value.
4. Preserve stable server IDs and historical snapshots; never generate replacements.
5. Require online re-verification against the same environment and business before server-derived permissions or account administration are trusted.
6. A business/shop mismatch becomes `conflict` and requires owner resolution; it never silently rebinds ERP data.
7. Google Drive continues to transport the encrypted inner backup. It does not become an identity-token store or synchronization engine.

A restored database remains a single writable installation after activation. Restoring the same backup onto two independently writable devices is still not synchronization.

## 9. C1 relationship

C1 belongs to the administrative provisioning channel, not the Android bridge.

- Phase 1 can freeze against the tested public Auth + `my_profile` contract while C1 is repaired.
- Staff creation/reset/termination may be operated through the Admin Viewer only after the gate in `ACCOUNT_BRIDGE_C1_VERIFICATION.md` passes.
- Android receives the outcome through a later staff sign-in/profile verification; it never proxies C1 and never holds elevated keys.
- C1 audit history and Android security audit history remain separate, correlated by stable business/staff IDs where available.

## 10. Required contract tests before implementation is accepted

### Account Server DEV

- Header shapes with the current publishable-key type;
- all five authentication classifications;
- role/status/shop changes reflected by `my_profile`;
- token expiry, refresh, logout, rate limit, malformed body, and network failure;
- stable UUID preservation through identity export/recovery.

### Android host/instrumented tests

- schema 8→9 migration creates no binding/link/token and preserves all rows;
- explicit link only; no username-based auto-link;
- DEV/PROD and business/shop mismatch fail closed;
- tokens are absent from Room, backups, logs, and saved UI state;
- format 3 round trip preserves stable mappings and forces `restored_unverified`;
- format 2 restore produces an unbound installation without invented server identity;
- `UNKNOWN` never disables a user;
- definitive disable/termination clears tokens and invalidates the affected local session;
- Android contains no admin key and cannot call admin RPCs/routes;
- existing ERP, Quick unlock, Drive backup, and full device regression remain green.

## 11. Phase 1 exit gate

Phase 1 is complete when:

1. this ownership and identity mapping are accepted;
2. the offline-grace duration/post-expiry tier is decided;
3. DEV contract probes validate current headers and response shapes;
4. the schema 9 and format 3 field lists are converted into migration/test checklists;
5. C1's status is recorded honestly as either gated or accepted;
6. no implementation begins with unresolved authority or backup semantics.

The next execution phase is server/restore-boundary hardening, followed by the Android schema 9 and backup-format-3 migration.
