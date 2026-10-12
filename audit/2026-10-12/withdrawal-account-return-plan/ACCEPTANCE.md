# Finite owner-funds acceptance matrix

Use literal independently prepared expected balances. Example base: Cash 1,000, Bank 500; original Cash withdrawal 250 => Cash 750, Bank 500.

B3b cases:
1. Move that withdrawal to Bank/250 => Cash 1,000, Bank 250. Original Cash -250 stays unchanged.
2. Move it to Bank/200 => Cash 1,000, Bank 300; then amount 150 => Bank 350; cancellation => Bank 500; restore Bank/125.50 => Bank 374.50.
3. Account-only correction with unchanged amount, amount-and-account correction, same-account delivered amount/cancel paths, correction after a previous account change.
4. No change, missing reason, invalid/non-Cash/Bank account, nonfinite/subpaisa/unsupported amount, Manager/Cashier write, closed original date and date-change bypass all refuse without partial write.
5. Inject failure on the second cash leg and after ledger/before attribution. Header, both legs, details, actor, audit and outbox roll back.
6. SQL rejects wrong signed/extra/duplicate/account/inventory movements. Supported write boundaries reject missing legs/contradictory details. Do not equate row-trigger validation with full transaction completeness.
7. Room14 upgrade and older supported same-account history preserve raw rows, values, UIDs and close-policy markers. Ambiguous metadata gets no invented account.
8. Legacy backup without details, new backup with details, different numeric IDs on replay, metadata-only missing-child repair, repeat replay and missing account/root dependency. Complete event or persistent refusal, never a partial write.

B3c cases (fresh Cash1,000/Bank500 base with Cash withdrawal250):
9. Receive100 into Cash => Cash850/Bank500; receive remaining150 into Bank => Cash850/Bank650. Withdrawal stays250. No sales/expense/profit/stock change.
10. Receipt100->80 on Cash writes -20. Change the receipt to Bank/80 retracts its current Cash amount and credits Bank80. Cancel that receipt retracts the last credited amount only. Restore a cancelled receipt remains linked and audited.
11. Actual receiving date can be later than a closed withdrawal date; a closed receiving date, future date or date before withdrawal is refused. Receipt corrections retain the original receiving date; correcting the wrong date uses cancel-and-record with actual dates, not a silent date edit.
12. Excess reference: receive300 after withdrawal250. Require explicit review; Cash becomes1,050, business profit remains0. It is owner funds, not sales. No arbitrary repayment cap.
13. Later lower/cancelled withdrawal with already received funds: show/review the changed relationship, preserve the real receipt and apply only the withdrawal correction. Never cancel cash received automatically.
14. Exact retry UID does not duplicate receipt; same UID/different facts refuses. Repeat sync/restore preserves all metadata and row counts. New unsupported type/missing dependency is retained for attention.
15. Ordinary and encrypted backup round trips, old-backup inference, source-only server event-type compatibility preflight, and two isolated databases with different IDs compare full headers, details, cash legs and attribution snapshots. No claim about live transport or competing physical receipts without their separate acceptance.

B3d cases:
16. Owner/Admin can select corrected account and review/cancel; Manager/Cashier cannot submit or use a hidden alternate path.
17. Root reference cannot be silently changed. Account/date/amount controls submit exact intended facts; ordinary versus mismatch review and persistent success receipt work; duplicate tap/retry is protected.
18. Saving/error/retry/Back/discard, closed-date refusal, no linked receipts, corrections/cancellations in the timeline, long account names and large values, light/night and representative larger text on A06. Header/footer remain fixed. Date dialog uses selected business date without timezone shifts.

Each execution run selects only its corresponding new cases and the existing methods it actually affects. Planning has executed none of these new cases.
