# Three presentation fixes - 14 October 2026

Implemented and installed in the regular signed app on A06 and S24. Broader business/server/staff acceptance remains deferred.

## Final changes

- Recovery alone omits the small merchant label from its outer access toolbar. The toolbar slot, recovery heading, one-time code, warning and acknowledgement remain; other sign-in steps retain their merchant label.
- NativeChromeBand moves only title text down 8 dp within its existing font-aware row. All role-owned headers inherit it. Status-bar spacing, total band height, metadata/day chip, content start and bottom navigation remain unchanged.
- The corrected PIN complaint concerns tapping **Quick PIN setup in Personal security**, not the sign-in keypad. The shared settings panel puts the editing card before the read-only phone status, removes its extra large in-body heading, filters the transient Auto-fill context-menu item on these two setup fields, and uses explicit Password/Next and NumberPassword/Done actions. Standard field editing, hidden values, 4-12 digit policy, identity-scoped drafts and existing enrollment/biometric callbacks remain.

## Evidence

Initial correct-target captures reproduced an Android Auto-fill popup and whole-page panning while the low PIN field was being brought above the keyboard. The final layout keeps it higher without changing the global IME policy. A window-mode experiment and a secure-field API experiment did not remove the shift and were reverted. The earlier misinterpreted sign-in transition experiment was also reverted; sign-in animation/keypad code is unchanged.

Six distinct affected A06 checks passed: header geometry at normal and dark 2x text; access/recovery geometry at normal and 2x text; real setup-keyboard trace; setup validation/submission/secret clearing with synthetic callbacks. The final setup trace covers direct tapping, repeated reopening, passphrase-to-PIN focus switching, no Auto-fill popup, field clearance and Done dismissal. PIN input rejects non-digits and a thirteenth digit without truncation; no actual account was enrolled.

S24 ran only the setup trace. Its first comparison exposed its taller keyboard still panning the header; removing the extra large in-body heading resolved that remaining clearance problem. Final A06/S24 traces assert no header panning (1-pixel tolerance): A06 61 sampled draw frames, header bottom always 298 px; S24 196 frames, always 511 px. Reviewed final opening/reopening captures show field clearance and no popup. This proves the selected physical cases, not every keyboard/device/font combination.

The initial header assertion used a wrong fixture name and was corrected before acceptance. Initial compilation issues and diagnostic runs are retained. Existing required Gradle packaging/lint/asset tasks ran; no full test suite, server mutation, real enrollment, staff rehearsal or shop-day test was added.

## Installed candidate

Regular com.saaf.milkshop 0.4.2/code 6, non-debuggable, preview fixtures disabled, existing release certificate unchanged. Both normal-profile updates used install -r, without uninstall or app-data clearing, and their installed APK hashes match the final signed artifact. Both launcher activities were opened. QA/protected profiles were not replaced.

The build is based on pushed commit 3579bf5 plus the three recorded working-tree presentation files. These latest source changes have not yet been committed/pushed. PACKAGE_MANIFEST.json, VERIFICATION.json and INSTALLED.json record the hashes; APK/log/capture files remain excluded from Git. No raw business data was read to claim upgrade/business acceptance.

## Scope preserved

No changes to the sign-in keypad/transition, database, recovery-code generation, PIN verifier/rate limits, server reset/recovery tooling, landscape policy, global keyboard/window settings or broad theme/list design. This repair belongs inside finish step 7. Steps 1-3 remain accepted; step 4 human checks, step 5 hosted recovery, step 6 final publication/acceptance and step 7 setup/custody/handover/broader acceptance remain pending.

Relevant Android API: [context-menu filtering](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/contextmenu/modifier/filterTextContextMenuComponents.modifier). This supports removing a specific menu item locally; it does not require disabling Android's autofill service. The 8 dp title offset and editing-card order are app design choices.
