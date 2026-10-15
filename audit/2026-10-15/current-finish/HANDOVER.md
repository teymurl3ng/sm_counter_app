# Current signed candidate and operational handover

**Latest candidate — ordinary PROD blocker repair:** Use signed **0.4.3/code7**, source `62c9d315662e67f871d9c124f86d73675c5f06f1`, [timezone-fix APK](../prod-timezone-fix/Saaf-Milk-0.4.3-prod-timezone-fix.apk), SHA-256 `d467d09d60c6490a8578b3945221c3afe9a34a3b1b492222b38ab8ca4ecf12ae`. Existing signing certificate is unchanged. A06 is updated with identical UID/first-install time and exact installed bytes. Six affected auth checks pass after a reproduced JSON-null timezone refresh failure. Post-patch ordinary-app live acceptance awaits the owner's private sign-in; S24 matching update awaits connection. [Repair result](../prod-timezone-fix/RESULT.md). The 0.4.2 source305a91d candidate block below records the superseded artifact; recovery/setup instructions still apply.

This replaces the old pre-layout-repair candidate instructions. Preparation, installation and technical acceptance do not substitute for private shop setup or the staff rehearsal.

## Exact candidate

- Ordinary Android package: `com.saaf.milkshop`, version `0.4.2`, version code `6`; preview fixtures disabled and not debuggable.
- Production-source commit: `305a91dac8ba70e839e92f80d52de8e4920089a7`. Later published app commits through `9b7f24a` record evidence/docs. This finish adds affected tests and documentation; production source has not changed.
- Signed APK: [Saaf-Milk-0.4.2-credit-label-fix.apk](../../2026-10-14/credit-label-fix/Saaf-Milk-0.4.2-credit-label-fix.apk).
- APK SHA-256: `ec2cf295884fece77d951e8022e3270ab5e0ec8fd9c223943178a677eed64ba0`.
- Signing certificate SHA-256: `9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1`.
- S24 ordinary profile installation was verified on 14 October. A06 ordinary profile was updated and installed bytes verified on 15 October; no uninstall/data clearing. A06's first-install date remains 10 October. The earlier installation script's launcher command was unsupported after the installation/hash checks had succeeded; a supported explicit activity launch then succeeded without reinstalling.
- Signature challenge using the existing protected signing key passes and matches the installed certificate; no private key was exported. Independent backup custody still requires the owner's confirmation. This is not proof of recovery from that independent backup.

Package data retention is expected from the supported update path, but retained business records/login access must be observed inside the actual ordinary app before being marked accepted. The isolated QA app cannot prove the ordinary app's private records.

## Production components

- Viewer source: private `sm-server-view` main `d2c94e15070eeee78628a9513b2eb7270b6bf950`; production alias `https://sm-server-view.vercel.app`.
- Account server tools: private `saaf-milk-account-server` main `c21b9837584725d95e8d5de63c00974fa92e30df`.
- Exact PROD: `tlsuxbghuhlqntkgtrxt`. Reviewed reset guard was installed and catalog-verified before this run; this run changed no schema, deployment, environment, protection or key settings.
- Real browser Owner/Admin fixture sessions reached the production Viewer and reported the expected commit, PROD environment and online account server. Disabled/protected-target denials, successful credential replacement and pending/completion behavior were exercised. All five deliberately created QA Auth identities were retired; original businesses/shops/staff/Auth IDs matched their checkpoints. Fixture termination/audit history is retained.

The reset policy is: Owner may reset linked same-business non-terminated supported accounts, including disabled accounts; Admin may reset active Manager/Cashier/Operations accounts. Reset does not enable an account. Save a newly displayed credential privately; dismissal/status cannot recover it later.

If an outcome is pending or uncertain, use **Check reset status** and preserve its request reference. Do not repeat the Auth change, bypass the participant guard, or force-clear a request. A trusted known-success result permits completion-only retry; it does not authorize another reset. See the published server `PROTECTED_RESET_OPERATIONS.md` for operator procedures. The browser test deliberately marked a trusted known-success fixture request uncertain to verify this path; it did not inject an uncontrolled production Auth outage.

## Actual shop setup and acceptance

1. Open the ordinary app, not Shop Day QA or Native Preview. Sign in privately with the existing local account. Confirm existing records and totals are present before entering new work.
2. Confirm the intended shop identity, PKR/currency, rates, opening stock/balances and assigned staff roles. Do not replace opening data with QA fixtures.
3. Confirm the ordinary app's existing PROD shop binding and privately held server credential. Local passphrase/recovery/PIN and server transport credentials are different. A shared counter device should retain its existing cashier-scoped transport arrangement rather than use an Owner credential just for convenience.
4. Verify ordinary-app PROD sign-in/sync and supported business actions on the intended setup. Do not create fabricated shop transactions as acceptance fixtures. Compare the next real controlled pilot day with the independent existing records.
5. Configure each person's local PIN/phone security privately and observe selected-account cancellation/success. A PIN is local convenience; portable restore still requires the proper backup recovery secret.
6. Confirm who holds the recovery secret and independent encrypted backup/signing-material copies. Do not paste passwords or keys into chat. Take and verify a recoverable encrypted backup through the supported path before replacing the existing record system.

**Lock app / switch account** keeps installation sync active. **Stop syncing** disconnects transport while retaining local records/binding state; it is not deletion or central revocation. Cashier can count/close. Owner/Admin reopening requires authentication and a reason; after reopening, drain the queued refused work and recount/reclose as appropriate.

The separate [Shop Day QA instructions](../../2026-10-14/staff-phone-handoff/START_HERE.md) and staff cards are for disposable rehearsal only. Their shared test passphrase must not be reused for real staff. The uncoached rehearsal, felt haptics/motion/readability, actual enrolled authentication and next-supported-date observation require real human outcomes.

## Recovery and rollback

- Local app records: use the supported encrypted portable backup/restore process, a compatible app and the correct recovery secret. Preserve the sole working shop database and a recoverable backup; do not clear data or downgrade across schema 17 as a rollback shortcut.
- Server identity: use the published empty-destination recovery workflow with a verified backup and separately approved destination. [Hosted synthetic recovery proof](../../2026-10-14/hosted-recovery/RESULT.md) is accepted; it does not certify every private backup. Preserve DEV/PROD during any later rehearsal.
- Signing continuity: future ordinary-app updates must use the recorded signing certificate. The current protected key works; an independent backup recovery should be verified with its actual custodian.
- Viewer/reset incident: retain the guard, pending references and audit. Fail closed; do not roll back to the older unguarded direct-reset route. Prefer a scoped forward repair.

Remaining acceptance and evidence are tracked in [RESULT.md](RESULT.md). Future Credit/reminders and the recovered product backlog remain outside this handover.
