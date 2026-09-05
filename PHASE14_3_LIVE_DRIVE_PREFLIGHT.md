# Phase 14.3 — Live Google Drive QA

Status: **PAUSED SAFELY — live consent, upload, hidden listing, and offline retry verified; inspection authentication unresolved**

Date: 2026-09-06  
Primary QA device: Samsung SM-A065F / Android 16 / API 36  
QA package: `com.saaf.milkshop.qa` / `0.1.0-qa`

## Outcome

The app-side cloud-backup implementation and physical-device prerequisites are ready for a live Google Drive run. The non-live provider, scheduling, failure, and recovery matrix passes **25/25** on the explicitly isolated QA build with zero skips or failures. The accepted QA APK remains byte-for-byte reproducible at SHA-256 `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C`.

Full live acceptance has not yet been claimed. Consent, initial and manual encrypted upload, hidden recovery-point listing, and offline queue/retry are proven. Recovery-point inspection paused when the expected private passphrase was not accepted; the exact owner-versus-cloud authentication stage was not isolated before stopping. Metered-policy behavior and disconnect/revocation also remain.

## Live execution evidence — 2026-09-06

- The user configured the external/testing OAuth audience, one owner-controlled test user, the narrow `drive.appdata` scope, the QA Android client, and the matching Web client in the dedicated `saaf-milk-qa` Cloud project.
- The Web client ID was supplied only as a local Gradle command-line property. It was not written to tracked source.
- The OAuth-enabled isolated QA APK built successfully with contract v4 / 86 endpoints and the UI cleanup gate intact. Its SHA-256 is `305BF889747164E675882213F203DD4B3FE6E2F70590057686B13080543739F8`.
- The APK was verified as `com.saaf.milkshop.qa` / `0.1.0-qa`, installed only on the primary QA Samsung, and initialized with disposable test data.
- Real Google consent and Drive connection completed. After reloading the Cloud area, the app reported one recovery point, `Last verified` at `2026-09-06 02:31:54`, and no cloud error.
- The user then selected **Back up now**. After the expected short queue delay and refresh, `Last verified` advanced, the recovery-point count increased from one to two, and no error appeared.
- In the same test account's normal Google Drive interface, neither recovery point appeared in **My Drive**, **Recent**, or a `Saaf` search. This confirms the live files use the intended hidden app-data area rather than user-visible Drive storage.
- With Airplane mode enabled, **Back up now** remained queued and no recovery point was created while offline. After Airplane mode was disabled and Wi-Fi returned, the same queued work completed automatically: the recovery-point count increased from two to three, the verification time advanced, and the prior offline error cleared.

### Finding F14.3-01 — interactive connection outlives the bridge timer

The first live connection displayed `The app took too long to respond. Please try again.` before the user could see completion, then reset the visible form. Retrying produced a passphrase error. Leaving and returning to Cloud loaded the successfully persisted Google account and verified recovery point.

Code inspection confirms the shared web bridge deletes every pending request after 30 seconds, while native `connectCloudBackup` legitimately waits for recovery-passphrase entry, Google account selection/consent, Drive identity verification, configuration, and scheduling. The native operation is not cancelled when the web request times out, so it can complete successfully after the UI has declared failure. This is a real feedback/reconciliation defect, not a failed backup or user mistake. Preserve the successful connection during the remaining acceptance checks; fix the interactive timeout and post-resume status reconciliation before Phase 14 closes.

### Finding F14.3-02 — offline refresh exposes a raw network exception

With Airplane mode enabled, **Back up now** was queued and a subsequent refresh correctly failed to reach Google, but the UI exposed the low-level DNS message `Unable to resolve host www.googleapis.com: No address associated with hostname`. Offline operation is expected and does not imply backup corruption, but this copy is too technical and does not tell the shopkeeper that the queued backup will retry after connectivity returns. Map this failure to concise offline/retry guidance while retaining diagnostic detail outside the user-facing message.

## Safe pause checkpoint

- The primary QA app remains connected to Google Drive with automatic Wi-Fi-only backup enabled and three verified encrypted recovery points.
- No restore, disconnect, authorization revocation, package clear, or second-phone Saaf action was performed after the successful live checks.
- The second phone's ordinary `com.saaf.milkshop` installation remains untouched; it was used only to view normal Google Drive, where no hidden recovery point appeared.
- Resume by identifying whether inspection rejects the Saaf owner passphrase before the Android dialog or the cloud recovery passphrase inside the Android dialog. Do not record either value.
- After inspection succeeds or the credential limitation is honestly resolved, finish metered-policy evidence and disconnect/revocation. Then fix F14.3-01 and F14.3-02 before closing Phase 14.

