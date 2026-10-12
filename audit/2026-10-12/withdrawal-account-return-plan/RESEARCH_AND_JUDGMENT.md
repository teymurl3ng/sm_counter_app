# Research and independent judgement

Official sources were read on 2026-10-12:
- [QuickBooks: equity](https://quickbooks.intuit.com/learn-support/en-us/help-article/equity/equity/L5Ic6InlI_US_en_US): distinguishes equity withdrawals from expenses and distinguishes owner draws from money lent with a repayment obligation.
- [QuickBooks: owner contributions](https://quickbooks.intuit.com/learn-support/en-us/help-article/bank-transactions/record-owner-contribution-capital-investment/L4CbzMP6h_US_en_US): records owner money entering the business as a dated deposit to the selected receiving account with equity classification.
- [QuickBooks: audit log](https://quickbooks.intuit.com/learn-support/en-global/help-article/audit-log/use-audit-log-quickbooks-online/L2WoVnW6I_ROW_en): retains who changed a transaction and its original details.
- [Android: Compose date pickers](https://developer.android.com/develop/ui/compose/components/datepickers): supports modal calendar selection with DatePickerDialog.

These sources support the accounting distinction, selected receiving account/date and retained change history. They do not prescribe this app's schema, offline sync policy, approval threshold or exact UI. Those are the proposed adaptations below, not claimed industry mandates.

Independent alternatives considered:
- Reuse cancellation for real cash received: rejected because it changes historical withdrawal truth instead of recording a later receipt.
- Infer corrected account from whichever correction row appears first: rejected by the two-leg account-change example; both an old-account credit and a new-account debit exist, and later amount-only changes must use the latest declared account.
- Expand generic events with several nullable owner-specific columns: workable, but a small immutable satellite keeps these fields local to owner-funds events and fits the existing satellite/envelope pattern. It still requires complete restore/replay integration.
- Treat returned funds as sales or negative operating expense: rejected by the equity classification and the current M18 model.
- Hard-cap all money received at a withdrawal amount: rejected as a universal financial invariant. Voluntary owner contributions can exceed a reference withdrawal, and a subsequent truthful withdrawal correction must not erase real deposits. Explicit review is the scoped typo/confusion safeguard; this is not a loan feature.
- Blindly apply a writer acknowledgement against a receiver's later totals: rejected; it would reinterpret historical evidence and create order-dependent replay refusals. Store and validate reviewed context as audit evidence.

The judgment hook was executed and its fixed reminder is saved. It has no independent code/design validation ability. Conclusions rely on inspected implementation, checked source hashes/XML and these primary references. Neither the earlier success summary nor the user's confidence establishes new compatibility or new test results.

Unverified boundaries kept explicit: live remote validation/deployment, future general multi-phone acceptance, and readiness of unrelated profile/planning receivers. No new server/recovery investigation, financial-storage redesign or whole-app visual audit was opened.
