# Ready for owner acceptance — 2026-10-10

Open **Saaf Milk (preview)** on the A06. Its package is com.saaf.milkshop.preview, version 0.4.0-preview, rebuilt from the current repaired source and updated in place today. The acceptance APK is saved beside this document. The preview's existing accounts/data were not cleared. Preparation stops here; no further benchmarks or broad test reruns are scheduled.

The other icon, **Saaf Milk**, belonged to com.saaf.milkshop.qa.benchmark, the isolated instrumentation benchmark. It was not the production app. It is now disabled for user 0, retaining its data, so its icon should disappear. Android rejected an icon-only component change; that failed attempt is saved separately. No production package is installed on this A06 in the captured package inventory, and no production APK was installed by preparation.

## Checks already completed by the agent

- Current preview build succeeds. APK identity is checked before installation; installation uses an in-place update, without uninstall or clear-data. The installed APK hash is checked against the saved acceptance artifact.
- Preview cold launch succeeds. Welcome → Continue → account chooser renders correctly at the device's current settings. Existing owner and preview accounts are visible; screenshots/hierarchies are saved. The bounded crash-buffer scan found no preview-process entry; this is not an exhaustive runtime guarantee.
- The repaired Viewer production build exists and is newer than its repaired source inputs. It is running on this computer at **http://127.0.0.1:3100/login**, bound to localhost and explicitly configured for saafmilk-dev, contract v1.2.0-write-admin. The login surface loads with a DEV badge. Unauthenticated health and overview endpoints return the intended 401. The initial probe incorrectly expected public health access; its failed expectation is preserved, then corrected from route source. No authenticated health/session PASS is claimed.
- Prior UI/domain/restore/database checks and their failures/skips remain preserved in the existing audit evidence. This preparation does not modify their historical snapshots.

## Your check order

1. **A06 visual acceptance:** open Saaf Milk (preview), select your existing owner account and enter your local passphrase. Inspect sale/payment selection, reports/date ranges, close/discard prompts and the motion using your usual display and keyboard settings. Preview staff accounts are available for role checks; their fixture passphrase is shown on the account screen. Those are local fixtures, not DEV server credentials.
2. **Real DEV account acceptance:** open the Viewer URL above **on this computer** and sign in with an authorized DEV owner/admin username and server credential. For the phone's server session, use Owner → More → Sync and explicitly select **DEV (testing)** before enrolling. The app's intentional default is PROD; preparation has not enrolled it or changed that default. Local app sign-in and server enrollment are separate. Use a disposable DEV shop for write tests, and pause before enrolling an existing local history if you have not chosen its recovery authority. Session availability and workflow acceptance remain unverified until a real login is supplied; the agent can then complete protected workflow checks without obtaining your password from storage.
3. **Decisions only if applicable:** identify affected historical records and their authoritative same-shop history; decide provider/password/deletion-policy changes. These are not setup tasks silently left unfinished.

The Viewer is a local running process, not a published website. If it stops, the agent can restart it; do not use the published PROD Viewer for this DEV acceptance. No PROD migration, Android production release or Viewer publication occurred. Production rollout remains a separate phase requiring explicit approval after the earlier automatic approval rejection.

## Internal recovery note

To re-enable the preserved benchmark later, the agent can run `adb -s R8VXA02SNKH shell pm enable --user 0 com.saaf.milkshop.qa.benchmark`. This is not an owner acceptance step. No benchmark reinstall or data reset is needed.

Scope decisions are in PREPARATION_SCOPE.md; current artifact/source/evidence hashes and readiness assertions are in verification-acceptance.json. Earlier F01–F33, S01 and A01 classifications remain unchanged.
