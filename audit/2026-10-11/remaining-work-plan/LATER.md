# Scope boundary

No new functional, visual, security or performance investigation beyond the reconciled backlog and necessary dependency reads was opened.

- Resolve exact remaining failure-pane count only when the management presentation batch starts; the old approximate fourteen is not a new count.
- Remote Viewer/server publication and provider state remain preflight work. No extra connector, secret discovery or account inspection was performed.
- Layout opportunities in the earlier source maps remain candidates until their own representative before capture. No whole-app physical crawl.
- General retail expansion, Credit/reminders, Urdu, approval queue, centralized identity migration and cleanup are later named work, not silently appended to the immediate implementation run.
- Existing current menu/chrome/PIN/settings changes are preserved. No landscape work is owed.

Record discoveries during execution with file, observed behavior and next-task ID. Do not follow them unless needed to finish the active batch's stated acceptance criteria.

## Batch 1a discovery — B5 SQL balance-rounding boundary

`phase18/app/src/main/java/com/saaf/milkshop/data/Entities.kt:367` defines account_balances with `ROUND(COALESCE(SUM(fl.amount), 0), 2)`. After the native Sale conversion repair, a single raw ledger movement of Rs 2.125 produced a 213-paisa receipt but a 212-paisa cash balance on the JVM framework SQLite driver. The first post-fix assertion failure is preserved in `../rounding-batch-1a/first-after-results.xml`.

Next bounded task: reproduce that literal on the production bundled SQLite driver, decide the shared account-balance rounding boundary, and review exact view/migration/cash-count compatibility before changing it. Preserve historical ledger rows. Do not claim a production-device defect solely from the framework-driver result, or call B5 complete. This is related remaining correctness work, not a new feature or an invitation to rewrite all money storage.

Follow-up completed on A06, bundled SQLite 3.50.1: 2.125 rounds to 2.13, but 1.005 rounds to 1.00. A literal quantity sale (0.5 L at Rs 2.01) confirmed receipt/Activity 101 paisa versus cash 100 paisa and expected count Rs 1.00. See ../rounding-bundled-check/RESULT.md and REPAIR_BRIEF.md. This supersedes the unverified-device qualification for the newly reproduced 1.005 case; it does not generalize the host's 2.125 behavior to production. B5 is still partial and is the next bounded monetary repair.

B5 follow-up closed by ../canonical-cash-rounding/RESULT.md: cash/count/close now share decimal HALF_UP on the pinned bundled driver, with legacy close policy retained. The old Robolectric SQLite parser cannot reliably distinguish some adjacent binary values; the unchanged oracle is verified on the actual bundled driver. No raw-money storage redesign is included. Matched versions are required for two writable devices before the v2 close-policy rollout.

2026-10-12 bounded discovery for batch 5/U4 wording: NativeOwnerRecordsFacade.requireRecordsHost permits Owner/Manager/Admin, but its denial text names only Owner/Manager. Permission behavior is retained; align the message during that mapped Records presentation batch. No additional route audit was opened.
