# B3 completion plan — three bounded runs

Status: three-run plan complete; B3b implementation is [verified](../withdrawal-account-correction/RESULT.md), B3c local receipt/remedy is [verified](../withdrawal-money-received/RESULT.md), and B3d Records UI is [accepted on S24](B3D_RESULT.md) after the user substituted S24 for unavailable A06. The B3c [server preflight](B3C_SERVER_PREFLIGHT.md) found that opaque event transport needs no new type allowlist; a proposed pusher-role gate was reversed because it conflicts with the shared offline outbox. Remote actor-proof remains in the existing access/transport trust work. Preserve the existing working tree and delivered fixes.

Execution scope update: [B3B_RUN_SCOPE.md](B3B_RUN_SCOPE.md) controlled the completed B3b run. B3b added account/reference truth only; future receipt-review fields and queries remain excluded until B3c.

## Agreed business boundary

Continue the existing M18 meaning: personal owner funds, not an owner loan or receivable. Genuine money received back is a voluntary owner contribution in accounting terms; the withdrawal link supplies history, not a repayment obligation. New receipt type proposed: OWNER_FUNDS_IN, with the user label Owner money received and the action Record money returned.

Both owner-funds directions stay outside sales, operating expenses, salary and business profit. Do not add loans, interest, owner balances owed, payroll, a general capital-management screen or accounting-category expansion.

All mutations keep original headers, ledger rows and UIDs. A correction replaces declared truth by appending a linked event and the required ledger difference. An actual receipt is a separate event, never a cancellation of the original withdrawal.

## Run B3b — corrected account truth and wrong-account remedy

Goal: a withdrawal can move from wrongly recorded Cash to Bank (or another existing Cash/Bank account), and every later correction uses the latest corrected account.

Proposed smallest model extension: an immutable owner_funds_details satellite, keyed by event_id, carrying payment_account_id and withdrawal_root_event_id. Original withdrawal references itself as root; its corrections retain that root. These details declare which account the event's amount describes. Receipt-review fields are excluded from B3b; decide and add them only if required in B3c. Do not infer account truth from the sign of an arbitrary correction row.

Use an additive Room 14 -> 15 migration, if 15 is still the next version when executing. Infer metadata only for unambiguous supported legacy same-account chains, retaining every stored amount/row/UID. A malformed/incomplete chain stays readable and receives an explicit unavailable/review state; never choose an account by guesswork. No ledger rewrite or raw-Double money migration.

CorrectionInput.OwnerDrawing gains an optional corrected payment account, defaulting to current truth so the delivered amount/cancellation path remains valid. Require a changed amount OR changed account, a fresh reason, Owner/Admin permissions and the unchanged original date. Cancellation keeps the last declared account with zero amount. Correcting a cancelled entry remains supported.

Literal ledger rule: old amount A on account X, new amount B on Y. On the same account write A-B. On different accounts write +A to X and -B to Y; omit zero movements. Example: Cash/250 -> Bank/200 means Cash +250, Bank -200. A later Bank/200 -> Bank/150 writes only Bank +50. Cancellation of that latest entry writes Bank +150. Do not touch the old Cash row.

SQL guards validate permitted accounts, parent/root relationships, exact per-account differences and no duplicate/extra/inventory rows. Services, restore and sync validate the complete expected movement set and commit it atomically. Per-row triggers alone cannot prove that both required rows were supplied; do not claim otherwise.

Compatibility belongs in this run:
- Emit owner-funds details inside the event sync envelope, with account/root UID references and an explicit owner-funds contract version.
- Parse/resolve metadata and all children before inserting anything. Park unresolved dependencies and unsupported contracts as whole records.
- Restore metadata before financial rows, ordered by actual dependencies rather than assuming numeric IDs are chronological.
- Include metadata in the identical-header replay/completeness check, even when every ledger row is already present. Fill only identical missing metadata; reject contradictory bodies. Do not reset cursors, replace local pending edits or start a general replay repair.
- Infer supported legacy missing details without erasing modern explicit truth.
- Bump portable snapshot schema separately (currently 10) so old apps refuse a newer snapshot instead of ignoring essential metadata. Preserve older supported backups. All writable clients must be upgraded before writing the new owner-funds shapes; older sync receivers are not assumed to reject new shapes safely. Add parent capture for the new immutable satellite if needed; no broad historical re-push.

End of run: typed/domain wrong-account writes, migration, backup and local UID replay verified. The existing UI still defaults to the current account until B3d; do not install or advertise an exposed account selector yet.

## Run B3c — actual owner money received and its recording remedy

Goal: record a separate positive owner-funds receipt on its actual business date, linked to the original withdrawal UID. Add OWNER_FUNDS_IN plus its detail/root reference. New receipts use an existing Cash/Bank destination (Cash by default), finite positive exact-paisa input and an optional note. No receipt may be dated before its referenced withdrawal or in the future. Use the existing business-date/open-day checks; the receiving date is independently checked even when the withdrawal date is closed.

