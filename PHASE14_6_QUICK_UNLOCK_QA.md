# Phase 14.6 — Native Quick Unlock

Status: **COMPLETE — extended security, shared-phone, recovery, and accessibility acceptance pass**

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

The cloud cryptography tests prove trusted-device local-key decryption, replacement-device portable-passphrase recovery, and the different-local-vault fallback used by a freshly initialized phone.

Completed host/build gates:

- bridge contract generation/drift check: v5, 91 endpoints;
- TypeScript check and Vite production build;
- source and packaged cleanup gates;
- Kotlin production and Android-test compilation;
- 7/7 host unit tests;
- isolated `com.saaf.milkshop.qa` app and Android-test APK assembly with the configured OAuth client identifier. After the Access-action feedback fix, the 2026-09-12 QA app APK SHA-256 is `2CD17B2276898399C70DC3B9EE5A9D649AD3541248AE23116AA7AB8D4F516531`.

The nine focused tests pass 9/9 on the isolated S24 QA installation. The combined Quick unlock, Phase 3/4 native security, and selected cloud cryptography regression passes 26/26. With the later explicit-choice regression, Phase 14.7 closes with 274 Android checks: 273/273 ordinary tests pass in `com.saaf.milkshop.qa.tests`, and the blank-install visual audit passes 1/1. Host tests pass 7/7.

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

The controlled physical PIN-failure sequence also passes. Attempts one and two show the generic verification failure (`20`–`21`). Attempt three enters the first pause (`22`). After more than 30 seconds, attempt four enters the longer pause (`23`), and the user's correct Quick PIN was still refused during that window (`24`, captured at 17:06:24 after the fourth-attempt capture at 17:04:47). After more than two minutes, attempt five was processed and required full-passphrase recovery (`25`); the correct Quick PIN remained blocked (`26`). A full-passphrase login restored the dashboard (`27`), and after sign-out the user confirmed that the correct Quick PIN opened the dashboard again (`28`). The screenshots do not expose either credential.

Removal and re-enrollment pass, and the hands-on flow exposed **F14.6-03 — Access action success was outside the retained viewport**. After removal completed at the bottom of My account, the user had to scroll to the top to discover “Quick unlock was removed from this phone,” leaving the action without visible confirmation. `SecurityWorkspace` now reveals its accessible success notice after an Access mutation completes. The UI-only change does not alter authentication or account state. TypeScript validation, Vite production packaging, cleanup gates, and QA APK assembly pass. The fixed APK was installed with data preserved; the user signed in with the full passphrase, re-enrolled phone security plus Quick PIN, and confirmed the updated flow worked. `29-quick-unlock-reenrolled-after-feedback-fix.png` confirms both methods are active again. After a fresh sign-out, the user confirmed that the re-enrolled Quick PIN opened the dashboard; `30-quick-pin-login-after-reenrollment.png` records the resulting state. Immediate feedback behavior is established by the user's live confirmation.

The final shared-phone identity check passes. The user enrolled two Saaf identities with distinct Quick PINs, opened each identity with its own PIN, and confirmed that each identity rejected the other identity's PIN with the appropriate generic error. Credentials were not recorded.

Phase 14.7 rebuilt the final OAuth-enabled QA APK after the complete regression. A first packaging attempt selected a different historical OAuth client and produced a generic Drive authorization failure; the corrected data-preserving build uses the exact historical Web client property. The user confirmed that Drive works and re-enrolled Quick unlock. The earlier instrumentation path had removed the disposable user-facing QA package before the dedicated test identity existed. Future connected tests use `com.saaf.milkshop.qa.tests` and cannot install over the user-facing `.qa` identity.

The cleared A06 replacement check then exposed a separate cloud recovery defect: selecting an S24 point stopped with `The cloud backup belongs to a different backup vault`. A local-vault mismatch now raises the typed missing-key result, so the bridge requests the portable cloud recovery passphrase. The new regression passes in the 6/6 cloud-cryptography suite. The user subsequently passed that security check and restored the S24 7:05 recovery point on the A06.

The restored account correctly had no transferred Quick unlock enrollment. After the user added an Android screen PIN, hands-on setup exposed **F14.7-02 — phone security was preselected and described as fingerprint-capable on a phone with no enrolled fingerprint**. Enrollment now starts with both methods off, resets both choices after removal, and labels the actual A06 capability `Phone security`. The user separately confirmed Quick PIN-only setup/login and Phone-security-only setup/login. The expanded focused behavior matrix passes 12/12, the complete ordinary device suite passes 273/273, and the final installed APK is 16,174,564 bytes with SHA-256 `5581218C0761ED3C33CA9D09877D19E4D2654B5B6DF5AC2790C80B364F223A24`.

## Physical acceptance closure

- Shared-account selection, each account's own PIN, cross-account PIN rejection, cancellation, process restart, full-passphrase fallback, the controlled wrong-PIN delays, fingerprint/phone-security login, Quick PIN cloud inspection, removal/re-enrollment, post-reboot login, and the strong-only final restore warning all pass.
- TalkBack can focus and activate the corrected visible Close button.
- A clean A06 installation showed no S24 Quick unlock identity or method. It could inspect the S24 recovery point only through the portable cloud recovery passphrase, and the S24 7:05 point was then restored successfully.
- The A06 received a secure Android screen PIN before the final Quick unlock checks; no fingerprint was enrolled. Removing/changing that lock after enrollment and changing biometric enrollment were not performed. Automated tests cover those Keystore invalidation paths; this documented non-invasive limitation is accepted for Phase 14.

## Plain English

Quick unlock now passes the full Phase 14 boundary on real phones. Each local PIN opens only its own Saaf account, the clean A06 did not inherit any S24 Quick unlock option, and the A06 required the separate cloud recovery passphrase before restoring the S24 backup. Setup no longer silently selects phone security or suggests that a nonexistent fingerprint is enrolled. All 273 ordinary Android tests, the visual audit, and seven host tests pass.
