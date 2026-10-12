# Withdrawal recording remedy — first unit complete

Owner/Admin can now open a withdrawal in Records, correct its recorded amount on the original account, or explicitly cancel it if it never happened. A fresh reason is required. Cancellation shows zero corrected amount and a Save cancellation button; the saved record is labelled Cancelled. The original event, UID and cash movement remain in history.

The new event carries corrected truth and a linked cash difference. For example, correcting Rs 250 to Rs 200 adds Rs 50 back to the original account in the records; cancelling the corrected Rs 200 adds Rs 200 back. This corrects a recording mistake. It does not describe cash physically returned by the owner.

The SQL guard permits only the exact linked difference on the original root's single Cash/Bank account. It refuses arbitrary positive withdrawal rows, a different account, extra financial rows and inventory movement. The domain rejects unchanged amounts, nonfinite/negative/subpaisa/out-of-range values and missing reasons. Manager/Cashier cannot use the remedy. Corrections retain the original date, so closed-day checks cannot be bypassed by choosing today. Failed writes roll back event, ledger, attribution, audit and outbox work together.

Room migration 13 -> 14 replaces the three affected validation triggers. It adds no entity fields or event types and rewrites no saved ledger rows. Existing correction UID serialization is reused. No frozen business specification or SQL blueprint changed.

## Evidence

16 latest focused checks passed, zero skipped:
- 10 final A06 core cases: amount decrease/increase, cancellation/restoration chain, history/actor preservation, no operating-expense or stock impact, bad-input/repeated-request refusal, injected rollback, wrong-role and closed-date refusal, strict SQL shape, backup restoration and idempotent UID replay, v13 -> v14 upgrade, ordinary monetary corrections and preserved legacy cash-close history across the current upgrade chain.
- Two A06 Records UI cases: explicit cancellation choice/required reason/save wording, and ordinary correction editor access.
- Four ordinary OwnerDrawing host tests.

Exact methods, XML and source hashes are in verification.json. The initial device attempt could not find the former USB connection; the same physical A06 was verified by its serial and tested over a stable wireless address. Those connection failures executed zero tests. The first real device run caught the unchanged Save correction label; the implementation was fixed and both affected UI cases passed. Final core and host checks were captured again. Failed-run console logs remain available.

The judgment hook ran as a reminder; it is not an independent validator. Diff whitespace and frozen baseline checks passed. Only the mapped Records editor/view-model changed among native UI files; the rest of the current UI work is preserved. No full suite, ordinary-app installation, commit/push, remote deployment or shop-data change occurred.

## Remaining scope

B3 is PARTIAL. This unit cannot change the recorded account or record actual cash returned. The next unit must represent those explicitly, including corrected account truth, original withdrawal UID, partial/full return totals, repeated/over-return policy, correction of a mistaken return, and backup/sync/report/UI behavior. Do not use cancellation when the withdrawal really happened and money was later returned.

B5 rounding remains verified. Phase 1 is incomplete until the remaining withdrawal unit is finished. The ordinary installed app has not received these changes yet.
