# Phase 14.8.54c — shared async-UI component adoption

Date: 2026-09-25
Branch: `main` (follows 14.8.54b, commit `c5ae155`)

## Outcome

The previously dead shared components in `common/NativeAsyncUi.kt` are now the real implementation for the Cashier and Operations families: 25 hand-rolled full-screen loading/failure panes across 17 files replaced with `NativeColdLoadingPane` / `NativeLoadFailurePane`.

- `NativeLoadFailurePane` gained an optional `retryTestTag` parameter so per-screen retry-button test tags (e.g. `ActivityRetryLoad`, `OpsMoreRetryLoad`) survive on the same node type — the contract tests kept passing unchanged.
- Scaffold-wrapped failure panes (Activity, Cashier More, Operations More, Attendance) keep their scaffolds/BackHandlers; only the inner bodies were replaced.
- All replaced panes preserve their original container test tags via modifier.

## Deliberately deferred: owner-family screens

The 14 owner screens embed loading/failure branches inline in their `when` arms (no named pane functions), so adoption means per-screen structural surgery with no user-visible gain; the duplication cost they carry is small. Owner More/Work/Overview already follow the 14.8.52/53 baseline after 54a/54b. Deferred until a natural touch point.

## Verification

- `qaFast`: PASS (`975` rows, `0` untested/orphans — native inventory gate confirms zero tag drift), `lintDebug` + `assembleDebug` PASS
- Device (SM-A065F): converted-screen contract suites **50/50 PASS** across two batches (More, Operations Work/More, Attendance, Deliveries, MakeYogurt, ReceiveMilk, RecordSpoilage, Activity, Cashier Expense, Collection, + facades). One timeout on `overpaymentRequiresExplicitAdvanceConfirmation` in the first combined batch reproduced as a PASS 6/6 in isolation — flake, not regression.
