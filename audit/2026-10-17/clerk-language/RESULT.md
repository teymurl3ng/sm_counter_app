# Clerk-language pass — complete at source / isolated-QA scope

17 October 2026. Baseline `6945500`. [Scope and judgment](PLAN.md); [exact wording inventory](COPY_MAP.json).

## What changed

Simplified the recent Credit, statement/share, customer-message and follow-up screens in 15 source files. Replaced composer/handoff, immutable change, server acceptance, policy and raw SET/CLEAR display codes with concrete actions and results. Current terms/review marks still apply while changes wait for sync. History labels distinguish opening an app, an uncertain attempt and a staff report from sending/delivery/payment evidence.

Examples: Pending server acceptance → Waiting for sync; Review message handoff → Review message attempt; Set aside local request → Remove from this device; Preparation history → Draft & share history; Flag for Owner review → Mark for Owner review. The switch now says Follow-up reminders, alongside explicit manual sending and separate-phone instructions.

Kept sale deadlines, calendar-month versus 30-day meaning, incomplete debt evidence, duplicate-contact warnings and offline-payment limitations. Checking may finish the original save; removing the device copy does not undo online changes. The final review uses “debt” for undated amounts, and “Could not confirm the app opened” for failed/uncertain history rather than claiming the app certainly did not open.

No changes to calculations, wire keys/enums, saved audit values, permissions, database/schema, scheduling or layout geometry. The only nonliteral source change maps existing SET/CLEAR values to display text; the unfinished-save message also stops printing its internal kind. Staff identifiers remain labelled IDs; staff identity redesign stays deferred. Existing PDF template and already clear Sale/notification labels were reviewed and retained.

## Evidence and bounded verification

- Final isolated app and Android-test APK build passed. Existing toolchain/deprecation warnings remain; no unrelated cleanup.
- 24 distinct pinned affected methods have passing evidence across the three runs below. Initial run: 23/24; the failed method matched both an underlying button and its dialog confirmation after the copy change. The test now selects the dialog button explicitly, retaining its outcome assertions.
- `wording-final`: 5/5, including the corrected confirmation selector and final statement/recovery copy.
- `wording-copy-final`: 5/5 after the last debt/history wording corrections and representative Credit-history fixture update. Only those affected checks were repeated.
- Source comparison verifies 13 files match exact quoted-copy replacements. The two recorded display deltas were manually reviewed. `git diff --check` passed.
- Inspected representative normal and dark/200% captures: Credit history, follow-up settings/confirmation, uncertain-message review and statement navigation. Text wraps/scrolls in existing containers; existing checks retain header/footer bounds and deliberate confirmation/recovery behavior. Fixture history now uses the same plain wording as the actual reader.

| Evidence | App APK SHA-256 |
|---|---|
| [Initial 24-method run](wording-first-results.json) | `3037d0e915dd224dbd746cc0563274bd5cc4d611097fb2b1c57896a6c902140e` |
| [Five recovery/statement checks](wording-final-results.json) | `191bcf78904d2873513b693f5d09c748b00806e1b285ad71d6a86952e09892ba` |
| [Five final copy checks](wording-copy-final-results.json) | `3f960f2ce2989c2362b80af100472db1657890d82f85cbb839d1dad64df79915` |

Only A06's disposable `com.saaf.milkshop.qa.tests` package was installed/reset. Its notifications were deliberately denied. No ordinary shop app update/reset, server operation, real-customer message or whole-app suite was performed. Raw captures/logs stay local and are excluded from the commit. Interrupted approval attempts caused no build/test execution; the later authorized attempts completed normally.

## Plan position and stop

✅ The explicitly deferred clerk-language pass is complete for this recent feature scope. Ordinary signed delivery remains phase 1. This does not complete human/private acceptance or phase 6's automatic-SMS route/cost decision. No further design or identity work was added to this pass.
