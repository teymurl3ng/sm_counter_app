# Exact Google-side release setup

Use the existing project after verifying its identity in Google Cloud Console. The historical QA project name is `saaf-milk-qa`; its current registration is not yet verified. Preserve existing clients, consent grants and recovery files.

Required ordinary Android OAuth registration:

- Package: `com.saaf.milkshop`
- Release certificate SHA-1: `4E:F6:28:33:3D:8B:DF:29:E8:D4:06:F5:F6:72:6C:12:90:F3:14:13`
- Certificate SHA-256 for independent comparison: `9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1`

The same project must have the appropriate Web application client for Credential Manager and the Google Drive API enabled. Copy only its public Web client ID into ignored `phase18/oauth.properties` as `googleWebClientId=...`; no client secret is used by this Android integration. The example file can be copied as a starting point. Explicit `-Psaaf.googleWebClientId=...` overrides the local file, including an intentionally empty override used by the rejection check.

Keep the existing `https://www.googleapis.com/auth/drive.appdata` scope. Automatic recovery files use hidden app-specific storage; the visible SaafMilk-backup folder is the separate destination for manual exports and signing custody.

Inspect the consent/audience status rather than assuming the old QA testing configuration is suitable for normal operation. [Google documents](https://developers.google.com/identity/protocols/oauth2#expiration) seven-day refresh-token expiry for External/Testing apps when requesting scopes beyond basic identity. Check production readiness and any actual Google requirements before claiming unattended backup is ready. Do not expand Drive permissions or purchase services for this repair.

Record the verified project/client relationship and a hash of the public Web ID in GOOGLE_REGISTRATION.json after reading the actual console configuration. Build configuration alone cannot prove that Google accepts the app.

The release build blocks absent/malformed IDs. The packaging script also checks the configured ID in the generated BuildConfig and APK, unchanged release certificate, expected package/version, production flags and input hashes. Finish with real on-device consent, encrypted upload, listing and non-destructive inspection, with private passphrases entered by the owner.
