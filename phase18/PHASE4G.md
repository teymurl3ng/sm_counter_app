# Phase 4G — Salary Payment UI

## Scope

Adds the Salary Payment vertical slice on top of the verified Phase 4F project.

## Flow

Salary Payment UI -> existing `SalaryPaymentService` -> atomic Room transaction -> canonical dashboard refresh.

## Inputs

- Employee (Salary account)
- Cash / JazzCash / Bank account
- Amount

The event date uses the same current-date convention as the existing Phase 4 transaction screens (`LocalDate.now()`); no separate date picker is introduced in this slice.

Because Salary accounts are business-specific and intentionally not seeded by SQL, this slice includes Add Employee to create a named Salary account needed for the payment workflow. The created account uses the existing schema meaning: type `Expense`, category `Salary`.

## Business authority

`SalaryPaymentService` remains authoritative for validation, account-category rules, event creation, and ledger movements. The UI does not duplicate salary accounting logic.

## Tests

`Phase4gSalaryUiTest` covers:

- successful salary payment: event count increases, selected Cash balance decreases by the salary amount, and the employee Salary balance increases by the same amount;
- invalid amount: visible synchronous error and no event creation.

`SpinnerWiringSmokeTest` was extended to seed a Salary account for the smoke test and require both Salary spinners to be populated before checking the complete spinner set.

## Verification

This environment cannot execute the Android instrumentation suite. Verify in Android Studio with the complete regression suite plus the Phase 4G test and spinner smoke test.
