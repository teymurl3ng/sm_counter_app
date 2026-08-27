# Phase 6 — Architecture Audit / Carry-Forward Items

Status: **Closed as an audit record; observations carried into Phase 7. No immediate refactor required.**

This document preserves the Phase 5C/Phase 6 architectural findings and records how they were carried forward. Phase 6A–6D were implemented without changing the frozen core/SQL design.

## GREEN — Proven / Leave Unchanged

- Business logic remains in the domain/service layer.
- UI does not directly create ledger/event records.
- Core formulas remain centralized rather than duplicated in screens.
- Corrections continue through `CorrectionService`.
- Canonical read models remain the source for dashboard state.
- History/detail UI reads the existing event and ledger model rather than creating a second history store.
- Multi-row writes remain atomic through Room write transactions.
- SQLite foreign-key enforcement is enabled on connection open.
- Room-side integrity triggers/checks reflect the SQL blueprint's integrity model.
- Vendor, customer, and employee identity remains the existing `account_id`; rename does not rewrite historical transactions.
- Business settings continue to use the existing `settings` table and transaction-service keys.

## YELLOW — Carried into Phase 7

### PH7-ARCH-01 — MainActivity size

At the Phase 6D checkpoint, `MainActivity.kt` is approximately 99,781 bytes. This is a maintenance risk, not a correctness defect.

**Rule:** do not refactor `MainActivity` for aesthetics. Revisit only if a concrete maintainability/correctness problem appears or a phase genuinely requires separation.

### PH7-ARCH-02 — Event-type constant exposure

History/detail/reporting code can access event-type constants exposed from domain service companion objects. This preserves a single source of truth today, but couples higher-level code to service implementation details.

**Rule:** do not introduce a new event-type abstraction unless a concrete requirement or defect justifies it.

### PH7-ARCH-03 — UI test synchronization helpers

Instrumentation tests contain repeated `waitForCondition()` patterns. Existing tests are green, but new tests must continue to distinguish synchronous validation from asynchronous operations.

**Rule:** no broad test-harness rewrite without concrete evidence.

### PH7-ARCH-04 — Event/header ↔ ledger audit coverage

The project has substantial transaction/correction coverage, but not yet a comprehensive automated cross-family audit comparing event-header truth with inventory/financial ledger rows and audit views.

**Rule:** retain as a hardening candidate; do not mix it into analytics unless there is a direct requirement.

### PH7-ARCH-05 — Master-data duplicate error semantics

Vendor, customer, and employee duplicate creation currently relies on the `accounts.name` UNIQUE constraint. Generic UI exception handling means duplicate-name failures and unrelated persistence failures can surface through the same generic message.

This is functionally correct today but diagnostically weak.

**Rule:** carry forward; do not change merely for elegance.

### PH7-ARCH-06 — Repeated master-data service pattern

`VendorManagementService`, `CustomerManagementService`, and `EmployeeManagementService` are intentionally similar (about 73 lines each at the Phase 6D checkpoint).

**Rule:** do not introduce a generic abstraction merely to remove duplication. Extract only if a concrete shared requirement appears.

## Phase 6 conclusion

No RED architectural defects were identified. Phase 6A–6D can remain as verified checkpoints without a core refactor.

See `PHASE7_ARCHITECTURE_AUDIT.md` for the living Phase 7 record and revised roadmap.
