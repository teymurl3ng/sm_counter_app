# Phase 14.3 — Live Google Drive Preflight

Status: **LIVE EXECUTION IN PROGRESS — consent, initial encrypted upload, and recovery-point listing verified**

Date: 2026-09-06  
Primary QA device: Samsung SM-A065F / Android 16 / API 36  
QA package: `com.saaf.milkshop.qa` / `0.1.0-qa`

## Outcome

The app-side cloud-backup implementation and physical-device prerequisites are ready for a live Google Drive run. The non-live provider, scheduling, failure, and recovery matrix passes **25/25** on the explicitly isolated QA build with zero skips or failures. The accepted QA APK remains byte-for-byte reproducible at SHA-256 `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C`.

Full live acceptance has not yet been claimed. Consent, initial encrypted upload, and recovery-point listing are now proven; manual upload, offline/retry and degraded-operation behavior, recovery-point inspection, and revocation remain to be exercised.

## Live execution evidence — 2026-09-06

- The user configured the external/testing OAuth audience, one owner-controlled test user, the narrow `drive.appdata` scope, the QA Android client, and the matching Web client in the dedicated `saaf-milk-qa` Cloud project.
- The Web client ID was supplied only as a local Gradle command-line property. It was not written to tracked source.
- The OAuth-enabled isolated QA APK built successfully with contract v4 / 86 endpoints and the UI cleanup gate intact. Its SHA-256 is `305BF889747164E675882213F203DD4B3FE6E2F70590057686B13080543739F8`.
- The APK was verified as `com.saaf.milkshop.qa` / `0.1.0-qa`, installed only on the primary QA Samsung, and initialized with disposable test data.
- Real Google consent and Drive connection completed. After reloading the Cloud area, the app reported one recovery point, `Last verified` at `2026-09-06 02:31:54`, and no cloud error.

### Finding F14.3-01 — interactive connection outlives the bridge timer

The first live connection displayed `The app took too long to respond. Please try again.` before the user could see completion, then reset the visible form. Retrying produced a passphrase error. Leaving and returning to Cloud loaded the successfully persisted Google account and verified recovery point.

Code inspection confirms the shared web bridge deletes every pending request after 30 seconds, while native `connectCloudBackup` legitimately waits for recovery-passphrase entry, Google account selection/consent, Drive identity verification, configuration, and scheduling. The native operation is not cancelled when the web request times out, so it can complete successfully after the UI has declared failure. This is a real feedback/reconciliation defect, not a failed backup or user mistake. Preserve the successful connection during the remaining acceptance checks; fix the interactive timeout and post-resume status reconciliation before Phase 14 closes.

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
- Generated `BuildConfig.GOOGLE_WEB_CLIENT_ID` is blank; no project-level or user-level Gradle value is present.
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

## Required user input

To continue the live half, the user must choose or create the Google Cloud project, register the Android QA client above, create the Web client in the same project, enable Drive API, add the owner-controlled test account to the OAuth test audience if required, and provide only the **Web client ID** for the local build. Account selection and all passwords/passphrases stay private on the phone.

## Plain English

The app's cloud-backup machinery passed all 25 tests we can run without contacting a real Google account. Google requires one registration for this exact Android test app plus a Web client ID used by Google's sign-in screen. Once those are created and a safe test account is chosen, we can perform the real upload, retry, recovery-point, and disconnect checks. No Google account details or secrets have been read or saved.
