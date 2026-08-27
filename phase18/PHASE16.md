# Phase 16 — ViewModel Integration / Presentation Ownership

## Purpose

Complete the integration of the Phase 15 ViewModel contracts into the existing presentation layer so modeled screen state is authoritative at the presentation boundary, while business/domain/data authorities remain unchanged.

## Internal work units

- 16.1 — Dashboard state ownership
- 16.2 — Backup/Restore state ownership
- 16.3 — History/Corrections state ownership
- 16.4 — Purchase/Sale transaction selection ownership
- 16.5 — Simple transaction selection ownership and remaining state audit
- 16.6 — Final Phase 16 verification

These are internal work units of Phase 16, not separate project phases.

## Current implementation

Dashboard, Backup/Restore, History/Corrections, Purchase, and Sale now use their ViewModels as presentation-state owners for the modeled state covered by this phase.

The five simple transaction forms now use `SimpleTransactionsViewModel` for reference data and selection state used by Vendor Payment, Credit Collection, Expense, and Salary Payment. The existing transaction services remain the authorities for validation, calculations, accounting, and persistence.

Expense category selection is also synchronized through the ViewModel before the existing `ExpenseService` call.

## Constraints

- No SQL or Room schema/semantic changes.
- No domain/business-rule/calculation changes.
- No UI technology migration.
- No XML ID/layout changes.
- Existing service and persistence authorities remain unchanged.
- Existing tests must continue to exercise the same business behavior.

## Verification status

Internal work units 16.1–16.5 are verified. Work unit 16.6 — final Phase 16 verification — is now complete. Phase 16 is fully verified.

Phase 16 correction: SimpleTransactionsViewModel reference data is refreshed after loadSaleData populates customers, ensuring Credit Collection selection validation reads the same customer list displayed by the UI.
