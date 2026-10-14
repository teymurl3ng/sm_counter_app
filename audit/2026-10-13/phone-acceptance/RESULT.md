# 9c1 — Representative A06 phone checks accepted

Seven distinct selected methods are accepted on A06 (SM-A065F, Android 16, 720×1600, density 300, system font scale 1.0). System touch feedback is enabled. The run is limited to the named phone checks, not whole-app visual/motion/accessibility or uncoached staff acceptance.

## Verified behavior

- Portrait remained locked under a rotation request. Existing test cleanup returned the device to natural orientation and unfroze rotation; no landscape layout work was added.
- Keypad/passphrase/account Back, account change and visible authentication-error routes passed. Another account's PIN did not unlock the selected Owner; that Owner's valid PIN opened its own account. These use actual isolated Room/security paths, not a fake PIN verifier.
- More and Close-day submenu clicks passed the device vibration-history check, respecting the system feedback setting. This is hardware dispatch evidence; perceived strength/comfort remains a human check.
- Ten Receive milk → Back → Delivery → Back cycles kept the named Owner chrome and returned to Work; final receipt-entry fields remained stacked without overlap. Settled Receive, Delivery and Products captures were reviewed. The screenshots do not prove there was no flash between frames, nor do the two entry routes certify all menus.
- The real Android gate reported **Phone security**, available. Its setup prompt title was observed twice by UIAutomator, physical Back cancelled both, and full business rows plus the existing encrypted Quick unlock record stayed unchanged. No phone credential or fingerprint was entered. A fresh disposable QA UUID key was removed in cleanup. This covers the native gate's setup cancellation; the existing controlled-gate routing evidence remains separate from successful enrolled-account release.
- Sale entry's real OS keyboard appeared before its Done action; after Done the field lost focus and the keyboard disappeared. Before/after whole-window captures show the focused quantity field above the keyboard, then the unobstructed sale body. This is one Sale field/keyboard, not every keyboard layout or submenu input.

## Test-only repair and exact run status

The first seven-method run took **61.439 seconds**, with **6 passes / 1 failure**. The keyboard method's default merged-tree finder could not locate `cashier_entry_input`; the failure explicitly reported one matching node in the unmerged tree. Source inspection confirmed the tag and `KeyboardActions(onDone = { focusManager.clearFocus() })` on the actual BasicTextField. No app keyboard defect was reproduced.

Only `saleEntryDoneActionClearsKeyboardFocus` in NativeFoundationUiTest was repaired to use that tagged unmerged node. It now waits for actual Android IME visibility before Done and absence afterward, and captures both states. The first repair compile accidentally chained a focus assertion onto the Unit-returning IME action; split those calls before rerunning. The final isolated build passed in **13 seconds**. Only this affected method was rerun, and passed in **9.83 seconds**. Six other checks remain credited from their first passing run; the initial run is not reported as 7/7.

The inverse method repair reconstructs the exact pre-run test-source hash, verifying no other existing method changed. The new NativePhonePromptAcceptanceTest, existing bootstrap runner and main APK hashes match across runs. All main-source hashes are unchanged. Exact selections, original failure, final run, build logs, package metadata and source/APK hashes are saved here. No full suite ran.

## Evidence limits and device/data boundaries

The native security screenshot is black except system bars because the secure window blocks capture. It is retained and explicitly classified as visually unreviewable; it is not evidence of how the prompt looks. The UIAutomator title observation and cancellation callbacks are the actual functional evidence. A06 did not report strong biometric availability in the app's label; successful fingerprint authentication and human prompt appearance/feel remain open.

Six synthetic app captures (portrait, Receive, Delivery, Products, keyboard before/after) were reviewed. Original generated sidecars remain untouched; a separate review record records the observations and actual system font scale. No 150%/200% system setting was applied. Earlier keypad/management larger-text fixtures stay credited; actual larger-text usability, known chrome abbreviations/truncation and TalkBack are not accepted by this batch.

SaafMilkTestRunner bootstrap was enabled only for these Activity UI tests, creating/enrolling synthetic local users/PINs inside `com.saaf.milkshop.qa.tests`. The new prompt test preserved business records and existing Quick unlock enrollment across its own calls. Only the isolated app/test APKs were installed; ordinary, preview and `.qa` package versions/install timestamps remain unchanged. No ordinary databases or credentials were read. Actual phone authentication availability and display/haptic settings were inspected read-only; phone credentials, biometrics and global text size were unchanged. No remote Auth/provider/Drive/server operation or release signing/publication ran. Existing unrelated dirty work remains.

## Whole plan and next bounded work

1. Money/withdrawals — complete at bounded QA scope.
2. Receiving sync — complete at bounded QA scope; physical convergence is separate if two devices write.
3. Mapped menus/lists — complete at bounded QA scope; human appearance acceptance remains.
4. Access/recovery/setup — local checks/copy complete; hosted recovery/reset gates and intended-device enrollment/handover remain open.
5. Shop-day/phone acceptance — 9a0 oracle, 9a1 automated day and **9c1 seven representative phone checks complete**. Uncoached staff 9a2, human motion/haptic/prompt/large-text/TalkBack observations and conditional two-physical-phone 9b remain open. Use [HUMAN_CHECKS.md](HUMAN_CHECKS.md); do not classify automation as a human pass.
6. Freeze/commit/push/sign/install — pending final accepted source/build, recovered-signing/upgrade evidence and ordinary-device handover.

Next only prepare the disposable staff-rehearsal handover around the existing task cards and expected fixture, after narrowing its setup boundary. Human-only observations can be collected alongside that handover. No broad audit, new visual design or repeated full test run is needed because this batch passed.
