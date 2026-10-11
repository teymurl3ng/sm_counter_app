# Bounded UX validation — 11 October 2026

User requested narrow goals and explicitly deferring discoveries. Existing authorization continues; A06 isolated package only, preserve five protected files.

Goals:
1. qaFast on the saved final edits.
2. Investigate only the retained Collect 150% text landscape navigation failure; one targeted correction, actual screenshot/semantics verification.
3. Verify five cashier safeguards: normal close/denied reopen, changed cash after count, same-date pending delivery, earlier-open pending delivery, and cashier UI close path.
4. Recheck two already-corrected assertions: empty catalog and standalone Activity navigation.

Eight selected device tests total: Collect landscape + five cashier tests + empty catalog + Activity. At most one correction/rerun after initial validation. If unresolved, report the remaining blocker and preserve diagnostics.

Excluded: packaged-web contract mismatch; remaining broad-suite fixture checks; new redesign/features; 591-test checkpoint; exhaustive motion/haptic/TalkBack acceptance; ordinary-package install/release. New findings go to NEXT_PASS.md without investigation here.

Pass completion: saved targeted changes, qaFast result, selected test counts, fresh Collect evidence, truthful residual limitations. Repository requires qaCheckpoint before overall screen/shared-helper acceptance; this pass does not claim that acceptance. Owner clears screenshot UNREVIEWED.
