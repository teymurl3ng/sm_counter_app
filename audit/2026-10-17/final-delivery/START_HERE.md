# Your app handover — 0.6.0

Use **Saaf Milk**, the ordinary app. **Shop Day QA**, **Native Preview** and other QA apps are separate test copies. Never enter real shop transactions in those test apps.

## What this update includes

- Customer Credit balances, payment terms and review history.
- Statements with PDF/image sharing, manual WhatsApp/SMS drafts and clear draft/share history.
- Owner follow-up lists, weekly/monthly/custom statement-review schedules, pause/postpone, quiet alerts and shared message-attempt review.
- Simpler wording across these new screens.

Automatic customer SMS is **not set up**. Messages still need to be reviewed and sent manually in the chosen message app, or typed on the shop's separate phone. Opening a message app does not prove sending or delivery.

## First use after the update

1. Sign in privately to your existing account. Do not register another shop or replace its opening figures. Never send your password, PIN or recovery secret in chat.
2. Check the shop name, a known Overview total and one familiar record/customer. Confirm these match your existing records. Update installation checks alone cannot prove those figures.
3. Open **Sync**, check the intended PROD shop, and complete an ordinary sync. If it fails or shows entries needing review, keep the reference and report it; do not clear app data or repeatedly recreate the connection.
4. Under **Customers**, open a familiar customer's statement and review it without sending. Open **Follow-ups** to see the available controls. Enabling shop-wide reminders and choosing an alert device is an explicit Owner choice; it is not automatically done by this handover. Grant notifications only if you want alerts on the selected device.

Changing payment terms applies to new credit sales after sync confirms the change. Existing sales keep their agreed deadlines. Statement timing does not extend them. Update and sync all sales devices before enabling the compatible Credit/reminder settings; an offline old app cannot participate safely. Review marks are separate from calculated debt status.

## Staff acceptance still needs people

Use your normal font settings. Check readability in Overview, Customers, Shop settings, Sale and Count cash. Open and close the common milk/delivery forms a few times and judge motion and vibration. Check private phone-security cancellation and successful unlock without sharing its PIN.

For a practice sale/day-close with invented figures, use the separate [Shop Day QA task cards](../../2026-10-14/staff-phone-handoff/STAFF_TASK_CARDS.md). Do not put practice figures into the real shop. Record help needed, confusing words and failures. The next-date observation waits for the next actual supported date; do not change the clock to fake it.

Cashier can count and close. Owner/Admin reopening requires authentication and a reason. Locking/switching accounts does not stop installation sync; stopping sync does not erase records or centrally revoke the device.

## Backup and recovery custody

Before replacing your previous record system, choose **Export backup** in the app's backup screen and make a supported **encrypted** shop backup. Choose and privately save its **Backup passphrase**. This is separate from your sign-in passphrase, Quick PIN and Owner recovery code. Store a copy outside the phone and outside this working project folder, and make sure its intended custodian can retrieve the correct backup passphrase. A copied file is not proof it can be restored; verify through the supported isolated recovery path, never by overwriting the sole live shop database.

The app signing keystore and its protected signing settings/passwords also need an independent private backup. Losing them can prevent future ordinary updates. A working current signing key is not proof of an independent recovered copy. Do not put keys, signing passwords, recovery secrets or raw shop exports into Git or chat.

If independent copies are missing, name a private destination/custodian and we can finish that step. No key or secret needs to be pasted here. Phone-security PIN convenience does not replace the portable-backup recovery secret; server sign-in credentials are a separate recovery concern.

## If something fails

Keep the current installation and records. Record the screen/action/error. Do not uninstall, clear data or install an older app over the upgraded database. Use a scoped forward repair or the supported recovery process with a verified backup. An unconfirmed message attempt needs review before another attempt; it must not be silently retried.

Human completion report: phone; private sign-in/known records/sync; readability/motion/vibration; unlock cancel/success; staff tasks needing help; backup/recovery/signing custody confirmed or still missing. No secret values are needed.
