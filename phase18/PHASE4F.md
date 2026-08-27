# Phase 4F — Expense UI

## Scope

Adds the Expense vertical slice on top of the verified Phase 4E project.

## Flow

Expense UI -> existing `ExpenseService` -> atomic Room transaction -> canonical dashboard refresh.

## Inputs

- Expense category
- Cash / JazzCash / Bank account
- Amount

The event date uses the same current-date convention as the existing Phase 4 transaction screens (`LocalDate.now()`); no separate date picker is introduced in this slice.

## Business authority

`ExpenseService` remains authoritative for validation, account-category rules, event creation, and ledger movements. The UI does not duplicate expense accounting logic.

Expense categories are exposed by `ExpenseService.EXPENSE_CATEGORIES` so the UI does not maintain a second business-rule list.

## Tests

`Phase4fExpenseUiTest` covers:

- successful expense: event count increases and selected Cash balance decreases by the expense amount;
- invalid amount: visible error and no event creation.

`SpinnerWiringSmokeTest` was extended only to wait for the new expense spinners to be populated before checking the complete spinner set.

## Verification

This environment cannot execute the Android instrumentation suite. Verify in Android Studio with the complete regression suite plus the Phase 4F test and spinner smoke test.
