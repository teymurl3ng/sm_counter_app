# 9b2 baseline and bounded plan complete; close-import repair required first

The offline/closing baseline is established without starting a general audit or changing production. The existing local contract is confirmed: Cashier can count/close, Owner/Admin can reopen with recent authentication and a reason, and reclose needs a fresh count. The transport account does not replace the acting local user's permission checks.

Four selected A06 methods ran in the isolated test package. Three passed: a source-generated verified close receives correctly into an empty-user database, the native Cashier close/next-date/reopen-denial control, and the native Owner close/reopen control. The initialized-receiver acceptance regression failed with the exact audit reason `Daily close requires a verified user`. The source Cashier had successfully closed without changing the ledger; the receiver had a different local Owner and no matching source-user ID. This proves a normal initialized recipient rejects the imported close, rather than proving that local Cashier closing is forbidden. Empty-database receiver coverage does not establish this normal-app behavior.

Implementation independently matches the refusal: applyCloseAction omits the foreign local actor ID; the local SQL guard rejects NULL when app_users exists. Ordinary SyncRuntime pulls advance the cursor after skipped records. The narrow repair therefore also needs retryable refusal retention. Existing trusted backup import already separates this actor constraint during its transaction; the proposed boundary is saved in FIX_SCOPE.md. No remote trigger/schema/API or production source was edited.

## Frozen physical scenario

EXPECTED.json uses exact Decimal arithmetic before execution. Begin from the accepted online post-sale state: Cash10380, Milk78 L, Yogurt17 kg, supplier17576.92 and customer0. One new 1 L cash sale at190 per phone gives each independent local view Cash10570/Milk77; after recovery both must have Cash10760/Milk76 and exactly two new sale movements. Retry/replay must not duplicate them. Count/close/refuse/Owner-reopen/recount/reclose changes no cash or stock; expected histories contain two counts and close/reopen/close.

The physical outage should use USB-controlled A06 while wireless S24 stays reachable, saving/restoring the original connectivity in a finally path. An injected transport failure must be labelled as such and does not certify a radio outage. Closing occurs after the two sales converge; simultaneous late offline posting across a close is not implied by this fixed case. Compare all 18 already accepted business tables plus cash-count/close-action histories and their UID references. Freeze the exact stages and affected checks after 9b2a, before executing.

Only A06 was visible to ADB in this baseline. No radio setting, real DEV fixture, server account, ordinary database or PROD was changed. Ordinary/preview/.qa package provenance matches before/after; the main QA APK exactly matches the accepted 9b1 build. All captured main/schema and existing test hashes remain unchanged. One new SyncClosePreflightTest file and baseline/plan evidence were added. No full suite ran.

## Whole plan

1. ~~Money, rounding and withdrawal remedies~~ — bounded QA complete.
2. ~~Profile/planning receiver and catch-up, including app-generated dates~~ — current online comparisons accepted.
3. ~~Mapped menu/list presentation~~ — bounded QA complete.
4. ~~Backup automation, local app restore, access wording/checks and fresh Counter seed reconciliation~~; hosted identity recovery, reset browser/production and actual device handover remain.
5. ~~Independent oracle, automated shop day, seven representative phone checks, physical online two-phone exchange and offline/closing baseline~~; next 9b2a close-import repair, then physical 9b2 offline/close/reopen, 9b3 interrupted retry/recovery and staff/remaining human checks.
6. Freeze, commit/push, signing recovery, signed ordinary upgrade/install and preservation check remain.

Nothing from the unexecuted physical offline/closing scenario is marked passed. This dependency stays within the existing closing acceptance; it is not a new product feature.
