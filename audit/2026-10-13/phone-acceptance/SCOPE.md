# 9c1 — Representative A06 phone acceptance

One phone, seven named methods, one new instrumentation file. No production edit, full suite, theme redesign, ordinary enrollment/database, phone credential/biometric change or server operation.

Existing NativeFoundationUiTest methods selected:

1. `phonePortraitLockSurvivesRotationRequest` — portrait stays locked; restore rotation state through its existing finally block.
2. `accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts` — keypad/passphrase/account Back and authentication error routes.
3. `quickPinRejectsAnotherAccountsPinAndUnlocksTheSelectedAccount` — real isolated PIN verification stays with the chosen local account.
4. `submenuClicksReachTheDeviceVibrator` — actual device vibration history for More and Close-day submenu, respecting system touch-feedback setting. It cannot certify how a person feels it.
5. `uxRepeatedOwnerEntriesKeepChromeAndCaptureScreens` — exactly ten repetitions of Receive milk and Delivery under Owner chrome. Retain final settled screenshots; do not claim a PNG proves absence of transient flashing.
6. `saleEntryDoneActionClearsKeyboardFocus` — actual activity entry focus clears via its IME action. This is narrower than all keyboard layouts/obstruction or TalkBack usability.

New `NativePhonePromptAcceptanceTest#realPhoneSecurityPromptBackCancelsWithoutEnrollment`: query actual Android authentication availability. If available, call the real AndroidQuickUnlockDeviceGate with a disposable UUID key and synthetic bytes, observe the Android setup prompt, press physical Back and assert cancellation twice. Cleanup only that key, prompt and coroutine; do not enter phone credentials or authenticate a fingerprint. Compare the test app's business rows and existing quick-unlock record before/after. Save capability/result metadata and a screenshot when available. If unavailable, record that fact and verify Unavailable without reporting a displayed/cancelled biometric prompt.

Use the existing instrumentation sandbox build/package. The existing UI tests require SaafMilkTestRunner bootstrap enabled, which creates/enrolls synthetic fixture accounts/PINs only inside `com.saaf.milkshop.qa.tests`; no ordinary production, preview or `.qa` package is installed or cleared. No `pm clear` is planned, since the existing isolated fixture is already disposable. Record package metadata and source/APK hashes before/after. Do not take ordinary credentials or records as test fixtures.

A06 is connected. No S24 or second phone is needed for this scope. Prepare/build/run the seven exact methods once; a failure receives a focused explanation and rerun only of affected methods if justified. Unrelated observations go to the existing later queue. Changes to production require a separately recorded reproduced-defect scope.

Current 150%/200% keypad and management geometry/captures remain credited from their existing bounded runs; this batch does not change global font scale or rerun those fixture selections. Human larger-text usability, known navigation/header tails, TalkBack, perceived vibration/motion and successful biometric authentication stay explicitly open. Prior controlled phone-gate routing tests prove originating-account/cancellation/late-result logic but not actual Android prompt appearance. This real gate test covers setup cancellation, not an enrolled account's successful phone-security release.

9a2 staff acceptance uses the existing TASK_CARDS.md and independently held EXPECTED.json. It stays pending until a person completes and reports the tasks without navigation coaching. 9b physical two-phone acceptance remains conditional when both devices will write. Freeze this boundary before source/build/device execution.

Focused test repair after initial 6/7: the keyboard check's default merged-tree finder missed the tagged BasicTextField; the failure explicitly reports one matching node in the unmerged tree. Source confirms the tag and Done callback exist on that field. Update only this selected existing method to use that actual tagged node. Also wait for real OS IME visibility before Done, then absence after Done, retaining before/after window captures. Rerun only this method. Other methods/production sources remain unchanged; do not diagnose every input/menu. The secure phone prompt screenshot is black, so it cannot certify prompt appearance; actual prompt title/cancellation observation remains separate evidence.
