# Phase 7 — Architecture Audit / Carry-Forward Notes

Status: **Phase 7 planning completed; 7A implementation in progress.**

This document is a living carry-forward record. It records architectural observations and decisions so they remain attached to the project rather than becoming lost conversation history.

## GREEN — Current architecture is healthy

- Business logic remains in domain services; UI does not directly write ledger/event records.
- Corrections remain append-only and go through `CorrectionService`.
- `current_stock` and `account_balances` remain canonical read models rather than duplicated mutable totals.
- History/detail UI reads existing event and ledger records rather than creating a second transaction store.
- Phase 6 master-data management preserves permanent `account_id` identity for vendors, customers, and employees.
- Business settings remain in the existing settings model and continue to feed existing transaction services.
- SQLite foreign-key enforcement and integrity triggers remain active per the SQL blueprint.

## YELLOW — Carry-forward items

### PH7-ARCH-01 — MainActivity growth

At the Phase 6D checkpoint, `MainActivity.kt` is approximately 99,781 bytes. This is a maintenance risk, not a correctness defect.

**Rule:** do not refactor `MainActivity` for aesthetics. Revisit only if a concrete maintainability/correctness problem appears or a phase genuinely requires separation.

### PH7-ARCH-02 — Event-type constant exposure

The history/detail/reporting code can access event-type constants exposed from domain service companion objects. This preserves a single source of truth today, but couples higher-level code to service implementation details.

**Rule:** do not introduce a new event-type abstraction unless a concrete requirement or defect justifies it.

### PH7-ARCH-03 — Repeated UI synchronization helpers

Instrumentation tests contain repeated `waitForCondition()` patterns. Existing tests are green, but new tests must continue to distinguish synchronous validation from asynchronous operations.

**Rule:** no broad test-harness rewrite without concrete evidence.

### PH7-ARCH-04 — Event/header ↔ ledger audit coverage

The project has substantial transaction/correction coverage, but not yet a comprehensive automated cross-family audit that compares event-header truth with inventory/financial ledger rows and the audit views.

**Rule:** retain this as a hardening candidate; do not mix it into analytics unless there is a direct requirement.

### PH7-ARCH-05 — Master-data duplicate error semantics

Vendor, customer, and employee duplicate creation currently relies on the `accounts.name` UNIQUE constraint. The UI catches generic exceptions, so duplicate-name failures and unrelated persistence failures can surface through the same generic message.

This is functionally correct today but diagnostically weak.

**Rule:** carry this forward; do not change it merely for elegance.

### PH7-ARCH-06 — Repeated master-data service pattern

`VendorManagementService`, `CustomerManagementService`, and `EmployeeManagementService` are intentionally similar, each roughly 73 lines at the Phase 6D checkpoint.

The repetition is currently explicit and easy to inspect.

**Rule:** do not introduce a generic abstraction merely to remove duplication. Extract only if a concrete shared requirement appears.

## Phase 7 architecture decision

Phase 7 introduces higher-level capabilities above the frozen transaction core.

```text
Presentation
    ↓
Dashboards / Reports
    ↓
Analytics / Planning
    ↓
Canonical read models / approved read queries
    ↓
Frozen business core
    ↓
Room / SQLite integrity
```

Higher layers may derive, summarize, plan, or visualize the core's truth. They must not redefine or mutate the core's accounting truth without an explicit core-specification change.

## Revised roadmap

- **7A** — Analytics / Reporting Foundation
- **7B** — Dashboards / Visual Reports
- **8** — Budgets / Targets / Planning
- **9** — Backup / Restore
- **10** — Application Preferences
- **11** — Final Hardening / Release Readiness

Each block remains a separately verified checkpoint. Related labels do not permit bundling multiple change categories into one build.

## 7A architectural contract

7A is read-only analytics/reporting sublogic.

### 7A may

- read existing canonical read models;
- read event/header data through approved read-only queries;
- add read-only DAO queries or SQLite views where required for reporting;
- calculate period aggregates, trends, counts, and KPIs from canonical truth.

### 7A may not

- add writable analytics/summary tables;
- persist computed totals as business truth;
- modify events, inventory logs, financial logs, accounts, products, settings, or correction records;
- create a second ledger/accounting calculation;
- mutate canonical read models.

### Correction-aware reporting rule

When reporting event-based business metrics, an event is considered effective only when it has no child correction. A correction chain therefore contributes its latest corrected event header rather than double-counting the original and correction events.

This is a reporting rule only; it does not alter the correction model.

## Pre-7A decision

No core or SQL redesign is required before 7A. 7A is implemented as a subordinate read-only analytics layer over the existing truth.

## Phase 7B carry-forward

- `PH7B-ARCH-01` — Dashboard presentation must remain downstream of `AnalyticsService`; no business calculations or writable analytics state in the UI.
- `PH7B-ARCH-02` — The initial dashboard uses period cards rather than a chart library; richer visualizations remain a later presentation concern.
