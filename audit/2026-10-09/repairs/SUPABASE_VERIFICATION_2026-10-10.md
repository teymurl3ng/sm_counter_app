# Supabase continuation — 2026-10-10

The connection became available after A06 verification. This supersedes the earlier access blocker, while preserving its dated evidence. All live operations used the Supabase MCP. No migration, business-row update, Auth account change, token rotation or deployment occurred.

## Verified live scope

| Environment | Project reference | State/version | Captured sync rows |
|---|---|---|---|
| saafmilk-dev | tghzmofkkdsaasvnixps | ACTIVE_HEALTHY; PostgreSQL 17.6 | 39 |
| saafmilk-prod | tlsuxbghuhlqntkgtrxt | ACTIVE_HEALTHY; PostgreSQL 17.6 | 36 |

Evidence is under evidence/supabase-*. The catalog queries run in explicit READ ONLY transactions with statement timeouts. Captured metadata includes public columns, constraints, indexes, RLS/policies, effective grants, triggers and all 34 public function definitions. Row inspection returned aggregate integrity counts only. No passwords, API secrets, refresh tokens, emails or personal row payloads were collected.

DEV and PROD have equivalent captured columns, policies, effective table/sequence grants, trigger definitions and public function definitions after normalizing comments/newlines. The raw staff timestamp function differs only by CRLF/LF. Migration histories differ in the earlier sync installation steps, but their resulting objects match. History labels alone are not proof of current function definitions. The preliminary concern about old SQLSTATEs was resolved: live C1.5 RPCs also match the prepared C1.5 bodies; they are not deployment drift.

## S01 — HIGH: direct authenticated sync INSERT bypasses RPC controls

Both live projects grant authenticated INSERT/UPDATE/DELETE/SELECT on sync_records and sequence USAGE. The INSERT policy checks only membership in sync_staff_shop_ids(). The sequence trigger assigns a number; it does not run sync_push's table whitelist, correction-role check, business-header binding or per-shop advisory lock. Consequently an assigned authenticated staff account can insert directly instead of using the intended RPC path. UPDATE/DELETE currently have no permitting RLS policies; their broad grants alone do not prove those operations can modify rows.

Read-only EXPLAIN, without ANALYZE, confirms that PostgreSQL accepts an authenticated direct INSERT plan. No live INSERT was executed. The native fixture replays the relevant captured live functions, grants and policies with synthetic staff/shops. It proves that sync_push rejects a cashier history correction while a direct INSERT accepts it, including a foreign business header for an authorized shop. A second direct INSERT accepts an unknown table and JSON null. These are local reproductions of the captured control gap, not live attacks or proof of a complete malicious-ledger workflow.

Live aggregate scans found zero non-object payloads, unknown table names or business/shop header mismatches at the captured snapshots. No observed poisoning or general historical-data correctness is claimed.

**Prepared repair:** SUPABASE_LIVE_REPAIR_2026-10-10.sql removes ordinary-role direct writes and sequence access, drops the INSERT policy, preserves authenticated SELECT for the invoker pull RPC, and validates an object-payload check. Existing service_role access is unchanged. All ordinary sync writes continue through the current SECURITY DEFINER push RPC. No blanket grants, sequence reset or business-history rewrite is included.

## Existing C1.6 repairs are not deployed

Both migration lists omit C1.6. Both catalogs lack admin_verify_created_identity(uuid), protect_last_active_owner() and staff_last_owner_guard. The existing reconciliation recorder rejects create_verify. The already-prepared repair for F07/F26 therefore remains local; the new Viewer reconciliation workflow cannot be certified against deployed C1.5.

The reviewed SQL includes the existing C1.6 repair in the same transaction as S01 hardening. It adds the verification RPC and required owner trigger without deleting Auth identities. Current aggregate snapshots show zero active staff without an Auth link, zero businesses without a live owner and zero pending reconciliation records. These are observations, not evidence that every historical interleaving was safe.

## Local bootstrap repairs and remaining differences

The initial fresh-bootstrap catalog lacked shops.shop_timezone and the profile's per-shop timezone field. Both live projects already have a nullable text column with no default. The local server migration.sql now mirrors that nullable field and the actual live my_profile definition. Unknown timezone remains null; the client's existing fallback is preserved. CONTRACT.md documents the additive field without changing its required frozen fields.

The local migration_c1_3_hardening.sql now fixes the internal redactor's search_path. The live helper is an invoker function restricted to service_role and privileged callers; public client roles cannot create objects in public. The advisor notice is hygiene/defense-in-depth, not a reproduced public privilege escalation. The live repair sets its path to empty while preserving recursive redaction semantics.