## Official setup requirements

Google's current Android guidance requires two OAuth client registrations in the same Google Cloud project:

1. **Android client** — binds the installed app identity to its package and signing certificate.
   - Package: `com.saaf.milkshop.qa`
   - Current QA/debug signing SHA-1: `97:ED:12:6E:03:31:B5:4C:11:10:10:6C:98:A9:2C:FA:A8:CB:4A:CC`
2. **Web application client** — its client ID is passed to Credential Manager as the server client ID and supplied to this build as `-Psaaf.googleWebClientId=<client-id>`.

The Google Drive API must be enabled in that project. The OAuth audience/consent screen must include the owner-controlled account as a test user when the project uses External + Testing. No client secret is required or stored by the Android app.

The implementation requests only `https://www.googleapis.com/auth/drive.appdata`. Google classifies this as the narrow app-data scope; files live in the hidden `appDataFolder`, are accessible only to the authorizing app, and are not visible in the normal Drive UI.

Official references:

- [Implement Sign in with Google using Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation)
- [Google Android sign-in codelab — Android and Web OAuth clients](https://codelabs.developers.google.com/sign-in-with-google-android)
- [Google Drive application data folder](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Choose Google Drive API scopes](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)
- [Google AuthorizationClient reference](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient)
- [Google OAuth consent-screen setup](https://support.google.com/cloud/answer/13461325)

## Verified local and device preflight

- Repository and Phase 14.2 checkpoint were clean before starting.
- Before live injection, generated `BuildConfig.GOOGLE_WEB_CLIENT_ID` was blank and no persistent project/user Gradle value existed. The later OAuth-enabled build received the Web client ID only through its local command line.
- The code uses native Credential Manager plus `AuthorizationClient`; Google authentication is not performed inside the WebView.
- The authorization request contains only `drive.appdata`.
- Drive list/upload paths explicitly use `spaces=appDataFolder` / parent `appDataFolder`.
- Google Play services is installed on the primary phone (`26.31.31 (260400-959389638)`).
- Airplane mode is disabled.
- At least one Google-account reference is present on the phone; no account identifier was displayed or recorded, and ownership/test suitability remains for the user to confirm.
- Contract v4 / 86 endpoints and the automatic UI cleanup/package gate pass as part of the build.
- After the test runner removed its transient target, the hash-verified QA APK was reinstalled, only `com.saaf.milkshop.qa` data was cleared, and the clean QA launcher opened successfully on the primary phone.
- Physical-device cloud resilience suite: **25/25**, zero skipped, zero failed:
  - `Phase10CloudBackupTest`;
  - `Phase11CloudBackupDurabilityTest`;
  - `Phase11CloudBackupFailureMatrixTest`;
  - `Phase11RecoveryIdentitySessionTest`.

## Safe live execution sequence

Once the two OAuth clients and test account are ready:

1. Rebuild with both `-Psaaf.isolatedQa=true` and the approved Web client ID; verify the resulting package is `com.saaf.milkshop.qa` before installation.
2. Install only on the primary QA Samsung and use dedicated fake shop data.
3. The owner signs into Saaf Milk, opens Backup → Cloud, chooses the owner-controlled Google test account, and enters a separate cloud-recovery passphrase privately on the phone.
4. Verify consent requests only Saaf Milk's app-data access, then verify the initial encrypted upload, a manual **Back up now** run, status refresh, and recovery-point listing.
5. Verify the remote item is not visible in normal My Drive and that only non-secret status metadata appears in the app/log evidence.
6. Starting from a known good recovery point, exercise offline and restored-network retry, metered-policy behavior, authorization expiry/action-required behavior, and continued offline shop operation.
7. Inspect a real recovery point without restoring it on the primary phone. Full replacement on another phone remains Phase 14.4.
8. Disconnect and verify Google authorization revocation, local schedule/key cleanup, retained remote encrypted recovery points, and a fresh consent requirement on reconnect.

Never record or commit the Google password, access token, client secret, recovery passphrase, recovery code, real email address, or account-private Drive metadata.

## Resume point

The external/testing Google Cloud project, Android client, Web client, narrow scope, and test audience are configured. The OAuth-enabled QA APK remains installed on the primary phone. On resume, keep the existing connection and three recovery points, distinguish the two private passphrase stages during inspection, and never paste credentials or recovery codes into chat or project files.

## Plain English

The real Google connection works. It created three encrypted recovery points, hid them from normal Drive, and automatically completed an offline-queued backup after Wi-Fi returned. Two confusing error messages were found and recorded. We stopped safely before restore or disconnect because recovery-point inspection did not accept the expected private passphrase; no secret was collected or saved.
