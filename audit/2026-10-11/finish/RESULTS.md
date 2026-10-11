# Bounded completion result — 11 October 2026

The approved implementation and bounded verification pass are finished. Phone design remains portrait. Capsule/menu styling is unchanged and deferred. Formal release acceptance remains pending; this is not a claim that every physical screen state has been filmed or approved.

| Step | Result |
|---|---|
| 1. Packaged web compatibility | Rebuilt current assets without weakening verification. Existing packaged paid-sale test passed against Room. New cleanup gate rejects the retained stale assets. |
| 2. Portrait | Actual A06 rotation request remained portrait. Large-text phone test now uses portrait. No landscape redesign. |
| 3. Corrected regressions | All six Owner discard-guard cases passed in the full run. Cashier close safeguards, retained receipts, catalog, identity and saved-domain cases passed. Three full-run failures were corrected and passed in the single focused rerun. |
| 4. Auth/recovery evidence | Six fresh light/dark account, passphrase-error and recovery captures. Recovery title clears status bar; code is a synthetic demo. Actual auth/domain behavior is covered separately. Owner review pending. |
| 5. Motion | Known ReceiveMilk/Deliveries entry/back routes repeated ten times each. Transition/state-race cases passed. Fixed 60-second route video recorded on A06 at exact test-start marker. Visual motion review pending. |
| 6. Haptics | Source callback inventory preserved. A06 OS history confirms finished CLICK effects for actual More navigation and Close-day submenu; disabled-system-setting suppression passed. Felt strength/quality requires human review. |
| 7. Checkpoint | One full device run: 596 cases, 593 passed, 3 failed, none skipped. One subsequent focused run: 13/13 passed, including all three failures. No further full run was started. |

## Exact gates

- qaFast: PASS. 94 unit cases: 93 passed, one pre-existing skip. Frozen baselines, bridge/assets, native inventory and compilation gates passed.
- qaScreen[uxfinish] preflight: 16/18 PASS; two keyboard-hidden Owner navigation fixtures corrected before the checkpoint, both passed there.
- qaScreen[uxcorrection]: PASS, 13/13 device cases.
- qaCheckpoint: FAIL in the complete run because three device cases failed. Those cases subsequently passed, but the original checkpoint is not relabeled PASS. All 596 named device cases have a passing result across the complete run and focused rerun; that is not a fresh single-run checkpoint PASS.
- Inventory: 1,001 controls; untested=0, orphans=0.
- Current complete-run evidence: 14 PNGs, 14 UNREVIEWED sidecars, one route MP4 UNREVIEWED. visualAcceptance=BLOCKED until Owner review.
- Protected logic: five pre-existing protected files preserved byte-for-byte; approved allow.logic=true used. No new protected edits. SQL/spec baselines unchanged.
- Only A06 R8VXA02SNKH and isolated com.saaf.milkshop.qa.tests were used for device changes. Ordinary com.saaf.milkshop was not installed, cleared or changed by this work.

## Corrections after the full run

The milk-entry setup timed out before saving, the sale callback assertion failed, and collection's saving text was outside the displayed viewport. The focused fixtures now wait for a dismissed IME before physical scrolling/tapping; milk setup additionally verifies the selected supplier and exact entered values. Collection explicitly scrolls the saving control into view. Money requests, physical-loss proof, and duplicate-submit assertions remain intact. The rerun passed all three and adjacent screen contracts. Keyboard/layout timing is the supported working explanation; this pass does not prove every earlier failure had one cause.

Post-checkpoint changes were limited to four test files and the Gradle focused-test group. Production source and packaged assets stayed unchanged. Sources were frozen during both runs. One launch was stopped before any device tests because of a wrong relative edit path; it is retained as setup diagnostics, not counted as a device test pass.

## Remaining acceptance work

1. Owner review of the fresh images and route video; feel the haptics on the phone. See FINAL_REVIEW.md.
2. A completely green qaCheckpoint on a later acceptance run after review. It was deliberately not repeated within this pass's one-checkpoint/one-rerun budget.

After that: the capsule/menu design overhaul requested by the user. Tablet/desktop and exhaustive physical accessibility/motion matrices remain separate scope. No unrelated defect investigation was added.

Full XML/logcats, source hashes, evidence provenance, stale-assets rejection, OS haptic history and both result summaries are retained in this folder. Historical preflight evidence is not counted as the complete checkpoint's current evidence.
