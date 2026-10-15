# Ordinary-app live PROD blocker

The user's already-open ordinary A06 app is configured for the intended shop. Foreground is `com.saaf.milkshop/.Main`, user 0, and installed bytes match source305a91d. Owner access, existing balances, September sale history and one historical sale's inventory/cash detail render. Screenshots are local ignored artifacts; do not publish private account names or financial figures.

One normal Sync now cycle with zero queued uploads failed visibly with `Unknown time-zone ID: null`. `AccountAuthClient.verifyProfile` uses `optString` on nullable JSON timezone fields. Android JSON null becomes the nonempty literal `null`; it wins over a valid per-shop fallback and reaches `BusinessClock.setZone` during refresh. Fix only those timezone reads, retaining explicit valid values and existing fallback/cache behavior. No server timezone/settings/schema, credentials, permissions, balances or transactions are to be changed.

Reproduce with the real Android JSON implementation in isolated QA, repair the parser, run only affected auth/refresh checks, commit a versioned signed patch, update connected A06 without clearing data, then observe one ordinary live sync. Previous17 presentation checks are credited; no full suite or new feature. S24 matching patch installation depends on connectivity. Independent backup custody and human/staff acceptance remain separate.
