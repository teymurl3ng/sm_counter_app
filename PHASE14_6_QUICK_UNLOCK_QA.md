# Phase 14.6 — Native Quick Unlock

Status: **IMPLEMENTED — build and static gates pass; focused physical-device run and hands-on acceptance remain**

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

## Automated evidence prepared

The focused `Phase14QuickUnlockTest` contains five tests covering:

1. device-local persistence and second-phone separation;
2. strong Android authentication versus non-strong PIN sessions;
3. increasing PIN delay, bounded lockout, and full-passphrase reset;
4. passphrase, phone-security, restore-style clearing, and Keystore invalidation paths;
5. exact bridge error meanings for cancellation, unavailable, invalidated, rate-limited, and failed authentication.

The existing cloud cryptography test now also proves trusted-device local-key decryption alongside replacement-device portable-passphrase recovery.

Completed host/build gates:

- bridge contract generation/drift check: v5, 91 endpoints;
- TypeScript check and Vite production build;
- source and packaged cleanup gates;
- Kotlin production and Android-test compilation;
- 7/7 host unit tests;
- isolated `com.saaf.milkshop.qa` app and Android-test APK assembly with the configured OAuth client identifier. The current QA app APK SHA-256 is `860DCA2CA78E73310E63108641032EB67667858966595A397BE908D2DBFE44D1`.

The full physical-device inventory is now 265 tests. It has not been claimed as passing in this checkpoint: no Android device was connected when the APKs were assembled.

## Remaining physical acceptance

- Install only `com.saaf.milkshop.qa` on the chosen QA phone.
- Run the five focused automated tests.
- Manually prove setup, phone-security login, PIN login, cancellation, wrong-PIN delay/fallback, routine backup reauthentication, removal, reboot/process restart, and clear shared-account identity.
- Verify secure-lock/biometric-change invalidation and that a cleared or second QA phone cannot use the Quick PIN to open a cloud recovery point.
- Check the new states with TalkBack and increased text before closing 14.6.

## Plain English

The fast sign-in system is built. A fingerprint/phone lock or personal short PIN can now replace repeated long-passphrase typing on the same protected phone, but that PIN cannot unlock a stolen cloud backup or approve a destructive restore. We still need to run it on a connected QA phone before calling Phase 14.6 complete.
