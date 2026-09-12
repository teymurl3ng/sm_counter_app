# Phase 14.3 — Live Google Drive QA

Status: **COMPLETE — live consent, encrypted upload, hidden listing, offline retry, inspection, revocation, and fresh-consent reconnect verified**

- Date: 2026-09-06
- Initial QA device: Samsung SM-A065F / Android 16 / API 36
- Completion QA device: Samsung SM-S921W / Android 16 / API 36
- QA package: `com.saaf.milkshop.qa` / `0.1.0-qa`

## Outcome

The app-side cloud-backup implementation completed its live Google Drive acceptance checkpoint. The provider, scheduling, metered-policy, retention, failure, wrong-account, authorization-required, and recovery matrix passes **25/25** on the explicitly isolated QA build with zero skips or failures. The accepted baseline QA APK remains byte-for-byte reproducible at SHA-256 `E7C8CA158B1688497C15DA77B73FAA008F8F781210650080DFADC34A7291BA6C`; the OAuth-enabled live-test APK is recorded separately below.

Physical-device evidence proves real consent, initial and manual encrypted upload, hidden recovery-point listing, offline queue/retry, successful no-restore inspection, revocation, retained remote points, and fresh-consent reconnect. Live carrier-data use was deliberately avoided; the explicit device suite supplies the metered-network constraint evidence without creating an unnecessary data-cost risk.

## Live execution evidence — 2026-09-06

- The user configured the external/testing OAuth audience, one owner-controlled test user, the narrow `drive.appdata` scope, the QA Android client, and the matching Web client in the dedicated `saaf-milk-qa` Cloud project.
- The Web client ID was supplied only as a local Gradle command-line property. It was not written to tracked source.
- The OAuth-enabled isolated QA APK built successfully with contract v4 / 86 endpoints and the UI cleanup gate intact. Its SHA-256 is `305BF889747164E675882213F203DD4B3FE6E2F70590057686B13080543739F8`.
- The APK was verified as `com.saaf.milkshop.qa` / `0.1.0-qa`, initially installed on the primary QA Samsung, and initialized with disposable test data.
- Real Google consent and Drive connection completed. After reloading the Cloud area, the app reported one recovery point, `Last verified` at `2026-09-06 02:31:54`, and no cloud error.
- The user then selected **Back up now**. After the expected short queue delay and refresh, `Last verified` advanced, the recovery-point count increased from one to two, and no error appeared.
- In the same test account's normal Google Drive interface, neither recovery point appeared in **My Drive**, **Recent**, or a `Saaf` search. This confirms the live files use the intended hidden app-data area rather than user-visible Drive storage.
- With Airplane mode enabled, **Back up now** remained queued and no recovery point was created while offline. After Airplane mode was disabled and Wi-Fi returned, the same queued work completed automatically: the recovery-point count increased from two to three, the verification time advanced, and the prior offline error cleared.
- The user moved the completion run to the faster Samsung SM-S921W. A byte-identical OAuth-enabled QA APK was installed with `--user 0`; the ordinary `com.saaf.milkshop` installation/profile was not modified. The fresh QA installation connected to the same owner-controlled Google test account and immediately discovered all three existing remote recovery points with no error.
- Connection queued an automatic recovery point and the user's **Back up now** action queued another. Both verified successfully, increasing the remote total from three to five without overwriting the earlier vault.
- The newest recovery point accepted the current Saaf owner passphrase at the page boundary and the separate cloud-recovery passphrase in the native Android dialog. Inspection reached the expected full-replacement warning, proving download, integrity/authenticity checks, decryption, and backup inspection. Restore was deliberately not confirmed because full replacement belongs to Phase 14.4.
- **Revoke access and disconnect** required reselecting the same Google account, then revoked access and returned the app to its disconnected form. The interactive bridge timed out before showing completion, but leaving and returning to Cloud reconciled the persisted disconnected state.
- Reconnecting required Google account selection, permission/consent approval, and the expected unverified-testing-app warning again. All five earlier points remained visible and a newly verified point increased the total to six, with no cloud error. This proves retained remote ciphertext and fresh authorization after revocation.

### Finding F14.3-01 — interactive cloud operations outlive the bridge timer

The first live connection displayed `The app took too long to respond. Please try again.` before the user could see completion, then reset the visible form. Retrying produced a passphrase error. Leaving and returning to Cloud loaded the successfully persisted Google account and verified recovery point.

