# 8b3a — local safe identity recovery repair, 13 October 2026

Completed the source/local-database part of the existing 8b3 recovery unit. **Identity recovery is not yet accepted against real Supabase Auth.** No project was created, no DEV/PROD state changed and nothing from this batch was published or deployed.

## Changes

The old destructive CLI and Python wipe entrypoint now refuse in-place wiping. Restore requires an explicit isolated destination env file, confirmed project name/reference and exact matching HTTPS Supabase host. Portable export retains shop timezone and audit actor ID/name. Strict source validation rejects forbidden credential/old-Auth fields, invalid timestamps/status, missing actors/Owners, duplicate/cross-business links and sources without established maintenance/unresolved-operation evidence.

A manifested snapshot converter validates all seven hashes/counts, the expected source, linked Auth identity consistency and reconciliation absence. It projects only portable identity/audit fields and omits the raw Auth data. Scheduled snapshots omit private reset state, so conversion requires independently established capture-time maintenance/no-unresolved-reset evidence. The two already accepted live backups do **not** automatically establish those conditions. Older manifest-free snapshots are refused by this converter rather than asserted safe.

A CLI-generated `20261013075706_safe_empty_identity_restore.sql` supplies service-only source status and atomic empty-destination import RPCs. They use explicit service authorization, empty search paths and revoked PUBLIC/ordinary-role execution. Import checks enabled current guards/catalog and reset/sync dependencies, locks public app/identity tables, private reset state and Auth users before checking emptiness, and preserves identity/audit rows in one transaction. A late failure rolls back every DB insert before any Auth dispatch. Concurrent imports serialize; READ COMMITTED is required. No new table/RLS bypass policy or weakened existing trigger was introduced.

Auth creation/linking remains separate. One call is sent per non-terminated staff member, disabled staff included; terminated staff receive no Auth identity. A failure retains destination state, reports partial/uncertain recovery and retains credentials for known-created identities. There is no deletion, rollback wipe or automatic replay. Operator instructions explain maintenance and verification before reuse. The normal staff update trigger advances `updated_at` when new Auth is linked; this is disclosed, not relabeled as exact timestamp preservation.

The normal bootstrap seed behavior is retained. A separate recovery bootstrap suppresses the business/shop sample seeds while retaining roles, then loads the existing C1.6/sync/protected-reset and new recovery migrations. It never clears a populated project.

## Selected verification

**16 host checks passed**, covering source/target refusal before any gateway call, stable portable semantics, active/disabled/terminated orchestration, old-wipe refusal, manifest corruption/count/source/Auth/reconciliation cases, and partial/uncertain Auth outcomes with known credentials retained.

**9 native PostgreSQL 18.6 groups passed**, including actual current-source bootstrap/import, stable IDs/timezone/status, full audit semantic comparison including legacy null actors/detail, ordinary-role permissions, late FK/duplicate/Owner failure rollback, existing unlinked Auth/unrelated app state preservation, enabled guards/isolation checks, actual uncertain-reset/reconciliation detection and two-connection import serialization. `host-results.json` and `native-results.json` retain the bounded results. Test Auth tables/helpers and service-role defaults are Supabase stand-ins; this is database proof, not live Auth/profile acceptance or deployed catalog parity.

The first host attempt had one incorrect substring assertion that matched the metadata key `auth_user_ids_omitted`; it was corrected to check staff fields. The first DB attempt stalled because the test read through a second connection while import locks were held; that exact disposable cluster was stopped. The next run exposed missing bare-PostgreSQL service fixture privileges; expected Supabase defaults were supplied to the test fixture. Final checks passed after explicitly preserving null audit detail and exercising uncertain reset/reconciliation. All disposable clusters were stopped and removed. No broader app tests, dependency/CLI upgrade, production advisor run or real credential dispatch ran.

Source baselines and exact installed hashes are in `baseline-source-hashes.json` / `source-sync.json`. Only eight reviewed recovery source/docs files were synchronized into the local external server checkout after baseline guards; unrelated named bootstrap/C1.6/sync/reset files are unchanged. Existing uncommitted work is preserved.

## Next existing boundary: 8b3b

Prepare the exact publication and empty-project proposal, identifying project/region/cost/privacy/schema/fixture and teardown boundary before any real project creation. Then rehearse current source with synthetic active Owner, disabled and terminated accounts against real Auth/profile policy, independently compare stable identities/full audit semantics, and verify populated-destination refusal. Existing shared DEV/PROD cannot serve as this fixture. Keep the identity item open until this evidence exists. App restore 8b4, shop setup 8c, reset browser/production, phase 5 and phase 6 stay separate.
