# Phase 14.6 — Native Quick Unlock

Status: **IMPLEMENTED — focused S24 automated and hands-on smoke pass; extended physical acceptance remains**

Date: 2026-09-09

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

The focused `Phase14QuickUnlockTest` contains six tests covering:

1. device-local persistence and second-phone separation;
2. strong Android authentication versus non-strong PIN sessions;
3. increasing PIN delay, bounded lockout, and full-passphrase reset;
4. passphrase, phone-security, restore-style clearing, and Keystore invalidation paths;
5. exact bridge error meanings for cancellation, unavailable, invalidated, rate-limited, and failed authentication.
6. real Android Keystore-encrypted enrollment-record save/read behavior on the physical phone.

The existing cloud cryptography test now also proves trusted-device local-key decryption alongside replacement-device portable-passphrase recovery.

Completed host/build gates:

- bridge contract generation/drift check: v5, 91 endpoints;
- TypeScript check and Vite production build;
- source and packaged cleanup gates;
- Kotlin production and Android-test compilation;
- 7/7 host unit tests;
- isolated `com.saaf.milkshop.qa` app and Android-test APK assembly with the configured OAuth client identifier. The current QA app APK SHA-256 is `B9D86930157A8DDEAB942DBC91EFA4DFD3B522D9E935E11F4D0F285938AF33EF`.

The six focused tests pass 6/6 on the isolated S24 QA installation. The full physical-device inventory is now 266 tests; the complete suite is intentionally deferred to Phase 14.7 rather than claimed here.

## Focused S24 acceptance

- Combined phone-security and six-digit Quick PIN enrollment succeeds.
- Logout followed by Quick PIN login opens the correct Saaf account.
- Logout followed by fingerprint/phone-security login opens the correct Saaf account.
- Physical testing exposed and fixed two blocking native integration defects: `BiometricPrompt` was invoked off the Fragment host's main thread, and AES-GCM additional authenticated data was supplied before Samsung Keystore completed user authentication.
- The ordinary `com.saaf.milkshop` installation was not installed over, cleared, or otherwise modified.

## Remaining physical acceptance

- Manually prove cancellation, wrong-PIN delay/fallback, routine backup reauthentication, removal, reboot/process restart, and clear shared-account identity.
- Verify secure-lock/biometric-change invalidation and that a cleared or second QA phone cannot use the Quick PIN to open a cloud recovery point.
- Check the new states with TalkBack and increased text. These extended checks remain part of the Phase 14.6 acceptance boundary and may be completed alongside the Phase 14.7 defect/regression pass.

## Plain English

The fast sign-in system now works on the S24: setup, short-PIN login, and fingerprint/phone-lock login all passed, and its six dedicated tests pass. The remaining misuse, recovery, lifecycle, and accessibility checks are still required before final closure.
