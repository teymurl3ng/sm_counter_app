# Production server deployment — 2026-10-10

The owner explicitly approved proceeding with PROD after the completed DEV acceptance. The earlier automatic approval rejection was an authorization gate; this newly authorized MCP deployment succeeded. This report supersedes older pending-PROD status statements without changing their historical evidence.

## Deployed

- Project: **saafmilk-prod / tlsuxbghuhlqntkgtrxt**, verified ACTIVE_HEALTHY before deployment.
- Migration: **20261010141548_saaf_c1_6_sync_boundary**, applied once through Supabase MCP.
- Exact reviewed SQL: audit/2026-10-09/repairs/SUPABASE_LIVE_REPAIR_2026-10-10.sql, SHA256 **8856aa3b069eb7a6badd165d97003c373c43963f8f7004950911129e8e5b8308**. This is the same file already applied and verified in DEV.
- **S01 — HIGH: repaired and verified in PROD.** Anonymous sync table access and ordinary-role direct writes/sequence access are revoked. Authenticated SELECT and the existing sync_push/sync_pull RPC interface remain available; service-role privileges are unchanged.
- C1.6 adds the final-active-owner guard and service-only created-identity verification RPC, extends creation reconciliation and fixes the recursive redactor search_path. The payload-object CHECK is validated.

Public functions increased from 34 to 36. Exactly two existing definitions changed: the business-scoped admin_record_auth_reconciliation overload and redact_json_secrets. Two functions were added: protect_last_active_owner and admin_verify_created_identity. Unrelated RPC bodies, including sync_push, sync_pull and my_profile, are byte-hash unchanged within PROD. New/repaired migration function definitions match DEV after normalizing line endings. The initial cross-project hash check also noticed a pre-existing CRLF/LF difference in set_staff_updated_at; its definitions are identical after newline normalization, and this migration did not change it.

## Verification and data preservation

Preflight and postflight confirm **36 sync records**, max sequence **72**, zero non-object payloads, zero unsupported table names and zero shop/business mismatches. The complete sync-data digest is unchanged. Business, shop and staff row digests are also unchanged.

Counts remain one business, one shop, three staff, three Auth identities, 32 audit rows and zero reconciliation rows. No active staff lacks an Auth link and no business lacks an active owner. No account was created/deleted/reset, no history row or sync cursor was rewritten, and no sequence reset or fixture allocation was made.

Postflight checks verify:

- Anonymous sync SELECT/INSERT/UPDATE/DELETE and sequence access are all false.
- Authenticated SELECT remains true; INSERT/UPDATE/DELETE and sequence access are false.
- Existing service-role table/sequence privileges are unchanged.
- The permissive INSERT policy is absent; the payload CHECK is validated and staff_last_owner_guard is enabled.
- The new internal functions cannot be executed by anonymous/authenticated roles; identity verification remains service-only.
- Under an existing active owner's SQL role/claim context, my_profile, admin_overview and sync_pull succeed. Pull returns all 36 records in that shop.
- Authenticated direct INSERT planning and anonymous SELECT planning are refused with SQLSTATE 42501. EXPLAIN was used without ANALYZE, so no writes were executed.
- Anonymous ping remains available. The service-only recursive redactor removes synthetic secret values and preserves harmless nested values.

The verification transaction used read calls and query planning, then rolled back. No test history, synthetic business, staff or reconciliation rows were inserted into PROD. The installed owner guard and reconciliation definitions match the DEV code whose rollback fixtures and real session checks are preserved separately. No live production disable/demote/delete or ambiguous-Auth fixture is claimed.

## Advisors

The mutable redactor search_path warning is cleared. Remaining notices are the previously classified RPC-only RLS tables and deliberately callable authenticated SECURITY DEFINER APIs, plus **leaked-password protection disabled**. No permissive policies or API revocations were added merely to silence these notices.

References: [RLS without policies](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy), [authenticated SECURITY DEFINER APIs](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable), [leaked-password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection). Password-policy/provider decisions remain separate from this migration.

## Evidence and remaining release work

This folder preserves project identity, migration history before/after, preflight/postflight catalogs, DEV reference definitions, migration response, advisor responses and the read-check SQL/result. verification-production-final.json records final assertions and file hashes. Historical rejection evidence and earlier manifests remain dated snapshots.

The PROD **server repair is deployed**. Android production installation/distribution and published Viewer deployment were not performed by this server step. A01/A02 and the earlier client repairs therefore remain awaiting their respective production releases. The local repaired Viewer is still running against DEV, and the A06 acceptance preview remains enrolled in DEV; no shop history or enrollment was moved to PROD.

This verification uses database role/claim contexts, not a new GoTrue login, signed JWT, production browser session or phone-to-PROD sync. DEV real Auth/session/staff checks already pass. A production application smoke check should follow publication with an authorized production account. No exhaustive feature parity, physical power-loss or production disaster-recovery guarantee is added by this deployment.
