# Google Drive release configuration repair

17 October 2026. User authorized a proper repair of the built-in Drive connection.

## Baseline and cause

Ordinary 0.6.0/code9 was packaged with an empty `GOOGLE_WEB_CLIENT_ID`. The interactive Google bridge checks this value before account selection/recovery setup and throws the observed message. Gradle defaulted the identifier to an empty string and the release script checked signing/provenance/preview flags without checking OAuth configuration. Earlier live Drive acceptance used the QA package and a one-time Web client flag; that evidence does not establish current release registration.

## Four bounded steps

1. Verify the existing Google project: correct Web client, Android client for `com.saaf.milkshop` and the current release signing SHA-1, Drive API, narrow app-data scope, and audience/consent status. Keep existing clients/vaults; create only the missing release registration. No new paid service or broad Drive scope.
2. Persist the verified Web identifier in ignored `phase18/oauth.properties`, retaining the explicit Gradle-property override. Add a release build guard for absent/malformed identifiers. Replace the technical missing-configuration message with ordinary language. Release as 0.6.1/code10 with unchanged package, signing key and database schema.
3. Run missing/malformed configuration rejection and configured acceptance through the actual Gradle task; build the signed release and verify package, signing certificate, version, embedded identifier, preview/test flags and production input hashes. No database/provider/cryptography changes or whole-app test run.
4. Update authorized ordinary phones with data retained, then verify real Google selection/consent, encrypted upload and recovery-point listing/inspection. User supplies private app and recovery passphrases on-device. Do not restore over the live shop or claim verification from build success alone.

## Research and judgment

- [Android authorization setup](https://developer.android.com/identity/authorization): the Android OAuth client registers the distribution package and signing SHA-1; the Web application client is also required by this sign-in setup.
- [Credential Manager Google sign-in](https://codelabs.developers.google.com/sign-in-with-google-android): Android and Web clients have different roles. Do not select a random historical identifier or substitute the QA Android registration for the ordinary app.
- [Drive app-specific data](https://developers.google.com/workspace/drive/api/guides/appdata): preserve the existing narrow `drive.appdata` scope and hidden recovery store. This repair does not move automatic backups into the visible SaafMilk-backup folder; manual exported files are a separate path.

Independent judgment hook run. The confirmed packaging omission is sufficient to explain the current error. Whether the release Android registration is also missing remains an external configuration question, not a confirmed second defect. A nonblank client ID alone cannot prove consent/upload works. A debug/QA success is not ordinary-release acceptance.

## Stop line

No backup redesign, new account system, general UI audit, server migration, SMS work, live database replacement or automatic revocation/deletion of existing Google clients. The earlier browser restriction was resolved by the owner; the existing project was inspected directly. Record unrelated discoveries for later.

## Progress

- [x] Verified the existing Web client and Drive API. Created the exact approved ordinary Android registration and read back its package/release fingerprint. Existing clients/backups retained. Declared only the app-data scope already used by the app. Google branding and Production change were explicitly approved, saved and read back.
- [x] Persistent ignored Web configuration and release guard accepted: missing/malformed rejection and real configured local-file acceptance pass3/3.
- [x] Signed0.6.1/code10 built from native production source b125417; package/certificate/embedded ID/production flags/hash checks pass. Database schema19 unchanged.
- [x] A06 ordinary replacement update verified by exact installed APK hash and retained UID/first-install values. S24 absent; no repeat A06 install is needed.
- [x] User selected the existing admin site for Google branding. Exact admin source7a00363 changed only the landing page and added a public privacy page. Touched-page lint, TypeScript/production build, five local and five live anonymous checks pass; auth source hashes unchanged. Existing Git integration published the verified production commit. No extra site/project or paid service created.
- [x] Save the approved app name/public links/domain and publish Google OAuth. In production read back; Google says non-sensitive data-access verification is not required. Cosmetic brand review remains a deferred note.
- [x] A06 private ordinary-app consent, encrypted upload/listing and non-destructive local-key inspection accepted: format5, security state included, confirmation unchecked and Restore disabled. Returned to protected backup home. No restore or secret capture.
- [ ] S24 update with the frozen artifact when reconnected. Broader independent passphrase recovery/human acceptance retain phase1 scope.

The minimum admin-site extension is two files: existing public homepage information/link and /privacy. No separate About page was needed. Runtime Google publication state is tracked separately from the APK's packaging-time snapshot; changing that state does not require rebuilding identical app code.