Code inspection confirms the shared web bridge deletes every pending request after 30 seconds, while native `connectCloudBackup` legitimately waits for recovery-passphrase entry, Google account selection/consent, Drive identity verification, configuration, and scheduling. The completion run also reproduced the same false timeout during `disconnectCloudBackup`, whose native work waits for account selection, identity verification, revocation, and credential-state clearing. In both cases native work can succeed after the UI declares failure because the timeout does not cancel it. This is a real feedback/reconciliation defect, not a failed backup or user mistake. Fix the interactive timeout and post-resume status reconciliation before Phase 14 closes.

### Finding F14.3-02 — offline refresh exposes a raw network exception

With Airplane mode enabled, **Back up now** was queued and a subsequent refresh correctly failed to reach Google, but the UI exposed the low-level DNS message `Unable to resolve host www.googleapis.com: No address associated with hostname`. Offline operation is expected and does not imply backup corruption, but this copy is too technical and does not tell the shopkeeper that the queued backup will retry after connectivity returns. Map this failure to concise offline/retry guidance while retaining diagnostic detail outside the user-facing message.

### Finding F14.3-03 — credential and reconnect language is ambiguous

The live flow uses three distinct authentication concepts: the current Saaf owner passphrase, Google-owned account authentication, and a separate cloud-recovery passphrase. The connection form does not explain this distinction clearly enough. After revocation, reconnect also asks the user to create a `new` recovery passphrase even though existing recovery points remain, because the implementation creates a new device vault; the same prior recovery passphrase may be reused but is not automatically retained. Revocation launches a Google account chooser to verify the exact account without explaining why. Clarify these boundaries and new-vault behavior without ever asking for or storing the Google password.

## Phase 14.7 closure — 2026-09-12

- **F14.3-01 closed:** manual picker/restore and interactive cloud connect, inspect, restore, and disconnect calls now use a five-minute bridge window. Cloud status refreshes after window focus, visibility return, and the native Activity resume event.
- **F14.3-02 closed:** native cloud network failures now map to concise connection/retry guidance. Raw DNS and transport exceptions remain internal.
- **F14.3-03 closed:** connect and disconnect copy now distinguishes the current Saaf owner passphrase, Google-owned authentication, and the separate cloud recovery passphrase. It explains reconnect/new-vault behavior, retained recovery points, and exact-account revocation.
- The complete isolated Android regression passes 271/271, with the visual audit passing separately 1/1. Existing cloud-provider-double coverage remains green within that inventory.

### Mandatory follow-on requirement — Quick unlock

The live journey required repeated entry of a long Saaf owner passphrase plus a separate long cloud-recovery passphrase. The owner explicitly made a short trusted-device path—fingerprint/other strong biometric, Android device credential, or short per-user PIN—a release requirement. Phase 14.6 now owns implementation and physical-device proof. The short path may unlock device-bound Keystore material for routine existing-device work, but it must never reduce portable Drive-backup encryption to a four-digit secret. See `QUICK_UNLOCK_REQUIREMENT.md`.

## Completion state and handoff

- The replacement QA installation on Samsung SM-S921W remains connected to Google Drive with automatic Wi-Fi-only backup enabled and six verified encrypted recovery points visible.
- No cloud restore was confirmed. Phase 14.4 owns the deliberate full-replacement recovery test on this disposable `com.saaf.milkshop.qa` installation.
- The ordinary `com.saaf.milkshop` installation/profile on that phone remains untouched; the isolated QA package was installed only for owner profile 0.
- The original Samsung SM-A065F was not cleared or modified during the completion run.
- No Google password, access token, client secret, Saaf passphrase, cloud-recovery passphrase, recovery code, or real account identifier was added to project evidence.
- Continue with second-device full replacement in 14.4, accessibility/device behavior in 14.5, mandatory Quick unlock in 14.6, then fix F14.3-01/F14.3-02/F14.3-03 and run final closure in 14.7.

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

## Phase 14.4 handoff

The external/testing Google Cloud project, Android client, Web client, narrow scope, and test audience remain configured. The OAuth-enabled QA APK is connected on the Samsung SM-S921W and lists six verified recovery points. The next checkpoint may deliberately restore one inspected point into this disposable QA package, but it must not touch the phone's ordinary Saaf installation/profile or record private credentials.

## Plain English

Real Google backup works across a disconnect and reconnect. Six encrypted recovery points are safely hidden in Drive; offline work retried automatically, a real point decrypted and passed inspection, revocation retained the remote copies, and reconnect required fresh Google permission. Three usability defects were recorded for later closure: misleading interactive timeouts, a raw offline DNS error, and unclear wording around the three credentials and new-vault reconnect behavior. No secret was collected or saved.
