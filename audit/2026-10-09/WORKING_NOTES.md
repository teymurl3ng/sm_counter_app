# Audit work log — 2026-10-09

This is the evidence log for the completed audit; the final findings report is [PROJECT_AUDIT_2026-10-09.md](<C:/Users/Teymur/Desktop/hybrid project/codex/Saaf_Milk_Codex_Handoff_Pack/Saaf_Milk_Real_Hybrid_Phase3_Native_Expansion_In_Progress_FIXED/PROJECT_AUDIT_2026-10-09.md:1>). No production code has been changed.

## Scope and snapshots

- Android/hybrid checkout: HEAD `4bee92b5d6c9145d9f658a0d0eb0c85cb74f3ea3`.
- Admin Viewer: `C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer`, HEAD `d99510e`.
- Account server: `C:/Users/Teymur/Desktop/saaf-anchor-tmp`, HEAD `0cf106e`. Sync RPC files say extracted from PROD on 2026-10-08; this audit has not verified live deployment parity.
- No production credentials or datasets accessed; no live server mutations.
- A06 device of record absent. The connected S24 (SM-S921W, serial RFCX116CYYX) was used for isolated QA checks and is recorded separately from A06 acceptance.

## Checks so far

- Existing Android host suite: 92 tests, 91 passed, one ignored; bridge contract and UI cleanup passed.
- Added temporary focused host probes: transient failure after one successful push batch returns `Drained(pushed=1)` while one row remains queued. Reproduced failure.
- Initial incoming-ledger rollback probe was inconclusive on the Robolectric framework driver. The production BundledSQLiteDriver probe on S24 then confirmed the partial event header (F32).
- `qaFast` FAIL: 10 inventory orphan tags and pending workflow-test references for drawing/sync additions. Frozen baselines and golden host contract pass.
- Viewer unit suite: 104 tests passed. TypeScript no-emit passed. ESLint: 13 errors and 5 warnings.
- Android lint: 0 errors, 236 warnings; see the final report.

## Source findings being verified

1. Grace refresh executes after empty/offline/failed pushes, without proof of authenticated server success.
2. Restore rebaseline falls through to push after failed pull; cursor-nonzero crash fallback also releases stale state.
3. Partial pull with prior applied rows is reported Done, allowing incomplete rebaseline classification.
4. Take-server only deletes queue/hold; empty pulls never run parked-row recovery.
5. Push compare-and-delete acknowledgement is not atomic with capture-trigger writes.
6. Push accepts malformed/empty 2xx body as success.
7. Ordinary sale refresh restores captured state, overwriting a concurrently completed save; save/follow-up results lack session guards.
8. Viewer logout has global scope; compare Supabase official contract before reporting.
9. Viewer create-staff deletes Auth identity on ambiguous DB RPC network failure.
10. Viewer cache invalidation prefixes do not match session-prefixed internal keys.
11. Identity DR snapshot lacks pagination and may fail when same-date output exists on backup branch.

## Final device/UI notes

- The full selected S24 run used temporary test-only compatibility adapters because the tracked instrumentation sources no longer compiled after the command API move. Those adapters were removed and the audit probes were moved here under probes/.
- Full selected run: 34 tests, 25 passed, 9 failed. F32 and F04 are confirmed. Restore outbox-count failures are stale expectations against format 5. Reports custom-range interaction is F33: ordinary hit-tested click fails, while direct semantics invocation succeeds. Warm-refresh and cashier navigation failures are stale contracts. The collection overpayment test still times out on its second dialog attempt; no source-confirmed product defect was assigned.
- Focused follow-up: reports custom-range test passed only with the diagnostic semantics fallback; collection overpayment failed again. No screenshot was retained because the test package's app-private evidence directory was not accessible after the ADB session ended.

## Decisions to keep separate

- Admin can reset owner credentials: explicitly permitted by server CONTRACT C1, but enables owner takeover; recommend revisiting policy rather than calling it accidental implementation.
- Device-scoped identities, per-clerk attribution, planning pull application, late closed-day event remedy, and plaintext Room are documented decisions/deferrals.
- Last-owner/role restrictions and Auth-reset permissions need consistent policy.
- Noncorrectable owner drawing is intentional, but documented reversal remedy needs a reachable operation.

The final report will contain bounded, ranked findings with trigger, consequence, source anchors, evidence level, and repair direction; open concerns and untested surfaces will be explicit.
