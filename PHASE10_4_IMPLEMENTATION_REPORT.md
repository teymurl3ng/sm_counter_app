# Phase 10.4 — Periodic Encrypted Google Drive Backup

Status: **IMPLEMENTED AND DEVICE-VERIFIED 2026-09-04**

## Outcome

The approved recovery pipeline is implemented end to end. An authenticated owner can connect a Google account, choose the network policy, request an immediate backup, inspect status and recovery points, perform a deliberate full-replacement restore, pause/resume the schedule, or revoke the connection. It is backup, not synchronization.

## Native boundary

- Google Drive API v3 `appDataFolder` and only the `drive.appdata` scope.
- Credential Manager account selection plus `AuthorizationClient`; access tokens are short-lived and never persisted.
- One coherent Room read transaction produces the complete format-2 payload.
- A random vault key encrypts every snapshot with a new AES-256-GCM nonce. Android Keystore wraps the scheduled-use key; an Argon2id-derived owner recovery key provides replacement-device recovery.
- Exact encrypted bytes remain in an immutable private outbox until the remote size/checksum is verified. Retries reuse the snapshot UUID and discover uncertain successful uploads before creating another file.
- One unique approximate-daily WorkManager schedule uses Wi-Fi by default, battery-not-low and storage-not-low constraints, exponential retry, and explicit stale/action-required status.
- Retention preserves 14 daily, 8 weekly, 12 monthly, and at least the three newest verified points. Cleanup begins only after a new verified upload.
- Cloud restore reuses the existing compatibility inspection, pre-restore local safety snapshot, transactional full replacement, and session invalidation.
- Disconnect revokes the selected Google account's grant and clears local schedule/key state; it deliberately retains remote recovery points.

Android-owned `FLAG_SECURE` dialogs collect and confirm cloud recovery passphrases. React never receives those secrets, Google tokens, key material, backup bytes, or raw paths; it only renders native status/metadata and dispatches owner intent.

## Contract and dependencies

Contract v4 adds eight owner-only system operations, for 86 total endpoints. Its SHA-256 is `D22921E90040EF70CED8AFDE535C6309544B75D5EBD7B726ED1DE1273E0FA136`.

Android dependencies add WorkManager 2.11.2, Credential Manager 1.6.0, Google ID 1.2.0, Google Play Services Auth 22.0.0, and coroutines Play Services integration. The production Web OAuth client ID is supplied at build time with `-Psaaf.googleWebClientId=<client-id>` and is intentionally absent from source control.

## Verification

- Bridge drift verifier: v4, 86 endpoints, pass.
- React lint and packaged web build: pass.
- Kotlin/debug Android-test compilation: pass.
- Focused cloud-backup device tests: 5/5 pass.
- Host debug unit tests: 7/7 pass.
- Complete isolated-QA Samsung instrumentation suite: 224/224 pass, 0 skipped, 0 failed.
- Debug APK: `Saaf-Milk-Phase10.4-debug.apk`, SHA-256 `8902CE18093D7D21AA6064C2E523ED640178E6523ABA9BC0BB461905D00A8BFE`.

## Remaining gates

Phase 11 owns cancellation/process-death matrices, prolonged offline/missed schedules, large histories, repeated recovery, account switching, Keystore loss, and retention races. Phases 14–16 must configure the production OAuth project and prove live Drive consent, upload, retry, revocation, quota behavior, and second-device recovery with release identities. The optional read-only owner-phone snapshot viewer remains deferred; multi-writer synchronization is not approved.