Extend the same satellite and use a separate guard migration (next Room version after B3b). Perform the bounded source-only server validation preflight for this new event type before declaring transport compatibility. If it needs a remote schema/RPC change, stop the local run with the exact separate patch scope; do not deploy or infer server acceptance from local replay.

New receipt: +R to destination. Same-account receipt correction R -> S: S-R. Account correction X/R -> Y/S: -R on X, +S on Y. Zero cancels a receipt that never happened. Preserve its root link and original receiving date, require a fresh reason and reopening when that date is closed. A genuine subsequent withdrawal is not a receipt cancellation.

New submissions retain a stable event UID across retry; an identical retry returns the already saved receipt, while a different body for that UID is refused. Read back receipt/context inside the write boundary so a late read failure cannot report failure after saving. Existing correction one-child/sibling protection remains.

### Excess and later corrections

Do not impose a loan-like hard repayment cap on voluntary owner funds. Show the current corrected withdrawal, money received so far and the new amount; require explicit review when the proposed total exceeds the reference withdrawal. Require the same review when a withdrawal correction/cancellation leaves received owner funds above its corrected amount. Store the writer's reviewed reference totals/acknowledgement as audit context, not a receiver-side cap based on later-known rows. Re-read current facts inside the local write transaction: if the review context changed, return review-required with the draft intact and write nothing.

Actual receipts stay recorded when a withdrawal is subsequently corrected; there is no automatic debit, deletion or reallocation of received cash. Net owner funds can become positive and still remain outside business profit. A cancelled withdrawal cannot originate further linked receipt entries; existing real receipts remain intact and labelled. No separate general owner-investment entry screen is included.

This is the chosen adaptation of owner-contribution accounting. It avoids blocking legitimate receipts and truthful corrections, and avoids a shared remaining-limit race between offline devices. It does not prove that two different receipt UIDs represent different physical money: stable retry IDs, actor/date/reference history and cash reconciliation remain necessary.

Apply/restore replay validates recorded facts and full ledger shape, not a newly computed acknowledgement requirement against a receiver's later history. Missing references, conflicting corrections or unsupported shapes preserve the whole row for attention; do not silently audit-skip the new metadata and count that as convergence.

End of run: new receipts, amount/account/cancel remedies, review rules, no profit/stock impact, backup and nonconflicting two-database UID replay pass. No new guarantee of general multi-device readiness; the project's remaining profile/planning/acceptance gates still apply.

## Run B3d — deliberate Records UI and final affected acceptance

Scope: current withdrawal correction editor; withdrawal detail's linked receipts; one new returned-money form/detail; small Owner Overview/Records wording for OWNER_FUNDS_IN. Keep Owner/Admin write gating and Manager read-only behavior.

Withdrawal editor adds an account selector and clear before/after amount/account summary. Cancellation cannot silently change an account. The returned-money action starts from a current withdrawal record, with immutable reference, amount, receiving account, actual date (default today) and note. Use a Material date dialog rather than a raw date text field.

Review distinguishes Correct a recording mistake, Cancel a record which never happened, and Money actually received. Show mismatched/excess totals explicitly and require acknowledgement before saving. Saving disables repeat submission; failure preserves draft and recovery; success leaves a saved receipt with clear Done/back action. Use the current feedback, haptic and draft/back-guard patterns, current NativeInformationSection facts, portrait chrome and theme. No auto-dismissed success state.

Return details show received amount/account/date, root link, recorded-by identity, correction/cancel state and note. The linked timeline uses effective receipt amounts for totals while retaining superseded/cancelled records visibly. No new general cash-account ledger, Work menu redesign, typography/capsule audit or header/footer rewrite.

End of run: both new actions are reachable and permission-correct, with loading/empty/failure/saving/saved/closed-date/large-value states checked in the affected QA evidence. Verify the two touched form families in light/night and representative larger text. Check date selection near the shop day boundary. No landscape requirement. The final selected screen checks used S24 by user direction; earlier affected acceptance used A06.

## Finite validation and stop rules

Only the new owner-funds tests and affected existing drawing/correction, backup/sync and migration methods. Independent literal balances and UID-normalized metadata are mandatory; do not compute expected values through production helpers. The case matrix is in ACCEPTANCE.md. No full facade suites or full app regression unless a new change/failure identifies a specific need.

Preflight each run's listed files and existing source hashes, implement its stated output, check it, capture evidence, update B3 status and stop. New unrelated findings go to the existing later queue. No ordinary-app install, commit/push, live staff/data mutations, server publication or deployment in these implementation runs. Release preparation remains its existing separate phase.

B3 closes only after all three runs and their compatibility/UI checks are complete. B5 remains complete within its delivered rounding scope. Do not combine this work with profile/planning receiver repair or menu redesign.
