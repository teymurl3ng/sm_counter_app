# Recovery preflight — verified baseline, 13 October 2026

This closes **8b1 preflight**, not recovery readiness. No remote write, workflow dispatch, real restore, account change, Drive change or signing-key read occurred. Raw identity/Auth rows were read transiently for snapshot metadata and never saved or printed. Only source, counts, keys, hashes, workflow errors and public provenance are retained.

## Server backup: current evidence

Repository `teymurl3ng/saaf-milk-account-server` is private and accessible. Remote main remains `0cf106e8422a6a04da340b025dbef5fa0018074e`. The deployed workflow is the old inline curl/checkout implementation; `scripts/identity_snapshot.py` is absent remotely (404). The prepared local workflow and collector remain unpublished.

The [12 October scheduled run](https://github.com/teymurl3ng/saaf-milk-account-server/actions/runs/38258501235) failed. Snapshot collection succeeded; branch commit failed because an untracked `.snapshot_dir` would be overwritten by checkout. Thus this run did not publish a recovery point. This is confirmed by job steps and its error log, not inferred from the old source alone.

Newest stored recovery point is directory `2026-10-09`, branch commit `208fbde5adc369ed038e595c00bdd8adcafde22c`, committed 9 October at 15:37 PKT. It was about 92.7 hours old at the metadata check. It contains seven JSON files and no manifest. Counts: 1 business, 1 shop, 3 staff, 2 assignments, 32 audit events, 0 reconciliation rows and 3 Auth users. Business-ID checksum matches PROD and differs from DEV, verifying its actual environment rather than trusting workflow comments. Current PROD has 2 shops and 50 audit events; the saved point is behind by **one shop and 18 audit entries by count**. Counts do not establish which exact rows differ, unchanged staff fields, original capture completeness or transactional consistency.

Deployed export requests have no REST pagination and only the first 100 Auth users. The stored snapshot is currently below those limits, so **no truncation of this particular snapshot is established**. The implementation is still capped for growth. Its date-only folder and branch switch after snapshot explain preservation risk. Local replacement uses paginated stable ordering, duplicate detection, branch switch before creation, unique timestamp folders, count/consistency manifest and scoped staging. Five existing focused offline checks pass, including capped REST responses, >100 Auth users and three repeated local backup-branch writes that retain older points. These are not two passing live workflow runs.

## Identity restore: implemented behavior and blockers

Four in-memory characterization checks confirm current source behavior. Stable business/shop/staff IDs and new credentials for active/disabled non-terminated accounts are implemented; terminated fixtures receive no Auth identity. These checks use synthetic gateways and are not a live restore pass.

Confirmed gaps inside existing O1:

- CLI export selects audit fields without `performed_by` or `performed_by_username`; restore also omits both. The contract promises actor preservation when present. The synthetic restore test reproduces actor loss.
- Stored workflow files are raw table/Auth dumps, while `recover_identity.py` accepts a combined portable export and rejects staff `auth_user_id`. A documented validated conversion is required; these formats are not interchangeable.
- Current emergency main unconditionally wipes. Its wipe deletes linked Auth users **before** database identity deletion and deletes audit last. It does not clear reconciliation dependencies. Current C1.6 last-active-Owner protection refuses deleting the last Owner; audit/reconciliation references can also prevent the current delete order. The operation is therefore not accepted for the current schema. No real wipe was used to test this; source order and SQL guard/FKs are observed, while exact first failing database statement is not established remotely.
- New reset private tables depend on business/staff and are absent from the current snapshot/bootstrap sequence. Recovery must explicitly handle pending/uncertain resets and the current matching schema; it must not remove guards or restore stale Auth IDs as a shortcut. Completed reset audit history and credential recovery are separate concerns.
- Snapshot consistency is sequential/nontransactional even in the prepared replacement. Exact recovery requires a maintenance boundary or a separately verified transactionally consistent backup. Do not relabel the collector as an atomic database dump.

These are scoped acceptance dependencies of the existing recovery item, not new product features. Do not run the emergency wiper against PROD, shared DEV or ordinary shop data.

## App and signing: preserved evidence

Earlier real Drive recovery is documented in `PHASE14_4_SECOND_DEVICE_RECOVERY.md`: a data-rich S24-to-isolated-A06 restore and incorrect-passphrase preservation. The October before/after SQL commit process-death probes are in `audit/2026-10-09/repairs/evidence/a06-restore-process-results.json`. They intentionally bypassed safety snapshot creation and did not select all preference-content behavior. Current full app export/restore remains A2; these historical passes are credited, not re-described as current acceptance.

Candidate tests are the existing `Phase9BackupRestoreTest` full records/security/identity/close-history methods, `SyncPlanningRestoreTest` planning-only outbox case and `AccountBridgeBackupFormat3Test` stable server IDs/restored-unverified case. Select only relevant methods for one isolated destination, using an Android emulator or one QA phone if needed. Two physical phones are not required for this restore rehearsal. No Android test was run in preflight.

`FINDINGS_STATUS.md` records the Owner's off-machine signing-key copy in Drive `My Drive/SaafMilk-backup`, SHA-256 verified on 29 September. The 10 October release report records certificate SHA-256 `9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1`. These are historical evidence; current key retrieval, password custody and a signing-from-recovered-key check remain O6/phase 6. Do not read or copy key bytes merely to reconfirm this note.

## Next bounded units and stop conditions

**8b2 — backup automation repair and publication acceptance.** Only `.github/workflows/identity-dr.yml`, `scripts/identity_snapshot.py`, necessary focused tests and an operator note. Preserve snapshot history and private visibility; no schema/identity/ledger reset. Review the exact two prepared source files and bound key compatibility/manifest validation, then prepare the concrete publication payload. On authorized publication, run twice with unique folders and verify both complete manifests, counts and older-point preservation on `identity-backups`; ensure target metadata identifies PROD without exposing secrets. Show the latest successful backup age and explain GitHub failed-run notification/activity checks. Do not store fetched private payloads in this app repository. Stop after remote acceptance or report the exact access blocker. Do not dispatch the old broken workflow as a test.

**8b3 — safe identity recovery repair/rehearsal.** Only portable export/validation/restore/bootstrap/operations boundaries necessary to preserve IDs and actors, validate raw-snapshot conversion and current schema, reject unsafe or unresolved sources, and restore into a verified **empty isolated destination**. Explicitly refuse the existing in-place wipe path for normal rehearsal; no trigger weakening or shared DEV reset. Use synthetic fixture identities first, active/disabled/terminated cases and full semantic audit comparison. A real separate project/branch, cost and destructive teardown need their own exact authorization. Prove no existing state or Auth is changed on preflight failure. No success label from fake Auth alone; retain that boundary if actual Auth acceptance is unavailable. Stop at the named fixture/result.

**8b4 — current app restore and recovery note.** One isolated Android source/destination, synthetic independently compared record headers/children/profiles/planning/security state, encrypted export/passphrase rejection and saved-state continuity. Preserve ordinary installation and Drive objects. Credit earlier targeted withdrawal/planning restore proofs; do not rerun all app suites. Document app portable secrets versus server credentials versus signing key as three distinct recovery needs. Signing-from-recovered-key acceptance stays with the accepted release if it requires Owner custody actions. Stop when the selected comparison passes or a concrete blocker is recorded.

Next run is **8b2 prepared backup automation**, not a broad recovery rewrite. Keep B4 browser/production, 8c shop setup, phase 5 and phase 6 separate. New unrelated observations stay in the existing later queue.