Fresh sync foreign keys use NO ACTION, while live uses ON DELETE CASCADE. This deletion-policy difference remains explicit; no live FK or deletion semantics were changed. The fresh payload check is intentional hardening, now included in the reviewable live SQL. The different shop/sequence index name has equivalent indexed columns. PostgreSQL 18 exposes NOT NULL constraints separately; those catalog entries are version formatting rather than new defects.

The catalog comparator reports three prepared/live function-body differences: C1.6's recorder, the redactor path and the fresh sequence trigger. The trigger comparison remains explicit; the proposal preserves the captured live trigger and changes its ordinary-role entry boundary instead. Lexical equality after comment/newline normalization is metadata comparison, not a formal semantic equivalence proof.

## Checks and limits

- supabase-native-boundary-repair.txt: **11 native PostgreSQL 18.6 checks pass**. Reproduces the direct-insert gap; verifies that invalid history rolls back the whole migration without dropping rows or partially revoking access; validates successful/repeated application, direct-write denial, preserved correction authorization and ordinary RPC push/pull, timezone profile, payload shape, recursive redaction and final-owner protection. Deliberately invalid rows were removed only in the disposable fixture before the success path.
- supabase-bootstrap-parity-final.txt: **11 native checks pass**, including READ COMMITTED/REPEATABLE READ competing owner removals. supabase-local-parity-baseline.txt and supabase-native-boundary-first-10.txt preserve the earlier successful stages. All temporary clusters were stopped and removed.
- Live authenticated context without an identity: auth.uid() null, zero eligible shops, zero visible sync rows, successful empty pull in both projects. Anonymous direct read fails with SQLSTATE 42501 at the private membership helper.
- Live owner-context public reads: profile and overview succeed, and a one-row pull succeeds within the owner's 3 DEV / 1 PROD eligible shops. A preliminary probe incorrectly called the private write-caller helper directly and failed with its intended permission denial; the corrected public read is saved separately.
- These use transaction-local SQL role/claim contexts with the real deployed DB functions. They do **not** test GoTrue sign-in, JWT issuance/signature verification, refresh, logout/revocation or a real Viewer/phone session. No end-user token was fabricated or used. Real Auth and deployed reconciliation rehearsal remains pending.

## Advisor findings kept separate

Both environments report:

- Seven RLS-enabled tables with no policies: intended RPC/service-role access. Effective direct anon/authenticated grants on staff and the other protected identity tables are revoked. Do not add permissive policies merely to silence [this advisor](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy).
- Eighteen authenticated SECURITY DEFINER RPCs: intended APIs whose bodies authorize the caller. Keep [the advisory](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable) separate from S01's actual direct-table bypass; do not blanket-revoke public application RPCs.
- The redactor's mutable search_path: prepared fix above; [remediation reference](https://supabase.com/docs/guides/database/database-linter?lint=0011_function_search_path_mutable).
- Disabled [leaked-password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection): an Auth configuration review item. No paid feature or password policy was enabled by this task.
- Seven [unindexed foreign keys](https://supabase.com/docs/guides/database/database-linter?lint=0001_unindexed_foreign_keys): performance hygiene, with no measured slow-query claim. PROD also reports one [unused audit index](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index); do not remove an audit/recovery index based solely on usage since a statistics reset.

The provider reports PostgreSQL 17.6, while its [17.11 rollout notice](https://supabase.com/changelog/postgres-15-19-17-11-breaking-changes) describes security fixes. Review provider patch/upgrade status separately; metadata does not establish an exploitable CVE. ltree/btree_gist are not installed; no upgrade, reindex or decryption was attempted. The installed extension list is preserved. Current documentation and the changelog were consulted; the markdown changelog fetch was unsupported, so the official HTML page was used.

## Next steps

Review and approve the exact SQL for DEV deployment, then run catalog/grant/advisor and ordinary push/pull/owner/reconciliation checks. Recheck the payload-shape preflight immediately before applying; the constraint validation aborts atomically if incompatible history appeared. The script is a proposal, not an installer already executed on the projects.

Rehearse real Auth/session and ambiguous-create reconciliation with an authorized DEV test account before releasing the Viewer changes. PROD deployment, historical partial-event recovery, owner visual acceptance, release motion tracing, persistent/costing dataset profiles and any physical power-loss test remain separate pending checks. The earlier A06 results and all original F01–F33 severities are preserved.

## Subsequent deployment — 2026-10-10

The preceding sections preserve the read-only/pre-deployment snapshot. DEV subsequently received the reviewed C1.6/S01 SQL through MCP, and deployed database checks pass; its rows remain unchanged after rolled-back verification. PROD's exact migration was rejected by automatic approval review for insufficient explicit production authorization, and its catalog remains unchanged. S01 remains HIGH/open in PROD. See DEPLOYMENT_AND_MANUAL_CHECKS_2026-10-10.md for the current deployment/acceptance checklist and verification-deployment-final.json for the later evidence hashes. Real Auth-session verification is still pending.
