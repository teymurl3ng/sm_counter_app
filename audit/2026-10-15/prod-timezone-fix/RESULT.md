# Bounded PROD timezone repair

The user's currently open A06 app was the ordinary `com.saaf.milkshop` package/user 0 and matched the installed source305a91d APK. Signed-in Owner access, PROD/verified/active sync status, existing Overview balances, historical Records and one sale's inventory/cash detail were observed. No credential was entered by the agent and no financial transaction or setting was changed. Private screenshots remain local and ignored.

One normal live Sync now attempt, starting with zero queued uploads, failed with `Unknown time-zone ID: null`. Source inspection isolated the profile parser's nullable timezone reads: Android `optString` converts JSON null into a nonempty string, overriding a valid per-shop fallback; refresh then sends it to `BusinessClock.setZone`.

The isolated real-Android regression reproduces the bug before the repair: expected Asia/Karachi, got the literal string null. [Before](before-results.json). The fix checks JSON null before reading each of the three timezone fields. Explicit zones still win, a valid shop fallback works, and absent zones stay absent so the cached clock zone is retained. No hardcoded timezone, auth-policy change, server/schema setting change, data migration or record repair was introduced.

Six affected auth/refresh checks pass, including three timezone cases, ordinary allowed sign-in, disabled/no-profile classification and unknown outcomes. [After](after-results.json). Existing seventeen presentation checks are credited; no broad suite was rerun.

The patch is versioned 0.4.3/code7 for ordinary signed installation. Build/installation/live sync acceptance is recorded separately; compilation and fake HTTP checks alone do not prove the live phone sync succeeds. The earlier305a91d candidate/freeze records are historical after this production repair. S24 needs the same patch when connected. Human staff checks and independent backup/signing custody remain their existing open items.
