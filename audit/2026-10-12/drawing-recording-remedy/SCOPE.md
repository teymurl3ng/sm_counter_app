# Withdrawal amendment and finite scope

B3 is split into two implementation units. This run implements only same-account recorded-amount correction and cancellation of a withdrawal which never happened. It does not implement wrong-account correction or an actual cash return. B3 remains partial until those separate semantics and their tests are complete.

Evidence: OwnerDrawingService records a positive event amount and a negative Cash/Bank ledger movement. CorrectionInput.Monetary excludes this type. Records supplies no correction prefill for it. SQL rejects positive OWNER_DRAWING movements and all zero event amounts. The comment suggesting a reverse drawing is not an executable remedy.

Industry basis: QuickBooks identifies an owner's draw as an equity withdrawal and distinguishes owner contributions as money put into the business; its audit log retains changes. We adapt that distinction to this app's existing immutable correction chain rather than treating returned cash as a cancelled historical withdrawal.
Sources: https://quickbooks.intuit.com/learn-support/en-us/help-article/equity/equity/L5Ic6InlI_US_en_US ; https://quickbooks.intuit.com/learn-support/en-us/help-article/chart-accounts/set-pay-owner-draw/L3AN10JVe_US_en_US ; https://quickbooks.intuit.com/learn-support/en-global/help-article/audit-log/use-audit-log-quickbooks-online/L2WoVnW6I_ROW_en

This unit:
- Owner/Admin only, retaining CORRECTION and OWNER_DRAWING permissions together. Manager generic correction permission is insufficient.
- Typed drawing correction amount: nonnegative exact paisa, finite and within Long range. Zero means explicit cancellation. A changed amount and a fresh mandatory reason are required.
- Date must remain the original business date; a closed date requires authorized reopening. Cannot move an old correction to today's date to bypass the lock.
- Append a same-type event linked by corrects_event_id. Preserve original header/ledger/UID. Existing one-child constraint rejects repeated attempts.
- Resolve the original root's single Cash/Bank account. This unit cannot change it. Ledger delta = parent corrected amount minus new corrected amount; cancellation restores the previously recorded withdrawal. Restoring an accidental cancellation is another linked correction.
- SQL permits a positive drawing row only for a linked correction, on the root account, with exactly that delta and one row. Never permit arbitrary positive drawing rows. Cancellation zero is allowed only for a linked OWNER_DRAWING event with a reason.
- Existing correction UI gets a distinct Drawing kind, explicit cancellation choice, original account explanation, mandatory reason and cancellation wording. Existing success/error/draft/haptic flow is reused.
- Room 13 -> 14 updates only the affected event/financial guard triggers. No entity fields, money storage, new bridge endpoint or remote migration. Existing UID correction serialization/restore/replay must be tested.

Finite files: Correction.kt, OwnerDrawing.kt (comment), HistoryBridgeCommandService.kt, NativeOwnerRecordsFacade.kt, NativeOwnerRecordsViewModel.kt, OwnerRecordsScreen.kt, DatabaseSql.kt, MilkShopDatabase.kt, generated Room14 schema; focused new tests and this evidence. Frozen spec/SQL unchanged. No other presentation or server work.

Acceptance: lower/higher amount, cancellation/restoration chain, repeated request, missing reason/nonfinite/subpaisa input, wrong role and closed-date bypass refusal; direct SQL wrong delta/account/positive unlinked movement refusal; atomic rollback; no profit/inventory effect; backup and UID replay; v13 upgrade plus selected ordinary drawing/correction/UI checks. A06 isolated test package only. No full suite or ordinary-app install.

Next unit amendment boundary: wrong-account correction needs explicit corrected account truth instead of reading an arbitrary correction leg. Actual return needs a distinct purpose and original withdrawal UID, current-date money-in behavior, partial/full-return totals, over-return policy, return correction semantics, and backup/sync/report/UI mapping. Choose and implement those together in their own bounded representation review; do not reuse cancellation for actual money received.

Necessary dependencies: inventory guard explicitly refuses OWNER_DRAWING inventory rows, so v14 replaces that third trigger too. CashRoundingCompatibilityTest factory includes 13 -> 14 so its existing v12 -> v13 legacy-history check can open the current Room database. No other rounding behavior changes.
