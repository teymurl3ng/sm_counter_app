# Phase 14.6 — Native Quick Unlock

Status: **IMPLEMENTED — two extended-QA security defects fixed; 9/9 Quick unlock and 26/26 related tests pass; hands-on acceptance remains**

Date: 2026-09-12 (extends the 2026-09-09 checkpoint)

## User outcome

Each Saaf user can opt into Quick unlock on a trusted Android phone after confirming their full passphrase once. The sign-in screen shows the selected Saaf identity before offering Android phone security or that identity's personal Quick PIN. The full passphrase always remains available.

Routine backup approval and cloud recovery-point inspection can also use Quick unlock. Connecting or revoking Google authorization, changing security, removing/enrolling Quick unlock, and the final destructive restore still require Android phone security or the full Saaf passphrase; PIN alone is rejected natively.

## Implemented boundary

- Android `BiometricPrompt` uses strong biometric or device credential with an authentication-bound Android Keystore key.
- Quick PIN is 4–12 digits, recommends six, uses a per-user Argon2id verifier, and is never stored or checked by React.
- The encrypted enrollment record lives in `noBackupFilesDir` behind a separate AES-256-GCM Android Keystore key.
- Quick unlock requires a secure phone lock. Removing phone security, changing biometric enrollment, losing a Keystore key, changing/recovering a passphrase, changing relevant account security, restoring app data, or explicitly removing Quick unlock invalidates the local record.
- The first two wrong PINs fail generically; the third pauses for 30 seconds, the fourth for two minutes, and the fifth requires a successful full-passphrase login or reauthentication.
- PIN sessions carry ordinary recent-authentication strength only. Android phone security and full passphrase carry strong recent-authentication strength.
- Existing-device cloud inspection first uses the already Keystore-wrapped local vault key. A cleared/replacement phone still needs the separate high-entropy cloud recovery passphrase.
- Bridge contract v5 exposes 91 native operations. Five additive Quick unlock endpoints return only local account labels/method availability and memory-only sessions—never verifiers, keys, PINs, or passphrases.

## UI behavior

- Shared phones show the Saaf identity before any fingerprint/phone-lock prompt; biometrics are not presented as employee identification.
- `Access & users > My account` explains the local-only recovery boundary and supports setup/removal.
- Routine backup confirmations offer full passphrase, Android phone security, or Quick PIN when allowed.
- Destructive restore, Google connection, and Google revocation hide the PIN option and are independently rejected by the native strong-authentication gate.
- Interactive Quick unlock calls use a two-minute bridge window so the native phone-security prompt does not falsely report the old 30-second timeout.

## Automated evidence

The focused `Phase14QuickUnlockTest` contains nine tests covering:

1. device-local persistence and second-phone separation;
2. strong Android authentication versus non-strong PIN sessions;
3. increasing PIN delay, bounded lockout, and full-passphrase reset;
4. passphrase, phone-security, restore-style clearing, and Keystore invalidation paths;
5. exact bridge error meanings for cancellation, unavailable, invalidated, rate-limited, and failed authentication.
6. real Android Keystore-encrypted enrollment-record save/read behavior on the physical phone.
7. one shared PIN failure limit across overlapping login and reauthentication requests;
8. rejection of phone authentication completed after a passphrase change;
9. rejection of phone reauthentication completed after enrollment removal.

The existing cloud cryptography test now also proves trusted-device local-key decryption alongside replacement-device portable-passphrase recovery.

Completed host/build gates:

- bridge contract generation/drift check: v5, 91 endpoints;
- TypeScript check and Vite production build;
- source and packaged cleanup gates;
- Kotlin production and Android-test compilation;
- 7/7 host unit tests;
- isolated `com.saaf.milkshop.qa` app and Android-test APK assembly with the configured OAuth client identifier. The 2026-09-12 QA app APK SHA-256 is `504445744C469EDCB22FAE86B157A9038FEBDBB056DC05B09170B8CDB8A99A4F`.

The nine focused tests pass 9/9 on the isolated S24 QA installation. The combined Quick unlock, Phase 3/4 native security, and selected cloud cryptography regression passes 26/26. The full physical-device inventory is now 269 tests; the complete suite remains the Phase 14.7 gate.

## Focused S24 acceptance

- Combined phone-security and six-digit Quick PIN enrollment succeeds.
- Logout followed by Quick PIN login opens the correct Saaf account.
- Logout followed by fingerprint/phone-security login opens the correct Saaf account.
- Physical testing exposed and fixed two blocking native integration defects: `BiometricPrompt` was invoked off the Fragment host's main thread, and AES-GCM additional authenticated data was supplied before Samsung Keystore completed user authentication.
- The ordinary `com.saaf.milkshop` installation was not installed over, cleared, or otherwise modified.

## Extended acceptance checkpoint — 2026-09-12

The paired S24's isolated QA installation resumes with its existing enrollment. Android Back cancels the native phone-security prompt and safely returns to sign-in. Force-stop/cold-start retains the account label and both Quick unlock methods. After the QA update and cold launch, the user confirmed that the dashboard opened; `05-post-update-pin-login.png` shows `test · Owner`. The user subsequently clarified that fingerprint may have been tapped instead, so the screenshot's filename is not proof of the authentication method: post-update Quick PIN login remains unconfirmed. After sign-out, `06-after-signout.png` shows the Quick unlock pause message; the exact preceding PIN-attempt sequence is not established. Next, use a successful full-passphrase sign-in to reset the pause, then deliberately retest PIN login. Evidence is under `phase14-evidence/sm-s921w-android16-phase14-6-extended/` (1080 × 2340, density 480, font scale 1.0).

