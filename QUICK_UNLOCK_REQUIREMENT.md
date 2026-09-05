# Mandatory Quick Unlock Requirement

Status: **APPROVED AND RELEASE-BLOCKING — 2026-09-06**

## User outcome

Routine Saaf use on a trusted Android phone must not require repeatedly typing a long passphrase. After one deliberate full-passphrase enrollment, a user must have a fast native path using a fingerprint/other strong biometric, the Android device credential, or a short personal app PIN.

This supersedes the earlier forward-looking deferral of a numeric quick-PIN shortcut. It does not rewrite that historical decision: Phase 14 live use supplied the missing evidence and the owner explicitly made low-friction access mandatory.

## Approved security boundary

- Call the feature **Quick unlock**, not a security bypass.
- Prefer Android `BiometricPrompt` with `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL` on an owner-controlled phone.
- Provide a per-Saaf-user numeric PIN fallback: four digits minimum for the requested fast path, with six digits recommended.
- Enrollment requires the user's current full Saaf passphrase. There is no default PIN and no silent enrollment.
- A short PIN must never derive, encrypt, or directly unwrap the portable backup data key. Its small keyspace is unsuitable as the off-device recovery secret.
- Keep quick-unlock material native and device-bound. Protect it with Android Keystore; React may display the journey but may not verify a PIN, assert biometric success, or retain secrets.
- Rate-limit failed PIN attempts, apply increasing delays, and require the full passphrase after the bounded failure threshold. Never erase shop data merely because quick unlock failed.
- Invalidate quick unlock after passphrase change/recovery, account disablement, relevant role/security change, app-data restore, device-security removal, Keystore invalidation, or explicit removal by the user.
- Biometrics authenticate an Android device user, not a particular Saaf employee. On a shared shop phone, the user must first select their Saaf identity; a per-user app PIN is the dependable identity-specific fast path. Do not imply that a fingerprint identifies which employee acted.

## Where quick unlock must work

- Routine app sign-in and session unlock on the already trusted installation.
- Recent reauthentication for ordinary protected actions.
- Existing-device cloud-backup management and recovery-point inspection, using the already device-protected cloud key instead of repeatedly asking for the long cloud-recovery passphrase.
- Safe fallback when biometric hardware is unavailable, not enrolled, temporarily locked, cancelled, or disabled for apps.

High-risk actions—credential changes, user administration, destructive restore, authorization revocation, and quick-unlock enrollment/removal—must use strong biometric/device credential or the full Saaf passphrase plus their existing deliberate confirmation. An app PIN alone must not silently authorize a destructive outcome.

## Recovery boundary

Quick unlock is local convenience, not portable recovery. A replacement phone, cleared installation, or lost Keystore still requires the full cloud-recovery passphrase or another separately designed high-entropy recovery mechanism. A four-digit PIN must never be sufficient to decrypt stolen Drive backups offline.

On an existing trusted phone, the app may use biometric/device-credential or PIN authentication to release a Keystore-wrapped local recovery key. It must not store or display the plaintext recovery passphrase.

## Required states and tests

- unavailable, offered, enrollment, confirmation, success, cancellation, and removal;
- biometric success/failure/temporary lockout/permanent lockout and device-credential fallback;
- short-PIN creation, mismatch, repeated failure, delay, lockout, and full-passphrase recovery;
- first use after reboot, app update, lifecycle recreation, session expiry, and process death;
- passphrase change/recovery, role change, disabled account, restored backup, changed biometric enrollment, and removed device security;
- shared-device account switching with correct actor attribution;
- routine login, recent reauthentication, cloud status/configuration, manual backup, and recovery-point inspection;
- proof that a cleared or second device still cannot decrypt a cloud recovery point with only the quick PIN;
- TalkBack, large text, cancellation/back behavior, and clear non-technical errors on representative physical devices.

## Phase placement

- **Phase 14.6 — Quick unlock implementation and device QA:** design, implement, threat-test, and physically verify the native feature.
- **Phase 14.7 — Defect closure and full regression:** close quick-unlock and other Phase 14 findings, then run the complete suite.
- **Phase 15 — Shopkeeper QA:** prove uncoached owners/staff can enroll, use, recover, and understand quick unlock without confusing it with portable backup recovery.
- **Phase 16 — Release readiness:** release is blocked unless secure quick unlock, invalidation, lockout, Keystore behavior, backup boundaries, and recovery documentation pass.

## Evidence basis

- Phase 14 live QA showed repeated long-passphrase entry and two different passphrases causing material friction during a normal backup task.
- Android supports native biometric/device-credential authentication and authentication-bound Keystore keys: [Show a biometric authentication dialog](https://developer.android.com/identity/sign-in/biometric-auth) and [KeyGenParameterSpec.Builder](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder).

## Plain English

On the same trusted phone, opening Saaf and approving normal protected work must be as quick as a fingerprint or short PIN. The long passphrase remains the emergency key for a lost or replacement phone, because making a four-digit code the only protection for cloud backups would make those backups easy to crack.
