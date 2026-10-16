# Piece A — rules and native presentation foundation

16 October 2026. Baseline `6b0d666`. Scope: [A_SCOPE](A_SCOPE.md). No ordinary app entry or automation activation is claimed.

## Implemented

A pure calculator keeps Sunday statement review separate from each sale's payment deadline, merges reasons into one customer row, and uses accepted policy/customer revisions plus occurrence date for stable statement cycles. Monthly dates clamp without losing the chosen day; custom intervals remain anchored. It handles review repeats, activation dates, pause, snooze, inactive customers, payments/advances and unknown debt without inventing deadlines. Quiet-hour boundary calculation is included; no worker is implemented here.

The authenticated reader uses canonical customer balances and existing Credit status inside a read transaction. Leadership permission is required. It excludes private customer notes and makes no financial writes.

Injected native list/review/settings/customer-schedule panes reuse the app's surfaces, typography and fixed Customers host. They include search/reason filters, overdue versus full balance, dirty-exit confirmation, validation, retained failure feedback, read-only/offline behavior and a manual fallback explaining the separate dumb phone. Pausing preserves the accepted schedule. Prepare actions are injected callbacks; durable saves, shared handoff gating and ordinary routing belong to B.

## Evidence and corrections

The scoped selection contains 13 unique methods: five calculator, two real database/permission reader and six injected UI methods. Initial run passed ten methods; the reader class could not initialize because its JUnit setup inferred a non-void result, and the policy save interaction timed out. The setup now explicitly returns Unit. A corrected reader/settings run passed three of four checks; the remaining save interaction exposed the generic test Activity's keyboard pan behavior. The final UI fixture now uses edge-to-edge plus adjustResize, matching NativePreviewActivity's window configuration. That alone did not resolve the two save gestures: four of six UI checks passed. Explicit input completion then exposed Default IME semantics in the new interval field. The field now declares Done; the tests finish input, wait for IME dismissal, and exercise Save. This verifies that deliberate completion path, not every possible keyboard/touch race. Final outcomes and artifact hashes are recorded in A_VERIFICATION.json; retain initial/corrected summaries rather than erase failed attempts.

Source review independently found that choosing Paused discarded an accepted monthly/custom schedule. Fixed it and extended the existing schedule test to assert preservation. Final synthetic screenshot review also found that a shared pane scroll offset could hide the save notice after returning to the list. Each page now starts at the top and a save error scrolls into view; the affected tests assert both success and error visibility. All six new UI methods are rerun after this shared-pane change. No global keyboard/header changes were made. Synthetic capture review covers default light and dark 200% list/review/settings plus schedule/save interactions; scrolling content may leave the viewport while shared chrome remains fixed. These are native composable fixtures, not an ordinary-app end-to-end claim or physical tablet acceptance.

The independent-judgment hook output is saved in a-judgment-hook.json. It is a reminder, not a test or external approval. Judgment: the pure calculator and read-only adapter boundary are suitable for A; local callbacks/fake state cannot establish coordinated saves, deduplication, notification reliability or delivery. Keeping these screens out of ordinary routing until B is therefore required. No evidence supports claiming automatic reminders or SMS delivered at this checkpoint.

## Owner clarification and exact stop

Customer volume fluctuates. No fixed number or invented monthly budget is needed to complete A/B. C must use current eligible recipients, message encoding/segments and actual unit prices to estimate each run, with adjustable limits chosen at configuration. An unknown price cannot authorize unbounded automatic spending.

✅ A rules/read model/injected UI foundation complete: 13 unique affected methods have passing latest results across scoped runs, including the final 6/6 UI run. B remains: pin exact shared contract/role/retry/restore tests, implement shared state, quiet scheduling, notification attention and all coordinated handoff routes, then DEV and focused two-phone acceptance before an exact production approval. C remains the concrete unattended-SMS route/cost decision. No provider, schema, cloud, ordinary app install, real contact, full suite or unrelated feature was part of A. Phase 1's ordinary signed delivery and human/backup custody remain open.