The user then signed in using the full Saaf passphrase and confirmed successful entry. `07-full-passphrase-fallback.png` independently shows the dashboard as `test · Owner`. After another sign-out, the user explicitly used the Saaf Quick PIN and confirmed successful login ("ok works"). Full-passphrase fallback, reset of the observed PIN pause, and explicit post-update Quick PIN login therefore pass. The earlier ambiguous authentication is not used as PIN evidence.

After another sign-out, the user explicitly selected phone security, used fingerprint authentication, and confirmed successful dashboard entry ("opened"). Post-update fingerprint login passes; `11-fingerprint-login.png` records the resulting dashboard. Both fast sign-in methods and the full-passphrase fallback are now separately confirmed.

Cloud recovery-point inspection with Quick PIN selected succeeded: the user reported that the review-warning button appeared, and `15-cloud-inspection-passed.png` shows the green security-check result for the September 12 recovery point. The final warning was then inspected without approving replacement. `17-final-restore-strong-only.png` shows only Passphrase and Phone security, an unchecked replacement acknowledgment, and a disabled restore action. Android Back closes the warning and retains the inspection result (`18-final-restore-cancelled.png`). No restore was performed. The first attempted warning tap instead yielded a screenshot at the page top (`16-final-restore-approval.png`); despite that filename, it is not dialog evidence. A subsequent observed button tap opened the warning correctly, so the initial jump remains unexplained rather than a reproduced defect.

Two native P1 security defects were reproduced before production changes using three new in-memory instrumentation tests on the S24, with `saafSkipBootstrap=true` to preserve the installed QA identity/data:

- **F14.6-01 — overlapping PIN attempts bypass the shared delay.** Eight concurrent login/reauthentication attempts all returned ordinary authentication failure instead of only the first two doing so and the remainder observing the 30-second pause. PIN verification uses a stale record outside the record-store mutex. Evidence: `quick-unlock-before.txt`, `overlappingLoginAndReauthenticationAttemptsShareThePinLimit`.
- **F14.6-02 — an in-flight phone-security result survives invalidation.** Controlled device-gate tests change the passphrase before the prompt completes, or remove enrollment before reauthentication completes. Both incorrectly succeed because enrollment/security binding is checked only before authentication. Evidence: `quick-unlock-before.txt`, `passphraseChangeWhilePhonePromptIsOpenCannotGrantANewSession` and `removalWhilePhonePromptIsOpenCannotReauthenticate`. These tests exercise real native security services with a controlled device gate, not a claim of physically changing the phone's lock or fingerprint enrollment.

The pre-fix run is 6 passing / 3 failing; all original focused checks still pass. Both findings are now fixed and covered by the passing regression. PIN verification and its attempt-state update execute under the same mutex, reading the current enrollment. Both login and reauthentication revalidate the enrollment and account-security binding after authentication, immediately before granting a session or recent-authentication credit. The native phone prompt remains outside that mutex so cancellation and invalidation can proceed.

Verification: `quick-unlock-after.txt` records **26/26 passing, zero failures/skips** (9 Quick unlock, 9 Phase 4 security, 7 Phase 3 user security, 1 cloud envelope/local-key recovery). Host tests pass 7/7; app/test APK assembly, bridge v5/91, packaged cleanup, and frozen specification/SQL hashes pass. Test APK SHA-256: `AC9160C2876D52CE1EE33F28E2D9945388F699186BA16E09E27B13F91A869A7D`. Only the `.qa` app/test packages were updated, using install-with-data-preservation and the existing OAuth configuration.

Following instructions to restart the phone, unlock Android normally, and open Saaf QA using its Quick PIN, the user confirmed "it opens". Post-reboot Quick PIN login passes by user report. Wireless ADB was initially disconnected; the user then connected USB and ADB reported the S24 authorized. `19-post-reboot-usb-dashboard.png` independently captures the resulting `test · Owner` dashboard; the authentication method remains established by the user's report.

## Remaining physical acceptance

- Complete a controlled wrong-PIN delay/threshold sequence, other routine backup actions, removal/re-enrollment, and shared-account switching. Prompt cancellation, enrollment visibility after process restart, full-passphrase fallback, explicit Quick PIN login after the observed pause, explicit post-update fingerprint login, Quick PIN cloud inspection, and the strong-only final restore warning with safe Back cancellation pass. Post-reboot Quick PIN login passes by user report.
- Verify secure-lock/biometric-change invalidation and that a cleared or second QA phone cannot use the Quick PIN to open a cloud recovery point.
- Check the new states with TalkBack and increased text. These extended checks remain part of the Phase 14.6 acceptance boundary and may be completed alongside the Phase 14.7 defect/regression pass.

## Plain English

The S24 has the updated QA build. The extended audit caught and fixed two native security races, and all 26 focused security/recovery tests plus seven host tests pass. Cancellation and enrollment retention after process restart are verified. The remaining hands-on authentication, recovery, and accessibility checks are still required before final closure.
