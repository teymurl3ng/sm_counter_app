# Remaining work — simple phase brief

This is the plain-English version of PLAN.md. All 51 backlog items are accounted for, but some are future features or checks that may find no problem. They are not 51 confirmed bugs or 51 requirements before a shop trial.

## Phase 1 — Make money handling consistent

Check that sales, collections, expenses and corrections round amounts using the same agreed rule. Then add a proper way to correct or cancel a wrongly recorded owner withdrawal, while keeping the original record and the reason for the change.

Finished when the affected calculations match independently written expected amounts, and withdrawal corrections change cash correctly without changing business profit. Existing records must remain intact.

Start with the small rounding check. Withdrawal corrections are a separate run because they affect the financial rules and saved records.

## Phase 2 — Make two phones receive complete information

The app already sends product and people details, budgets and collection targets. Some of that information is not fully applied on the receiving phone. Complete that receiving work and provide a controlled way to recover information previously skipped.

Finished when two isolated copies agree after edits, offline work and repeated sync, without losing unsent work or creating duplicates. Actual two-phone acceptance comes in phase 5.

This phase is required before relying on two writable phones. A restricted single-device trial can happen without it if that limitation is explicit.

## Phase 3 — Finish the remaining menu and list design

Reuse the current app style and make text, values, status and actions easier to distinguish. Work in four separate groups:

1. Personal security settings, including consistent PIN setup.
2. Records, products, sales and deliveries, including their detail screens. First add the cashier's own expenses to Activity as a separate functional change.
3. Customers, suppliers, employees and user accounts, including their details.
4. Backup, sync, costing, reports and security history.

Finished when the mapped screens are readable in light and night modes, work with larger text, align consistently and retain their existing actions. Each group is completed once. Portrait stays the supported layout.

## Phase 4 — Check permissions, recovery and shop setup

Restrict sensitive account resets appropriately, prove that backups can actually be restored in a safe test environment, and verify the intended shop-device setup. Explain clearly what locking, disconnecting and revoking access do.

Finished when denied actions cannot change accounts, recovery reproduces the expected records, and the shop has a documented access and recovery procedure. No existing staff credentials or live shop data are changed as part of testing.

Central control over every person's access is a separate decision if the deployment needs it; it is not a flag we simply switch on.

## Phase 5 — Rehearse a real shop day

Use test data to open the day, receive stock, produce goods, sell, collect money, record an expense, deliver, count cash, close, correct a mistake and reopen when allowed. Compare cash, stock and customer balances against expected numbers prepared independently.

Also check the actual phone experience: PIN and biometric cancellation, Back, haptics, screen transitions, keyboard and larger text. If two writable phones are intended, test both together after phase 2.

Finished when the intended users can complete the tasks without being coached around confusing screens, the totals agree, and the required recovery and phone checks are recorded.

## Phase 6 — Prepare the accepted release

Stop feature work, record the exact source and test evidence, commit and push the accepted changes, and prepare the signed ordinary app update. Verify that upgrading preserves existing shop records and document recovery or rollback options.

Finished when there is an identifiable release build, its relevant checks pass, and any remaining limitations are clearly recorded. Release preparation and publication are separate actions where authorization is needed.

## How we keep this manageable

- One small batch per run, even when a phase contains several batches.
- Run tests affected by that batch. Any broader final checkpoint is selected separately on frozen source.
- Record new unrelated findings for later instead of expanding the current run.
- Keep future features, landscape, broad cleanup and dependency upgrades outside this completion plan.
- Most implementation and test preparation can be automated. Staff still need to try the actual workflow; felt haptics and practical clarity cannot be established by automated tests alone.

The first implementation run is phase 1's rounding check. A supervised synthetic rehearsal can begin early; live use requires the applicable money, access, recovery and release checks. Finishing the visual work alone does not establish readiness for business records.

Execution update: the scoped rounding work is complete and verified on A06. The next run covers correcting or cancelling an owner withdrawal. Phase 1 is finished only when that separate remedy is verified too. See ../canonical-cash-rounding/RESULT.md. The installed ordinary app has not been updated by these test runs.

2026-10-12: correcting the recorded withdrawal amount and cancelling a withdrawal which never happened are complete. The remaining withdrawal work is changing a wrongly recorded account and recording money actually returned to the shop. Phase 1 still needs that final unit.

2026-10-12 final update: the wrong-account correction, actual money received/remedy and Records UI are complete at their bounded QA scope. The final focused screen checks passed on S24 after A06 became unavailable. Phase 1 is complete; phase 2 profile receiving is next. See [B3d result](../../2026-10-12/withdrawal-account-return-plan/B3D_RESULT.md) and [current baseline](../../../CURRENT_BASELINE.md).

2026-10-12 phase-2 update: the five-family profile receiver and its one-time historical catch-up are complete at their bounded QA scope. The separate planning receiver/catch-up batch B2/B10 is next. Physical two-phone acceptance remains phase 5. See [B1b result](../../2026-10-12/profile-receiver/B1B_RESULT.md).
