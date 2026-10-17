# Current app delivery and handover

17 October 2026. Current signed candidate: **0.6.0/code9**.

**Open release defect — built-in Google Drive connection:** 0.6.0 has an empty Google OAuth Web client ID and cannot connect through the app's built-in Drive flow. Manual encrypted export and the independently verified signing backup are separate paths. A scoped 0.6.1/code10 repair is in preparation; it is not yet built or installed. Missing/malformed configuration rejection passes2/2; actual Google-side release registration, configured packaging and real consent/upload/inspection remain pending. [Repair scope and progress](audit/2026-10-17/drive-release-fix/PLAN.md).

**A06 and S24:** both run verified signed **0.6.0/code9**, with their existing installations retained. A signed-in ordinary Overview was observed on A06 after private sign-in. Known financial records, live sync and human staff outcomes still need acceptance.

**Independent signing backup verified:** the selected **SaafMilk-backup** folder is on the correct Google account and is private, with only the owner permission reported. Its existing keystore matches the current release key byte for byte. The downloaded key and settings successfully signed and verified a random challenge with the release certificate. Temporary private downloads were removed; no Drive file was changed. These are password-protected keystore and separate signing-settings files, not an additional encrypted archive. Keep the folder private. [Non-secret verification](audit/2026-10-17/final-delivery/CUSTODY_VERIFICATION.json).

**Next private step:** no shop-data backup was present in that folder at inspection. Finish or cancel any current rate edits, then use **More → Backup & restore → Export backup**. Privately choose/save a separate backup passphrase of at least15 characters and export to **Google Drive → SaafMilk-backup**. Never send that passphrase here. The encrypted shop copy and supported isolated recovery remain unverified; never restore over the sole live database as a test.

Start with [the short handover guide](audit/2026-10-17/final-delivery/START_HERE.md). [Technical result and exact pending actions](audit/2026-10-17/final-delivery/RESULT.md), [release manifest](audit/2026-10-17/final-delivery/RELEASE_MANIFEST.json), [phone installation evidence](audit/2026-10-17/final-delivery/RELEASE_INSTALLS.json), [local signed APK](audit/2026-10-17/final-delivery/Saaf-Milk-0.6.0.apk).

This includes completed statements, manual messages, Owner reminders and simpler wording. Automatic customer SMS is not set up and is not a prerequisite to using those completed features. No real shop was opted in by the delivery process.

Earlier dated 0.4.x/0.5.0 handover notes remain historical evidence, not current APK instructions. Use [the six-phase plan](NEXT_WORK_PLAN_2026-10-15.md) for completion status; the parked backlog is separate.
