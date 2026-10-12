# B3b — narrowed execution scope

Status: completed and verified within this boundary; see [B3b result](../withdrawal-account-correction/RESULT.md). This remains the controlling boundary for B3b and supersedes speculative future preparation in the broader withdrawal plan. B3 remains partial.

## One outcome

The authenticated Owner/Admin correction path can change the recorded Cash/Bank account of a withdrawal, optionally correcting its amount at the same time. Later amount corrections and cancellation use the latest declared account. Original records remain intact.

Example: a recorded Cash withdrawal of Rs 250 should have been Bank/Rs 200. Append a correction restoring Rs 250 to Cash and deducting Rs 200 from Bank. Later correcting that withdrawal to Rs 150 restores Rs 50 to Bank. Cancellation then restores Rs 150 to Bank. None of these operations represents money physically returned.

## Included work

1. Add only the account/reference data needed to retain declared account truth across correction chains. Proposed immutable details have event ID, payment account ID and withdrawal root event ID. No reserved receipt-review columns or returned-money queries.
2. Extend the existing typed correction input with an optional corrected account. Omission retains the current account. Keep the existing amount/cancellation path, original date, fresh reason, role guards, sibling protection and transactional attribution/audit.
3. Validate exact financial legs. Same account uses old amount minus new amount. Changed account restores the old amount to the old account and deducts the new amount from the new account; omit zero legs. Cancellation retains the latest account. No stock, sale, expense or profit movement.
4. Preserve supported existing history through the additive database migration and backup export/restore. Infer account truth only from unambiguous legacy chains; incomplete or contradictory history must not be guessed. Update snapshot capability version when required to prevent older readers ignoring essential data.
5. Serialize account/root references by UID, atomically apply the complete event and its details on an isolated receiver, and handle repeat/missing-dependency replay. Existing-header replay must account for missing essential details. No live sync rollout is claimed.

Complete movement validation belongs at domain, restore and sync boundaries. A row trigger validates one row; it cannot prove that both required financial legs were submitted.

## File boundary

Existing paths below are relative to `phase18/app/src/main/java/com/saaf/milkshop/`:

- `domain/OwnerDrawing.kt`, `domain/Correction.kt` and, if needed, one small `domain/OwnerFundsTruth.kt` resolver.
- New `data/OwnerFundsEntities.kt` / `data/OwnerFundsDao.kt`, containing only the three account/reference fields and their required reads.
- `data/DatabaseSql.kt`, `data/MilkShopDatabase.kt` and the generated next schema (current Room version 14).
- `data/BackupDaos.kt`, `BackupManager.kt`, `BackupSnapshotCodec.kt`, `BackupRestoreEngine.kt`.
- `data/SyncEntities.kt`, `data/SyncMigrationSql.kt`, `sync/SyncEnvelopeFactory.kt`, `sync/SyncApplyEngine.kt`, solely for the new detail's capture, UID references, full movement validation and replay.
- `application/commands/HistoryBridgeCommandService.kt` only if propagation or the existing permission/date/atomic boundary requires it. Reuse its current behavior; do not refactor it.
- Focused account-correction instrumentation cases, affected methods in `DrawingRecordingRemedyTest`, and ordinary `OwnerDrawingTest` regression methods. Do not edit unrelated test suites.

These are allowed boundaries, not a requirement to edit every file. Record a necessary extra dependency before editing it; stop and report if meeting this outcome requires redesigning another subsystem.

## Selected checks and stop condition

Use acceptance groups 1–8 from `ACCEPTANCE.md` only, plus affected existing drawing methods:

- Account-only and account-plus-amount corrections, then a subsequent amount correction, cancellation and restore of a cancelled record.
- Literal independent Cash/Bank balances, unchanged original rows, no stock or operating-profit effect.
- Invalid account/amount, missing reason, unchanged truth, wrong role, duplicate replacement, original closed date and changed-date bypass refuse without partial writes.
- Wrong or extra financial legs refused; injected second-leg/attribution failure rolls back all new rows and outbox/audit work.
- Version-14 upgrade preserves supported raw facts; ambiguous legacy history gains no invented truth.
- Legacy and new backup round trips, UID replay with different numeric IDs, repeated replay, missing details and missing account/root dependency retain complete facts or an explicit whole-record refusal.

Run only selected tests affected by these changes. Use isolated test databases and the QA test package on A06 when available. No ordinary-app install or full suite. Save results, exact source/schema hashes and limitations; stop when the listed behavior is verified. A failing selected check is investigated within this boundary; it does not open a global audit.

## Explicit exclusions

No returned-money event, excess-review rules, receipt timeline, account-selector UI, new forms, global design work, profile/planning receiver repair, server deployment, remote-policy audit, staff credential change, ordinary-app installation, commit/push or release packaging. No reserved schema fields or infrastructure for B3c/B3d.

The account selector remains B3d. This run establishes tested data behavior and compatibility; it does not deliver a new visible account-change control or prove old clients can consume the new sync shape. All writable clients must receive compatible builds before exposing that control.

New unrelated findings are recorded in the existing later queue without investigation. B3c starts only in its own subsequent run.
