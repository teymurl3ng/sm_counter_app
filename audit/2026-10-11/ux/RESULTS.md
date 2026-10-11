# Bounded pass results — 11 October 2026

Completed the agreed bounded pass; no full checkpoint or additional investigation was started.

## Change

Collect's standalone screen now calls its existing adaptive scaffold and shows its header. Its navigation respects saving and the entry leave guard. The existing hosted-scaffold/header providers retain one navigation surface/header inside CashierPreviewHost. The retained failing tree had only the body and no navigation nodes; this explains that particular landscape failure without claiming the production cashier host lacked navigation.

The landscape test now verifies all four navigation actions are displayed/clickable at 150% composition text, plus amount and Record remain scroll-reachable. It exports fresh whole-window evidence.

## Checks

- qaFast: PASS against current sources. Unit tests: 94 total, 93 passed, one pre-existing skip; zero failures/errors.
- qaScreen[uxbounded]: PASS, eight device tests, eight passed, zero skipped/failed. Build plus device run: 1m 55s.
- Five cashier cases passed: normal close/new-date opening/denied reopen; cash movement after count refuses without partial write; pending same-date delivery refuses then Cashier resolves/closes; earlier-open pending delivery refuses then Cashier resolves/closes both covered dates; complete Cashier UI count/cancel/confirm/close flow with audit data.
- Empty-catalog feedback/disabled posting and standalone Activity selected navigation passed.
- Inventory: 1,001 control rows, untested=0, orphans=0. Static control coverage is distinct from full physical UX acceptance.
- qaCheckpoint: NOT RUN in this bounded scope. Last completed candidate remains FAIL (575/591), predating corrective edits. No claim of overall screen/shared-helper acceptance.
- Fresh evidence=1; unreviewed=1; visualAcceptance=BLOCKED pending Owner review. The screenshot was inspected, observations retained, and remains UNREVIEWED. Earlier-run evidence remains separately retained.
- Five pre-existing protected edits remain byte-for-byte identical; no protected domain/data edit in this pass. Approved allow.logic exception used for checks. Frozen baselines passed. Ordinary package not installed/modified; only A06 R8VXA02SNKH isolated com.saaf.milkshop.qa.tests instrumentation.
- Main-source hash manifest matches source after test completion. Result XML/logs are retained in bounded-results; device-summary.json enumerates the exact eight tests.

## Scope boundary

NATIVE_SCREEN_ACCEPTANCE.md requires qaCheckpoint “before accepting a screen or shared helper as done” and says “Owner review clears UNREVIEWED.” This pass supplies focused functional verification only, preserving that acceptance boundary.

Packaged-web mismatch, broader regression checks and physical review remain deferred in NEXT_PASS.md. Newly observed landscape side-inset/header-space concerns are recorded there without expanding this pass.
