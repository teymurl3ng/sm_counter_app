# Bounded PROD timezone repair

The user's currently open A06 app was the ordinary `com.saaf.milkshop` package/user 0 and matched the installed source305a91d APK. Signed-in Owner access, PROD/verified/active sync status, existing Overview balances, historical Records and one sale's inventory/cash detail were observed. No credential was entered by the agent and no financial transaction or setting was changed. Private screenshots remain local and ignored.

One normal live Sync now attempt, starting with zero queued uploads, failed with `Unknown time-zone ID: null`. Source inspection isolated the profile parser's nullable timezone reads: Android `optString` converts JSON null into a nonempty string, overriding a valid per-shop fallback; refresh then sends it to `BusinessClock.setZone`.

The isolated real-Android regression reproduces the bug before the repair: expected Asia/Karachi, got the literal string null. [Before](before-results.json). The fix checks JSON null before reading each of the three timezone fields. Explicit zones still win, a valid shop fallback works, and absent zones stay absent so the cached clock zone is retained. No hardcoded timezone, auth-policy change, server/schema setting change, data migration or record repair was introduced.

Six affected auth/refresh checks pass, including three timezone cases, ordinary allowed sign-in, disabled/no-profile classification and unknown outcomes. [After](after-results.json). Existing seventeen presentation checks are credited; no broad suite was rerun.

The patch is versioned 0.4.3/code7 from committed source `62c9d315662e67f871d9c124f86d73675c5f06f1`. Signed release assembly passes; non-debuggable/preview-disabled package and the existing certificate are verified. APK SHA-256: `d467d09d60c6490a8578b3945221c3afe9a34a3b1b492222b38ab8ca4ecf12ae`. [Manifest](PACKAGE_MANIFEST.json).

A06 received a normal profile0 replacement update without uninstalling or clearing data. UID and first-install time are unchanged; installed bytes match the signed artifact and the ordinary app launches. [Installation](a06-installed.json). Restart correctly returned to the welcome/private-account flow. The owner has been asked to sign in privately again for post-patch live sync and sampled retained-record verification. Compilation/fake HTTP checks and successful installation alone do not prove that final live result.

The earlier305a91d candidate/freeze records are historical after this production repair. S24 is absent from ADB and one mDNS discovery; it needs the same patch when connected. Human staff checks and independent backup/signing custody remain their existing open items. No broad reconnection or testing run was opened.
