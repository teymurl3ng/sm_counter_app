# Native UX implementation and acceptance

User approved the repair scope and automated QA on 10 October 2026. Implementation is in the current checkout. Release acceptance is pending the full checkpoint and physical evidence review.

## Changes

- Authentication keeps its identity band mounted while changing the step body. Status-bar, navigation-bar and keyboard insets apply to the recovery flow.
- Five role shells own one header and one navigation surface. Panes crossfade for 180 ms without scaling the whole page. Outgoing panes keep snapshots but relinquish Back, accessibility and touch input. Refresh completions merge into the latest route and draft state.
- Shared rows use a distinct surface, rounded boundary and stronger supporting text. Current measured light-theme contrast is in `audit/2026-10-10/ux/current_token_contrast.tsv`.
- Submenu, selection, submit and destructive callbacks use the shared haptic vocabulary. System touch-feedback preference is respected. Success feedback is deduplicated by receipt identity. These code checks do not establish perceived vibration quality on either phone.
- Sales read all active catalog products and prices by product ID. More than six products use a searchable chooser, keeping quantity and payment fields reachable. Missing prices remain visible and require a valid explicit wholesale price before posting. The existing Liters/Kg inventory and dairy costing model remains; this repair does not establish universal retail accounting support.
- Welcome and role headers observe shop settings. Displayed merchant identity is separate from the signed-in actor. Product identity is Counter Ledger; cryptographic namespaces and package identifiers remain stable.
- Actual operations drive busy and outcome feedback. Receipts remain until an explicit next action. Catalog/settings/security/costing/backup writes publish saved outcomes after persistence; a later readback failure states that the save succeeded. Failure notifications retain inline error context. Fake welcome progress was removed.
- List/detail/create/edit/settings/access/receipt panes preserve their outgoing data during transitions. QA preview account notices now have explicit dismissal.

## Daily work policy

A cashier can count the opening drawer or handover and close the business date without waiting for an absent Owner. New dates are already open automatically; this is a whole-business-date close, not a new register-session subsystem. Closing requires the latest cash count, no changed expected cash since that count, a reason for variance and resolved or archived pending deliveries. Confirmation names the date and earlier-date coverage. Only Owner/Admin may reopen with the existing reason and full-passphrase authorization. The cash-close actor remains in the audit trail.

