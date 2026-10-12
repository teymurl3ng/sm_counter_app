# B2b/B10 planning replay and restore result — 2026-10-12

Status: complete at bounded QA scope. Phase 2 receiving work is complete at this scope; physical two-phone acceptance remains phase 5.

## B2b historical planning replay

Older Android receivers consumed server sequences while skipping `budget_plans` and `collections_targets`. After a successful ordinary pull, the new one-time `SyncPlanningCatchup` scans current server rows from sequence zero, filters those two tables, and applies them through the existing receiver. Its completion key includes environment, server URL, shop, and installation identity. It leaves the ordinary cursor and local outbox untouched. Late parents recover through parking; a queued local edit remains local and parks the server version. Network and semantic refusals leave catch-up pending for retry. Repeated rows are now reported as already present, separately from refused rows, so a resumed replay can complete.

## B10 planning-only restore

The isolated pre-fix fixture reproduced the defect: planning-only restore left a queued entry for a deleted local plan (`expected 0, was 1`), and restore capture could enqueue the backup plan as a fresh local edit. The fix drops sync capture during selected planning replacement, clears only planning outbox/park/review rows, and restores capture before commit. Unrelated queued business edits remain. On re-enrollment, the restored planning row enters the existing held live comparison; a differing server row remains held for review. The ordinary cursor remains unchanged until enrollment's existing reset-to-zero path. Full business restore continues to use its prior classified path.

## Verification and limits

Isolated `com.saaf.milkshop.qa.tests` APK on S24 (`RFCX116CYYX`), built with `-Psaaf.instrumentationSandbox=true`. The affected planning receiver, planning catch-up, planning-only restore, profile catch-up, one full-restore compatibility case, and two apply/report compatibility cases passed together: `OK (15 tests)`. After adding the final differing-server classification assertion, the planning-only restore fixture passed again: `OK (1 test)`. The earlier B2a receiver/capture run passed `OK (4 tests)`. `git diff --check` passed. No broad suite, physical two-phone test, ordinary-app install, or server change was performed.

Deployment boundary: both writable phones need an Android build with the receiver. An older build still skips planning rows. The one-time replay uses the server's current per-UID rows; it does not reconstruct server versions no longer retained by that transport. Planning conflicts and held differing rows require owner review under the existing sync review workflow.
