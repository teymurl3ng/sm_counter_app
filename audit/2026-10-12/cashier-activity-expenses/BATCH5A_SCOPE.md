# Phase 3 batch 5a — cashier's own expenses in Activity

Observed baseline: `NativeActivityFacade` filters the current business date and actor link but allows only sale and collection event types. Expense recording creates the same actor link and an `EXPENSE` event with category and Cash/Bank movements. Cashier Activity therefore omits an expense the cashier just recorded.

Change only the Activity read boundary and required list/detail copy. Include `EXPENSE` in the same date/actor list and owner-checked detail. Resolve category from the expense movement/profile (including archived categories) and payment account from the Cash/Bank movement; show positive amount, category, account and notes. Keep sale/collection behavior, navigation, permissions, date semantics, corrections and receipts intact. No shop-wide financial list, styling redesign, new schema or extra operation types.

Selected tests: two cashiers plus owner expense; current-day versus other date; expense list/detail category, payment account, amount and notes; direct detail denial for another actor; existing collection boundary and Activity screen callbacks; focused S24 QA only. Stop after this data/display boundary. Batch 5b handles visual hierarchy later.
