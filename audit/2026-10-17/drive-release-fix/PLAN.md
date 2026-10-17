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

No backup redesign, new account system, general UI audit, server migration, SMS work, live database replacement or automatic revocation/deletion of existing Google clients. Browser access is currently disabled in Work settings; user has been asked to enable it and open the existing project while local preparation continues. Record unrelated discoveries for later.

## Progress

- [ ] Google-side release registration verified/corrected.
- [ ] Persistent verified local configuration; release guard already rejects missing and malformed IDs through actual preReleaseBuild (2/2).
- [ ] Signed 0.6.1 package checks accepted.
- [ ] Ordinary update and real Drive connection/upload/inspection accepted.