The delegated cashier start/end policy follows configurable employee permissions in [Shopify POS](https://help.shopify.com/en/manual/your-account/users/roles/permissions/pos-permissions) and [Lightspeed Retail](https://retail-support.lightspeedhq.com/hc/en-us/articles/229129608-Setting-up-employee-roles-and-access). The delivery safeguard is specific to this app's date lock; it is not claimed as an industry-wide rule.

## Wrap-up status

Stopped further validation at the user's request to conserve usage. Changes are saved in this checkout; this is not a release acceptance or a completed all-screen motion audit.

- Latest completed `qaFast`: PASS. Unit results: 94 total, 93 passed, one pre-existing skipped test. Android and instrumentation compilation passed. A final status-icon expression correction and test-copy correction were made after that build and have not been rerun.
- Completed `qaCheckpoint` candidate: FAIL, 591 device tests, 575 passed, 16 failed, none skipped. The complete XML, logcats and failure list are retained in `audit/2026-10-10/ux/checkpoint-candidate1-results`. This run predates the final correction batch.
- Latest affected `qaScreen=uxfinalfixes`: INTERRUPTED at the user's wrap-up request. Last console progress: 26/56 completed, 3 failed. This is not a passing suite. The remaining diagnostic failures are the old empty-catalog text expectation, a packaged web/native contract mismatch, and large-text landscape navigation visibility. The empty-catalog assertion now follows the new visible message, retaining the disabled-post assertion, but has not been rerun.
- Earlier focused UX results were 44/45; the remaining cashier close test subsequently passed independently 1/1 after dismissing its QA-only account notice. Asset checks passed 3/3 and corrected Back routes passed 3/3. These earlier results do not substitute for the final checkpoint.
- Inventory: 1,001 control rows, zero untested/orphan rows. This is a static coverage gate, not proof that every screen animation or physical interaction passed.
- Retained candidate evidence: eight candidate-one PNGs plus one new partial-run landscape diagnostic; all nine remain UNREVIEWED. The partial evidence directory also contains earlier captures and must not be presented as fresh proof for the latest source. Auth/recovery and Sale still need current dedicated export/review.
- Isolated motion probe: 30 transitions, guard-ownership assertions passed. Synthetic frame totals: median 18.73 ms, p95 111.29 ms, 30 callbacks over reported platform deadlines. This is diagnostic evidence, not an app-wide smoothness pass.
- Protected preservation was rechecked at wrap-up: all five pre-existing protected domain/data edits match the approved baseline byte-for-byte. Frozen specification/SQL baselines were not edited. Canonical instrumentation target: `com.saaf.milkshop.qa.tests`; interactive QA: `com.saaf.milkshop.qa`; motion probing: `com.saaf.milkshop.qa.benchmark`. A06 `R8VXA02SNKH` was the only device mutated. The ordinary package was never installed or modified by this work.

## Historical work needed before acceptance — superseded by 11 October completion

1. Resolve the packaged WebView contract mismatch. Its diagnostic page says: “App components do not match. Reinstall a complete Saaf Milk build.” Preserve contract verification; do not weaken it to make the legacy test pass.
2. Investigate the captured 150% text landscape navigation failure in Collect. Do not treat it as resolved by the successful portrait tests.
3. Rerun the interrupted affected suite and cashier close regressions, including the new earlier-open delivery safeguard. Six Owner entry test fixtures now register and exercise the actual leave guard; the completed portion of the focused rerun showed no further failures in those cases, but the entire suite remains interrupted.
4. Run one final checkpoint against frozen final sources. Export fresh Auth/recovery, Sale, Collect and representative role evidence. Review motion and actual vibration on a device; still screenshots and vibration API calls cannot establish these.
5. Owner review clears screenshot UNREVIEWED under `NATIVE_SCREEN_ACCEPTANCE.md`; until then visualAcceptance remains BLOCKED. No ordinary-package release has been prepared or installed.

## Acceptance boundaries

Screenshot export now verifies the exported file rather than silently accepting a failed private-cache copy. Evidence sidecars retain `UNREVIEWED` until Owner review under `NATIVE_SCREEN_ACCEPTANCE.md`. Still images alone cannot prove motion quality. The initial 71-entry source inventory is preserved as a baseline; broad runtime coverage and missing physical matrix axes will be reported separately. Haptic feel, TalkBack interaction and exhaustive per-screen viewport/re-entry combinations must not be claimed from compile/test counts.

## Bounded follow-up — 11 October 2026

Current qaFast PASS (93 unit passes, one pre-existing skip); qaScreen[uxbounded] PASS (8/8 A06 tests). Standalone Collect now mounts its existing adaptive scaffold; 150% landscape form/navigation checks pass. Five cashier close safeguards, empty catalog and Activity navigation passed. No new full checkpoint ran; overall acceptance remains pending. Fresh landscape evidence: one PNG, one UNREVIEWED sidecar. No protected edits or ordinary-package changes. New observations were deferred without investigation. See `audit/2026-10-11/ux/RESULTS.md`, `BOUNDED_SCOPE.md` and `NEXT_PASS.md`.

## Fixed-scope completion — 11 October 2026

This status supersedes the historical wrap-up and remaining-work lists above. User clarified that phones are portrait-only and deferred capsule/menu styling. Packaged web assets now match the strict native contract, A06 portrait lock is verified, Auth/recovery evidence is current, and the known ten-cycle re-entry and actual OS haptic checks passed. No capsule/theme edits were made during this pass.

qaFast PASS; full qaCheckpoint FAIL (593/596, three failures); single focused qaScreen[uxcorrection] PASS (13/13), resolving every failing case. Production source stayed unchanged between those runs. Formal checkpoint is not relabeled PASS. Current complete-run evidence: 14 PNGs/14 UNREVIEWED plus one route MP4; Owner visual and felt-haptic review remain. Inventory 1,001 controls, untested=0, orphans=0. Five approved pre-existing protected files remain byte-identical; ordinary app installation untouched.

The bounded pass is closed. Remaining acceptance: Owner review and a later fully green checkpoint; then the deferred capsule/menu overhaul. Full results: [audit/2026-10-11/finish/RESULTS.md](audit/2026-10-11/finish/RESULTS.md). Review bundle: [FINAL_REVIEW.md](audit/2026-10-11/finish/FINAL_REVIEW.md).


## Interactive QA handoff — 11 October 2026

Latest full checkpoint: 594/596, FAIL. Single focused correction run: qaScreen[uxready] PASS, 8/8; both failing cases pass after test-fixture corrections. No production edits in this correction batch. Portrait-only and deferred capsule styling remain in force. Owner review remains pending for 14 screenshots and physical motion/haptics. See audit/2026-10-11/ready-to-test/RESULTS.md and TEST_ON_PHONE.md for exact results and interactive QA handoff.

## Capsule redesign — 11 October 2026

The user subsequently approved the capsule audit and redesign. All 62 native screen-source entries now have recorded treatments, all 36 existing shared row surfaces were reapplied, and mapped missing equivalents were added. Shared inner insets and page gutters are consistent, forms/records use flexible groups, and the reproduced Cashier blind Count cash overlap is fixed. Portrait-only phones, role permissions and money/auth behavior are preserved.

qaFast PASS; full qaCheckpoint FAIL, 596/600; final expanded qaScreen[surfaces] PASS, 48/48, including all four failing cases after scroll/IME test-interaction corrections. Production source remained unchanged between the full run and focused retry. Formal checkpoint is not relabeled PASS. Inventory 1,001 rows, untested=0/orphans=0. Full-run evidence: 24 PNGs/24 UNREVIEWED and one known-route recording; representative coverage, not exhaustive per-state visual acceptance. Protected files remain identical to the approved baseline; ordinary app untouched. See audit/2026-10-11/capsule-implementation/RESULTS.md, REVIEW.md and HANDOFF.json for the QA update and remaining acceptance boundaries.


## Stable app bars — 11 October 2026

Unified top/bottom geometry through one shared header and navigation implementation; 69 work-header calls/63 screen-source files mapped. Access/Welcome share the top-band rule, account names no longer expand access headings, and Activity detail refresh no longer inserts layout height. Fixed font-sample metrics support enlarged text without destination-dependent sizing. Existing portrait lock, callbacks, guards and financial/auth behavior are preserved.

qaFast PASS; final geometry/text-layout tests 9/9 PASS; full frozen-source checkpoint FAIL, 606/609, with all three failed payment-navigation cases passing unchanged focused reruns. Separate fresh-owner UI test PASS, 1/1, after a test selector correction. Formal checkpoint is not relabeled PASS. Inventory 1,001, untested/orphans=0. Fresh full-run evidence: 33 PNGs/33 UNREVIEWED plus one route MP4; Owner visual acceptance pending. Protected files unchanged; ordinary app untouched. See audit/2026-10-11/chrome-unification/RESULTS.md, REVIEW.md and HANDOFF.json.
