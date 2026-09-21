# Phase 14.8.27 — Owner Work Record expense blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-21

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Expense is **multi-role** (`AppPermission.EXPENSE` for Owner + Cashier). This slice must:

1. **Role-neutral content** — form, loading, failure, draft leave-guard presentation in `nativeui/expense/`. No `OwnerSection` / `CashierSection` / foreign bottom nav.
2. **Role-owned shell** — `OwnerExpenseEntryScreen` wraps content in `OwnerScaffold(Work)`; hardware Back → Work home; `EntryLeaveGuard` on Back **and** bottom-nav exits.
3. **Foundation proof** — Owner chrome ✓, Cashier/Ops chrome ✗, Back → Work, session intact.

**Forbidden:** mounting Cashier expense (or any foreign role) screens inside Owner. Cashier expense host wrapper is **out of scope** this slice.

## Connected (this slice)

- Owner Work **Record expense** opens the Owner-hosted expense form.
- Load active expense categories + Cash/Bank payment accounts from `HybridReadModelService.transactionReferences`.
- Post via typed `OperationsBridgeCommandService.recordExpense` (not JSON bridge).
- Success receipt with **Record another** and **Back** to Work.
- Dirty draft leave-guard on hardware Back and Owner bottom nav.

## Explicit preview / deferred

- Cashier-hosted expense shell / Collect-adjacent expense entry
- Pay supplier / Pay salary / Cash count and close
- Manager / Admin expense hosts

## Native data boundary

`NativeExpenseFacade(database, security)`:

- require `AppPermission.EXPENSE`;
- map active `expenseCategories` + `paymentAccounts`; default payment account Cash when present;
- `record` converts paisa → amount and uses Collection-style `occurredAt = businessDate` (`LocalDate.toString()`);
- never call domain `ExpenseService` directly from UI; never invent JSON bridge payloads.

Domain `ExpenseService` / `ExpenseInput` remain read-only for this work — bridge already resolves category by accountId and passes `displayName` into `ExpenseService`.

## Accept gate

**AUTO-ACCEPTED.** Connected = Owner-hosted expense form + post under CORE METHOD. Cashier host deferred.
