# Account Bridge — Schema 9 & Backup Format 3 Checklist

**Status:** issued 2026-09-25 (Phase 1 exit-gate item 4)  
**Authority:** `ACCOUNT_BRIDGE_PHASE1_SPEC.md` (frozen 2026-09-25) — this file converts its §4/§8/§10 field lists into migration and test checklists. It adds no semantics.

## Existing code anchors

- `MilkShopDatabase` is at `version = 8` (`phase18/app/src/main/java/com/saaf/milkshop/data/MilkShopDatabase.kt`).
- Existing security entities: `AppUserEntity` (`app_users`), `UserCredentialEntity` (`user_credentials`), `SecurityAuditEntryEntity` (`security_audit_entries`).
- Portable backup: `BackupManager.kt`, `BackupRestoreEngine.kt`, `BackupCryptography.kt` (current format 2).
- Frozen baseline hashes in `README.md` remain intact; schema 9 is an approved native extension, not a silent baseline change.

## A. Room schema 8 → 9 migration

- [ ] A1. Migration 8→9 is **additive only**: no schema-8 table, column, or row is altered, dropped, or rewritten. Existing ERP/security data is untouched by the migration itself.
- [ ] A2. New entity `account_installation_binding` (singleton):
  - [ ] columns exactly per spec §4: `binding_id`, `environment` (`dev`/`prod`, immutable after activation), `business_id`, `shop_id`, `shop_name_snapshot`, `status` (`pending`/`active`/`suspended`/`restored_unverified`/`conflict`), `verified_at`, `contract_version`, `created_at`, `updated_at`;
  - [ ] at most one row enforced (DAO + entity constraint + test);
  - [ ] changing environment/business/shop is impossible via update-in-place; only the owner-confirmed unlink/reset flow (which preserves ERP data and audit history) may clear it.
- [ ] A3. New entity `server_identity_links`:
  - [ ] `local_user_id` PK/FK → `app_users.user_id`; `server_staff_id` unique stable UUID; `business_id` must equal the installation binding (validated on write + test);
  - [ ] `username_snapshot`, `role_snapshot` (display/reference only), `status` (`pending`/`verified`/`offline_grace`/`suspended`/`revoked`/`restored_unverified`/`conflict`), `verified_at`, nullable `server_changed_at`, `last_error_code` (stable diagnostic code only);
  - [ ] a server staff UUID links to at most one local user per installation (uniqueness test).
- [ ] A4. New entity `server_shop_snapshots`: `server_shop_id`, name snapshot, observation timestamp, linked local user. Cache only — never an ERP shop ledger; no write path from shop management UI.
- [ ] A5. `security_audit_entries` gains the new safe action types: enrollment, link/unlink, verification, environment mismatch, remote suspension/revocation, restore-to-unverified, session clearing. Payload carries stable IDs and result codes only — never credentials, tokens, or raw response bodies (serialization test).
- [ ] A6. Offline-grace fields and derivation (spec §6, decided 2026-09-25):
  - [ ] grace = 7 days from `verified_at`, reset on every successful verification; `UNKNOWN` never changes it;
  - [ ] post-expiry tier = writes blocked / reads allowed, enforced at the native command boundary (deny-by-default), not in UI code;
  - [ ] grace expiry never deletes link/user/data and never fabricates a server denial;
  - [ ] definitive `DISABLED`/`NOT_AUTHORIZED` applies revocation rules immediately regardless of remaining grace.
- [ ] A7. Migration test: 8→9 preserves every schema-8 row (full-table count/checksum comparison), creates **no** binding/link row, and leaves token stores empty.
- [ ] A8. Exported schema 9 JSON is committed; `phase18/qa` inventory scripts remain green.

## B. Portable backup format 3

- [ ] B1. Format version 2 → 3 inside the existing Argon2id + AES-256-GCM authenticated envelope. Format-2 **import** remains supported; format-2 export is not produced anymore.
- [ ] B2. Payload additions (inside the encrypted/authenticated envelope, spec §8):
  - [ ] installation environment, business UUID, shop UUID, safe name snapshots;
  - [ ] local-user ↔ server-staff mappings (stable IDs only);
  - [ ] last verified role/shop snapshots;
  - [ ] binding/link state and verification timestamps;
  - [ ] local audit events for linking and verification.
- [ ] B3. Never-included list enforced by a serialization/round-trip test: Supabase access/refresh tokens; plaintext server credentials; admin/service-role/publishable runtime keys; Auth user IDs or fake emails; Android Keystore keys/aliases; Quick PIN plaintext, biometric or device-credential material; active local sessions; transient lockout timers; raw network bodies or headers.
- [ ] B4. Restore semantics (all seven spec §8 rules):
  - [ ] verify/decrypt and restore Room atomically through the existing replacement boundary;
  - [ ] invalidate every local and remote session and delete all persisted remote tokens;
  - [ ] convert every restored installation/link state to `restored_unverified` regardless of backed-up value;
  - [ ] preserve stable server IDs and historical snapshots — never generate replacements;
  - [ ] require online re-verification against the same environment and business before server-derived permissions or account administration are trusted;
  - [ ] business/shop mismatch becomes `conflict` requiring owner resolution; never silently rebind ERP data;
  - [ ] Google Drive remains transport only — not an identity-token store or synchronization engine.
- [ ] B5. Round-trip tests: format-3 export→restore preserves stable mappings and forces `restored_unverified`; format-2 restore yields an **unbound** installation with no invented server identity.
- [ ] B6. Restored database remains a single writable installation after activation; the double-restore warning stays in user documentation.

## C. Android host/instrumented test matrix (spec §10, verbatim scope)

- [ ] C1. schema 8→9 migration creates no binding/link/token and preserves all rows (= A7);
- [ ] C2. explicit link only — no username/display-name-based auto-link, even when usernames match;
- [ ] C3. DEV/PROD mismatch and business/shop mismatch fail closed (`conflict`, no rebind);
- [ ] C4. tokens absent from Room, backups, logs, and saved UI state (includes Compose state and saved-instance state);
- [ ] C5. format 3 round trip preserves stable mappings and forces `restored_unverified` (= B5);
- [ ] C6. format 2 restore produces an unbound installation without invented server identity (= B5);
- [ ] C7. `UNKNOWN` never disables a user or expires grace early;
- [ ] C8. definitive disable/termination clears tokens and invalidates the affected local session;
- [ ] C9. the APK contains no admin/service key and cannot call admin RPCs/routes (static scan + network allowlist test);
- [ ] C10. existing ERP, Quick unlock, Drive backup, and full device regression remain green (273-test baseline plus native suites).

## D. Explicit non-goals / guards

- No networking enters the app in the schema-9 migration itself; the typed client arrives later behind the frozen §5 contract.
- No C1 (write administration) dependency: Android never calls C1 routes and never holds elevated keys.
- No multi-writer synchronization; restoring the same backup on two writable devices stays unsupported.
- The frozen Phase 18 logic specification and SQL blueprint hashes are untouched; schema 9 is recorded as an approved native extension in the phase record.

## Review gate

This checklist is reviewed and accepted before schema-9 implementation begins (spec §11 item 6). Implementation may then proceed top-down: A2–A5 entities → A7 migration test → A6 grace enforcement → B1–B6 format 3 → C matrix → full regression.
