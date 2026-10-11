# Capsule redesign — QA handoff

Updated interactive QA APK installed successfully with install -r on A06 R8VXA02SNKH. Left open at Preview Cashier → More → Count cash with blank fields. Existing accounts and QA first-install time are preserved; ordinary app metadata is unchanged. Download/copy Saaf-Milk-QA.apk only for the isolated QA package. Actual installed-app after captures are inspection/interactive-cashier-more.png and inspection/interactive-count-cash.png, with UNREVIEWED sidecars.

Implementation covers the mapped native screen families. All 62 source entries are accounted for, all 36 previous shared capsule callers were reapplied, and 48 shared surfaces now use the common variants. Role wrappers inherit shared content. See SCREEN_TRACKER.csv, changed-native-sources.json and surface-call-audit.json for exact coverage.

Confirmed fixes: inner text padding is owned consistently, menus and lists have exterior gutters, records/forms use grouped or flexible layouts, picker selection retains its indicators, and Cashier blind Count cash no longer overlaps its controls. Phone portrait lock and money/auth policies are unchanged.

## Validation

- qaFast: PASS, 93 unit passes and one existing skip; inventory 1,001 rows, untested=0 and orphans=0.
- Initial affected surfaces run: 34/34 PASS. Three missed Owner list gutters were corrected afterward and included in the frozen full checkpoint. affected-summary.json records this ordering.
- Full qaCheckpoint: FAIL, 596/600 passed, no skips. The 23-minute run is retained unchanged in checkpoint-results and checkpoint-console.txt. It is not relabeled PASS.
- Four failures: Admin expense entry/back tests tapped a shortcut without scrolling to its new position; Overview expected a lower section above the fold; Owner expense navigation raced IME dismissal. Test interactions were corrected without further production changes.
- Final qaScreen[surfaces]: PASS, 48/48, including all four previously failing cases and complete Owner Overview/expense contract classes. All final production changes are covered by this run. See correction-summary.json and correction-results for exact proof. No additional production/test edits followed it.
- An earlier aborted checkpoint was intentionally stopped to finish three list gutters. checkpoint-aborted-note.json retains that provenance; it does not count as validation.

Full checkpoint source hashes matched before the corrections. Corrections affect test interactions and the focused test map only. Full-run coverage plus successful focused retries can resolve these reported cases, but a fully green single checkpoint remains a formal acceptance item.

## Evidence and boundaries

The full checkpoint produced 24 PNGs with 24 UNREVIEWED sidecars, plus one route recording. Original evidence is preserved in checkpoint-evidence/checkpoint-current. Later focused exports are retained separately; they must not be described as a new full checkpoint. See REVIEW.md for representative before/after images and TEST_ON_PHONE.md for the phone review.

Representative image inspection found consistent menu insets and separate Count cash fields. Automated tests cover shared geometry in light/dark/default/150% fixture text, long content, selected state, physical Count cash submission and the established workflow matrix. They do not prove exhaustive visual quality across every screen state, perceived motion smoothness or felt haptics. Owner visual acceptance remains BLOCKED until review under NATIVE_SCREEN_ACCEPTANCE.md.

Five pre-existing protected files match the approved execution baseline byte-for-byte. No protected/domain edits were added. Ordinary com.saaf.milkshop installation metadata is preserved; interactive QA updates use com.saaf.milkshop.qa with install -r and no data reset. Only A06 R8VXA02SNKH is mutated. The final APK identity, hash and package observations are in HANDOFF.json.

Scope is closed to further design/business investigation. Remaining acceptance: Owner review of the redesigned app and physical motion/haptics, dedicated visual checks of states not captured here, and a later fully green single checkpoint. New unrelated findings belong to the next pass.
